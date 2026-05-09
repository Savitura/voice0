package com.voice0.app.solana

import com.solana.programs.Program
import com.solana.programs.SystemProgram
import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.AccountMeta
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import com.solana.transaction.TransactionInstruction
import com.voice0.app.data.Amount
import com.voice0.app.data.SwapParams
import com.voice0.app.data.TransferParams
import com.voice0.app.network.HeliusRpc
import com.voice0.app.network.JupiterClient
import com.voice0.app.network.JupiterQuote
import com.funkatronics.encoders.Base58
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Builds Solana transactions. Designed to be called CLIENT-SIDE just before
 * signing — never trust the server to hand us bytes to sign.
 *
 * Supports:
 *  - Native SOL transfer via SystemProgram
 *  - SPL token transfer (TransferChecked)
 *  - Jupiter swap (delegates to Jupiter's swap endpoint, then asserts payer)
 */
class TxBuilder(
    private val rpc: HeliusRpc,
    private val jsonFmt: Json = Json { ignoreUnknownKeys = true },
) {

    /** Returns base64 of a serialized VersionedTransaction. */
    suspend fun buildTransfer(
        params: TransferParams,
        payer: SolanaPublicKey,
    ): String {
        val blockhash = rpc.getLatestBlockhash()
        val to = SolanaPublicKey.from(params.destination)

        val ix: TransactionInstruction = if (params.mint == "native") {
            val lamports = Amount.toBaseUnits(params.amount, decimals = 9).toLong()
            SystemProgram.transfer(payer, to, lamports)
        } else {
            val mint = SolanaPublicKey.from(params.mint)
            val decimals = rpc.getMintDecimals(params.mint)
            val baseUnits: BigInteger = Amount.toBaseUnits(params.amount, decimals)
            buildSplTransferChecked(
                payer = payer,
                destinationOwner = to,
                mint = mint,
                amount = baseUnits.toLong(),
                decimals = decimals,
            )
        }

        val message = Message.Builder()
            .addInstruction(ix)
            .setRecentBlockhash(blockhash)
            .build()

        // Compile to legacy message bytes (most wallets sign these directly).
        val tx = Transaction(message)
        return android.util.Base64.encodeToString(tx.serialize(), android.util.Base64.NO_WRAP)
    }

    /**
     * Builds a Jupiter swap. Returns base64 of the unsigned VersionedTransaction
     * Jupiter built for us, plus the original quote (for review-screen display).
     */
    suspend fun buildSwap(
        params: SwapParams,
        payer: SolanaPublicKey,
    ): Pair<String, JupiterQuote> {
        val decimals = if (params.inputMint == "native") 9 else rpc.getMintDecimals(params.inputMint)
        val amountBase = Amount.toBaseUnits(params.amount, decimals).toString()

        val quote = JupiterClient.quote(
            inputMint = params.inputMint,
            outputMint = params.outputMint,
            amountBaseUnits = amountBase,
            slippageBps = params.slippageBps,
        )
        val rawQuote: JsonObject = jsonFmt.encodeToJsonElement(JupiterQuote.serializer(), quote) as JsonObject
        val txBase64 = JupiterClient.buildSwapTx(rawQuote, payer.base58())
        return txBase64 to quote
    }

    /**
     * Manually constructs an SPL TransferChecked instruction. The destination
     * account is the *owner's* associated token account (we look it up via the
     * deterministic ATA derivation). Source ATA is derived from `payer`.
     */
    private suspend fun buildSplTransferChecked(
        payer: SolanaPublicKey,
        destinationOwner: SolanaPublicKey,
        mint: SolanaPublicKey,
        amount: Long,
        decimals: Int,
    ): TransactionInstruction {
        val sourceAta = AssociatedTokenAccount.derive(payer, mint)
        val destAta = AssociatedTokenAccount.derive(destinationOwner, mint)

        // TransferChecked layout: [u8 instruction=12][u64 amount LE][u8 decimals]
        val data = ByteBuffer.allocate(1 + 8 + 1).order(ByteOrder.LITTLE_ENDIAN).apply {
            put(12.toByte())
            putLong(amount)
            put(decimals.toByte())
        }.array()

        return TransactionInstruction(
            programId = SolanaProgramIds.TOKEN_PROGRAM,
            accounts = listOf(
                AccountMeta(sourceAta, isSigner = false, isWritable = true),
                AccountMeta(mint, isSigner = false, isWritable = false),
                AccountMeta(destAta, isSigner = false, isWritable = true),
                AccountMeta(payer, isSigner = true, isWritable = false),
            ),
            data = data,
        )
    }
}

object SolanaProgramIds {
    val SYSTEM_PROGRAM = SolanaPublicKey.from("11111111111111111111111111111111")
    val TOKEN_PROGRAM = SolanaPublicKey.from("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA")
    val ASSOCIATED_TOKEN_PROGRAM = SolanaPublicKey.from("ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL")
}

/**
 * Derives the canonical associated-token-account address for (owner, mint).
 * Uses Solana's PDA-derivation rules: seeds = [owner, TOKEN_PROGRAM, mint].
 */
object AssociatedTokenAccount {
    suspend fun derive(owner: SolanaPublicKey, mint: SolanaPublicKey): SolanaPublicKey {
        return Program.findDerivedAddress(
            seeds = listOf(
                owner.bytes,
                SolanaProgramIds.TOKEN_PROGRAM.bytes,
                mint.bytes,
            ),
            programId = SolanaProgramIds.ASSOCIATED_TOKEN_PROGRAM,
        ).getOrThrow()
    }
}

private fun SolanaPublicKey.base58(): String = Base58.encodeToString(bytes)
