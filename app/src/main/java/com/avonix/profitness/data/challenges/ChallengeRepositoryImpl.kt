package com.avonix.profitness.data.challenges

import com.avonix.profitness.data.challenges.dto.ChallengeDetailDto
import com.avonix.profitness.data.challenges.dto.MyChallengeRowDto
import com.avonix.profitness.data.challenges.dto.PublicChallengeRowDto
import com.avonix.profitness.domain.challenges.ChallengeDetail
import com.avonix.profitness.domain.challenges.ChallengeSummary
import com.avonix.profitness.domain.challenges.ChallengeTargetType
import com.avonix.profitness.domain.challenges.ChallengeVisibility
import com.avonix.profitness.domain.challenges.toDomain
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
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
}
