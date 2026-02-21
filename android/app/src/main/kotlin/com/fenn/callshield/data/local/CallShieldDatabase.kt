package com.fenn.callshield.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fenn.callshield.data.local.dao.BlocklistDao
import com.fenn.callshield.data.local.dao.CallHistoryDao
import com.fenn.callshield.data.local.dao.PrefixRuleDao
import com.fenn.callshield.data.local.dao.ScamDigestDao
import com.fenn.callshield.data.local.dao.SeedDbDao
import com.fenn.callshield.data.local.dao.WhitelistDao
import com.fenn.callshield.data.local.entity.BlocklistEntry
import com.fenn.callshield.data.local.entity.CallHistoryEntry
import com.fenn.callshield.data.local.entity.PrefixRule
import com.fenn.callshield.data.local.entity.ScamDigestEntry
import com.fenn.callshield.data.local.entity.SeedDbMeta
import com.fenn.callshield.data.local.entity.SeedDbNumber
import com.fenn.callshield.data.local.entity.WhitelistEntry

@Database(
    entities = [
        BlocklistEntry::class,
        WhitelistEntry::class,
        PrefixRule::class,
        CallHistoryEntry::class,
        SeedDbMeta::class,
        SeedDbNumber::class,
        ScamDigestEntry::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class CallShieldDatabase : RoomDatabase() {
    abstract fun blocklistDao(): BlocklistDao
    abstract fun whitelistDao(): WhitelistDao
    abstract fun prefixRuleDao(): PrefixRuleDao
    abstract fun callHistoryDao(): CallHistoryDao
    abstract fun seedDbDao(): SeedDbDao
    abstract fun scamDigestDao(): ScamDigestDao
}
