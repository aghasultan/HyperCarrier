package dev.hypercarrier.patcher.ipc

import android.annotation.SuppressLint
import android.content.Context
import android.os.IBinder
import android.os.PersistableBundle
import android.os.Process
import android.telephony.CarrierConfigManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.Keep
import dev.hypercarrier.patcher.IPrivilegedCarrierService
import java.lang.reflect.Method
import kotlin.system.exitProcess

/**
 * Privileged carrier service executed under UID 2000 (com.android.shell) via Shizuku IPC.
 * Performs rootless, reboot-persistent CarrierConfig disk overrides, direct ImsMmTelManager &
 * ImsProvisioningManager configuration, low-level network mode enforcement, and radio power cycling.
 */
@Keep
class PrivilegedCarrierService : IPrivilegedCarrierService.Stub {

    companion object {
        private const val TAG = "PrivilegedCarrierSvc"

        // Bitmasks for Network Types (Android Telephony standard constants)
        private const val NETWORK_TYPE_BITMASK_LTE = 1L shl 13
        private const val NETWORK_TYPE_BITMASK_NR = 1L shl 19
        private const val NETWORK_TYPE_BITMASK_LTE_CA = 1L shl 14
        private const val NETWORK_TYPE_BITMASK_UMTS = 1L shl 3
        private const val NETWORK_TYPE_BITMASK_HSPA = 1L shl 10
        private const val NETWORK_TYPE_BITMASK_HSPAP = 1L shl 15
    }

    /**
     * Default constructor required for Shizuku UserService instantiation.
     */
    constructor() : super() {
        Log.i(TAG, "PrivilegedCarrierService initialized under UID: ${Process.myUid()}, PID: ${Process.myPid()}")
    }

    /**
     * Constructor accepting Context for in-process testing.
     */
    @Suppress("UNUSED_PARAMETER")
    constructor(context: Context) : super() {
        Log.i(TAG, "PrivilegedCarrierService initialized with Context under UID: ${Process.myUid()}")
    }

