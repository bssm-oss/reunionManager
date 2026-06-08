package com.bssm.reunionmanager.ui.screen.common

internal fun needsPerspectiveSetup(
    participantNames: List<String>,
    userDisplayName: String,
): Boolean {
    val normalizedName = userDisplayName.trim()
    return participantNames.size >= 2 && (normalizedName.isBlank() || normalizedName !in participantNames)
}

internal fun perspectiveNameOptions(participantNames: List<String>): List<String> {
    val names = participantNames
        .map { name -> name.trim() }
        .filter { name -> name.isNotBlank() }
        .distinct()
    return names.takeIf { it.size in 2..4 }.orEmpty()
}

internal fun perspectiveSetupSupportingText(
    participantNames: List<String>,
    userDisplayName: String,
): String {
    val normalizedName = userDisplayName.trim()
    return when {
        normalizedName.isBlank() -> "내 카톡 이름을 저장한 뒤 분석하세요."
        participantNames.size >= 2 && normalizedName !in participantNames -> {
            "저장한 이름이 이 대화에 없어요. 카카오톡에 보이는 이름으로 고친 뒤 분석하세요."
        }
        else -> "내 카톡 이름을 저장한 뒤 분석하세요."
    }
}

internal fun perspectiveNameButtonText(name: String): String {
    return "${name.trim()} 선택"
}
