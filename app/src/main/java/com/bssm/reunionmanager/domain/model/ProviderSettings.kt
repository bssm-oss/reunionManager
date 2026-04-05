package com.bssm.reunionmanager.domain.model

data class ProviderSettings(
    val apiKey: String = "",
    val modelName: String = DEFAULT_MODEL,
    val endpoint: String = DEFAULT_ENDPOINT,
) {
    val isConfigured: Boolean = apiKey.isNotBlank()

    companion object {
        const val DEFAULT_MODEL: String = "gemini-2.0-flash"
        const val DEFAULT_ENDPOINT: String = "https://generativelanguage.googleapis.com/v1beta"
    }
}
