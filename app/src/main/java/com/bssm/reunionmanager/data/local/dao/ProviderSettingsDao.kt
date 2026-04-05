package com.bssm.reunionmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.bssm.reunionmanager.data.local.entity.ProviderSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderSettingsDao {
    @Upsert
    suspend fun upsert(entity: ProviderSettingsEntity)

    @Query("SELECT * FROM provider_settings WHERE id = 0")
    fun observe(): Flow<ProviderSettingsEntity?>

    @Query("SELECT * FROM provider_settings WHERE id = 0")
    suspend fun get(): ProviderSettingsEntity?
}
