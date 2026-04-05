package com.bssm.reunionmanager.domain.usecase

import com.bssm.reunionmanager.data.importer.KakaoTalkConversationParser
import com.bssm.reunionmanager.data.repository.ConversationRepository
import com.bssm.reunionmanager.domain.model.ImportConversationResult

class ImportConversationUseCase(
    private val parser: KakaoTalkConversationParser,
    private val repository: ConversationRepository,
) {
    suspend operator fun invoke(sourceName: String, rawText: String): ImportConversationResult {
        val parsedConversation = parser.parse(fileName = sourceName, rawText = rawText)
        return repository.importConversation(
            parsedConversation = parsedConversation,
            rawText = rawText,
            sourceName = sourceName,
        )
    }
}
