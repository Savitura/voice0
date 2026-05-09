package com.voice0.app.network

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

@Serializable
data class JupiterQuote(
    val inputMint: String,
    val outputMint: String,
    val inAmount: String,
    val outAmount: String,
    val otherAmountThreshold: String,
    val swapMode: String,
    val slippageBps: Int,
    val priceImpactPct: String,
    val routePlan: List<JsonElement> = emptyList(),
)

@Serializable
private data class SwapBuildRequest(
    val quoteResponse: JsonObject,
    val userPublicKey: String,
    val wrapAndUnwrapSol: Boolean = true,
    val dynamicComputeUnitLimit: Boolean = true,
    val prioritizationFeeLamports: String = "auto",
)

@Serializable
private data class SwapBuildResponse(val swapTransaction: String)

object JupiterClient {
    private const val QUOTE_URL = "https://api.jup.ag/swap/v1/quote"
    private const val SWAP_URL = "https://api.jup.ag/swap/v1/swap"

    /**
     * Returns the raw quote JsonObject (passed verbatim to /swap) and the
     * parsed JupiterQuote (used for display / price-impact checks).
     * Keeping the raw response avoids re-encoding a stripped object back to Jupiter,
     * which caused malformed transactions.
     */
    suspend fun quote(
        inputMint: String,
        outputMint: String,
        amountBaseUnits: String,
        slippageBps: Int,
    ): Pair<JsonObject, JupiterQuote> {
        val response = Net.client.get(QUOTE_URL) {
            parameter("inputMint", inputMint)
            parameter("outputMint", outputMint)
            parameter("amount", amountBaseUnits)
            parameter("slippageBps", slippageBps)
            parameter("onlyDirectRoutes", false)
        }
        if (!response.status.isSuccess()) {
            error("Jupiter quote ${response.status.value}: ${response.bodyAsText()}")
        }
        val raw: JsonObject = response.body<JsonElement>().jsonObject
        val parsed: JupiterQuote = Net.json.decodeFromJsonElement(JupiterQuote.serializer(), raw)
        return raw to parsed
    }

    /** Returns base64-encoded VersionedTransaction bytes from Jupiter. */
    suspend fun buildSwapTx(quoteRaw: JsonObject, userPublicKey: String): String {
        val response = Net.client.post(SWAP_URL) {
            contentType(ContentType.Application.Json)
            setBody(
                SwapBuildRequest(
                    quoteResponse = quoteRaw,
                    userPublicKey = userPublicKey,
                ),
            )
        }
        if (!response.status.isSuccess()) {
            error("Jupiter swap ${response.status.value}: ${response.bodyAsText()}")
        }
        return response.body<SwapBuildResponse>().swapTransaction
    }
}
