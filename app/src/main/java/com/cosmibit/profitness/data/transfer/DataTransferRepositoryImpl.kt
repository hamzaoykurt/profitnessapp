package com.cosmibit.profitness.data.transfer

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.cosmibit.profitness.data.local.AppDatabase
import com.cosmibit.profitness.data.local.entity.ExerciseEntity
import com.cosmibit.profitness.data.local.entity.ExerciseLogEntity
import com.cosmibit.profitness.data.local.entity.ProgramDayEntity
import com.cosmibit.profitness.data.local.entity.ProgramEntity
import com.cosmibit.profitness.data.local.entity.ProgramExerciseEntity
import com.cosmibit.profitness.data.local.entity.SetCompletionEntity
import com.cosmibit.profitness.data.local.entity.WeightLogEntity
import com.cosmibit.profitness.data.local.entity.WorkoutLogEntity
import com.cosmibit.profitness.data.profile.ProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataTransferRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val profileRepository: ProfileRepository
) : DataTransferRepository {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    override suspend fun exportTo(userId: String, destination: Uri): Result<DataTransferSummary> =
        withContext(Dispatchers.IO) {
            runCatching {
                val programs = database.programDao().getUserPrograms(userId)
                val workouts = database.workoutDao().getAllLogsForUser(userId)
                val exerciseLogs = database.workoutDao().getAllExerciseLogsForUser(userId)
                val sets = database.setCompletionDao().getAllForUser(userId)
                val weights = database.weightLogDao().getAllForUser(userId)
                val exercises = database.exerciseDao().getVisible(userId)
                val profile = profileRepository.getProfile(userId).getOrNull()

                val root = buildJsonObject {
                    put("format", FORMAT)
                    put("version", VERSION)
                    put("exportedAt", Instant.now().toString())
                    put("sourceUserId", userId)
                    put("profile", buildJsonObject {
                        putNullable("displayName", profile?.display_name)
                        putNullable("avatar", profile?.avatar_url)
                        putNullable("fitnessGoal", profile?.fitness_goal)
                        putNullable("heightCm", profile?.height_cm)
                        putNullable("weightKg", profile?.weight_kg)
                        putNullable("gender", profile?.gender)
                        putNullable("birthDate", profile?.birth_date)
                    })
                    put("exercises", buildJsonArray { exercises.forEach { add(it.toJson()) } })
                    put("programs", buildJsonArray {
                        programs.forEach { relation ->
                            add(buildJsonObject {
                                put("program", relation.program.toJson())
                                put("days", buildJsonArray {
                                    relation.days.sortedBy { it.day.dayIndex }.forEach { day ->
                                        add(buildJsonObject {
                                            put("day", day.day.toJson())
                                            put("exercises", buildJsonArray {
                                                day.exercises.sortedBy { it.orderIndex }.forEach { add(it.toJson()) }
                                            })
                                        })
                                    }
                                })
                            })
                        }
                    })
                    put("workouts", buildJsonArray { workouts.forEach { add(it.toJson()) } })
                    put("exerciseLogs", buildJsonArray { exerciseLogs.forEach { add(it.toJson()) } })
                    put("setRecords", buildJsonArray { sets.forEach { add(it.toJson()) } })
                    put("weightRecords", buildJsonArray { weights.forEach { add(it.toJson()) } })
                }

                context.contentResolver.openOutputStream(destination, "wt")?.bufferedWriter()?.use {
                    it.write(json.encodeToString(JsonObject.serializer(), root))
                } ?: error("Dosya açılamadı")

                DataTransferSummary(programs.size, workouts.size, sets.size, weights.size)
            }
        }

    override suspend fun importFrom(userId: String, source: Uri): Result<DataTransferSummary> =
        withContext(Dispatchers.IO) {
            runCatching {
                val text = context.contentResolver.openInputStream(source)?.bufferedReader()?.use { it.readText() }
                    ?: error("Dosya okunamadı")
                require(text.toByteArray().size <= MAX_FILE_BYTES) { "Yedek dosyası çok büyük" }
                val root = json.parseToJsonElement(text).jsonObject
                require(root.string("format") == FORMAT) { "Bu dosya ProFitness yedeği değil" }
                require(root.int("version") == VERSION) { "Yedek sürümü desteklenmiyor" }
                require(root.string("sourceUserId") == userId) { "Bu yedek farklı bir hesaba ait" }

                val profile = root.objectOrNull("profile")
                if (profile != null) {
                    profileRepository.updateProfile(
                        userId = userId,
                        displayName = profile.stringOrNull("displayName") ?: "",
                        avatar = profile.stringOrNull("avatar") ?: "🏋️",
                        fitnessGoal = profile.stringOrNull("fitnessGoal") ?: "",
                        heightCm = profile.doubleOrNull("heightCm") ?: 0.0,
                        weightKg = profile.doubleOrNull("weightKg") ?: 0.0,
                        gender = profile.stringOrNull("gender") ?: "",
                        birthDate = profile.stringOrNull("birthDate") ?: ""
                    )
                }

                val exercises = root.array("exercises").map { it.jsonObject.toExercise(userId) }
                val programBundles = root.array("programs").map { it.jsonObject }
                val programs = programBundles.map { it.obj("program").toProgram(userId) }
                val days = programBundles.flatMap { bundle ->
                    bundle.array("days").map { it.jsonObject.obj("day").toProgramDay() }
                }
                val programExercises = programBundles.flatMap { bundle ->
                    bundle.array("days").flatMap { day ->
                        day.jsonObject.array("exercises").map { it.jsonObject.toProgramExercise() }
                    }
                }
                val workouts = root.array("workouts").map { it.jsonObject.toWorkout(userId) }
                val exerciseLogs = root.array("exerciseLogs").map { it.jsonObject.toExerciseLog() }
                val sets = root.array("setRecords").map { it.jsonObject.toSetRecord(userId) }
                val weights = root.array("weightRecords").map { it.jsonObject.toWeight(userId) }

                database.withTransaction {
                    if (exercises.isNotEmpty()) database.exerciseDao().upsertAll(exercises)
                    if (programs.isNotEmpty()) database.programDao().upsertPrograms(programs)
                    if (days.isNotEmpty()) database.programDao().upsertDays(days)
                    if (programExercises.isNotEmpty()) database.programDao().upsertExercises(programExercises)
                    if (workouts.isNotEmpty()) database.workoutDao().upsertLogs(workouts)
                    if (exerciseLogs.isNotEmpty()) database.workoutDao().upsertExerciseLogs(exerciseLogs)
                    if (sets.isNotEmpty()) database.setCompletionDao().upsertAll(sets)
                    if (weights.isNotEmpty()) database.weightLogDao().upsertAll(weights)
                }

                DataTransferSummary(programs.size, workouts.size, sets.size, weights.size)
            }
        }

    private fun ExerciseEntity.toJson() = buildJsonObject {
        put("id", id); put("name", name); put("nameEn", nameEn); put("targetMuscle", targetMuscle)
        put("category", category); put("setsDefault", setsDefault); put("repsDefault", repsDefault)
        put("description", description); put("imageUrl", imageUrl); put("sportType", sportType)
        put("trackingMode", trackingMode); putNullable("createdBy", createdBy)
    }

    private fun ProgramEntity.toJson() = buildJsonObject {
        put("id", id); put("name", name); put("type", type); put("isActive", isActive)
        put("createdAt", createdAt); putNullable("contentHash", contentHash)
        putNullable("appliedFromSharedId", appliedFromSharedId)
    }

    private fun ProgramDayEntity.toJson() = buildJsonObject {
        put("id", id); put("programId", programId); put("dayIndex", dayIndex); put("title", title)
        put("isRestDay", isRestDay); put("notes", notes)
    }

    private fun ProgramExerciseEntity.toJson() = buildJsonObject {
        put("id", id); put("programDayId", programDayId); put("exerciseId", exerciseId)
        put("sets", sets); put("reps", reps); put("weightKg", weightKg); put("restSeconds", restSeconds)
        put("orderIndex", orderIndex); putNullable("targetDurationSeconds", targetDurationSeconds)
        putNullable("targetDistanceMeters", targetDistanceMeters); putNullable("targetElevationMeters", targetElevationMeters)
        putNullable("targetInclinePercent", targetInclinePercent); put("section", section); put("notes", notes)
        putNullable("groupId", groupId); put("groupType", groupType); put("groupLabel", groupLabel)
        putNullable("groupRounds", groupRounds); putNullable("groupRestSeconds", groupRestSeconds)
    }

    private fun WorkoutLogEntity.toJson() = buildJsonObject {
        put("id", id); putNullable("programDayId", programDayId); put("date", date)
        putNullable("startedAt", startedAt); putNullable("finishedAt", finishedAt)
    }

    private fun ExerciseLogEntity.toJson() = buildJsonObject {
        put("id", id); put("workoutLogId", workoutLogId); put("exerciseId", exerciseId)
        put("setsCompleted", setsCompleted); put("repsCompleted", repsCompleted)
        put("isCompleted", isCompleted); put("durationSeconds", durationSeconds)
    }

    private fun SetCompletionEntity.toJson() = buildJsonObject {
        put("exerciseId", exerciseId); put("programDayId", programDayId); put("setIndex", setIndex); put("date", date)
        putNullable("weightKg", weightKg); putNullable("repsActual", repsActual); putNullable("durationSeconds", durationSeconds)
        putNullable("distanceMeters", distanceMeters); putNullable("elevationMeters", elevationMeters)
        putNullable("inclinePercent", inclinePercent); put("deleted", deleted); put("updatedAtMs", updatedAtMs)
    }

    private fun WeightLogEntity.toJson() = buildJsonObject {
        put("id", id); put("weightKg", weightKg); put("note", note); put("recordedAt", recordedAt)
    }

    private fun JsonObject.toExercise(userId: String) = ExerciseEntity(
        id = string("id"), name = string("name"), nameEn = stringOrNull("nameEn") ?: "",
        targetMuscle = string("targetMuscle"), category = string("category"), setsDefault = int("setsDefault"),
        repsDefault = int("repsDefault"), description = stringOrNull("description") ?: "",
        imageUrl = stringOrNull("imageUrl") ?: "", sportType = stringOrNull("sportType") ?: "",
        trackingMode = stringOrNull("trackingMode") ?: "", createdBy = stringOrNull("createdBy")?.let { userId }
    )

    private fun JsonObject.toProgram(userId: String) = ProgramEntity(
        id = string("id"), userId = userId, name = string("name"), type = string("type"),
        isActive = boolean("isActive"), createdAt = stringOrNull("createdAt") ?: "",
        contentHash = stringOrNull("contentHash"), appliedFromSharedId = stringOrNull("appliedFromSharedId")
    )

    private fun JsonObject.toProgramDay() = ProgramDayEntity(
        id = string("id"), programId = string("programId"), dayIndex = int("dayIndex"), title = string("title"),
        isRestDay = boolean("isRestDay"), notes = stringOrNull("notes") ?: ""
    )

    private fun JsonObject.toProgramExercise() = ProgramExerciseEntity(
        id = string("id"), programDayId = string("programDayId"), exerciseId = string("exerciseId"),
        sets = int("sets"), reps = int("reps"), weightKg = floatOrNull("weightKg") ?: 0f,
        restSeconds = intOrNull("restSeconds") ?: 90, orderIndex = intOrNull("orderIndex") ?: 0,
        targetDurationSeconds = intOrNull("targetDurationSeconds"), targetDistanceMeters = floatOrNull("targetDistanceMeters"),
        targetElevationMeters = floatOrNull("targetElevationMeters"), targetInclinePercent = floatOrNull("targetInclinePercent"),
        section = stringOrNull("section") ?: "", notes = stringOrNull("notes") ?: "", groupId = stringOrNull("groupId"),
        groupType = stringOrNull("groupType") ?: "straight", groupLabel = stringOrNull("groupLabel") ?: "",
        groupRounds = intOrNull("groupRounds"), groupRestSeconds = intOrNull("groupRestSeconds")
    )

    private fun JsonObject.toWorkout(userId: String) = WorkoutLogEntity(
        id = string("id"), userId = userId, programDayId = stringOrNull("programDayId"), date = string("date"),
        startedAt = stringOrNull("startedAt"), finishedAt = stringOrNull("finishedAt"), synced = false
    )

    private fun JsonObject.toExerciseLog() = ExerciseLogEntity(
        id = string("id"), workoutLogId = string("workoutLogId"), exerciseId = string("exerciseId"),
        setsCompleted = int("setsCompleted"), repsCompleted = int("repsCompleted"),
        isCompleted = booleanOrNull("isCompleted") ?: true, durationSeconds = intOrNull("durationSeconds") ?: 0, synced = false
    )

    private fun JsonObject.toSetRecord(userId: String) = SetCompletionEntity(
        userId = userId, exerciseId = string("exerciseId"), programDayId = string("programDayId"),
        setIndex = int("setIndex"), date = string("date"), weightKg = floatOrNull("weightKg"),
        repsActual = intOrNull("repsActual"), durationSeconds = intOrNull("durationSeconds"),
        distanceMeters = floatOrNull("distanceMeters"), elevationMeters = floatOrNull("elevationMeters"),
        inclinePercent = floatOrNull("inclinePercent"), synced = false, dirty = true,
        deleted = booleanOrNull("deleted") ?: false, updatedAtMs = longOrNull("updatedAtMs") ?: System.currentTimeMillis()
    )

    private fun JsonObject.toWeight(userId: String) = WeightLogEntity(
        id = string("id"), userId = userId, weightKg = double("weightKg"),
        note = stringOrNull("note") ?: "", recordedAt = string("recordedAt"), synced = false
    )

    private fun JsonObject.array(key: String): JsonArray = this[key]?.jsonArray ?: JsonArray(emptyList())
    private fun JsonObject.obj(key: String): JsonObject = this[key]?.jsonObject ?: error("Eksik alan: $key")
    private fun JsonObject.objectOrNull(key: String): JsonObject? = (this[key] as? JsonObject)
    private fun JsonObject.string(key: String): String = stringOrNull(key) ?: error("Eksik alan: $key")
    private fun JsonObject.stringOrNull(key: String): String? = this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull
    private fun JsonObject.int(key: String): Int = intOrNull(key) ?: error("Geçersiz alan: $key")
    private fun JsonObject.intOrNull(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull
    private fun JsonObject.longOrNull(key: String): Long? = this[key]?.jsonPrimitive?.longOrNull
    private fun JsonObject.floatOrNull(key: String): Float? = this[key]?.jsonPrimitive?.floatOrNull
    private fun JsonObject.double(key: String): Double = doubleOrNull(key) ?: error("Geçersiz alan: $key")
    private fun JsonObject.doubleOrNull(key: String): Double? = this[key]?.jsonPrimitive?.doubleOrNull
    private fun JsonObject.boolean(key: String): Boolean = booleanOrNull(key) ?: error("Geçersiz alan: $key")
    private fun JsonObject.booleanOrNull(key: String): Boolean? = this[key]?.jsonPrimitive?.booleanOrNull

    private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: String?) =
        put(key, value?.let(::JsonPrimitive) ?: JsonNull)
    private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: Int?) =
        put(key, value?.let(::JsonPrimitive) ?: JsonNull)
    private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: Float?) =
        put(key, value?.let(::JsonPrimitive) ?: JsonNull)
    private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(key: String, value: Double?) =
        put(key, value?.let(::JsonPrimitive) ?: JsonNull)

    private companion object {
        const val FORMAT = "profitness-backup"
        const val VERSION = 1
        const val MAX_FILE_BYTES = 10 * 1024 * 1024
    }
}
