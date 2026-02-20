package com.fenn.callguard.di

import android.content.Context
import androidx.room.Room
import com.fenn.callguard.data.local.CallGuardDatabase
import com.fenn.callguard.data.local.dao.BlocklistDao
import com.fenn.callguard.data.local.dao.CallHistoryDao
import com.fenn.callguard.data.local.dao.PrefixRuleDao
import com.fenn.callguard.data.local.dao.ScamDigestDao
import com.fenn.callguard.data.local.dao.SeedDbDao
import com.fenn.callguard.data.local.dao.WhitelistDao
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
    fun provideDatabase(@ApplicationContext context: Context): CallGuardDatabase =
        Room.databaseBuilder(context, CallGuardDatabase::class.java, "callguard.db")
            .build()

    @Provides fun provideBlocklistDao(db: CallGuardDatabase): BlocklistDao = db.blocklistDao()
    @Provides fun provideWhitelistDao(db: CallGuardDatabase): WhitelistDao = db.whitelistDao()
    @Provides fun providePrefixRuleDao(db: CallGuardDatabase): PrefixRuleDao = db.prefixRuleDao()
    @Provides fun provideCallHistoryDao(db: CallGuardDatabase): CallHistoryDao = db.callHistoryDao()
    @Provides fun provideSeedDbDao(db: CallGuardDatabase): SeedDbDao = db.seedDbDao()
    @Provides fun provideScamDigestDao(db: CallGuardDatabase): ScamDigestDao = db.scamDigestDao()
}
