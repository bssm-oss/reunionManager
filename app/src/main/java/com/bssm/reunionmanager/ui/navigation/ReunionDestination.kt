package com.bssm.reunionmanager.ui.navigation

sealed class ReunionDestination(
    val route: String,
    val title: String,
) {
    data object Home : ReunionDestination(route = "home", title = "오늘")
    data object Import : ReunionDestination(route = "import", title = "카톡 내용")
    data object Conversations : ReunionDestination(route = "conversations", title = "지난 기록")
    data object Settings : ReunionDestination(route = "settings", title = "내 정보")

    data object ConversationDetail : ReunionDestination(
        route = "conversation/{conversationId}",
        title = "플랜 기록",
    ) {
        fun createRoute(conversationId: Long): String = "conversation/$conversationId"
    }

    data object Analysis : ReunionDestination(
        route = "analysis/{conversationId}",
        title = "회복 맵",
    ) {
        fun createRoute(conversationId: Long): String = "analysis/$conversationId"
    }

    data object PlanCalendar : ReunionDestination(
        route = "calendar/{conversationId}",
        title = "이번 주",
    ) {
        fun createRoute(conversationId: Long): String = "calendar/$conversationId"
    }
}
