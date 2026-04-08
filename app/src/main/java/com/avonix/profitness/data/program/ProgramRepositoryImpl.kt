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
import com.avonix.profitness.data.cache.DiskCache

class ProgramRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val disk: DiskCache
) : ProgramRepository {

    // ── In-memory session cache ──────────────────────────────────────────────
    @Volatile private var _programs: List<Program>? = null
    @Volatile private var _programsUid: String? = null
    @Volatile private var _active: Program? = null
    @Volatile private var _activeUid: String? = null
    @Volatile private var _activeLoaded = false   // null program vs not-loaded ayrımı
    @Volatile private var _exercises: List<ExerciseItem>? = null

    override fun invalidateActiveCache() {
        _active = null; _activeUid = null; _activeLoaded = false
        disk.removeByPrefix("active_")
    }

    private fun invalidatePrograms() {
        _programs = null; _programsUid = null
        _active = null; _activeUid = null; _activeLoaded = false
        disk.removeByPrefix("programs_")
        disk.removeByPrefix("active_")
    }

    // ── Get Programs ──────────────────────────────────────────────────────────

    override suspend fun getUserPrograms(userId: String): Result<List<Program>> =
        withContext(Dispatchers.IO) {
            // 1. Memory
            _programs?.takeIf { _programsUid == userId }?.let { return@withContext Result.success(it) }
            // 2. Disk
            disk.get<List<Program>>("programs_$userId")?.let {
                _programs = it; _programsUid = userId
                return@withContext Result.success(it)
            }
            // 3. Network
            runCatching {
                val dtos = supabase.postgrest["programs"]
                    .select { filter { eq("user_id", userId) } }
                    .decodeList<ProgramDto>()

                val programs = dtos.map { dto ->
                    val days = fetchDays(dto.id)
                    dto.toDomain().copy(days = days)
                }
                programs
            }.also { r -> r.getOrNull()?.let {
                _programs = it; _programsUid = userId
                disk.put("programs_$userId", it)
            }}
        }

    override suspend fun getActiveProgram(userId: String): Result<Program?> =
        withContext(Dispatchers.IO) {
            // 1. Memory
            if (_activeLoaded && _activeUid == userId) return@withContext Result.success(_active)
            // 2. Disk — aktif program List olarak saklanır (boş = null, tek eleman = program)
            disk.get<List<Program>>("active_$userId")?.let { list ->
                val p = list.firstOrNull()
                _active = p; _activeUid = userId; _activeLoaded = true
                return@withContext Result.success(p)
            }
            // 3. Network
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
            }.also { r ->
                if (r.isSuccess) {
                    val p = r.getOrNull()
                    _active = p; _activeUid = userId; _activeLoaded = true
                    disk.put("active_$userId", if (p != null) listOf(p) else emptyList<Program>())
                }
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
            }.also { r -> if (r.isSuccess) invalidatePrograms() }
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
            }.also { r -> if (r.isSuccess) invalidatePrograms() }
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
            }.also { r -> if (r.isSuccess) invalidatePrograms() }
        }

    // ── Get Exercises ─────────────────────────────────────────────────────────

    override suspend fun getAllExercises(): Result<List<ExerciseItem>> =
        withContext(Dispatchers.IO) {
            _exercises?.let { return@withContext Result.success(it) }
            disk.get<List<ExerciseItem>>("exercises")?.let {
                _exercises = it; return@withContext Result.success(it)
            }
            runCatching {
                supabase.postgrest["exercises"]
                    .select { order("category", Order.ASCENDING) }
                    .decodeList<ExerciseDto>()
                    .map { it.toDomain() }
            }.also { r -> r.getOrNull()?.let { _exercises = it; disk.put("exercises", it) } }
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

    override suspend fun updateProgram(
        programId: String,
        name     : String,
        days     : List<ManualDayInput>
    ): Result<Program> = withContext(Dispatchers.IO) {
        runCatching {
            // 1) Program adını güncelle
            supabase.postgrest["programs"]
                .update({ set("name", name) }) { filter { eq("id", programId) } }

            // 2) Mevcut günlerin ID'lerini çek
            val oldDayIds = supabase.postgrest["program_days"]
                .select { filter { eq("program_id", programId) } }
                .decodeList<ProgramDayDto>()
                .map { it.id }

            // 3) Eski egzersizleri sil
            for (dayId in oldDayIds) {
                runCatching {
                    supabase.postgrest["program_exercises"]
                        .delete { filter { eq("program_day_id", dayId) } }
                }
            }

            // 3b) workout_logs'daki referansı NULL yap — geçmişi koruyoruz, bağlantıyı koparıyoruz
            for (dayId in oldDayIds) {
                runCatching {
                    supabase.postgrest["workout_logs"]
                        .update({ set("program_day_id", null as String?) }) {
                            filter { eq("program_day_id", dayId) }
                        }
                }
            }

            // 4) Eski günleri sil
            supabase.postgrest["program_days"]
                .delete { filter { eq("program_id", programId) } }

            // 5) Yeni günleri ve egzersizleri ekle
            val createdDays = days.mapIndexed { dayIdx, dayInput ->
                supabase.postgrest["program_days"]
                    .insert(buildJsonObject {
                        put("program_id", programId)
                        put("day_index", dayIdx)
                        put("title", dayInput.title)
                        put("is_rest_day", dayInput.isRestDay)
                    })

                val dayDto = supabase.postgrest["program_days"]
                    .select {
                        filter {
                            eq("program_id", programId)
                            eq("day_index", dayIdx)
                        }
                    }
                    .decodeSingle<ProgramDayDto>()

                dayInput.exercises.forEach { exInput ->
                    supabase.postgrest["program_exercises"]
                        .insert(buildJsonObject {
                            put("program_day_id", dayDto.id)
                            put("exercise_id", exInput.exerciseId)
                            put("sets", exInput.sets)
                            put("reps", exInput.reps)
                            put("rest_seconds", exInput.restSeconds)
                            put("order_index", exInput.orderIndex)
                        })
                }

                // Tüm egzersizleri DB'den çek — döndürülen Program nesnesi doğru olsun
                val exercises = fetchExercises(dayDto.id)
                dayDto.toDomain().copy(exercises = exercises)
            }

            // 6) Güncel programı döndür
            val programDto = supabase.postgrest["programs"]
                .select { filter { eq("id", programId) } }
                .decodeSingle<ProgramDto>()

            programDto.toDomain().copy(days = createdDays)
        }.also { r -> if (r.isSuccess) invalidatePrograms() }
    }

    override suspend fun deleteProgram(programId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1) program_days ID'lerini çek
                val dayIds = supabase.postgrest["program_days"]
                    .select { filter { eq("program_id", programId) } }
                    .decodeList<ProgramDayDto>()
                    .map { it.id }

                if (dayIds.isNotEmpty()) {
                    // 2) workout_logs'daki program_day_id referansını NULL yap — geçmiş korunuyor
                    for (dayId in dayIds) {
                        runCatching {
                            supabase.postgrest["workout_logs"]
                                .update({ set("program_day_id", null as String?) }) {
                                    filter { eq("program_day_id", dayId) }
                                }
                        }
                    }

                    // 3) program_exercises sil
                    for (dayId in dayIds) {
                        runCatching {
                            supabase.postgrest["program_exercises"]
                                .delete { filter { eq("program_day_id", dayId) } }
                        }
                    }
                }

                // 4) program_days sil
                supabase.postgrest["program_days"]
                    .delete { filter { eq("program_id", programId) } }

                // 5) programı sil
                supabase.postgrest["programs"]
                    .delete { filter { eq("id", programId) } }
                Unit
            }.also { r -> if (r.isSuccess) invalidatePrograms() }
        }

    // ── Add Exercise ──────────────────────────────────────────────────────────

    override suspend fun addExercise(
        name: String,
        nameEn: String,
        targetMuscle: String,
        category: String,
        setsDefault: Int,
        repsDefault: Int
    ): Result<ExerciseItem> = withContext(Dispatchers.IO) {
        _exercises = null; disk.remove("exercises")  // yeni egzersiz eklendi
        runCatching {
            supabase.postgrest["exercises"]
                .insert(
                    buildJsonObject {
                        put("name", name)
                        put("name_en", nameEn)
                        put("target_muscle", targetMuscle)
                        put("category", category)
                        put("sets_default", setsDefault)
                        put("reps_default", repsDefault)
                        put("description", "")
                    }
                )

            // Fetch the just-inserted exercise by name
            supabase.postgrest["exercises"]
                .select { filter { eq("name", name) } }
                .decodeSingle<ExerciseDto>()
                .toDomain()
        }
    }

    // ── Request Exercise ──────────────────────────────────────────────────────

    override suspend fun requestExercise(
        userId: String,
        name: String,
        targetMuscle: String,
        notes: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            supabase.postgrest["exercise_requests"]
                .insert(
                    buildJsonObject {
                        put("user_id", userId)
                        put("name", name)
                        put("target_muscle", targetMuscle)
                        put("notes", notes)
                    }
                )
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
