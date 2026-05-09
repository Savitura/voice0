package com.voice0.app.network

import com.voice0.app.BuildConfig
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
private data class ElevenLabsSttResponse(val text: String)

object ElevenLabsClient {
    private const val URL = "https://api.elevenlabs.io/v1/speech-to-text"

    /** Upload an audio file and return the transcribed text. */
    suspend fun transcribe(audio: File): String {
        check(BuildConfig.ELEVENLABS_API_KEY.isNotBlank()) {
            "ELEVENLABS_API_KEY is not set in local.properties"
        }

        val httpResponse = Net.client.post(URL) {
            header("xi-api-key", BuildConfig.ELEVENLABS_API_KEY)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("model_id", "scribe_v1")
                        append("language_code", "en")
                        append(
                            key = "file",
                            value = audio.readBytes(),
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, "audio/m4a")
                                append(HttpHeaders.ContentDisposition, "filename=\"recording.m4a\"")
                            },
                        )
                    },
                ),
            )
        }
        if (!httpResponse.status.isSuccess()) {
            error("ElevenLabs ${httpResponse.status.value}: ${httpResponse.bodyAsText()}")
        }
        return httpResponse.body<ElevenLabsSttResponse>().text
    }
}
