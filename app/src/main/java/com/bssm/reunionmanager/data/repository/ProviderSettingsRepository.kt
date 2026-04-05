package com.bssm.reunionmanager.data.repository

import com.bssm.reunionmanager.data.local.dao.ProviderSettingsDao
import com.bssm.reunionmanager.data.local.entity.ProviderSettingsEntity
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
                apiKey = settings.apiKey,
                modelName = settings.modelName,
                endpoint = settings.endpoint,
            ),
        )
    }

    private fun ProviderSettingsEntity.toDomainModel(): ProviderSettings {
        return ProviderSettings(
            apiKey = apiKey,
            modelName = modelName,
            endpoint = endpoint,
        )
    }
}
