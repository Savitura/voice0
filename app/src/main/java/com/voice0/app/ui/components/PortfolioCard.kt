package com.voice0.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voice0.app.ui.theme.Outline
import com.voice0.app.ui.theme.Surface
import com.voice0.app.ui.theme.TextMuted
import com.voice0.app.ui.theme.TextPrimary

@Composable
fun PortfolioCard(
    totalUsd: Double,
    walletPubkey: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, Outline, RoundedCornerShape(12.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "Total value",
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
        )
        Text(
            "$" + if (totalUsd >= 1) "%.2f".format(totalUsd) else "%.4f".format(totalUsd),
            color = TextPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp,
        )
        if (walletPubkey != null) {
            Text(
                "${walletPubkey.take(6)}…${walletPubkey.takeLast(6)}",
                color = TextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
