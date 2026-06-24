package com.bssm.reunionmanager.data.analysis

import com.bssm.reunionmanager.domain.analysis.AnalysisProvider
import com.bssm.reunionmanager.domain.model.AnalysisInput
import com.bssm.reunionmanager.domain.model.AnalysisReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.net.HttpURLConnection
import java.net.URL

class OpenRouterAnalysisProvider(
    private val chatClient: OpenRouterChatClient,
) : AnalysisProvider {
    constructor(
        apiKey: String,
        model: String = DEFAULT_MODEL,
        endpoint: String = DEFAULT_ENDPOINT,
    ) : this(
        HttpOpenRouterChatClient(
            apiKey = apiKey,
            model = model,
            endpoint = endpoint,
        ),
    )

    override suspend fun analyze(input: AnalysisInput): AnalysisReport {
        val responseText = chatClient.complete(
            systemPrompt = ReunionAnalysisPrompt.SYSTEM_PROMPT,
            userPrompt = ReunionAnalysisPrompt.buildUserPrompt(input),
        )
        return AnalysisJsonResponseParser.parse(responseText)
    }

    companion object {
        const val DEFAULT_MODEL: String = "deepseek/deepseek-v4-flash"
        const val DEFAULT_ENDPOINT: String = "https://openrouter.ai/api/v1/chat/completions"
    }
}

interface OpenRouterChatClient {
    suspend fun complete(systemPrompt: String, userPrompt: String): String
}

class HttpOpenRouterChatClient(
    private val apiKey: String,
    private val model: String,
    private val endpoint: String,
) : OpenRouterChatClient {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun complete(systemPrompt: String, userPrompt: String): String = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "OpenRouter API 키가 설정되지 않았습니다." }
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("HTTP-Referer", "https://github.com/bssm-oss/reunionManager")
            setRequestProperty("X-Title", "Reunion Manager")
        }

        val requestBody = buildRequestBody(systemPrompt, userPrompt).toString()
        connection.outputStream.use { output ->
            output.write(requestBody.toByteArray(Charsets.UTF_8))
        }

        val statusCode = connection.responseCode
        val responseBody = if (statusCode in 200..299) {
            connection.inputStream.use { input -> input.reader(Charsets.UTF_8).readText() }
        } else {
            connection.errorStream?.use { input -> input.reader(Charsets.UTF_8).readText() }.orEmpty()
        }

        if (statusCode !in 200..299) {
            error("OpenRouter 요청 실패($statusCode): ${responseBody.safeSnippet()}")
        }

        responseBody.extractAssistantContent()
    }

    private fun buildRequestBody(systemPrompt: String, userPrompt: String) = buildJsonObject {
        put("model", model)
        putJsonArray("messages") {
            add(
                buildJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                },
            )
            add(
                buildJsonObject {
                    put("role", "user")
                    put("content", userPrompt)
                },
            )
        }
        put("temperature", 0.2)
        put("max_tokens", 900)
        put(
            "response_format",
            buildJsonObject {
                put("type", "json_object")
            },
        )
    }

    private fun String.extractAssistantContent(): String {
        val root = json.parseToJsonElement(this).jsonObject
        return root["choices"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("message")
            ?.jsonObject
            ?.get("content")
            ?.jsonPrimitive
            ?.contentOrNull
            ?.takeIf { content -> content.isNotBlank() }
            ?: error("OpenRouter 응답에서 분석 내용을 찾지 못했습니다.")
    }

    private fun String.safeSnippet(): String {
        return replace(Regex("sk-or-v1-[A-Za-z0-9_-]+"), "[redacted]")
            .replace(Regex("Bearer\\s+\\S+"), "Bearer [redacted]")
            .take(240)
    }
}
