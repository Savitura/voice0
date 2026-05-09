package com.voice0.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.voice0.app.data.AppPhase
import com.voice0.app.ui.theme.Outline
import com.voice0.app.ui.theme.Recording
import com.voice0.app.ui.theme.Surface
import com.voice0.app.ui.theme.SurfaceHigh
import com.voice0.app.ui.theme.TextMuted
import com.voice0.app.ui.theme.TextSecondary

@Composable
fun VoiceButton(
    phase: AppPhase,
    onPressIn: () -> Unit,
    onPressOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRecording = phase == AppPhase.RECORDING
    val isBusy = phase in listOf(AppPhase.TRANSCRIBING, AppPhase.PARSING, AppPhase.SIMULATING)
    val pressable = phase == AppPhase.IDLE || isRecording

    val buttonScale by animateFloatAsState(
        targetValue = if (isRecording) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "btnScale",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "p1",
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1600, easing = LinearEasing), RepeatMode.Restart, initialStartOffset = StartOffset(800),
        ),
        label = "p2",
    )

    Box(modifier = modifier.size(140.dp), contentAlignment = Alignment.Center) {
        // Pulse rings
        Canvas(Modifier.size(140.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val base = size.minDimension / 2f * 0.72f
            if (isRecording) {
                drawCircle(Recording, base * (1f + pulse1 * 0.6f), center, alpha = (1f - pulse1) * 0.25f)
                drawCircle(Recording, base * (1f + pulse2 * 0.6f), center, alpha = (1f - pulse2) * 0.25f)
            }
        }

        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(buttonScale)
                .clip(CircleShape)
                .background(if (isRecording) Recording.copy(alpha = 0.1f) else Surface)
                .border(
                    width = 1.dp,
                    color = when {
                        isRecording -> Recording.copy(alpha = 0.5f)
                        isBusy -> TextMuted.copy(alpha = 0.3f)
                        else -> Outline
                    },
                    shape = CircleShape,
                )
                .pointerInput(pressable) {
                    if (!pressable) return@pointerInput
                    detectTapGestures(
                        onPress = {
                            onPressIn()
                            try { tryAwaitRelease() } finally { onPressOut() }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            when {
                isBusy -> CircularProgressIndicator(
                    color = TextSecondary,
                    trackColor = Outline,
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.dp,
                )
                isRecording -> Icon(
                    Icons.Filled.Mic, "Recording",
                    tint = Recording, modifier = Modifier.size(36.dp),
                )
                else -> Icon(
                    Icons.Filled.MicOff, "Hold to speak",
                    tint = TextMuted, modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}
