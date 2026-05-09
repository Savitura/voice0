package com.voice0.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voice0.app.data.TokenBalance
import com.voice0.app.solana.Balances
import com.voice0.app.ui.theme.Outline
import com.voice0.app.ui.theme.Success
import com.voice0.app.ui.theme.SurfaceHigh
import com.voice0.app.ui.theme.TextMuted
import com.voice0.app.ui.theme.TextPrimary
import com.voice0.app.ui.theme.TextSecondary

@Composable
fun TokenRow(
    b: TokenBalance,
    price: Double?,
    modifier: Modifier = Modifier,
) {
    val usdValue = price?.let { it * b.balance }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceHigh)
            .border(1.dp, Outline, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(b.symbol, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (price != null) {
                Text(
                    "$${"%.4f".format(price)}",
                    color = TextMuted,
                    fontSize = 11.sp,
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                Balances.formatBalance(b.balance),
                color = TextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
            )
            if (usdValue != null) {
                Text(
                    "$" + if (usdValue >= 1) "%.2f".format(usdValue) else "%.4f".format(usdValue),
                    color = Success.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}
