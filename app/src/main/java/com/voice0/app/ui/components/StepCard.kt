package com.voice0.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voice0.app.data.SolanaTxStep
import com.voice0.app.data.StepType
import com.voice0.app.data.Tokens
import com.voice0.app.ui.theme.Accent
import com.voice0.app.ui.theme.AccentLight
import com.voice0.app.ui.theme.Danger
import com.voice0.app.ui.theme.DangerDeep
import com.voice0.app.ui.theme.Outline
import com.voice0.app.ui.theme.OutlineLow
import com.voice0.app.ui.theme.Success
import com.voice0.app.ui.theme.SuccessDeep
import com.voice0.app.ui.theme.Surface
import com.voice0.app.ui.theme.SurfaceHigh
import com.voice0.app.ui.theme.TextMuted
import com.voice0.app.ui.theme.TextPrimary
import com.voice0.app.ui.theme.TextSecondary
import com.voice0.app.ui.theme.Warning
import com.voice0.app.ui.theme.WarningDeep
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun StepCard(
    step: SolanaTxStep,
    index: Int,
    prices: Map<String, Double>,
) {
    val simOk = step.simulationOk

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceHigh)
            .border(1.dp, Outline, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                (index + 1).toString(),
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Surface)
                    .border(1.dp, Outline, RoundedCornerShape(4.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text(
                    step.type.name,
                    color = AccentLight,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            if (simOk != null) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (simOk) Success else Danger),
                )
                Text(
                    if (simOk) "Passed" else "Failed",
                    color = if (simOk) Success else Danger,
                    fontSize = 11.sp,
                )
            }
        }

        Text(
            step.humanSummary,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(OutlineLow),
        )

        when (step.type) {
            StepType.SWAP -> SwapDetails(step.params, prices)
            StepType.TRANSFER -> TransferDetails(step.params, prices)
        }

        if (step.requiresExtraConfirm) {
            WarningTag("High price impact (${"%.2f".format((step.priceImpactPct ?: 0.0) * 100)}%)")
        }
        step.warnings.forEach { WarningTag(it) }
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun WarningTag(text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(WarningDeep)
            .border(1.dp, Warning.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("⚠", color = Warning, fontSize = 11.sp)
        Text(text, color = Warning, fontSize = 12.sp)
    }
}

@Composable
private fun SwapDetails(params: kotlinx.serialization.json.JsonObject, prices: Map<String, Double>) {
    val inputMint = params["inputMint"]?.string() ?: return
    val outputMint = params["outputMint"]?.string() ?: return
    val amount = params["amount"]?.string()?.toDoubleOrNull() ?: 0.0
    val slip = params["slippageBps"]?.string()?.toIntOrNull() ?: 0

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ParamRow("From", "$amount ${Tokens.symbolFromMint(inputMint)}", usd(amount, inputMint, prices))
        Text("↓", color = TextMuted, fontSize = 16.sp)
        ParamRow("To", Tokens.symbolFromMint(outputMint))
        ParamRow("Slippage", "${slip / 100.0}%")
    }
}

@Composable
private fun TransferDetails(params: kotlinx.serialization.json.JsonObject, prices: Map<String, Double>) {
    val mint = params["mint"]?.string() ?: return
    val amount = params["amount"]?.string()?.toDoubleOrNull() ?: 0.0
    val dest = params["destination"]?.string() ?: ""

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ParamRow("Amount", "$amount ${Tokens.symbolFromMint(mint)}", usd(amount, mint, prices))
        ParamRow("To", truncate(dest))
    }
}

@Composable
private fun ParamRow(label: String, value: String, usd: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TextMuted, fontSize = 13.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(value, color = TextSecondary, fontSize = 13.sp)
            if (usd != null) Text(usd, color = Success, fontSize = 12.sp)
        }
    }
}

private fun truncate(s: String): String =
    if (s.length <= 12) s else "${s.take(6)}…${s.takeLast(4)}"

private fun JsonElement.string(): String =
    runCatching { jsonPrimitive.content }.getOrDefault("")

private fun usd(amount: Double, mint: String, prices: Map<String, Double>): String? {
    val p = prices[mint] ?: return null
    val total = amount * p
    return "$" + if (total >= 1) "%.2f".format(total) else "%.4f".format(total)
}
