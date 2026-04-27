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
    /**
     * TRUE if caller's local programs contain a row with content matching this snapshot
     * (server-computed via SHA-256). Edits to a previously-applied program flip this back
     * to FALSE so the "Uygula" button reappears.
     */
    val is_applied_by_me    : Boolean = false,
    /** Server-computed canonical hash of program_data; lets clients short-circuit comparisons. */
    val content_hash        : String? = null,
    val created_at          : String
)

/** Supabase RPC: public.list_my_shared_programs() */
@Serializable
data class MySharedProgramRowDto(
    val id                  : String,
    val original_program_id : String? = null,
    val title               : String,
    val description         : String? = null,
    val tags                : List<String> = emptyList(),
    val difficulty          : String? = null,
    val duration_weeks      : Int? = null,
    val days_per_week       : Int? = null,
    val likes_count         : Int = 0,
    val saves_count         : Int = 0,
    val downloads_count     : Int = 0,
    val created_at          : String,
    val updated_at          : String? = null,
    val source_exists       : Boolean = false,
    val source_program_name : String? = null,
    val is_out_of_sync      : Boolean = false,
    /** SHA-256 hash stored on the shared snapshot row at publish time. */
    val shared_content_hash : String? = null,
    /** Current hash of the source program (NULL if source program was deleted). */
    val source_content_hash : String? = null
)
