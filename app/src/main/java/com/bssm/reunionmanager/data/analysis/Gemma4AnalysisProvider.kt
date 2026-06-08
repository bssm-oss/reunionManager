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
            "모델 파일을 찾을 수 없습니다. 모델을 다시 선택하거나 안전 정리를 사용하세요."
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
            appendLine("You are a cautious Korean relationship-repair assistant analyzing a KakaoTalk transcript.")
            appendLine("Internally run three private passes before writing JSON: evidence reader, safety critic, and message writer. Do not reveal these passes.")
            appendLine("Use only the provided transcript evidence. Do not infer love, intent, or certainty.")
            appendLine("If there are rejection, boundary, harassment, or repeated unanswered-message signs, recommend waiting or not contacting.")
            appendLine("Treat moved-on statements such as 새로 만나는 사람 있어, 각자 잘 지내자, 친구로 지내자, or 나중에 내가 연락할게 as a boundary to wait, not as an opening.")
            appendLine("Return only valid compact JSON with these exact keys:")
            appendLine("headline, contactReadiness, evidence, relationshipSummary, reunionObjective, nextStep, messageDraft, alternativeDrafts, caution")
            appendLine("Write every value in Korean. Do not include markdown fences, commentary, therapy claims, or certainty.")
            appendLine("headline must be one specific Korean summary under 24 characters, not a generic title.")
            appendLine("relationshipSummary must be one sentence grounded in last sender, silence duration, and signal excerpts.")
            appendLine("reunionObjective must describe the user's safest immediate goal, not a broad relationship goal.")
            appendLine("When Perspective summary includes 내 최근 메시지 or 상대 최근 메시지, use them to make the summary and draft specific.")
            appendLine("If the transcript looks like a group, work, technical, or transactional chat without personal relationship signals, set contactReadiness to 정보 부족 and do not draft a contact message.")
            appendLine("contactReadiness must be one of: 지금은 보류, 먼저 사과 필요, 아주 가볍게 가능, 정보 부족.")
            appendLine("evidence must list 2-3 short reasons grounded in the provided stats or excerpts.")
            appendLine("If contactReadiness is 지금은 보류, messageDraft must say not to send a message today.")
            appendLine("If the last sender role is 상대, do not frame the action as a new first contact; draft a short reply to the counterpart's last message.")
            appendLine("If the counterpart's last message proposes a concrete time or place to meet, acknowledge and confirm that plan instead of asking to start a new conversation.")
            appendLine("If the counterpart asks whether a day or time works, do not pretend the plan is confirmed; draft a short availability-check reply.")
            appendLine("Mention a late reply only when Conversation stats says 마지막 메시지 이후 경과 is at least 1 day; otherwise do not write 답이 늦었네.")
            appendLine("Otherwise, messageDraft must be one gentle first-contact message under 70 Korean characters.")
            appendLine("alternativeDrafts must contain exactly 3 short candidate messages or no-send actions separated by newline characters.")
            appendLine("Treat 3 or more consecutive final messages from the user as an unanswered-message risk.")
            appendLine("If the user's KakaoTalk name is not configured, set contactReadiness to 정보 부족, do not draft a contact message, and ask the user to save their KakaoTalk name first.")
            appendLine("Conversation title: ${input.conversationTitle}")
            appendLine("Participants: ${input.participantNames.joinToString()}")
            appendLine("Message count: ${input.messageCount}")
            appendLine("Perspective summary:")
            appendLine(input.perspectiveSummary)
            appendLine("Conversation stats:")
            appendLine(input.statsSummary)
            appendLine("Important excerpt:")
            appendLine(input.excerpt)
            appendLine("Recent excerpt:")
            appendLine(input.recentExcerpt)
            appendLine("Signal excerpt:")
            appendLine(input.signalExcerpt.ifBlank { "No explicit signal messages were detected." })
        }
    }

    private fun parseResponse(responseText: String): AnalysisReport {
        val parsedJson = json.parseToJsonElement(responseText.extractJsonObject()).jsonObject
        return AnalysisReport(
            headline = parsedJson.textValue("headline", "다시 연락하기 전 확인할 점"),
            contactReadiness = parsedJson.textValue("contactReadiness", "정보 부족"),
            evidence = parsedJson.textValue("evidence", "최근 흐름과 감정 신호를 더 확인해야 합니다."),
            relationshipSummary = parsedJson.textValue("relationshipSummary", "대화 흐름을 단정하지 말고 천천히 확인하세요."),
            reunionObjective = parsedJson.textValue("reunionObjective", "상대에게 부담을 주지 않는 짧은 안부로 반응을 확인하세요."),
            nextStep = parsedJson.textValue("nextStep", "오늘은 긴 설명보다 짧고 차분한 첫 문장만 준비하세요."),
            messageDraft = parsedJson.textValue(
                key = "messageDraft",
                fallback = "오랜만이야. 괜찮다면 한 번 차분하게 이야기해보고 싶어.",
            ),
            alternativeDrafts = parsedJson.textValue(
                key = "alternativeDrafts",
                fallback = "오랜만이야. 잘 지내고 있는지 궁금해서 연락했어.\n부담 주려는 건 아니고, 괜찮다면 안부만 묻고 싶어.\n답은 천천히 해도 괜찮아. 그냥 안부 전하고 싶었어.",
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
