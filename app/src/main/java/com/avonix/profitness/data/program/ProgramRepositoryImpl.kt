package com.avonix.profitness.data.program

import com.avonix.profitness.data.local.dao.ExerciseDao
import com.avonix.profitness.data.local.dao.ProgramDao
import com.avonix.profitness.data.local.entity.ExerciseEntity
import com.avonix.profitness.data.local.entity.ProgramDayEntity
import com.avonix.profitness.data.local.entity.ProgramEntity
import com.avonix.profitness.data.local.entity.ProgramExerciseEntity
import com.avonix.profitness.data.local.relation.ProgramWithDays
import com.avonix.profitness.data.program.dto.ExerciseDto
import com.avonix.profitness.data.program.dto.ProgramDayDto
import com.avonix.profitness.data.program.dto.ProgramDto
import com.avonix.profitness.data.program.dto.ProgramExerciseWithNameDto
import com.avonix.profitness.data.sync.SyncManager
import com.avonix.profitness.domain.model.ExerciseItem
import com.avonix.profitness.domain.model.Program
import com.avonix.profitness.domain.model.ProgramDay
import com.avonix.profitness.domain.model.ProgramExercise
import com.avonix.profitness.domain.model.ProgramType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject

class ProgramRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val programDao: ProgramDao,
    private val exerciseDao: ExerciseDao,
    private val syncManager: SyncManager
) : ProgramRepository {

    // ═════════════════════════════════════════════════════════════════════════
    //  OBSERVE — Room Flow (reactive, always available)
    // ═════════════════════════════════════════════════════════════════════════

    override fun observeActiveProgram(userId: String): Flow<Program?> =
        programDao.observeActiveProgram(userId)
            .map { it?.toDomain() }
            .flowOn(Dispatchers.IO)

    override fun observeUserPrograms(userId: String): Flow<List<Program>> =
        programDao.observeUserPrograms(userId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    override fun observeExercises(): Flow<List<ExerciseItem>> =
        exerciseDao.observeAll()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    // ═════════════════════════════════════════════════════════════════════════
    //  ONE-SHOT READS — Room'dan (anında)
    // ═════════════════════════════════════════════════════════════════════════

    override suspend fun getUserPrograms(userId: String): Result<List<Program>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val local = programDao.getUserPrograms(userId)
                if (local.isNotEmpty()) return@runCatching local.map { it.toDomain() }
                // Room boşsa Supabase'den çek
                syncManager.pullPrograms(userId)
                programDao.getUserPrograms(userId).map { it.toDomain() }
            }
        }

    override suspend fun getActiveProgram(userId: String): Result<Program?> =
        withContext(Dispatchers.IO) {
            runCatching {
                programDao.getActiveProgram(userId)?.toDomain()
            }
        }

    override suspend fun getAllExercises(): Result<List<ExerciseItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val local = exerciseDao.getAll()
                if (local.isNotEmpty()) return@runCatching local.map { it.toDomain() }
                syncManager.pullExercises()
                exerciseDao.getAll().map { it.toDomain() }
            }
        }

    // ═════════════════════════════════════════════════════════════════════════
    //  WRITE — Supabase'e yaz, sonra Room'a sync et
    // ═════════════════════════════════════════════════════════════════════════

    override suspend fun createFromTemplate(userId: String, templateKey: String): Result<Program> =
        withContext(Dispatchers.IO) {
            runCatching {
                val template = findTemplate(templateKey)
                    ?: error("Template bulunamadı: $templateKey")

                // Diğer programları pasife al
                supabase.postgrest["programs"]
                    .update({ set("is_active", false) }) {
                        filter { eq("user_id", userId); eq("is_active", true) }
                    }

                // Program oluştur
                supabase.postgrest["programs"]
                    .insert(buildJsonObject {
                        put("user_id", userId)
                        put("name", templateKey)
                        put("type", ProgramType.TEMPLATE.name.lowercase())
                        put("is_active", true)
                    })

                val programDto = supabase.postgrest["programs"]
                    .select { filter { eq("user_id", userId); eq("is_active", true) } }
                    .decodeSingle<ProgramDto>()

                // Tüm egzersizleri çek
                val allExercises = supabase.postgrest["exercises"]
                    .select().decodeList<ExerciseDto>()
                val exerciseMap = allExercises.associateBy { it.name.trim().lowercase() }

                // Günleri ve egzersizleri oluştur
                template.days.forEachIndexed { dayIdx, templateDay ->
                    supabase.postgrest["program_days"]
                        .insert(buildJsonObject {
                            put("program_id", programDto.id)
                            put("day_index", dayIdx)
                            put("title", templateDay.title)
                            put("is_rest_day", templateDay.isRestDay)
                        })

                    val dayDto = supabase.postgrest["program_days"]
                        .select {
                            filter { eq("program_id", programDto.id); eq("day_index", dayIdx) }
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
                                .insert(buildJsonObject {
                                    put("program_day_id", dayDto.id)
                                    put("exercise_id", exerciseDto.id)
                                    put("sets", templateEx.sets)
                                    put("reps", templateEx.reps)
                                    put("rest_seconds", templateEx.restSeconds)
                                    put("order_index", exIdx)
                                })
                        }
                    }
                }

                // Room'a sync et (Supabase'den tam veri çek)
                syncManager.pullPrograms(userId)
                syncManager.pullExercises()

                // Oluşturulan programı Room'dan döndür
                programDao.getActiveProgram(userId)?.toDomain()
                    ?: error("Program Room'a sync edilemedi")
            }
        }

    override suspend fun createManual(userId: String, name: String, days: List<ManualDayInput>): Result<Program> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest["programs"]
                    .update({ set("is_active", false) }) {
                        filter { eq("user_id", userId); eq("is_active", true) }
                    }

                supabase.postgrest["programs"]
                    .insert(buildJsonObject {
                        put("user_id", userId)
                        put("name", name)
                        put("type", ProgramType.MANUAL.name.lowercase())
                        put("is_active", true)
                    })

                val programDto = supabase.postgrest["programs"]
                    .select { filter { eq("user_id", userId); eq("is_active", true) } }
                    .decodeSingle<ProgramDto>()

                days.forEachIndexed { dayIdx, dayInput ->
                    supabase.postgrest["program_days"]
                        .insert(buildJsonObject {
                            put("program_id", programDto.id)
                            put("day_index", dayIdx)
                            put("title", dayInput.title)
                            put("is_rest_day", dayInput.isRestDay)
                        })

                    val dayDto = supabase.postgrest["program_days"]
                        .select {
                            filter { eq("program_id", programDto.id); eq("day_index", dayIdx) }
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
                }

                syncManager.pullPrograms(userId)

                programDao.getActiveProgram(userId)?.toDomain()
                    ?: error("Program Room'a sync edilemedi")
            }
        }

    override suspend fun setActive(programId: String, userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest["programs"]
                    .update({ set("is_active", false) }) {
                        filter { eq("user_id", userId); eq("is_active", true) }
                    }
                supabase.postgrest["programs"]
                    .update({ set("is_active", true) }) {
                        filter { eq("id", programId) }
                    }
                // Room'u da güncelle — anında UI yansıması
                programDao.deactivateAll(userId)
                programDao.activate(programId)
                Unit
            }
        }

    override suspend fun updateProgramName(programId: String, name: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                supabase.postgrest["programs"]
                    .update({ set("name", name) }) { filter { eq("id", programId) } }
                programDao.updateName(programId, name)
                Unit
            }
        }

    override suspend fun updateProgram(
        programId: String,
        name: String,
        days: List<ManualDayInput>
    ): Result<Program> = withContext(Dispatchers.IO) {
        runCatching {
            // Mimari karar (content-addressable snapshot):
            // Düzenleme programın id'sini değiştirmez — mevcut satır mutate edilir.
            // Paylaşılan kopya (shared_programs.program_data) ayrı, immutable bir snapshot
            // olarak donmuş; o asla bu düzenlemeden etkilenmez. Discover feed'inin
            // "UYGULANDI/UYGULA" çentiği ise her satır için sunucu tarafında hesaplanan
            // SHA-256 content_hash'ine bakar (programs trigger'ı her INSERT/UPDATE/DELETE
            // sonrası hash'i yeniler). Kullanıcı düzenleme yapınca hash değişir → çentik
            // otomatik olarak "UYGULA"ya döner.

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

            // 3b) workout_logs FK referansını NULL yap (gün siliniyor — log tarihi korunur)
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

            // 5) Yeni günleri ve egzersizleri ekle (program id aynı kalır)
            days.forEachIndexed { dayIdx, dayInput ->
                val newDayId = UUID.randomUUID().toString()
                supabase.postgrest["program_days"]
                    .insert(buildJsonObject {
                        put("id", newDayId)
                        put("program_id", programId)
                        put("day_index", dayIdx)
                        put("title", dayInput.title)
                        put("is_rest_day", dayInput.isRestDay)
                    })

                dayInput.exercises.forEach { exInput ->
                    supabase.postgrest["program_exercises"]
                        .insert(buildJsonObject {
                            put("id", UUID.randomUUID().toString())
                            put("program_day_id", newDayId)
                            put("exercise_id", exInput.exerciseId)
                            put("sets", exInput.sets)
                            put("reps", exInput.reps)
                            put("rest_seconds", exInput.restSeconds)
                            put("order_index", exInput.orderIndex)
                        })
                }
            }

            // 6) Room'a sync et — content_hash trigger'larca yeniden hesaplanmış olarak gelir
            val userId = supabase.postgrest["programs"]
                .select { filter { eq("id", programId) } }
                .decodeSingle<ProgramDto>().user_id
            syncManager.pullPrograms(userId)

            programDao.getUserPrograms(userId)
                .firstOrNull { it.program.id == programId }
                ?.toDomain()
                ?: error("Güncellenen program Room'da bulunamadı")
        }
    }

    override suspend fun deleteProgram(programId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Room'dan hemen sil (anında UI yansıması)
                programDao.deleteProgram(programId)

                // Supabase'den sil
                val dayIds = supabase.postgrest["program_days"]
                    .select { filter { eq("program_id", programId) } }
                    .decodeList<ProgramDayDto>()
                    .map { it.id }

                if (dayIds.isNotEmpty()) {
                    for (dayId in dayIds) {
                        runCatching {
                            supabase.postgrest["workout_logs"]
                                .update({ set("program_day_id", null as String?) }) {
                                    filter { eq("program_day_id", dayId) }
                                }
                        }
                        runCatching {
                            supabase.postgrest["program_exercises"]
                                .delete { filter { eq("program_day_id", dayId) } }
                        }
                    }
                }
                supabase.postgrest["program_days"]
                    .delete { filter { eq("program_id", programId) } }
                supabase.postgrest["programs"]
                    .delete { filter { eq("id", programId) } }
                // Supabase silme sırasında bir sync Room'a geri yazmış olabilir;
                // işlem bitince Room'dan kesin olarak sil.
                programDao.deleteProgram(programId)
                Unit
            }
        }

    override suspend fun addExercise(
        name: String,
        nameEn: String,
        targetMuscle: String,
        category: String,
        setsDefault: Int,
        repsDefault: Int
    ): Result<ExerciseItem> = withContext(Dispatchers.IO) {
        runCatching {
            supabase.postgrest["exercises"]
                .insert(buildJsonObject {
                    put("name", name)
                    put("name_en", nameEn)
                    put("target_muscle", targetMuscle)
                    put("category", category)
                    put("sets_default", setsDefault)
                    put("reps_default", repsDefault)
                    put("description", "")
                })

            val dto = supabase.postgrest["exercises"]
                .select { filter { eq("name", name) } }
                .decodeSingle<ExerciseDto>()

            // Room'a da ekle
            exerciseDao.upsert(ExerciseEntity(
                id = dto.id, name = dto.name, nameEn = dto.name_en,
                targetMuscle = dto.target_muscle, category = dto.category,
                setsDefault = dto.sets_default, repsDefault = dto.reps_default,
                description = dto.description
            ))

            dto.toDomain()
        }
    }

    override suspend fun requestExercise(
        userId: String,
        name: String,
        targetMuscle: String,
        notes: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            supabase.postgrest["exercise_requests"]
                .insert(buildJsonObject {
                    put("user_id", userId)
                    put("name", name)
                    put("target_muscle", targetMuscle)
                    put("notes", notes)
                })
            Unit
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SYNC
    // ═════════════════════════════════════════════════════════════════════════

    override suspend fun syncFromRemote(userId: String) {
        syncManager.pullPrograms(userId)
        syncManager.pullExercises()
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  MAPPERS: Room → Domain
    // ═════════════════════════════════════════════════════════════════════════

    private suspend fun ProgramWithDays.toDomain(): Program {
        val dayIds = days.map { it.day.id }
        val exerciseMap = if (dayIds.isNotEmpty()) {
            programDao.getExercisesForDays(dayIds).groupBy { it.programDayId }
        } else emptyMap()

        return Program(
            id = program.id,
            userId = program.userId,
            name = program.name,
            type = runCatching { ProgramType.valueOf(program.type.uppercase()) }
                .getOrDefault(ProgramType.MANUAL),
            isActive = program.isActive,
            createdAt = program.createdAt,
            contentHash = program.contentHash,
            appliedFromSharedId = program.appliedFromSharedId,
            days = days.map { dwe ->
                ProgramDay(
                    id = dwe.day.id,
                    programId = dwe.day.programId,
                    dayIndex = dwe.day.dayIndex,
                    title = dwe.day.title,
                    isRestDay = dwe.day.isRestDay,
                    exercises = (exerciseMap[dwe.day.id] ?: emptyList()).map { pe ->
                        ProgramExercise(
                            id = pe.id,
                            programDayId = pe.programDayId,
                            exerciseId = pe.exerciseId,
                            exerciseName = pe.exerciseName,
                            targetMuscle = pe.targetMuscle,
                            sets = pe.sets,
                            reps = pe.reps,
                            weightKg = pe.weightKg,
                            restSeconds = pe.restSeconds,
                            orderIndex = pe.orderIndex,
                            category = pe.category,
                            imageUrl = pe.imageUrl
                        )
                    }
                )
            }
        )
    }

    private fun ExerciseEntity.toDomain() = ExerciseItem(
        id = id, name = name, nameEn = nameEn,
        targetMuscle = targetMuscle, category = category,
        setsDefault = setsDefault, repsDefault = repsDefault,
        description = description
    )

    private fun ExerciseDto.toDomain() = ExerciseItem(
        id = id, name = name, nameEn = name_en,
        targetMuscle = target_muscle, category = category,
        setsDefault = sets_default, repsDefault = reps_default,
        description = description
    )
}
