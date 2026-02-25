package com.fenn.callshield.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fenn.callshield.data.local.dao.BlocklistDao
import com.fenn.callshield.data.local.dao.CallerEventDao
import com.fenn.callshield.data.local.dao.CallHistoryDao
import com.fenn.callshield.data.local.dao.DndCommandDao
import com.fenn.callshield.data.local.dao.TraiReportDao
import com.fenn.callshield.data.local.dao.PrefixRuleDao
import com.fenn.callshield.data.local.dao.ScamDigestDao
import com.fenn.callshield.data.local.dao.SeedDbDao
import com.fenn.callshield.data.local.dao.WhitelistDao
import com.fenn.callshield.data.local.entity.BlocklistEntry
import com.fenn.callshield.data.local.entity.CallerEventEntry
import com.fenn.callshield.data.local.entity.CallHistoryEntry
import com.fenn.callshield.data.local.entity.DndCommandEntry
import com.fenn.callshield.data.local.entity.TraiReportEntry
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
        CallerEventEntry::class,
        TraiReportEntry::class,
        DndCommandEntry::class,
    ],
    version = 6,
    exportSchema = true,
)
abstract class CallShieldDatabase : RoomDatabase() {
    abstract fun blocklistDao(): BlocklistDao
    abstract fun whitelistDao(): WhitelistDao
    abstract fun prefixRuleDao(): PrefixRuleDao
    abstract fun callHistoryDao(): CallHistoryDao
    abstract fun seedDbDao(): SeedDbDao
    abstract fun scamDigestDao(): ScamDigestDao
    abstract fun callerEventDao(): CallerEventDao
    abstract fun traiReportDao(): TraiReportDao
    abstract fun dndCommandDao(): DndCommandDao
}
