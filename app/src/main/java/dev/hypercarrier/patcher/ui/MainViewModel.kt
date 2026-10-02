package dev.hypercarrier.patcher.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.hypercarrier.patcher.data.BenchmarkResult
import dev.hypercarrier.patcher.data.CarrierAggregationInfo
import dev.hypercarrier.patcher.data.CarrierConfigItem
import dev.hypercarrier.patcher.data.CarrierConfigPayloadBuilder
import dev.hypercarrier.patcher.data.CarrierPreset
import dev.hypercarrier.patcher.data.CarrierPresets
import dev.hypercarrier.patcher.data.InjectionResult
import dev.hypercarrier.patcher.data.NetworkModeOption
import dev.hypercarrier.patcher.data.SubscriptionData
import dev.hypercarrier.patcher.ipc.ShizukuBridge
import dev.hypercarrier.patcher.service.RadioGuardService
import dev.hypercarrier.patcher.telephony.NetworkBenchmarkManager
import dev.hypercarrier.patcher.telephony.SubscriptionHelper
import dev.hypercarrier.patcher.telephony.TelephonyDiagnosticsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Main ViewModel managing UI state, subscription selection, injection actions,
 * network mode enforcer, auto-healer watchdog, and latency benchmarking.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainViewModel"

        val NETWORK_MODES = listOf(
            NetworkModeOption(
                id = 2,
                title = "5G NSA + LTE-CA Turbo (Full Power)",
                subtitle = "Maximum Bandwidth & 4G/5G Aggregation",
                description = "Enables NR + LTE + LTE-CA aggregation with zero data delay."
            ),
            NetworkModeOption(
                id = 1,
                title = "5G SA Only (Ultra-Fast Pure 5G)",
                subtitle = "Forces Pure 5G Core without LTE fallback",
                description = "Eliminates LTE anchor ping overhead; forces pure 5G Standalone."
            ),
            NetworkModeOption(
                id = 3,
                title = "LTE-A Only (Extreme Battery)",
                subtitle = "High-speed 4G+ without 5G NR battery draw",
                description = "Locks onto 4G+ LTE-Advanced multi-carrier aggregation."
            )
        )
    }

    private val subscriptionHelper = SubscriptionHelper(application)
    private val telephonyDiagnosticsManager = TelephonyDiagnosticsManager(
        application,
        Executors.newSingleThreadExecutor()
    )
    private val benchmarkManager = NetworkBenchmarkManager()

    val isShizukuRunning = ShizukuBridge.isShizukuRunning
    val hasShizukuPermission = ShizukuBridge.hasPermission
    val isServiceConnected = ShizukuBridge.isServiceConnected
    val serviceError = ShizukuBridge.serviceError

    val signalMetrics = telephonyDiagnosticsManager.signalMetrics
    val imsCapabilities = telephonyDiagnosticsManager.imsCapabilities
    val carrierAggregation = telephonyDiagnosticsManager.carrierAggregation
    val networkDisplayName = telephonyDiagnosticsManager.networkDisplayName

    val benchmarkResult = benchmarkManager.benchmarkState

    private val _subscriptions = MutableStateFlow<List<SubscriptionData>>(emptyList())
    val subscriptions: StateFlow<List<SubscriptionData>> = _subscriptions.asStateFlow()

    private val _selectedSubscription = MutableStateFlow<SubscriptionData?>(null)
    val selectedSubscription: StateFlow<SubscriptionData?> = _selectedSubscription.asStateFlow()

    private val _selectedNetworkMode = MutableStateFlow<Int>(2) // Default to Full Performance
    val selectedNetworkMode: StateFlow<Int> = _selectedNetworkMode.asStateFlow()

    private val _isRadioGuardActive = MutableStateFlow<Boolean>(RadioGuardService.isRunning)
    val isRadioGuardActive: StateFlow<Boolean> = _isRadioGuardActive.asStateFlow()

    private val _injectionResult = MutableStateFlow<InjectionResult>(InjectionResult.Idle)
    val injectionResult: StateFlow<InjectionResult> = _injectionResult.asStateFlow()

    private val _activeConfigItems = MutableStateFlow<List<CarrierConfigItem>>(emptyList())
    val activeConfigItems: StateFlow<List<CarrierConfigItem>> = _activeConfigItems.asStateFlow()

    private val _isLoadingConfig = MutableStateFlow(false)
    val isLoadingConfig: StateFlow<Boolean> = _isLoadingConfig.asStateFlow()

    init {
        refreshSubscriptions()
    }

    /**
     * Refreshes SIM cards and active subscriptions.
     */
    fun refreshSubscriptions() {
        val subs = subscriptionHelper.getActiveSubscriptions()
        _subscriptions.value = subs

        if (subs.isNotEmpty()) {
            val currentSelected = _selectedSubscription.value
            val match = subs.firstOrNull { it.subscriptionId == currentSelected?.subscriptionId }
                ?: subs.firstOrNull { it.isDataActive }
                ?: subs.first()

            selectSubscription(match)

            // Auto-enable VoIMS (VoLTE opt-in) on active subscriptions
            subs.forEach { sub ->
                viewModelScope.launch {
                    dev.hypercarrier.patcher.ipc.HyperCarrierEngine.provisionVoIms(sub.subscriptionId, true)
                }
            }
        } else {
            _selectedSubscription.value = null
            telephonyDiagnosticsManager.stopMonitoring()
        }
    }

    /**
     * Selects a subscription for configuration and diagnostics monitoring.
     */
    fun selectSubscription(subscription: SubscriptionData) {
        _selectedSubscription.value = subscription
        telephonyDiagnosticsManager.startMonitoring(subscription.subscriptionId)
        loadActiveCarrierConfig(subscription.subscriptionId)
    }

    /**
     * Requests Shizuku permission.
     */
    fun requestShizukuPermission() {
        ShizukuBridge.requestPermission()
    }

    /**
     * Retries connecting to Shizuku.
     */
    fun retryShizukuConnection() {
        ShizukuBridge.checkState()
    }

    /**
     * Applies a pre-baked Carrier Preset with reboot-persistence and Turbo CA.
     */
    fun applyPreset(preset: CarrierPreset) {
        val sub = _selectedSubscription.value
        if (sub == null) {
            _injectionResult.value = InjectionResult.Error("Please select a SIM card first.")
            return
        }

        viewModelScope.launch {
            _injectionResult.value = InjectionResult.InProgress
            try {
                val bundle = preset.payloadBuilder(sub.subscriptionId)
                val result = dev.hypercarrier.patcher.ipc.HyperCarrierEngine.applyFullCarrierProfile(
                    getApplication(), sub.subscriptionId, bundle
                )

                if (result.isSuccess) {
                    _injectionResult.value = InjectionResult.Success(
                        message = "Successfully applied '${preset.name}'! IMS Stack Reset & 4G/5G Calling Unlocked.",
                        appliedKeysCount = bundle.size()
                    )
                    loadActiveCarrierConfig(sub.subscriptionId)
                    telephonyDiagnosticsManager.refreshImsCapabilities(sub.subscriptionId)
                } else {
                    _injectionResult.value = InjectionResult.Error(
                        message = "Injection failed: ${result.exceptionOrNull()?.message}",
                        throwable = result.exceptionOrNull()
                    )
                }
            } catch (t: Throwable) {
                _injectionResult.value = InjectionResult.Error("Error applying preset: ${t.message}", t)
            }
        }
    }

    /**
     * Injects custom CarrierConfig bundle with reboot-persistence.
     */
    fun applyCustomBundle(bundle: PersistableBundle) {
        val sub = _selectedSubscription.value
        if (sub == null) {
            _injectionResult.value = InjectionResult.Error("Please select a SIM card first.")
            return
        }

        viewModelScope.launch {
            _injectionResult.value = InjectionResult.InProgress
            try {
                val result = dev.hypercarrier.patcher.ipc.HyperCarrierEngine.applyFullCarrierProfile(
                    getApplication(), sub.subscriptionId, bundle
                )

                if (result.isSuccess) {
                    _injectionResult.value = InjectionResult.Success(
                        message = "CarrierConfig overrides, VoIMS Provisioning & Modem IMS Stack Reset applied!",
                        appliedKeysCount = bundle.size()
                    )
                    loadActiveCarrierConfig(sub.subscriptionId)
                    telephonyDiagnosticsManager.refreshImsCapabilities(sub.subscriptionId)
                } else {
                    _injectionResult.value = InjectionResult.Error(
                        message = "Injection failed: ${result.exceptionOrNull()?.message}",
                        throwable = result.exceptionOrNull()
                    )
                }
            } catch (t: Throwable) {
                _injectionResult.value = InjectionResult.Error("Error injecting bundle: ${t.message}", t)
            }
        }
    }

    /**
     * Clears all CarrierConfig overrides for the selected SIM, restoring OEM defaults.
     */
    fun clearOverrides() {
        val sub = _selectedSubscription.value
        if (sub == null) {
            _injectionResult.value = InjectionResult.Error("Please select a SIM card first.")
            return
        }

        viewModelScope.launch {
            _injectionResult.value = InjectionResult.InProgress
            try {
                val result = dev.hypercarrier.patcher.ipc.HyperCarrierEngine.clearConfig(
                    getApplication(), sub.subscriptionId
                )
                dev.hypercarrier.patcher.ipc.HyperCarrierEngine.resetIms(sub.subscriptionId)

                if (result.isSuccess) {
                    _injectionResult.value = InjectionResult.Success(
                        message = "CarrierConfig overrides cleared. Restored OEM / Carrier defaults.",
                        appliedKeysCount = 0
                    )
                    loadActiveCarrierConfig(sub.subscriptionId)
                    telephonyDiagnosticsManager.refreshImsCapabilities(sub.subscriptionId)
                } else {
                    _injectionResult.value = InjectionResult.Error(
                        message = "Clear failed: ${result.exceptionOrNull()?.message}",
                        throwable = result.exceptionOrNull()
                    )
                }
            } catch (t: Throwable) {
                _injectionResult.value = InjectionResult.Error("Error clearing overrides: ${t.message}", t)
            }
        }
    }

    /**
     * Enforces high-level network mode (5G SA Only, 5G NSA/LTE-A Turbo, LTE-A Only).
     */
    fun setNetworkMode(modeId: Int) {
        val sub = _selectedSubscription.value ?: return
        _selectedNetworkMode.value = modeId
        viewModelScope.launch {
            val result = dev.hypercarrier.patcher.ipc.HyperCarrierEngine.setAllowedNetworkMode(getApplication(), sub.subscriptionId, modeId)
            if (result.isSuccess) {
                _injectionResult.value = InjectionResult.Success(
                    message = "Network Mode enforced successfully.",
                    appliedKeysCount = 1
                )
            } else {
                _injectionResult.value = InjectionResult.Error("Failed to set mode: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * Executes 1-tap Radio Turbo Flush.
     */
    fun triggerRadioFlush() {
        val sub = _selectedSubscription.value ?: return
        viewModelScope.launch {
            _injectionResult.value = InjectionResult.InProgress
            val result = dev.hypercarrier.patcher.ipc.HyperCarrierEngine.executeRadioFlush(sub.subscriptionId)
            if (result.isSuccess) {
                _injectionResult.value = InjectionResult.Success(
                    message = "Radio Turbo Flush executed! Modem IMS SIP stack reset & re-attached to optimal carrier components.",
                    appliedKeysCount = 1
                )
                telephonyDiagnosticsManager.refreshImsCapabilities(sub.subscriptionId)
            } else {
                _injectionResult.value = InjectionResult.Error("Radio flush failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * Sets custom carrier display name (SPN in status bar).
     */
    fun setCarrierDisplayName(name: String) {
        val sub = _selectedSubscription.value ?: return
        viewModelScope.launch {
            _injectionResult.value = InjectionResult.InProgress
            val bundle = PersistableBundle().apply {
                putBoolean("carrier_name_override_bool", true)
                putString("carrier_name_string", name)
            }
            val result = dev.hypercarrier.patcher.ipc.HyperCarrierEngine.applyFullCarrierProfile(
                getApplication(), sub.subscriptionId, bundle
            )
            if (result.isSuccess) {
                _injectionResult.value = InjectionResult.Success(
                    message = "Carrier name branding updated to '$name'!",
                    appliedKeysCount = 2
                )
                loadActiveCarrierConfig(sub.subscriptionId)
            } else {
                _injectionResult.value = InjectionResult.Error("Failed to update carrier name: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * Sets custom VoLTE / VoWiFi SIP User-Agent string.
     */
    fun setCustomUserAgent(userAgent: String) {
        val sub = _selectedSubscription.value ?: return
        viewModelScope.launch {
            _injectionResult.value = InjectionResult.InProgress
            val bundle = PersistableBundle().apply {
                putString("ims.ims_user_agent_string", userAgent)
            }
            val result = dev.hypercarrier.patcher.ipc.HyperCarrierEngine.applyFullCarrierProfile(
                getApplication(), sub.subscriptionId, bundle
            )
            if (result.isSuccess) {
                _injectionResult.value = InjectionResult.Success(
                    message = "VoLTE SIP User-Agent updated!",
                    appliedKeysCount = 1
                )
                loadActiveCarrierConfig(sub.subscriptionId)
            } else {
                _injectionResult.value = InjectionResult.Error("Failed to update User-Agent: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * Exports full real-time field telemetry report.
     */
    fun exportTelemetryReport(): String {
        val sub = _selectedSubscription.value
        val metrics = signalMetrics.value
        val ims = imsCapabilities.value
        val ca = carrierAggregation.value
        val benchmark = benchmarkResult.value

        return buildString {
            appendLine("=== HYPERCARRIER FIELD TELEMETRY REPORT ===")
            appendLine("Timestamp: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}")
            appendLine("SIM Carrier: ${sub?.carrierName ?: "Unknown"} (PLMN: ${sub?.plmn ?: "N/A"})")
            appendLine("Slot: ${if (sub?.isEmbedded == true) "eSIM" else "SIM ${(sub?.slotIndex ?: 0) + 1}"}")
            appendLine()
            appendLine("--- RF SIGNAL PARAMETERS ---")
            appendLine("Network Type: ${metrics.networkType}")
            appendLine("RSRP: ${if (metrics.rsrpDbm != -999) "${metrics.rsrpDbm} dBm" else "N/A"}")
            appendLine("RSRQ: ${if (metrics.rsrqDb != -999) "${metrics.rsrqDb} dB" else "N/A"}")
            appendLine("SINR: ${if (metrics.sinrDb != -999) "${metrics.sinrDb} dB" else "N/A"}")
            appendLine("ASU: ${metrics.asu}")
            appendLine("Serving Band: ${metrics.band}")
            appendLine("Physical Cell ID (PCI): ${metrics.pci}")
            appendLine("Cell Identity: ${metrics.cellId}")
            appendLine()
            appendLine("--- IMS ENGINE & VOICE CORE ---")
            appendLine("IMS Registration Status: ${if (ims.isImsRegistered) "REGISTERED" else "UNREGISTERED"}")
            appendLine("VoLTE (4G Voice): ${if (ims.isVoLteAvailable) "AVAILABLE" else "DISABLED"}")
            appendLine("VoWiFi (Wi-Fi Calling): ${if (ims.isVoWifiAvailable) "AVAILABLE" else "DISABLED"}")
            appendLine("VoNR (5G Voice): ${if (ims.isVoNrAvailable) "AVAILABLE" else "DISABLED"}")
            appendLine("ViLTE (Video): ${if (ims.isVideoTelephonyAvailable) "AVAILABLE" else "DISABLED"}")
            appendLine("UT / XCAP: ${if (ims.isUtAvailable) "AVAILABLE" else "DISABLED"}")
            appendLine("Transport: ${ims.transportType}")
            appendLine()
            appendLine("--- CARRIER AGGREGATION (CA) ---")
            appendLine("Total Aggregated Bandwidth: ${ca.totalAggregatedBandwidthMhz} MHz")
            appendLine("MIMO Configuration: ${ca.mimoLayers}")
            appendLine("Downlink Modulation: ${ca.modulation}")
            appendLine("Is Aggregating: ${ca.isAggregating}")
            appendLine()
            appendLine("--- LATENCY & JITTER BENCHMARK ---")
            appendLine("Server: ${benchmark.serverName}")
            appendLine("Average Latency: ${String.format("%.2f", benchmark.avgLatencyMs)} ms")
            appendLine("Jitter: ${String.format("%.2f", benchmark.jitterMs)} ms")
            appendLine("Packet Loss: ${benchmark.packetLossPercent}%")
        }
    }

    /**
     * Toggles VoLTE (4G HD Voice) on/off in real-time.
     */
    fun toggleVoLte(enable: Boolean) {
        val sub = _selectedSubscription.value ?: return
        viewModelScope.launch {
            _injectionResult.value = InjectionResult.InProgress
            val bundle = PersistableBundle().apply {
                putBoolean(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL, enable)
                putBoolean(CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL, true)
                putBoolean(CarrierConfigManager.KEY_ENHANCED_4G_LTE_ON_BY_DEFAULT_BOOL, enable)
                putBoolean(CarrierConfigManager.KEY_HIDE_ENHANCED_4G_LTE_BOOL, false)
            }
            val result = dev.hypercarrier.patcher.ipc.HyperCarrierEngine.applyFullCarrierProfile(
                getApplication(), sub.subscriptionId, bundle
            )
            if (result.isSuccess) {
                _injectionResult.value = InjectionResult.Success(
                    message = "VoLTE ${if (enable) "Enabled & Provisioned" else "Disabled"}.",
                    appliedKeysCount = bundle.size()
                )
                telephonyDiagnosticsManager.refreshImsCapabilities(sub.subscriptionId)
            } else {
                _injectionResult.value = InjectionResult.Error("Failed to set VoLTE: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * Toggles VoWiFi (Wi-Fi Calling) on/off in real-time.
     */
    fun toggleVoWifi(enable: Boolean) {
        val sub = _selectedSubscription.value ?: return
        viewModelScope.launch {
            _injectionResult.value = InjectionResult.InProgress
            val bundle = PersistableBundle().apply {
                putBoolean(CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL, enable)
                putBoolean(CarrierConfigManager.KEY_CARRIER_DEFAULT_WFC_IMS_ROAMING_ENABLED_BOOL, enable)
                putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_MODE_BOOL, true)
                putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL, true)
                putInt(CarrierConfigManager.KEY_WFC_SPN_FORMAT_IDX_INT, 4)
            }
            val result = dev.hypercarrier.patcher.ipc.HyperCarrierEngine.applyFullCarrierProfile(
                getApplication(), sub.subscriptionId, bundle
            )
            if (result.isSuccess) {
                _injectionResult.value = InjectionResult.Success(
                    message = "Wi-Fi Calling ${if (enable) "Enabled & Provisioned" else "Disabled"}.",
                    appliedKeysCount = bundle.size()
                )
                telephonyDiagnosticsManager.refreshImsCapabilities(sub.subscriptionId)
            } else {
                _injectionResult.value = InjectionResult.Error("Failed to set VoWiFi: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * Toggles 5G VoNR on/off in real-time.
     */
    fun toggleVoNr(enable: Boolean) {
        val sub = _selectedSubscription.value ?: return
        viewModelScope.launch {
            _injectionResult.value = InjectionResult.InProgress
            val bundle = PersistableBundle().apply {
                putBoolean(CarrierConfigManager.KEY_VONR_ENABLED_BOOL, enable)
                putBoolean(CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL, true)
            }
            val result = dev.hypercarrier.patcher.ipc.HyperCarrierEngine.applyFullCarrierProfile(
                getApplication(), sub.subscriptionId, bundle
            )
            if (result.isSuccess) {
                _injectionResult.value = InjectionResult.Success(
                    message = "5G VoNR ${if (enable) "Enabled & Provisioned" else "Disabled"}.",
                    appliedKeysCount = bundle.size()
                )
                telephonyDiagnosticsManager.refreshImsCapabilities(sub.subscriptionId)
            } else {
                _injectionResult.value = InjectionResult.Error("Failed to set VoNR: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * Toggles Carrier Video Calling (ViLTE) on/off in real-time.
     */
    fun toggleViLte(enable: Boolean) {
        val sub = _selectedSubscription.value ?: return
        viewModelScope.launch {
            _injectionResult.value = InjectionResult.InProgress
            val bundle = PersistableBundle().apply {
                putBoolean(CarrierConfigManager.KEY_CARRIER_VT_AVAILABLE_BOOL, enable)
            }
            val result = dev.hypercarrier.patcher.ipc.HyperCarrierEngine.applyFullCarrierProfile(
                getApplication(), sub.subscriptionId, bundle
            )
            if (result.isSuccess) {
                _injectionResult.value = InjectionResult.Success(
                    message = "ViLTE Video Calling ${if (enable) "Enabled" else "Disabled"}.",
                    appliedKeysCount = 1
                )
                telephonyDiagnosticsManager.refreshImsCapabilities(sub.subscriptionId)
            } else {
                _injectionResult.value = InjectionResult.Error("Failed to set ViLTE: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * Sets Wi-Fi Calling preference mode (1 = Wi-Fi Preferred, 2 = Cellular Preferred).
     */
    fun setVoWifiMode(mode: Int) {
        val sub = _selectedSubscription.value ?: return
        viewModelScope.launch {
            _injectionResult.value = InjectionResult.InProgress
            val bundle = PersistableBundle().apply {
                putInt(CarrierConfigManager.KEY_CARRIER_DEFAULT_WFC_IMS_MODE_INT, mode)
                putInt(CarrierConfigManager.KEY_CARRIER_DEFAULT_WFC_IMS_ROAMING_MODE_INT, mode)
            }
            val result = dev.hypercarrier.patcher.ipc.HyperCarrierEngine.applyFullCarrierProfile(
                getApplication(), sub.subscriptionId, bundle
            )
            if (result.isSuccess) {
                _injectionResult.value = InjectionResult.Success(
                    message = "Wi-Fi Calling Mode updated to: ${if (mode == 1) "Wi-Fi Preferred" else "Cellular Preferred"}.",
                    appliedKeysCount = 1
                )
            } else {
                _injectionResult.value = InjectionResult.Error("Failed to set VoWiFi mode: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * Forces immediate IMS re-registration trigger on modem.
     */
    fun forceReRegisterIms() {
        val sub = _selectedSubscription.value ?: return
        viewModelScope.launch {
            _injectionResult.value = InjectionResult.InProgress
            val result = dev.hypercarrier.patcher.ipc.HyperCarrierEngine.resetIms(sub.subscriptionId)
            dev.hypercarrier.patcher.ipc.HyperCarrierEngine.provisionVoIms(sub.subscriptionId, true)
            if (result.isSuccess) {
                _injectionResult.value = InjectionResult.Success(
                    message = "Modem IMS Stack Reset dispatched & VoIMS re-provisioned.",
                    appliedKeysCount = 1
                )
                kotlinx.coroutines.delay(1500)
                telephonyDiagnosticsManager.refreshImsCapabilities(sub.subscriptionId)
            } else {
                _injectionResult.value = InjectionResult.Error("IMS re-registration failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }


    /**
     * Toggles the Autonomous Radio Guard & Auto-Healer background watchdog.
     */
    fun toggleRadioGuard(enable: Boolean) {
        val context = getApplication<Application>()
        val intent = Intent(context, RadioGuardService::class.java).apply {
            action = if (enable) RadioGuardService.ACTION_START_GUARD else RadioGuardService.ACTION_STOP_GUARD
        }

        try {
            if (enable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        context.startForegroundService(intent)
                    } catch (t: Throwable) {
                        Log.w(TAG, "startForegroundService fallback to startService: ${t.message}")
                        context.startService(intent)
                    }
                } else {
                    context.startService(intent)
                }
                _isRadioGuardActive.value = true
                _injectionResult.value = InjectionResult.Success("Auto-Healer Watchdog Activated.", 1)
            } else {
                context.startService(intent)
                _isRadioGuardActive.value = false
                _injectionResult.value = InjectionResult.Success("Auto-Healer Watchdog Deactivated.", 0)
            }
        } catch (t: Throwable) {
            _isRadioGuardActive.value = false
            _injectionResult.value = InjectionResult.Error("Failed to toggle Auto-Healer: ${t.message}")
        }
    }

    /**
     * Executes real-time Anycast latency & jitter benchmark.
     */
    fun runBenchmark(host: String = "1.1.1.1", label: String = "Cloudflare Anycast (1.1.1.1)") {
        viewModelScope.launch {
            benchmarkManager.runBenchmark(host, label)
        }
    }

    /**
     * Dismisses the injection result banner.
     */
    fun dismissInjectionResult() {
        _injectionResult.value = InjectionResult.Idle
    }

    /**
     * Loads active CarrierConfig bundle and parses keys for inspection.
     */
    fun loadActiveCarrierConfig(subId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingConfig.value = true
            try {
                val ccm = getApplication<Application>().getSystemService(Context.CARRIER_CONFIG_SERVICE) as? CarrierConfigManager
                val bundle = ccm?.getConfigForSubId(subId)

                val items = mutableListOf<CarrierConfigItem>()
                if (bundle != null) {
                    val keySet = bundle.keySet()
                    for (key in keySet) {
                        val valueObj = bundle.get(key)
                        val typeName = when (valueObj) {
                            is Boolean -> "Boolean"
                            is Int -> "Int"
                            is Long -> "Long"
                            is Double -> "Double"
                            is String -> "String"
                            is IntArray -> "IntArray"
                            is LongArray -> "LongArray"
                            is DoubleArray -> "DoubleArray"
                            is Array<*> -> "StringArray"
                            is PersistableBundle -> "Bundle"
                            else -> valueObj?.javaClass?.simpleName ?: "Unknown"
                        }

                        val stringValue = when (valueObj) {
                            is IntArray -> valueObj.contentToString()
                            is LongArray -> valueObj.contentToString()
                            is DoubleArray -> valueObj.contentToString()
                            is Array<*> -> valueObj.contentToString()
                            else -> valueObj?.toString() ?: "null"
                        }

                        val isOverridden = isKeyKnownOverride(key)

                        items.add(
                            CarrierConfigItem(
                                key = key,
                                value = stringValue,
                                type = typeName,
                                isOverridden = isOverridden
                            )
                        )
                    }
                }
                items.sortBy { it.key }
                _activeConfigItems.value = items
            } catch (t: Throwable) {
                Log.e(TAG, "Error loading active CarrierConfig: ${t.message}", t)
            } finally {
                _isLoadingConfig.value = false
            }
        }
    }

    private fun isKeyKnownOverride(key: String): Boolean {
        return key.startsWith("carrier_volte") ||
                key.startsWith("carrier_wfc") ||
                key.startsWith("vonr_") ||
                key.startsWith("5g_") ||
                key.startsWith("carrier_nr") ||
                key.contains("bandwidth") ||
                key.contains("delay") ||
                key.contains("thresholds") ||
                key.contains("enhanced_4g")
    }

    override fun onCleared() {
        super.onCleared()
        telephonyDiagnosticsManager.stopMonitoring()
    }
}
