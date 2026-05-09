package com.voice0.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voice0.app.data.AppPhase
import com.voice0.app.data.Cluster
import com.voice0.app.data.TxRecord
import com.voice0.app.ui.components.NetworkToggle
import com.voice0.app.ui.components.PortfolioCard
import com.voice0.app.ui.components.TokenRow
import com.voice0.app.ui.components.VoiceButton
import com.voice0.app.ui.theme.Bg
import com.voice0.app.ui.theme.Danger
import com.voice0.app.ui.theme.DangerDeep
import com.voice0.app.ui.theme.LightBg
import com.voice0.app.ui.theme.LightTextMuted
import com.voice0.app.ui.theme.LightTextPrimary
import com.voice0.app.ui.theme.Recording
import com.voice0.app.ui.theme.Success
import com.voice0.app.ui.theme.Surface
import com.voice0.app.ui.theme.SurfaceHigh
import com.voice0.app.ui.theme.SurfaceLow
import com.voice0.app.ui.theme.TextHint
import com.voice0.app.ui.theme.TextMuted
import com.voice0.app.ui.theme.TextPrimary
import com.voice0.app.ui.theme.TextSecondary
import com.voice0.app.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    state: HomeViewModel.UiState,
    onCluster: (Cluster) -> Unit,
    onConnectWallet: () -> Unit,
    onPressIn: () -> Unit,
    onPressOut: () -> Unit,
    onSubmitText: (String) -> Unit,
    onConfirm: () -> Unit,
    onReset: () -> Unit,
    onShowHistory: () -> Unit,
    onClearHistory: () -> Unit,
) {
    when (state.phase) {
        AppPhase.REVIEWING -> ReviewScreen(state, onConfirm, onReset)
        AppPhase.EXECUTING -> ExecutionScreen(state)
        AppPhase.DONE -> SuccessScreen(state, onReset)
        AppPhase.BALANCE_RESULT -> BalanceScreen(state, onReset)
        AppPhase.HISTORY -> HistoryScreen(state.history, onBack = onReset, onClear = onClearHistory)
        else -> IdleScreen(
            state = state,
            onCluster = onCluster,
            onConnectWallet = onConnectWallet,
            onPressIn = onPressIn,
            onPressOut = onPressOut,
            onSubmitText = onSubmitText,
            onReset = onReset,
            onShowHistory = onShowHistory,
        )
    }
}

// ─────────────────────────────────────────────────────────
// IDLE SCREEN
// Layout: LightBg fills full screen. Dark zone wraps its
// content at top. Light content zone gets remaining space.
// Dark dock is pinned at bottom.
// ─────────────────────────────────────────────────────────

