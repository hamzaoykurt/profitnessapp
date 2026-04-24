package com.avonix.profitness.core.analytics

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight analytics façade. Currently a no-op (writes to logcat in debug).
 * Real backend (Firebase / Mixpanel / Amplitude) can replace the impl later
 * without touching call sites.
 */
interface AnalyticsTracker {
    fun track(event: String, props: Map<String, Any?> = emptyMap())
}

/** Logcat-only impl — safe in production (TAG discoverable via `adb logcat -s AProf`). */
@Singleton
class NoOpAnalyticsTracker @Inject constructor() : AnalyticsTracker {
    override fun track(event: String, props: Map<String, Any?>) {
        if (props.isEmpty()) {
            Log.d(TAG, event)
        } else {
            Log.d(TAG, "$event ${props.entries.joinToString(" ") { "${it.key}=${it.value}" }}")
        }
    }

    private companion object { const val TAG = "AProf" }
}

/**
 * Canonical event names — keeping them typed prevents drift.
 * Only FAZ 7J-specific keys are listed here; older fazes may define their own elsewhere.
 */
object AnalyticsEvents {
    // Challenge 2.0
    const val CHALLENGE_CREATED             = "challenge_created"
    const val CHALLENGE_JOINED              = "challenge_joined"
    const val CHALLENGE_LEFT                = "challenge_left"
    const val CHALLENGE_PRIVATE_JOIN_PROMPT = "challenge_private_join_prompt"
    const val EVENT_MOVEMENT_TOGGLED        = "event_movement_toggled"
    const val SKIP_PROGRAM_TOGGLED          = "skip_program_toggled"
    const val TODAY_BANNER_OPENED           = "today_banner_opened"
    const val UPCOMING_EVENT_OPENED         = "upcoming_event_opened"
    const val EVENT_MAP_OPENED              = "event_map_opened"
    const val EVENT_LINK_OPENED             = "event_link_opened"
}
