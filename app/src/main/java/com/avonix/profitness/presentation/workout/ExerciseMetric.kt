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
    IndoorFootball("indoor_football", "Halı Saha"),
    Volleyball("volleyball", "Voleybol"),
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
            IndoorFootball,
            Volleyball,
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
    val supportsReps: Boolean = false,
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
    val haystack = listOf(category, name, target, reps)
        .joinToString(" ")
        .lowercase()
        .normalizeTurkishAscii()
    val repBasedStrengthMovement = repBasedStrengthKeywords.any { it in haystack }
    val explicitSport = SportType.fromRaw(sportTypeRaw).takeUnless {
        sportTypeRaw.isNullOrBlank() || repBasedStrengthMovement
    }
    val sportType = explicitSport ?: classifySportType(category, name, target, reps)
    val normalizedReps = reps.lowercase().normalizeTurkishAscii()
    val repsLooksTimed = repsLooksTimed(normalizedReps)
    val isTimedHold = timedHoldKeywords.any { it in haystack }
    val explicitMetric = when (trackingModeRaw) {
        "duration", "duration_reps", "duration_distance", "duration_distance_elevation" -> if (repBasedStrengthMovement && !repsLooksTimed && !isTimedHold) {
            ExerciseMetric.Strength
        } else {
            when (trackingModeRaw) {
                "duration" -> ExerciseMetric.Duration
                "duration_reps" -> ExerciseMetric.Duration
                else -> ExerciseMetric.DurationDistance
            }
        }
        "strength" -> if (repsLooksTimed || isTimedHold) ExerciseMetric.Duration else ExerciseMetric.Strength
        else -> null
    }
    val metric = explicitMetric ?: when (sportType) {
        SportType.Strength -> {
            if (repsLooksTimed || isTimedHold) ExerciseMetric.Duration else ExerciseMetric.Strength
        }
        SportType.YogaPilates,
        SportType.Boxing,
        SportType.JumpRopeHiit,
        SportType.Football,
        SportType.IndoorFootball,
        SportType.Volleyball,
        SportType.BasketballTennis -> ExerciseMetric.Duration
        else -> ExerciseMetric.DurationDistance
    }

    return ActivityTrackingSpec(
        sportType = sportType,
        metric = metric,
        supportsDistance = metric == ExerciseMetric.DurationDistance,
        supportsIncline = sportType == SportType.Cycling || sportType == SportType.Running || sportType == SportType.WalkingHiking,
        supportsElevation = sportType == SportType.Running || sportType == SportType.Cycling || sportType == SportType.WalkingHiking,
        supportsReps = trackingModeRaw == "duration_reps" ||
            listOf("jump rope", "ip atlama", "double under").any { it in haystack },
        primaryUnit = when (metric) {
            ExerciseMetric.Strength -> "set"
            ExerciseMetric.Duration -> if (trackingModeRaw == "duration_reps") "adet" else "dk"
            ExerciseMetric.DurationDistance -> "m"
        }
    )
}

fun isDurationSetExercise(
    category: String,
    name: String,
    target: String,
    reps: String,
    sets: Int,
    sportTypeRaw: String? = null,
    trackingModeRaw: String? = null
): Boolean {
    if (sets <= 1) return false
    val metric = classifyExerciseMetric(category, name, target, reps, sportTypeRaw, trackingModeRaw)
    if (metric != ExerciseMetric.Duration) return false
    if (trackingModeRaw == "duration_reps") return false
    val haystack = listOf(category, name, target, reps)
        .joinToString(" ")
        .lowercase()
        .normalizeTurkishAscii()
    return repsLooksTimed(reps.lowercase().normalizeTurkishAscii()) ||
        timedSetKeywords.any { it in haystack }
}

fun defaultDurationSecondsForExercise(
    category: String,
    name: String,
    target: String,
    reps: Int,
    sportTypeRaw: String? = null,
    trackingModeRaw: String? = null
): Int {
    val haystack = listOf(category, name, target)
        .joinToString(" ")
        .lowercase()
        .normalizeTurkishAscii()
    return when {
        trackingModeRaw == "duration_reps" || listOf("jump rope", "ip atlama", "double under").any { it in haystack } -> 10 * 60
        timedSetKeywords.any { it in haystack } -> reps.takeIf { it in 5..600 } ?: 60
        distanceSetLikeKeywords.any { it in haystack } -> 60
        reps in 5..180 -> reps * 60
        else -> 20 * 60
    }
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
    if (listOf("jump rope", "ip atlama").any { it in haystack }) return SportType.JumpRopeHiit
    if (listOf("yoga", "pilates", "mobility", "stretch", "esneme").any { it in haystack }) return SportType.YogaPilates
    if (listOf("boxing", "boks", "mma", "shadow boxing", "kickbox").any { it in haystack }) return SportType.Boxing
    if (listOf("hali saha", "halisaha", "futsal").any { it in haystack }) return SportType.IndoorFootball
    if (listOf("football", "soccer", "futbol").any { it in haystack }) return SportType.Football
    if (listOf("voleybol", "volleyball").any { it in haystack }) return SportType.Volleyball
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
    "plank", "side plank", "wall sit", "hollow hold", "hollow body hold", "dead hang",
    "mobility", "yoga", "pilates", "stretch", "esneme", "pose"
)

private val timedSetKeywords = listOf(
    "plank", "side plank", "wall sit", "hollow hold", "hollow body hold", "dead hang",
    "stretch", "esneme", "pose", "jump rope", "ip atlama"
)

private val distanceSetLikeKeywords = listOf(
    "farmer walk", "farmer carry", "suitcase carry", "carry", "sled push", "sled pull", "shuttle run"
)

private val repBasedStrengthKeywords = listOf(
    "lunge", "split squat", "crunch", "leg raise", "russian twist", "ab wheel",
    "burpee", "mountain climber", "box jump", "jump squat", "push-up", "push up",
    "calf raise", "cat-cow", "bird dog", "glute bridge", "medicine ball slam"
)

private fun repsLooksTimed(reps: String): Boolean =
    listOf("sn", "sec", "second", "saniye", " dk", "min", "minute", "dakika")
        .any { it in reps }

private fun String.normalizeTurkishAscii(): String =
    replace('\u0131', 'i')
        .replace('\u011f', 'g')
        .replace('\u00fc', 'u')
        .replace('\u015f', 's')
        .replace('\u00f6', 'o')
        .replace('\u00e7', 'c')
