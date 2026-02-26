package com.fenn.callshield.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fenn.callshield.data.local.CallShieldDatabase
import com.fenn.callshield.data.local.dao.BlocklistDao
import com.fenn.callshield.data.local.dao.CallerEventDao
import com.fenn.callshield.data.local.dao.CallHistoryDao
import com.fenn.callshield.data.local.dao.DndCommandDao
import com.fenn.callshield.data.local.dao.PrefixRuleDao
import com.fenn.callshield.data.local.dao.ScamDigestDao
import com.fenn.callshield.data.local.dao.SeedDbDao
import com.fenn.callshield.data.local.dao.TraiReportDao
import com.fenn.callshield.data.local.dao.WhitelistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `trai_reports` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `numberHash` TEXT NOT NULL,
                `displayLabel` TEXT NOT NULL,
                `preparedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `dnd_commands` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `command` TEXT NOT NULL,
                `smsBody` TEXT NOT NULL,
                `categories` TEXT,
                `sentAt` INTEGER NOT NULL,
                `confirmedByUser` INTEGER NOT NULL DEFAULT 0,
                `confirmedAt` INTEGER
            )
            """.trimIndent()
        )
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Recreate prefix_rules with autoincrement id, matchType, and renamed prefix→pattern
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `prefix_rules_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `pattern` TEXT NOT NULL,
                `matchType` TEXT NOT NULL DEFAULT 'prefix',
                `action` TEXT NOT NULL,
                `label` TEXT NOT NULL DEFAULT '',
                `addedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `prefix_rules_new` (`pattern`, `matchType`, `action`, `label`, `addedAt`)
            SELECT `prefix`, 'prefix', `action`, `label`, `addedAt` FROM `prefix_rules`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `prefix_rules`")
        db.execSQL("ALTER TABLE `prefix_rules_new` RENAME TO `prefix_rules`")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recent_caller_events` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `numberHash` TEXT NOT NULL,
                `eventType` TEXT NOT NULL,
                `occurredAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_recent_caller_events_numberHash_occurredAt`
            ON `recent_caller_events` (`numberHash`, `occurredAt`)
            """.trimIndent()
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CallShieldDatabase =
        Room.databaseBuilder(context, CallShieldDatabase::class.java, "callshield.db")
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .fallbackToDestructiveMigrationFrom(1)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides fun provideBlocklistDao(db: CallShieldDatabase): BlocklistDao = db.blocklistDao()
    @Provides fun provideWhitelistDao(db: CallShieldDatabase): WhitelistDao = db.whitelistDao()
    @Provides fun providePrefixRuleDao(db: CallShieldDatabase): PrefixRuleDao = db.prefixRuleDao()
    @Provides fun provideCallHistoryDao(db: CallShieldDatabase): CallHistoryDao = db.callHistoryDao()
    @Provides fun provideSeedDbDao(db: CallShieldDatabase): SeedDbDao = db.seedDbDao()
    @Provides fun provideScamDigestDao(db: CallShieldDatabase): ScamDigestDao = db.scamDigestDao()
    @Provides fun provideCallerEventDao(db: CallShieldDatabase): CallerEventDao = db.callerEventDao()
    @Provides fun provideTraiReportDao(db: CallShieldDatabase): TraiReportDao = db.traiReportDao()
    @Provides fun provideDndCommandDao(db: CallShieldDatabase): DndCommandDao = db.dndCommandDao()
}
