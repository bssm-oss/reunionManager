package com.bssm.reunionmanager.data.analysis

import com.bssm.reunionmanager.domain.analysis.AnalysisSafetyRules
import com.bssm.reunionmanager.domain.model.AnalysisInput
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class OpenRouterAnalysisProviderTest {
    @Test
    fun analyze_sendsSharedReunionPromptAndParsesJsonResponse() = runTest {
        val client = RecordingClient(
            response = """
                {
                  "headline": "천천히 답장",
                  "contactReadiness": "아주 가볍게 가능",
                  "evidence": "상대가 마지막에 답장을 남겼습니다.\n경계 표현은 없습니다.",
                  "relationshipSummary": "상대가 마지막에 낮은 압박의 답장을 남긴 상태입니다.",
                  "reunionObjective": "상대의 속도를 존중하며 짧게 답합니다.",
                  "nextStep": "짧은 답장 한 문장만 준비하세요.",
                  "messageDraft": "메시지 봤어. 말해줘서 고마워.",
                  "alternativeDrafts": "메시지 봤어. 고마워.\n천천히 이야기해도 괜찮아.\n답은 편할 때 해도 괜찮아.",
                  "caution": "답을 재촉하지 마세요."
                }
            """.trimIndent(),
        )
        val provider = OpenRouterAnalysisProvider(client)

        val result = provider.analyze(lowPressureInput())

        assertEquals("천천히 답장", result.headline)
        assertEquals("아주 가볍게 가능", result.contactReadiness)
        assertEquals("메시지 봤어. 말해줘서 고마워.", result.messageDraft)
        assertTrue(client.systemPrompt.contains("cautious Korean relationship-repair assistant"))
        assertTrue(client.userPrompt.contains("boundary/safety critic"))
        assertTrue(client.userPrompt.contains("Return only valid JSON"))
        assertTrue(client.userPrompt.contains("KakaoTalk"))
        assertTrue(client.userPrompt.contains("연락하지 말아줘"))
    }

    @Test
    fun analyze_liveOpenRouterSmokeUsesDeepSeekV4FlashWhenKeyIsAvailable() = runTest {
        assumeTrue(
            "RUN_OPENROUTER_LIVE_TESTS=true일 때만 실제 OpenRouter smoke를 실행합니다.",
            liveOpenRouterTestsEnabled(),
        )
        val apiKey = openRouterApiKeyForTest()
        assumeTrue("OPENROUTER_API_KEY가 없으면 실제 OpenRouter smoke는 건너뜁니다.", apiKey.isNotBlank())

        val provider = OpenRouterAnalysisProvider(apiKey = apiKey)
        val rawReport = provider.analyze(dramaticBoundaryInput())
        val finalReport = AnalysisSafetyRules.finalizeReport(rawReport, dramaticBoundaryInput())

        assertEquals("지금은 보류", finalReport.contactReadiness)
        assertTrue(finalReport.messageDraft.contains("보내지 않습니다"))
        assertFalse(finalReport.messageDraft.contains("집 앞"))
        assertFalse(finalReport.messageDraft.contains("한 번만"))
        assertTrue(AnalysisSafetyRules.isAllowedReadiness(rawReport.contactReadiness))
    }

    private class RecordingClient(
        private val response: String,
    ) : OpenRouterChatClient {
        lateinit var systemPrompt: String
            private set
        lateinit var userPrompt: String
            private set

        override suspend fun complete(systemPrompt: String, userPrompt: String): String {
            this.systemPrompt = systemPrompt
            this.userPrompt = userPrompt
            return response
        }
    }

    private companion object {
        fun openRouterApiKeyForTest(): String {
            System.getenv("OPENROUTER_API_KEY")
                ?.trim()
                ?.takeIf { key -> key.isNotBlank() }
                ?.let { key -> return key }

            var directory: File? = File(System.getProperty("user.dir") ?: ".")
            while (directory != null) {
                val dotEnv = directory.resolve(".env")
                if (dotEnv.isFile) {
                    return dotEnv.readLines()
                        .asSequence()
                        .map { line -> line.trim() }
                        .filter { line -> line.isNotBlank() && !line.startsWith("#") && line.contains("=") }
                        .map { line ->
                            line.substringBefore("=")
                                .removePrefix("export")
                                .trim() to line.substringAfter("=")
                        }
                        .firstOrNull { (key, _) -> key == "OPENROUTER_API_KEY" }
                        ?.second
                        ?.trim()
                        ?.trim('"', '\'')
                        .orEmpty()
                }
                directory = directory.parentFile
            }
            return ""
        }

        fun liveOpenRouterTestsEnabled(): Boolean {
            return listOf(
                System.getenv("RUN_OPENROUTER_LIVE_TESTS"),
                System.getProperty("RUN_OPENROUTER_LIVE_TESTS"),
            ).any { value -> value.equals("true", ignoreCase = true) || value == "1" }
        }

        fun lowPressureInput(): AnalysisInput {
            return AnalysisInput(
                conversationTitle = "민지와의 대화",
                participantNames = listOf("현우", "민지"),
                messageCount = 3,
                excerpt = "현우: 오랜만이야\n민지: 나도 가끔 생각났어\n민지: 괜찮다면 천천히 이야기해도 돼",
                recentExcerpt = "민지: 나도 가끔 생각났어\n민지: 괜찮다면 천천히 이야기해도 돼",
                signalExcerpt = "민지: 괜찮다면 천천히 이야기해도 돼\n민지: 연락하지 말아줘 같은 경계 표현은 없습니다.",
                statsSummary = "마지막 메시지: 괜찮다면 천천히 이야기해도 돼\n마지막 발신자의 연속 발화: 2개\n마지막 메시지 이후 경과: 알 수 없음",
                perspectiveSummary = """
                    내 카톡 이름: 현우
                    상대 후보: 민지
                    마지막 메시지 발신자 역할: 상대
                    마지막 연속 발화 역할: 상대 2개
                    내 최근 메시지: 오랜만이야
                    상대 최근 메시지: 괜찮다면 천천히 이야기해도 돼
                    내 마지막 연속 발화: 0개
                    상대 마지막 연속 발화: 2개
                """.trimIndent(),
            )
        }

        fun dramaticBoundaryInput(): AnalysisInput {
            return AnalysisInput(
                conversationTitle = "민지와의 대화",
                participantNames = listOf("현우", "민지"),
                messageCount = 4,
                excerpt = "현우: 제발 한 번만 만나줘. 집 앞이라도 갈게.\n현우: 나 너 없으면 안 될 것 같아. 마지막으로 얘기하자.\n민지: 그러지 마. 무서워. 나 새로 만나는 사람 있고 다시 볼 생각 없어.\n민지: 연락하지 말아줘. 오면 신고할게.",
                recentExcerpt = "현우: 나 너 없으면 안 될 것 같아. 마지막으로 얘기하자.\n민지: 그러지 마. 무서워. 나 새로 만나는 사람 있고 다시 볼 생각 없어.\n민지: 연락하지 말아줘. 오면 신고할게.",
                signalExcerpt = "민지: 무서워. 새로 만나는 사람 있고 다시 볼 생각 없어.\n민지: 연락하지 말아줘. 오면 신고할게.",
                statsSummary = "마지막 메시지: 연락하지 말아줘. 오면 신고할게.\n마지막 발신자의 연속 발화: 2개\n마지막 메시지 이후 경과: 알 수 없음",
                perspectiveSummary = """
                    내 카톡 이름: 현우
                    상대 후보: 민지
                    마지막 메시지 발신자 역할: 상대
                    마지막 연속 발화 역할: 상대 2개
                    내 최근 메시지: 나 너 없으면 안 될 것 같아. 마지막으로 얘기하자.
                    상대 최근 메시지: 연락하지 말아줘. 오면 신고할게.
                    내 마지막 연속 발화: 0개
                    상대 마지막 연속 발화: 2개
                """.trimIndent(),
            )
        }
    }
}
