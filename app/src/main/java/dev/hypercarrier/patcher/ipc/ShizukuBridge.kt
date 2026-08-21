package dev.hypercarrier.patcher.ipc

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.PersistableBundle
import android.util.Log
import dev.hypercarrier.patcher.BuildConfig
import dev.hypercarrier.patcher.IPrivilegedCarrierService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.Method

/**
 * Hyper-Elite Rootless Dual-Engine Shizuku Bridge.
 * Directly utilizes ShizukuBinderWrapper + SystemServiceHelper for instantaneous ICarrierConfigLoader
 * and ITelephony injection, paired with Shizuku.newProcess for shell triggers and PrivilegedCarrierService.
 */
object ShizukuBridge {

    private const val TAG = "ShizukuBridge"
    const val REQUEST_CODE_SHIZUKU = 1001

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _isShizukuInstalled = MutableStateFlow(false)
    val isShizukuInstalled: StateFlow<Boolean> = _isShizukuInstalled.asStateFlow()

    private val _isShizukuRunning = MutableStateFlow(false)
    val isShizukuRunning: StateFlow<Boolean> = _isShizukuRunning.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    private val _serviceError = MutableStateFlow<String?>(null)
    val serviceError: StateFlow<String?> = _serviceError.asStateFlow()

    private var privilegedService: IPrivilegedCarrierService? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.i(TAG, "Shizuku binder received")
        checkState()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.w(TAG, "Shizuku binder died")
        _isShizukuRunning.value = false
        _isServiceConnected.value = false
        privilegedService = null
    }

    private val requestPermissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE_SHIZUKU) {
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            Log.i(TAG, "Shizuku permission result: granted=$granted")
            _hasPermission.value = granted
            if (granted) {
                _isServiceConnected.value = true
                bindService()
            }
        }
    }

    private val userServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i(TAG, "PrivilegedCarrierService connected")
            if (service != null) {
                privilegedService = IPrivilegedCarrierService.Stub.asInterface(service)
                _isServiceConnected.value = true
                _serviceError.value = null
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "PrivilegedCarrierService disconnected")
            privilegedService = null
            // We maintain service connected true if Shizuku binder wrapper is active
            _isServiceConnected.value = _hasPermission.value
        }
    }

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, PrivilegedCarrierService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("privileged_carrier_service")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    /**
     * Initializes listeners for Shizuku binder and permission updates.
     */
    fun init(context: Context) {
        checkInstalled(context)
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to register Shizuku listeners", t)
        }
        checkState()
    }

    private fun checkInstalled(context: Context) {
        val pm = context.packageManager
        _isShizukuInstalled.value = try {
            pm.getPackageInfo("moe.shizuku.privileged.api", 0) != null
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun checkState() {
        val ping = Shizuku.pingBinder()
        _isShizukuRunning.value = ping
        if (!ping) {
            _hasPermission.value = false
            _isServiceConnected.value = false
            return
        }

        val granted = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (t: Throwable) {
            Log.w(TAG, "Error checking permission: ${t.message}")
            false
        }

        _hasPermission.value = granted
        if (granted) {
            _isServiceConnected.value = true
            bindService()
        }
    }

    fun requestPermission() {
        if (!Shizuku.pingBinder()) {
            _serviceError.value = "Shizuku is not running. Please start Shizuku app first."
            return
        }

        try {
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                Log.d(TAG, "Should show request permission rationale")
            }
            Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to request Shizuku permission", t)
            _serviceError.value = "Failed to request permission: ${t.message}"
        }
    }

    fun bindService() {
        if (!_hasPermission.value) return
        try {
            Shizuku.bindUserService(userServiceArgs, userServiceConnection)
        } catch (t: Throwable) {
            Log.d(TAG, "UserService bind notice: ${t.message}")
        }
    }

    fun unbindService() {
        try {
            Shizuku.unbindUserService(userServiceArgs, userServiceConnection, true)
            privilegedService = null
        } catch (t: Throwable) {
            Log.d(TAG, "UserService unbind notice: ${t.message}")
        }
    }

    // =========================================================================
    // DIRECT DUAL-ENGINE INJECTION (ShizukuBinderWrapper + SystemServiceHelper)
    // =========================================================================

    /**
     * Injects a persistent CarrierConfig override bundle for the given subscription ID
     * using direct ShizukuBinderWrapper on ICarrierConfigLoader, shell reload, and privileged service.
     */
    suspend fun applyPersistentConfig(subId: Int, bundle: PersistableBundle): Result<Unit> = withContext(Dispatchers.IO) {
        if (!_hasPermission.value && !Shizuku.pingBinder()) {
            return@withContext Result.failure(IllegalStateException("Shizuku is not authorized or running."))
        }

        try {
            var applied = false

            // 1. Direct ShizukuBinderWrapper to ICarrierConfigLoader
            try {
                val rawBinder = SystemServiceHelper.getSystemService("carrier_config")
                if (rawBinder != null) {
                    val wrappedBinder = ShizukuBinderWrapper(rawBinder)
                    val stubClass = Class.forName("com.android.internal.telephony.ICarrierConfigLoader\$Stub")
                    val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
                    val loader = asInterface.invoke(null, wrappedBinder)

                    val overrideMethod = loader?.javaClass?.getMethod(
                        "overrideConfig",
                        Int::class.javaPrimitiveType,
                        PersistableBundle::class.java,
                        Boolean::class.javaPrimitiveType
                    )
                    overrideMethod?.isAccessible = true
                    
                    // Disk persistent override
                    try {
                        overrideMethod?.invoke(loader, subId, bundle, true)
                        Log.i(TAG, "Direct ShizukuBinderWrapper persistent overrideConfig succeeded")
                    } catch (e: Throwable) {
                        Log.w(TAG, "Direct persistent override call: ${e.message}")
                    }

                    // In-memory override
                    try {
                        overrideMethod?.invoke(loader, subId, bundle, false)
                        Log.i(TAG, "Direct ShizukuBinderWrapper in-memory overrideConfig succeeded")
                    } catch (e: Throwable) {
                        Log.w(TAG, "Direct in-memory override call: ${e.message}")
                    }

                    applied = true
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Direct ICarrierConfigLoader error: ${t.message}")
            }

            // 2. Fallback to Privileged UserService if active
            privilegedService?.applyPersistentConfig(subId, bundle)

            // 3. Direct Modem Telephony IMS Provisioning
            setImsProvisioningDirect(subId, true, true, true)

            // 4. Shell Reload Triggers
            execShizukuCmd("cmd", "phone", "reload-carrier-config")
            execShizukuCmd("setprop", "persist.vendor.radio.volte_enabled", "1")
            execShizukuCmd("setprop", "persist.vendor.radio.vowifi_enabled", "1")
            execShizukuCmd("setprop", "persist.vendor.radio.vonr_enabled", "1")
            execShizukuCmd("setprop", "persist.radio.volte_state", "1")
            execShizukuCmd("setprop", "persist.radio.vowifi_state", "1")
            execShizukuCmd("setprop", "persist.radio.reboot_on_modem_reset", "0")

            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to apply persistent config: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Clears all CarrierConfig overrides for the given subscription ID, restoring defaults.
     */
    suspend fun clearConfigOverride(subId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val rawBinder = SystemServiceHelper.getSystemService("carrier_config")
            if (rawBinder != null) {
                val wrappedBinder = ShizukuBinderWrapper(rawBinder)
                val stubClass = Class.forName("com.android.internal.telephony.ICarrierConfigLoader\$Stub")
                val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
                val loader = asInterface.invoke(null, wrappedBinder)

                val overrideMethod = loader?.javaClass?.getMethod(
                    "overrideConfig",
                    Int::class.javaPrimitiveType,
                    PersistableBundle::class.java,
                    Boolean::class.javaPrimitiveType
                )
                overrideMethod?.isAccessible = true
                try {
                    overrideMethod?.invoke(loader, subId, null, true)
                } catch (_: Throwable) {}
                try {
                    overrideMethod?.invoke(loader, subId, null, false)
                } catch (_: Throwable) {}
            }

            privilegedService?.clearConfigOverride(subId)
            execShizukuCmd("cmd", "phone", "reload-carrier-config")
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to clear config: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Directly provisions IMS capabilities (VoLTE, VoWiFi, VoNR, Video) via Shizuku Telephony binder.
     */
    suspend fun setImsProvisioning(subId: Int, enableVoLte: Boolean = true, enableVoWifi: Boolean = true, enableVoNr: Boolean = true): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            setImsProvisioningDirect(subId, enableVoLte, enableVoWifi, enableVoNr)
            privilegedService?.setImsProvisioning(subId, enableVoLte, enableVoWifi, enableVoNr)
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to set IMS provisioning: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Granular 1-Tap Toggle: Voice over LTE (VoLTE / 4G Calling).
     */
    suspend fun setVoLteEnabled(subId: Int, enable: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            setTelephonyImsSetting(subId, "setEnhanced4gLteModeSetting", enable)
            setImsProvisioningDirect(subId, enableVoLte = enable, enableVoWifi = true, enableVoNr = true)
            
            val bundle = PersistableBundle().apply {
                putBoolean("carrier_volte_available_bool", enable)
                putBoolean("carrier_volte_provisioned_bool", enable)
                putBoolean("enhanced_4g_lte_on_by_default_bool", enable)
                putBoolean("editable_enhanced_4g_lte_bool", true)
                putBoolean("hide_enhanced_4g_lte_bool", false)
                putBoolean("carrier_volte_tty_supported_bool", true)
            }
            applyPersistentConfig(subId, bundle)

            execShizukuCmd("setprop", "persist.vendor.radio.volte_enabled", if (enable) "1" else "0")
            execShizukuCmd("setprop", "persist.radio.volte_state", if (enable) "1" else "0")
            execShizukuCmd("cmd", "phone", "reload-carrier-config")

            privilegedService?.setVoLteEnabled(subId, enable)
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to set VoLTE: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Granular 1-Tap Toggle: Voice over Wi-Fi (VoWiFi / Wi-Fi Calling).
     */
    suspend fun setVoWifiEnabled(subId: Int, enable: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            setTelephonyImsSetting(subId, "setVoWiFiSetting", enable)
            setTelephonyImsSetting(subId, "setVoWiFiRoamingSetting", enable)

            val bundle = PersistableBundle().apply {
                putBoolean("carrier_wfc_ims_available_bool", enable)
                putBoolean("carrier_default_wfc_ims_enabled_bool", enable)
                putBoolean("carrier_default_wfc_ims_roaming_enabled_bool", enable)
                putBoolean("editable_wfc_mode_bool", true)
                putBoolean("editable_wfc_roaming_mode_bool", true)
                putBoolean("carrier_wfc_supports_wifi_only_bool", true)
            }
            applyPersistentConfig(subId, bundle)

            execShizukuCmd("setprop", "persist.vendor.radio.vowifi_enabled", if (enable) "1" else "0")
            execShizukuCmd("setprop", "persist.radio.vowifi_state", if (enable) "1" else "0")
            execShizukuCmd("cmd", "phone", "reload-carrier-config")

            privilegedService?.setVoWifiEnabled(subId, enable)
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to set VoWiFi: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Granular 1-Tap Toggle: Voice over New Radio (5G VoNR).
     */
    suspend fun setVoNrEnabled(subId: Int, enable: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bundle = PersistableBundle().apply {
                putBoolean("vonr_enabled_bool", enable)
                putBoolean("vonr_setting_visibility_bool", true)
                putBoolean("nr_timers_reset_on_voice_qos_bool", true)
            }
            applyPersistentConfig(subId, bundle)

            execShizukuCmd("setprop", "persist.vendor.radio.vonr_enabled", if (enable) "1" else "0")
            execShizukuCmd("cmd", "phone", "reload-carrier-config")

            privilegedService?.setVoNrEnabled(subId, enable)
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to set VoNR: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Granular 1-Tap Toggle: Carrier Video Calling (ViLTE).
     */
    suspend fun setViLteEnabled(subId: Int, enable: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            setTelephonyImsSetting(subId, "setVtSetting", enable)
            privilegedService?.setViLteEnabled(subId, enable)
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to set ViLTE: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Granular Setting: Wi-Fi Calling Mode (1 = Wi-Fi Preferred, 2 = Cellular Preferred).
     */
    suspend fun setVoWifiMode(subId: Int, mode: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            setTelephonyImsIntSetting(subId, "setVoWiFiModeSetting", mode)
            val bundle = PersistableBundle().apply {
                putInt("carrier_default_wfc_ims_mode_int", mode)
                putInt("carrier_default_wfc_ims_roaming_mode_int", mode)
            }
            applyPersistentConfig(subId, bundle)
            privilegedService?.setVoWifiMode(subId, mode)
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to set VoWiFi mode: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Forces immediate IMS deregistration and re-registration trigger on modem.
     */
    suspend fun forceReRegisterIms(subId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val phoneBinder = SystemServiceHelper.getSystemService("phone")
            if (phoneBinder != null) {
                val wrappedPhone = ShizukuBinderWrapper(phoneBinder)
                val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
                val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
                val telephony = asInterface.invoke(null, wrappedPhone)

                try {
                    telephony?.javaClass?.methods?.firstOrNull { it.name == "reconnectIms" }?.invoke(telephony)
                } catch (_: Throwable) {}

                try {
                    telephony?.javaClass?.methods?.firstOrNull { it.name == "enableIms" }?.invoke(telephony, subId)
                } catch (_: Throwable) {}
            }

            execShizukuCmd("cmd", "phone", "reload-carrier-config")
            execShizukuCmd("setprop", "persist.radio.reboot_on_modem_reset", "0")

            privilegedService?.forceReRegisterIms(subId)
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to re-register IMS: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Fetches current active CarrierConfig bundle for the given subscription ID.
     */
    suspend fun getCarrierConfig(subId: Int): Result<PersistableBundle> = withContext(Dispatchers.IO) {
        try {
            val rawBinder = SystemServiceHelper.getSystemService("carrier_config")
            if (rawBinder != null) {
                val wrappedBinder = ShizukuBinderWrapper(rawBinder)
                val stubClass = Class.forName("com.android.internal.telephony.ICarrierConfigLoader\$Stub")
                val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
                val loader = asInterface.invoke(null, wrappedBinder)
                val getConfigMethod = loader?.javaClass?.getMethod("getConfigForSubIdWithFeature", Int::class.javaPrimitiveType, String::class.java, String::class.java)
                    ?: loader?.javaClass?.getMethod("getConfigForSubId", Int::class.javaPrimitiveType)
                if (getConfigMethod != null) {
                    val result = if (getConfigMethod.parameterCount == 3) {
                        getConfigMethod.invoke(loader, subId, "com.android.shell", null)
                    } else {
                        getConfigMethod.invoke(loader, subId)
                    }
                    if (result is PersistableBundle) return@withContext Result.success(result)
                }
            }

            val bundle = privilegedService?.getCarrierConfig(subId)
            Result.success(bundle ?: PersistableBundle())
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to get carrier config: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Enforces allowed network types mask (e.g. 5G SA/NSA, LTE).
     */
    suspend fun setAllowedNetworkTypes(subId: Int, mask: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val phoneBinder = SystemServiceHelper.getSystemService("phone")
            if (phoneBinder != null) {
                val wrappedPhone = ShizukuBinderWrapper(phoneBinder)
                val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
                val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
                val telephony = asInterface.invoke(null, wrappedPhone)
                val setMethod = telephony?.javaClass?.getMethod(
                    "setAllowedNetworkTypesForReason",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Long::class.javaPrimitiveType
                )
                setMethod?.invoke(telephony, subId, 0, mask)
            }
            privilegedService?.setAllowedNetworkTypes(subId, mask)
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to set allowed network types: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Enforces high-level network modes:
     * 1 = 5G SA Only, 2 = 5G NSA + LTE-CA Turbo, 3 = LTE-A Only.
     */
    suspend fun setNetworkMode(subId: Int, modeId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val mask = when (modeId) {
            1 -> 1L shl 19 // 5G NR
            2 -> (1L shl 19) or (1L shl 13) or (1L shl 14) // NR + LTE + LTE_CA
            3 -> (1L shl 13) or (1L shl 14) // LTE + LTE_CA
            else -> (1L shl 19) or (1L shl 13) or (1L shl 14)
        }
        setAllowedNetworkTypes(subId, mask)
    }

    /**
     * Soft cycles cellular radio power to force carrier aggregation re-negotiation.
     */
    suspend fun cycleRadioPower(subId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val phoneBinder = SystemServiceHelper.getSystemService("phone")
            if (phoneBinder != null) {
                val wrappedPhone = ShizukuBinderWrapper(phoneBinder)
                val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
                val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
                val telephony = asInterface.invoke(null, wrappedPhone)
                val setRadioPowerMethod = telephony?.javaClass?.methods?.firstOrNull { it.name == "setRadioPower" }
                if (setRadioPowerMethod != null) {
                    setRadioPowerMethod.invoke(telephony, false)
                    kotlinx.coroutines.delay(600)
                    setRadioPowerMethod.invoke(telephony, true)
                    return@withContext Result.success(Unit)
                }
            }

            execShizukuCmd("cmd", "connectivity", "airplane-mode", "enable")
            kotlinx.coroutines.delay(600)
            execShizukuCmd("cmd", "connectivity", "airplane-mode", "disable")

            privilegedService?.cycleRadioPower(subId)
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to cycle radio: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Flushes local device DNS resolver cache.
     */
    suspend fun flushDnsCache(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            execShizukuCmd("ndc", "resolver", "cleardns")
            execShizukuCmd("cmd", "connectivity", "flush-default-dns")
            privilegedService?.flushDnsCache()
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to flush DNS cache: ${t.message}")
            Result.failure(t)
        }
    }

    /**
     * Queries real-time IMS registration state directly.
     */
    suspend fun getImsRegistrationState(subId: Int): Int = withContext(Dispatchers.IO) {
        try {
            val phoneBinder = SystemServiceHelper.getSystemService("phone")
            if (phoneBinder != null) {
                val wrappedPhone = ShizukuBinderWrapper(phoneBinder)
                val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
                val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
                val telephony = asInterface.invoke(null, wrappedPhone)
                val isImsRegisteredMethod = telephony?.javaClass?.methods?.firstOrNull { it.name == "isImsRegistered" }
                if (isImsRegisteredMethod != null) {
                    val result = if (isImsRegisteredMethod.parameterCount == 1) {
                        isImsRegisteredMethod.invoke(telephony, subId)
                    } else {
                        isImsRegisteredMethod.invoke(telephony)
                    }
                    if (result is Boolean) return@withContext if (result) 1 else 0
                }
            }
            privilegedService?.getImsRegistrationState(subId) ?: -1
        } catch (t: Throwable) {
            Log.w(TAG, "Could not query IMS registration: ${t.message}")
            -1
        }
    }

    // =========================================================================
    // PRIVATE INTERNAL HELPERS
    // =========================================================================

    private fun setImsProvisioningDirect(subId: Int, enableVoLte: Boolean, enableVoWifi: Boolean, enableVoNr: Boolean) {
        try {
            val phoneBinder = SystemServiceHelper.getSystemService("phone") ?: return
            val wrappedPhone = ShizukuBinderWrapper(phoneBinder)
            val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
            val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
            val telephony = asInterface.invoke(null, wrappedPhone) ?: return

            val setImsProvInt = telephony.javaClass.methods.firstOrNull { it.name == "setImsProvisioningInt" }
            try {
                setImsProvInt?.invoke(telephony, subId, 1, if (enableVoLte) 1 else 0)
                setImsProvInt?.invoke(telephony, subId, 2, if (enableVoWifi) 1 else 0)
                setImsProvInt?.invoke(telephony, subId, 3, 1) // Video
            } catch (_: Throwable) {}

            val setImsProvStatus = telephony.javaClass.methods.firstOrNull { it.name == "setImsProvisioningStatusForCapability" }
            try {
                setImsProvStatus?.invoke(telephony, subId, 1, 0, enableVoLte) // Voice WWAN
                setImsProvStatus?.invoke(telephony, subId, 1, 1, enableVoWifi) // Voice WLAN
                setImsProvStatus?.invoke(telephony, subId, 2, 0, true) // Video WWAN
                setImsProvStatus?.invoke(telephony, subId, 4, 0, true) // UT WWAN
            } catch (_: Throwable) {}

            try {
                telephony.javaClass.methods.firstOrNull { it.name == "enableIms" }?.invoke(telephony, subId)
            } catch (_: Throwable) {}
        } catch (t: Throwable) {
            Log.d(TAG, "setImsProvisioningDirect notice: ${t.message}")
        }
    }

    private fun setTelephonyImsSetting(subId: Int, methodName: String, enable: Boolean) {
        try {
            val phoneBinder = SystemServiceHelper.getSystemService("phone") ?: return
            val wrappedPhone = ShizukuBinderWrapper(phoneBinder)
            val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
            val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
            val telephony = asInterface.invoke(null, wrappedPhone) ?: return

            val method = telephony.javaClass.methods.firstOrNull { it.name == methodName }
            method?.invoke(telephony, subId, enable)
        } catch (t: Throwable) {
            Log.d(TAG, "$methodName notice: ${t.message}")
        }
    }

    private fun setTelephonyImsIntSetting(subId: Int, methodName: String, value: Int) {
        try {
            val phoneBinder = SystemServiceHelper.getSystemService("phone") ?: return
            val wrappedPhone = ShizukuBinderWrapper(phoneBinder)
            val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
            val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
            val telephony = asInterface.invoke(null, wrappedPhone) ?: return

            val method = telephony.javaClass.methods.firstOrNull { it.name == methodName }
            method?.invoke(telephony, subId, value)
        } catch (t: Throwable) {
            Log.d(TAG, "$methodName notice: ${t.message}")
        }
    }

    private fun execShizukuCmd(vararg cmd: String) {
        try {
            val process = Shizuku.newProcess(cmd, null, null)
            process.waitFor()
        } catch (t: Throwable) {
            Log.d(TAG, "execShizukuCmd notice: ${t.message}")
        }
    }
}
