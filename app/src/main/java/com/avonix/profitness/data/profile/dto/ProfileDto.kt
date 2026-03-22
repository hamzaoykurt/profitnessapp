package com.avonix.profitness.data.profile.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val user_id      : String,
    val display_name : String? = null,
    val avatar_url   : String? = null,   // emoji string ("🏋️") veya gelecekte gerçek URL
    val gender       : String? = null,
    val height_cm    : Double? = null,
    val weight_kg    : Double? = null,
    val fitness_goal : String? = null,
    val current_rank : String  = "Bronze",
    val total_xp     : Int     = 0,
    val birth_date   : String? = null
)
