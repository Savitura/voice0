package com.voice0.app.network

import com.voice0.app.data.Cluster
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Minimal JSON-RPC client for Solana via Helius. We only use the handful of
 * methods the app needs: getLatestBlockhash, getBalance, getTokenAccountsByOwner,
 * getMultipleAccounts, simulateTransaction.
 */
class HeliusRpc(private val cluster: Cluster) {
    private val url = cluster.rpcUrl()

    @Serializable
    private data class RpcRequest(
        val jsonrpc: String = "2.0",
        val id: Int = 1,
        val method: String,
        val params: JsonElement,
    )

    @Serializable
    private data class RpcResponse(
        val jsonrpc: String = "2.0",
        val id: Int? = null,
        val result: JsonElement? = null,
        val error: JsonObject? = null,
    )

    private suspend fun call(method: String, params: JsonElement): JsonElement {
        check(url.isNotBlank()) {
            "RPC URL not set for $cluster (set HELIUS_RPC_URL / HELIUS_DEVNET_RPC_URL)"
        }
        val res: RpcResponse = Net.client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(RpcRequest(method = method, params = params))
        }.body()
        if (res.error != null) error("RPC $method failed: ${res.error}")
        return res.result ?: error("RPC $method returned no result")
    }

    suspend fun getLatestBlockhash(): String {
        val result = call(
            "getLatestBlockhash",
            buildJsonArray { add(buildJsonObject { put("commitment", kotlinx.serialization.json.JsonPrimitive("confirmed")) }) },
        ).jsonObject
        return result["value"]!!.jsonObject["blockhash"]!!.jsonPrimitive.content
    }

    suspend fun getBalanceLamports(pubkey: String): Long {
        val result = call(
            "getBalance",
            buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive(pubkey)) },
        ).jsonObject
        return result["value"]!!.jsonPrimitive.content.toLong()
    }

    /** Returns parsed token accounts for the SPL Token program. */
    suspend fun getParsedTokenAccountsByOwner(pubkey: String): JsonElement {
        return call(
            "getTokenAccountsByOwner",
            buildJsonArray {
                add(kotlinx.serialization.json.JsonPrimitive(pubkey))
                add(buildJsonObject { put("programId", kotlinx.serialization.json.JsonPrimitive("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA")) })
                add(buildJsonObject { put("encoding", kotlinx.serialization.json.JsonPrimitive("jsonParsed")) })
            },
        )
    }

    /** Returns the on-chain decimals for an SPL mint. */
    suspend fun getMintDecimals(mint: String): Int {
        val result = call(
            "getAccountInfo",
            buildJsonArray {
                add(kotlinx.serialization.json.JsonPrimitive(mint))
                add(buildJsonObject { put("encoding", kotlinx.serialization.json.JsonPrimitive("jsonParsed")) })
            },
        ).jsonObject
        val parsed = result["value"]!!.jsonObject["data"]!!.jsonObject["parsed"]!!.jsonObject
        return parsed["info"]!!.jsonObject["decimals"]!!.jsonPrimitive.content.toInt()
    }

    data class SimulationResult(
        val ok: Boolean,
        val unitsConsumed: Long,
        val error: String?,
        val logs: List<String>,
    )

    suspend fun simulateTransaction(txBase64: String): SimulationResult {
        val result = call(
            "simulateTransaction",
            buildJsonArray {
                add(kotlinx.serialization.json.JsonPrimitive(txBase64))
                add(
                    buildJsonObject {
                        put("encoding", kotlinx.serialization.json.JsonPrimitive("base64"))
                        put("sigVerify", kotlinx.serialization.json.JsonPrimitive(false))
                        put("replaceRecentBlockhash", kotlinx.serialization.json.JsonPrimitive(true))
                    },
                )
            },
        ).jsonObject
        val value = result["value"]!!.jsonObject
        val err = value["err"]
        val logs = (value["logs"] as? kotlinx.serialization.json.JsonArray)?.map { it.jsonPrimitive.content } ?: emptyList()
        val units = value["unitsConsumed"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
        return SimulationResult(
            ok = err == null || err is kotlinx.serialization.json.JsonNull,
            unitsConsumed = units,
            error = if (err == null || err is kotlinx.serialization.json.JsonNull) null else err.toString(),
            logs = logs,
        )
    }
}
