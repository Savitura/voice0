package com.voice0.app.data

import kotlinx.serialization.Serializable

@Serializable
data class TxRecord(
    val id: String,
    val timestampMs: Long,
    val cluster: String,
    val intent: String,
    val steps: List<String>,
    val signatures: List<String>,
)
