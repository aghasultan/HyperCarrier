package dev.hypercarrier.patcher

import android.app.Application
import android.os.Build
import android.util.Log
import dev.hypercarrier.patcher.ipc.ShizukuBridge
import org.lsposed.hiddenapibypass.HiddenApiBypass

/**
 * HyperCarrier Application Entry Point.
 */
class HyperCarrierApp : Application() {

    companion object {
        lateinit var instance: HyperCarrierApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                HiddenApiBypass.setHiddenApiExemptions("")
                Log.i("HyperCarrierApp", "HiddenApiBypass exemptions set successfully")
            } catch (t: Throwable) {
                Log.w("HyperCarrierApp", "Failed to set HiddenApiBypass: ${t.message}")
            }
        }

        Log.i("HyperCarrierApp", "Initializing HyperCarrier Engine on Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        ShizukuBridge.init(this)
    }
}
