package com.voice0.app.data

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * Convert a human amount (e.g. "0.5", "1234.567") to base units as BigInteger
 * given the token decimals. Throws if the amount has more fractional digits
 * than the token allows (no silent precision loss).
 */
object Amount {
    private val numericRegex = Regex("""^\d+(\.\d+)?$""")

    fun toBaseUnits(amount: String, decimals: Int): BigInteger {
        require(numericRegex.matches(amount)) { "Invalid numeric amount: $amount" }
        val bd = BigDecimal(amount)
        val scaled = bd.movePointRight(decimals)
        require(scaled.scale() <= 0 || scaled.stripTrailingZeros().scale() <= 0) {
            "Amount $amount has more fractional digits than $decimals decimals allow"
        }
        return scaled.setScale(0, RoundingMode.UNNECESSARY).toBigInteger()
    }

    fun toBaseUnits(amount: Double, decimals: Int): BigInteger =
        toBaseUnits(BigDecimal.valueOf(amount).toPlainString(), decimals)

    fun fromBaseUnits(base: BigInteger, decimals: Int): String =
        BigDecimal(base).movePointLeft(decimals).stripTrailingZeros().toPlainString()
}
