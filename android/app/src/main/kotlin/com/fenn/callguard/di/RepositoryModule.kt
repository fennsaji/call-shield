package com.fenn.callguard.di

import com.fenn.callguard.data.repository.BlocklistRepositoryImpl
import com.fenn.callguard.data.repository.CallHistoryRepositoryImpl
import com.fenn.callguard.data.repository.PrefixRuleRepositoryImpl
import com.fenn.callguard.data.repository.ReputationRepositoryImpl
import com.fenn.callguard.data.repository.WhitelistRepositoryImpl
import com.fenn.callguard.domain.repository.BlocklistRepository
import com.fenn.callguard.domain.repository.CallHistoryRepository
import com.fenn.callguard.domain.repository.PrefixRuleRepository
import com.fenn.callguard.domain.repository.ReputationRepository
import com.fenn.callguard.domain.repository.WhitelistRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindBlocklist(impl: BlocklistRepositoryImpl): BlocklistRepository
    @Binds @Singleton abstract fun bindWhitelist(impl: WhitelistRepositoryImpl): WhitelistRepository
    @Binds @Singleton abstract fun bindPrefixRule(impl: PrefixRuleRepositoryImpl): PrefixRuleRepository
    @Binds @Singleton abstract fun bindReputation(impl: ReputationRepositoryImpl): ReputationRepository
    @Binds @Singleton abstract fun bindCallHistory(impl: CallHistoryRepositoryImpl): CallHistoryRepository
}
