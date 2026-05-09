package com.voice0.app.data

data class TokenInfo(
    val symbol: String,
    val mint: String, // "native" for SOL
    val decimals: Int,
    val name: String,
)

object Tokens {
    val SOL = TokenInfo("SOL", "native", 9, "Solana")
    val USDC = TokenInfo("USDC", "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v", 6, "USD Coin")
    val USDT = TokenInfo("USDT", "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB", 6, "Tether USD")
    val BONK = TokenInfo("BONK", "DezXAZ8z7PnrnRJjz3wXBoRgixCa6xjnB7YaB1pPB263", 5, "Bonk")
    val JUP = TokenInfo("JUP", "JUPyiwrYJFskUPiHa7hkeR8VUtAeFoSYbKedZNsDvCN", 6, "Jupiter")

    val all: List<TokenInfo> = listOf(SOL, USDC, USDT, BONK, JUP)
    private val bySymbol = all.associateBy { it.symbol.uppercase() }
    private val byMint = all.associateBy { it.mint }

    fun bySymbol(symbol: String): TokenInfo? = bySymbol[symbol.uppercase()]
    fun byMint(mint: String): TokenInfo? = byMint[mint]

    fun symbolFromMint(mint: String): String =
        if (mint == "native") "SOL" else byMint[mint]?.symbol ?: "UNKNOWN"
}
