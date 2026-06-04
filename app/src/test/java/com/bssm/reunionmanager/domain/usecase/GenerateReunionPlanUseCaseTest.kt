package com.bssm.reunionmanager.domain.usecase

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.bssm.reunionmanager.data.analysis.FakeAnalysisProvider
import com.bssm.reunionmanager.data.importer.ParsedConversation
import com.bssm.reunionmanager.data.importer.ParsedMessage
import com.bssm.reunionmanager.data.local.ReunionManagerDatabase
import com.bssm.reunionmanager.data.repository.AnalysisRepository
import com.bssm.reunionmanager.data.repository.ConversationRepository
import com.bssm.reunionmanager.data.repository.ProviderSettingsRepository
import com.bssm.reunionmanager.domain.analysis.AnalysisProvider
import com.bssm.reunionmanager.domain.model.AnalysisInput
import com.bssm.reunionmanager.domain.model.AnalysisReport
import com.bssm.reunionmanager.domain.model.GemmaBackend
import com.bssm.reunionmanager.domain.model.ImportConversationResult
import com.bssm.reunionmanager.domain.model.ProviderSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GenerateReunionPlanUseCaseTest {
    private lateinit var database: ReunionManagerDatabase
    private lateinit var conversationRepository: ConversationRepository
    private lateinit var analysisRepository: AnalysisRepository
    private lateinit var providerSettingsRepository: ProviderSettingsRepository

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
        analysisRepository = AnalysisRepository(database.analysisResultDao())
        providerSettingsRepository = ProviderSettingsRepository(database.providerSettingsDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun invoke_usesFakeProviderWhenModelPathIsMissing() = runTest {
        val importedId = (conversationRepository.importConversation(
            parsedConversation = sampleParsedConversation,
            rawText = "analysis raw text",
            sourceName = "analysis.txt",
        ) as ImportConversationResult.Imported).conversationId

        val useCase = GenerateReunionPlanUseCase(
            conversationRepository = conversationRepository,
            analysisRepository = analysisRepository,
            providerSettingsRepository = providerSettingsRepository,
            fakeAnalysisProvider = FakeAnalysisProvider(),
            gemmaProviderFactory = { error("Gemma provider should not be used without a model path.") },
        )

        val result = useCase(importedId)

        assertTrue(result.isSuccess)
        assertEquals("fake", result.getOrNull())

        val detail = conversationRepository.observeConversationDetail(importedId).first()
        requireNotNull(detail)
        assertTrue(detail.latestAnalysis != null)
        assertTrue(detail.latestAnalysis!!.headline.contains("첫 단계"))
        assertTrue(detail.latestAnalysis!!.messageDraft.contains("오랜만이야"))
    }

    @Test
    fun invoke_usesGemmaProviderWhenModelPathIsConfigured() = runTest {
        val importedId = importSampleConversation()
        providerSettingsRepository.save(
            ProviderSettings(
                modelPath = "/data/local/tmp/gemma-4-E4B-it.litertlm",
                modelName = "gemma-4-E4B-it.litertlm",
                backend = GemmaBackend.GPU,
            ),
        )

        val useCase = GenerateReunionPlanUseCase(
            conversationRepository = conversationRepository,
            analysisRepository = analysisRepository,
            providerSettingsRepository = providerSettingsRepository,
            fakeAnalysisProvider = FakeAnalysisProvider(),
            gemmaProviderFactory = { settings ->
                assertEquals("/data/local/tmp/gemma-4-E4B-it.litertlm", settings.modelPath)
                assertEquals(GemmaBackend.GPU, settings.backend)
                StaticAnalysisProvider(
                    AnalysisReport(
                        headline = "Mock Gemma headline",
                        relationshipSummary = "Mock Gemma relationship summary",
                        reunionObjective = "Mock Gemma reunion objective",
                        nextStep = "Mock Gemma next step",
                        messageDraft = "Mock Gemma message draft",
                        caution = "Mock Gemma caution",
                    ),
                )
            },
        )

        val result = useCase(importedId)

        assertTrue(result.isSuccess)
        assertEquals("gemma4", result.getOrNull())

        val detail = conversationRepository.observeConversationDetail(importedId).first()
        requireNotNull(detail)
        assertNotNull(detail.latestAnalysis)
        assertEquals("Mock Gemma headline", detail.latestAnalysis!!.headline)
        assertEquals("Mock Gemma relationship summary", detail.latestAnalysis!!.relationshipSummary)
        assertEquals("Mock Gemma message draft", detail.latestAnalysis!!.messageDraft)
    }

    @Test
    fun invoke_returnsFailureWhenConfiguredGemmaProviderErrors() = runTest {
        val importedId = importSampleConversation()
        providerSettingsRepository.save(
            ProviderSettings(
                modelPath = "/data/local/tmp/missing-gemma-4-E4B-it.litertlm",
                modelName = "gemma-4-E4B-it.litertlm",
                backend = GemmaBackend.CPU,
            )
        )

        val useCase = GenerateReunionPlanUseCase(
            conversationRepository = conversationRepository,
            analysisRepository = analysisRepository,
            providerSettingsRepository = providerSettingsRepository,
            fakeAnalysisProvider = FakeAnalysisProvider(),
            gemmaProviderFactory = { FailingAnalysisProvider("모델 파일을 찾을 수 없습니다.") },
        )

        val result = useCase(importedId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("모델 파일을 찾을 수 없습니다."))

        val detail = conversationRepository.observeConversationDetail(importedId).first()
        requireNotNull(detail)
        assertFalse(detail.latestAnalysis != null)
    }

    private suspend fun importSampleConversation(): Long {
        return (conversationRepository.importConversation(
            parsedConversation = sampleParsedConversation,
            rawText = "analysis raw text",
            sourceName = "analysis.txt",
        ) as ImportConversationResult.Imported).conversationId
    }

    private class StaticAnalysisProvider(
        private val report: AnalysisReport,
    ) : AnalysisProvider {
        override suspend fun analyze(input: AnalysisInput): AnalysisReport = report
    }

    private class FailingAnalysisProvider(
        private val message: String,
    ) : AnalysisProvider {
        override suspend fun analyze(input: AnalysisInput): AnalysisReport {
            error(message)
        }
    }

    private companion object {
        val sampleParsedConversation = ParsedConversation(
            title = "분석 테스트",
            exportedAtEpochMillis = null,
            participants = listOf("민지", "현우"),
            messages = listOf(
                ParsedMessage(
                    senderName = "민지",
                    sentAtEpochMillis = 1_710_000_000_000,
                    content = "대화를 다시 정리해보자",
                ),
                ParsedMessage(
                    senderName = "현우",
                    sentAtEpochMillis = 1_710_000_060_000,
                    content = "좋아, 차분하게 얘기해보자",
                ),
            ),
        )
    }
}
