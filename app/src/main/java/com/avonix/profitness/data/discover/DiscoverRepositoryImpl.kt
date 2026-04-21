package com.avonix.profitness.data.discover

import com.avonix.profitness.data.discover.dto.DiscoverFeedRowDto
import com.avonix.profitness.data.discover.dto.MySharedProgramRowDto
import com.avonix.profitness.domain.discover.DiscoverSort
import com.avonix.profitness.domain.discover.MySharedProgram
import com.avonix.profitness.domain.discover.SharedProgram
import com.avonix.profitness.domain.discover.toDomain
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

/** Nullable sarma — `null` ise `JsonNull`, aksi halde `JsonPrimitive(value)`.
 *  PostgREST RPC'de key'in gövdeden TAMAMEN atılması, default'u olmayan
 *  parametre için 404 Not Found'a yol açıyor. Bu helper ile her alan daima
 *  gönderilir; DB default'u (ya da NULL) uygulanır. */
private fun jn(value: String?): JsonElement = if (value == null) JsonNull else JsonPrimitive(value)
private fun jn(value: Int?):    JsonElement = if (value == null) JsonNull else JsonPrimitive(value)

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
                    // Null alanlar da gönderilir — aksi halde PostgREST default'suz
                    // bir overload arar ve 404 döner (FAZ 7I-FIX).
                    put("p_description",    jn(description))
                    put("p_tags",           buildJsonArray { tags.forEach { add(JsonPrimitive(it)) } })
                    put("p_difficulty",     jn(difficulty))
                    put("p_duration_weeks", jn(durationWeeks))
                    put("p_days_per_week",  jn(daysPerWeek))
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

    override suspend fun listMyShared(): Result<List<MySharedProgram>> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest.rpc("list_my_shared_programs")
                    .decodeList<MySharedProgramRowDto>()
                    .map { it.toDomain() }
            }
        }

    override suspend fun updateShared(
        sharedId: String,
        title: String?,
        description: String?,
        tags: List<String>?,
        difficulty: String?,
        durationWeeks: Int?,
        daysPerWeek: Int?,
        resyncSnapshot: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            supabase.postgrest.rpc(
                "update_shared_program",
                buildJsonObject {
                    put("p_shared_id", sharedId)
                    put("p_title",          jn(title))
                    put("p_description",    jn(description))
                    // tags: null → JsonNull (DB COALESCE mevcut değeri korur)
                    if (tags == null) put("p_tags", JsonNull)
                    else              put("p_tags", buildJsonArray { tags.forEach { add(JsonPrimitive(it)) } })
                    put("p_difficulty",     jn(difficulty))
                    put("p_duration_weeks", jn(durationWeeks))
                    put("p_days_per_week",  jn(daysPerWeek))
                    put("p_resync_snapshot", resyncSnapshot)
                }
            )
            Unit
        }
    }

    override suspend fun deleteShared(sharedId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest.rpc(
                    "delete_shared_program",
                    buildJsonObject { put("p_shared_id", sharedId) }
                )
                Unit
            }
        }
}
