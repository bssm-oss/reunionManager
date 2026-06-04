package com.bssm.reunionmanager.data.analysis

import android.content.Context
import com.bssm.reunionmanager.domain.analysis.AnalysisProvider
import com.bssm.reunionmanager.domain.model.AnalysisInput
import com.bssm.reunionmanager.domain.model.AnalysisReport
import com.bssm.reunionmanager.domain.model.GemmaBackend
import com.bssm.reunionmanager.domain.model.ProviderSettings
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

class Gemma4AnalysisProvider(
    private val context: Context,
    private val settings: ProviderSettings,
) : AnalysisProvider {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun analyze(input: AnalysisInput): AnalysisReport = withContext(Dispatchers.IO) {
        val modelFile = File(settings.modelPath)
        require(modelFile.isFile) {
            "모델 파일을 찾을 수 없습니다. 모델을 다시 선택하거나 데모 모드를 사용하세요."
        }

        val engineConfig = EngineConfig(
            modelPath = modelFile.absolutePath,
            backend = settings.backend.toLiteRtBackend(),
            cacheDir = context.cacheDir.absolutePath,
        )

        Engine(engineConfig).use { engine ->
            engine.initialize()
            val responseText = engine.createConversation().use { conversation ->
                conversation.sendMessage(buildPrompt(input)).toText()
            }
            parseResponse(responseText)
        }
    }

    private fun buildPrompt(input: AnalysisInput): String {
        return buildString {
            appendLine("You are preparing a cautious Korean reunion-planning summary from a KakaoTalk chat excerpt.")
            appendLine("Return only valid compact JSON with these exact keys:")
            appendLine("headline, relationshipSummary, reunionObjective, nextStep, messageDraft, caution")
            appendLine("Write every value in Korean. Do not include markdown fences, commentary, therapy claims, or certainty.")
            appendLine("messageDraft must be one gentle first-contact message under 70 Korean characters.")
            appendLine("Conversation title: ${input.conversationTitle}")
            appendLine("Participants: ${input.participantNames.joinToString()}")
            appendLine("Message count: ${input.messageCount}")
            appendLine("Excerpt:")
            appendLine(input.excerpt)
        }
    }

    private fun parseResponse(responseText: String): AnalysisReport {
        val parsedJson = json.parseToJsonElement(responseText.extractJsonObject()).jsonObject
        return AnalysisReport(
            headline = parsedJson.textValue("headline", "다시 연락하기 전 확인할 점"),
            relationshipSummary = parsedJson.textValue("relationshipSummary", "대화 흐름을 단정하지 말고 천천히 확인하세요."),
            reunionObjective = parsedJson.textValue("reunionObjective", "상대에게 부담을 주지 않는 짧은 안부로 반응을 확인하세요."),
            nextStep = parsedJson.textValue("nextStep", "오늘은 긴 설명보다 짧고 차분한 첫 문장만 준비하세요."),
            messageDraft = parsedJson.textValue(
                key = "messageDraft",
                fallback = "오랜만이야. 괜찮다면 한 번 차분하게 이야기해보고 싶어.",
            ),
            caution = parsedJson.textValue("caution", "답을 재촉하지 말고 상대의 속도를 존중하세요."),
        )
    }

    private fun JsonObject.textValue(key: String, fallback: String): String {
        return this[key]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { fallback } ?: fallback
    }

    private fun String.extractJsonObject(): String {
        val start = indexOf('{')
        val end = lastIndexOf('}')
        require(start >= 0 && end > start) {
            "모델 응답을 계획 형식으로 읽지 못했습니다."
        }
        return substring(start, end + 1)
    }

    private fun Message.toText(): String {
        return contents.contents
            .filterIsInstance<Content.Text>()
            .joinToString(separator = "") { it.text }
            .ifBlank { toString() }
    }

    private fun GemmaBackend.toLiteRtBackend(): Backend {
        return when (this) {
            GemmaBackend.CPU -> Backend.CPU()
            GemmaBackend.GPU -> Backend.GPU()
        }
    }
}
