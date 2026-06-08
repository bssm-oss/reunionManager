package com.bssm.reunionmanager.data.analysis

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bssm.reunionmanager.domain.analysis.AnalysisSafetyRules
import com.bssm.reunionmanager.domain.model.AnalysisInput
import com.bssm.reunionmanager.domain.model.GemmaBackend
import com.bssm.reunionmanager.domain.model.ProviderSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Gemma4AnalysisProviderDeviceSmokeTest {
    @Test
    fun configuredLiteRtModelProducesSanitizedReport() = runBlocking {
        val modelPath = InstrumentationRegistry.getArguments()
            .getString("gemmaModelPath")
            .orEmpty()
        assumeTrue(
            "Pass -Pandroid.testInstrumentationRunnerArguments.gemmaModelPath=/data/local/tmp/model.litertlm to run real Gemma.",
            modelPath.isNotBlank(),
        )

        val provider = Gemma4AnalysisProvider(
            context = ApplicationProvider.getApplicationContext(),
            settings = ProviderSettings(
                modelPath = modelPath,
                modelName = modelPath.substringAfterLast('/'),
                backend = GemmaBackend.CPU,
                userDisplayName = "현우",
            ),
        )

        val report = AnalysisSafetyRules.finalizeReport(provider.analyze(smokeInput), smokeInput)

        assertTrue(report.headline.isNotBlank())
        assertTrue(report.contactReadiness in allowedReadiness)
        assertTrue(report.messageDraft.length <= 90)
        assertFalse(report.messageDraft.contains("당장"))
        assertFalse(report.messageDraft.contains("집 앞"))
    }

    private companion object {
        val allowedReadiness = setOf("지금은 보류", "먼저 사과 필요", "아주 가볍게 가능", "정보 부족")

        val smokeInput = AnalysisInput(
            conversationTitle = "민지와의 대화",
            participantNames = listOf("민지", "현우"),
            messageCount = 3,
            excerpt = "현우: 오랜만이야\n민지: 나도 가끔 생각났어\n민지: 괜찮다면 천천히 이야기해도 돼",
            recentExcerpt = "현우: 오랜만이야\n민지: 나도 가끔 생각났어\n민지: 괜찮다면 천천히 이야기해도 돼",
            signalExcerpt = "민지: 나도 가끔 생각났어\n민지: 괜찮다면 천천히 이야기해도 돼",
            statsSummary = "마지막 메시지: 괜찮다면 천천히 이야기해도 돼\n마지막 발신자의 연속 발화: 2개\n마지막 메시지 이후 경과: 알 수 없음",
            perspectiveSummary = """
                내 카톡 이름: 현우
                상대 후보: 민지
                마지막 메시지 발신자 역할: 상대
                마지막 연속 발화 역할: 상대 2개
                내 메시지: 1개
                상대 메시지: 2개
                내 최근 메시지: 오랜만이야
                상대 최근 메시지: 괜찮다면 천천히 이야기해도 돼
                내 마지막 연속 발화: 0개
                상대 마지막 연속 발화: 2개
            """.trimIndent(),
        )
    }
}
