package com.avonix.profitness.presentation.profile

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import com.avonix.profitness.core.BaseViewModel
import com.avonix.profitness.data.leaderboard.LeaderboardRepository
import com.avonix.profitness.data.profile.ProfileRepository
import com.avonix.profitness.data.profile.dto.AchievementDto
import com.avonix.profitness.data.store.UserPlan
import com.avonix.profitness.data.store.UserPlanRepository
import com.avonix.profitness.data.workout.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ── Rank Sistemi ──────────────────────────────────────────────────────────────

/** DB'deki temiz URL + avatar_updated_at birleştirerek Coil'e cache-busting URL üretir. */
fun buildAvatarDisplayUrl(avatarUrl: String?, updatedAt: Long?): String {
    if (avatarUrl.isNullOrBlank()) return "🏋️"
    if (!avatarUrl.startsWith("http")) return avatarUrl
    return if (updatedAt != null) "$avatarUrl?t=$updatedAt" else avatarUrl
}

fun computeRank(xp: Int): String = when {
    xp >= 50000 -> "Diamond"
    xp >= 15000 -> "Platinum"
    xp >= 5000  -> "Gold"
    xp >= 1000  -> "Silver"
    else        -> "Bronze"
}

// ── Achievement Modeli (UI için) ──────────────────────────────────────────────

@Stable
data class AchievementUiModel(
    val key        : String,
    val name       : String,
    val description: String,
    val icon       : String,
    val category   : String,
    val threshold  : Int,
    val isUnlocked : Boolean
)

// ── State ─────────────────────────────────────────────────────────────────────

@Stable
data class ProfileState(
    val displayName          : String               = "",
    val avatar               : String               = "🏋️",
    val rank                 : String               = "Bronze",
    val level                : Int                  = 1,
    val xp                   : Int                  = 0,
    val xpPerLevel           : Int                  = 1000,
    val currentStreak        : Int                  = 0,
    val longestStreak        : Int                  = 0,
    val streakRankPosition   : Long                 = 0L,
    val streakRankTotalUsers : Long                 = 0L,
    val totalWorkouts        : Int                  = 0,
    val totalExercises       : Int                  = 0,
    val totalDurationSeconds : Int                  = 0,
    val totalDistanceMeters  : Float                = 0f,
    /** 7 eleman — Pazartesi=0 … Pazar=6; 0.0–1.0 oranında tamamlanma */
    val weeklyActivity       : ImmutableList<Float>  = List(7) { 0f }.toImmutableList(),
    /** Son 13 haftalık antrenman sayıları (grafik için, en eskiden en yeniye) */
    val weeklyWorkoutCounts  : ImmutableList<Int>   = List(13) { 0 }.toImmutableList(),
    /** Başarımlar — tüm tanımlı başarımlar unlocked durumlarıyla */
    val achievements         : ImmutableList<AchievementUiModel> = emptyList<AchievementUiModel>().toImmutableList(),
    val fitnessGoal          : String               = "",
    val heightCm             : Double               = 0.0,
    val weightKg             : Double               = 0.0,
    val gender               : String               = "",
    val bmi                  : Double               = 0.0,
    val bodyFatPct           : Double               = 0.0,
    val birthDate            : String               = "",
    val totalCalories        : Int                  = 0,
    val isLoading            : Boolean              = true,
    val isSaving             : Boolean              = false,
    val userPlan             : UserPlan             = UserPlan.FREE,
    val aiCredits            : Int                  = UserPlanRepository.INITIAL_CREDITS_PLACEHOLDER
)

// ── Events ────────────────────────────────────────────────────────────────────

