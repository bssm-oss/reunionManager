package com.bssm.reunionmanager.data.analysis

import java.io.File

object LocalModelFileStore {
    fun modelsDir(filesDir: File): File {
        return File(filesDir, "models")
    }

    fun modelFile(filesDir: File, sourceName: String): File {
        return File(modelsDir(filesDir), sourceName.toSafeModelFileName())
    }

    fun tempModelFile(filesDir: File, sourceName: String): File {
        return File(modelsDir(filesDir), "${sourceName.toSafeModelFileName()}.tmp")
    }

    fun deleteCopiedModelFile(filesDir: File, modelPath: String): Boolean {
        val file = copiedModelFileOrNull(filesDir = filesDir, modelPath = modelPath) ?: return false
        return file.isFile && file.delete()
    }

    private fun copiedModelFileOrNull(filesDir: File, modelPath: String): File? {
        if (modelPath.isBlank()) {
            return null
        }
        return runCatching {
            val modelsDir = modelsDir(filesDir).canonicalFile
            val modelFile = File(modelPath).canonicalFile
            if (modelFile.isInside(modelsDir)) modelFile else null
        }.getOrNull()
    }

    private fun File.isInside(parent: File): Boolean {
        val childPath = path
        val parentPath = parent.path
        return childPath.startsWith(parentPath + File.separator)
    }

    private fun String.toSafeModelFileName(): String {
        return replace(Regex("[^A-Za-z0-9._-]"), "_")
    }
}
