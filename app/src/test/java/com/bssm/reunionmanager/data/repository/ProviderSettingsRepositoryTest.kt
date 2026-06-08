package com.bssm.reunionmanager.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.bssm.reunionmanager.data.local.ReunionManagerDatabase
import com.bssm.reunionmanager.domain.model.GemmaBackend
import com.bssm.reunionmanager.domain.model.ProviderSettings
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProviderSettingsRepositoryTest {
    private lateinit var database: ReunionManagerDatabase
    private lateinit var repository: ProviderSettingsRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ReunionManagerDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = ProviderSettingsRepository(database.providerSettingsDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun save_preservesModelVerificationStatus() = runTest {
        repository.save(
            ProviderSettings(
                modelPath = "/models/gemma-4-E4B-it.litertlm",
                modelName = "gemma-4-E4B-it.litertlm",
                backend = GemmaBackend.GPU,
                userDisplayName = "현우",
                verifiedModelPath = "/models/gemma-4-E4B-it.litertlm",
                verifiedBackend = GemmaBackend.GPU,
                verifiedAtEpochMillis = 42L,
            ),
        )

        val settings = repository.get()

        assertEquals("/models/gemma-4-E4B-it.litertlm", settings.modelPath)
        assertEquals(GemmaBackend.GPU, settings.backend)
        assertEquals("현우", settings.userDisplayName)
        assertEquals("/models/gemma-4-E4B-it.litertlm", settings.verifiedModelPath)
        assertEquals(GemmaBackend.GPU, settings.verifiedBackend)
        assertEquals(42L, settings.verifiedAtEpochMillis)
        assertTrue(settings.isModelVerified)
    }
}
