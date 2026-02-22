package com.fenn.callshield.family

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Network layer for family sync operations against the Supabase Edge Functions:
 *   POST /family-pair     → [registerPairing]
 *   POST /family-sync     → [pushRules]
 *   GET  /family-sync     → [pullRules]
 *   DELETE /family-unpair → [unpair]
 */
interface FamilySyncRepository {

    /** Guardian: registers a new pairing token hash with the backend. */
    suspend fun registerPairing(tokenHash: String, expiresAt: String): Result<Unit>

    /** Guardian: pushes updated rules to the backend for the dependent to fetch. */
    suspend fun pushRules(tokenHash: String, ruleType: String, rulePayload: JsonObject): Result<Unit>

    /** Dependent: fetches the latest rules pushed by the guardian. */
    suspend fun pullRules(tokenHash: String): Result<List<FamilySyncRule>>

    /** Either device: deletes all sync data for this pairing token. */
    suspend fun unpair(tokenHash: String): Result<Unit>
}

@Serializable
data class FamilySyncRule(
    val rule_type: String,
    val rule_payload: JsonObject,
    val updated_at: String,
)
