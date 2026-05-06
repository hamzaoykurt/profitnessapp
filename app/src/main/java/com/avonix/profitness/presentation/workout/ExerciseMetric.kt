package com.avonix.profitness.presentation.workout

enum class ExerciseMetric {
    Strength,
    Duration,
    DurationDistance
}

fun classifyExerciseMetric(
    category: String,
    name: String,
    target: String,
    reps: String
): ExerciseMetric {
    val haystack = listOf(category, name, target, reps)
        .joinToString(" ")
        .lowercase()
        .normalizeTurkishAscii()

    val repsLooksTimed = reps.lowercase().normalizeTurkishAscii().let { value ->
        listOf("sn", "sec", "second", "saniye", " dk", "min", "minute", "dakika").any { it in value }
    }

    val durationOnlyKeywords = listOf(
        "plank", "wall sit", "hollow hold", "dead hang", "mobility", "yoga", "pilates",
        "stretch", "esneme", "walking lunge"
    )
    if (durationOnlyKeywords.any { it in haystack } || repsLooksTimed) {
        return ExerciseMetric.Duration
    }

    val distanceKeywords = listOf(
        "kardiyo", "cardio", "kos", "run", "jog", "treadmill", "bisiklet", "bike",
        "cycle", "cycling", "yuz", "swim", "yuruyus", "yurume", "walk",
        "kurek", "rowing", "rower", "ergometer", "elliptical", "tempo", "interval", "hiit"
    )
    if (distanceKeywords.any { it in haystack }) {
        return ExerciseMetric.DurationDistance
    }

    val strengthKeywords = listOf(
        "strength", "makine", "machine", "cable", "serbest agirlik", "free weight",
        "dumbbell", "barbell", "plate loaded", "smith", "pulldown", "lat", "row",
        "press", "bench", "fly", "curl", "pushdown", "pullover", "raise", "extension",
        "squat", "deadlift", "rdl", "hip thrust", "leg press", "calf", "crunch"
    )
    if (strengthKeywords.any { it in haystack }) {
        return ExerciseMetric.Strength
    }

    return ExerciseMetric.Strength
}

private fun String.normalizeTurkishAscii(): String =
    replace('\u0131', 'i')
        .replace('\u011f', 'g')
        .replace('\u00fc', 'u')
        .replace('\u015f', 's')
        .replace('\u00f6', 'o')
        .replace('\u00e7', 'c')
