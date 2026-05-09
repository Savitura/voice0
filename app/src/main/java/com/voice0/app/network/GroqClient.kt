package com.voice0.app.network

import com.voice0.app.BuildConfig
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class GroqMessage(val role: String, val content: String)

@Serializable
private data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>,
    @SerialName("max_tokens") val maxTokens: Int,
    val temperature: Double = 0.0,
)

@Serializable
private data class GroqChoice(val message: GroqMessage)

@Serializable
private data class GroqResponse(val choices: List<GroqChoice>)

object GroqClient {
    private const val URL = "https://api.groq.com/openai/v1/chat/completions"

    suspend fun complete(
        systemPrompt: String,
        userText: String,
        maxTokens: Int = 1024,
    ): String {
        check(BuildConfig.GROQ_API_KEY.isNotBlank()) {
            "GROQ_API_KEY is not set in local.properties"
        }

        val httpResponse = Net.client.post(URL) {
            header("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
            contentType(ContentType.Application.Json)
            setBody(
                GroqRequest(
                    model = BuildConfig.GROQ_MODEL,
                    messages = listOf(
                        GroqMessage(role = "system", content = systemPrompt),
                        GroqMessage(role = "user", content = "<user_intent>$userText</user_intent>"),
                    ),
                    maxTokens = maxTokens,
                ),
            )
        }

        if (!httpResponse.status.isSuccess()) {
            error("Groq ${httpResponse.status.value}: ${httpResponse.bodyAsText()}")
        }

        val response: GroqResponse = httpResponse.body()
        return response.choices.firstOrNull()?.message?.content
            ?: error("Groq returned no content")
    }
}
