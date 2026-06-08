package com.bssm.reunionmanager.data.repository

import androidx.room.withTransaction
import com.bssm.reunionmanager.data.importer.ParsedConversation
import com.bssm.reunionmanager.data.local.ReunionManagerDatabase
import com.bssm.reunionmanager.data.local.dao.AnalysisResultDao
import com.bssm.reunionmanager.data.local.dao.ConversationDao
import com.bssm.reunionmanager.data.local.dao.MessageDao
import com.bssm.reunionmanager.data.local.dao.ParticipantDao
import com.bssm.reunionmanager.data.local.entity.AnalysisResultEntity
import com.bssm.reunionmanager.data.local.entity.ConversationEntity
import com.bssm.reunionmanager.data.local.entity.MessageEntity
import com.bssm.reunionmanager.data.local.entity.ParticipantEntity
import com.bssm.reunionmanager.domain.model.AnalysisInput
import com.bssm.reunionmanager.domain.model.AnalysisReport
import com.bssm.reunionmanager.domain.model.ConversationDetail
import com.bssm.reunionmanager.domain.model.ConversationMessage
import com.bssm.reunionmanager.domain.model.ConversationSummary
import com.bssm.reunionmanager.domain.model.ImportConversationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.security.MessageDigest

class ConversationRepository(
    private val database: ReunionManagerDatabase,
    private val conversationDao: ConversationDao,
    private val participantDao: ParticipantDao,
    private val messageDao: MessageDao,
    private val analysisResultDao: AnalysisResultDao,
) {
    fun observeConversationSummaries(): Flow<List<ConversationSummary>> {
        return combine(
            conversationDao.observeAll(),
            analysisResultDao.observeLatestForAllConversations(),
        ) { conversations, latestAnalyses ->
            val latestAnalysisByConversationId = latestAnalyses.associateBy { analysis -> analysis.conversationId }
            conversations.map { entity ->
                ConversationSummary(
                    id = entity.id,
                    title = entity.title,
                    participantCount = entity.participantCount,
                    messageCount = entity.messageCount,
                    importedAtEpochMillis = entity.importedAtEpochMillis,
                    sourceName = entity.sourceName,
                    latestAnalysisHeadline = latestAnalysisByConversationId[entity.id]?.headline,
                )
            }
        }
    }

    fun observeConversationDetail(conversationId: Long): Flow<ConversationDetail?> {
        return combine(
            conversationDao.observeById(conversationId),
            participantDao.observeByConversationId(conversationId),
            messageDao.observeByConversationId(conversationId),
            analysisResultDao.observeLatestForConversation(conversationId),
        ) { conversation, participants, messages, latestAnalysis ->
            conversation?.let {
                ConversationDetail(
                    id = it.id,
                    title = it.title,
                    sourceName = it.sourceName,
                    participantNames = participants.map { participant -> participant.name },
                    messages = messages.map { message ->
                        ConversationMessage(
                            id = message.id,
                            senderName = message.senderName,
                            sentAtEpochMillis = message.sentAtEpochMillis,
                            content = message.content,
                        )
                    },
                    latestAnalysis = latestAnalysis?.toDomainModel(),
                )
            }
        }
    }

    suspend fun importConversation(
        parsedConversation: ParsedConversation,
        rawText: String,
        sourceName: String,
    ): ImportConversationResult {
        // Duplicate protection is still based on the imported transcript, with the UTF-8 BOM
        // normalized away so the same file is not saved twice because of an encoding marker.
        val sourceHash = rawText.stripUtf8Bom().sha256()
        conversationDao.findIdBySourceHash(sourceHash)?.let { existingId ->
            return ImportConversationResult.Duplicate(existingId)
        }

        val conversationId = database.withTransaction {
            val insertedId = conversationDao.insert(
                ConversationEntity(
                    title = parsedConversation.title,
                    sourceName = sourceName,
                    sourceHash = sourceHash,
                    importedAtEpochMillis = System.currentTimeMillis(),
                    exportedAtEpochMillis = parsedConversation.exportedAtEpochMillis,
                    participantCount = parsedConversation.participants.size,
                    messageCount = parsedConversation.messages.size,
                ),
            )

            participantDao.insertAll(
                parsedConversation.participants.map { name ->
                    ParticipantEntity(
                        conversationId = insertedId,
                        name = name,
                    )
                },
            )

            messageDao.insertAll(
                parsedConversation.messages.mapIndexed { index, message ->
                    MessageEntity(
                        conversationId = insertedId,
                        sequenceIndex = index,
                        senderName = message.senderName,
                        sentAtEpochMillis = message.sentAtEpochMillis,
                        content = message.content,
                    )
                },
            )

            insertedId
        }

        return ImportConversationResult.Imported(conversationId)
    }

    suspend fun buildAnalysisInput(
        conversationId: Long,
        userDisplayName: String = "",
    ): AnalysisInput? {
        val conversation = conversationDao.getById(conversationId) ?: return null
        val participants = participantDao.getByConversationId(conversationId).map { it.name }
        val messages = messageDao.getByConversationId(conversationId)
        val openingMessages = messages.take(8)
        val recentMessages = messages.takeLast(40)
        val signalMessages = messages.signalWindow()
        val excerpt = buildString {
            appendSection("초반 대화", openingMessages.formatForAnalysis())
            appendSection("감정/경계 신호 주변", signalMessages.formatForAnalysis())
            appendSection("최근 대화", recentMessages.formatForAnalysis())
        }.trim()

        return AnalysisInput(
            conversationTitle = conversation.title,
            participantNames = participants,
            messageCount = messages.size,
            excerpt = excerpt,
            recentExcerpt = recentMessages.formatForAnalysis(),
            signalExcerpt = signalMessages.formatForAnalysis(),
            statsSummary = messages.toStatsSummary(conversation.exportedAtEpochMillis),
            perspectiveSummary = messages.toPerspectiveSummary(
                userDisplayName = userDisplayName.trim(),
                participantNames = participants,
            ),
        )
    }

    suspend fun deleteConversation(conversationId: Long): Boolean {
        return conversationDao.deleteById(conversationId) > 0
    }

    private fun String.sha256(): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun String.stripUtf8Bom(): String {
        return removePrefix("\uFEFF")
    }

    private fun AnalysisResultEntity.toDomainModel(): AnalysisReport {
        return AnalysisReport(
            headline = headline,
            contactReadiness = contactReadiness.ifBlank { "정보 부족" },
            evidence = evidence.ifBlank { "대화 근거가 부족해 최근 흐름을 더 확인해야 합니다." },
            relationshipSummary = relationshipSummary,
            reunionObjective = reunionObjective,
            nextStep = nextStep,
            messageDraft = messageDraft.ifBlank {
                "오랜만이야. 부담 주려는 건 아니고, 괜찮다면 한 번 차분하게 이야기해보고 싶어."
            },
            alternativeDrafts = alternativeDrafts,
            caution = caution,
        )
    }

    private fun StringBuilder.appendSection(title: String, body: String) {
        if (body.isBlank()) {
            return
        }
        if (isNotEmpty()) {
            appendLine()
        }
        appendLine("[$title]")
        appendLine(body)
    }

    private fun List<MessageEntity>.signalWindow(): List<MessageEntity> {
        if (isEmpty()) {
            return emptyList()
        }
        val signalIndexes = mapIndexedNotNull { index, message ->
            if (message.content.hasAnalysisSignal()) index else null
        }
        val windowIndexes = signalIndexes
            .flatMap { index -> (index - 1)..(index + 1) }
            .filter { index -> index in indices }
            .toSortedSet()
        return windowIndexes.map { index -> this[index] }.takeLast(24)
    }

    private fun List<MessageEntity>.formatForAnalysis(): String {
        return joinToString(separator = "\n") { message ->
            "${message.senderName}: ${message.content.compactForAnalysis()}"
        }
    }

    private fun List<MessageEntity>.toStatsSummary(exportedAtEpochMillis: Long?): String {
        if (isEmpty()) {
            return "저장된 메시지가 없습니다."
        }

        val lastMessage = last()
        val firstMessage = first()
        val lastGapMillis = if (size >= 2) {
            lastMessage.sentAtEpochMillis - this[size - 2].sentAtEpochMillis
        } else {
            0L
        }
        val conversationSpanMillis = lastMessage.sentAtEpochMillis - firstMessage.sentAtEpochMillis
        val afterLastMessageMillis = exportedAtEpochMillis
            ?.minus(lastMessage.sentAtEpochMillis)
            ?.takeIf { millis -> millis >= 0L }
        val largestGapMillis = zipWithNext()
            .maxOfOrNull { (previous, next) -> next.sentAtEpochMillis - previous.sentAtEpochMillis }
            ?: 0L
        val longGapCount = zipWithNext()
            .count { (previous, next) -> next.sentAtEpochMillis - previous.sentAtEpochMillis >= LONG_GAP_MILLIS }
        val lastSenderRun = lastSenderRunCount()
        val signalCount = count { message -> message.content.hasAnalysisSignal() }
        val senderCounts = groupBy { message -> message.senderName }
            .mapValues { (_, senderMessages) -> senderMessages.size }
            .entries
            .sortedByDescending { entry -> entry.value }
            .joinToString(separator = ", ") { entry -> "${entry.key} ${entry.value}개" }

        return buildString {
            appendLine("전체 메시지: ${size}개")
            appendLine("참여자별 메시지: $senderCounts")
            appendLine("첫 메시지 발신자: ${firstMessage.senderName}")
            appendLine("마지막 메시지 발신자: ${lastMessage.senderName}")
            appendLine("마지막 메시지: ${lastMessage.content.compactForAnalysis(maxLength = 80)}")
            appendLine("마지막 발신자의 연속 발화: ${lastSenderRun}개")
            appendLine("대화 기간: ${conversationSpanMillis.toHoursLabel()}")
            appendLine("마지막 메시지 전 공백: ${lastGapMillis.toHoursLabel()}")
            appendLine("마지막 메시지 이후 경과: ${afterLastMessageMillis?.toHoursLabel() ?: "알 수 없음"}")
            appendLine("6시간 이상 긴 공백: ${longGapCount}회")
            appendLine("가장 긴 공백: ${largestGapMillis.toHoursLabel()}")
            append("감정/경계 신호 메시지: ${signalCount}개")
        }
    }

    private fun List<MessageEntity>.toPerspectiveSummary(
        userDisplayName: String,
        participantNames: List<String>,
    ): String {
        val normalizedUserDisplayName = userDisplayName.trim()
        if (isEmpty()) {
            return "내 카톡 이름: ${normalizedUserDisplayName.ifBlank { "설정되지 않음" }}\n발신자 역할: 알 수 없음"
        }

        val lastMessage = last()
        val lastSenderRun = lastSenderRunCount()
        val isUserNameConfigured = normalizedUserDisplayName.isNotBlank()
        val isUserKnown = isUserNameConfigured && participantNames.contains(normalizedUserDisplayName)
        val lastSenderRole = when {
            !isUserKnown -> "알 수 없음"
            lastMessage.senderName == normalizedUserDisplayName -> "나"
            else -> "상대"
        }
        val counterpartNames = if (isUserKnown) {
            participantNames.filterNot { name -> name == normalizedUserDisplayName }
        } else {
            emptyList()
        }
        val myMessageCount = if (isUserKnown) count { message -> message.senderName == normalizedUserDisplayName } else 0
        val counterpartMessageCount = if (isUserKnown) size - myMessageCount else 0
        val myFinalRun = if (lastSenderRole == "나") lastSenderRun else 0
        val counterpartFinalRun = if (lastSenderRole == "상대") lastSenderRun else 0
        val myRecentMessage = if (isUserKnown) {
            lastOrNull { message -> message.senderName == normalizedUserDisplayName }
        } else {
            null
        }
        val counterpartRecentMessage = if (isUserKnown) {
            lastOrNull { message -> message.senderName != normalizedUserDisplayName }
        } else {
            null
        }

        return buildString {
            appendLine("내 카톡 이름: ${normalizedUserDisplayName.ifBlank { "설정되지 않음" }}")
            if (isUserNameConfigured && !isUserKnown) {
                appendLine("내 카톡 이름 확인 필요: 저장한 이름이 이 대화 참가자와 일치하지 않습니다.")
            }
            appendLine("상대 후보: ${counterpartNames.joinToString().ifBlank { "알 수 없음" }}")
            appendLine("마지막 메시지 발신자 역할: $lastSenderRole")
            appendLine("마지막 연속 발화 역할: $lastSenderRole ${lastSenderRun}개")
            if (isUserKnown) {
                appendLine("내 메시지: ${myMessageCount}개")
                appendLine("상대 메시지: ${counterpartMessageCount}개")
                appendLine("내 최근 메시지: ${myRecentMessage?.content?.compactForAnalysis(maxLength = 80) ?: "없음"}")
                appendLine("상대 최근 메시지: ${counterpartRecentMessage?.content?.compactForAnalysis(maxLength = 80) ?: "없음"}")
                appendLine("내 마지막 연속 발화: ${myFinalRun}개")
                append("상대 마지막 연속 발화: ${counterpartFinalRun}개")
            } else if (isUserNameConfigured) {
                append("관점 주의: 저장한 내 카톡 이름이 대화 참가자와 일치하지 않아 마지막 발신자가 사용자인지 상대인지 확정할 수 없습니다.")
            } else {
                append("관점 주의: 내 카톡 이름이 설정되지 않아 마지막 발신자가 사용자인지 상대인지 확정할 수 없습니다.")
            }
        }
    }

    private fun List<MessageEntity>.lastSenderRunCount(): Int {
        if (isEmpty()) {
            return 0
        }
        val lastSender = last().senderName
        return asReversed()
            .takeWhile { message -> message.senderName == lastSender }
            .size
    }

    private fun String.compactForAnalysis(maxLength: Int = 160): String {
        val compact = replace(Regex("\\s+"), " ").trim()
        return if (compact.length <= maxLength) compact else compact.take(maxLength - 1) + "…"
    }

    private fun String.hasAnalysisSignal(): Boolean {
        val normalized = lowercase()
        return ANALYSIS_SIGNAL_KEYWORDS.any { keyword -> normalized.contains(keyword) }
    }

    private fun Long.toHoursLabel(): String {
        if (this <= 0L) {
            return "없음"
        }
        val hours = this / HOUR_MILLIS
        return when {
            hours < 1L -> "1시간 미만"
            hours < 24L -> "${hours}시간"
            else -> "${hours / 24L}일 ${hours % 24L}시간"
        }
    }

    private companion object {
        const val HOUR_MILLIS: Long = 60L * 60L * 1000L
        const val LONG_GAP_MILLIS: Long = 6L * HOUR_MILLIS
        val ANALYSIS_SIGNAL_KEYWORDS = listOf(
            "미안",
            "사과",
            "고마워",
            "괜찮",
            "보고싶",
            "보고 싶",
            "생각났",
            "그리",
            "좋아",
            "만나",
            "얘기",
            "대화",
            "부담",
            "불편",
            "괜찮지 않아",
            "싫어",
            "그만",
            "하지마",
            "답장하지",
            "연락하지",
            "차단",
            "힘들",
            "짜증",
            "화나",
            "헤어",
            "정리",
            "끝",
        )
    }
}
