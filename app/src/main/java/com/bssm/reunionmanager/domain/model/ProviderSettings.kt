package com.bssm.reunionmanager.domain.model

data class ProviderSettings(
    val modelPath: String = "",
    val modelName: String = DEFAULT_MODEL,
    val backend: GemmaBackend = GemmaBackend.CPU,
    val userDisplayName: String = "",
    val verifiedModelPath: String = "",
    val verifiedBackend: GemmaBackend = GemmaBackend.CPU,
    val verifiedAtEpochMillis: Long? = null,
) {
    val isConfigured: Boolean = modelPath.isNotBlank()
    val isModelVerified: Boolean =
        isConfigured &&
            verifiedAtEpochMillis != null &&
            verifiedModelPath == modelPath &&
            verifiedBackend == backend

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
