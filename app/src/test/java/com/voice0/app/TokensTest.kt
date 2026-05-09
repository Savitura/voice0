package com.voice0.app

import com.voice0.app.data.Tokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TokensTest {
    @Test fun `BONK has 5 decimals`() {
        assertEquals(5, Tokens.bySymbol("BONK")?.decimals)
    }

    @Test fun `lookup by symbol case insensitive`() {
        assertNotNull(Tokens.bySymbol("usdc"))
        assertNotNull(Tokens.bySymbol("USDC"))
        assertNull(Tokens.bySymbol("XYZ"))
    }

    @Test fun `symbolFromMint maps native to SOL`() {
        assertEquals("SOL", Tokens.symbolFromMint("native"))
    }

    @Test fun `unknown mint returns UNKNOWN`() {
        assertEquals("UNKNOWN", Tokens.symbolFromMint("BadMintAddressxxxxxxxxxxxxxxxxxxxxxxxxx"))
    }
}
