package com.voice0.app.solana

import com.solana.publickey.SolanaPublicKey
import com.voice0.app.data.SolanaTxBundle
import com.voice0.app.data.SolanaTxStep
import com.voice0.app.data.StepType
import com.voice0.app.data.SwapParams
import com.voice0.app.data.TransferParams
import com.voice0.app.network.HeliusRpc
import com.voice0.app.network.JupiterClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * For each step in a parsed bundle:
 *  - build the transaction client-side
 *  - simulate via RPC
 *  - estimate fee, surface price-impact warning, mark requiresExtraConfirm
 *
 * The built `txBase64` is NOT carried in the public bundle — we rebuild it again
 * at confirm time and re-assert. This object only annotates the bundle with
 * simulation results.
 */
class Simulator(
    private val rpc: HeliusRpc,
    private val txBuilder: TxBuilder,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {

    suspend fun simulate(bundle: SolanaTxBundle, payer: SolanaPublicKey): SolanaTxBundle {
        val newSteps = mutableListOf<SolanaTxStep>()
        val warnings = bundle.warnings.toMutableList()
        var totalFee = 0L
        var allOk = true

        for (step in bundle.steps) {
            try {
                val (txB64, priceImpact) = build(step, payer)
                val sim = rpc.simulateTransaction(txB64)
                if (!sim.ok) {
                    allOk = false
                    warnings += "Simulation failed for \"${step.humanSummary}\": ${sim.error}"
                }
                val fee = 5000 + sim.unitsConsumed
                totalFee += fee

                val highImpact = priceImpact != null && priceImpact > 0.05
                if (highImpact) {
                    warnings += "High price impact: ${"%.2f".format(priceImpact!! * 100)}%"
                }

                newSteps += step.copy(
                    feeLamports = fee,
                    simulationOk = sim.ok,
                    priceImpactPct = priceImpact,
                    requiresExtraConfirm = highImpact,
                )
            } catch (e: Exception) {
                allOk = false
                warnings += "Build/sim failed for \"${step.humanSummary}\": ${e.message}"
                newSteps += step.copy(simulationOk = false)
            }
        }

        return bundle.copy(
            steps = newSteps,
            simulationPassed = allOk,
            estimatedFeeLamports = totalFee,
            warnings = warnings,
        )
    }

    private suspend fun build(step: SolanaTxStep, payer: SolanaPublicKey): Pair<String, Double?> {
        return when (step.type) {
            StepType.TRANSFER -> {
                val params = json.decodeFromJsonElement(TransferParams.serializer(), step.params)
                txBuilder.buildTransfer(params, payer) to null
            }
            StepType.SWAP -> {
                val params = json.decodeFromJsonElement(SwapParams.serializer(), step.params)
                val (b64, quote) = txBuilder.buildSwap(params, payer)
                b64 to quote.priceImpactPct.toDoubleOrNull()
            }
        }
    }
}
