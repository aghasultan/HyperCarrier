package dev.hypercarrier.patcher.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.telephony.SubscriptionManager
import dev.hypercarrier.patcher.ipc.HyperCarrierEngine
import dev.hypercarrier.patcher.ipc.ShizukuBridge
import dev.hypercarrier.patcher.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Quick Settings Tile displaying live IMS registration status (VoLTE / VoWiFi / VoNR)
 * queried directly from hardware modem baseband.
 */
class ImsStatusTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        // Open the app to Diagnostics screen
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_TARGET_TAB", "diagnostics")
        }
        startActivityAndCollapse(intent)
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val subId = SubscriptionManager.getDefaultDataSubscriptionId()

        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            tile.state = Tile.STATE_UNAVAILABLE
            tile.label = "IMS: No SIM"
            tile.subtitle = "Insert SIM"
            tile.updateTile()
            return
        }

        serviceScope.launch {
            val isRegistered = HyperCarrierEngine.isImsRegistered(subId) || ShizukuBridge.getImsRegistrationState(subId) == 1

            if (isRegistered) {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "IMS: Registered"
                tile.subtitle = "VoLTE / VoWiFi Active"
            } else {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "IMS: Unregistered"
                tile.subtitle = "Tap to Diagnose"
            }
            tile.updateTile()
        }
    }
}
