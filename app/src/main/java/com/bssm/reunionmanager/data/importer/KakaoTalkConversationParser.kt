package com.bssm.reunionmanager.data.importer

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class KakaoTalkConversationParser {
    private val exportDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val dateDividerRegex = Regex("-+\\s*(\\d{4})년\\s*(\\d{1,2})월\\s*(\\d{1,2})일\\s*[^\\d-]+-+")
    private val messageRegex = Regex("^\\[([^]]+)] \\[([^]]+)] (.*)$")
    private val exportDateRegex = Regex("^저장한 날짜\\s*:\\s*(.+)$")
    // KakaoTalk exports interleave system notices with user messages. These lines are skipped so
    // they do not silently contaminate the preceding message body.
    private val systemNoticeRegex = Regex(".*(님이 들어왔습니다\\.?|님이 나갔습니다\\.?|님을 초대했습니다\\.?|불법촬영물|식별 및 게재제한 조치 안내).*")
    // Export timestamps are local wall-clock values, so they must be preserved in the device's
    // local zone instead of being forced into UTC at parse time.
    private val localZoneId: ZoneId = ZoneId.systemDefault()

    fun parse(fileName: String, rawText: String): ParsedConversation {
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
                val sentAtEpochMillis = parseTime(currentDate = currentDate!!, timeToken = messageMatch.groupValues[2])
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
            throw IllegalArgumentException("The selected file does not contain a supported KakaoTalk conversation.")
        }

        return ParsedConversation(
            title = title,
            exportedAtEpochMillis = exportedAtEpochMillis,
            participants = participants.toList(),
            messages = messages.map {
                ParsedMessage(
                    senderName = it.senderName,
                    sentAtEpochMillis = it.sentAtEpochMillis,
                    content = it.content.toString(),
                )
            },
        )
    }

    private fun parseTime(currentDate: LocalDate, timeToken: String): Long {
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

    private fun String.extractTitle(fileName: String): String {
        val cleaned = trim().removeSuffix(" 님과 카카오톡 대화").removeSuffix(" 카카오톡 대화")
        return cleaned.ifBlank {
            fileName.substringBeforeLast('.').ifBlank { "Imported KakaoTalk conversation" }
        }
    }

    private data class MutableParsedMessage(
        val senderName: String,
        val sentAtEpochMillis: Long,
        val content: StringBuilder,
    )
}
