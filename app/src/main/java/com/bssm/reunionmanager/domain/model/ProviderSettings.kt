package com.bssm.reunionmanager.domain.model

data class ProviderSettings(
    val modelPath: String = "",
    val modelName: String = DEFAULT_MODEL,
    val backend: GemmaBackend = GemmaBackend.CPU,
) {
    val isConfigured: Boolean = modelPath.isNotBlank()

    companion object {
        const val DEFAULT_MODEL: String = "gemma-4-E4B-it.litertlm"
    }
}

enum class GemmaBackend {
    CPU,
    GPU,
    ;

    companion object {
        fun fromStoredValue(value: String): GemmaBackend {
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: CPU
        }
    }
}
