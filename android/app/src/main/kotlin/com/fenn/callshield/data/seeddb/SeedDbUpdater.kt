package com.fenn.callshield.data.seeddb

import com.fenn.callshield.data.local.dao.SeedDbDao
import com.fenn.callshield.data.local.entity.SeedDbMeta
import com.fenn.callshield.data.local.entity.SeedDbNumber
import com.fenn.callshield.network.ApiClient
import com.fenn.callshield.util.DeviceTokenManager
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readUTF8Line
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads and applies seed DB updates.
 *
 * Flow:
 *   1. Fetch manifest → get current version + sha256 + signed download URL
 *   2. Compare against local [SeedDbMeta.version]; skip if up to date
 *   3. Stream-download the CSV, verifying SHA-256 as we go
 *   4. Bulk-insert into [seed_db_numbers] in batches of 1000
 *   5. Update [SeedDbMeta]
 *
 * CSV format: number_hash,category,confidence_score  (no header row)
 */
@Singleton
class SeedDbUpdater @Inject constructor(
    private val apiClient: ApiClient,
    private val seedDbDao: SeedDbDao,
    private val deviceTokenManager: DeviceTokenManager,
) {

    sealed class UpdateResult {
        data object AlreadyUpToDate : UpdateResult()
        data class Updated(val version: Int, val rowsInserted: Int) : UpdateResult()
        data class Failed(val reason: String) : UpdateResult()
    }

    suspend fun checkAndUpdate(): UpdateResult {
        return try {
            val manifest = apiClient.getSeedDbManifest(deviceTokenManager.deviceTokenHash)
            val localMeta = seedDbDao.getMeta()

            if (localMeta != null && localMeta.version >= manifest.version) {
                return UpdateResult.AlreadyUpToDate
            }

            val rowsInserted = downloadAndApply(manifest)
            UpdateResult.Updated(manifest.version, rowsInserted)
        } catch (e: Exception) {
            UpdateResult.Failed(e.message ?: "Unknown error")
        }
    }

    private suspend fun downloadAndApply(
        manifest: com.fenn.callshield.network.SeedDbManifestResponse,
    ): Int {
        val digest = MessageDigest.getInstance("SHA-256")
        // Accumulate all rows in memory — nothing is written to the DB until the
        // checksum is verified, so a mismatch never corrupts previously valid data.
        val allRows = mutableListOf<SeedDbNumber>()

        val response = apiClient.http.get(manifest.download_url)
        val channel = response.bodyAsChannel()

        // Stream-parse CSV line by line
        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: break
            digest.update(line.toByteArray(Charsets.UTF_8))
            digest.update('\n'.code.toByte())

            val parts = line.split(',')
            if (parts.size < 3) continue

            val (hash, category, scoreStr) = parts
            val score = scoreStr.trim().toDoubleOrNull() ?: continue

            allRows.add(SeedDbNumber(numberHash = hash.trim(), category = category.trim(), confidenceScore = score))
        }

        // Verify checksum before touching the DB
        val actualChecksum = digest.digest().joinToString("") { "%02x".format(it) }
        if (actualChecksum != manifest.sha256) {
            throw SecurityException(
                "Seed DB checksum mismatch: expected ${manifest.sha256}, got $actualChecksum"
            )
        }

        // Atomically swap old data for new in a single transaction
        seedDbDao.replaceAll(allRows)
        seedDbDao.upsertMeta(SeedDbMeta(version = manifest.version, sha256Checksum = manifest.sha256))
        return allRows.size
    }

    companion object {
        private const val BATCH_SIZE = 1000
    }
}
