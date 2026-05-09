package com.voice0.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.voice0.app.ui.theme.Accent
import com.voice0.app.ui.theme.Bg
import com.voice0.app.ui.theme.Danger
import com.voice0.app.ui.theme.DangerDeep
import com.voice0.app.ui.theme.Outline
import com.voice0.app.ui.theme.OutlineLow
import com.voice0.app.ui.theme.Success
import com.voice0.app.ui.theme.Surface
import com.voice0.app.ui.theme.SurfaceHigh
import com.voice0.app.ui.theme.SurfaceLow
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
            .background(Bg)
            .systemBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Review",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceHigh)
                    .border(1.dp, Outline, RoundedCornerShape(8.dp))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", color = TextMuted, fontSize = 13.sp)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Outline),
        )

        if (state.transcript.isNotBlank()) {
            Text(
                "\"${state.transcript}\"",
                color = TextMuted,
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
        ) {
            itemsIndexed(bundle.steps) { idx, step ->
                StepCard(step, idx, state.prices)
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceHigh)
                        .border(1.dp, Outline, RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SummaryRow(
                        "Simulation",
                        if (bundle.simulationPassed) "✓ Passed" else "✗ Failed",
                        if (bundle.simulationPassed) Success else Danger,
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(OutlineLow))
                    SummaryRow("Network fee", formatLamports(bundle.estimatedFeeLamports), TextSecondary)
                }

                if (needsExtra) {
                    Box(Modifier.height(10.dp))
                    HighImpactBlock(ack = ackHighImpact, onAck = { ackHighImpact = it })
                }

                if (bundle.warnings.isNotEmpty()) {
                    Box(Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(WarningDeep)
                            .border(1.dp, Warning.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Warnings", color = Warning, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        bundle.warnings.forEach {
                            Text("• $it", color = Warning.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }
                }

                Box(Modifier.height(16.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Outline),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Cancel",
                color = TextMuted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceHigh)
                    .border(1.dp, Outline, RoundedCornerShape(10.dp))
                    .clickable { onDismiss() }
                    .padding(horizontal = 20.dp, vertical = 13.dp),
            )
            Text(
                "Execute",
                color = if (canExecute) Color.White else TextMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (canExecute) Accent else SurfaceLow)
                    .border(
                        1.dp,
                        if (canExecute) Color.Transparent else Outline,
                        RoundedCornerShape(10.dp),
                    )
                    .clickable(enabled = canExecute) { onConfirm() }
                    .padding(vertical = 13.dp),
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TextMuted, fontSize = 13.sp)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun HighImpactBlock(ack: Boolean, onAck: (Boolean) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DangerDeep)
            .border(1.dp, Danger.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("High price impact", color = Danger, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(
            "This swap will move the market price significantly.",
            color = Danger.copy(alpha = 0.75f),
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.clickable { onAck(!ack) },
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (ack) Danger else DangerDeep)
                    .border(1.dp, Danger.copy(alpha = 0.5f), RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (ack) Text("✓", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text("I understand the risk", color = Danger.copy(alpha = 0.9f), fontSize = 13.sp)
        }
    }
}

private fun formatLamports(lamports: Long): String {
    val sol = lamports / 1_000_000_000.0
    return if (sol < 0.0001) "$lamports lamports" else "~${"%.5f".format(sol)} SOL"
}
