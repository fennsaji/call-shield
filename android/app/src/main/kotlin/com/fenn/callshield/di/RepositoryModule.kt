package com.fenn.callshield.di

import com.fenn.callshield.data.repository.BlocklistRepositoryImpl
import com.fenn.callshield.data.repository.CallHistoryRepositoryImpl
import com.fenn.callshield.data.repository.PrefixRuleRepositoryImpl
import com.fenn.callshield.data.repository.ReputationRepositoryImpl
import com.fenn.callshield.data.repository.WhitelistRepositoryImpl
import com.fenn.callshield.domain.repository.BlocklistRepository
import com.fenn.callshield.domain.repository.CallHistoryRepository
import com.fenn.callshield.domain.repository.PrefixRuleRepository
import com.fenn.callshield.domain.repository.ReputationRepository
import com.fenn.callshield.domain.repository.WhitelistRepository
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
