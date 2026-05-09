package com.voice0.app.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.net.UnknownHostException

object Net {
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    val client: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 30_000
        }
        install(HttpRequestRetry) {
            maxRetries = 3
            retryOnExceptionIf { _, cause ->
                // Retry on DNS failures and connection resets
                cause is UnknownHostException || cause.message?.contains("connection", ignoreCase = true) == true
            }
            delayMillis { retry -> retry * 500L }
        }
        install(Logging) { level = LogLevel.NONE }
        engine {
            config {
                retryOnConnectionFailure(true)
            }
        }
    }
}
