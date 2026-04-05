package com.bssm.reunionmanager.domain.model

sealed interface ImportConversationResult {
    data class Imported(val conversationId: Long) : ImportConversationResult

    data class Duplicate(val conversationId: Long) : ImportConversationResult
}
