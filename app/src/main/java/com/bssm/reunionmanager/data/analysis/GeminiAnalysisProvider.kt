package com.bssm.reunionmanager.data.analysis

import com.bssm.reunionmanager.domain.analysis.AnalysisProvider
import com.bssm.reunionmanager.domain.model.AnalysisInput
import com.bssm.reunionmanager.domain.model.AnalysisReport
import com.bssm.reunionmanager.domain.model.ProviderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL

class GeminiAnalysisProvider(
    private val settings: ProviderSettings,
) : AnalysisProvider {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun analyze(input: AnalysisInput): AnalysisReport = withContext(Dispatchers.IO) {
        require(settings.apiKey.isNotBlank()) { "A Gemini API key is required." }

        val url = URL("${settings.endpoint.trimEnd('/')}/models/${settings.modelName}:generateContent?key=${settings.apiKey}")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Content-Type", "application/json")
        }

        val prompt = buildPrompt(input)
        val requestBody = buildJsonObject {
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("parts", buildJsonArray {
                        add(buildJsonObject {
                            put("text", prompt)
                        })
                    })
                })
            })
        }.toString()

        connection.outputStream.use { stream ->
            stream.write(requestBody.toByteArray())
        }

        val responseText = if (connection.responseCode in 200..299) {
            connection.inputStream.bufferedReader().readText()
        } else {
            val errorBody = connection.errorStream?.bufferedReader()?.readText().orEmpty()
            throw IllegalStateException("Gemini analysis failed with HTTP ${connection.responseCode}. $errorBody")
        }

        parseResponse(responseText)
    }

    private fun buildPrompt(input: AnalysisInput): String {
        return buildString {
            appendLine("You are preparing a cautious reunion-planning summary.")
            appendLine("Return only valid JSON with keys: headline, relationshipSummary, reunionObjective, nextStep, caution.")
            appendLine("Avoid certainty and avoid therapy claims.")
            appendLine("Conversation title: ${input.conversationTitle}")
            appendLine("Participants: ${input.participantNames.joinToString()}")
            appendLine("Message count: ${input.messageCount}")
            appendLine("Excerpt:")
            appendLine(input.excerpt)
        }
    }

    private fun parseResponse(responseText: String): AnalysisReport {
        val text = json.parseToJsonElement(responseText)
            .jsonObject["candidates"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("content")
            ?.jsonObject
            ?.get("parts")
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("text")
            ?.jsonPrimitive
            ?.content
            ?.trim()
            ?: throw IllegalStateException("Gemini response did not contain text content.")

        val parsedJson = json.parseToJsonElement(text).jsonObject
        return AnalysisReport(
            headline = parsedJson.getValue("headline").jsonPrimitive.content,
            relationshipSummary = parsedJson.getValue("relationshipSummary").jsonPrimitive.content,
            reunionObjective = parsedJson.getValue("reunionObjective").jsonPrimitive.content,
            nextStep = parsedJson.getValue("nextStep").jsonPrimitive.content,
            caution = parsedJson.getValue("caution").jsonPrimitive.content,
        )
    }
}
