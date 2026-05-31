package com.avonix.profitness.core.theme

private data class LocalizedFitnessTerm(
    val english: String,
    val turkish: String,
    val aliases: Set<String> = emptySet()
)

private fun normalizeDisplayKey(value: String): String =
    value.trim()
        .lowercase()
        .replace('\u0131', 'i')
        .replace('\u011f', 'g')
        .replace('\u00fc', 'u')
        .replace('\u015f', 's')
        .replace('\u00f6', 'o')
        .replace('\u00e7', 'c')
        .replace(Regex("\\s+"), " ")

private val ExerciseNameTerms = listOf(
    LocalizedFitnessTerm("Squat", "Çömelme"),
    LocalizedFitnessTerm("Hack Squat", "Hack squat"),
    LocalizedFitnessTerm("Goblet Squat", "Goblet squat"),
    LocalizedFitnessTerm("Bulgarian Split Squat", "Bulgar split squat"),
    LocalizedFitnessTerm("Jump Squat", "Sıçramalı squat"),
    LocalizedFitnessTerm("Lunge", "Lunge"),
    LocalizedFitnessTerm("Leg Press", "Bacak press"),
    LocalizedFitnessTerm("Leg Curl", "Bacak curl"),
    LocalizedFitnessTerm("Seated Leg Curl", "Oturarak bacak curl"),
    LocalizedFitnessTerm("Leg Extension", "Bacak extension"),
    LocalizedFitnessTerm("Standing Calf Raise", "Ayakta calf raise"),
    LocalizedFitnessTerm("Seated Calf Raise", "Oturarak calf raise"),
    LocalizedFitnessTerm("Deadlift", "Deadlift"),
    LocalizedFitnessTerm("Romanian Deadlift", "Romanian deadlift"),
    LocalizedFitnessTerm("Stiff Leg Deadlift", "Stiff leg deadlift"),
    LocalizedFitnessTerm("Flat Bench Press", "Düz bench press"),
    LocalizedFitnessTerm("Bench Press", "Bench press"),
    LocalizedFitnessTerm("Incline Bench Press", "Eğimli bench press"),
    LocalizedFitnessTerm("Decline Bench Press", "Ters eğimli bench press"),
    LocalizedFitnessTerm("Dumbbell Bench Press", "Dumbbell bench press"),
    LocalizedFitnessTerm("Dumbbell Flat Fly", "Dumbbell fly"),
    LocalizedFitnessTerm("Incline Dumbbell Fly", "Eğimli dumbbell fly"),
    LocalizedFitnessTerm("Pec Deck Flye", "Pec deck fly"),
    LocalizedFitnessTerm("Cable Crossover", "Cable crossover"),
    LocalizedFitnessTerm("Chest Dips", "Göğüs dips"),
    LocalizedFitnessTerm("Push-Up", "Şınav", setOf("push up", "pushup", "sinav")),
    LocalizedFitnessTerm("Diamond Push-Up", "Diamond şınav"),
    LocalizedFitnessTerm("Pull-Up", "Barfiks", setOf("pull up", "pullup")),
    LocalizedFitnessTerm("Dips", "Dips"),
    LocalizedFitnessTerm("Row", "Row"),
    LocalizedFitnessTerm("Barbell Row", "Barbell row"),
    LocalizedFitnessTerm("Bent Over Barbell Row", "Bent over barbell row"),
    LocalizedFitnessTerm("Dumbbell Row", "Dumbbell row"),
    LocalizedFitnessTerm("Single Arm Dumbbell Row", "Tek kol dumbbell row"),
    LocalizedFitnessTerm("Seated Cable Row", "Seated cable row"),
    LocalizedFitnessTerm("Wide Grip Seated Row", "Geniş tutuş seated row"),
    LocalizedFitnessTerm("Wide Grip Cable Row", "Geniş tutuş cable row"),
    LocalizedFitnessTerm("T-Bar Row", "T-bar row"),
    LocalizedFitnessTerm("Machine Row", "Machine row"),
    LocalizedFitnessTerm("Chest Supported Row", "Göğüs destekli row"),
    LocalizedFitnessTerm("Inverted Row", "Inverted row"),
    LocalizedFitnessTerm("Lat Pulldown", "Lat pulldown"),
    LocalizedFitnessTerm("Hammer Grip Lat Pulldown", "Hammer tutuş lat pulldown"),
    LocalizedFitnessTerm("Hyperextension", "Hyperextension"),
    LocalizedFitnessTerm("Barbell Shoulder Press", "Barbell omuz press"),
    LocalizedFitnessTerm("Dumbbell Shoulder Press", "Dumbbell omuz press"),
    LocalizedFitnessTerm("Arnold Press", "Arnold press"),
    LocalizedFitnessTerm("Lateral Raise", "Yana açış"),
    LocalizedFitnessTerm("Cable Lateral Raise", "Cable yana açış"),
    LocalizedFitnessTerm("Front Raise", "Öne açış"),
    LocalizedFitnessTerm("Rear Delt Fly", "Arka omuz fly"),
    LocalizedFitnessTerm("Upright Row", "Upright row"),
    LocalizedFitnessTerm("Shrug", "Shrug"),
    LocalizedFitnessTerm("Barbell Biceps Curl", "Barbell biceps curl"),
    LocalizedFitnessTerm("Biceps Curl", "Biceps curl"),
    LocalizedFitnessTerm("EZ Bar Curl", "EZ bar curl"),
    LocalizedFitnessTerm("Hammer Curl", "Hammer curl"),
    LocalizedFitnessTerm("Concentration Curl", "Concentration curl"),
    LocalizedFitnessTerm("Preacher Curl", "Preacher curl"),
    LocalizedFitnessTerm("Triceps Pushdown", "Triceps pushdown"),
    LocalizedFitnessTerm("Overhead Triceps Extension", "Overhead triceps extension"),
    LocalizedFitnessTerm("Skull Crusher", "Skull crusher"),
    LocalizedFitnessTerm("Close Grip Bench Press", "Dar tutuş bench press"),
    LocalizedFitnessTerm("Dips (Triceps)", "Triceps dips"),
    LocalizedFitnessTerm("Plank", "Plank"),
    LocalizedFitnessTerm("Side Plank", "Yan plank"),
    LocalizedFitnessTerm("Crunch", "Mekik"),
    LocalizedFitnessTerm("Leg Raise", "Bacak kaldırma"),
    LocalizedFitnessTerm("Russian Twist", "Russian twist"),
    LocalizedFitnessTerm("Ab Wheel", "Ab wheel"),
    LocalizedFitnessTerm("Mountain Climber", "Mountain climber"),
    LocalizedFitnessTerm("Burpee", "Burpee"),
    LocalizedFitnessTerm("Box Jump", "Box jump"),
    LocalizedFitnessTerm("Kettlebell Swing", "Kettlebell swing"),
    LocalizedFitnessTerm("Battle Ropes", "Battle ropes"),
    LocalizedFitnessTerm("Jump Rope", "İp atlama"),
    LocalizedFitnessTerm("Double Unders", "Double unders"),
    LocalizedFitnessTerm("Treadmill Run", "Koşu bandı koşusu"),
    LocalizedFitnessTerm("Outdoor Walk", "Açık hava yürüyüşü"),
    LocalizedFitnessTerm("Cycling", "Bisiklet"),
    LocalizedFitnessTerm("Swimming", "Yüzme"),
    LocalizedFitnessTerm("Rowing Machine", "Kürek makinesi"),
    LocalizedFitnessTerm("Elliptical", "Eliptik"),
    LocalizedFitnessTerm("Stair Climber", "Merdiven tırmanıcı"),
    LocalizedFitnessTerm("Shadow Boxing", "Gölge boksu"),
    LocalizedFitnessTerm("Yoga Flow", "Yoga akışı"),
    LocalizedFitnessTerm("Single Leg Glute Bridge", "Tek bacak glute bridge")
)

