package dev.hypercarrier.patcher.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.telephony.SubscriptionManager
import android.widget.Toast
import dev.hypercarrier.patcher.data.CarrierConfigPayloadBuilder
import dev.hypercarrier.patcher.ipc.ShizukuBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Quick Settings Tile to toggle Voice over New Radio (VoNR) / 5G SA Voice on the fly.
 */
class VoNRToggleTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isVoNrEnabled = true

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val subId = SubscriptionManager.getDefaultDataSubscriptionId()
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            Toast.makeText(this, "No active SIM card found", Toast.LENGTH_SHORT).show()
            return
        }

        if (!ShizukuBridge.hasPermission.value) {
            Toast.makeText(this, "Shizuku authorization required to toggle VoNR", Toast.LENGTH_SHORT).show()
            return
        }

        val targetState = !isVoNrEnabled

        serviceScope.launch {
            val bundle = CarrierConfigPayloadBuilder()
                .enableVoNr(enabled = targetState, settingVisibility = true)
                .build()

            val result = ShizukuBridge.applyPersistentConfig(subId, bundle)
            if (result.isSuccess) {
                isVoNrEnabled = targetState
                updateTileState()
                Toast.makeText(
                    this@VoNRToggleTileService,
                    if (targetState) "VoNR (5G SA Voice) Enabled" else "VoNR Disabled",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@VoNRToggleTileService,
                    "Failed to toggle VoNR: ${result.exceptionOrNull()?.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val subId = SubscriptionManager.getDefaultDataSubscriptionId()

        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            tile.state = Tile.STATE_UNAVAILABLE
            tile.label = "VoNR"
            tile.subtitle = "No SIM"
            tile.updateTile()
            return
        }

        tile.state = if (isVoNrEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "VoNR (5G Voice)"
        tile.subtitle = if (isVoNrEnabled) "Enabled" else "Disabled"
        tile.updateTile()
    }
}
