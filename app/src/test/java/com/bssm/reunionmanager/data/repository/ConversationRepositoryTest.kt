package com.bssm.reunionmanager.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.bssm.reunionmanager.data.importer.ParsedConversation
import com.bssm.reunionmanager.data.importer.ParsedMessage
import com.bssm.reunionmanager.data.local.ReunionManagerDatabase
import com.bssm.reunionmanager.domain.model.AnalysisReport
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
class ConversationRepositoryTest {
    private lateinit var database: ReunionManagerDatabase
    private lateinit var repository: ConversationRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ReunionManagerDatabase::class.java,
        ).allowMainThreadQueries().build()

        repository = ConversationRepository(
            database = database,
            conversationDao = database.conversationDao(),
            participantDao = database.participantDao(),
            messageDao = database.messageDao(),
            analysisResultDao = database.analysisResultDao(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun importConversation_persistsSummaryAndDetail() = runTest {
        val result = repository.importConversation(
            parsedConversation = sampleParsedConversation,
            rawText = "same raw text",
            sourceName = "sample.txt",
        )

        assertTrue(result is ImportConversationResult.Imported)
        val importedId = (result as ImportConversationResult.Imported).conversationId

        val summaries = repository.observeConversationSummaries().first()
        assertEquals(1, summaries.size)
        assertEquals("테스트 대화", summaries.first().title)

        val detail = repository.observeConversationDetail(importedId).first()
        requireNotNull(detail)
        assertEquals(2, detail.participantNames.size)
        assertEquals(2, detail.messages.size)
        assertEquals("안녕", detail.messages.first().content)
    }

    @Test
    fun observeConversationSummaries_includesLatestAnalysisHeadline() = runTest {
        val result = repository.importConversation(
            parsedConversation = sampleParsedConversation,
            rawText = "summary analysis raw text",
            sourceName = "summary-analysis.txt",
        )
        val importedId = (result as ImportConversationResult.Imported).conversationId
        val analysisRepository = AnalysisRepository(database.analysisResultDao())

        analysisRepository.saveLatest(
            conversationId = importedId,
            providerType = "fake",
            report = AnalysisReport(
                headline = "상대 답장에 짧게 응답",
                contactReadiness = "아주 가볍게 가능",
                evidence = "테스트 근거",
                relationshipSummary = "테스트 요약",
                reunionObjective = "짧게 답합니다.",
                nextStep = "한 문장만 준비하세요.",
                messageDraft = "메시지 봤어. 고마워.",
                alternativeDrafts = "메시지 봤어. 고마워.\n천천히 이야기하자\n답은 천천히 해도 돼",
                caution = "재촉하지 않습니다.",
            ),
        )

        val summary = repository.observeConversationSummaries().first().single()

        assertEquals("상대 답장에 짧게 응답", summary.latestAnalysisHeadline)
    }

    @Test
    fun importConversation_returnsDuplicateWhenHashMatches() = runTest {
        repository.importConversation(
            parsedConversation = sampleParsedConversation,
            rawText = "duplicate text",
            sourceName = "sample.txt",
        )

        val result = repository.importConversation(
            parsedConversation = sampleParsedConversation,
            rawText = "duplicate text",
            sourceName = "sample.txt",
        )

        assertTrue(result is ImportConversationResult.Duplicate)
    }

    @Test
    fun buildAnalysisInput_includesRecentSignalsAndStats() = runTest {
        val result = repository.importConversation(
            parsedConversation = signalHeavyConversation,
            rawText = "signal raw text",
            sourceName = "signal.txt",
        )
        val importedId = (result as ImportConversationResult.Imported).conversationId

        val input = repository.buildAnalysisInput(importedId)
        requireNotNull(input)

        assertEquals(18, input.messageCount)
        assertTrue(input.excerpt.contains("[최근 대화]"))
        assertTrue(input.signalExcerpt.contains("부담"))
        assertTrue(input.statsSummary.contains("마지막 메시지 발신자: 현우"))
        assertTrue(input.statsSummary.contains("마지막 발신자의 연속 발화: 2개"))
        assertTrue(input.statsSummary.contains("대화 기간:"))
        assertTrue(input.statsSummary.contains("마지막 메시지 전 공백:"))
        assertTrue(input.statsSummary.contains("마지막 메시지 이후 경과: 알 수 없음"))
        assertTrue(input.statsSummary.contains("6시간 이상 긴 공백"))
        assertTrue(input.perspectiveSummary.contains("마지막 메시지 발신자 역할: 알 수 없음"))
        assertTrue(input.perspectiveSummary.contains("내 카톡 이름: 설정되지 않음"))
        assertTrue(input.perspectiveSummary.contains("관점 주의"))
    }

    @Test
    fun buildAnalysisInput_includesElapsedTimeAfterLastMessageWhenExportTimeExists() = runTest {
        val baseTime = 1_710_000_000_000L
        val result = repository.importConversation(
            parsedConversation = ParsedConversation(
                title = "내보내기 시간 테스트",
                exportedAtEpochMillis = baseTime + 3 * 24 * 60 * 60 * 1000L,
                participants = listOf("민지", "현우"),
                messages = listOf(
                    ParsedMessage(
                        senderName = "현우",
                        sentAtEpochMillis = baseTime,
                        content = "잘 지냈어?",
                    ),
                    ParsedMessage(
                        senderName = "민지",
                        sentAtEpochMillis = baseTime + 60_000L,
                        content = "응, 잘 지내?",
                    ),
                ),
            ),
            rawText = "export elapsed raw text",
            sourceName = "export-elapsed.txt",
        )
        val importedId = (result as ImportConversationResult.Imported).conversationId

        val input = repository.buildAnalysisInput(importedId)

        requireNotNull(input)
        assertTrue(input.statsSummary.contains("마지막 메시지 이후 경과: 2일 23시간"))
    }

    @Test
    fun buildAnalysisInput_marksLastSenderRoleWhenUserDisplayNameIsKnown() = runTest {
        val result = repository.importConversation(
            parsedConversation = signalHeavyConversation,
            rawText = "signal raw text with user",
            sourceName = "signal-user.txt",
        )
        val importedId = (result as ImportConversationResult.Imported).conversationId

        val input = repository.buildAnalysisInput(
            conversationId = importedId,
            userDisplayName = "현우",
        )
        requireNotNull(input)

        assertTrue(input.perspectiveSummary.contains("내 카톡 이름: 현우"))
        assertTrue(input.perspectiveSummary.contains("상대 후보: 민지"))
        assertTrue(input.perspectiveSummary.contains("마지막 메시지 발신자 역할: 나"))
        assertTrue(input.perspectiveSummary.contains("내 최근 메시지: 오늘은 여기까지만 하자"))
        assertTrue(input.perspectiveSummary.contains("상대 최근 메시지: 그때는 내가 너무 부담스럽게 말했던 것 같아"))
        assertTrue(input.perspectiveSummary.contains("내 마지막 연속 발화: 2개"))
    }

    private companion object {
        val sampleParsedConversation = ParsedConversation(
            title = "테스트 대화",
            exportedAtEpochMillis = 1_710_000_000_000,
            participants = listOf("민지", "현우"),
            messages = listOf(
                ParsedMessage(
                    senderName = "민지",
                    sentAtEpochMillis = 1_710_000_000_000,
                    content = "안녕",
                ),
                ParsedMessage(
                    senderName = "현우",
                    sentAtEpochMillis = 1_710_000_060_000,
                    content = "오랜만이야",
                ),
            ),
        )

        val signalHeavyConversation = ParsedConversation(
            title = "신호 테스트",
            exportedAtEpochMillis = null,
            participants = listOf("민지", "현우"),
            messages = (0 until 15).map { index ->
                ParsedMessage(
                    senderName = if (index % 2 == 0) "민지" else "현우",
                    sentAtEpochMillis = 1_710_000_000_000 + index * 60_000L,
                    content = "일상 메시지 $index",
                )
            } + listOf(
                ParsedMessage(
                    senderName = "민지",
                    sentAtEpochMillis = 1_710_000_900_000,
                    content = "그때는 내가 너무 부담스럽게 말했던 것 같아",
                ),
                ParsedMessage(
                    senderName = "현우",
                    sentAtEpochMillis = 1_710_030_000_000,
                    content = "조금 천천히 얘기하고 싶어",
                ),
                ParsedMessage(
                    senderName = "현우",
                    sentAtEpochMillis = 1_710_030_060_000,
                    content = "오늘은 여기까지만 하자",
                ),
            ),
        )
    }
}
