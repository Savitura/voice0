package com.voice0.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voice0.app.data.Cluster
import com.voice0.app.ui.theme.Accent
import com.voice0.app.ui.theme.Outline
import com.voice0.app.ui.theme.SurfaceHigh
import com.voice0.app.ui.theme.SurfaceLow
import com.voice0.app.ui.theme.TextMuted
import com.voice0.app.ui.theme.TextPrimary

@Composable
fun NetworkToggle(cluster: Cluster, onChange: (Cluster) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceLow)
            .border(1.dp, Outline, RoundedCornerShape(8.dp))
            .padding(3.dp),
    ) {
        Pill("Mainnet", cluster == Cluster.Mainnet) { onChange(Cluster.Mainnet) }
        Pill("Devnet", cluster == Cluster.Devnet) { onChange(Cluster.Devnet) }
    }
}

@Composable
private fun Pill(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) TextPrimary else TextMuted,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Accent else SurfaceLow)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 5.dp),
    )
}
