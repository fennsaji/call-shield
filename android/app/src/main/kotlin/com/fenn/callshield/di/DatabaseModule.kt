package com.fenn.callshield.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fenn.callshield.data.local.CallShieldDatabase
import com.fenn.callshield.data.local.dao.BlocklistDao
import com.fenn.callshield.data.local.dao.CallerEventDao
import com.fenn.callshield.data.local.dao.CallHistoryDao
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
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
            .build()

    @Provides fun provideBlocklistDao(db: CallShieldDatabase): BlocklistDao = db.blocklistDao()
    @Provides fun provideWhitelistDao(db: CallShieldDatabase): WhitelistDao = db.whitelistDao()
    @Provides fun providePrefixRuleDao(db: CallShieldDatabase): PrefixRuleDao = db.prefixRuleDao()
    @Provides fun provideCallHistoryDao(db: CallShieldDatabase): CallHistoryDao = db.callHistoryDao()
    @Provides fun provideSeedDbDao(db: CallShieldDatabase): SeedDbDao = db.seedDbDao()
    @Provides fun provideScamDigestDao(db: CallShieldDatabase): ScamDigestDao = db.scamDigestDao()
    @Provides fun provideCallerEventDao(db: CallShieldDatabase): CallerEventDao = db.callerEventDao()
    @Provides fun provideTraiReportDao(db: CallShieldDatabase): TraiReportDao = db.traiReportDao()
}
