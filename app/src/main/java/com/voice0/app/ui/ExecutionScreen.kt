package com.voice0.app.ui

import androidx.compose.foundation.background
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
import com.voice0.app.ui.theme.Bg
import com.voice0.app.ui.theme.LightBg
import com.voice0.app.ui.theme.LightSurface
import com.voice0.app.ui.theme.LightTextMuted
import com.voice0.app.ui.theme.LightTextPrimary
import com.voice0.app.ui.theme.LightTextSecondary
import com.voice0.app.ui.theme.SurfaceHigh
import com.voice0.app.ui.theme.TextMuted
import com.voice0.app.ui.theme.TextPrimary
import com.voice0.app.viewmodel.HomeViewModel

@Composable
fun ExecutionScreen(state: HomeViewModel.UiState) {
    val bundle = state.bundle

    Column(
        modifier = Modifier.fillMaxSize().background(LightBg).systemBarsPadding(),
    ) {
        // Dark top
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Bg)
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = TextPrimary,
                    trackColor = TextPrimary.copy(alpha = 0.10f),
                    modifier = Modifier.size(80.dp),
                    strokeWidth = 3.dp,
                )
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(SurfaceHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✍", fontSize = 22.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Signing & sending", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Waiting for wallet approval…", color = TextMuted, fontSize = 14.sp)
        }

        // Light bottom with steps
        if (bundle != null && bundle.steps.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Steps", color = LightTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                bundle.steps.forEachIndexed { idx, step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(LightSurface)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("${idx + 1}", color = LightTextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(step.humanSummary, color = LightTextSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
