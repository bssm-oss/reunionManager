package com.bssm.reunionmanager.ui.navigation

sealed class ReunionDestination(
    val route: String,
    val title: String,
) {
    data object Home : ReunionDestination(route = "home", title = "Reunion Manager")
    data object Import : ReunionDestination(route = "import", title = "Import chat")
    data object Conversations : ReunionDestination(route = "conversations", title = "Saved chats")
    data object Settings : ReunionDestination(route = "settings", title = "AI settings")

    data object ConversationDetail : ReunionDestination(
        route = "conversation/{conversationId}",
        title = "Conversation",
    ) {
        fun createRoute(conversationId: Long): String = "conversation/$conversationId"
    }

    data object Analysis : ReunionDestination(
        route = "analysis/{conversationId}",
        title = "Reunion plan",
    ) {
        fun createRoute(conversationId: Long): String = "analysis/$conversationId"
    }
}
