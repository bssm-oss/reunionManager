package com.bssm.reunionmanager.domain.usecase

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.bssm.reunionmanager.data.analysis.FakeAnalysisProvider
import com.bssm.reunionmanager.data.importer.KakaoTalkConversationParser
import com.bssm.reunionmanager.data.local.ReunionManagerDatabase
import com.bssm.reunionmanager.data.repository.AnalysisRepository
import com.bssm.reunionmanager.data.repository.ConversationRepository
import com.bssm.reunionmanager.data.repository.ProviderSettingsRepository
import com.bssm.reunionmanager.domain.model.ImportConversationResult
import com.bssm.reunionmanager.domain.model.ProviderSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RealisticKakaoTalkFlowRegressionTest {
    private lateinit var database: ReunionManagerDatabase
    private lateinit var conversationRepository: ConversationRepository
    private lateinit var providerSettingsRepository: ProviderSettingsRepository
    private lateinit var importConversationUseCase: ImportConversationUseCase
    private lateinit var generateReunionPlanUseCase: GenerateReunionPlanUseCase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ReunionManagerDatabase::class.java,
        ).allowMainThreadQueries().build()

        conversationRepository = ConversationRepository(
            database = database,
            conversationDao = database.conversationDao(),
            participantDao = database.participantDao(),
            messageDao = database.messageDao(),
            analysisResultDao = database.analysisResultDao(),
        )
        providerSettingsRepository = ProviderSettingsRepository(database.providerSettingsDao())
        importConversationUseCase = ImportConversationUseCase(
            parser = KakaoTalkConversationParser(),
            repository = conversationRepository,
        )
        generateReunionPlanUseCase = GenerateReunionPlanUseCase(
            conversationRepository = conversationRepository,
            analysisRepository = AnalysisRepository(database.analysisResultDao()),
            providerSettingsRepository = providerSettingsRepository,
            fakeAnalysisProvider = FakeAnalysisProvider(),
            gemmaProviderFactory = { error("Gemma provider should not be used without a verified model.") },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun importAndAnalyze_handlesRealisticKakaoTalkExportsEndToEnd() = runTest {
        val cases = listOf(
            flowCase(
                name = "date divider first counterpart reply",
                sourceName = "date-divider-reply.txt",
                rawText = """
                    --------------- 2026년 6월 8일 월요일 ---------------
                    [현우] [오전 10:55] 오랜만이야. 잘 지내?
                    [민지] [오전 11:03] 응 나도 잘 지내. 너는?
                """.trimIndent(),
                userDisplayName = "현우",
                expectedTitle = "date-divider-reply",
                expectedReadiness = "아주 가볍게 가능",
                messageMustContain = "나는 잘 지내고 있어",
                messageMustNotContain = "오랜만이야",
            ),
            flowCase(
                name = "counterpart explicit boundary",
                sourceName = "boundary.txt",
                rawText = """
                    민지 님과 카카오톡 대화
                    저장한 날짜 : 2026-06-08 12:00:00

                    --------------- 2026년 6월 7일 일요일 ---------------
                    [현우] [오후 9:10] 잠깐 이야기할 수 있을까?
                    [민지] [오후 9:12] 이제 연락하지 말아줘. 우리 관계는 정리하자.
                """.trimIndent(),
                userDisplayName = "현우",
                expectedTitle = "민지",
                expectedReadiness = "지금은 보류",
                messageMustContain = "보내지 않습니다",
                messageMustNotContain = "안부",
            ),
            flowCase(
                name = "technical group chat",
                sourceName = "technical-group.txt",
                rawText = """
                    LLM 프로젝트 카카오톡 대화
                    저장한 날짜 : 2026-06-08 12:00:00

                    --------------- 2026년 6월 6일 토요일 ---------------
                    [현우] [오전 10:10] 오늘 RAG 테스트 결과 정리했어
                    [민지] [오전 10:12] OpenAI API 응답 지연이 있어서 모델 설정 다시 볼게
                    [준호] [오전 10:13] 임베딩 배포 로그도 회의 전에 공유할게
                """.trimIndent(),
                userDisplayName = "현우",
                expectedTitle = "LLM 프로젝트",
                expectedReadiness = "정보 부족",
                messageMustContain = "보낼 문장을 만들지 않습니다",
                messageMustNotContain = "오랜만이야",
            ),
            flowCase(
                name = "missing user name",
                sourceName = "missing-name.txt",
                rawText = """
                    민지 님과 카카오톡 대화
                    저장한 날짜 : 2026-06-08 12:00:00

                    --------------- 2026년 6월 8일 월요일 ---------------
                    [현우] [오전 10:55] 오랜만이야. 잘 지내?
                    [민지] [오전 11:03] 응 나도 가끔 생각났어
                """.trimIndent(),
                userDisplayName = "",
                expectedTitle = "민지",
                expectedReadiness = "정보 부족",
                messageMustContain = "내 카톡 이름",
                messageMustNotContain = "오랜만이야",
            ),
            flowCase(
                name = "counterpart schedule question",
                sourceName = "schedule-question.txt",
                rawText = """
                    민지 님과 카카오톡 대화
                    저장한 날짜 : 2026-06-08 12:00:00

                    --------------- 2026년 6월 8일 월요일 ---------------
                    [현우] [오후 2:00] 괜찮다면 짧게 얼굴 볼 수 있을까?
                    [민지] [오후 2:05] 토요일 저녁에 시간 돼?
                """.trimIndent(),
                userDisplayName = "현우",
                expectedTitle = "민지",
                expectedReadiness = "아주 가볍게 가능",
                messageMustContain = "가능한지 확인",
                messageMustNotContain = "약속한 시간",
            ),
            flowCase(
                name = "counterpart moved on emotionally",
                sourceName = "moved-on.txt",
                rawText = """
                    민지 님과 카카오톡 대화
                    저장한 날짜 : 2026-06-08 12:00:00

                    --------------- 2026년 6월 8일 월요일 ---------------
                    [현우] [오후 8:40] 다시 한 번 이야기할 수 있을까?
                    [민지] [오후 8:43] 나 이제 마음 정리했어. 다시 볼 생각 없어.
                """.trimIndent(),
                userDisplayName = "현우",
                expectedTitle = "민지",
                expectedReadiness = "지금은 보류",
                messageMustContain = "보내지 않습니다",
                messageMustNotContain = "안부",
            ),
            flowCase(
                name = "dramatic boundary with fear and new partner",
                sourceName = "dramatic-boundary.txt",
                rawText = """
                    민지 님과 카카오톡 대화
                    저장한 날짜 : 2026-06-08 12:00:00

                    --------------- 2026년 6월 8일 월요일 ---------------
                    [현우] [오후 11:40] 제발 한 번만 만나줘. 집 앞이라도 갈게.
                    [현우] [오후 11:42] 나 너 없으면 안 될 것 같아. 마지막으로 얘기하자.
                    [민지] [오후 11:50] 그러지 마. 무서워. 나 새로 만나는 사람 있고 다시 볼 생각 없어.
                    [민지] [오후 11:51] 연락하지 말아줘. 오면 신고할게.
                """.trimIndent(),
                userDisplayName = "현우",
                expectedTitle = "민지",
                expectedReadiness = "지금은 보류",
                messageMustContain = "보내지 않습니다",
                messageMustNotContain = "한 번만",
            ),
        )

        assertEquals(7, cases.size)
        cases.forEach { case ->
            providerSettingsRepository.save(ProviderSettings(userDisplayName = case.userDisplayName))

            val importResult = importConversationUseCase(
                sourceName = case.sourceName,
                rawText = case.rawText,
            )
            assertTrue(case.name, importResult is ImportConversationResult.Imported)
            val conversationId = (importResult as ImportConversationResult.Imported).conversationId

            val providerType = generateReunionPlanUseCase(conversationId)

            assertTrue(case.name, providerType.isSuccess)
            assertEquals(case.name, "fake", providerType.getOrNull())
            val detail = conversationRepository.observeConversationDetail(conversationId).first()
            requireNotNull(detail)
            assertEquals(case.name, case.expectedTitle, detail.title)
            assertEquals(case.name, case.expectedReadiness, detail.latestAnalysis?.contactReadiness)
            val report = requireNotNull(detail.latestAnalysis)
            assertTrue(case.name, report.messageDraft.contains(case.messageMustContain))
            assertFalse(case.name, report.messageDraft.contains(case.messageMustNotContain))
            assertFalse(case.name, report.messageDraft.contains("집 앞"))
            assertFalse(case.name, report.messageDraft.contains("당장"))
        }
    }

    private data class FlowCase(
        val name: String,
        val sourceName: String,
        val rawText: String,
        val userDisplayName: String,
        val expectedTitle: String,
        val expectedReadiness: String,
        val messageMustContain: String,
        val messageMustNotContain: String,
    )

    private fun flowCase(
        name: String,
        sourceName: String,
        rawText: String,
        userDisplayName: String,
        expectedTitle: String,
        expectedReadiness: String,
        messageMustContain: String,
        messageMustNotContain: String,
    ): FlowCase {
        return FlowCase(
            name = name,
            sourceName = sourceName,
            rawText = rawText,
            userDisplayName = userDisplayName,
            expectedTitle = expectedTitle,
            expectedReadiness = expectedReadiness,
            messageMustContain = messageMustContain,
            messageMustNotContain = messageMustNotContain,
        )
    }
}
