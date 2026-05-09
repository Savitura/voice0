package com.voice0.app.solana

import com.voice0.app.data.TokenBalance
import com.voice0.app.data.Tokens
import com.voice0.app.network.HeliusRpc
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object Balances {
    suspend fun fetch(rpc: HeliusRpc, ownerPubkeyBase58: String): List<TokenBalance> {
        val out = mutableListOf<TokenBalance>()

        val lamports = rpc.getBalanceLamports(ownerPubkeyBase58)
        out.add(TokenBalance("SOL", "native", lamports / 1_000_000_000.0, 9))

        val resp = rpc.getParsedTokenAccountsByOwner(ownerPubkeyBase58)
        val value = (resp.jsonObject["value"] as? JsonArray) ?: return out
        for (entry in value) {
            val info = entry.jsonObject["account"]?.jsonObject
                ?.get("data")?.jsonObject
                ?.get("parsed")?.jsonObject
                ?.get("info")?.jsonObject ?: continue
            val mint = info["mint"]?.jsonPrimitive?.content ?: continue
            val ui = info["tokenAmount"]?.jsonObject?.get("uiAmount")?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
            if (ui <= 0.0) continue
            val token = Tokens.byMint(mint) ?: continue
            out.add(TokenBalance(token.symbol, mint, ui, token.decimals))
        }
        return out
    }

    fun formatBalance(balance: Double): String = when {
        balance == 0.0 -> "0"
        balance >= 1_000_000.0 -> "%.1fM".format(balance / 1_000_000.0)
        balance >= 1_000.0 -> "%.1fK".format(balance / 1_000.0)
        balance >= 1.0 -> "%.2f".format(balance)
        balance >= 0.001 -> "%.4f".format(balance)
        else -> "%.3g".format(balance)
    }
}
