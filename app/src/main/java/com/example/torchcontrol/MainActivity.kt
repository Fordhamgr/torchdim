package com.example.torchcontrol

import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TorchDimLandingScreen()
        }
    }
}

@Composable
fun TorchDimLandingScreen() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val camId = remember { Torch.id(context) }
    val maxLevel = remember { camId?.let { Torch.max(context, it) } ?: 6 }

    var isTorchOn by remember { mutableStateOf(Torch.isOn()) }
    var currentLevel by remember {
        mutableIntStateOf(camId?.let { Torch.level(context, it) } ?: maxLevel)
    }

    // CameraManager Torch Callback to stay live in sync
    DisposableEffect(camId) {
        if (camId == null) return@DisposableEffect onDispose {}

        val cb = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (cameraId == camId) {
                    Torch.setOn(enabled)
                    isTorchOn = enabled
                    if (enabled) {
                        currentLevel = Torch.level(context, cameraId)
                    }
                }
            }
        }
        Torch.cm(context).registerTorchCallback(cb, null)

        onDispose {
            Torch.cm(context).unregisterTorchCallback(cb)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0C))
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Header with App Icon and Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini Pill Icon (half grey, half yellow)
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF48484A))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(13.dp)
                            .align(Alignment.BottomCenter)
                            .background(Color(0xFFFFD60A))
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = stringResource(R.string.app_name),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Giant Vertical Hero Capsule Slider
            HeroCapsuleSlider(
                level = if (isTorchOn) currentLevel else 0,
                maxLevel = maxLevel,
                onLevelChange = { newLevel ->
                    if (camId != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (newLevel == 0) {
                            Torch.set(context, camId, null)
                            isTorchOn = false
                        } else {
                            currentLevel = newLevel
                            isTorchOn = true
                            Torch.save(context, newLevel)
                            Torch.set(context, camId, newLevel)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Status Labels
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isTorchOn) "Level $currentLevel" else stringResource(R.string.off_headline),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isTorchOn) "of $maxLevel · Torch is on" else "of $maxLevel · Torch is off",
                    color = Color(0xFF8E8E93),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. "Add the tile" Guide Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = stringResource(R.string.card_title),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.card_description),
                        color = Color(0xFF8E8E93),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Main Action Button (Turn on / Turn off)
            Button(
                onClick = {
                    if (camId != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isTorchOn) {
                            Torch.set(context, camId, null)
                            isTorchOn = false
                        } else {
                            val target = if (currentLevel > 0) currentLevel else maxLevel
                            Torch.save(context, target)
                            Torch.set(context, camId, target)
                            isTorchOn = true
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD60A)
                )
            ) {
                Text(
                    text = if (isTorchOn) stringResource(R.string.btn_turn_off) else stringResource(R.string.btn_turn_on),
                    color = Color(0xFF121212),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Footer Link to GitHub Repository
            val githubUrl = stringResource(R.string.github_repo_url)
            Column(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .clickable {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
                        context.startActivity(browserIntent)
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.source_on_github),
                    color = Color(0xFF636366),
                    fontSize = 12.sp
                )
                Text(
                    text = stringResource(R.string.github_repo_display),
                    color = Color(0xFF8E8E93),
                    fontSize = 12.sp,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}

@Composable
fun HeroCapsuleSlider(
    level: Int,
    maxLevel: Int,
    onLevelChange: (Int) -> Unit
) {
    val trackColor = Color(0xFF3A3A3C)
    val fillColor = Color(0xFFFFD60A)

    var currentDragLevel by remember(level) { mutableIntStateOf(level) }

    Box(
        modifier = Modifier
            .size(width = 150.dp, height = 300.dp)
            .pointerInput(maxLevel) {
                detectTapGestures { offset ->
                    val frac = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                    val target = if (frac < 0.12f) {
                        0
                    } else {
                        val activeFrac = (frac - 0.12f) / 0.88f
                        (1 + (activeFrac * maxLevel).toInt()).coerceIn(1, maxLevel)
                    }
                    if (target != currentDragLevel) {
                        currentDragLevel = target
                        onLevelChange(target)
                    }
                }
            }
            .pointerInput(maxLevel) {
                detectDragGestures(
                    onDrag = { change, _ ->
                        change.consume()
                        val frac = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                        val target = if (frac < 0.12f) {
                            0
                        } else {
                            val activeFrac = (frac - 0.12f) / 0.88f
                            (1 + (activeFrac * maxLevel).toInt()).coerceIn(1, maxLevel)
                        }
                        if (target != currentDragLevel) {
                            currentDragLevel = target
                            onLevelChange(target)
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val radius = w / 2f

            val capsulePath = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        rect = androidx.compose.ui.geometry.Rect(0f, 0f, w, h),
                        cornerRadius = CornerRadius(radius, radius)
                    )
                )
            }

            clipPath(capsulePath) {
                // 1. Dark grey capsule background
                drawRect(color = trackColor)

                // 2. Yellow Fill proportional to level
                if (currentDragLevel > 0) {
                    val fillFrac = currentDragLevel / maxLevel.toFloat()
                    val fillHeight = h * fillFrac
                    val fillTop = h - fillHeight
                    drawRect(
                        color = fillColor,
                        topLeft = Offset(0f, fillTop),
                        size = Size(w, fillHeight)
                    )
                }
            }
        }
    }
}
