package com.bssm.reunionmanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bssm.reunionmanager.data.local.dao.AnalysisResultDao
import com.bssm.reunionmanager.data.local.dao.ConversationDao
import com.bssm.reunionmanager.data.local.dao.MessageDao
import com.bssm.reunionmanager.data.local.dao.ParticipantDao
import com.bssm.reunionmanager.data.local.dao.ProviderSettingsDao
import com.bssm.reunionmanager.data.local.entity.AnalysisResultEntity
import com.bssm.reunionmanager.data.local.entity.ConversationEntity
import com.bssm.reunionmanager.data.local.entity.MessageEntity
import com.bssm.reunionmanager.data.local.entity.ParticipantEntity
import com.bssm.reunionmanager.data.local.entity.ProviderSettingsEntity

@Database(
    entities = [
        ConversationEntity::class,
        ParticipantEntity::class,
        MessageEntity::class,
        AnalysisResultEntity::class,
        ProviderSettingsEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class ReunionManagerDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun participantDao(): ParticipantDao
    abstract fun messageDao(): MessageDao
    abstract fun analysisResultDao(): AnalysisResultDao
    abstract fun providerSettingsDao(): ProviderSettingsDao

    companion object {
        fun build(context: Context): ReunionManagerDatabase {
            return Room.databaseBuilder(
                context,
                ReunionManagerDatabase::class.java,
                RoomConfig.DATABASE_NAME,
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE provider_settings ADD COLUMN modelPath TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE provider_settings ADD COLUMN backend TEXT NOT NULL DEFAULT 'CPU'")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE analysis_results ADD COLUMN messageDraft TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
