package com.voice0.app.data

import com.voice0.app.BuildConfig

enum class Cluster(val rpcName: String, val mwaName: String) {
    Mainnet("mainnet-beta", "solana:mainnet"),
    Devnet("devnet", "solana:devnet");

    fun rpcUrl(): String = when (this) {
        Mainnet -> BuildConfig.HELIUS_RPC_URL
        Devnet -> BuildConfig.HELIUS_DEVNET_RPC_URL
    }
}
