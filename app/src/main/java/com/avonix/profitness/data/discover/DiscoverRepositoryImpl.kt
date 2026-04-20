package com.avonix.profitness.data.discover

import com.avonix.profitness.data.discover.dto.DiscoverFeedRowDto
import com.avonix.profitness.domain.discover.DiscoverSort
import com.avonix.profitness.domain.discover.SharedProgram
import com.avonix.profitness.domain.discover.toDomain
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class DiscoverRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : DiscoverRepository {

    override suspend fun getFeed(
        sort: DiscoverSort, limit: Int, offset: Int
    ): Result<List<SharedProgram>> = withContext(Dispatchers.IO) {
        runCatching {
            supabase.postgrest.rpc(
                "get_discover_feed",
                buildJsonObject {
                    put("p_sort", sort.raw)
                    put("p_limit", limit)
                    put("p_offset", offset)
                }
            ).decodeList<DiscoverFeedRowDto>().map { it.toDomain() }
        }
    }

    override suspend fun toggleLike(programId: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest.rpc(
                    "toggle_program_like",
                    buildJsonObject { put("p_program_id", programId) }
                ).decodeAs<Boolean>()
            }
        }

    override suspend fun toggleSave(programId: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest.rpc(
                    "toggle_program_save",
                    buildJsonObject { put("p_program_id", programId) }
                ).decodeAs<Boolean>()
            }
        }

    override suspend fun shareMyProgram(
        originalProgramId: String,
        title: String,
        description: String?,
        tags: List<String>,
        difficulty: String?,
        durationWeeks: Int?,
        daysPerWeek: Int?
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            supabase.postgrest.rpc(
                "share_program",
                buildJsonObject {
                    put("p_original_program_id", originalProgramId)
                    put("p_title", title)
                    if (description != null) put("p_description", description)
                    put("p_tags", buildJsonArray { tags.forEach { add(JsonPrimitive(it)) } })
                    if (difficulty != null) put("p_difficulty", difficulty)
                    if (durationWeeks != null) put("p_duration_weeks", durationWeeks)
                    if (daysPerWeek != null) put("p_days_per_week", daysPerWeek)
                }
            ).decodeAs<String>()
        }
    }

    override suspend fun applyProgram(sharedProgramId: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest.rpc(
                    "apply_shared_program",
                    buildJsonObject { put("p_shared_id", sharedProgramId) }
                ).decodeAs<String>()
            }
        }
}
