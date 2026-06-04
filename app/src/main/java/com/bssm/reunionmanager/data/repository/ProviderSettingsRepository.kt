package com.bssm.reunionmanager.data.repository

import com.bssm.reunionmanager.data.local.dao.ProviderSettingsDao
import com.bssm.reunionmanager.data.local.entity.ProviderSettingsEntity
import com.bssm.reunionmanager.domain.model.GemmaBackend
import com.bssm.reunionmanager.domain.model.ProviderSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProviderSettingsRepository(
    private val providerSettingsDao: ProviderSettingsDao,
) {
    fun observe(): Flow<ProviderSettings> {
        return providerSettingsDao.observe().map { entity ->
            entity?.toDomainModel() ?: ProviderSettings()
        }
    }

    suspend fun get(): ProviderSettings {
        return providerSettingsDao.get()?.toDomainModel() ?: ProviderSettings()
    }

    suspend fun save(settings: ProviderSettings) {
        providerSettingsDao.upsert(
            ProviderSettingsEntity(
                apiKey = "",
                modelName = settings.modelName,
                endpoint = "",
                modelPath = settings.modelPath,
                backend = settings.backend.name,
                userDisplayName = settings.userDisplayName,
            ),
        )
    }

    private fun ProviderSettingsEntity.toDomainModel(): ProviderSettings {
        return ProviderSettings(
            modelPath = modelPath,
            modelName = modelName,
            backend = GemmaBackend.fromStoredValue(backend),
            userDisplayName = userDisplayName,
        )
    }
}
