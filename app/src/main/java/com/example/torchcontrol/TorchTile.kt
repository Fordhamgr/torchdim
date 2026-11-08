package com.example.torchcontrol

import android.graphics.drawable.Icon
import android.hardware.camera2.CameraManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

// ---------- Quick Settings tile: Instant cycling on tap ----------
class TorchTile : TileService() {
    private val cb = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            val id = Torch.id(this@TorchTile)
            if (cameraId == id) {
                Torch.setOn(enabled)
                updateTileUi()
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        Torch.cm(this).registerTorchCallback(cb, null)
        updateTileUi()
    }

    override fun onStopListening() {
        super.onStopListening()
        Torch.cm(this).unregisterTorchCallback(cb)
    }

    override fun onClick() {
        // Atomic cycling: updates HAL and in-memory state with zero latency
        val (isOn, newLevel) = Torch.cycleTile(this)
        updateTileUi(isOn, newLevel)
    }

    private fun updateTileUi(forceOn: Boolean? = null, forceLevel: Int? = null) {
        val id = Torch.id(this) ?: return
        val max = Torch.max(this, id)
        val isOn = forceOn ?: Torch.isOn()
        val currentLvl = forceLevel ?: Torch.level(this, id)

        qsTile?.apply {
            state = if (isOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            subtitle = if (isOn) {
                when (currentLvl) {
                    max -> "Level $max (Max)"
                    1 -> "Level 1 (Min)"
                    else -> "Level $currentLvl"
                }
            } else {
                "Off"
            }
            icon = Icon.createWithResource(this@TorchTile, Torch.getTileIconRes(isOn, currentLvl, max))
            updateTile()
        }
    }
}
