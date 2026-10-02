package dev.hypercarrier.patcher.ipc

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.IActivityManager
import android.app.UiAutomationConnection
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.IInterface
import android.os.PersistableBundle
import android.os.ServiceManager
import android.telephony.CarrierConfigManager
import android.telephony.TelephonyFrameworkInitializer
import android.telephony.ims.ProvisioningManager
import android.util.Log
import com.android.internal.telephony.ICarrierConfigLoader
import com.android.internal.telephony.ISub
import com.android.internal.telephony.ITelephony
import dev.hypercarrier.patcher.HyperCarrierApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper

/**
 * HyperCarrierEngine is the unified privileged subsystem orchestrator.
 * It coordinates:
 * 1. Broker Instrumentation CarrierConfig Injection with Delegated Shell Permission Identity.
 * 2. Low-level ITelephony IMS SIP stack reset (resetIms) and VoIMS provisioning (setImsProvisioningInt).
 * 3. Real-time IMS registration verification directly from the hardware baseband.
 */
object HyperCarrierEngine {

    private const val TAG = "HyperCarrierEngine"

    // Cache internal binder stubs
    private val interfaceCache = HashMap<String, IInterface>()

    private inline fun <reified T : IInterface> getCachedInterface(key: String, loader: () -> T?): T? {
        interfaceCache[key]?.let { return it as? T }
        val loaded = loader()
        if (loaded != null) {
            interfaceCache[key] = loaded
        }
        return loaded
    }

    private fun getCarrierConfigLoader(): ICarrierConfigLoader? = getCachedInterface("ICarrierConfigLoader") {
        try {
            val binder: IBinder? = try {
                TelephonyFrameworkInitializer
                    .getTelephonyServiceManager()
                    .carrierConfigServiceRegisterer
                    .get()
            } catch (_: Throwable) {
                ServiceManager.getService(Context.CARRIER_CONFIG_SERVICE)
            }
            binder?.let { ICarrierConfigLoader.Stub.asInterface(ShizukuBinderWrapper(it)) }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to bind ICarrierConfigLoader: ${t.message}")
            null
        }
    }

    private fun getTelephony(): ITelephony? = getCachedInterface("ITelephony") {
        try {
            val binder: IBinder? = try {
                TelephonyFrameworkInitializer
                    .getTelephonyServiceManager()
                    .telephonyServiceRegisterer
                    .get()
            } catch (_: Throwable) {
                ServiceManager.getService(Context.TELEPHONY_SERVICE)
            }
            binder?.let { ITelephony.Stub.asInterface(ShizukuBinderWrapper(it)) }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to bind ITelephony: ${t.message}")
            null
        }
    }

    private fun getISub(): ISub? = getCachedInterface("ISub") {
        try {
            val binder: IBinder? = try {
                TelephonyFrameworkInitializer
                    .getTelephonyServiceManager()
                    .subscriptionServiceRegisterer
                    .get()
            } catch (_: Throwable) {
                ServiceManager.getService("isub")
            }
            binder?.let { ISub.Stub.asInterface(ShizukuBinderWrapper(it)) }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to bind ISub: ${t.message}")
            null
        }
    }

    private fun getActivityManager(): IActivityManager? = getCachedInterface("IActivityManager") {
        try {
            val binder = ServiceManager.getService(Context.ACTIVITY_SERVICE)
            binder?.let { IActivityManager.Stub.asInterface(ShizukuBinderWrapper(it)) }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to bind IActivityManager: ${t.message}")
            null
        }
    }

    /**
     * Injects CarrierConfig bundle into the Android framework via Broker Instrumentation
     * under Delegated Shell Permission Identity.
     */
    suspend fun overrideConfig(
        context: Context,
        subId: Int,
        overrides: PersistableBundle?,
        persistent: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!Shizuku.pingBinder()) {
            return@withContext Result.failure(IllegalStateException("Shizuku is not running or connected."))
        }