@Composable
private fun IdleScreen(
    state: HomeViewModel.UiState,
    onCluster: (Cluster) -> Unit,
    onConnectWallet: () -> Unit,
    onPressIn: () -> Unit,
    onPressOut: () -> Unit,
    onSubmitText: (String) -> Unit,
    onReset: () -> Unit,
    onShowHistory: () -> Unit,
) {
    var typed by remember { mutableStateOf("") }
    var showInput by remember { mutableStateOf(false) }

    val totalUsd = state.balances.sumOf { b -> (state.prices[b.mint] ?: 0.0) * b.balance }
    val walletConnected = state.walletPubkeyBase58 != null
    val hasBalances = state.balances.isNotEmpty()
    val isIdle = state.phase == AppPhase.IDLE

    // Outer: LightBg fills screen, Column with dark-wrapping-top then light-middle then dark-dock
    Column(modifier = Modifier.fillMaxSize().background(LightBg)) {

        // ── DARK TOP ZONE (wraps content, NOT weighted) ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Bg)
                .statusBarsPadding(),
        ) {
            AppHeader(state.cluster, state.walletPubkeyBase58, onCluster, onConnectWallet)

            if (walletConnected && hasBalances) {
                PortfolioCard(totalUsd = totalUsd, walletPubkey = state.walletPubkeyBase58)
                Spacer(Modifier.height(28.dp))
                QuickActions()
                Spacer(Modifier.height(28.dp))
            } else if (!walletConnected) {
                ConnectPrompt(onConnectWallet)
            } else {
                Spacer(Modifier.height(32.dp))
            }
        }

        // ── LIGHT MIDDLE ZONE (scrollable, takes all remaining space) ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(24.dp))

                // Assets
                if (walletConnected && hasBalances) {
                    Text(
                        "Assets",
                        color = LightTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.balances.forEach { b -> TokenRow(b, state.prices[b.mint]) }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // Recent transactions
                if (state.history.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Transaction", color = LightTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "See All",
                            color = LightTextMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { onShowHistory() },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.history.take(3).forEach { record -> RecentRow(record, onShowHistory) }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                // Hint chips
                if (isIdle || state.phase == AppPhase.ERROR) {
                    Text("Try Saying", color = LightTextMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(10.dp))
                    HintChips { hint -> onSubmitText(hint) }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // ── DARK BOTTOM DOCK (pinned below scrollable content) ──
            BottomDock(
                state = state,
                typed = typed,
                onTypedChange = { typed = it },
                showInput = showInput,
                onShowInput = { showInput = it },
                onPressIn = onPressIn,
                onPressOut = onPressOut,
                onSubmitText = { text -> onSubmitText(text); typed = ""; showInput = false },
                onReset = onReset,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// HEADER
// ─────────────────────────────────────────────────────────

@Composable
private fun AppHeader(
    cluster: Cluster,
    walletPubkey: String?,
    onCluster: (Cluster) -> Unit,
    onConnectWallet: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Logo: dark rounded square with "V" mark, like Aivora's arrow-in-square
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceHigh),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "▼",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Text("voice0", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NetworkToggle(cluster, onCluster)
            WalletPill(walletPubkey, onConnectWallet)
        }
    }
}

// ─────────────────────────────────────────────────────────
// QUICK ACTIONS — ↑ ↓ swap in dark circles
// ─────────────────────────────────────────────────────────

@Composable
private fun QuickActions() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 60.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ActionButton(Icons.Filled.ArrowUpward, "Send")
        ActionButton(Icons.Filled.ArrowDownward, "Receive")
        ActionButton(Icons.Filled.SwapVert, "Swap")
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier.size(50.dp).clip(CircleShape).background(SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = TextPrimary, modifier = Modifier.size(20.dp))
        }
        Text(label, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────
// BOTTOM DOCK — dark bar, clips with rounded top
// ─────────────────────────────────────────────────────────

@Composable
private fun BottomDock(
    state: HomeViewModel.UiState,
    typed: String,
    onTypedChange: (String) -> Unit,
    showInput: Boolean,
    onShowInput: (Boolean) -> Unit,
    onPressIn: () -> Unit,
    onPressOut: () -> Unit,
    onSubmitText: (String) -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Bg)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnimatedContent(
            targetState = state.phase to state.error,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            label = "status",
        ) { (phase, error) ->
            when (phase) {
                AppPhase.RECORDING -> StatusRow(dot = Recording, label = "Recording…", color = Recording)
                AppPhase.TRANSCRIBING -> StatusRow(label = "Transcribing…")
                AppPhase.PARSING -> StatusRow(label = "Parsing intent…")
                AppPhase.SIMULATING -> StatusRow(label = "Simulating…")
                AppPhase.ERROR -> ErrorBlock(error?.message, onReset)
                else -> Box(Modifier.height(0.dp))
            }
        }

        VoiceButton(state.phase, onPressIn, onPressOut)

        if (state.phase == AppPhase.IDLE) {
            if (!showInput) {
                Text(
                    "or type instead",
                    color = TextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onShowInput(true) },
                )
            } else {
                TextEditor(
                    value = typed,
                    onChange = onTypedChange,
                    onSubmit = { onSubmitText(typed) },
                    onCancel = { onTypedChange(""); onShowInput(false) },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// CONNECT PROMPT
// ─────────────────────────────────────────────────────────

@Composable
private fun ConnectPrompt(onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Your Wallet", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "Connect a Solana wallet to view\nyour portfolio and execute voice commands",
            color = TextMuted, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 22.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Connect Wallet",
            color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .clickable { onClick() }
                .padding(vertical = 14.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────
// HINT CHIPS — dark pills on light background (like ref)
// ─────────────────────────────────────────────────────────

@Composable
private fun HintChips(onSelect: (String) -> Unit) {
    val hints = listOf("Check my balance", "Swap 0.5 SOL to USDC", "Swap 100 USDC to SOL", "Swap 0.1 SOL to BONK")
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        hints.forEach { hint ->
            Text(
                hint,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1C1C1E))
                    .clickable { onSelect(hint) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// RECENT ROW — dark pill card on white bg, like reference
// ─────────────────────────────────────────────────────────

@Composable
private fun RecentRow(record: TxRecord, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C1C1E))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(Success.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Success, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            record.intent,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
    }
}

// ─────────────────────────────────────────────────────────
// SMALL COMPOSABLES
// ─────────────────────────────────────────────────────────

@Composable
private fun StatusRow(dot: Color = TextSecondary, label: String, color: Color = TextSecondary) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
        Text(label, color = color, fontSize = 14.sp)
    }
}

@Composable
private fun WalletPill(pubkey: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceHigh)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(if (pubkey != null) Success else TextMuted))
        Text(
            pubkey?.let { "${it.take(4)}…${it.takeLast(4)}" } ?: "Connect",
            color = if (pubkey != null) TextSecondary else TextMuted,
            fontSize = 12.sp, fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ErrorBlock(message: String?, onReset: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(DangerDeep).padding(14.dp),
        ) {
            Text(message ?: "Unknown error", color = Danger, fontSize = 13.sp, lineHeight = 18.sp)
        }
        Text("Try again", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.clickable { onReset() })
    }
}

@Composable
private fun TextEditor(value: String, onChange: (String) -> Unit, onSubmit: () -> Unit, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BasicTextField(
            value = value, onValueChange = onChange,
            textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, lineHeight = 22.sp),
            decorationBox = { inner ->
                if (value.isEmpty()) Text("e.g. swap 0.5 SOL to USDC", color = TextHint, fontSize = 15.sp)
                inner()
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            Text(
                "Cancel", color = TextMuted, fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(SurfaceLow)
                    .clickable { onCancel() }.padding(horizontal = 14.dp, vertical = 8.dp),
            )
            Text(
                "Submit", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White)
                    .clickable { onSubmit() }.padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }
    }
}
