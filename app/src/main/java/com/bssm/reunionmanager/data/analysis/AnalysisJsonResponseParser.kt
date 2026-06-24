package com.bssm.reunionmanager.data.analysis

import com.bssm.reunionmanager.domain.model.AnalysisReport
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object AnalysisJsonResponseParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(responseText: String): AnalysisReport {
        val parsedJson = json.parseToJsonElement(responseText.extractJsonObject()).jsonObject
        return AnalysisReport(
            headline = parsedJson.textValue("headline", "다시 연락하기 전 확인할 점"),
            contactReadiness = parsedJson.textValue("contactReadiness", "정보 부족"),
            evidence = parsedJson.textValue("evidence", "최근 흐름과 감정 신호를 더 확인해야 합니다."),
            relationshipSummary = parsedJson.textValue("relationshipSummary", "카톡 내용을 단정하지 말고 천천히 확인해보면 좋아요."),
            reunionObjective = parsedJson.textValue("reunionObjective", "상대에게 부담을 주지 않는 짧은 안부로 현재 온도만 살펴요."),
            nextStep = parsedJson.textValue("nextStep", "오늘은 긴 설명보다 짧고 차분한 첫 문장만 준비해보면 좋아요."),
            messageDraft = parsedJson.textValue(
                key = "messageDraft",
                fallback = "오랜만이야. 괜찮다면 한 번 차분하게 이야기해보고 싶어.",
            ),
            alternativeDrafts = parsedJson.textValue(
                key = "alternativeDrafts",
                fallback = "오랜만이야. 잘 지내고 있는지 궁금해서 연락했어.\n부담 주려는 건 아니고, 괜찮다면 안부만 묻고 싶어.\n답은 천천히 해도 괜찮아. 그냥 안부 전하고 싶었어.",
            ),
            caution = parsedJson.textValue("caution", "답을 재촉하지 않고 상대의 속도를 존중해요."),
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
}
