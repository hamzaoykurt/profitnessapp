package com.avonix.profitness.presentation.workout

enum class ExerciseMetric {
    Strength,
    Duration,
    DurationDistance
}

enum class SportType(val raw: String, val label: String) {
    Strength("strength", "Kuvvet"),
    Running("running", "Koşu"),
    Cycling("cycling", "Bisiklet"),
    Swimming("swimming", "Yüzme"),
    Rowing("rowing", "Kürek"),
    WalkingHiking("walking_hiking", "Yürüyüş"),
    JumpRopeHiit("jump_rope_hiit", "HIIT"),
    YogaPilates("yoga_pilates", "Yoga"),
    Boxing("boxing", "Boks"),
    Football("football", "Futbol"),
    BasketballTennis("basketball_tennis", "Basketbol/Tenis");

    companion object {
        fun fromRaw(raw: String?): SportType =
            entries.firstOrNull { it.raw == raw } ?: Strength

        val challengeChoices = listOf(
            Running,
            Cycling,
            Swimming,
            Rowing,
            WalkingHiking,
            JumpRopeHiit,
            YogaPilates,
            Boxing,
            Football,
            BasketballTennis
        )
    }
}

data class ActivityTrackingSpec(
    val sportType: SportType,
    val metric: ExerciseMetric,
    val supportsDistance: Boolean = false,
    val supportsIncline: Boolean = false,
    val supportsElevation: Boolean = false,
    val primaryUnit: String = ""
) {
    val isActivityBased: Boolean get() = metric != ExerciseMetric.Strength
}

fun classifyExerciseMetric(
    category: String,
    name: String,
    target: String,
    reps: String,
    sportTypeRaw: String? = null,
    trackingModeRaw: String? = null
): ExerciseMetric = activityTrackingSpec(category, name, target, reps, sportTypeRaw, trackingModeRaw).metric

fun activityTrackingSpec(
    category: String,
    name: String,
    target: String,
    reps: String,
    sportTypeRaw: String? = null,
    trackingModeRaw: String? = null
): ActivityTrackingSpec {
    val explicitSport = SportType.fromRaw(sportTypeRaw).takeUnless { sportTypeRaw.isNullOrBlank() }
    val sportType = explicitSport ?: classifySportType(category, name, target, reps)
    val explicitMetric = when (trackingModeRaw) {
        "duration" -> ExerciseMetric.Duration
        "duration_distance", "duration_distance_elevation" -> ExerciseMetric.DurationDistance
        "strength" -> ExerciseMetric.Strength
        else -> null
    }
    val metric = explicitMetric ?: when (sportType) {
        SportType.Strength -> {
            val repsLooksTimed = reps.lowercase().normalizeTurkishAscii().let { value ->
                listOf("sn", "sec", "second", "saniye", " dk", "min", "minute", "dakika").any { it in value }
            }
            if (repsLooksTimed || timedHoldKeywords.any {
                    it in listOf(category, name, target).joinToString(" ").lowercase().normalizeTurkishAscii()
                }
            ) ExerciseMetric.Duration else ExerciseMetric.Strength
        }
        SportType.YogaPilates, SportType.Boxing, SportType.JumpRopeHiit -> ExerciseMetric.Duration
        else -> ExerciseMetric.DurationDistance
    }

    return ActivityTrackingSpec(
        sportType = sportType,
        metric = metric,
        supportsDistance = metric == ExerciseMetric.DurationDistance,
        supportsIncline = sportType == SportType.Cycling || sportType == SportType.Running || sportType == SportType.WalkingHiking,
        supportsElevation = sportType == SportType.Running || sportType == SportType.Cycling || sportType == SportType.WalkingHiking,
        primaryUnit = when (metric) {
            ExerciseMetric.Strength -> "set"
            ExerciseMetric.Duration -> "dk"
            ExerciseMetric.DurationDistance -> "m"
        }
    )
}

fun classifySportType(
    category: String,
    name: String,
    target: String,
    reps: String = ""
): SportType {
    val haystack = listOf(category, name, target, reps)
        .joinToString(" ")
        .lowercase()
        .normalizeTurkishAscii()

    if (listOf("bisiklet", "bike", "cycle", "cycling", "spinning").any { it in haystack }) return SportType.Cycling
    if (listOf("yuz", "swim", "swimming", "havuz").any { it in haystack }) return SportType.Swimming
    if (listOf("kurek", "rowing", "rower", "ergometer", "erg").any { it in haystack }) return SportType.Rowing
    if (listOf("kos", "run", "jog", "treadmill", "tempo").any { it in haystack }) return SportType.Running
    if (listOf("yuruyus", "yurume", "walk", "hike", "hiking", "trekking").any { it in haystack }) return SportType.WalkingHiking
    if (listOf("jump rope", "ip atlama", "hiit", "interval", "burpee", "mountain climber").any { it in haystack }) return SportType.JumpRopeHiit
    if (listOf("yoga", "pilates", "mobility", "stretch", "esneme").any { it in haystack }) return SportType.YogaPilates
    if (listOf("boxing", "boks", "mma", "shadow boxing", "kickbox").any { it in haystack }) return SportType.Boxing
    if (listOf("football", "soccer", "futbol", "hali saha").any { it in haystack }) return SportType.Football
    if (listOf("basket", "basketball", "tennis", "tenis").any { it in haystack }) return SportType.BasketballTennis

    val strengthKeywords = listOf(
        "strength", "makine", "machine", "cable", "serbest agirlik", "free weight",
        "dumbbell", "barbell", "plate loaded", "smith", "pulldown", "lat", "row",
        "press", "bench", "fly", "curl", "pushdown", "pullover", "raise", "extension",
        "squat", "deadlift", "rdl", "hip thrust", "leg press", "calf", "crunch"
    )
    if (strengthKeywords.any { it in haystack }) {
        return SportType.Strength
    }

    if (listOf("kardiyo", "cardio", "elliptical").any { it in haystack }) return SportType.Running

    return SportType.Strength
}

private val timedHoldKeywords = listOf(
    "plank", "wall sit", "hollow hold", "dead hang", "mobility", "yoga", "pilates",
    "stretch", "esneme", "walking lunge"
)

private fun String.normalizeTurkishAscii(): String =
    replace('\u0131', 'i')
        .replace('\u011f', 'g')
        .replace('\u00fc', 'u')
        .replace('\u015f', 's')
        .replace('\u00f6', 'o')
        .replace('\u00e7', 'c')
