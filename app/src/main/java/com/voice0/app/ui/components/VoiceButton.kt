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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.voice0.app.data.AppPhase
import com.voice0.app.ui.theme.Accent
import com.voice0.app.ui.theme.AccentLight
import com.voice0.app.ui.theme.Outline
import com.voice0.app.ui.theme.Recording
import com.voice0.app.ui.theme.RecordingDeep
import com.voice0.app.ui.theme.Surface
import com.voice0.app.ui.theme.SurfaceHigh
import com.voice0.app.ui.theme.TextMuted

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
        targetValue = if (isRecording) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "btnScale",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse1",
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(800),
        ),
        label = "pulse2",
    )

    Box(
        modifier = modifier.size(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Expanding rings when recording — solid color, no glow
        if (isRecording) {
            Canvas(modifier = Modifier.size(180.dp)) {
                val base = size.minDimension / 2f * 0.778f
                drawCircle(
                    color = Recording,
                    radius = base * (1f + pulse1 * 0.70f),
                    alpha = (1f - pulse1) * 0.30f,
                )
                drawCircle(
                    color = Recording,
                    radius = base * (1f + pulse2 * 0.70f),
                    alpha = (1f - pulse2) * 0.30f,
                )
            }
        }

        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(buttonScale)
                .clip(CircleShape)
                .background(
                    when {
                        isRecording -> RecordingDeep
                        isBusy -> SurfaceHigh
                        else -> Surface
                    }
                )
                .border(
                    width = if (isRecording) 1.5.dp else 1.dp,
                    color = when {
                        isRecording -> Recording.copy(alpha = 0.6f)
                        isBusy -> Accent.copy(alpha = 0.4f)
                        else -> Outline
                    },
                    shape = CircleShape,
                )
                .pointerInput(pressable) {
                    if (!pressable) return@pointerInput
                    detectTapGestures(
                        onPress = {
                            onPressIn()
                            try {
                                tryAwaitRelease()
                            } finally {
                                onPressOut()
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            when {
                isBusy -> CircularProgressIndicator(
                    color = AccentLight,
                    trackColor = Outline,
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 2.dp,
                )
                isRecording -> Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Recording",
                    tint = Recording,
                    modifier = Modifier.size(48.dp),
                )
                else -> Icon(
                    imageVector = Icons.Filled.MicOff,
                    contentDescription = "Hold to speak",
                    tint = TextMuted,
                    modifier = Modifier.size(44.dp),
                )
            }
        }
    }
}
