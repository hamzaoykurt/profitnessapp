package com.avonix.profitness.data.challenges

import com.avonix.profitness.data.challenges.dto.ChallengeDetailDto
import com.avonix.profitness.data.challenges.dto.MyChallengeRowDto
import com.avonix.profitness.data.challenges.dto.PublicChallengeRowDto
import com.avonix.profitness.domain.challenges.ChallengeDetail
import com.avonix.profitness.domain.challenges.ChallengeSummary
import com.avonix.profitness.domain.challenges.ChallengeTargetType
import com.avonix.profitness.domain.challenges.ChallengeVisibility
import com.avonix.profitness.domain.challenges.CreateEventChallengeRequest
import com.avonix.profitness.domain.challenges.toDomain
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class ChallengeRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : ChallengeRepository {

    override suspend fun listPublicChallenges(limit: Int, offset: Int): Result<List<ChallengeSummary>> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest.rpc(
                    "list_public_challenges",
                    buildJsonObject {
                        put("p_limit", limit)
                        put("p_offset", offset)
                    }
                ).decodeList<PublicChallengeRowDto>().map { it.toDomain() }
            }
        }

    override suspend fun listMyChallenges(): Result<List<ChallengeSummary>> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest.rpc("list_my_challenges")
                    .decodeList<MyChallengeRowDto>()
                    .map { it.toDomain() }
            }
        }

    override suspend fun getChallengeDetail(challengeId: String): Result<ChallengeDetail> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest.rpc(
                    "get_challenge_detail",
                    buildJsonObject { put("p_challenge_id", challengeId) }
                ).decodeAs<ChallengeDetailDto>().toDomain()
            }
        }

    override suspend fun createChallenge(
        title: String,
        description: String,
        targetType: ChallengeTargetType,
        targetValue: Long,
        startDateIso: String,
        endDateIso: String,
        visibility: ChallengeVisibility,
        password: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            supabase.postgrest.rpc(
                "create_challenge",
                buildJsonObject {
                    put("p_title",        title.trim())
                    put("p_description",  description.trim())
                    put("p_target_type",  targetType.raw)
                    put("p_target_value", targetValue)
                    put("p_start_date",   startDateIso)
                    put("p_end_date",     endDateIso)
                    put("p_visibility",   visibility.raw)
                    val pw = password?.trim().orEmpty()
                    if (pw.isNotEmpty()) put("p_password", pw)
                    else put("p_password", JsonNull)
                }
            ).decodeAs<String>()
        }
    }

    override suspend fun createEventChallenge(req: CreateEventChallengeRequest): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val movementsJson = buildJsonArray {
                    req.movements.forEach { m ->
                        addJsonObject {
                            put("exercise_id", m.exerciseId)
                            put("sort_index", m.sortIndex)
                            if (m.suggestedSets != null) put("suggested_sets", m.suggestedSets)
                            else put("suggested_sets", JsonNull)
                            if (m.suggestedReps != null) put("suggested_reps", m.suggestedReps)
                            else put("suggested_reps", JsonNull)
                            if (m.suggestedDurSec != null) put("suggested_dur_s", m.suggestedDurSec)
                            else put("suggested_dur_s", JsonNull)
                        }
                    }
                }
                supabase.postgrest.rpc(
                    "create_event_challenge",
                    buildJsonObject {
                        put("p_title",         req.title.trim())
                        val desc = req.description?.trim().orEmpty()
                        if (desc.isNotEmpty()) put("p_description", desc) else put("p_description", JsonNull)
                        put("p_event_mode",    req.mode.raw)
                        put("p_event_date",    req.dateIso)
                        if (req.timeIso != null) put("p_event_time", req.timeIso) else put("p_event_time", JsonNull)
                        put("p_event_timezone", req.timezone)
                        if (req.location != null) put("p_event_location", req.location) else put("p_event_location", JsonNull)
                        if (req.geoLat != null) put("p_geo_lat", req.geoLat) else put("p_geo_lat", JsonNull)
                        if (req.geoLng != null) put("p_geo_lng", req.geoLng) else put("p_geo_lng", JsonNull)
                        if (req.onlineUrl != null) put("p_online_url", req.onlineUrl) else put("p_online_url", JsonNull)
                        if (req.targetType != null) put("p_target_type", req.targetType.raw) else put("p_target_type", JsonNull)
                        if (req.targetValue != null) put("p_target_value", req.targetValue) else put("p_target_value", JsonNull)
                        put("p_visibility", req.visibility.raw)
                        val pw = req.password?.trim().orEmpty()
                        if (pw.isNotEmpty()) put("p_password", pw) else put("p_password", JsonNull)
                        put("p_movements", movementsJson)
                    }
                ).decodeAs<String>()
            }
        }

    override suspend fun joinChallenge(challengeId: String, password: String?): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest.rpc(
                    "join_challenge",
                    buildJsonObject {
                        put("p_challenge_id", challengeId)
                        val pw = password?.trim().orEmpty()
                        if (pw.isNotEmpty()) put("p_password", pw)
                        else put("p_password", JsonNull)
                    }
                ).decodeAs<Boolean>()
            }
        }

    override suspend fun leaveChallenge(challengeId: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest.rpc(
                    "leave_challenge",
                    buildJsonObject { put("p_challenge_id", challengeId) }
                ).decodeAs<Boolean>()
            }
        }

    override suspend fun refreshMyProgress(): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest.rpc("refresh_my_challenge_progress")
                    .decodeAs<Int>()
            }
        }

    override suspend fun completeMovement(
        challengeId: String,
        movementId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            supabase.postgrest.rpc(
                "complete_event_movement",
                buildJsonObject {
                    put("p_challenge_id", challengeId)
                    put("p_movement_id", movementId)
                }
            )
            Unit
        }
    }

    override suspend fun uncompleteMovement(
        challengeId: String,
        movementId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            supabase.postgrest.rpc(
                "uncomplete_event_movement",
                buildJsonObject {
                    put("p_challenge_id", challengeId)
                    put("p_movement_id", movementId)
                }
            )
            Unit
        }
    }

    override suspend fun completeMultipleMovements(
        challengeId: String,
        movementIds: List<String>
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val idsJson = buildJsonArray { movementIds.forEach { add(it) } }
            supabase.postgrest.rpc(
                "complete_multiple_movements",
                buildJsonObject {
                    put("p_challenge_id", challengeId)
                    put("p_movement_ids", idsJson)
                }
            ).decodeAs<Int>()
        }
    }

    override suspend fun listMyEventsForDate(dateIso: String): Result<List<ChallengeSummary>> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest.rpc(
                    "list_my_events_for_date",
                    buildJsonObject { put("p_date", dateIso) }
                ).decodeList<MyChallengeRowDto>().map { it.toDomain() }
            }
        }

    override suspend fun listMyUpcomingEvents(days: Int): Result<List<ChallengeSummary>> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest.rpc(
                    "list_my_upcoming_events",
                    buildJsonObject { put("p_days", days) }
                ).decodeList<MyChallengeRowDto>().map { it.toDomain() }
            }
        }
}
