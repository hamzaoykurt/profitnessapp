package com.avonix.profitness.presentation.leaderboard

import androidx.lifecycle.viewModelScope
import com.avonix.profitness.core.BaseViewModel
import com.avonix.profitness.data.leaderboard.LeaderboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI Modelleri ──────────────────────────────────────────────────────────────

enum class LeaderboardTab { Xp, Achievements }

/** Tek satır — iki sıralama tipi için ortak UI modeli. */
data class LeaderboardRow(
    val userId     : String,
    val displayName: String,
    val avatar     : String,   // emoji veya URL
    val score      : Long,     // xp veya başarım sayısı
    val position   : Long,
    val isMe       : Boolean
)

/** Oturumdaki kullanıcının özet kartı — her iki sekme için. */
data class MyPositionSummary(
    val position   : Long = 0L,
    val totalUsers : Long = 0L,
    val score      : Long = 0L
)

data class LeaderboardState(
    val selectedTab     : LeaderboardTab       = LeaderboardTab.Xp,
    val xpRows          : List<LeaderboardRow> = emptyList(),
    val achievementRows : List<LeaderboardRow> = emptyList(),
    val myXp            : MyPositionSummary    = MyPositionSummary(),
    val myAchievements  : MyPositionSummary    = MyPositionSummary(),
    val isLoading       : Boolean              = true,
    val error           : String?              = null
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val repo    : LeaderboardRepository,
    private val supabase: SupabaseClient
) : BaseViewModel<LeaderboardState, Nothing>(LeaderboardState()) {

    init { load() }

    fun selectTab(tab: LeaderboardTab) {
        updateState { it.copy(selectedTab = tab) }
    }

    fun refresh() = load()

    private fun load() {
        val uid = supabase.auth.currentUserOrNull()?.id
        updateState { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val xpDeferred     = async { repo.getXpLeaderboard(100) }
            val achDeferred    = async { repo.getAchievementLeaderboard(100) }
            val myXpDeferred   = async { repo.getMyXpRank() }
            val myAchDeferred  = async { repo.getMyAchievementRank() }

            val xpRes     = xpDeferred.await()
            val achRes    = achDeferred.await()
            val myXpRes   = myXpDeferred.await()
            val myAchRes  = myAchDeferred.await()

            val firstErr =
                xpRes.exceptionOrNull()?.message
                    ?: achRes.exceptionOrNull()?.message
                    ?: myXpRes.exceptionOrNull()?.message
                    ?: myAchRes.exceptionOrNull()?.message

            val xpRows = xpRes.getOrNull().orEmpty().map { dto ->
                LeaderboardRow(
                    userId      = dto.user_id,
                    displayName = dto.display_name.ifBlank { "Anonim" },
                    avatar      = dto.avatar_url?.takeIf { it.isNotBlank() } ?: "🏋️",
                    score       = dto.xp.toLong(),
                    position    = dto.rank_position,
                    isMe        = uid != null && dto.user_id == uid
                )
            }
            val achRows = achRes.getOrNull().orEmpty().map { dto ->
                LeaderboardRow(
                    userId      = dto.user_id,
                    displayName = dto.display_name.ifBlank { "Anonim" },
                    avatar      = dto.avatar_url?.takeIf { it.isNotBlank() } ?: "🏋️",
                    score       = dto.achievement_count,
                    position    = dto.rank_position,
                    isMe        = uid != null && dto.user_id == uid
                )
            }

            val myXp = myXpRes.getOrNull()?.let {
                MyPositionSummary(
                    position   = it.rank_position,
                    totalUsers = it.total_users,
                    score      = it.xp.toLong()
                )
            } ?: MyPositionSummary()

            val myAch = myAchRes.getOrNull()?.let {
                MyPositionSummary(
                    position   = it.rank_position,
                    totalUsers = it.total_users,
                    score      = it.achievement_count
                )
            } ?: MyPositionSummary()

            updateState {
                it.copy(
                    xpRows          = xpRows,
                    achievementRows = achRows,
                    myXp            = myXp,
                    myAchievements  = myAch,
                    isLoading       = false,
                    error           = firstErr
                )
            }
        }
    }
}
