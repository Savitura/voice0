package com.voice0.app

import com.voice0.app.solana.Balances
import org.junit.Assert.assertEquals
import org.junit.Test

class BalancesFormatTest {
    @Test fun `zero formats as 0`() {
        assertEquals("0", Balances.formatBalance(0.0))
    }

    @Test fun `millions get M suffix`() {
        assertEquals("1.2M", Balances.formatBalance(1_234_000.0))
    }

    @Test fun `thousands get K suffix`() {
        assertEquals("5.4K", Balances.formatBalance(5_432.0))
    }

    @Test fun `between 1 and 1000 uses 2 decimals`() {
        assertEquals("12.34", Balances.formatBalance(12.34))
    }

    @Test fun `tiny amounts use precision`() {
        // formatBalance returns "%.4f" for >= 0.001
        assertEquals("0.0050", Balances.formatBalance(0.005))
    }
}
