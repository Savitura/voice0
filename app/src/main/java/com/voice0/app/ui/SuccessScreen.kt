package com.voice0.app.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voice0.app.data.Cluster
import com.voice0.app.ui.theme.Bg
import com.voice0.app.ui.theme.LightBg
import com.voice0.app.ui.theme.LightOutline
import com.voice0.app.ui.theme.LightSurface
import com.voice0.app.ui.theme.LightTextMuted
import com.voice0.app.ui.theme.LightTextPrimary
import com.voice0.app.ui.theme.LightTextSecondary
import com.voice0.app.ui.theme.Success
import com.voice0.app.ui.theme.TextMuted
import com.voice0.app.ui.theme.TextPrimary
import com.voice0.app.viewmodel.HomeViewModel

@Composable
fun SuccessScreen(state: HomeViewModel.UiState, onNew: () -> Unit) {
    val ctx = LocalContext.current
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
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(Success.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("✓", color = Success, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
            Text("Transaction sent", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                bundle?.steps?.joinToString(" · ") { it.humanSummary } ?: "",
                color = TextMuted, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp,
            )
        }

        // Light bottom with signatures
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            if (state.signatures.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text("Signatures", color = LightTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                state.signatures.forEach { sig ->
                    SigCard(sig, state.cluster, ctx)
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Bottom button
        Box(
            modifier = Modifier.fillMaxWidth().background(LightSurface).padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Text(
                "New Intent", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black)
                    .clickable { onNew() }
                    .padding(vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun SigCard(sig: String, cluster: Cluster, ctx: android.content.Context) {
    val short = if (sig.length > 28) "${sig.take(22)}…${sig.takeLast(6)}" else sig
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LightSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(short, color = LightTextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Box(Modifier.fillMaxWidth().height(1.dp).background(LightOutline))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipBtn("Copy") {
                val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("signature", sig))
            }
            ChipBtn("View on Solscan ↗") {
                val url = if (cluster == Cluster.Devnet) "https://solscan.io/tx/$sig?cluster=devnet" else "https://solscan.io/tx/$sig"
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            }
        }
    }
}

@Composable
private fun ChipBtn(label: String, onClick: () -> Unit) {
    Text(
        label, color = LightTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(LightBg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}
