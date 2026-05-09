package com.voice0.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voice0.app.ui.components.StepCard
import com.voice0.app.ui.theme.Bg
import com.voice0.app.ui.theme.Danger
import com.voice0.app.ui.theme.DangerDeep
import com.voice0.app.ui.theme.LightBg
import com.voice0.app.ui.theme.LightOutline
import com.voice0.app.ui.theme.LightSurface
import com.voice0.app.ui.theme.LightTextMuted
import com.voice0.app.ui.theme.LightTextSecondary
import com.voice0.app.ui.theme.Success
import com.voice0.app.ui.theme.SurfaceHigh
import com.voice0.app.ui.theme.TextMuted
import com.voice0.app.ui.theme.TextPrimary
import com.voice0.app.ui.theme.TextSecondary
import com.voice0.app.ui.theme.Warning
import com.voice0.app.ui.theme.WarningDeep
import com.voice0.app.viewmodel.HomeViewModel

@Composable
fun ReviewScreen(
    state: HomeViewModel.UiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val bundle = state.bundle ?: return
    var ackHighImpact by remember { mutableStateOf(false) }

    val needsExtra = bundle.steps.any { it.requiresExtraConfirm }
    val canExecute = bundle.simulationPassed && (!needsExtra || ackHighImpact)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg)
            .systemBarsPadding(),
    ) {
        // Dark header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(Bg)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Review", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceHigh)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", color = TextMuted, fontSize = 13.sp)
                }
            }
            if (state.transcript.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text("\"${state.transcript}\"", color = TextSecondary, fontSize = 14.sp, fontStyle = FontStyle.Italic)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
        ) {
            item { Spacer(Modifier.height(16.dp)) }
            itemsIndexed(bundle.steps) { idx, step -> StepCard(step, idx, state.prices) }
            item {
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(LightSurface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SummaryRow("Simulation", if (bundle.simulationPassed) "Passed" else "Failed", if (bundle.simulationPassed) Success else Danger)
                    Box(Modifier.fillMaxWidth().height(1.dp).background(LightOutline))
                    SummaryRow("Network fee", formatLamports(bundle.estimatedFeeLamports), LightTextSecondary)
                }

                if (needsExtra) {
                    Spacer(Modifier.height(10.dp))
                    HighImpactBlock(ack = ackHighImpact, onAck = { ackHighImpact = it })
                }

                if (bundle.warnings.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(WarningDeep)
                            .border(1.dp, Warning.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Warnings", color = Warning, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        bundle.warnings.forEach { Text("• $it", color = Warning.copy(alpha = 0.8f), fontSize = 13.sp) }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // Bottom action bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LightSurface)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Cancel", color = LightTextMuted, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(LightBg)
                    .clickable { onDismiss() }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
            )
            Text(
                "Execute",
                color = if (canExecute) Color.White else LightTextMuted,
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (canExecute) Color.Black else LightBg)
                    .clickable(enabled = canExecute) { onConfirm() }
                    .padding(vertical = 14.dp),
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, color = LightTextMuted, fontSize = 14.sp)
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HighImpactBlock(ack: Boolean, onAck: (Boolean) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DangerDeep)
            .border(1.dp, Danger.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("High price impact", color = Danger, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text("This swap will move the market price significantly.", color = Danger.copy(alpha = 0.75f), fontSize = 13.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.clickable { onAck(!ack) },
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (ack) Danger else DangerDeep)
                    .border(1.dp, Danger.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (ack) Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text("I understand the risk", color = Danger.copy(alpha = 0.9f), fontSize = 13.sp)
        }
    }
}

private fun formatLamports(lamports: Long): String {
    val sol = lamports / 1_000_000_000.0
    return if (sol < 0.0001) "$lamports lamports" else "~${"%.5f".format(sol)} SOL"
}
