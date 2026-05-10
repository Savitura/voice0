package com.voice0.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.publickey.SolanaPublicKey
import com.voice0.app.audio.AudioRecorder
import com.voice0.app.data.AppError
import com.voice0.app.data.AppPhase
import com.voice0.app.data.Cluster
import com.voice0.app.data.SolanaTxBundle
import com.voice0.app.data.SolanaTxStep
import com.voice0.app.data.StepType
import com.voice0.app.data.SwapParams
import com.voice0.app.data.TokenBalance
import com.voice0.app.data.TransferParams
import com.voice0.app.data.TxHistoryRepository
import com.voice0.app.data.TxRecord
import com.voice0.app.network.ElevenLabsClient
import com.voice0.app.network.HeliusRpc
import com.voice0.app.network.PriceClient
import com.voice0.app.parser.IntentParser
import com.voice0.app.solana.Balances
import com.voice0.app.solana.Simulator
import com.voice0.app.solana.TxAsserter
import com.voice0.app.solana.TxBuilder
import com.voice0.app.wallet.WalletManager
import com.voice0.app.wallet.WalletUserCancelled
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    data class UiState(
        val phase: AppPhase = AppPhase.IDLE,
        val cluster: Cluster = Cluster.Mainnet,
        val transcript: String = "",
        val bundle: SolanaTxBundle? = null,
        val balances: List<TokenBalance> = emptyList(),
        val prices: Map<String, Double> = emptyMap(),
        val signatures: List<String> = emptyList(),
        val walletPubkeyBase58: String? = null,
        val error: AppError? = null,
        val recordingMillis: Long = 0,
        val history: List<TxRecord> = emptyList(),
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val recorder = AudioRecorder(app)
    private val wallet = WalletManager(app)
    private val parser = IntentParser()
    private val json = Json { ignoreUnknownKeys = true }
    private val txHistory = TxHistoryRepository(app)

    init {
        txHistory.records.onEach { records ->
            _state.update { it.copy(history = records) }
        }.launchIn(viewModelScope)
        // Restore wallet pubkey from encrypted prefs so the user doesn't have to
        // re-approve the wallet on every app launch.
        val cached = wallet.cachedPublicKey(_state.value.cluster)
        if (cached != null) {
            _state.update { it.copy(walletPubkeyBase58 = cached) }
            viewModelScope.launch { refreshBalancesAndPrices(cached) }
        }
    }

    private fun rpc() = HeliusRpc(_state.value.cluster)
    private fun txBuilder() = TxBuilder(rpc(), _state.value.cluster)

    fun setCluster(c: Cluster) {
        if (_state.value.phase != AppPhase.IDLE && _state.value.phase != AppPhase.REVIEWING &&
            _state.value.phase != AppPhase.DONE && _state.value.phase != AppPhase.ERROR
        ) return
        // Restore cached pubkey for the target cluster — don't wipe an existing session
        val cached = wallet.cachedPublicKey(c)
        _state.update {
            it.copy(
                cluster = c,
                balances = emptyList(),
                walletPubkeyBase58 = cached,
            )
        }
        if (cached != null) {
            viewModelScope.launch { refreshBalancesAndPrices(cached) }
        }
    }

    fun startRecording() {
        if (_state.value.phase != AppPhase.IDLE) return
        try {
            recorder.start()
            _state.update { it.copy(phase = AppPhase.RECORDING, recordingMillis = 0) }
        } catch (e: Exception) {
            _state.update { it.copy(phase = AppPhase.ERROR, error = AppError(AppPhase.RECORDING, e.message ?: "Mic error")) }
        }
    }

    fun stopRecordingAndProcess(sender: ActivityResultSender) {
        if (_state.value.phase != AppPhase.RECORDING) return
        viewModelScope.launch {
            val file = try {
                recorder.stop()
            } catch (e: Exception) {
                _state.update { it.copy(phase = AppPhase.ERROR, error = AppError(AppPhase.RECORDING, e.message ?: "Stop failed")) }
                return@launch
            }
            _state.update { it.copy(phase = AppPhase.TRANSCRIBING) }
            try {
                val text = ElevenLabsClient.transcribe(file)
                file.delete()
                _state.update { it.copy(transcript = text) }
                runIntentPipeline(sender, text)
            } catch (e: Exception) {
                _state.update { it.copy(phase = AppPhase.ERROR, error = AppError(AppPhase.TRANSCRIBING, e.message ?: "STT failed")) }
            }
        }
    }

    fun submitTextIntent(sender: ActivityResultSender, text: String) {
        if (_state.value.phase != AppPhase.IDLE) return
        viewModelScope.launch {
            _state.update { it.copy(transcript = text, phase = AppPhase.TRANSCRIBING) }
            runIntentPipeline(sender, text)
        }
    }

    private suspend fun runIntentPipeline(sender: ActivityResultSender, text: String) {
        try {
            // Connect wallet lazily.
            val pubkeyBase58 = _state.value.walletPubkeyBase58 ?: run {
                val session = wallet.connect(sender, _state.value.cluster)
                _state.update { it.copy(walletPubkeyBase58 = session.publicKeyBase58) }
                refreshBalancesAndPrices(session.publicKeyBase58)
                session.publicKeyBase58
            }

            _state.update { it.copy(phase = AppPhase.PARSING) }
            val parsed = parser.parse(text)

            if (parsed.isBalanceQuery) {
                refreshBalancesAndPrices(pubkeyBase58)
                _state.update { it.copy(phase = AppPhase.BALANCE_RESULT) }
                return
            }

            _state.update { it.copy(phase = AppPhase.SIMULATING) }
            val payer = SolanaPublicKey.from(pubkeyBase58)
            val sim = Simulator(rpc(), txBuilder()).simulate(parsed, payer)

            _state.update { it.copy(bundle = sim, phase = AppPhase.REVIEWING) }
            refreshPrices(sim)
        } catch (e: Exception) {
            _state.update { it.copy(phase = AppPhase.ERROR, error = AppError(_state.value.phase, e.message ?: "Pipeline error")) }
        }
    }

    fun confirmAndSign(sender: ActivityResultSender) {
        val bundle = _state.value.bundle ?: return
        val pubkey = _state.value.walletPubkeyBase58 ?: return
        viewModelScope.launch {
            _state.update { it.copy(phase = AppPhase.EXECUTING) }
            try {
                val payer = SolanaPublicKey.from(pubkey)
                val builder = txBuilder()
                val txs = mutableListOf<String>()
                for (step in bundle.steps) {
                    val tx = buildAndAssert(step, payer, builder)
                    txs += tx
                }
                val sigs = wallet.signAndSend(sender, _state.value.cluster, txs)
                _state.update { it.copy(phase = AppPhase.DONE, signatures = sigs) }
                refreshBalancesAndPrices(pubkey)
                val b = _state.value.bundle
                if (b != null && sigs.isNotEmpty()) {
                    txHistory.add(
                        TxRecord(
                            id = UUID.randomUUID().toString(),
                            timestampMs = System.currentTimeMillis(),
                            cluster = _state.value.cluster.name,
                            intent = b.intent,
                            steps = b.steps.map { it.humanSummary },
                            signatures = sigs,
                        )
                    )
                }
            } catch (e: WalletUserCancelled) {
                _state.update { it.copy(phase = AppPhase.REVIEWING) }
            } catch (e: Exception) {
                _state.update { it.copy(phase = AppPhase.ERROR, error = AppError(AppPhase.EXECUTING, e.message ?: "Execution failed")) }
            }
        }
    }

    private suspend fun buildAndAssert(step: SolanaTxStep, payer: SolanaPublicKey, builder: TxBuilder): String {
        return when (step.type) {
            StepType.TRANSFER -> {
                val params = json.decodeFromJsonElement(TransferParams.serializer(), step.params)
                val tx = builder.buildTransfer(params, payer)
                TxAsserter.assertTransfer(tx, params, payer)
                tx
            }
            StepType.SWAP -> {
                val params = json.decodeFromJsonElement(SwapParams.serializer(), step.params)
                val (tx, _) = builder.buildSwap(params, payer)
                TxAsserter.assertSwap(tx, params, payer)
                tx
            }
        }
    }

    fun resetToIdle() {
        _state.update {
            it.copy(
                phase = AppPhase.IDLE,
                error = null,
                transcript = "",
                bundle = null,
                signatures = emptyList(),
            )
        }
    }

    fun showHistory() {
        _state.update { it.copy(phase = AppPhase.HISTORY) }
    }

    fun clearHistory() {
        viewModelScope.launch { txHistory.clear() }
    }

    fun connectWallet(sender: ActivityResultSender) {
        viewModelScope.launch {
            try {
                val session = wallet.connect(sender, _state.value.cluster)
                _state.update { it.copy(walletPubkeyBase58 = session.publicKeyBase58) }
                refreshBalancesAndPrices(session.publicKeyBase58)
            } catch (_: Exception) {
                // user cancelled
            }
        }
    }

    private suspend fun refreshBalancesAndPrices(pubkeyBase58: String) {
        try {
            val bals = Balances.fetch(rpc(), pubkeyBase58)
            _state.update { it.copy(balances = bals) }
            val prices = PriceClient.fetch(bals.map { it.mint })
            _state.update { it.copy(prices = it.prices + prices) }
        } catch (_: Exception) {
            // non-critical
        }
    }

    private suspend fun refreshPrices(bundle: SolanaTxBundle) {
        try {
            val mints = bundle.steps.flatMap { step ->
                listOfNotNull(
                    step.params["inputMint"]?.toString()?.removeSurrounding("\""),
                    step.params["outputMint"]?.toString()?.removeSurrounding("\""),
                    step.params["mint"]?.toString()?.removeSurrounding("\""),
                )
            }.distinct()
            val p = PriceClient.fetch(mints)
            _state.update { it.copy(prices = it.prices + p) }
        } catch (_: Exception) { /* non-critical */ }
    }
}
