package com.voice0.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voice0.app.data.TokenBalance
import com.voice0.app.solana.Balances
import com.voice0.app.ui.theme.TokenBONK
import com.voice0.app.ui.theme.TokenDefault
import com.voice0.app.ui.theme.TokenJUP
import com.voice0.app.ui.theme.TokenSOL
import com.voice0.app.ui.theme.TokenUSDC
import com.voice0.app.ui.theme.TokenUSDT

private fun tokenColor(symbol: String): Color = when (symbol.uppercase()) {
    "SOL" -> TokenSOL
    "USDC" -> TokenUSDC
    "USDT" -> TokenUSDT
    "BONK" -> TokenBONK
    "JUP" -> TokenJUP
    else -> TokenDefault
}

// Dark card matching the Aivora reference style (dark pills on light bg)
@Composable
fun TokenRow(
    b: TokenBalance,
    price: Double?,
    modifier: Modifier = Modifier,
) {
    val usdValue = price?.let { it * b.balance }
    val color = tokenColor(b.symbol)
    val cardBg = Color(0xFF1C1C1E)
    val textMain = Color.White
    val textSub = Color(0xFFAEAEB2)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Colored symbol circle
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(b.symbol.take(1), color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(b.symbol, color = textMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            if (price != null) {
                Text("$${"%.2f".format(price)}", color = textSub, fontSize = 13.sp)
            }
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                Balances.formatBalance(b.balance),
                color = textMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
            )
            if (usdValue != null) {
                Text(
                    "$" + if (usdValue >= 1) "%,.2f".format(usdValue) else "%.4f".format(usdValue),
                    color = textSub, fontSize = 13.sp, textAlign = TextAlign.End,
                )
            }
        }
    }
}
