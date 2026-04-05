package com.bssm.reunionmanager.domain.usecase

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.bssm.reunionmanager.data.analysis.FakeAnalysisProvider
import com.bssm.reunionmanager.data.analysis.GeminiAnalysisProvider
import com.bssm.reunionmanager.data.importer.ParsedConversation
import com.bssm.reunionmanager.data.importer.ParsedMessage
import com.bssm.reunionmanager.data.local.ReunionManagerDatabase
import com.bssm.reunionmanager.data.repository.AnalysisRepository
import com.bssm.reunionmanager.data.repository.ConversationRepository
import com.bssm.reunionmanager.data.repository.ProviderSettingsRepository
import com.bssm.reunionmanager.domain.model.ImportConversationResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun invoke_usesFakeProviderWhenApiKeyIsMissing() = runTest {
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
            geminiProviderFactory = { settings -> GeminiAnalysisProvider(settings) },
        )

        val result = useCase(importedId)

        assertTrue(result.isSuccess)
        assertEquals("fake", result.getOrNull())

        val detail = conversationRepository.observeConversationDetail(importedId).first()
        requireNotNull(detail)
        assertTrue(detail.latestAnalysis != null)
        assertTrue(detail.latestAnalysis!!.headline.contains("재회 초안"))
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
