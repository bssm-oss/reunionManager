package com.bssm.reunionmanager.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.bssm.reunionmanager.data.importer.ParsedConversation
import com.bssm.reunionmanager.data.importer.ParsedMessage
import com.bssm.reunionmanager.data.local.ReunionManagerDatabase
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
    }
}
