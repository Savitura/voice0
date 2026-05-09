package com.voice0.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voice0.app.data.TokenBalance
import com.voice0.app.solana.Balances
import com.voice0.app.ui.theme.LightSurface
import com.voice0.app.ui.theme.LightTextMuted
import com.voice0.app.ui.theme.LightTextPrimary
import com.voice0.app.ui.theme.LightTextSecondary

@Composable
fun BalanceStrip(balances: List<TokenBalance>, prices: Map<String, Double>) {
    if (balances.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        balances.forEach { b ->
            val usd = prices[b.mint]?.let { it * b.balance }
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(LightSurface)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(b.symbol, color = LightTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text(Balances.formatBalance(b.balance), color = LightTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (usd != null) {
                    Text("$" + if (usd >= 1) "%.2f".format(usd) else "%.4f".format(usd), color = LightTextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}
