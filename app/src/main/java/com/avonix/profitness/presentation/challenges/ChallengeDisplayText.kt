package com.avonix.profitness.presentation.challenges

import com.avonix.profitness.core.theme.AppThemeState
import com.avonix.profitness.core.theme.strings
import com.avonix.profitness.core.theme.t
import com.avonix.profitness.domain.challenges.ChallengeTargetType
import com.avonix.profitness.domain.challenges.EventMode
import com.avonix.profitness.presentation.workout.SportType

internal fun ChallengeTargetType.displayLabel(theme: AppThemeState): String = when (this) {
    ChallengeTargetType.TotalWorkouts -> theme.t("Toplam Antrenman", "Total Workouts")
    ChallengeTargetType.TotalXp -> theme.t("Toplam XP", "Total XP")
    ChallengeTargetType.CurrentStreak -> theme.t("Aktif Seri", "Current Streak")
    ChallengeTargetType.TotalDurationMinutes -> theme.t("Toplam Süre", "Total Duration")
    ChallengeTargetType.TotalDistanceM -> theme.t("Mesafe", "Distance")
    ChallengeTargetType.TotalDistanceKm -> theme.t("Mesafe", "Distance")
    ChallengeTargetType.MovementsCompleted -> theme.t("Hareket", "Movement")
}

internal fun ChallengeTargetType.displayUnit(theme: AppThemeState): String = when (this) {
    ChallengeTargetType.TotalWorkouts -> theme.t("antrenman", "workouts")
    ChallengeTargetType.TotalXp -> "XP"
    ChallengeTargetType.CurrentStreak -> theme.t("gün", "days")
    ChallengeTargetType.TotalDurationMinutes -> theme.t("dk", "min")
    ChallengeTargetType.TotalDistanceM -> "m"
    ChallengeTargetType.TotalDistanceKm -> "km"
    ChallengeTargetType.MovementsCompleted -> theme.t("hareket", "movements")
}

internal fun SportType.displayLabel(theme: AppThemeState): String = when (this) {
    SportType.Strength -> theme.t("Kuvvet", "Strength")
    SportType.Running -> theme.t("Koşu", "Running")
    SportType.Cycling -> theme.t("Bisiklet", "Cycling")
    SportType.Swimming -> theme.t("Yüzme", "Swimming")
    SportType.Rowing -> theme.t("Kürek", "Rowing")
    SportType.WalkingHiking -> theme.t("Yürüyüş", "Walking")
    SportType.JumpRopeHiit -> "HIIT"
    SportType.YogaPilates -> "Yoga"
    SportType.Boxing -> theme.t("Boks", "Boxing")
    SportType.Football -> theme.t("Futbol", "Football")
    SportType.IndoorFootball -> theme.t("Halı Saha", "Indoor Football")
    SportType.Volleyball -> theme.t("Voleybol", "Volleyball")
    SportType.BasketballTennis -> theme.t("Basketbol/Tenis", "Basketball/Tennis")
}

internal fun EventMode.displayLabel(theme: AppThemeState): String = when (this) {
    EventMode.Physical -> theme.strings.eventModePhysical
    EventMode.Online -> theme.strings.eventModeOnline
    EventMode.MovementList -> theme.t("HAREKET LİSTESİ", "MOVEMENT LIST")
}
