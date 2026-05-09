package com.voice0.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voice0.app.data.AppPhase
import com.voice0.app.data.Cluster
import com.voice0.app.ui.components.AppDivider
import com.voice0.app.ui.components.NetworkToggle
import com.voice0.app.ui.components.PortfolioCard
import com.voice0.app.ui.components.SectionLabel
import com.voice0.app.ui.components.TokenRow
import com.voice0.app.ui.components.VoiceButton
import com.voice0.app.ui.theme.Accent
import com.voice0.app.ui.theme.AccentLight
import com.voice0.app.ui.theme.Bg
import com.voice0.app.ui.theme.Danger
import com.voice0.app.ui.theme.DangerDeep
import com.voice0.app.ui.theme.Outline
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
) {
    when (state.phase) {
        AppPhase.REVIEWING -> ReviewScreen(state, onConfirm, onReset)
        AppPhase.EXECUTING -> ExecutionScreen(state)
        AppPhase.DONE -> SuccessScreen(state, onReset)
        AppPhase.BALANCE_RESULT -> BalanceScreen(state, onReset)
        else -> IdleScreen(
            state = state,
            onCluster = onCluster,
            onConnectWallet = onConnectWallet,
            onPressIn = onPressIn,
            onPressOut = onPressOut,
            onSubmitText = onSubmitText,
            onReset = onReset,
        )
    }
}

@Composable
private fun IdleScreen(
    state: HomeViewModel.UiState,
    onCluster: (Cluster) -> Unit,
    onConnectWallet: () -> Unit,
    onPressIn: () -> Unit,
    onPressOut: () -> Unit,
    onSubmitText: (String) -> Unit,
    onReset: () -> Unit,
) {
    var typed by remember { mutableStateOf("") }
    var showInput by remember { mutableStateOf(false) }

    val totalUsd = state.balances.sumOf { b -> (state.prices[b.mint] ?: 0.0) * b.balance }
    val walletConnected = state.walletPubkeyBase58 != null
    val hasBalances = state.balances.isNotEmpty()
    val isIdle = state.phase == AppPhase.IDLE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .systemBarsPadding(),
    ) {
        AppHeader(state.cluster, state.walletPubkeyBase58, onCluster, onConnectWallet)
        AppDivider()

        // Scrollable content area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            if (walletConnected && hasBalances) {
                PortfolioCard(
                    totalUsd = totalUsd,
                    walletPubkey = state.walletPubkeyBase58,
                )
                Spacer(Modifier.height(20.dp))
                SectionLabel("Tokens", Modifier.padding(bottom = 10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.balances.forEach { b ->
                        TokenRow(b, state.prices[b.mint])
                    }
                }
                Spacer(Modifier.height(24.dp))
            } else if (!walletConnected) {
                ConnectPrompt(onConnectWallet)
                Spacer(Modifier.height(24.dp))
            }

            if (isIdle || state.phase == AppPhase.ERROR) {
                SectionLabel("Try saying", Modifier.padding(bottom = 10.dp))
                HintChips { hint ->
                    onSubmitText(hint)
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        AppDivider()

        // Fixed bottom action dock — always visible
        ActionDock(
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
        Text(
            "voice0",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NetworkToggle(cluster, onCluster)
            WalletPill(walletPubkey, onConnectWallet)
        }
    }
}

@Composable
private fun ActionDock(
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
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AnimatedContent(
            targetState = state.phase to state.error,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            label = "dockStatus",
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
                    color = AccentLight,
                    fontSize = 12.sp,
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

@Composable
private fun ConnectPrompt(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, Outline, RoundedCornerShape(12.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Connect wallet",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Connect a Solana wallet to view your portfolio and execute voice commands.",
            color = TextMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp,
        )
        Text(
            "Connect",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Accent)
                .clickable { onClick() }
                .padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun HintChips(onSelect: (String) -> Unit) {
    val hints = listOf(
        "Check my balance",
        "Swap 0.5 SOL to USDC",
        "Swap 100 USDC to SOL",
        "Swap 0.1 SOL to BONK",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        hints.forEach { hint ->
            Text(
                hint,
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceHigh)
                    .border(1.dp, Outline, RoundedCornerShape(8.dp))
                    .clickable { onSelect(hint) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun StatusRow(
    dot: androidx.compose.ui.graphics.Color = Accent,
    label: String,
    color: androidx.compose.ui.graphics.Color = TextSecondary,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dot),
        )
        Text(label, color = color, fontSize = 14.sp)
    }
}

@Composable
private fun WalletPill(pubkey: String?, onClick: () -> Unit) {
    val label = pubkey?.let { "${it.take(4)}…${it.takeLast(4)}" } ?: "Connect"
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Surface)
            .border(1.dp, Outline, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (pubkey != null) Success else TextMuted),
        )
        Text(
            label,
            color = if (pubkey != null) TextSecondary else TextMuted,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ErrorBlock(message: String?, onReset: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DangerDeep)
                .border(1.dp, Danger.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                .padding(14.dp),
        ) {
            Text(
                message ?: "Unknown error",
                color = Danger,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
        Text(
            "Try again",
            color = AccentLight,
            fontSize = 13.sp,
            modifier = Modifier.clickable { onReset() },
        )
    }
}

@Composable
private fun TextEditor(
    value: String,
    onChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceHigh)
            .border(1.dp, Outline, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, lineHeight = 22.sp),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text("e.g. swap 0.5 SOL to USDC", color = TextHint, fontSize = 15.sp)
                }
                inner()
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                "Cancel",
                color = TextMuted,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceLow)
                    .border(1.dp, Outline, RoundedCornerShape(8.dp))
                    .clickable { onCancel() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
            Text(
                "Submit",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Accent)
                    .clickable { onSubmit() }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }
    }
}
