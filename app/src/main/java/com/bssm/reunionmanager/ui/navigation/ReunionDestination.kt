package com.bssm.reunionmanager.ui.navigation

sealed class ReunionDestination(
    val route: String,
    val title: String,
) {
    data object Home : ReunionDestination(route = "home", title = "재회 플랜")
    data object Import : ReunionDestination(route = "import", title = "대화 흐름")
    data object Conversations : ReunionDestination(route = "conversations", title = "지난 플랜")
    data object Settings : ReunionDestination(route = "settings", title = "설정")

    data object ConversationDetail : ReunionDestination(
        route = "conversation/{conversationId}",
        title = "대화 기록",
    ) {
        fun createRoute(conversationId: Long): String = "conversation/$conversationId"
    }

    data object Analysis : ReunionDestination(
        route = "analysis/{conversationId}",
        title = "플랜",
    ) {
        fun createRoute(conversationId: Long): String = "analysis/$conversationId"
    }
}
