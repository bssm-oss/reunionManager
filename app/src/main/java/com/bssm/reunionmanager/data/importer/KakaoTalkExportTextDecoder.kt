package com.bssm.reunionmanager.data.importer

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

object KakaoTalkExportTextDecoder {
    fun decode(bytes: ByteArray): String {
        require(bytes.isNotEmpty()) { "선택한 파일이 비어 있습니다." }

        detectBom(bytes)?.let { bom ->
            return decodeStrict(bytes.copyOfRange(bom.offset, bytes.size), bom.charset)
        }

        fallbackCharsets.forEach { charset ->
            runCatching { decodeStrict(bytes, charset) }
                .getOrNull()
                ?.let { return it }
        }

        throw IllegalArgumentException("대화 파일의 문자 인코딩을 읽지 못했습니다. 카카오톡에서 다시 내보낸 파일을 선택하세요.")
    }

    private fun detectBom(bytes: ByteArray): BomCharset? {
        return when {
            bytes.startsWith(0xEF, 0xBB, 0xBF) -> BomCharset(charset = StandardCharsets.UTF_8, offset = 3)
            bytes.startsWith(0xFF, 0xFE) -> BomCharset(charset = StandardCharsets.UTF_16LE, offset = 2)
            bytes.startsWith(0xFE, 0xFF) -> BomCharset(charset = StandardCharsets.UTF_16BE, offset = 2)
            else -> null
        }
    }

    private fun decodeStrict(bytes: ByteArray, charset: Charset): String {
        return try {
            charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (exception: CharacterCodingException) {
            throw IllegalArgumentException("Unsupported text encoding: ${charset.name()}", exception)
        }
    }

    private fun ByteArray.startsWith(vararg prefix: Int): Boolean {
        return size >= prefix.size &&
            prefix.indices.all { index -> (this[index].toInt() and 0xFF) == prefix[index] }
    }

    private data class BomCharset(
        val charset: Charset,
        val offset: Int,
    )

    private val fallbackCharsets = listOfNotNull(
        StandardCharsets.UTF_8,
        charsetOrNull("MS949"),
        charsetOrNull("x-windows-949"),
        charsetOrNull("EUC-KR"),
    ).distinctBy { charset -> charset.name() }

    private fun charsetOrNull(name: String): Charset? {
        return runCatching { Charset.forName(name) }.getOrNull()
    }
}