private val FitnessGroupTerms = listOf(
    LocalizedFitnessTerm("Chest", "Göğüs", setOf("gogus")),
    LocalizedFitnessTerm("Back", "Sırt", setOf("sirt")),
    LocalizedFitnessTerm("Shoulders", "Omuz", setOf("shoulder", "omuz")),
    LocalizedFitnessTerm("Biceps", "Pazı", setOf("biseps")),
    LocalizedFitnessTerm("Triceps", "Arka kol", setOf("triseps")),
    LocalizedFitnessTerm("Arms", "Kol", setOf("arm", "kol")),
    LocalizedFitnessTerm("Legs", "Bacak", setOf("leg", "bacak")),
    LocalizedFitnessTerm("Quads", "Ön bacak", setOf("quad", "quadriceps")),
    LocalizedFitnessTerm("Hamstrings", "Arka bacak", setOf("hamstring")),
    LocalizedFitnessTerm("Calves", "Baldır", setOf("calf", "baldir")),
    LocalizedFitnessTerm("Core", "Karın", setOf("abs", "abdominal", "karin")),
    LocalizedFitnessTerm("Cardio", "Kardiyo", setOf("kardiyo", "kardiyovaskuler", "kardiyovasküler")),
    LocalizedFitnessTerm("Conditioning", "Kondisyon", setOf("kondisyon")),
    LocalizedFitnessTerm("General", "Genel", setOf("genel")),
    LocalizedFitnessTerm("Free Weight", "Serbest ağırlık", setOf("free weights", "serbest agirlik", "serbest ağırlık")),
    LocalizedFitnessTerm("Machine", "Makine", setOf("makine")),
    LocalizedFitnessTerm("Bodyweight", "Vücut ağırlığı", setOf("body weight", "vucut agirligi", "vücut ağırlığı")),
    LocalizedFitnessTerm("Cable", "Kablo", setOf("kablo")),
    LocalizedFitnessTerm("Strength", "Kuvvet", setOf("kuvvet", "guc", "güç")),
    LocalizedFitnessTerm("Fitness", "Fitness", setOf("fitnes"))
)

