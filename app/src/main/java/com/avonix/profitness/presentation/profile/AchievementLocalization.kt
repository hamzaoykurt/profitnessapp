package com.avonix.profitness.presentation.profile

import com.avonix.profitness.core.theme.AppLanguage
import com.avonix.profitness.core.theme.AppThemeState

private val AchievementEnglishText = mapOf(
    "3 Günlük Seri" to "3-Day Streak",
    "5 Günlük Seri" to "5-Day Streak",
    "7 Günlük Seri" to "7-Day Streak",
    "7 Günlük" to "7-Day Streak",
    "2 Haftalık Seri" to "2-Week Streak",
    "Haftalık Savaşçı" to "Weekly Warrior",
    "Demir Disiplin" to "Iron Discipline",
    "60 Günlük Seri" to "60-Day Streak",
    "100 Günlük Seri" to "100-Day Streak",
    "365 Günlük Seri" to "365-Day Streak",
    "365 Gunluk Seri" to "365-Day Streak",
    "İlk Antrenman" to "First Workout",
    "İlk Zafer" to "First Victory",
    "İlk Adım" to "First Step",
    "Çeyrek Yüzlük" to "Quarter Century",
    "Efsane" to "Legend",
    "Yıllık Şampiyon" to "Yearly Champion",
    "Yarım Bin" to "Half Thousand",
    "Gümüş Rank" to "Silver Rank",
    "Altın Rank" to "Gold Rank",
    "Platin Rank" to "Platinum Rank",
    "Elmas Rank" to "Diamond Rank",
    "Süper Üye" to "Super Member",
    "Mükemmellik" to "Excellence",
    "Egzersiz Ustası" to "Exercise Master",
    "100 Egzersiz" to "100 Exercises",
    "500 Egzersiz" to "500 Exercises",
    "1000 Egzersiz" to "1000 Exercises",
    "5000 Egzersiz" to "5000 Exercises",
    "100 Exercises" to "100 Exercises",
    "500 Exercises" to "500 Exercises",
    "1000 Exercises" to "1000 Exercises",
    "5000 Exercises" to "5000 Exercises",
    "Level 5" to "Level 5",
    "Level 15" to "Level 15",
    "Level 30" to "Level 30",
    "Level 50" to "Level 50",
    "3 gün üst üste antrenman yaptın!" to "You trained 3 days in a row!",
    "5 gün üst üste antrenman yaptın!" to "You trained 5 days in a row!",
    "7 gün üst üste antrenman yaptın!" to "You trained 7 days in a row!",
    "14 gün üst üste antrenman yaptın!" to "You trained 14 days in a row!",
    "30 gün üst üste antrenman!" to "30 days of training in a row!",
    "60 gün üst üste — inanılmaz!" to "60 days in a row - incredible!",
    "100 gün üst üste — efsane disiplin!" to "100 days in a row - legendary discipline!",
    "365 gün boyunca seri kırmadınız!" to "You kept your streak for 365 days!",
    "365 gun boyunca seri kırmadınız!" to "You kept your streak for 365 days!",
    "Silver ranka ulaştın!" to "You reached Silver rank!",
    "Gold ranka ulaştın!" to "You reached Gold rank!",
    "Platinum ranka ulaştın — elit!" to "You reached Platinum rank - elite!",
    "Diamond rank — zirvedesin!" to "Diamond rank - you are at the top!",
    "50000 XP — tanrısal güç!" to "50000 XP - divine power!"
)

