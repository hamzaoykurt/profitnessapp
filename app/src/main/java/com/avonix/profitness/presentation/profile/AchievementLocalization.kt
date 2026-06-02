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

internal fun localizedAchievementText(value: String, theme: AppThemeState): String {
    if (theme.language != AppLanguage.ENGLISH) return value
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
        .replaceAchievementPattern(Regex("""^(\d+)\s+XP — tanrısal güç!$""")) { "${it.groupValues[1]} XP - divine power!" }
}

private inline fun String.replaceAchievementPattern(
    regex: Regex,
    transform: (MatchResult) -> String
): String {
    val match = regex.matchEntire(this) ?: return this
    return transform(match)
}
