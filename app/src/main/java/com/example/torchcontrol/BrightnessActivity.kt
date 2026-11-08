package com.example.torchcontrol

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout

// ---------- Long-press popup: floating iOS-style pill slider ----------
class BrightnessActivity : Activity() {
    private var id: String? = null
    private var pill: PillSlider? = null
    private var isUserDragging = false

    private val cb = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == id) {
                Torch.setOn(enabled)
                // Guard: NEVER overwrite the slider while the user is actively dragging!
                if (!isUserDragging) {
                    runOnUiThread {
                        val current = if (enabled) Torch.level(this@BrightnessActivity, cameraId) else 0
                        pill?.setLevel(current)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val camId = Torch.id(this)
        if (camId == null) {
            finish()
            return
        }
        id = camId
        val max = Torch.max(this, camId)
        val initialLevel = if (Torch.isOn()) Torch.level(this, camId) else 0

        val density = resources.displayMetrics.density
        val pillWidth = (105 * density).toInt()
        val pillHeight = (280 * density).toInt()

        val slider = PillSlider(
            c = this,
            steps = max,
            initialLevel = initialLevel,
            onDragStateChanged = { dragging ->
                isUserDragging = dragging
            },
            onChange = { l ->
                if (l == 0) {
                    Torch.set(this, camId, null)
                } else {
                    Torch.set(this, camId, l)
                }
            }
        )
        pill = slider

        Torch.cm(this).registerTorchCallback(cb, null)

        val root = FrameLayout(this).apply {
            setBackgroundColor(0x88000000.toInt())
            setOnClickListener { finish() }
            addView(slider, FrameLayout.LayoutParams(pillWidth, pillHeight, Gravity.CENTER))
        }
        setContentView(root)
    }

    override fun onStop() {
        super.onStop()
        if (id != null) Torch.cm(this).unregisterTorchCallback(cb)
        finish()
    }
}

class PillSlider(
    c: Context,
    private val steps: Int,
    initialLevel: Int,
    private val onDragStateChanged: (Boolean) -> Unit,
    private val onChange: (Int) -> Unit
) : View(c) {
    private var level: Int = initialLevel
    private val density = resources.displayMetrics.density
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2C2C2E.toInt() }
    private val fg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 0, 0, 0)
        strokeWidth = 1.5f * density
    }
    private val textDark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(230, 20, 20, 20)
        textAlign = Paint.Align.CENTER
        textSize = 15f * density
        isFakeBoldText = true
    }
    private val textLight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(230, 255, 255, 255)
        textAlign = Paint.Align.CENTER
        textSize = 15f * density
        isFakeBoldText = true
    }

    fun setLevel(l: Int) {
        if (level != l) {
            level = l
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.save()
        val clipP = Path().apply {
            addRoundRect(0f, 0f, w, h, w / 2, w / 2, Path.Direction.CW)
        }
        canvas.clipPath(clipP)

        // Dark background pill
        canvas.drawRect(0f, 0f, w, h, bg)

        // Fill from bottom up
        if (level > 0) {
            val fillHeight = h * (level / steps.toFloat())
            val fillTop = h - fillHeight
            canvas.drawRect(0f, fillTop, w, h, fg)
        }

        // Subtle divider lines for the 6 steps
        for (i in 1 until steps) {
            val y = h * (1f - i / steps.toFloat())
            canvas.drawLine(10f * density, y, w - 10f * density, y, dividerPaint)
        }

        // Level text indicator
        val label = when (level) {
            0 -> "OFF"
            steps -> "MAX (6)"
            1 -> "MIN (1)"
            else -> "Level $level"
        }
        val textY = h - 22f * density
        val fillFrac = level / steps.toFloat()
        val paint = if (fillFrac > 0.18f) textDark else textLight
        canvas.drawText(label, w / 2f, textY, paint)

        canvas.restore()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                onDragStateChanged(true)
                parent?.requestDisallowInterceptTouchEvent(true)
                updateLevelFromTouch(e.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                updateLevelFromTouch(e.y)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onDragStateChanged(false)
                return true
            }
        }
        return super.onTouchEvent(e)
    }

    private fun updateLevelFromTouch(y: Float) {
        val frac = (1f - (y / height)).coerceIn(0f, 1f)
        // Bottom 15% is OFF (level 0), remaining 85% is linearly divided into 1..steps
        val newLevel = if (frac < 0.15f) {
            0
        } else {
            val activeFrac = (frac - 0.15f) / 0.85f
            (1 + (activeFrac * steps).toInt()).coerceIn(1, steps)
        }
        if (newLevel != level) {
            level = newLevel
            invalidate()
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            onChange(newLevel)
        }
    }
}
