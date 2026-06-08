package com.bssm.reunionmanager.data.importer

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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
    fun parse_supportsPcCsvExportWithDateUserMessageColumns() {
        val parsed = parser.parse(
            fileName = "pc-export.csv",
            rawText = pcCsvConversation,
        )

        assertEquals("pc-export", parsed.title)
        assertEquals(listOf("민지", "현우"), parsed.participants)
        assertEquals(4, parsed.messages.size)
        assertEquals("오랜만이야. 잘 지냈어?", parsed.messages[0].content)
        assertEquals("사진", parsed.messages[2].content)
        assertTrue(parsed.messages[3].content.contains("줄바꿈까지"))
    }

    @Test
    fun parse_supportsPcCsvExportWithUtf8Bom() {
        val parsed = parser.parse(
            fileName = "pc-export.csv",
            rawText = "\uFEFF$pcCsvConversation",
        )

        assertEquals("pc-export", parsed.title)
        assertEquals(listOf("민지", "현우"), parsed.participants)
        assertEquals(4, parsed.messages.size)
    }

    @Test
    fun parse_stripsUtf8BomFromMobileTitle() {
        val parsed = parser.parse(
            fileName = "sample.txt",
            rawText = "\uFEFF$sampleConversation",
        )

        assertEquals("LLM RAG Langchain 통합", parsed.title)
        assertEquals(listOf("가나다", "J", "ABC"), parsed.participants)
    }

    @Test
    fun parse_usesFileNameWhenMobileExportStartsWithDateDivider() {
        val parsed = parser.parse(
            fileName = "reunion_kakao_sample.txt",
            rawText = dateDividerFirstConversation,
        )

        assertEquals("reunion_kakao_sample", parsed.title)
        assertEquals(listOf("현우", "민지"), parsed.participants)
        assertEquals(2, parsed.messages.size)
        assertEquals("오랜만이야. 잘 지내?", parsed.messages.first().content)
    }

    @Test
    fun parse_supportsCorpusStyleMessengerLines() {
        val parsed = parser.parse(
            fileName = "public-corpus-format.txt",
            rawText = corpusStyleConversation,
        )

        assertEquals("public-corpus-format", parsed.title)
        assertEquals(listOf("P1", "P2"), parsed.participants)
        assertEquals(4, parsed.messages.size)
        assertEquals("이모티콘", parsed.messages[1].content)
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

    @Test
    fun parse_rejectsUnsupportedText() {
        try {
            parser.parse(
                fileName = "broken.txt",
                rawText = "this is not a KakaoTalk export",
            )
            fail("Unsupported text should not be parsed.")
        } catch (exception: IllegalArgumentException) {
            assertEquals(
                "지원하는 카카오톡 대화 파일이 아닙니다. .txt 또는 CSV 내보내기 파일을 선택하세요.",
                exception.message,
            )
        }
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

        val dateDividerFirstConversation = """
            --------------- 2026년 6월 8일 월요일 ---------------
            [현우] [오전 10:55] 오랜만이야. 잘 지내?
            [민지] [오전 11:03] 응 나도 잘 지내. 너는?
        """.trimIndent()

        val conversationWithMidstreamSystemNotice = """
            샘플 채팅방 카카오톡 대화
            저장한 날짜 : 2024-04-05 01:36:14

            --------------- 2024년 3월 27일 수요일 ---------------
            [가나다] [오전 10:55] 첫 번째 메시지
            홍길동님이 나갔습니다.
            [ABC] [오전 10:56] 두 번째 메시지
        """.trimIndent()

        val pcCsvConversation = """
            Date,User,Message
            2024-03-27 10:55:00,민지,오랜만이야. 잘 지냈어?
            2024-03-27 10:56:00,현우,응 잘 지냈어
            2024-03-27 10:57:00,민지,사진
            2024-03-27 10:58:00,현우,"나도 가끔 생각났어
            줄바꿈까지 있는 메시지야"
        """.trimIndent()

        val corpusStyleConversation = """
            2019-11-04 22:25:00 , P1 : 콜라잇오
            2019-11-04 22:25:00 , P1 : 이모티콘
            2019-11-04 22:25:00 , P2 : ㅋㄱㅋㄱㅋㄱㅋ콜라
            2019-11-04 22:26:00 , P1 : 사진
        """.trimIndent()
    }
}