    /**
     * Injects a persistent CarrierConfig override bundle for the given subscription ID.
     */
    @SuppressLint("DiscouragedPrivateApi")
    override fun applyPersistentConfig(subId: Int, bundle: PersistableBundle?) {
        Log.i(TAG, "applyPersistentConfig called for subId=$subId with ${bundle?.size() ?: 0} keys")
        try {
            var applied = false

            // Strategy 1: Reflection on CarrierConfigManager instance (Persistent + In-Memory)
            try {
                val context = getSystemOrAppContext()
                if (context != null) {
                    val ccm = context.getSystemService(Context.CARRIER_CONFIG_SERVICE) as? CarrierConfigManager
                    if (ccm != null) {
                        val overrideMethod = CarrierConfigManager::class.java.getMethod(
                            "overrideConfig",
                            Int::class.javaPrimitiveType,
                            PersistableBundle::class.java,
                            Boolean::class.javaPrimitiveType
                        )
                        overrideMethod.isAccessible = true
                        
                        try {
                            overrideMethod.invoke(ccm, subId, bundle, true)
                        } catch (e: Throwable) {
                            Log.w(TAG, "Persistent override call: ${e.message}")
                        }

                        try {
                            overrideMethod.invoke(ccm, subId, bundle, false)
                        } catch (e: Throwable) {
                            Log.w(TAG, "In-memory override call: ${e.message}")
                        }

                        applied = true
                        Log.i(TAG, "Successfully invoked CarrierConfigManager.overrideConfig via Context")
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Strategy 1 failed, trying ServiceManager: ${e.message}")
            }

            // Strategy 2: Direct binder IPC to ICarrierConfigLoader via ServiceManager
            if (!applied) {
                try {
                    val serviceManagerClass = Class.forName("android.os.ServiceManager")
                    val getServiceMethod: Method = serviceManagerClass.getMethod("getService", String::class.java)
                    val binder = getServiceMethod.invoke(null, "carrier_config") as? IBinder
                    if (binder != null) {
                        val stubClass = Class.forName("com.android.internal.telephony.ICarrierConfigLoader\$Stub")
                        val asInterfaceMethod = stubClass.getMethod("asInterface", IBinder::class.java)
                        val loader = asInterfaceMethod.invoke(null, binder)

                        val overrideMethod = loader?.javaClass?.getMethod(
                            "overrideConfig",
                            Int::class.javaPrimitiveType,
                            PersistableBundle::class.java,
                            Boolean::class.javaPrimitiveType
                        )
                        overrideMethod?.isAccessible = true
                        try {
                            overrideMethod?.invoke(loader, subId, bundle, true)
                        } catch (_: Throwable) {}
                        try {
                            overrideMethod?.invoke(loader, subId, bundle, false)
                        } catch (_: Throwable) {}
                        applied = true
                        Log.i(TAG, "Successfully invoked ICarrierConfigLoader.overrideConfig via ServiceManager")
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Strategy 2 error: ${e.message}")
                }
            }

            // Automatically provision IMS flags and triggers
            setImsProvisioning(subId, true, true, true)

            // Shell reload triggers to wake telephony daemon
            try {
                Runtime.getRuntime().exec("cmd phone reload-carrier-config").waitFor()
            } catch (_: Throwable) {}

        } catch (t: Throwable) {
            Log.e(TAG, "Failed to apply persistent CarrierConfig for subId=$subId", t)
            throw RuntimeException("Failed to apply persistent CarrierConfig: ${t.message}", t)
        }
    }

    /**
     * Clears all CarrierConfig overrides for the given subscription ID, restoring OEM/Carrier defaults.
     */
    override fun clearConfigOverride(subId: Int) {
        Log.i(TAG, "clearConfigOverride called for subId=$subId")
        applyPersistentConfig(subId, null)
    }

    /**
     * Granular 1-Tap Toggle: Voice over LTE (VoLTE / 4G Calling).
     */
    override fun setVoLteEnabled(subId: Int, enable: Boolean) {
        Log.i(TAG, "setVoLteEnabled: subId=$subId, enable=$enable")
        try {
            // 1. ImsMmTelManager
            val context = getSystemOrAppContext()
            if (context != null) {
                try {
                    val imsManager = context.getSystemService(android.telephony.ims.ImsManager::class.java)
                    val mmTelManager = imsManager?.getImsMmTelManager(subId)
                    val setMethod = mmTelManager?.javaClass?.getMethod("setAdvancedCallingSettingEnabled", Boolean::class.javaPrimitiveType)
                    setMethod?.isAccessible = true
                    setMethod?.invoke(mmTelManager, enable)
                    Log.i(TAG, "Invoked ImsMmTelManager.setAdvancedCallingSettingEnabled($enable)")
                } catch (e: Throwable) {
                    Log.d(TAG, "ImsMmTelManager call note: ${e.message}")
                }

                // 2. ImsProvisioningManager
                try {
                    val provClass = Class.forName("android.telephony.ims.ImsProvisioningManager")
                    val createMethod = provClass.getMethod("createForSubscriptionId", Int::class.javaPrimitiveType)
                    val provManager = createMethod.invoke(null, subId)
                    val setProvIntMethod = provClass.getMethod("setProvisioningIntValue", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                    setProvIntMethod.invoke(provManager, 10, if (enable) 1 else 0) // KEY_VOIMS_OPT_IN_STATUS
                    setProvIntMethod.invoke(provManager, 0, if (enable) 1 else 0)  // KEY_VOICE_OVER_LTE_ENABLED

                    val setProvStatusMethod = provClass.getMethod("setProvisioningStatusForCapability", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType)
                    setProvStatusMethod.invoke(provManager, 1, 1, enable) // VOICE over WWAN
                    Log.i(TAG, "Invoked ImsProvisioningManager for VoLTE")
                } catch (e: Throwable) {
                    Log.d(TAG, "ImsProvisioningManager call note: ${e.message}")
                }
            }

            // 3. ITelephony
            val telephony = getITelephony()
            if (telephony != null) {
                val setVolte = telephony.javaClass.methods.firstOrNull { it.name == "setEnhanced4gLteModeSetting" }
                try {
                    setVolte?.invoke(telephony, subId, enable)
                } catch (_: Throwable) {}

                val setImsProvInt = telephony.javaClass.methods.firstOrNull { it.name == "setImsProvisioningInt" }
                try {
                    setImsProvInt?.invoke(telephony, subId, 1, if (enable) 1 else 0)
                } catch (_: Throwable) {}

                val setImsProvStatus = telephony.javaClass.methods.firstOrNull { it.name == "setImsProvisioningStatusForCapability" }
                try {
                    setImsProvStatus?.invoke(telephony, subId, 1, 0, enable)
                } catch (_: Throwable) {}
            }

            // 4. Overrides Bundle
            val bundle = PersistableBundle().apply {
                putBoolean("carrier_volte_available_bool", enable)
                putBoolean("carrier_volte_provisioned_bool", enable)
                putBoolean("enhanced_4g_lte_on_by_default_bool", enable)
                putBoolean("editable_enhanced_4g_lte_bool", true)
                putBoolean("hide_enhanced_4g_lte_bool", false)
                putBoolean("carrier_volte_tty_supported_bool", true)
            }
            applyPersistentConfig(subId, bundle)

            // 5. System Properties
            Runtime.getRuntime().exec(arrayOf("setprop", "persist.vendor.radio.volte_enabled", if (enable) "1" else "0")).waitFor()
            Runtime.getRuntime().exec(arrayOf("setprop", "persist.radio.volte_state", if (enable) "1" else "0")).waitFor()

        } catch (t: Throwable) {
            Log.e(TAG, "Error in setVoLteEnabled: ${t.message}", t)
        }
    }

    /**
     * Granular 1-Tap Toggle: Voice over Wi-Fi (VoWiFi / Wi-Fi Calling).
     */
    override fun setVoWifiEnabled(subId: Int, enable: Boolean) {
        Log.i(TAG, "setVoWifiEnabled: subId=$subId, enable=$enable")
        try {
            val context = getSystemOrAppContext()
            if (context != null) {
                try {
                    val imsManager = context.getSystemService(android.telephony.ims.ImsManager::class.java)
                    val mmTelManager = imsManager?.getImsMmTelManager(subId)
                    val setWfc = mmTelManager?.javaClass?.getMethod("setVoWiFiSettingEnabled", Boolean::class.javaPrimitiveType)
                    setWfc?.isAccessible = true
                    setWfc?.invoke(mmTelManager, enable)

                    val setWfcRoaming = mmTelManager?.javaClass?.getMethod("setVoWiFiRoamingSettingEnabled", Boolean::class.javaPrimitiveType)
                    setWfcRoaming?.isAccessible = true
                    setWfcRoaming?.invoke(mmTelManager, enable)
                    Log.i(TAG, "Invoked ImsMmTelManager.setVoWiFiSettingEnabled($enable)")
                } catch (e: Throwable) {
                    Log.d(TAG, "ImsMmTelManager VoWiFi note: ${e.message}")
                }

                try {
                    val provClass = Class.forName("android.telephony.ims.ImsProvisioningManager")
                    val createMethod = provClass.getMethod("createForSubscriptionId", Int::class.javaPrimitiveType)
                    val provManager = createMethod.invoke(null, subId)
                    val setProvStatusMethod = provClass.getMethod("setProvisioningStatusForCapability", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType)
                    setProvStatusMethod.invoke(provManager, 1, 2, enable) // VOICE over WLAN
                    Log.i(TAG, "Invoked ImsProvisioningManager for VoWiFi")
                } catch (e: Throwable) {
                    Log.d(TAG, "ImsProvisioningManager VoWiFi note: ${e.message}")
                }
            }

            val telephony = getITelephony()
            if (telephony != null) {
                val setWfc = telephony.javaClass.methods.firstOrNull { it.name == "setVoWiFiSetting" }
                try {
                    setWfc?.invoke(telephony, subId, enable)
                } catch (_: Throwable) {}

                val setImsProvInt = telephony.javaClass.methods.firstOrNull { it.name == "setImsProvisioningInt" }
                try {
                    setImsProvInt?.invoke(telephony, subId, 2, if (enable) 1 else 0)
                } catch (_: Throwable) {}
            }

            val bundle = PersistableBundle().apply {
                putBoolean("carrier_wfc_ims_available_bool", enable)
                putBoolean("carrier_default_wfc_ims_enabled_bool", enable)
                putBoolean("carrier_default_wfc_ims_roaming_enabled_bool", enable)
                putBoolean("editable_wfc_mode_bool", true)
                putBoolean("editable_wfc_roaming_mode_bool", true)
                putBoolean("carrier_wfc_supports_wifi_only_bool", true)
            }
            applyPersistentConfig(subId, bundle)

            Runtime.getRuntime().exec(arrayOf("setprop", "persist.vendor.radio.vowifi_enabled", if (enable) "1" else "0")).waitFor()
            Runtime.getRuntime().exec(arrayOf("setprop", "persist.radio.vowifi_state", if (enable) "1" else "0")).waitFor()

        } catch (t: Throwable) {
            Log.e(TAG, "Error in setVoWifiEnabled: ${t.message}", t)
        }
    }

    /**
     * Granular 1-Tap Toggle: Voice over New Radio (5G VoNR).
     */
    override fun setVoNrEnabled(subId: Int, enable: Boolean) {
        Log.i(TAG, "setVoNrEnabled: subId=$subId, enable=$enable")
        try {
            val bundle = PersistableBundle().apply {
                putBoolean("vonr_enabled_bool", enable)
                putBoolean("vonr_setting_visibility_bool", true)
                putBoolean("nr_timers_reset_on_voice_qos_bool", true)
            }
            applyPersistentConfig(subId, bundle)

            Runtime.getRuntime().exec(arrayOf("setprop", "persist.vendor.radio.vonr_enabled", if (enable) "1" else "0")).waitFor()
        } catch (t: Throwable) {
            Log.e(TAG, "Error in setVoNrEnabled: ${t.message}", t)
        }
    }

    /**
     * Granular 1-Tap Toggle: Carrier Video Calling (ViLTE).
     */
    override fun setViLteEnabled(subId: Int, enable: Boolean) {
        Log.i(TAG, "setViLteEnabled: subId=$subId, enable=$enable")
        try {
            val context = getSystemOrAppContext()
            if (context != null) {
                try {
                    val imsManager = context.getSystemService(android.telephony.ims.ImsManager::class.java)
                    val mmTelManager = imsManager?.getImsMmTelManager(subId)
                    val setVt = mmTelManager?.javaClass?.getMethod("setVtSettingEnabled", Boolean::class.javaPrimitiveType)
                    setVt?.isAccessible = true
                    setVt?.invoke(mmTelManager, enable)
                } catch (_: Throwable) {}
            }

            val telephony = getITelephony()
            if (telephony != null) {
                val setVt = telephony.javaClass.methods.firstOrNull { it.name == "setVtSetting" }
                try {
                    setVt?.invoke(telephony, subId, enable)
                } catch (_: Throwable) {}
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error in setViLteEnabled: ${t.message}", t)
        }
    }

    /**
     * Granular Setting: Wi-Fi Calling Mode (1 = Wi-Fi Preferred, 2 = Cellular Preferred).
     */
    override fun setVoWifiMode(subId: Int, mode: Int) {
        Log.i(TAG, "setVoWifiMode: subId=$subId, mode=$mode")
        try {
            val context = getSystemOrAppContext()
            if (context != null) {
                try {
                    val imsManager = context.getSystemService(android.telephony.ims.ImsManager::class.java)
                    val mmTelManager = imsManager?.getImsMmTelManager(subId)
                    val setMode = mmTelManager?.javaClass?.getMethod("setVoWiFiModeSetting", Int::class.javaPrimitiveType)
                    setMode?.isAccessible = true
                    setMode?.invoke(mmTelManager, mode)
                } catch (_: Throwable) {}
            }

            val telephony = getITelephony()
            if (telephony != null) {
                val setMode = telephony.javaClass.methods.firstOrNull { it.name == "setVoWiFiModeSetting" }
                try {
                    setMode?.invoke(telephony, subId, mode)
                } catch (_: Throwable) {}
            }

            val bundle = PersistableBundle().apply {
                putInt("carrier_default_wfc_ims_mode_int", mode)
                putInt("carrier_default_wfc_ims_roaming_mode_int", mode)
            }
            applyPersistentConfig(subId, bundle)
        } catch (t: Throwable) {
            Log.e(TAG, "Error in setVoWifiMode: ${t.message}", t)
        }
    }

    /**
     * Forces immediate IMS deregistration and re-registration trigger on modem.
     */
    override fun forceReRegisterIms(subId: Int) {
        Log.i(TAG, "forceReRegisterIms called for subId=$subId")
        try {
            val telephony = getITelephony()
            if (telephony != null) {
                val reconnectIms = telephony.javaClass.methods.firstOrNull { it.name == "reconnectIms" }
                try {
                    reconnectIms?.invoke(telephony)
                } catch (_: Throwable) {}

                val enableIms = telephony.javaClass.methods.firstOrNull { it.name == "enableIms" }
                try {
                    enableIms?.invoke(telephony, subId)
                } catch (_: Throwable) {}
            }

            // Shell triggers to force telephony daemon reload
            Runtime.getRuntime().exec("cmd phone reload-carrier-config").waitFor()
            Runtime.getRuntime().exec("setprop persist.radio.reboot_on_modem_reset 0").waitFor()
        } catch (t: Throwable) {
            Log.e(TAG, "Error in forceReRegisterIms: ${t.message}", t)
        }
    }

    /**
     * Directly provisions IMS capabilities (VoLTE, VoWiFi, VoNR, Video, UT) via ITelephony & IImsConfig.
     */
    override fun setImsProvisioning(subId: Int, enableVoLte: Boolean, enableVoWifi: Boolean, enableVoNr: Boolean) {
        setVoLteEnabled(subId, enableVoLte)
        setVoWifiEnabled(subId, enableVoWifi)
        setVoNrEnabled(subId, enableVoNr)
    }

    /**
     * Retrieves the active CarrierConfig bundle for the given subscription ID.
     */
    override fun getCarrierConfig(subId: Int): PersistableBundle {
        try {
            val context = getSystemOrAppContext()
            if (context != null) {
                val ccm = context.getSystemService(Context.CARRIER_CONFIG_SERVICE) as? CarrierConfigManager
                val config = ccm?.getConfigForSubId(subId)
                if (config != null) return config
            }

            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
            val binder = getServiceMethod.invoke(null, "carrier_config") as? IBinder
            if (binder != null) {
                val stubClass = Class.forName("com.android.internal.telephony.ICarrierConfigLoader\$Stub")
                val asInterfaceMethod = stubClass.getMethod("asInterface", IBinder::class.java)
                val loader = asInterfaceMethod.invoke(null, binder)
                val getConfigMethod = loader?.javaClass?.getMethod("getConfigForSubIdWithFeature", Int::class.javaPrimitiveType, String::class.java, String::class.java)
                    ?: loader?.javaClass?.getMethod("getConfigForSubId", Int::class.javaPrimitiveType)
                if (getConfigMethod != null) {
                    val result = if (getConfigMethod.parameterCount == 3) {
                        getConfigMethod.invoke(loader, subId, "com.android.shell", null)
                    } else {
                        getConfigMethod.invoke(loader, subId)
                    }
                    if (result is PersistableBundle) return result
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error fetching CarrierConfig for subId=$subId", t)
        }
        return PersistableBundle()
    }

    /**
     * Queries real-time IMS registration state for the given subscription ID.
     * Returns: 1 if registered, 0 if unregistered, -1 on error.
     */
    override fun getImsRegistrationState(subId: Int): Int {
        try {
            val telephony = getITelephony()
            if (telephony != null) {
                val isImsRegisteredMethod = telephony.javaClass.methods.firstOrNull { it.name == "isImsRegistered" }
                if (isImsRegisteredMethod != null) {
                    val result = if (isImsRegisteredMethod.parameterCount == 1) {
                        isImsRegisteredMethod.invoke(telephony, subId)
                    } else {
                        isImsRegisteredMethod.invoke(telephony)
                    }
                    if (result is Boolean) {
                        return if (result) 1 else 0
                    }
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Could not query IMS registration via ITelephony: ${t.message}")
        }
        return -1
    }

    /**
     * Enforces allowed network types mask (e.g. 5G SA/NSA, LTE, etc.).
     */
    @SuppressLint("DiscouragedPrivateApi")
    override fun setAllowedNetworkTypes(subId: Int, mask: Long) {
        Log.i(TAG, "setAllowedNetworkTypes called for subId=$subId, mask=$mask")
        try {
            val context = getSystemOrAppContext()
            if (context != null) {
                val tm = (context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager)
                    ?.createForSubscriptionId(subId)
                if (tm != null) {
                    val method = TelephonyManager::class.java.getMethod(
                        "setAllowedNetworkTypesForReason",
                        Int::class.javaPrimitiveType,
                        Long::class.javaPrimitiveType
                    )
                    method.isAccessible = true
                    method.invoke(tm, 0, mask)
                    Log.i(TAG, "Successfully set allowed network types on TelephonyManager")
                    return
                }
            }

            val telephony = getITelephony()
            if (telephony != null) {
                val setMethod = telephony.javaClass.getMethod(
                    "setAllowedNetworkTypesForReason",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Long::class.javaPrimitiveType
                )
                setMethod.invoke(telephony, subId, 0, mask)
                Log.i(TAG, "Successfully set allowed network types via ITelephony binder")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to set allowed network types: ${t.message}", t)
            throw RuntimeException("Failed to set allowed network types: ${t.message}", t)
        }
    }

    /**
     * Enforces high-level network modes:
     * 1 = 5G Standalone (SA) Only (NR only)
     * 2 = 5G NSA + LTE-CA Turbo (NR + LTE + LTE_CA)
     * 3 = LTE-A Only (LTE + LTE_CA)
     */
    override fun setNetworkMode(subId: Int, mode: Int) {
        Log.i(TAG, "setNetworkMode called for subId=$subId, mode=$mode")
        val mask = when (mode) {
            1 -> NETWORK_TYPE_BITMASK_NR // 5G SA Only
            2 -> NETWORK_TYPE_BITMASK_NR or NETWORK_TYPE_BITMASK_LTE or NETWORK_TYPE_BITMASK_LTE_CA // 5G NSA + LTE-CA Turbo
            3 -> NETWORK_TYPE_BITMASK_LTE or NETWORK_TYPE_BITMASK_LTE_CA // LTE-A Only
            else -> NETWORK_TYPE_BITMASK_NR or NETWORK_TYPE_BITMASK_LTE or NETWORK_TYPE_BITMASK_LTE_CA or NETWORK_TYPE_BITMASK_HSPAP or NETWORK_TYPE_BITMASK_HSPA or NETWORK_TYPE_BITMASK_UMTS
        }
        setAllowedNetworkTypes(subId, mask)
    }

    /**
     * Soft cycles radio power to force cellular carrier aggregation re-negotiation.
     */
    override fun cycleRadioPower(subId: Int) {
        Log.i(TAG, "cycleRadioPower called for subId=$subId")
        try {
            val telephony = getITelephony()
            if (telephony != null) {
                val setRadioPowerMethod = telephony.javaClass.methods.firstOrNull { it.name == "setRadioPower" }
                if (setRadioPowerMethod != null) {
                    setRadioPowerMethod.invoke(telephony, false)
                    Thread.sleep(600)
                    setRadioPowerMethod.invoke(telephony, true)
                    Log.i(TAG, "Successfully cycled radio power via ITelephony")
                    return
                }
            }

            Runtime.getRuntime().exec("cmd connectivity airplane-mode enable").waitFor()
            Thread.sleep(600)
            Runtime.getRuntime().exec("cmd connectivity airplane-mode disable").waitFor()
            Log.i(TAG, "Successfully cycled radio via airplane-mode command")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to cycle radio power", t)
        }
    }

    /**
     * Flushes local device DNS resolver cache.
     */
    override fun flushDnsCache() {
        Log.i(TAG, "flushDnsCache called")
        try {
            Runtime.getRuntime().exec("ndc resolver cleardns").waitFor()
            Runtime.getRuntime().exec("cmd connectivity flush-default-dns").waitFor()
            Log.i(TAG, "DNS resolver cache flushed successfully")
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to flush DNS cache: ${t.message}")
        }
    }

    /**
     * Returns true when executing in Shell UID 2000.
     */
    override fun isPrivileged(): Boolean {
        return Process.myUid() == 2000 || Process.myUid() == 0
    }

    /**
     * Terminate the privileged user service process cleanly.
     */
    override fun destroy() {
        Log.i(TAG, "PrivilegedCarrierService destroy() called. Terminating process.")
        exitProcess(0)
    }

    private fun getITelephony(): Any? {
        return try {
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
            val binder = getServiceMethod.invoke(null, "phone") as? IBinder
            if (binder != null) {
                val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
                val asInterfaceMethod = stubClass.getMethod("asInterface", IBinder::class.java)
                asInterfaceMethod.invoke(null, binder)
            } else null
        } catch (t: Throwable) {
            Log.w(TAG, "Could not get ITelephony: ${t.message}")
            null
        }
    }

    /**
     * Helper to retrieve system Context when running as a Shizuku UserService.
     */
    private fun getSystemOrAppContext(): Context? {
        return try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentActivityThreadMethod = activityThreadClass.getMethod("currentActivityThread")
            val activityThread = currentActivityThreadMethod.invoke(null)
            val getSystemContextMethod = activityThreadClass.getMethod("getSystemContext")
            getSystemContextMethod.invoke(activityThread) as? Context
        } catch (t: Throwable) {
            Log.w(TAG, "Could not obtain SystemContext: ${t.message}")
            null
        }
    }
}
