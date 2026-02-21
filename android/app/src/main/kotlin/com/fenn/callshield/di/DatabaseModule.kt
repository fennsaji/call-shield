package com.fenn.callshield.di

import android.content.Context
import androidx.room.Room
import com.fenn.callshield.data.local.CallShieldDatabase
import com.fenn.callshield.data.local.dao.BlocklistDao
import com.fenn.callshield.data.local.dao.CallHistoryDao
import com.fenn.callshield.data.local.dao.PrefixRuleDao
import com.fenn.callshield.data.local.dao.ScamDigestDao
import com.fenn.callshield.data.local.dao.SeedDbDao
import com.fenn.callshield.data.local.dao.WhitelistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CallShieldDatabase =
        Room.databaseBuilder(context, CallShieldDatabase::class.java, "callshield.db")
            .build()

    @Provides fun provideBlocklistDao(db: CallShieldDatabase): BlocklistDao = db.blocklistDao()
    @Provides fun provideWhitelistDao(db: CallShieldDatabase): WhitelistDao = db.whitelistDao()
    @Provides fun providePrefixRuleDao(db: CallShieldDatabase): PrefixRuleDao = db.prefixRuleDao()
    @Provides fun provideCallHistoryDao(db: CallShieldDatabase): CallHistoryDao = db.callHistoryDao()
    @Provides fun provideSeedDbDao(db: CallShieldDatabase): SeedDbDao = db.seedDbDao()
    @Provides fun provideScamDigestDao(db: CallShieldDatabase): ScamDigestDao = db.scamDigestDao()
}
