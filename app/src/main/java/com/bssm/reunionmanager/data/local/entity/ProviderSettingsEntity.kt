package com.bssm.reunionmanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "provider_settings")
data class ProviderSettingsEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val apiKey: String,
    val modelName: String,
    val endpoint: String,
) {
    companion object {
        const val SINGLE_ROW_ID: Int = 0
    }
}
