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
import com.voice0.app.ui.components.AppDivider
import com.voice0.app.ui.components.PortfolioCard
import com.voice0.app.ui.components.SectionLabel
import com.voice0.app.ui.components.TokenRow
import com.voice0.app.ui.theme.AccentLight
import com.voice0.app.ui.theme.Bg
import com.voice0.app.ui.theme.Outline
import com.voice0.app.ui.theme.SurfaceLow
import com.voice0.app.ui.theme.TextMuted
import com.voice0.app.ui.theme.TextPrimary
import com.voice0.app.viewmodel.HomeViewModel

@Composable
fun BalanceScreen(state: HomeViewModel.UiState, onDone: () -> Unit) {
    val balances = state.balances
    val prices = state.prices
    val totalUsd = balances.sumOf { b -> (prices[b.mint] ?: 0.0) * b.balance }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .systemBarsPadding(),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Portfolio",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            )
            Text(
                "Done",
                color = AccentLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceLow)
                    .border(1.dp, Outline, RoundedCornerShape(8.dp))
                    .clickable { onDone() }
                    .padding(horizontal = 16.dp, vertical = 7.dp),
            )
        }

        AppDivider()

        if (balances.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("No tokens found", color = TextMuted, fontSize = 14.sp)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Box(Modifier.height(16.dp))
                if (totalUsd > 0) {
                    PortfolioCard(
                        totalUsd = totalUsd,
                        walletPubkey = state.walletPubkeyBase58,
                    )
                }
            }
            item {
                Box(Modifier.height(8.dp))
                SectionLabel("Tokens", Modifier.padding(bottom = 2.dp))
            }
            items(balances) { b ->
                TokenRow(b, prices[b.mint])
            }
            item { Box(Modifier.height(16.dp)) }
        }
    }
}