private val AchievementTurkishText = AchievementEnglishText.entries.associate { (tr, en) -> en to tr } + mapOf(
    "First Step" to "İlk Adım",
    "Exercise Master" to "Egzersiz Ustası",
    "100 Exercises" to "100 Egzersiz",
    "500 Exercises" to "500 Egzersiz",
    "1000 Exercises" to "1000 Egzersiz",
    "5000 Exercises" to "5000 Egzersiz",
    "Level 5" to "Seviye 5",
    "Level 15" to "Seviye 15",
    "Level 30" to "Seviye 30",
    "Level 50" to "Seviye 50",
    "First 100 XP!" to "İlk 100 XP!",
    "100 XP collected!" to "100 XP topladın!",
    "250 XP collected!" to "250 XP topladın!",
    "500 XP collected!" to "500 XP topladın!",
    "1000 XP collected!" to "1000 XP topladın!",
    "2500 XP - rising!" to "2500 XP - yükseliş!",
    "5000 XP - gold level!" to "5000 XP - altın seviye!",
    "10000 XP - legendary performance!" to "10000 XP - efsanevi performans!",
    "25000 XP - legendary!" to "25000 XP - efsanevi!",
    "50000 XP - divine power!" to "50000 XP - tanrısal güç!",
    "You completed your first workout!" to "İlk antrenmanını tamamladın!",
    "You completed 100 exercises!" to "100 egzersiz tamamladın!",
    "You completed 500 exercises!" to "500 egzersiz tamamladın!",
    "You completed 1000 exercises!" to "1000 egzersiz tamamladın!",
    "You completed 5000 exercises!" to "5000 egzersiz tamamladın!",
    "You reached level 5!" to "Seviye 5'e ulaştın!",
    "You reached level 15!" to "Seviye 15'e ulaştın!",
    "You reached level 30!" to "Seviye 30'a ulaştın!",
    "You reached level 50 - legendary!" to "Seviye 50'ye ulaştın - efsane!"
)

internal fun localizedAchievementText(value: String, theme: AppThemeState): String {
    return if (theme.language == AppLanguage.ENGLISH) {
        localizedAchievementEnglish(value)
    } else {
        localizedAchievementTurkish(value)
    }
}

private fun localizedAchievementEnglish(value: String): String {
    AchievementEnglishText[value]?.let { return it }
    return value
        .replaceAchievementPattern(Regex("""^(\d+)\s+Günlük Seri$""")) { "${it.groupValues[1]}-Day Streak" }
        .replaceAchievementPattern(Regex("""^(\d+)\s+Gunluk Seri$""")) { "${it.groupValues[1]}-Day Streak" }
        .replaceAchievementPattern(Regex("""^(\d+)\s+Haftalık Seri$""")) { "${it.groupValues[1]}-Week Streak" }
        .replaceAchievementPattern(Regex("""^(\d+)\s+Antrenman$""")) { "${it.groupValues[1]} Workouts" }
        .replaceAchievementPattern(Regex("""^(\d+)\s+Workouts$""")) { value }
        .replaceAchievementPattern(Regex("""^(\d+)\s+Egzersiz$""")) { "${it.groupValues[1]} Exercises" }
        .replaceAchievementPattern(Regex("""^Seviye\s+(\d+)$""")) { "Level ${it.groupValues[1]}" }
        .replaceAchievementPattern(Regex("""^(\d+)\s+gün üst üste antrenman yaptın!$""")) { "You trained ${it.groupValues[1]} days in a row!" }
        .replaceAchievementPattern(Regex("""^(\d+)\s+antrenman tamamladın!$""")) { "You completed ${it.groupValues[1]} workouts!" }
        .replaceAchievementPattern(Regex("""^(\d+)\s+antrenman — pes etmedin!$""")) { "${it.groupValues[1]} workouts - you did not quit!" }
        .replaceAchievementPattern(Regex("""^(\d+)\s+antrenman — efsane oldun!$""")) { "${it.groupValues[1]} workouts - legendary!" }
        .replaceAchievementPattern(Regex("""^(\d+)\s+antrenman — durdurulamıyorsun!$""")) { "${it.groupValues[1]} workouts - unstoppable!" }
        .replaceAchievementPattern(Regex("""^(\d+)\s+antrenman — tam bir yıl!$""")) { "${it.groupValues[1]} workouts - a full year!" }
        .replaceAchievementPattern(Regex("""^(\d+)\s+antrenman — canavarlaştın!$""")) { "${it.groupValues[1]} workouts - unstoppable!" }
        .replaceAchievementPattern(Regex("""^(\d+)\s+egzersiz tamamladın!$""")) { "You completed ${it.groupValues[1]} exercises!" }
        .replaceAchievementPattern(Regex("""^Seviye\s+(\d+)e ulaştınız!$""")) { "You reached level ${it.groupValues[1]}!" }
        .replaceAchievementPattern(Regex("""^Seviye\s+(\d+)a ulaştınız!$""")) { "You reached level ${it.groupValues[1]}!" }
        .replaceAchievementPattern(Regex("""^Seviye\s+(\d+)ye ulaştınız - Efsane!$""")) { "You reached level ${it.groupValues[1]} - legendary!" }
        .replaceAchievementPattern(Regex("""^Seviye\s+(\d+)'[ae] ulaştın!$""")) { "You reached level ${it.groupValues[1]}!" }
        .replaceAchievementPattern(Regex("""^Seviye\s+(\d+)'ye ulaştın - efsane!$""")) { "You reached level ${it.groupValues[1]} - legendary!" }
        .replaceAchievementPattern(Regex("""^(\d+)\s+XP — tanrısal güç!$""")) { "${it.groupValues[1]} XP - divine power!" }
}

