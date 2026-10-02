package dev.hypercarrier.patcher.ipc

import android.annotation.SuppressLint
import android.app.IActivityManager
import android.app.Instrumentation
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.os.Process
import android.os.ServiceManager
import android.telephony.CarrierConfigManager
import android.util.Log
import rikka.shizuku.ShizukuBinderWrapper

/**
 * HyperInstrumentation runs with Delegated Shell Permission Identity under Shizuku.
 * By executing am.startDelegateShellPermissionIdentity(Process.myUid(), null), the app process
 * adopts full Shell UID 2000 permissions, allowing CarrierConfigManager.overrideConfig to execute
 * without SecurityException or caller UID permission drops on Android 14/15/16/17.
 */
class HyperInstrumentation : Instrumentation() {

    companion object {
        private const val TAG = "HyperInstrumentation"
    }

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        if (arguments == null) {
            finish(0, null)
            return
        }

        val subId = arguments.getInt("subId", 0)
        val clear = arguments.getBoolean("clear", false)
        val persistent = arguments.getBoolean("persistent", false)
        
        val overrides: PersistableBundle? = if (clear) {
            null
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arguments.getParcelable("overrides", PersistableBundle::class.java)
            } else {
                @Suppress("DEPRECATION")
                arguments.getParcelable("overrides")
            }
        }

        overrideConfig(subId, overrides, clear, persistent)
    }

    @SuppressLint("MissingPermission")
    private fun overrideConfig(subId: Int, overrides: PersistableBundle?, clear: Boolean, persistent: Boolean) {
        Log.i(TAG, "Executing overrideConfig for subId=$subId, clear=$clear, persistent=$persistent")
        val amService = ServiceManager.getService(Context.ACTIVITY_SERVICE)
        val am = IActivityManager.Stub.asInterface(ShizukuBinderWrapper(amService))
        val myUid = Process.myUid()

        try {
            am.startDelegateShellPermissionIdentity(myUid, null)
            val ccm = context.getSystemService(CarrierConfigManager::class.java)
            if (ccm != null) {
                if (clear) {
                    try {
                        ccm.overrideConfig(subId, null, false)
                    } catch (_: Throwable) {
                        try {
                            ccm.overrideConfig(subId, null)
                        } catch (t: Throwable) {
                            Log.e(TAG, "Failed to clear carrier config: ${t.message}", t)
                        }
                    }
                    Log.i(TAG, "CarrierConfig cleared successfully for subId=$subId")
                } else {
                    try {
                        ccm.overrideConfig(subId, overrides, persistent)
                        Log.i(TAG, "CarrierConfig override applied with persistent=$persistent")
                    } catch (e: SecurityException) {
                        Log.w(TAG, "Persistent override rejected by OS policy, falling back to in-memory: ${e.message}")
                        ccm.overrideConfig(subId, overrides, false)
                    } catch (_: Throwable) {
                        ccm.overrideConfig(subId, overrides)
                    }
                    Log.i(TAG, "CarrierConfig override successfully applied for subId=$subId")
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error in HyperInstrumentation overrideConfig: ${t.message}", t)
        } finally {
            try {
                am.stopDelegateShellPermissionIdentity()
            } catch (_: Throwable) {
                try {
                    val method = am.javaClass.getDeclaredMethod("stopDelegateShellPermissionIdentity", Int::class.javaPrimitiveType)
                    method.invoke(am, myUid)
                } catch (_: Throwable) {}
            }
            finish(0, null)
        }
    }
}
