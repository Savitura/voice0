package com.voice0.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.txDataStore by preferencesDataStore(name = "tx_history")
private val KEY = stringPreferencesKey("records")
private const val MAX_RECORDS = 50

class TxHistoryRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    val records: Flow<List<TxRecord>> = context.txDataStore.data.map { prefs ->
        prefs[KEY]?.let { raw ->
            runCatching { json.decodeFromString<List<TxRecord>>(raw) }.getOrDefault(emptyList())
        } ?: emptyList()
    }

    suspend fun add(record: TxRecord) {
        context.txDataStore.edit { prefs ->
            val current = prefs[KEY]?.let {
                runCatching { json.decodeFromString<List<TxRecord>>(it) }.getOrDefault(emptyList())
            } ?: emptyList()
            val updated = (listOf(record) + current).take(MAX_RECORDS)
            prefs[KEY] = json.encodeToString(updated)
        }
    }

    suspend fun clear() {
        context.txDataStore.edit { it.remove(KEY) }
    }
}
