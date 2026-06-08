package com.bssm.reunionmanager.data.analysis

import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalModelFileStoreTest {
    @Test
    fun modelFile_sanitizesSelectedFileName() {
        val filesDir = File("/tmp/reunion")

        val file = LocalModelFileStore.modelFile(filesDir, "../gemma 4.litertlm")

        assertEquals(File(filesDir, "models/.._gemma_4.litertlm"), file)
    }

    @Test
    fun deleteCopiedModelFile_removesOnlyAppPrivateModelFile() {
        val filesDir = createTempDirectory("reunion-files").toFile()
        val modelFile = LocalModelFileStore.modelFile(filesDir, "gemma-4.litertlm")
        modelFile.parentFile?.mkdirs()
        modelFile.writeText("fake model")

        val deleted = LocalModelFileStore.deleteCopiedModelFile(filesDir, modelFile.absolutePath)

        assertTrue(deleted)
        assertFalse(modelFile.exists())
    }

    @Test
    fun deleteCopiedModelFile_doesNotRemoveExternalFile() {
        val filesDir = createTempDirectory("reunion-files").toFile()
        val externalFile = createTempDirectory("external-model").resolve("gemma-4.litertlm").toFile()
        externalFile.writeText("external model")

        val deleted = LocalModelFileStore.deleteCopiedModelFile(filesDir, externalFile.absolutePath)

        assertFalse(deleted)
        assertTrue(externalFile.exists())
    }

    @Test
    fun deleteCopiedModelFile_doesNotRemoveModelsDirectory() {
        val filesDir = createTempDirectory("reunion-files").toFile()
        val modelsDir = LocalModelFileStore.modelsDir(filesDir).apply { mkdirs() }

        val deleted = LocalModelFileStore.deleteCopiedModelFile(filesDir, modelsDir.absolutePath)

        assertFalse(deleted)
        assertTrue(modelsDir.isDirectory)
    }

    @Test
    fun deleteCopiedModelFile_ignoresBlankOrMissingPath() {
        val filesDir = createTempDirectory("reunion-files").toFile()

        assertFalse(LocalModelFileStore.deleteCopiedModelFile(filesDir, ""))
        assertFalse(LocalModelFileStore.deleteCopiedModelFile(filesDir, File(filesDir, "models/missing.litertlm").absolutePath))
    }
}
