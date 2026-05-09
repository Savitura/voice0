package com.voice0.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voice0.app.ui.components.PortfolioCard
import com.voice0.app.ui.components.TokenRow
import com.voice0.app.ui.theme.Bg
import com.voice0.app.ui.theme.LightBg
import com.voice0.app.ui.theme.LightTextMuted
import com.voice0.app.ui.theme.LightTextPrimary
import com.voice0.app.ui.theme.TextPrimary
import com.voice0.app.ui.theme.TextSecondary
import com.voice0.app.viewmodel.HomeViewModel

@Composable
fun BalanceScreen(state: HomeViewModel.UiState, onDone: () -> Unit) {
    val balances = state.balances
    val prices = state.prices
    val totalUsd = balances.sumOf { b -> (prices[b.mint] ?: 0.0) * b.balance }

    Column(
        modifier = Modifier.fillMaxSize().background(LightBg).systemBarsPadding(),
    ) {
        // Dark header with balance
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Bg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Portfolio", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Done", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onDone() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            if (totalUsd > 0) {
                PortfolioCard(totalUsd = totalUsd, walletPubkey = state.walletPubkeyBase58)
            }
            Spacer(Modifier.height(16.dp))
        }

        if (balances.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tokens found", color = LightTextMuted, fontSize = 15.sp)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                Text("Tokens", color = LightTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp))
            }
            items(balances) { b -> TokenRow(b, prices[b.mint]) }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