        try {
            val am = getActivityManager()
                ?: return@withContext Result.failure(IllegalStateException("IActivityManager binder unavailable"))

            val args = Bundle().apply {
                putInt("subId", subId)
                if (overrides != null) {
                    putParcelable("overrides", overrides)
                    putBoolean("clear", false)
                } else {
                    putBoolean("clear", true)
                }
                putBoolean("persistent", persistent)
            }

            Log.i(TAG, "Starting HyperInstrumentation for subId=$subId, keys=${overrides?.size() ?: 0}")

            am.startInstrumentation(
                ComponentName(context, HyperInstrumentation::class.java),
                null,
                ActivityManager.INSTR_FLAG_NO_RESTART,
                args,
                null,
                UiAutomationConnection(),
                0,
                null
            )

            // Direct fallback: Also invoke ICarrierConfigLoader in-memory
            try {
                val loader = getCarrierConfigLoader()
                val overrideMethod = loader?.javaClass?.methods?.firstOrNull {
                    it.name == "overrideConfig" && (it.parameterCount == 3 || it.parameterCount == 2)
                }
                overrideMethod?.isAccessible = true
                if (overrideMethod != null && loader != null) {
                    if (overrideMethod.parameterCount == 3) {
                        overrideMethod.invoke(loader, subId, overrides, false)
                    } else {
                        overrideMethod.invoke(loader, subId, overrides)
                    }
                }
            } catch (_: Throwable) {}

            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "overrideConfig failed: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Clears all CarrierConfig overrides for the given SIM, restoring OEM defaults.
     */
    suspend fun clearConfig(context: Context, subId: Int): Result<Unit> {
        return overrideConfig(context, subId, null, persistent = false)
    }

    /**
     * Provisions VoIMS opt-in status (VoLTE / IMS voice calling) via ITelephony.setImsProvisioningInt.
     */
    suspend fun provisionVoIms(subId: Int, enable: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val telephony = getTelephony()
                ?: return@withContext Result.failure(IllegalStateException("ITelephony unavailable"))

            val value = if (enable) {
                ProvisioningManager.PROVISIONING_VALUE_ENABLED
            } else {
                ProvisioningManager.PROVISIONING_VALUE_DISABLED
            }

            // Key 10 is ProvisioningManager.KEY_VOIMS_OPT_IN_STATUS
            val key = ProvisioningManager.KEY_VOIMS_OPT_IN_STATUS
            val result = telephony.setImsProvisioningInt(subId, key, value)
            Log.i(TAG, "telephony.setImsProvisioningInt(subId=$subId, key=$key, value=$value) returned $result")
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "provisionVoIms failed: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Resets the hardware modem's IMS registration state, forcing immediate re-registration.
     */
    suspend fun resetIms(subId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val telephony = getTelephony()
                ?: return@withContext Result.failure(IllegalStateException("ITelephony unavailable"))
            val sub = getISub()
                ?: return@withContext Result.failure(IllegalStateException("ISub unavailable"))

            val slotIndex = sub.getSlotIndex(subId)
            telephony.resetIms(slotIndex)
            Log.i(TAG, "telephony.resetIms(slotIndex=$slotIndex) executed successfully")
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "resetIms failed: ${t.message}", t)
            Result.failure(t)
        }
    }

    /**
     * Queries the actual hardware IMS registration status directly from the telephony service.
     */
    fun isImsRegistered(subId: Int): Boolean {
        return try {
            val telephony = getTelephony() ?: return false
            telephony.isImsRegistered(subId)
        } catch (t: Throwable) {
            Log.w(TAG, "isImsRegistered check failed: ${t.message}")
            false
        }
    }

    /**
     * Reads active CarrierConfig bundle for the given subscription.
     */
    fun getCarrierConfig(subId: Int): PersistableBundle? {
        return try {
            val loader = getCarrierConfigLoader() ?: return null
            try {
                loader.getConfigForSubIdWithFeature(subId, loader.defaultCarrierServicePackageName, "")
            } catch (_: Throwable) {
                try {
                    loader.getConfigForSubId(subId, loader.defaultCarrierServicePackageName)
                } catch (_: Throwable) {
                    val method = loader.javaClass.getMethod("getConfigForSubId", Int::class.javaPrimitiveType)
                    method.invoke(loader, subId) as? PersistableBundle
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "getCarrierConfig failed: ${t.message}")
            null
        }
    }

    /**
     * 1-Tap Turbo Action: Injects overrides, provisions VoIMS, and resets the modem IMS stack.
     */
    suspend fun applyFullCarrierProfile(
        context: Context,
        subId: Int,
        bundle: PersistableBundle
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val overrideResult = overrideConfig(context, subId, bundle, persistent = false)
        if (overrideResult.isFailure) {
            return@withContext overrideResult
        }

        // Set VoIMS Provisioning
        provisionVoIms(subId, true)

        // Reset modem IMS SIP stack
        resetIms(subId)

        // Set low-level vendor radio properties
        try {
            Runtime.getRuntime().exec("setprop persist.vendor.radio.volte_enabled 1").waitFor()
            Runtime.getRuntime().exec("setprop persist.vendor.radio.vowifi_enabled 1").waitFor()
            Runtime.getRuntime().exec("setprop persist.vendor.radio.vonr_enabled 1").waitFor()
        } catch (_: Throwable) {}

        Result.success(Unit)
    }
}
