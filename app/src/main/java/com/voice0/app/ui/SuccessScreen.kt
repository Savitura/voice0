package com.voice0.app.ui

import android.content.Intent
import android.net.Uri
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
import com.voice0.app.ui.theme.Accent
import com.voice0.app.ui.theme.AccentLight
import com.voice0.app.ui.theme.Bg
import com.voice0.app.ui.theme.Outline
import com.voice0.app.ui.theme.OutlineLow
import com.voice0.app.ui.theme.Success
import com.voice0.app.ui.theme.SuccessDeep
import com.voice0.app.ui.theme.Surface
import com.voice0.app.ui.theme.SurfaceLow
import com.voice0.app.ui.theme.TextMuted
import com.voice0.app.ui.theme.TextPrimary
import com.voice0.app.ui.theme.TextSecondary
import com.voice0.app.viewmodel.HomeViewModel

@Composable
fun SuccessScreen(state: HomeViewModel.UiState, onNew: () -> Unit) {
    val ctx = LocalContext.current
    val bundle = state.bundle

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .systemBarsPadding()
            .padding(28.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SuccessDeep)
                .border(1.5.dp, Success.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("✓", color = Success, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }

        Box(Modifier.size(24.dp))

        Text(
            "Transaction sent",
            color = TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        )

        Box(Modifier.size(6.dp))

        Text(
            bundle?.steps?.joinToString(" · ") { it.humanSummary } ?: "",
            color = TextMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )

        Box(Modifier.size(32.dp))

        if (state.signatures.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Signatures",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                )
                state.signatures.forEach { sig ->
                    SigCard(sig, state.cluster, ctx)
                }
            }
            Box(Modifier.size(28.dp))
        }

        Text(
            "New Intent",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Accent)
                .clickable { onNew() }
                .padding(vertical = 15.dp),
        )
    }
}

@Composable
private fun SigCard(sig: String, cluster: Cluster, ctx: android.content.Context) {
    val short = if (sig.length > 28) "${sig.take(22)}…${sig.takeLast(6)}" else sig
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceLow)
            .border(1.dp, Outline, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            short,
            color = TextSecondary.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.3.sp,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(OutlineLow),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipBtn("Copy") {
                val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                    as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("signature", sig))
            }
            ChipBtn("View on Solscan ↗") {
                val url = if (cluster == Cluster.Devnet)
                    "https://solscan.io/tx/$sig?cluster=devnet"
                else
                    "https://solscan.io/tx/$sig"
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
    }
}

@Composable
private fun ChipBtn(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = AccentLight,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Surface)
            .border(1.dp, Outline, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
