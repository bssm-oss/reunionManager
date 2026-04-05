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
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

class ConversationRepository(
    private val database: ReunionManagerDatabase,
    private val conversationDao: ConversationDao,
    private val participantDao: ParticipantDao,
    private val messageDao: MessageDao,
    private val analysisResultDao: AnalysisResultDao,
) {
    fun observeConversationSummaries(): Flow<List<ConversationSummary>> {
        return conversationDao.observeAll().map { conversations ->
            conversations.map { entity ->
                ConversationSummary(
                    id = entity.id,
                    title = entity.title,
                    participantCount = entity.participantCount,
                    messageCount = entity.messageCount,
                    importedAtEpochMillis = entity.importedAtEpochMillis,
                    sourceName = entity.sourceName,
                    latestAnalysisHeadline = null,
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
        val sourceHash = rawText.sha256()
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

    suspend fun buildAnalysisInput(conversationId: Long): AnalysisInput? {
        val conversation = conversationDao.getById(conversationId) ?: return null
        val participants = participantDao.getByConversationId(conversationId).map { it.name }
        val messages = messageDao.getByConversationId(conversationId)
        val excerpt = messages.take(12).joinToString(separator = "\n") { message ->
            "${message.senderName}: ${message.content}"
        }

        return AnalysisInput(
            conversationTitle = conversation.title,
            participantNames = participants,
            messageCount = messages.size,
            excerpt = excerpt,
        )
    }

    private fun String.sha256(): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun AnalysisResultEntity.toDomainModel(): AnalysisReport {
        return AnalysisReport(
            headline = headline,
            relationshipSummary = relationshipSummary,
            reunionObjective = reunionObjective,
            nextStep = nextStep,
            caution = caution,
        )
    }
}
