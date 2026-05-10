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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voice0.app.data.TxRecord
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
import com.voice0.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    records: List<TxRecord>,
    onBack: () -> Unit,
    onClear: () -> Unit,
) {
    val ctx = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().background(LightBg).systemBarsPadding(),
    ) {
        // Dark header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(Bg)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("← Back", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onBack() })
            Text("History", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (records.isNotEmpty()) {
                Text("Clear", color = TextMuted, fontSize = 13.sp, modifier = Modifier.clickable { onClear() })
            } else {
                Box(Modifier)
            }
        }

        if (records.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No transactions yet", color = LightTextMuted, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text("Completed swaps and transfers will appear here.", color = LightTextMuted.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { Spacer(Modifier.height(16.dp)) }
                items(records, key = { it.id }) { record ->
                    TxRecordCard(record) { sig ->
                        val url = if (record.cluster == "Devnet") "https://solscan.io/tx/$sig?cluster=devnet" else "https://solscan.io/tx/$sig"
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun TxRecordCard(record: TxRecord, onViewSig: (String) -> Unit) {
    val fmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    val dateStr = fmt.format(Date(record.timestampMs))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LightSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Success.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Success, modifier = Modifier.size(20.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(record.intent, color = LightTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp)
                if (record.steps.size > 1) {
                    record.steps.forEach { step -> Text("· $step", color = LightTextSecondary, fontSize = 12.sp) }
                }
            }
            Text(
                record.cluster, color = LightTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(LightBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(LightOutline))

        record.signatures.forEach { sig ->
            val short = "${sig.take(18)}…${sig.takeLast(6)}"
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(short, color = LightTextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("Solscan ↗", color = LightTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onViewSig(sig) })
            }
        }

        Text(dateStr, color = LightTextMuted, fontSize = 11.sp)
    }
}
