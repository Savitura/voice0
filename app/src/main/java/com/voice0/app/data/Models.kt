package com.voice0.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
sealed interface TxParams

@Serializable
@SerialName("swap")
data class SwapParams(
    val inputMint: String,
    val outputMint: String,
    val amount: Double,
    val slippageBps: Int,
) : TxParams

@Serializable
@SerialName("transfer")
data class TransferParams(
    val mint: String,        // "native" for SOL
    val amount: Double,
    val destination: String,
) : TxParams

@Serializable
enum class StepType {
    @SerialName("swap") SWAP,
    @SerialName("transfer") TRANSFER,
}

@Serializable
data class SolanaTxStep(
    val id: String,
    val type: StepType,
    val humanSummary: String,
    val params: JsonObject,             // raw params; downstream parses by type
    val priceImpactPct: Double? = null, // populated by simulator (swap only)
    val feeLamports: Long? = null,
    val simulationOk: Boolean? = null,
    val warnings: List<String> = emptyList(),
    val requiresExtraConfirm: Boolean = false,
)

@Serializable
data class SolanaTxBundle(
    val intent: String,
    val steps: List<SolanaTxStep>,
    val simulationPassed: Boolean = false,
    val estimatedFeeLamports: Long = 0,
    val warnings: List<String> = emptyList(),
    val isBalanceQuery: Boolean = false,
)

enum class AppPhase {
    IDLE, RECORDING, TRANSCRIBING, PARSING, SIMULATING,
    REVIEWING, EXECUTING, DONE, BALANCE_RESULT, ERROR,
}

data class AppError(val phase: AppPhase, val message: String)

data class TokenBalance(
    val symbol: String,
    val mint: String,
    val balance: Double,
    val decimals: Int,
)
