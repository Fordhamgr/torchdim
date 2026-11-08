package com.example.torchcontrol

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

// ---------- Synchronous State Machine & Shared Logic ----------
object Torch {
    private var cachedLevel: Int? = null
    private var isTorchOn: Boolean = false

    fun cm(c: Context): CameraManager = c.getSystemService(CameraManager::class.java)

    // First camera that has a flash unit (camera 0 on your phone)
    fun id(c: Context): String? = cm(c).cameraIdList.firstOrNull {
        cm(c).getCameraCharacteristics(it)
            .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
    }

    // Max brightness level (6 on your phone). 1 = on/off only
    fun max(c: Context, id: String): Int = cm(c).getCameraCharacteristics(id)
        .get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1

    // Read saved level (synchronously cached in memory)
    fun level(c: Context, id: String): Int {
        cachedLevel?.let { return it }
        val maxLevel = max(c, id)
        val saved = c.getSharedPreferences("torch", Context.MODE_PRIVATE)
            .getInt("level", maxLevel)
            .coerceIn(1, maxLevel)
        cachedLevel = saved
        return saved
    }

    fun save(c: Context, level: Int) {
        cachedLevel = level
        c.getSharedPreferences("torch", Context.MODE_PRIVATE)
            .edit()
            .putInt("level", level)
            .apply()
    }

    // Direct hardware dispatch
    fun set(c: Context, id: String, level: Int?) {
        try {
            if (level == null || level <= 0) {
                isTorchOn = false
                cm(c).setTorchMode(id, false)
            } else {
                val target = level.coerceIn(1, max(c, id))
                isTorchOn = true
                cachedLevel = target
                save(c, target)
                if (max(c, id) <= 1) {
                    cm(c).setTorchMode(id, true)
                } else {
                    cm(c).turnOnTorchWithStrengthLevel(id, target)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isOn(): Boolean = isTorchOn
    fun setOn(enabled: Boolean) { isTorchOn = enabled }

    // Atomic tile cycling: OFF -> Level 6 (Max) -> 5 -> 4 -> 3 -> 2 -> 1 -> OFF
    fun cycleTile(c: Context): Pair<Boolean, Int> {
        val camId = id(c) ?: return Pair(false, 0)
        val maxLevel = max(c, camId)

        if (!isTorchOn) {
            // First press from OFF: turn ON at MAX brightness!
            set(c, camId, maxLevel)
            return Pair(true, maxLevel)
        } else {
            val current = level(c, camId)
            if (current <= 1) {
                // Reached minimum: turn OFF
                set(c, camId, null)
                return Pair(false, 0)
            } else {
                // Cycle down by 1 level
                val next = current - 1
                set(c, camId, next)
                return Pair(true, next)
            }
        }
    }

    // High performance vector icon resource lookup (Zero IPC bitmap allocation)
    fun getTileIconRes(isOn: Boolean, level: Int, maxLevel: Int = 6): Int {
        if (!isOn) return R.drawable.ic_torch_off
        return when (level.coerceIn(1, maxLevel)) {
            1 -> R.drawable.ic_torch_lvl1
            2 -> R.drawable.ic_torch_lvl2
            3 -> R.drawable.ic_torch_lvl3
            4 -> R.drawable.ic_torch_lvl4
            5 -> R.drawable.ic_torch_lvl5
            else -> R.drawable.ic_torch_lvl6
        }
    }
}
