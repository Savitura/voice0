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
 * so reconnects after app restart skip the wallet approval UI.
 *
 * Both connect() and signAndSend() attempt reauthorize with the cached token first.
 * If reauthorize fails (token expired / revoked), they fall back to a fresh authorize
 * inside the same MWA session — no crash, no manual reconnect needed.
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

    private fun adapter() = MobileWalletAdapter(connectionIdentity = identity)

    private fun authKey(cluster: Cluster) = "auth_token_${cluster.name}"
    private fun pubkeyKey(cluster: Cluster) = "pubkey_${cluster.name}"

    private fun Cluster.toRpcCluster() = when (this) {
        Cluster.Mainnet -> RpcCluster.MainnetBeta
        Cluster.Devnet -> RpcCluster.Devnet
    }

    fun cachedPublicKey(cluster: Cluster): String? =
        prefs.getString(pubkeyKey(cluster), null)?.takeIf { it.isNotBlank() }

    fun cachedSessionFor(cluster: Cluster): Boolean =
        prefs.contains(authKey(cluster))

    private fun saveSession(cluster: Cluster, authToken: String, publicKeyBytes: ByteArray) {
        prefs.edit()
            .putString(authKey(cluster), authToken)
            .putString(pubkeyKey(cluster), Base58.encodeToString(publicKeyBytes))
            .apply()
    }

    suspend fun connect(sender: ActivityResultSender, cluster: Cluster): Session {
        val mwa = adapter()
        val rpcCluster = cluster.toRpcCluster()
        val cachedToken = prefs.getString(authKey(cluster), null)

        val result = mwa.transact(sender) {
            if (cachedToken == null) {
                authorize(identity.identityUri, identity.iconUri, identity.identityName, rpcCluster)
            } else {
                try {
                    reauthorize(identity.identityUri, identity.iconUri, identity.identityName, cachedToken)
                } catch (e: Exception) {
                    // Cached token expired or revoked — request fresh authorization
                    authorize(identity.identityUri, identity.iconUri, identity.identityName, rpcCluster)
                }
            }
        }

        return when (result) {
            is TransactionResult.Success -> {
                val auth = result.authResult
                val account = auth.accounts.first()
                saveSession(cluster, auth.authToken, account.publicKey)
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
     * Sign and send a list of base64-encoded VersionedTransactions.
     * If the cached auth token has expired, re-authorizes silently before signing.
     */
    suspend fun signAndSend(
        sender: ActivityResultSender,
        cluster: Cluster,
        txsBase64: List<String>,
    ): List<String> {
        require(txsBase64.isNotEmpty()) { "No transactions to sign" }
        val mwa = adapter()
        val txBytes = txsBase64.map { android.util.Base64.decode(it, android.util.Base64.NO_WRAP) }
        val cachedToken = prefs.getString(authKey(cluster), null)
            ?: error("Not authorized. Connect a wallet first.")
        val rpcCluster = cluster.toRpcCluster()

        val result = mwa.transact(sender) {
            try {
                reauthorize(identity.identityUri, identity.iconUri, identity.identityName, cachedToken)
            } catch (e: Exception) {
                // Token expired — re-authorize in the same MWA session so signing can proceed
                val auth = authorize(identity.identityUri, identity.iconUri, identity.identityName, rpcCluster)
                saveSession(cluster, auth.authToken, auth.accounts.first().publicKey)
            }
            signAndSendTransactions(txBytes.toTypedArray())
        }

        return when (result) {
            is TransactionResult.Success -> result.payload.signatures.map { Base58.encodeToString(it) }
            is TransactionResult.NoWalletFound -> error("No MWA-compatible wallet installed")
            is TransactionResult.Failure -> {
                val msg = result.e.message ?: "Wallet error"
                when {
                    msg.contains("cancelled", ignoreCase = true) ||
                    msg.contains("rejected", ignoreCase = true) ->
                        throw WalletUserCancelled(msg)
                    msg.contains("authorization", ignoreCase = true) -> {
                        // Token permanently invalid — wipe it so next attempt does fresh authorize
                        clearSession(cluster)
                        error("Wallet session expired. Please try again to reconnect.")
                    }
                    else -> error(msg)
                }
            }
        }
    }

    fun clearSession(cluster: Cluster) {
        prefs.edit().remove(authKey(cluster)).remove(pubkeyKey(cluster)).apply()
    }
}

class WalletUserCancelled(message: String) : RuntimeException(message)
