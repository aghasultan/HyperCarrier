package dev.hypercarrier.patcher.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.telephony.SubscriptionManager
import android.widget.Toast
import dev.hypercarrier.patcher.ipc.ShizukuBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Quick Settings Tile to execute a 1-tap "Radio Turbo Flush".
 * Soft cycles radio power to immediately flush dead cell locks and lock onto optimal 5G / LTE-A carrier components.
 */
class RadioFlushTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.label = "Radio Flush"
        tile.subtitle = "Tap to Re-attach"
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val subId = SubscriptionManager.getDefaultDataSubscriptionId()
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            Toast.makeText(this, "No active SIM card found", Toast.LENGTH_SHORT).show()
            return
        }

        if (!ShizukuBridge.hasPermission.value) {
            Toast.makeText(this, "Shizuku authorization required to flush radio", Toast.LENGTH_SHORT).show()
            return
        }

        val tile = qsTile
        tile?.state = Tile.STATE_ACTIVE
        tile?.label = "Flushing..."
        tile?.updateTile()

        serviceScope.launch {
            Toast.makeText(this@RadioFlushTileService, "Flushing Radio & Re-aggregating Bands...", Toast.LENGTH_SHORT).show()
            ShizukuBridge.cycleRadioPower(subId)
            ShizukuBridge.flushDnsCache()

            tile?.state = Tile.STATE_INACTIVE
            tile?.label = "Radio Flush"
            tile?.subtitle = "Done"
            tile?.updateTile()
        }
    }
}
