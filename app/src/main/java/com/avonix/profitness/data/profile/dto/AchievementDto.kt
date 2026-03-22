package com.avonix.profitness.data.profile.dto

import kotlinx.serialization.Serializable

@Serializable
data class AchievementDto(
    val id         : String,
    val key        : String,
    val name       : String,
    val name_en    : String,
    val description: String? = null,
    val icon       : String? = null,
    val category   : String = "milestone",
    val threshold  : Int    = 1
)

@Serializable
data class UserAchievementDto(
    val id            : String = "",
    val user_id       : String,
    val achievement_id: String,
    val unlocked_at   : String = ""
)
