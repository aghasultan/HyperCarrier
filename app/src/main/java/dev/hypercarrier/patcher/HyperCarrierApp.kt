package dev.hypercarrier.patcher

import android.app.Application
import android.util.Log
import dev.hypercarrier.patcher.ipc.ShizukuBridge

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
        Log.i("HyperCarrierApp", "Initializing HyperCarrier Engine on Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
        ShizukuBridge.init(this)
    }
}
