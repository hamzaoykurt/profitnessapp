package com.avonix.profitness.data.program

import com.avonix.profitness.data.program.dto.ExerciseDto
import com.avonix.profitness.data.program.dto.ProgramDayDto
import com.avonix.profitness.data.program.dto.ProgramDto
import com.avonix.profitness.data.program.dto.ProgramExerciseWithNameDto
import com.avonix.profitness.data.program.dto.toDomain
import com.avonix.profitness.domain.model.ExerciseItem
import com.avonix.profitness.domain.model.Program
import com.avonix.profitness.domain.model.ProgramType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class ProgramRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : ProgramRepository {

    // ── Get Programs ──────────────────────────────────────────────────────────

    override suspend fun getUserPrograms(userId: String): Result<List<Program>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dtos = supabase.postgrest["programs"]
                    .select { filter { eq("user_id", userId) } }
                    .decodeList<ProgramDto>()

                val programs = dtos.map { dto ->
                    val days = fetchDays(dto.id)
                    dto.toDomain().copy(days = days)
                }
                programs
            }
        }

    override suspend fun getActiveProgram(userId: String): Result<Program?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dto = supabase.postgrest["programs"]
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("is_active", true)
                        }
                    }
                    .decodeSingleOrNull<ProgramDto>()
                    ?: return@runCatching null

                val days = fetchDays(dto.id)
                dto.toDomain().copy(days = days)
            }
        }

    private suspend fun fetchDays(programId: String): List<com.avonix.profitness.domain.model.ProgramDay> {
        val dayDtos = supabase.postgrest["program_days"]
            .select {
                filter { eq("program_id", programId) }
                order("day_index", Order.ASCENDING)
            }
            .decodeList<ProgramDayDto>()

        return dayDtos.map { dayDto ->
            val exercises = fetchExercises(dayDto.id)
            dayDto.toDomain().copy(exercises = exercises)
        }
    }

    private suspend fun fetchExercises(programDayId: String): List<com.avonix.profitness.domain.model.ProgramExercise> {
        return supabase.postgrest["program_exercises"]
            .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("*, exercises(name, target_muscle, category, image_url)")) {
                filter { eq("program_day_id", programDayId) }
                order("order_index", Order.ASCENDING)
            }
            .decodeList<ProgramExerciseWithNameDto>()
            .map { it.toDomain() }
    }

    // ── Create From Template ──────────────────────────────────────────────────

    override suspend fun createFromTemplate(userId: String, templateKey: String): Result<Program> =
        withContext(Dispatchers.IO) {
            runCatching {
                val template = findTemplate(templateKey)
                    ?: error("Template bulunamadı: $templateKey")

                // Deactivate existing programs
                deactivateAll(userId)

                // Insert program record (don't decode insert response — use separate SELECT)
                supabase.postgrest["programs"]
                    .insert(
                        buildJsonObject {
                            put("user_id", userId)
                            put("name", templateKey)
                            put("type", ProgramType.TEMPLATE.name.lowercase())
                            put("is_active", true)
                        }
                    )

                // Fetch the just-created program via SELECT
                val programDto = supabase.postgrest["programs"]
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("is_active", true)
                        }
                    }
                    .decodeSingle<ProgramDto>()

                // Fetch all exercises upfront for batch lookup
                val allExercises = supabase.postgrest["exercises"]
                    .select()
                    .decodeList<ExerciseDto>()
                val exerciseMap = allExercises.associateBy { it.name.trim().lowercase() }

                // Create days and exercises
                val createdDays = template.days.mapIndexed { dayIdx, templateDay ->
                    // Insert day (don't decode insert response)
                    supabase.postgrest["program_days"]
                        .insert(
                            buildJsonObject {
                                put("program_id", programDto.id)
                                put("day_index", dayIdx)
                                put("title", templateDay.title)
                                put("is_rest_day", templateDay.isRestDay)
                            }
                        )

                    // Fetch the just-inserted day via SELECT
                    val dayDto = supabase.postgrest["program_days"]
                        .select {
                            filter {
                                eq("program_id", programDto.id)
                                eq("day_index", dayIdx)
                            }
                        }
                        .decodeSingle<ProgramDayDto>()

                    if (!templateDay.isRestDay) {
                        templateDay.exercises.forEachIndexed { exIdx, templateEx ->
                            val exerciseDto = exerciseMap[templateEx.exerciseName.trim().lowercase()]
                                ?: exerciseMap.entries
                                    .firstOrNull { it.key.contains(templateEx.exerciseName.trim().lowercase()) }
                                    ?.value
                                ?: return@forEachIndexed

                            supabase.postgrest["program_exercises"]
                                .insert(
                                    buildJsonObject {
                                        put("program_day_id", dayDto.id)
                                        put("exercise_id", exerciseDto.id)
                                        put("sets", templateEx.sets)
                                        put("reps", templateEx.reps)
                                        put("rest_seconds", templateEx.restSeconds)
                                        put("order_index", exIdx)
                                    }
                                )
                        }
                    }

                    dayDto.toDomain()
                }

                programDto.toDomain().copy(days = createdDays)
            }
        }

    // ── Create Manual ─────────────────────────────────────────────────────────

    override suspend fun createManual(
        userId: String,
        name: String,
        days: List<ManualDayInput>
    ): Result<Program> =
        withContext(Dispatchers.IO) {
            runCatching {
                deactivateAll(userId)

                // Insert program record (don't decode insert response — use separate SELECT)
                supabase.postgrest["programs"]
                    .insert(
                        buildJsonObject {
                            put("user_id", userId)
                            put("name", name)
                            put("type", ProgramType.MANUAL.name.lowercase())
                            put("is_active", true)
                        }
                    )

                // Fetch the just-created program via SELECT
                val programDto = supabase.postgrest["programs"]
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("is_active", true)
                        }
                    }
                    .decodeSingle<ProgramDto>()

                val createdDays = days.mapIndexed { dayIdx, dayInput ->
                    // Insert day (don't decode insert response)
                    supabase.postgrest["program_days"]
                        .insert(
                            buildJsonObject {
                                put("program_id", programDto.id)
                                put("day_index", dayIdx)
                                put("title", dayInput.title)
                                put("is_rest_day", dayInput.isRestDay)
                            }
                        )

                    // Fetch the just-inserted day via SELECT
                    val dayDto = supabase.postgrest["program_days"]
                        .select {
                            filter {
                                eq("program_id", programDto.id)
                                eq("day_index", dayIdx)
                            }
                        }
                        .decodeSingle<ProgramDayDto>()

                    dayInput.exercises.forEach { exInput ->
                        supabase.postgrest["program_exercises"]
                            .insert(
                                buildJsonObject {
                                    put("program_day_id", dayDto.id)
                                    put("exercise_id", exInput.exerciseId)
                                    put("sets", exInput.sets)
                                    put("reps", exInput.reps)
                                    put("rest_seconds", exInput.restSeconds)
                                    put("order_index", exInput.orderIndex)
                                }
                            )
                    }

                    dayDto.toDomain()
                }

                programDto.toDomain().copy(days = createdDays)
            }
        }

    // ── Set Active ────────────────────────────────────────────────────────────

    override suspend fun setActive(programId: String, userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                deactivateAll(userId)
                supabase.postgrest["programs"]
                    .update({ set("is_active", true) }) {
                        filter { eq("id", programId) }
                    }
                Unit
            }
        }

    // ── Get Exercises ─────────────────────────────────────────────────────────

    override suspend fun getAllExercises(): Result<List<ExerciseItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest["exercises"]
                    .select { order("category", Order.ASCENDING) }
                    .decodeList<ExerciseDto>()
                    .map { it.toDomain() }
            }
        }

    // ── Update / Delete ───────────────────────────────────────────────────────

    override suspend fun updateProgramName(programId: String, name: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest["programs"]
                    .update({ set("name", name) }) {
                        filter { eq("id", programId) }
                    }
                Unit
            }
        }

    override suspend fun deleteProgram(programId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest["programs"]
                    .delete { filter { eq("id", programId) } }
                Unit
            }
        }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun deactivateAll(userId: String) {
        supabase.postgrest["programs"]
            .update({ set("is_active", false) }) {
                filter {
                    eq("user_id", userId)
                    eq("is_active", true)
                }
            }
    }
}
