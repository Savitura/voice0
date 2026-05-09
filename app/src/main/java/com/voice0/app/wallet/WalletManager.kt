package com.voice0.app.wallet

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.RpcCluster
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.voice0.app.data.Cluster
import com.funkatronics.encoders.Base58

/**
 * Mobile Wallet Adapter wrapper. Caches the auth token in EncryptedSharedPreferences
 * so a second connect after app restart skips the wallet's UI.
 */
class WalletManager(context: Context) {

    private val prefs = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "voice0_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val identity = ConnectionIdentity(
        identityUri = android.net.Uri.parse("https://voice0.app"),
        iconUri = android.net.Uri.parse("favicon.ico"),
        identityName = "voice0",
    )

    data class Session(val publicKey: ByteArray, val accountLabel: String?, val cluster: Cluster) {
        val publicKeyBase58: String get() = Base58.encodeToString(publicKey)
    }

    private fun adapter(cluster: Cluster): MobileWalletAdapter {
        return MobileWalletAdapter(
            connectionIdentity = identity,
        )
    }

    private fun authKey(cluster: Cluster) = "auth_token_${cluster.name}"

    suspend fun connect(sender: ActivityResultSender, cluster: Cluster): Session {
        val mwa = adapter(cluster)
        val rpcCluster = when (cluster) {
            Cluster.Mainnet -> RpcCluster.MainnetBeta
            Cluster.Devnet -> RpcCluster.Devnet
        }
        val authToken = prefs.getString(authKey(cluster), null)
        val result = mwa.transact(sender) { _ ->
            if (authToken == null) {
                authorize(identity.identityUri, identity.iconUri, identity.identityName, rpcCluster)
            } else {
                reauthorize(identity.identityUri, identity.iconUri, identity.identityName, authToken)
            }
        }
        return when (result) {
            is TransactionResult.Success -> {
                val auth = result.authResult
                prefs.edit().putString(authKey(cluster), auth.authToken).apply()
                val account = auth.accounts.first()
                Session(
                    publicKey = account.publicKey,
                    accountLabel = account.accountLabel,
                    cluster = cluster,
                )
            }
            is TransactionResult.NoWalletFound ->
                error("No MWA-compatible wallet installed. Install Phantom, Solflare, or Backpack.")
            is TransactionResult.Failure ->
                error("Wallet authorization failed: ${result.e.message}")
        }
    }

    /**
     * Sign and send a list of base64-encoded VersionedTransactions through the
     * connected wallet. Returns base58 signatures.
     */
    suspend fun signAndSend(
        sender: ActivityResultSender,
        cluster: Cluster,
        txsBase64: List<String>,
    ): List<String> {
        require(txsBase64.isNotEmpty()) { "No transactions to sign" }
        val mwa = adapter(cluster)
        val txBytes = txsBase64.map { android.util.Base64.decode(it, android.util.Base64.NO_WRAP) }
        val authToken = prefs.getString(authKey(cluster), null)
            ?: error("Not authorized. Call connect() first.")

        val result = mwa.transact(sender) { _ ->
            reauthorize(identity.identityUri, identity.iconUri, identity.identityName, authToken)
            signAndSendTransactions(txBytes.toTypedArray())
        }
        return when (result) {
            is TransactionResult.Success -> result.payload.signatures.map { Base58.encodeToString(it) }
            is TransactionResult.NoWalletFound -> error("No MWA-compatible wallet installed")
            is TransactionResult.Failure -> {
                val msg = result.e.message ?: "Wallet error"
                if (msg.contains("cancelled", true) || msg.contains("rejected", true)) {
                    throw WalletUserCancelled(msg)
                }
                error(msg)
            }
        }
    }

    fun clearSession(cluster: Cluster) {
        prefs.edit().remove(authKey(cluster)).apply()
    }

    fun cachedSessionFor(cluster: Cluster): Boolean =
        prefs.contains(authKey(cluster))
}

class WalletUserCancelled(message: String) : RuntimeException(message)
