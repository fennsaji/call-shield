package com.fenn.callshield.data.seeddb

import androidx.room.withTransaction
import com.fenn.callshield.data.local.CallShieldDatabase
import com.fenn.callshield.data.local.dao.SeedDbDao
import com.fenn.callshield.data.local.entity.SeedDbMeta
import com.fenn.callshield.data.local.entity.SeedDbNumber
import com.fenn.callshield.network.ApiClient
import com.fenn.callshield.util.DeviceTokenManager
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readUTF8Line
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads and applies seed DB updates.
 *
 * Flow:
 *   1. Fetch manifest → get current version + sha256 + signed download URL
 *   2. Compare against local [SeedDbMeta.version]; skip if up to date
 *   3. Stream-download the CSV to a temp file, computing SHA-256 as we stream
 *   4. Verify checksum against the manifest before touching the DB
 *   5. Re-read the verified temp file and batch-insert [BATCH_SIZE] rows at a time
 *      within a single DB transaction — never holds all 500k rows in heap
 *   6. Update [SeedDbMeta]
 *
 * CSV format: number_hash,category,confidence_score  (no header row)
 */
@Singleton
class SeedDbUpdater @Inject constructor(
    private val apiClient: ApiClient,
    private val database: CallShieldDatabase,
    private val deviceTokenManager: DeviceTokenManager,
) {
    private val seedDbDao: SeedDbDao get() = database.seedDbDao()

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
        val tempFile = File.createTempFile("seed_db_", ".csv")
        try {
            // Phase 1: Stream download to temp file, computing checksum on the fly
            val digest = MessageDigest.getInstance("SHA-256")
            val response = apiClient.http.get(manifest.download_url)
            val channel = response.bodyAsChannel()

            tempFile.outputStream().bufferedWriter().use { writer ->
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    digest.update(line.toByteArray(Charsets.UTF_8))
                    digest.update('\n'.code.toByte())
                    writer.write(line)
                    writer.newLine()
                }
            }

            // Phase 2: Verify checksum before touching the DB
            val actualChecksum = digest.digest().joinToString("") { "%02x".format(it) }
            if (actualChecksum != manifest.sha256) {
                throw SecurityException(
                    "Seed DB checksum mismatch: expected ${manifest.sha256}, got $actualChecksum"
                )
            }

            // Phase 3: Stream-insert from the verified temp file in BATCH_SIZE chunks.
            // The entire clear + insert runs in a single transaction so a crash mid-insert
            // leaves the old data intact. Use a while loop (not forEachLine) so suspend
            // DAO calls are valid inside the withTransaction suspend body.
            val rowsInserted = database.withTransaction {
                seedDbDao.clearAll()
                val batch = mutableListOf<SeedDbNumber>()
                var count = 0
                tempFile.bufferedReader().use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        val parts = line.split(',')
                        if (parts.size >= 3) {
                            val score = parts[2].trim().toDoubleOrNull()
                            if (score != null) {
                                batch.add(
                                    SeedDbNumber(
                                        numberHash = parts[0].trim(),
                                        category = parts[1].trim(),
                                        confidenceScore = score,
                                    )
                                )
                                if (batch.size >= BATCH_SIZE) {
                                    seedDbDao.insertAll(batch.toList())
                                    count += batch.size
                                    batch.clear()
                                }
                            }
                        }
                        line = reader.readLine()
                    }
                }
                if (batch.isNotEmpty()) {
                    seedDbDao.insertAll(batch.toList())
                    count += batch.size
                }
                seedDbDao.upsertMeta(SeedDbMeta(version = manifest.version, sha256Checksum = manifest.sha256))
                count
            }

            return rowsInserted
        } finally {
            tempFile.delete()
        }
    }

    companion object {
        private const val BATCH_SIZE = 1000
    }
}