private fun localizedAchievementTurkish(value: String): String {
    AchievementTurkishText[value]?.let { return it }
    return value
        .replaceAchievementPattern(Regex("""^(\d+)-Day Streak$""")) { "${it.groupValues[1]} Günlük Seri" }
        .replaceAchievementPattern(Regex("""^(\d+)-Week Streak$""")) { "${it.groupValues[1]} Haftalık Seri" }
        .replaceAchievementPattern(Regex("""^(\d+)\s+Workouts$""")) { "${it.groupValues[1]} Antrenman" }
        .replaceAchievementPattern(Regex("""^(\d+)\s+Exercises$""")) { "${it.groupValues[1]} Egzersiz" }
        .replaceAchievementPattern(Regex("""^Level\s+(\d+)$""")) { "Seviye ${it.groupValues[1]}" }
        .replaceAchievementPattern(Regex("""^You trained\s+(\d+)\s+days in a row!$""")) { "${it.groupValues[1]} gün üst üste antrenman yaptın!" }
        .replaceAchievementPattern(Regex("""^You completed\s+(\d+)\s+workouts!$""")) { "${it.groupValues[1]} antrenman tamamladın!" }
        .replaceAchievementPattern(Regex("""^You completed\s+(\d+)\s+exercises!$""")) { "${it.groupValues[1]} egzersiz tamamladın!" }
        .replaceAchievementPattern(Regex("""^You reached level\s+(\d+)!$""")) { "Seviye ${it.groupValues[1]}${turkishLevelSuffix(it.groupValues[1])} ulaştın!" }
        .replaceAchievementPattern(Regex("""^You reached level\s+(\d+)\s+-\s+legendary!$""")) { "Seviye ${it.groupValues[1]}${turkishLevelSuffix(it.groupValues[1])} ulaştın - efsane!" }
        .replaceAchievementPattern(Regex("""^(\d+)\s+XP\s+-\s+divine power!$""")) { "${it.groupValues[1]} XP - tanrısal güç!" }
}

private fun turkishLevelSuffix(level: String): String =
    when (level.toIntOrNull()?.rem(100)) {
        10, 30, 40, 60, 90 -> "'a"
        20, 50, 70 -> "'ye"
        else -> when (level.lastOrNull()) {
            '6' -> "'ya"
            '9' -> "'a"
            else -> "'e"
        }
    }

private inline fun String.replaceAchievementPattern(
    regex: Regex,
    transform: (MatchResult) -> String
): String {
    val match = regex.matchEntire(this) ?: return this
    return transform(match)
}
