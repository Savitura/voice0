package com.voice0.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voice0.app.ui.theme.Accent
import com.voice0.app.ui.theme.AccentLight
import com.voice0.app.ui.theme.Bg
import com.voice0.app.ui.theme.Outline
import com.voice0.app.ui.theme.SurfaceHigh
import com.voice0.app.ui.theme.SurfaceLow
import com.voice0.app.ui.theme.TextMuted
import com.voice0.app.ui.theme.TextPrimary
import com.voice0.app.ui.theme.TextSecondary
import com.voice0.app.viewmodel.HomeViewModel

@Composable
fun ExecutionScreen(state: HomeViewModel.UiState) {
    val bundle = state.bundle
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .systemBarsPadding()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                color = Accent,
                trackColor = Accent.copy(alpha = 0.10f),
                modifier = Modifier.size(80.dp),
                strokeWidth = 3.dp,
            )
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceHigh)
                    .border(1.dp, Outline, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("✍", fontSize = 22.sp)
            }
        }

        Box(Modifier.size(28.dp))

        Text(
            "Signing & sending",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp,
        )

        Box(Modifier.size(6.dp))

        Text(
            "Waiting for wallet approval…",
            color = TextMuted,
            fontSize = 13.sp,
        )

        if (bundle != null && bundle.steps.isNotEmpty()) {
            Box(Modifier.size(28.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                bundle.steps.forEachIndexed { idx, step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceLow)
                            .border(1.dp, Outline, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "${idx + 1}",
                            color = AccentLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            step.humanSummary,
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
