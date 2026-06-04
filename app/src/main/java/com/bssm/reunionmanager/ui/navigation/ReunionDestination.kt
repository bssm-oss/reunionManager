package com.bssm.reunionmanager.ui.navigation

sealed class ReunionDestination(
    val route: String,
    val title: String,
) {
    data object Home : ReunionDestination(route = "home", title = "재회 매니저")
    data object Import : ReunionDestination(route = "import", title = "대화 가져오기")
    data object Conversations : ReunionDestination(route = "conversations", title = "저장한 대화")
    data object Settings : ReunionDestination(route = "settings", title = "로컬 AI")

    data object ConversationDetail : ReunionDestination(
        route = "conversation/{conversationId}",
        title = "대화 보기",
    ) {
        fun createRoute(conversationId: Long): String = "conversation/$conversationId"
    }

    data object Analysis : ReunionDestination(
        route = "analysis/{conversationId}",
        title = "재회 계획",
    ) {
        fun createRoute(conversationId: Long): String = "analysis/$conversationId"
    }
}
