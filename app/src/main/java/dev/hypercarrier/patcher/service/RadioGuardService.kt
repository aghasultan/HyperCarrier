package dev.hypercarrier.patcher.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.hypercarrier.patcher.data.CarrierPresets
import dev.hypercarrier.patcher.ipc.ShizukuBridge
import dev.hypercarrier.patcher.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Autonomous Radio Guard & Auto-Healer background service.
 * Continuously monitors IMS registration and cellular radio health, automatically soft-resetting
 * the radio and re-asserting persistent overrides when connectivity degrades.
 */
class RadioGuardService : Service() {

    companion object {
        private const val TAG = "RadioGuardService"
        private const val CHANNEL_ID = "hypercarrier_radio_guard"
        private const val NOTIFICATION_ID = 9001

        const val ACTION_START_GUARD = "dev.hypercarrier.action.START_GUARD"
        const val ACTION_STOP_GUARD = "dev.hypercarrier.action.STOP_GUARD"
        const val ACTION_TRIGGER_HEAL = "dev.hypercarrier.action.TRIGGER_HEAL"

        var isRunning: Boolean = false
            private set
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var watchdogJob: Job? = null
    private var lastHealTimestamp: Long = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_GUARD -> {
                stopWatchdog()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                isRunning = false
                return START_NOT_STICKY
            }
            ACTION_TRIGGER_HEAL -> {
                serviceScope.launch {
                    val subId = SubscriptionManager.getDefaultDataSubscriptionId()
                    healRadio(subId, force = true)
                }
            }
            else -> {
                val notification = buildNotification("Monitoring IMS & 5G/4G+ Connectivity")
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                            startForeground(
                                NOTIFICATION_ID,
                                notification,
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                            )
                        } catch (_: Throwable) {
                            try {
                                startForeground(
                                    NOTIFICATION_ID,
                                    notification,
                                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                                )
                            } catch (_: Throwable) {
                                startForeground(NOTIFICATION_ID, notification)
                            }
                        }
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                    isRunning = true
                    startWatchdog()
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to start foreground service: ${t.message}", t)
                    isRunning = false
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        }
        return START_STICKY
    }

    private fun startWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = serviceScope.launch {
            Log.i(TAG, "Radio Guard watchdog loop started")
            while (isActive) {
                delay(30_000) // Poll every 30 seconds
                try {
                    val subId = SubscriptionManager.getDefaultDataSubscriptionId()
                    if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID && ShizukuBridge.hasPermission.value) {
                        val imsState = ShizukuBridge.getImsRegistrationState(subId)
                        if (imsState == 0) {
                            Log.w(TAG, "IMS unregistered detected on subId=$subId. Triggering auto-heal...")
                            healRadio(subId, force = false)
                        }
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "Error in watchdog check: ${t.message}")
                }
            }
        }
    }

    private fun stopWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = null
    }

    private suspend fun healRadio(subId: Int, force: Boolean) {
        val now = System.currentTimeMillis()
        // 30s cooldown unless forced
        if (!force && now - lastHealTimestamp < 30_000) {
            Log.d(TAG, "Skipping heal due to cooldown")
            return
        }

        lastHealTimestamp = now
        updateNotification("Auto-Healing Radio: Re-asserting CarrierConfig & CA...")

        val bundle = CarrierPresets.GLOBAL_ULTRA_UNLOCK.payloadBuilder(subId)
        ShizukuBridge.applyPersistentConfig(subId, bundle)
        ShizukuBridge.cycleRadioPower(subId)
        ShizukuBridge.flushDnsCache()

        delay(3000)
        updateNotification("Radio Healed: VoLTE & 5G/4G+ Protected")
    }

    private fun buildNotification(statusText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val healIntent = Intent(this, RadioGuardService::class.java).apply {
            action = ACTION_TRIGGER_HEAL
        }
        val healPendingIntent = PendingIntent.getService(
            this, 1, healIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HyperCarrier Auto-Healer")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth) // System icon fallback
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_rotate, "Heal Now", healPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "HyperCarrier Radio Guard",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors and automatically heals cellular IMS and 5G/LTE connection drops"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopWatchdog()
        isRunning = false
    }
}
