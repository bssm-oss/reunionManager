package com.bssm.reunionmanager.domain.model

object GemmaModelFileRules {
    private const val MIN_MODEL_BYTES = 16L * 1024L * 1024L

    fun requireSupportedFileName(fileName: String) {
        require(fileName.trim().endsWith(".litertlm", ignoreCase = true)) {
            "Gemma 4 .litertlm 모델 파일을 선택하세요."
        }
    }

    fun requirePlausibleModelSize(sizeBytes: Long) {
        require(sizeBytes >= MIN_MODEL_BYTES) {
            "모델 파일이 너무 작습니다. 원본 .litertlm 파일을 선택하세요."
        }
    }
}
