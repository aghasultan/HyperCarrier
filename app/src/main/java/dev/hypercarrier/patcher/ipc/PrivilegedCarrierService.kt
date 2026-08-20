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
 * Performs rootless, reboot-persistent CarrierConfig disk overrides, network mode enforcement,
 * and low-level radio power cycling.
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
     * Persists across reboots by writing directly to disk (/data/user_de/0/com.android.phone/files/carrierconfig-*.xml).
     */
    @SuppressLint("DiscouragedPrivateApi")
    override fun applyPersistentConfig(subId: Int, bundle: PersistableBundle?) {
        Log.i(TAG, "applyPersistentConfig called for subId=$subId with ${bundle?.size() ?: 0} keys")
        try {
            var applied = false

            // Strategy 1: Reflection on CarrierConfigManager instance
            try {
                val context = getSystemOrAppContext()
                if (context != null) {
                    val ccm = context.getSystemService(Context.CARRIER_CONFIG_SERVICE) as? CarrierConfigManager
                    if (ccm != null) {
                        val method = CarrierConfigManager::class.java.getMethod(
                            "overrideConfig",
                            Int::class.javaPrimitiveType,
                            PersistableBundle::class.java,
                            Boolean::class.javaPrimitiveType
                        )
                        method.isAccessible = true
                        method.invoke(ccm, subId, bundle, true)
                        applied = true
                        Log.i(TAG, "Successfully invoked CarrierConfigManager.overrideConfig(subId=$subId, persistent=true) via Context")
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Strategy 1 failed, trying ServiceManager: ${e.message}")
            }

            // Strategy 2: Direct binder IPC to ICarrierConfigLoader via ServiceManager
            if (!applied) {
                val serviceManagerClass = Class.forName("android.os.ServiceManager")
                val getServiceMethod: Method = serviceManagerClass.getMethod("getService", String::class.java)
                val binder = getServiceMethod.invoke(null, "carrier_config") as? IBinder
                    ?: throw IllegalStateException("carrier_config binder service not found")

                val stubClass = Class.forName("com.android.internal.telephony.ICarrierConfigLoader\$Stub")
                val asInterfaceMethod = stubClass.getMethod("asInterface", IBinder::class.java)
                val loader = asInterfaceMethod.invoke(null, binder)
                    ?: throw IllegalStateException("ICarrierConfigLoader interface is null")

                val overrideMethod = loader.javaClass.getMethod(
                    "overrideConfig",
                    Int::class.javaPrimitiveType,
                    PersistableBundle::class.java,
                    Boolean::class.javaPrimitiveType
                )
                overrideMethod.isAccessible = true
                overrideMethod.invoke(loader, subId, bundle, true)
                Log.i(TAG, "Successfully invoked ICarrierConfigLoader.overrideConfig(subId=$subId, persistent=true) via ServiceManager")
            }
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

            // Fallback via ServiceManager
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
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
            val binder = getServiceMethod.invoke(null, "phone") as? IBinder
            if (binder != null) {
                val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
                val asInterfaceMethod = stubClass.getMethod("asInterface", IBinder::class.java)
                val telephony = asInterfaceMethod.invoke(null, binder)
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
                    // Reason 0 = ALLOWED_NETWORK_TYPES_REASON_USER
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

            // Fallback via ITelephony binder
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
            val binder = getServiceMethod.invoke(null, "phone") as? IBinder
            if (binder != null) {
                val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
                val asInterfaceMethod = stubClass.getMethod("asInterface", IBinder::class.java)
                val telephony = asInterfaceMethod.invoke(null, binder)
                val setMethod = telephony?.javaClass?.getMethod(
                    "setAllowedNetworkTypesForReason",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Long::class.javaPrimitiveType
                )
                setMethod?.invoke(telephony, subId, 0, mask)
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
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
            val binder = getServiceMethod.invoke(null, "phone") as? IBinder
            if (binder != null) {
                val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
                val asInterfaceMethod = stubClass.getMethod("asInterface", IBinder::class.java)
                val telephony = asInterfaceMethod.invoke(null, binder)
                val setRadioPowerMethod = telephony?.javaClass?.methods?.firstOrNull { it.name == "setRadioPower" }

                if (setRadioPowerMethod != null) {
                    setRadioPowerMethod.invoke(telephony, false)
                    Thread.sleep(600)
                    setRadioPowerMethod.invoke(telephony, true)
                    Log.i(TAG, "Successfully cycled radio power via ITelephony")
                    return
                }
            }

            // Fallback via shell cmd
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