sealed class ProfileEvent {
    data class ShowSnackbar(val message: String) : ProfileEvent()
    data class AchievementUnlocked(val name: String, val icon: String, val description: String = "") : ProfileEvent()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository : ProfileRepository,
    private val leaderboardRepository: LeaderboardRepository,
    private val planRepository    : UserPlanRepository,
    private val workoutRepository : WorkoutRepository,
    private val supabase          : SupabaseClient
) : BaseViewModel<ProfileState, ProfileEvent>(ProfileState()) {

    private var lastLoadMs = 0L
    private var isInitialized = false

    init {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                planRepository.planFlow,
                planRepository.creditsFlow
            ) { plan, credits -> plan to credits }
                .collect { (plan, credits) ->
                    updateState { it.copy(userPlan = plan, aiCredits = credits) }
                }
        }
    }

    fun initLoad() {
        if (isInitialized) return
        isInitialized = true
        loadProfile()
    }

    /** Tab geçişleri ve ON_RESUME için — 5 dakika geçmediyse ve veri varsa atla */
    fun reloadIfStale() {
        if (System.currentTimeMillis() - lastLoadMs < 5 * 60_000L &&
            uiState.value.displayName.isNotBlank()) return
        loadProfile()
    }

    fun refreshNow() {
        lastLoadMs = 0L
        profileRepository.invalidateStatsCache()
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            // İlk açılışta loading göster; tekrar yükleme varsa eski veri görünür kalsın
            updateState { it.copy(isLoading = uiState.value.displayName.isBlank()) }

            val userId = supabase.auth.currentUserOrNull()?.id ?: run {
                updateState { it.copy(isLoading = false) }
                return@launch
            }

            // Paralel fetch
            val monday    = LocalDate.now().with(DayOfWeek.MONDAY)
            val mondayStr = monday.format(DateTimeFormatter.ISO_LOCAL_DATE)

            val profileDef      = async { profileRepository.getProfile(userId) }
            val statsDef        = async { profileRepository.getUserStats(userId) }
            val ratiosDef       = async { profileRepository.getWeeklyCompletionRatios(userId, mondayStr) }
            val workoutDatesDef = async {
                val from = LocalDate.now().minusWeeks(13).format(DateTimeFormatter.ISO_LOCAL_DATE)
                profileRepository.getWorkoutDates(userId, from)
            }
            val allAchDef       = async { profileRepository.getAllAchievements() }
            val unlockedAchDef  = async { profileRepository.getUnlockedAchievementKeys(userId) }
            val localStreakDef   = async { workoutRepository.getStreak(userId) }
            val trackedDef       = async { workoutRepository.getTrackedExerciseSummaries(userId) }

            val profile          = profileDef.await().getOrNull()
            val stats            = statsDef.await().getOrNull()
            val completionRatios = ratiosDef.await().getOrDefault(emptyMap())
            val workoutDates     = workoutDatesDef.await().getOrNull().orEmpty()
            val allAch           = allAchDef.await().getOrNull().orEmpty()
            val unlockedKeys     = unlockedAchDef.await().getOrNull().orEmpty()
            val localStreak      = localStreakDef.await().getOrElse { stats?.current_streak ?: 0 }
            val currentStreak    = maxOf(localStreak, stats?.current_streak ?: 0)
            val trackedSummaries = trackedDef.await().getOrNull().orEmpty()
            val streakRank       = leaderboardRepository.getMyStreakRank().getOrNull()

            // Bu haftanın aktif günleri — her gün için orantılı tamamlanma (0.0 – 1.0)
            // workout_logs.program_day_id üzerinden hesaplanıyor, aktif program seçimine bağlı değil
            val todayIdx = LocalDate.now().dayOfWeek.value - 1   // 0=Pzt, 6=Paz
            val weeklyActivity = (0..6).map { i ->
                if (i > todayIdx) return@map 0f   // gelecekteki günler
                val date = monday.plusDays(i.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
                completionRatios[date] ?: 0f
            }.toImmutableList()

            // 13 haftalık antrenman grafiği
            val weeklyWorkoutCounts = buildWeeklyChart(workoutDates)

            // Başarımlar UI modeline çevir
            val achievements = allAch.map { ach ->
                AchievementUiModel(
                    key        = ach.key,
                    name       = ach.name,
                    description= ach.description ?: "",
                    icon       = ach.icon ?: "🏆",
                    category   = ach.category,
                    threshold  = ach.threshold,
                    isUnlocked = ach.key in unlockedKeys
                )
            }.toImmutableList()

            val lvl          = stats?.level ?: 1
            val xpForNext    = 500   // Her seviye sabit 500 XP
            val totalWorkouts= stats?.total_workouts ?: 0
            val totalExercises= stats?.total_exercises ?: 0
            val totalDurationSeconds = trackedSummaries.sumOf { it.totalDurationSeconds }
            val totalDistanceMeters = trackedSummaries.sumOf { it.totalDistanceMeters.toDouble() }.toFloat()
            val currentXp    = stats?.xp ?: 0

            // BMI ve vücut yağı hesapla
            val h = profile?.height_cm ?: 0.0
            val w = profile?.weight_kg ?: 0.0
            val bmi = if (h > 0 && w > 0) w / ((h / 100.0) * (h / 100.0)) else 0.0
            val birthDate = profile?.birth_date ?: ""
            val age = ageFromBirthDate(birthDate) ?: 25

            val isMale = (profile?.gender ?: "").lowercase() == "male"
            val bodyFat = if (bmi > 0) {
                val raw = 1.2 * bmi + 0.23 * age + (if (isMale) -10.8 else 0.0) - 5.4
                raw.coerceIn(3.0, 60.0)
            } else 0.0

            // Rank hesapla ve gerekirse güncelle
            val computedRank  = computeRank(currentXp)
            val storedRank    = profile?.current_rank ?: "Bronze"
            if (computedRank != storedRank) {
                profileRepository.updateRank(userId, computedRank)
            }

            lastLoadMs = System.currentTimeMillis()
            updateState {
                it.copy(
                    displayName         = profile?.display_name.orEmpty(),
                    avatar              = buildAvatarDisplayUrl(profile?.avatar_url, profile?.avatar_updated_at),
                    rank                = computedRank,
                    level               = lvl,
                    xp                  = currentXp,
                    xpPerLevel          = xpForNext,
                    currentStreak       = currentStreak,
                    longestStreak       = stats?.longest_streak ?: 0,
                    streakRankPosition  = streakRank?.rank_position ?: 0L,
                    streakRankTotalUsers= streakRank?.total_users ?: 0L,
                    totalWorkouts       = totalWorkouts,
                    totalExercises      = totalExercises,
                    totalDurationSeconds= totalDurationSeconds,
                    totalDistanceMeters = totalDistanceMeters,
                    fitnessGoal         = profile?.fitness_goal.orEmpty(),
                    heightCm            = profile?.height_cm ?: 0.0,
                    weightKg            = profile?.weight_kg ?: 0.0,
                    gender              = profile?.gender.orEmpty(),
                    bmi                 = bmi,
                    bodyFatPct          = bodyFat,
                    birthDate           = birthDate,
                    weeklyActivity      = weeklyActivity,
                    weeklyWorkoutCounts = weeklyWorkoutCounts,
                    achievements        = achievements,
                    isLoading           = false
                )
            }

            // Achievement kontrolü
            checkAchievements(userId, currentXp, lvl, totalWorkouts, totalExercises, currentStreak, unlockedKeys, allAch)
        }
    }

    /** Profil bilgilerini DB'ye kaydeder ve state'i günceller. onComplete kayıt tamamlanınca çağrılır. */
    fun updateProfile(
        displayName: String, avatar: String, fitnessGoal: String,
        heightCm: Double, weightKg: Double, gender: String,
        birthDate: String = "", onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            updateState { it.copy(isSaving = true) }

            val userId = supabase.auth.currentUserOrNull()?.id ?: run {
                updateState { it.copy(isSaving = false) }
                onComplete()
                return@launch
            }

            val result = profileRepository.updateProfile(userId, displayName, avatar, fitnessGoal, heightCm, weightKg, gender, birthDate)
            if (result.isSuccess) {
                val newBmi = if (heightCm > 0 && weightKg > 0) weightKg / ((heightCm / 100.0) * (heightCm / 100.0)) else 0.0
                val isMale2 = gender.lowercase() == "male"
                val age = ageFromBirthDate(birthDate) ?: 25
                val newBodyFat = if (newBmi > 0) {
                    (1.2 * newBmi + 0.23 * age + (if (isMale2) -10.8 else 0.0) - 5.4).coerceIn(3.0, 60.0)
                } else 0.0
                updateState { it.copy(displayName = displayName, avatar = avatar, fitnessGoal = fitnessGoal, heightCm = heightCm, weightKg = weightKg, gender = gender, birthDate = birthDate, bmi = newBmi, bodyFatPct = newBodyFat, isSaving = false) }
                sendEvent(ProfileEvent.ShowSnackbar("Profil güncellendi"))
            } else {
                updateState { it.copy(isSaving = false) }
                sendEvent(ProfileEvent.ShowSnackbar("Güncelleme başarısız"))
            }
            onComplete()
        }
    }

    fun uploadPhoto(imageBytes: ByteArray) {
        viewModelScope.launch {
            val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
            updateState { it.copy(isSaving = true) }
            val result = profileRepository.uploadProfilePhoto(userId, imageBytes)
            if (result.isSuccess) {
                val url = result.getOrThrow()
                updateState { it.copy(avatar = url, isSaving = false) }
                sendEvent(ProfileEvent.ShowSnackbar("Fotoğraf yüklendi"))
            } else {
                updateState { it.copy(isSaving = false) }
                sendEvent(ProfileEvent.ShowSnackbar("Fotoğraf yüklenemedi"))
            }
        }
    }

    // ── Yardımcı: 13 haftalık grafik ──────────────────────────────────────────

    private fun ageFromBirthDate(birthDate: String): Int? =
        runCatching {
            val date = LocalDate.parse(birthDate, DateTimeFormatter.ISO_LOCAL_DATE)
            Period.between(date, LocalDate.now()).years.takeIf { it in 8..100 }
        }.getOrNull()

    private fun buildWeeklyChart(workoutDates: List<String>): ImmutableList<Int> {
        val today  = LocalDate.now()
        val counts = MutableList(13) { 0 }
        workoutDates.forEach { dateStr ->
            runCatching {
                val date     = LocalDate.parse(dateStr)
                val weeksAgo = java.time.temporal.ChronoUnit.WEEKS.between(
                    date.with(DayOfWeek.MONDAY), today.with(DayOfWeek.MONDAY)
                ).toInt()
                if (weeksAgo in 0..12) counts[12 - weeksAgo]++
            }
        }
        return counts.toImmutableList()
    }

    // ── FAZ 5C: Achievement Kontrol ───────────────────────────────────────────

    private suspend fun checkAchievements(
        userId          : String,
        xp              : Int,
        level           : Int,
        workouts        : Int,
        totalExercises  : Int,
        streak          : Int,
        unlocked        : Set<String>,
        allAch          : List<AchievementDto>
    ) {
        val toCheck = mapOf(
            "xp"              to xp,
            "level"           to level,
            "volume"          to workouts,
            "streak"          to streak,
            "milestone"       to workouts,
            "total_exercises" to totalExercises
        )

        val achievementsToUnlock = allAch.mapNotNull { ach ->
            val value = toCheck[ach.category] ?: return@mapNotNull null
            ach.takeIf { it.key !in unlocked && value >= it.threshold }
        }

        if (achievementsToUnlock.isEmpty()) return

        val keysToUnlock = achievementsToUnlock.map { it.key }
        val result = profileRepository.unlockAchievements(userId, keysToUnlock)
        if (result.isSuccess) {
            achievementsToUnlock.forEach { ach ->
                sendEvent(ProfileEvent.AchievementUnlocked(ach.name, ach.icon ?: "🏆", ach.description ?: ""))
            }
            updateState { st ->
                st.copy(
                    achievements = st.achievements.map { uiAch ->
                        if (uiAch.key in keysToUnlock) uiAch.copy(isUnlocked = true) else uiAch
                    }.toImmutableList()
                )
            }
        }
    }
}
