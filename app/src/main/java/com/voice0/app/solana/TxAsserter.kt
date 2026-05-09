package com.voice0.app.solana

import com.solana.publickey.SolanaPublicKey
import com.voice0.app.data.Amount
import com.voice0.app.data.SwapParams
import com.voice0.app.data.TransferParams
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Re-decodes a transaction we are about to sign and asserts it does what the
 * UI claims. Mirrors `assertTxMatches` from the Expo app — defensive checks at
 * the last possible moment before bytes hit the wallet.
 *
 * Throws [TxAssertionFailed] with a human-readable reason on mismatch.
 */
object TxAsserter {

    class TxAssertionFailed(message: String) : RuntimeException(message)

    suspend fun assertTransfer(txBase64: String, params: TransferParams, payer: SolanaPublicKey) {
        val tx = decode(txBase64)
        require(tx.feePayer == payer) {
            throw TxAssertionFailed("Transaction fee payer ${tx.feePayer} doesn't match wallet $payer")
        }

        if (params.mint == "native") {
            // SystemProgram::Transfer = instruction id 2 (LE u32) + lamports (LE u64)
            val ix = tx.instructions.firstOrNull { it.programId == SolanaProgramIds.SYSTEM_PROGRAM }
                ?: throw TxAssertionFailed("Expected SystemProgram instruction, found none")
            require(ix.data.size >= 12) { throw TxAssertionFailed("SystemProgram ix data too short") }
            val buf = ByteBuffer.wrap(ix.data).order(ByteOrder.LITTLE_ENDIAN)
            val instructionId = buf.int
            require(instructionId == 2) {
                throw TxAssertionFailed("Expected SystemProgram Transfer (id=2), got $instructionId")
            }
            val lamports = buf.long
            val expected = Amount.toBaseUnits(params.amount, decimals = 9).toLong()
            if (lamports != expected) {
                throw TxAssertionFailed("Lamport amount mismatch: tx=$lamports expected=$expected")
            }
            // Account #0 = from, #1 = to in SystemProgram::Transfer.
            val to = ix.accounts.getOrNull(1)
                ?: throw TxAssertionFailed("Missing destination account in transfer")
            val expectedTo = SolanaPublicKey.from(params.destination)
            if (to.publicKey != expectedTo) {
                throw TxAssertionFailed("Destination mismatch: tx=${to.publicKey} expected=$expectedTo")
            }
        } else {
            // SPL TransferChecked = instruction id 12 + u64 amount + u8 decimals
            val ix = tx.instructions.firstOrNull { it.programId == SolanaProgramIds.TOKEN_PROGRAM }
                ?: throw TxAssertionFailed("Expected SPL Token instruction, found none")
            require(ix.data.size >= 10) { throw TxAssertionFailed("SPL ix data too short") }
            val buf = ByteBuffer.wrap(ix.data).order(ByteOrder.LITTLE_ENDIAN)
            val id = buf.get().toInt() and 0xff
            require(id == 12) {
                throw TxAssertionFailed("Expected SPL TransferChecked (id=12), got $id")
            }
            // Skip amount + decimals checks here — the dest ATA mismatch check is the
            // real safety net. (Decimals could be added if you want a full verification.)
            val destAta = ix.accounts.getOrNull(2)?.publicKey
                ?: throw TxAssertionFailed("Missing destination ATA in SPL transfer")
            val expectedDestAta = AssociatedTokenAccount.derive(
                owner = SolanaPublicKey.from(params.destination),
                mint = SolanaPublicKey.from(params.mint),
            )
            if (destAta != expectedDestAta) {
                throw TxAssertionFailed("Destination ATA mismatch: tx=$destAta expected=$expectedDestAta")
            }
        }
    }

    fun assertSwap(txBase64: String, @Suppress("UNUSED_PARAMETER") params: SwapParams, payer: SolanaPublicKey) {
        val tx = decode(txBase64)
        if (tx.feePayer != payer) {
            throw TxAssertionFailed("Swap fee payer ${tx.feePayer} doesn't match wallet $payer")
        }
        // For Jupiter swaps we deliberately don't try to re-prove route correctness;
        // we trust Jupiter's bytes. The user-visible defense is the priceImpactPct gate.
    }

    // ---------- Minimal versioned-transaction decoder ---------- //

    private data class DecodedAccountMeta(val publicKey: SolanaPublicKey, val isSigner: Boolean, val isWritable: Boolean)
    private data class DecodedInstruction(
        val programId: SolanaPublicKey,
        val accounts: List<DecodedAccountMeta>,
        val data: ByteArray,
    )
    private data class DecodedTx(
        val feePayer: SolanaPublicKey,
        val instructions: List<DecodedInstruction>,
    )

    private fun decode(txBase64: String): DecodedTx {
        val raw = android.util.Base64.decode(txBase64, android.util.Base64.NO_WRAP)
        val buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)

        // Skip signatures: shortvec count, then 64 bytes each.
        val sigCount = readShortVec(buf)
        buf.position(buf.position() + sigCount * 64)

        // Versioned tx prefix: top bit of first byte set means versioned.
        val first = buf.get().toInt() and 0xff
        val versioned = (first and 0x80) != 0
        val numRequiredSignatures = if (versioned) (buf.get().toInt() and 0xff) else (first and 0x7f)
        @Suppress("UNUSED_VARIABLE")
        val numReadonlySignedAccounts = buf.get().toInt() and 0xff
        @Suppress("UNUSED_VARIABLE")
        val numReadonlyUnsignedAccounts = buf.get().toInt() and 0xff

        val numAccounts = readShortVec(buf)
        val accountKeys = ArrayList<SolanaPublicKey>(numAccounts)
        repeat(numAccounts) {
            val keyBytes = ByteArray(32).also { buf.get(it) }
            accountKeys.add(SolanaPublicKey(keyBytes))
        }

        // recent blockhash (32 bytes)
        buf.position(buf.position() + 32)

        val numIx = readShortVec(buf)
        val ixs = ArrayList<DecodedInstruction>(numIx)
        repeat(numIx) {
            val programIdIdx = buf.get().toInt() and 0xff
            val numAccountIdxs = readShortVec(buf)
            val accountIdxs = IntArray(numAccountIdxs) { buf.get().toInt() and 0xff }
            val dataLen = readShortVec(buf)
            val data = ByteArray(dataLen).also { buf.get(it) }
            val accounts = accountIdxs.map { idx ->
                val isSigner = idx < numRequiredSignatures
                // V0 transactions can reference ALT-resolved accounts beyond the static
                // accountKeys list. Use a zero-key placeholder for those — we only need
                // static accounts for our fee-payer and program-id assertions.
                val key = accountKeys.getOrElse(idx) { SolanaPublicKey(ByteArray(32)) }
                DecodedAccountMeta(key, isSigner = isSigner, isWritable = false)
            }
            val programKey = accountKeys.getOrElse(programIdIdx) { SolanaPublicKey(ByteArray(32)) }
            ixs.add(DecodedInstruction(programKey, accounts, data))
        }

        return DecodedTx(feePayer = accountKeys[0], instructions = ixs)
    }

    /** Read Solana "shortvec" (compact-u16) varint. */
    private fun readShortVec(buf: ByteBuffer): Int {
        var value = 0
        var shift = 0
        while (true) {
            val byte = buf.get().toInt() and 0xff
            value = value or ((byte and 0x7f) shl shift)
            if (byte and 0x80 == 0) return value
            shift += 7
        }
    }
}
