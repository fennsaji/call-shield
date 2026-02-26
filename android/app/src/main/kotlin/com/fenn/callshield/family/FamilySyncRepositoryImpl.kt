package com.fenn.callshield.family

import com.fenn.callshield.BuildConfig
import com.fenn.callshield.network.ApiClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FamilySyncRepositoryImpl @Inject constructor(
    private val apiClient: ApiClient,
) : FamilySyncRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val baseUrl = "${BuildConfig.SUPABASE_URL}/functions/v1"

    override suspend fun registerPairing(
        tokenHash: String,
        expiresAt: String,
        guardianDeviceHash: String,
        planType: String,
        subscriptionExpiresAt: String?,
    ): Result<Unit> = runCatching {
        val response = apiClient.http.post("$baseUrl/family-pair") {
            contentType(ContentType.Application.Json)
            setBody(
                PairRequest(
                    token_hash = tokenHash,
                    expires_at = expiresAt,
                    guardian_device_hash = guardianDeviceHash,
                    plan_type = planType,
                    subscription_expires_at = subscriptionExpiresAt,
                )
            )
        }
        check(response.status == HttpStatusCode.Created) {
            "registerPairing failed: ${response.status.value} ${response.bodyAsText()}"
        }
    }

    override suspend fun pushRules(
        tokenHash: String,
        ruleType: String,
        rulePayload: JsonObject,
    ): Result<Unit> = runCatching {
        val response = apiClient.http.post("$baseUrl/family-sync") {
            contentType(ContentType.Application.Json)
            setBody(PushRulesRequest(token_hash = tokenHash, rule_type = ruleType, rule_payload = rulePayload))
        }
        check(response.status == HttpStatusCode.OK) {
            "pushRules failed: ${response.status.value} ${response.bodyAsText()}"
        }
    }

    override suspend fun pullRules(tokenHash: String): Result<List<FamilySyncRule>> = runCatching {
        val response = apiClient.http.get("$baseUrl/family-sync") {
            url { parameters.append("token_hash", tokenHash) }
        }
        if (response.status == HttpStatusCode.PaymentRequired) {
            val body = runCatching { json.parseToJsonElement(response.bodyAsText()) }.getOrNull()
            val reason = (body as? kotlinx.serialization.json.JsonObject)
                ?.get("reason")?.jsonPrimitive?.content ?: "subscription_expired"
            throw SubscriptionExpiredException(reason)
        }
        check(response.status == HttpStatusCode.OK) {
            "pullRules failed: ${response.status.value} ${response.bodyAsText()}"
        }
        json.decodeFromString<PullRulesResponse>(response.bodyAsText()).rules
    }

    override suspend fun unpair(tokenHash: String): Result<Unit> = runCatching {
        val response = apiClient.http.delete("$baseUrl/family-unpair") {
            contentType(ContentType.Application.Json)
            setBody(UnpairRequest(token_hash = tokenHash))
        }
        check(response.status == HttpStatusCode.OK) {
            "unpair failed: ${response.status.value} ${response.bodyAsText()}"
        }
    }

    override suspend fun revokeSubscription(guardianDeviceHash: String): Result<Unit> = runCatching {
        val response = apiClient.http.post("$baseUrl/family-revoke") {
            contentType(ContentType.Application.Json)
            setBody(RevokeRequest(guardian_device_hash = guardianDeviceHash))
        }
        check(response.status == HttpStatusCode.OK) {
            "revokeSubscription failed: ${response.status.value} ${response.bodyAsText()}"
        }
    }

    override suspend fun renewSubscription(
        guardianDeviceHash: String,
        planType: String,
        subscriptionExpiresAt: String?,
    ): Result<Unit> = runCatching {
        val response = apiClient.http.post("$baseUrl/family-renew") {
            contentType(ContentType.Application.Json)
            setBody(
                RenewRequest(
                    guardian_device_hash = guardianDeviceHash,
                    plan_type = planType,
                    subscription_expires_at = subscriptionExpiresAt,
                )
            )
        }
        check(response.status == HttpStatusCode.OK) {
            "renewSubscription failed: ${response.status.value} ${response.bodyAsText()}"
        }
    }
}

@Serializable private data class PairRequest(
    val token_hash: String,
    val expires_at: String,
    val guardian_device_hash: String,
    val plan_type: String,
    val subscription_expires_at: String? = null,
)
@Serializable private data class PushRulesRequest(val token_hash: String, val rule_type: String, val rule_payload: JsonObject)
@Serializable private data class PullRulesResponse(val rules: List<FamilySyncRule>)
@Serializable private data class UnpairRequest(val token_hash: String)
@Serializable private data class RevokeRequest(val guardian_device_hash: String)
@Serializable private data class RenewRequest(
    val guardian_device_hash: String,
    val plan_type: String,
    val subscription_expires_at: String? = null,
)
