package com.bssm.reunionmanager.data.importer

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Test

class KakaoTalkExportTextDecoderTest {
    @Test
    fun decode_readsUtf8Text() {
        val decoded = KakaoTalkExportTextDecoder.decode(sampleConversation.toByteArray(StandardCharsets.UTF_8))

        assertEquals(sampleConversation, decoded)
    }

    @Test
    fun decode_stripsUtf8Bom() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
            sampleConversation.toByteArray(StandardCharsets.UTF_8)

        val decoded = KakaoTalkExportTextDecoder.decode(bytes)

        assertEquals(sampleConversation, decoded)
    }

    @Test
    fun decode_readsMs949KoreanExports() {
        val decoded = KakaoTalkExportTextDecoder.decode(sampleConversation.toByteArray(Charset.forName("MS949")))

        assertEquals(sampleConversation, decoded)
    }

    @Test
    fun decode_readsUtf16LittleEndianBomCsvExports() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) +
            sampleCsvConversation.toByteArray(StandardCharsets.UTF_16LE)

        val decoded = KakaoTalkExportTextDecoder.decode(bytes)

        assertEquals(sampleCsvConversation, decoded)
    }

    @Test(expected = IllegalArgumentException::class)
    fun decode_rejectsEmptyFile() {
        KakaoTalkExportTextDecoder.decode(ByteArray(0))
    }

    private companion object {
        val sampleConversation = """
            민지 님과 카카오톡 대화
            저장한 날짜 : 2024-04-05 01:36:14

            --------------- 2024년 3월 27일 수요일 ---------------
            [민지] [오전 10:55] 오랜만이야. 잘 지냈어?
            [현우] [오전 10:56] 응 잘 지냈어
        """.trimIndent()

        val sampleCsvConversation = """
            Date,User,Message
            2024-03-27 10:55:00,민지,오랜만이야. 잘 지냈어?
            2024-03-27 10:56:00,현우,응 잘 지냈어
        """.trimIndent()
    }
}
