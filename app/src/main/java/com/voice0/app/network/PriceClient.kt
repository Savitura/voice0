package com.voice0.app.network

import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Fetches token prices from Jupiter's price API. Keyed by mint (or "native" for SOL).
 */
object PriceClient {
    private const val URL = "https://price.jup.ag/v6/price"

    /** @return map from mint to USD price */
    suspend fun fetch(mints: List<String>): Map<String, Double> {
        if (mints.isEmpty()) return emptyMap()
        // Jupiter expects symbols *or* mint addresses; pass mints (replace "native" with SOL mint).
        val solMint = "So11111111111111111111111111111111111111112"
        val ids = mints.joinToString(",") { if (it == "native") solMint else it }
        val res: JsonObject = Net.client.get("$URL?ids=$ids").body<JsonElement>().jsonObject
        val data = res["data"]?.jsonObject ?: return emptyMap()
        val out = mutableMapOf<String, Double>()
        for (mint in mints) {
            val key = if (mint == "native") solMint else mint
            val entry = data[key]?.jsonObject ?: continue
            val price = entry["price"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: continue
            out[mint] = price
        }
        return out
    }
}
