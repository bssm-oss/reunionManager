package com.bssm.reunionmanager.data.importer

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KakaoTalkConversationParserTest {
    private val parser = KakaoTalkConversationParser()

    @Test
    fun parse_extractsTitleParticipantsAndMessages() {
        val parsed = parser.parse(
            fileName = "sample.txt",
            rawText = sampleConversation,
        )

        assertEquals("LLM RAG Langchain 통합", parsed.title)
        assertEquals(listOf("가나다", "J", "ABC"), parsed.participants)
        assertEquals(5, parsed.messages.size)
        assertEquals("안녕하세요", parsed.messages.first().content)
    }

    @Test
    fun parse_appendsMultilineContinuationToPreviousMessage() {
        val parsed = parser.parse(
            fileName = "sample.txt",
            rawText = sampleConversation,
        )

        assertTrue(parsed.messages[3].content.contains("오우 감사합니다"))
        assertTrue(parsed.messages[3].content.contains("rag 입문인데"))
    }

    @Test
    fun parse_ignoresSystemNoticeBetweenMessages() {
        val parsed = parser.parse(
            fileName = "sample.txt",
            rawText = conversationWithMidstreamSystemNotice,
        )

        assertEquals(2, parsed.messages.size)
        assertEquals("첫 번째 메시지", parsed.messages[0].content)
        assertEquals("두 번째 메시지", parsed.messages[1].content)
    }

    @Test
    fun parse_usesLocalTimezoneForExportedAndMessageTimes() {
        val parsed = parser.parse(
            fileName = "sample.txt",
            rawText = sampleConversation,
        )

        val expectedExportedAt = LocalDateTime.of(2024, 4, 5, 1, 36, 14)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val expectedFirstMessageAt = LocalDateTime.of(2024, 3, 27, 10, 55)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        assertEquals(expectedExportedAt, parsed.exportedAtEpochMillis)
        assertEquals(expectedFirstMessageAt, parsed.messages.first().sentAtEpochMillis)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parse_rejectsUnsupportedText() {
        parser.parse(
            fileName = "broken.txt",
            rawText = "this is not a KakaoTalk export",
        )
    }

    private companion object {
        val sampleConversation = """
            LLM RAG Langchain 통합 님과 카카오톡 대화
            저장한 날짜 : 2024-04-05 01:36:14

            --------------- 2024년 3월 27일 수요일 ---------------
            TEST님이 들어왔습니다.
            [가나다] [오전 10:55] 안녕하세요
            [가나다] [오전 10:57] 혹시 한국어에 유리한 임베딩 방법이 있을가요?
            [J] [오전 11:00] Bge m3 모델이 잘합니다
            [가나다] [오전 11:01] 오우 감사합니다
            rag 입문인데
            [ABC] [오전 11:05] OPENAI 임베딩 쓰는 것보다 효과가 좋은 것인가요?
        """.trimIndent()

        val conversationWithMidstreamSystemNotice = """
            샘플 채팅방 카카오톡 대화
            저장한 날짜 : 2024-04-05 01:36:14

            --------------- 2024년 3월 27일 수요일 ---------------
            [가나다] [오전 10:55] 첫 번째 메시지
            홍길동님이 나갔습니다.
            [ABC] [오전 10:56] 두 번째 메시지
        """.trimIndent()
    }
}