private fun List<LocalizedFitnessTerm>.findTerm(value: String): LocalizedFitnessTerm? {
    val key = normalizeDisplayKey(value)
    return firstOrNull { term ->
        key == normalizeDisplayKey(term.english) ||
            key == normalizeDisplayKey(term.turkish) ||
            term.aliases.any { alias -> key == normalizeDisplayKey(alias) }
    }
}

fun AppThemeState.exerciseDisplayName(name: String, nameEn: String = ""): String {
    val knownFromName = ExerciseNameTerms.findTerm(name)
    val knownFromEnglish = nameEn.takeIf { it.isNotBlank() }?.let { ExerciseNameTerms.findTerm(it) }
    val term = knownFromEnglish ?: knownFromName

    val english = term?.english ?: nameEn.takeIf { it.isNotBlank() } ?: name
    if (language == AppLanguage.ENGLISH) return english

    val turkish = term?.turkish
        ?: name.takeIf {
            it.isNotBlank() && !normalizeDisplayKey(it).equals(normalizeDisplayKey(english))
        }

    return if (turkish.isNullOrBlank() || normalizeDisplayKey(turkish) == normalizeDisplayKey(english)) {
        english
    } else {
        "$english ($turkish)"
    }
}

fun AppThemeState.fitnessTermDisplayName(value: String): String {
    val term = FitnessGroupTerms.findTerm(value) ?: return value
    return if (language == AppLanguage.ENGLISH) {
        term.english
    } else {
        "${term.english} (${term.turkish})"
    }
}
