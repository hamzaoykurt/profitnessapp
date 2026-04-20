package com.avonix.profitness.data.discover.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Supabase RPC: public.get_discover_feed(p_sort, p_limit, p_offset) */
@Serializable
data class DiscoverFeedRowDto(
    val id                  : String,
    val creator_id          : String,
    val creator_display_name: String? = null,
    val creator_avatar_url  : String? = null,
    val title               : String,
    val description         : String? = null,
    val program_data        : JsonElement? = null,
    val tags                : List<String> = emptyList(),
    val difficulty          : String? = null,
    val duration_weeks      : Int? = null,
    val days_per_week       : Int? = null,
    val likes_count         : Int = 0,
    val saves_count         : Int = 0,
    val downloads_count     : Int = 0,
    val is_liked_by_me      : Boolean = false,
    val is_saved_by_me      : Boolean = false,
    val created_at          : String
)
