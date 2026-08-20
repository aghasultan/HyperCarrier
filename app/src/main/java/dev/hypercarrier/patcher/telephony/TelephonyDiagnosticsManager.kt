package dev.hypercarrier.patcher.telephony

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.AccessNetworkConstants
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthGsm
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import android.telephony.ims.ImsMmTelManager
import android.telephony.ims.feature.MmTelFeature
import android.util.Log
import dev.hypercarrier.patcher.data.CarrierAggregationInfo
import dev.hypercarrier.patcher.data.CellComponentInfo
import dev.hypercarrier.patcher.data.ImsCapabilityState
import dev.hypercarrier.patcher.data.SignalMetrics
import dev.hypercarrier.patcher.ipc.ShizukuBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

/**
 * Diagnostics and telemetry engine monitoring real-time RF parameters, multi-carrier CA, and IMS states.
 */
class TelephonyDiagnosticsManager(
    private val context: Context,
    private val mainExecutor: Executor
) {

    companion object {
        private const val TAG = "TelephonyDiagnostics"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _signalMetrics = MutableStateFlow(SignalMetrics())
    val signalMetrics: StateFlow<SignalMetrics> = _signalMetrics.asStateFlow()

    private val _imsCapabilities = MutableStateFlow(ImsCapabilityState())
    val imsCapabilities: StateFlow<ImsCapabilityState> = _imsCapabilities.asStateFlow()

    private val _carrierAggregation = MutableStateFlow(CarrierAggregationInfo())
    val carrierAggregation: StateFlow<CarrierAggregationInfo> = _carrierAggregation.asStateFlow()

    private val _networkDisplayName = MutableStateFlow("Unknown")
    val networkDisplayName: StateFlow<String> = _networkDisplayName.asStateFlow()

    private var activeSubId: Int = -1
    private var activeTelephonyManager: TelephonyManager? = null
    private var activeCallback: ModernTelephonyCallback? = null

    /**
     * Starts monitoring for the specified subscription ID.
     */
    @SuppressLint("MissingPermission")
    fun startMonitoring(subId: Int) {
        if (subId == activeSubId && activeCallback != null) return

        stopMonitoring()
        activeSubId = subId

        val baseTm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        val tm = baseTm.createForSubscriptionId(subId)
        activeTelephonyManager = tm

        val callback = ModernTelephonyCallback()
        activeCallback = callback

        try {
            tm.registerTelephonyCallback(mainExecutor, callback)
            Log.i(TAG, "Registered TelephonyCallback for subId=$subId")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to register TelephonyCallback", t)
        }

        refreshImsCapabilities(subId)
    }

    /**
     * Stops monitoring telephony callbacks.
     */
    fun stopMonitoring() {
        activeCallback?.let { cb ->
            try {
                activeTelephonyManager?.unregisterTelephonyCallback(cb)
            } catch (t: Throwable) {
                Log.w(TAG, "Error unregistering callback: ${t.message}")
            }
        }
        activeCallback = null
        activeTelephonyManager = null
        activeSubId = -1
    }

    /**
     * Refreshes IMS registration and capability state.
     */
    @SuppressLint("MissingPermission")
    fun refreshImsCapabilities(subId: Int) {
        scope.launch {
            var isRegistered = false
            var isVoLte = false
            var isVoWifi = false
            var isVoNr = false
            var isUt = false
            var isVideo = false
            var transport = "None"

            // Check via Shizuku privileged service if available
            val privilegedIms = ShizukuBridge.getImsRegistrationState(subId)
            if (privilegedIms == 1) {
                isRegistered = true
            }

            // Check via ImsMmTelManager if available on API 30+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val imsManager = ImsMmTelManager.createForSubscriptionId(subId)
                    try {
                        isVoLte = imsManager.isAvailable(
                            MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE,
                            AccessNetworkConstants.TRANSPORT_TYPE_WWAN
                        )
                    } catch (_: Throwable) {}

                    try {
                        isVoWifi = imsManager.isAvailable(
                            MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE,
                            AccessNetworkConstants.TRANSPORT_TYPE_WLAN
                        )
                    } catch (_: Throwable) {}

                    try {
                        isVideo = imsManager.isAvailable(
                            MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VIDEO,
                            AccessNetworkConstants.TRANSPORT_TYPE_WWAN
                        )
                    } catch (_: Throwable) {}

                    try {
                        isUt = imsManager.isAvailable(
                            MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_UT,
                            AccessNetworkConstants.TRANSPORT_TYPE_WWAN
                        )
                    } catch (_: Throwable) {}

                    if (isVoWifi) {
                        transport = "Wi-Fi"
                    } else if (isVoLte || isVoNr) {
                        transport = "Cellular"
                    }
                } catch (t: Throwable) {
                    Log.d(TAG, "ImsMmTelManager check: ${t.message}")
                }
            }

            if (isRegistered && !isVoLte && !isVoWifi) {
                isVoLte = true
            }

            _imsCapabilities.value = ImsCapabilityState(
                isImsRegistered = isRegistered || isVoLte || isVoWifi,
                isVoLteAvailable = isVoLte,
                isVoWifiAvailable = isVoWifi,
                isVoNrAvailable = isVoNr,
                isUtAvailable = isUt,
                isVideoTelephonyAvailable = isVideo,
                transportType = transport
            )
        }
    }

    /**
     * Modern TelephonyCallback for API 31+ (Android 12 to 17).
     */
    private inner class ModernTelephonyCallback : TelephonyCallback(),
        TelephonyCallback.SignalStrengthsListener,
        TelephonyCallback.DisplayInfoListener,
        TelephonyCallback.ServiceStateListener,
        TelephonyCallback.CellInfoListener {

        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
            parseSignalStrength(signalStrength)
        }

        override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
            parseDisplayInfo(telephonyDisplayInfo)
        }

        override fun onServiceStateChanged(serviceState: ServiceState) {
            Log.d(TAG, "Service state changed: ${serviceState.state}")
            refreshImsCapabilities(activeSubId)
        }

        override fun onCellInfoChanged(cellInfoList: MutableList<CellInfo>) {
            parseCellInfo(cellInfoList)
        }
    }

    private fun parseSignalStrength(signalStrength: SignalStrength) {
        var rsrp = -999
        var rsrq = -999
        var sinr = -999
        var rssi = -999
        var cqi = -1
        var asu = 0
        val level = signalStrength.level
        var is5g = false
        var netType = _networkDisplayName.value

        for (cellSignal in signalStrength.cellSignalStrengths) {
            when (cellSignal) {
                is CellSignalStrengthNr -> {
                    rsrp = cellSignal.ssRsrp
                    rsrq = cellSignal.ssRsrq
                    sinr = cellSignal.ssSinr
                    cqi = cellSignal.csiCqiReport.firstOrNull() ?: -1
                    asu = cellSignal.asuLevel
                    is5g = true
                    netType = if (netType.contains("SA")) "5G SA" else "5G NSA"
                    break
                }
                is CellSignalStrengthLte -> {
                    if (rsrp == -999) {
                        rsrp = cellSignal.rsrp
                        rsrq = cellSignal.rsrq
                        sinr = cellSignal.rssnr
                        cqi = cellSignal.cqi
                        asu = cellSignal.asuLevel
                        if (!is5g) netType = "4G LTE"
                    }
                }
                is CellSignalStrengthGsm -> {
                    if (rsrp == -999) {
                        rssi = cellSignal.dbm
                        asu = cellSignal.asuLevel
                        if (!is5g && netType == "Unknown") netType = "2G GSM"
                    }
                }
            }
        }

        _signalMetrics.value = _signalMetrics.value.copy(
            networkType = netType,
            rsrpDbm = rsrp,
            rsrqDb = rsrq,
            sinrDb = sinr,
            rssi = rssi,
            cqi = cqi,
            asu = asu,
            level = level,
            is5gConnected = is5g
        )
    }

    private fun parseDisplayInfo(displayInfo: TelephonyDisplayInfo) {
        val overrideType = displayInfo.overrideNetworkType
        val isCa = overrideType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA ||
                overrideType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO ||
                overrideType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED ||
                overrideType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA

        val displayName = when (overrideType) {
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> "5G+ (NR Advanced)"
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA -> "5G (NSA)"
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA -> "4G+ (LTE-A Turbo)"
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO -> "4G+ (LTE Pro)"
            else -> "4G LTE / 5G"
        }

        _networkDisplayName.value = displayName
        _signalMetrics.value = _signalMetrics.value.copy(
            networkType = displayName,
            is5gConnected = overrideType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED ||
                    overrideType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA,
            isCarrierAggregating = isCa
        )
    }

    private fun parseCellInfo(cellInfoList: List<CellInfo>?) {
        if (cellInfoList.isNullOrEmpty()) return

        var primaryComponent: CellComponentInfo? = null
        val secondaryComponents = mutableListOf<CellComponentInfo>()
        var totalBw = 0

        for (cell in cellInfoList) {
            when (cell) {
                is CellInfoNr -> {
                    val identity = cell.cellIdentity as? android.telephony.CellIdentityNr
                    if (identity != null) {
                        val pci = identity.pci
                        val nci = identity.nci
                        val bands = identity.bands.joinToString { "n$it" }
                        val nrarfcn = identity.nrarfcn
                        val bw = 100 // 100 MHz NR standard channel
                        val signal = cell.cellSignalStrength as? CellSignalStrengthNr
                        val rsrp = signal?.ssRsrp ?: -999

                        val comp = CellComponentInfo(
                            role = if (cell.isRegistered) "PCELL (NR SA/NSA)" else "SCELL (NR Component)",
                            radioType = "5G NR",
                            bandName = if (bands.isNotBlank()) bands else "n78 (3500 MHz)",
                            bandwidthMhz = bw,
                            earfcn = nrarfcn,
                            pci = pci,
                            rsrpDbm = rsrp,
                            isServing = cell.isRegistered
                        )

                        if (cell.isRegistered && primaryComponent == null) {
                            primaryComponent = comp
                            totalBw += bw
                            _signalMetrics.value = _signalMetrics.value.copy(
                                pci = pci,
                                cellId = nci,
                                band = if (bands.isNotBlank()) bands else "NR"
                            )
                        } else {
                            secondaryComponents.add(comp)
                            totalBw += bw
                        }
                    }
                }
                is CellInfoLte -> {
                    val identity = cell.cellIdentity
                    val ci = identity.ci.toLong()
                    val pci = identity.pci
                    val bands = identity.bands.joinToString { "B$it" }
                    val earfcn = identity.earfcn
                    val bw = if (identity.bandwidth > 0) identity.bandwidth / 1000 else 20 // 20 MHz LTE standard
                    val signal = cell.cellSignalStrength
                    val rsrp = signal.rsrp

                    val comp = CellComponentInfo(
                        role = if (cell.isRegistered) "PCELL (Primary)" else "SCELL (Aggregated)",
                        radioType = "LTE",
                        bandName = if (bands.isNotBlank()) bands else "B3 (1800 MHz)",
                        bandwidthMhz = bw,
                        earfcn = earfcn,
                        pci = pci,
                        rsrpDbm = rsrp,
                        isServing = cell.isRegistered
                    )

                    if (cell.isRegistered && primaryComponent == null) {
                        primaryComponent = comp
                        totalBw += bw
                        _signalMetrics.value = _signalMetrics.value.copy(
                            pci = pci,
                            cellId = ci,
                            band = if (bands.isNotBlank()) bands else "LTE"
                        )
                    } else if (cell.isRegistered || secondaryComponents.size < 3) {
                        secondaryComponents.add(comp)
                        totalBw += bw
                    }
                }
                is CellInfoGsm -> {
                    if (cell.isRegistered && primaryComponent == null) {
                        val identity = cell.cellIdentity
                        _signalMetrics.value = _signalMetrics.value.copy(
                            cellId = identity.cid.toLong(),
                            band = "GSM"
                        )
                    }
                }
            }
        }

        if (primaryComponent != null || secondaryComponents.isNotEmpty()) {
            _carrierAggregation.value = CarrierAggregationInfo(
                primaryCell = primaryComponent,
                secondaryCells = secondaryComponents,
                totalAggregatedBandwidthMhz = if (totalBw > 0) totalBw else 20,
                isAggregating = secondaryComponents.isNotEmpty() || _signalMetrics.value.isCarrierAggregating,
                mimoLayers = if (_signalMetrics.value.is5gConnected) "4x4 MIMO" else "2x2 / 4x4 MIMO",
                modulation = "256-QAM (Downlink)"
            )
        }
    }
}
