package com.voice0.app

import com.voice0.app.data.Amount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigInteger

class AmountTest {
    @Test fun `1 SOL is 1e9 lamports`() {
        assertEquals(BigInteger.valueOf(1_000_000_000), Amount.toBaseUnits("1", 9))
    }

    @Test fun `BONK with 5 decimals`() {
        // 1234.5 BONK = 123_450_000 base units
        assertEquals(BigInteger.valueOf(123_450_000), Amount.toBaseUnits("1234.5", 5))
    }

    @Test fun `USDC with 6 decimals partial`() {
        assertEquals(BigInteger.valueOf(50_000_000), Amount.toBaseUnits("50", 6))
        assertEquals(BigInteger.valueOf(123_456), Amount.toBaseUnits("0.123456", 6))
    }

    @Test fun `rejects more fractional digits than decimals`() {
        assertThrows(IllegalArgumentException::class.java) {
            Amount.toBaseUnits("0.1234567", 6)
        }
    }

    @Test fun `rejects non-numeric`() {
        assertThrows(IllegalArgumentException::class.java) {
            Amount.toBaseUnits("abc", 6)
        }
    }

    @Test fun `roundtrip`() {
        val s = "1.23"
        val b = Amount.toBaseUnits(s, 6)
        assertEquals(s, Amount.fromBaseUnits(b, 6))
    }
}
