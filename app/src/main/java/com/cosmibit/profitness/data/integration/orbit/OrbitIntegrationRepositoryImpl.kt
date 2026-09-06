package com.cosmibit.profitness.data.integration.orbit

import com.cosmibit.profitness.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrbitIntegrationRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : OrbitIntegrationRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = 12_000
            connectTimeoutMillis = 8_000
            socketTimeoutMillis = 12_000
        }
        install(ContentNegotiation) { json(json) }
    }
    private val mutableState = MutableStateFlow(OrbitIntegrationState())
    override val state: StateFlow<OrbitIntegrationState> = mutableState.asStateFlow()

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).run {
            launch { refresh() }
        }
    }

    override suspend fun refresh(): Result<OrbitIntegrationState> = withContext(Dispatchers.IO) {
        mutableState.value = mutableState.value.copy(isLoading = true)
        runCatching { post(OrbitRequest(action = "status")).toDomain() }
            .onSuccess { mutableState.value = it }
            .onFailure {
                mutableState.value = mutableState.value.copy(
                    status = if (mutableState.value.isConnected) {
                        OrbitConnectionStatus.TEMPORARILY_UNAVAILABLE
                    } else OrbitConnectionStatus.NOT_CONNECTED,
                    isLoading = false,
                    isInitialized = true,
                    lastErrorCode = (it as? OrbitApiException)?.code ?: "temporarily_unavailable"
                )
            }
    }

    override suspend fun beginConnection(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val response = post(OrbitRequest(action = "connect", idempotencyKey = UUID.randomUUID().toString()))
            response.authorizationUrl?.takeIf(String::isNotBlank)
                ?: throw OrbitApiException("authorization_url_missing", "Orbit bağlantısı başlatılamadı.")
        }.onFailure { mutableState.value = mutableState.value.copy(lastErrorCode = (it as? OrbitApiException)?.code ?: "connect_failed") }
    }

    override suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            post(OrbitRequest(action = "disconnect", idempotencyKey = UUID.randomUUID().toString()))
            mutableState.value = OrbitIntegrationState(isInitialized = true)
        }.onFailure { mutableState.value = mutableState.value.copy(lastErrorCode = (it as? OrbitApiException)?.code ?: "disconnect_failed") }
    }

    override suspend fun setSyncEnabled(enabled: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val state = post(OrbitRequest(action = "set_sync_enabled", enabled = enabled)).toDomain()
            mutableState.value = state
        }
    }

    override suspend fun requestSync(timeZone: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!mutableState.value.canSync) return@withContext Result.success(Unit)
        runCatching {
            val response = post(
                OrbitRequest(
                    action = "sync",
                    timeZone = timeZone,
                    idempotencyKey = UUID.randomUUID().toString()
                )
            )
            mutableState.value = response.toDomain()
        }.onFailure { mutableState.value = mutableState.value.copy(lastErrorCode = (it as? OrbitApiException)?.code ?: "sync_failed") }
    }

    private suspend fun post(request: OrbitRequest): OrbitResponse {
        supabase.auth.awaitInitialization()
        val token = supabase.auth.currentSessionOrNull()?.accessToken
            ?: throw OrbitApiException("unauthorized", "Orbit için giriş yapmanız gerekiyor.")
        val response = client.post(
            "${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/orbit-fitness-integration"
        ) {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!response.status.isSuccess()) {
            val error = runCatching { response.body<OrbitError>() }.getOrNull()
            throw OrbitApiException(
                error?.code ?: "temporarily_unavailable",
                error?.message ?: "Orbit şu anda yanıt vermiyor."
            )
        }
        return response.body()
    }
}

internal class OrbitApiException(val code: String, override val message: String) : Exception(message)

@Serializable
private data class OrbitRequest(
    val action: String,
    val enabled: Boolean? = null,
    val timeZone: String? = null,
    val idempotencyKey: String? = null
)

@Serializable
private data class OrbitError(val code: String? = null, val message: String? = null)

@Serializable
private data class OrbitResponse(
    val status: String = "not_connected",
    val accountLabel: String? = null,
    val syncEnabled: Boolean = false,
    val fitnessSyncEntitled: Boolean = false,
    val lastSyncedAt: String? = null,
    val lastErrorCode: String? = null,
    val authorizationUrl: String? = null,
    val manageUrl: String? = null
) {
    fun toDomain() = OrbitIntegrationPolicy.sanitize(OrbitIntegrationState(
        status = OrbitIntegrationPolicy.status(status),
        accountLabel = accountLabel,
        syncEnabled = syncEnabled,
        fitnessSyncEntitled = fitnessSyncEntitled,
        lastSyncedAt = lastSyncedAt,
        lastErrorCode = lastErrorCode,
        manageUrl = manageUrl
    ))
}
