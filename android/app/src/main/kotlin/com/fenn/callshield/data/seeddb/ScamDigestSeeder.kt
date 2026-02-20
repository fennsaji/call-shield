package com.fenn.callshield.data.seeddb

import android.content.Context
import com.fenn.callshield.data.local.dao.ScamDigestDao
import com.fenn.callshield.data.local.entity.ScamDigestEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScamDigestSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ScamDigestDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Seeds scam_digest table from assets/scam_digest.json if empty. */
    suspend fun seedIfNeeded() {
        if (dao.count() > 0) return
        try {
            val raw = context.assets.open("scam_digest.json").bufferedReader().readText()
            val items = json.decodeFromString<List<ScamDigestJson>>(raw)
            dao.insertAll(items.map {
                ScamDigestEntry(
                    id = it.id,
                    title = it.title,
                    body = it.body,
                    source = it.source,
                    publishedAt = it.publishedAt,
                )
            })
        } catch (_: Exception) {
            // Asset missing or parse failure — non-fatal
        }
    }

    @Serializable
    private data class ScamDigestJson(
        val id: Int,
        val title: String,
        val body: String,
        val source: String,
        val publishedAt: Long,
    )
}
