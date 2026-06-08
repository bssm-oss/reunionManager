package com.bssm.reunionmanager.data.importer

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class KakaoTalkConversationParser {
    private val exportDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val csvDateFormatters = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.KOREAN),
        DateTimeFormatter.ofPattern("yyyy-M-d H:mm:ss", Locale.KOREAN),
        DateTimeFormatter.ofPattern("yyyy. M. d. a h:mm", Locale.KOREAN),
        DateTimeFormatter.ofPattern("yyyy년 M월 d일 a h:mm", Locale.KOREAN),
    )
    private val dateDividerRegex = Regex("-+\\s*(\\d{4})년\\s*(\\d{1,2})월\\s*(\\d{1,2})일\\s*[^\\d-]+-+")
    private val messageRegex = Regex("^\\[([^]]+)] \\[([^]]+)] (.*)$")
    private val exportDateRegex = Regex("^저장한 날짜\\s*:\\s*(.+)$")
    private val corpusLineRegex = Regex("^\\s*(\\d{4}-\\d{1,2}-\\d{1,2}\\s+\\d{1,2}:\\d{2}:\\d{2})\\s*,\\s*([^:]+?)\\s*:\\s*(.*)\\s*$")
    // KakaoTalk exports interleave system notices with user messages. These lines are skipped so
    // they do not silently contaminate the preceding message body.
    private val systemNoticeRegex = Regex(".*(님이 들어왔습니다\\.?|님이 나갔습니다\\.?|님을 초대했습니다\\.?|불법촬영물|식별 및 게재제한 조치 안내).*")
    // Export timestamps are local wall-clock values, so they must be preserved in the device's
    // local zone instead of being forced into UTC at parse time.
    private val localZoneId: ZoneId = ZoneId.systemDefault()

    fun parse(fileName: String, rawText: String): ParsedConversation {
        val normalizedRawText = rawText.stripUtf8Bom()
        return parseMobileTextExport(fileName = fileName, rawText = normalizedRawText)
            ?: parseCsvLikeExport(fileName = fileName, rawText = normalizedRawText)
            ?: throw IllegalArgumentException("지원하는 카카오톡 대화 파일이 아닙니다. .txt 또는 CSV 내보내기 파일을 선택하세요.")
    }

    private fun parseMobileTextExport(fileName: String, rawText: String): ParsedConversation? {
        val normalizedLines = rawText.replace("\r\n", "\n").split('\n')
        val title = normalizedLines.firstOrNull().orEmpty().extractTitle(fileName)

        var currentDate: LocalDate? = null
        var exportedAtEpochMillis: Long? = null
        val participants = linkedSetOf<String>()
        val messages = mutableListOf<MutableParsedMessage>()

        normalizedLines.forEach { rawLine ->
            val line = rawLine.trimEnd()
            if (line.isBlank()) {
                return@forEach
            }

            if (exportedAtEpochMillis == null) {
                exportDateRegex.find(line)?.groupValues?.getOrNull(1)?.let { value ->
                    exportedAtEpochMillis = LocalDateTime.parse(value.trim(), exportDateFormatter)
                        .atZone(localZoneId)
                        .toInstant()
                        .toEpochMilli()
                    return@forEach
                }
            }

            dateDividerRegex.matchEntire(line)?.let { match ->
                currentDate = LocalDate.of(
                    match.groupValues[1].toInt(),
                    match.groupValues[2].toInt(),
                    match.groupValues[3].toInt(),
                )
                return@forEach
            }

            val messageMatch = messageRegex.matchEntire(line)
            if (messageMatch != null && currentDate != null) {
                val senderName = messageMatch.groupValues[1].trim()
                val sentAtEpochMillis = parseMobileTime(currentDate = currentDate!!, timeToken = messageMatch.groupValues[2])
                val content = messageMatch.groupValues[3].trim()

                participants += senderName
                messages += MutableParsedMessage(
                    senderName = senderName,
                    sentAtEpochMillis = sentAtEpochMillis,
                    content = StringBuilder(content),
                )
                return@forEach
            }

            if (systemNoticeRegex.matches(line)) {
                return@forEach
            }

            if (messages.isEmpty()) {
                return@forEach
            }

            messages.last().content.append("\n").append(line.trim())
        }

        if (messages.isEmpty()) {
            return null
        }

        return ParsedConversation(
            title = title,
            exportedAtEpochMillis = exportedAtEpochMillis,
            participants = participants.toList(),
            messages = messages.toParsedMessages(),
        )
    }

    private fun parseCsvLikeExport(fileName: String, rawText: String): ParsedConversation? {
        val corpusParsed = parseCorpusStyleLines(rawText)
        if (corpusParsed != null) {
            return corpusParsed.toParsedConversation(fileName)
        }

        val rows = parseCsvRows(rawText)
            .map { row -> row.map { it.trim() } }
            .filter { row -> row.any { it.isNotBlank() } }
        if (rows.isEmpty()) {
            return null
        }

        val headerIndex = rows.indexOfFirst { row -> row.any { it.toHeaderKey() in supportedHeaderKeys } }
        val columnMap = if (headerIndex >= 0) rows[headerIndex].toColumnMap() else CsvColumnMap(date = 0, sender = 1, message = 2)
        if (!columnMap.isComplete) {
            return null
        }

        val dataRows = rows.drop(if (headerIndex >= 0) headerIndex + 1 else 0)
        val parsed = ParsedCsvAccumulator()
        dataRows.forEach { row ->
            val sentAt = row.getOrNull(columnMap.date)?.parseCsvDateTime() ?: return@forEach
            val senderName = row.getOrNull(columnMap.sender)?.trim().orEmpty()
            val content = row.getOrNull(columnMap.message)?.trim().orEmpty()
            if (senderName.isBlank() || content.isBlank() || systemNoticeRegex.matches(content)) {
                return@forEach
            }
            parsed.add(senderName = senderName, sentAt = sentAt, content = content)
        }

        return parsed.toParsedConversation(fileName)
    }

    private fun parseCorpusStyleLines(rawText: String): ParsedCsvAccumulator? {
        val parsed = ParsedCsvAccumulator()
        rawText.replace("\r\n", "\n").lineSequence().forEach { rawLine ->
            val match = corpusLineRegex.matchEntire(rawLine) ?: return@forEach
            val sentAt = match.groupValues[1].parseCsvDateTime() ?: return@forEach
            val senderName = match.groupValues[2].trim()
            val content = match.groupValues[3].trim()
            if (senderName.isBlank() || content.isBlank() || systemNoticeRegex.matches(content)) {
                return@forEach
            }
            parsed.add(senderName = senderName, sentAt = sentAt, content = content)
        }
        return parsed.takeIf { it.hasMessages }
    }

    private fun parseMobileTime(currentDate: LocalDate, timeToken: String): Long {
        val parts = timeToken.trim().split(' ')
        require(parts.size == 2) { "Unsupported KakaoTalk time token: $timeToken" }

        val period = parts[0]
        val hourMinute = parts[1].split(':')
        require(hourMinute.size == 2) { "Unsupported KakaoTalk time token: $timeToken" }

        var hour = hourMinute[0].toInt()
        val minute = hourMinute[1].toInt()

        if (period == "오후" && hour != 12) {
            hour += 12
        } else if (period == "오전" && hour == 12) {
            hour = 0
        }

        return currentDate.atTime(hour, minute).atZone(localZoneId).toInstant().toEpochMilli()
    }

    private fun String.parseCsvDateTime(): Long? {
        val value = trim().trim('"')
        return csvDateFormatters.firstNotNullOfOrNull { formatter ->
            runCatching {
                LocalDateTime.parse(value, formatter)
                    .atZone(localZoneId)
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()
        }
    }

    private fun parseCsvRows(rawText: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var index = 0
        val normalized = rawText.replace("\r\n", "\n")

        while (index < normalized.length) {
            val char = normalized[index]
            when {
                char == '"' && inQuotes && normalized.getOrNull(index + 1) == '"' -> {
                    currentField.append('"')
                    index += 1
                }

                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    currentRow += currentField.toString()
                    currentField.clear()
                }

                char == '\n' && !inQuotes -> {
                    currentRow += currentField.toString()
                    currentField.clear()
                    rows += currentRow.toList()
                    currentRow.clear()
                }

                else -> currentField.append(char)
            }
            index += 1
        }

        currentRow += currentField.toString()
        if (currentRow.any { it.isNotBlank() }) {
            rows += currentRow.toList()
        }

        return rows
    }

    private fun List<String>.toColumnMap(): CsvColumnMap {
        val normalized = map { it.toHeaderKey() }
        return CsvColumnMap(
            date = normalized.indexOfFirst { it in dateHeaderKeys },
            sender = normalized.indexOfFirst { it in senderHeaderKeys },
            message = normalized.indexOfFirst { it in messageHeaderKeys },
        )
    }

    private fun String.toHeaderKey(): String {
        return stripUtf8Bom()
            .lowercase(Locale.ROOT)
            .replace(" ", "")
            .replace("_", "")
            .replace("-", "")
    }

    private fun String.stripUtf8Bom(): String {
        return removePrefix("\uFEFF")
    }

    private fun String.extractTitle(fileName: String): String {
        val cleaned = trim().removeSuffix(" 님과 카카오톡 대화").removeSuffix(" 카카오톡 대화")
        return cleaned.ifBlank { fileName.toConversationTitle() }
    }

    private fun String.toConversationTitle(): String {
        return substringBeforeLast('.').ifBlank { "Imported KakaoTalk conversation" }
    }

    private fun List<MutableParsedMessage>.toParsedMessages(): List<ParsedMessage> {
        return map {
            ParsedMessage(
                senderName = it.senderName,
                sentAtEpochMillis = it.sentAtEpochMillis,
                content = it.content.toString(),
            )
        }
    }

    private fun ParsedCsvAccumulator.toParsedConversation(fileName: String): ParsedConversation? {
        if (!hasMessages) {
            return null
        }
        return ParsedConversation(
            title = fileName.toConversationTitle(),
            exportedAtEpochMillis = null,
            participants = participants.toList(),
            messages = messages.toParsedMessages(),
        )
    }

    private data class MutableParsedMessage(
        val senderName: String,
        val sentAtEpochMillis: Long,
        val content: StringBuilder,
    )

    private data class CsvColumnMap(
        val date: Int,
        val sender: Int,
        val message: Int,
    ) {
        val isComplete: Boolean = date >= 0 && sender >= 0 && message >= 0
    }

    private class ParsedCsvAccumulator {
        val participants = linkedSetOf<String>()
        val messages = mutableListOf<MutableParsedMessage>()
        val hasMessages: Boolean
            get() = messages.isNotEmpty()

        fun add(senderName: String, sentAt: Long, content: String) {
            participants += senderName
            messages += MutableParsedMessage(
                senderName = senderName,
                sentAtEpochMillis = sentAt,
                content = StringBuilder(content),
            )
        }
    }

    private companion object {
        val dateHeaderKeys = setOf("date", "datetime", "time", "timestamp", "날짜", "일시", "시간", "대화날짜")
        val senderHeaderKeys = setOf("user", "sender", "name", "대화명", "사용자", "이름", "발신자")
        val messageHeaderKeys = setOf("message", "messege", "text", "content", "메시지", "메세지", "내용", "대화내용")
        val supportedHeaderKeys = dateHeaderKeys + senderHeaderKeys + messageHeaderKeys
    }
}
