package com.avonix.profitness.presentation.leaderboard

import androidx.lifecycle.viewModelScope
import com.avonix.profitness.core.BaseViewModel
import com.avonix.profitness.data.leaderboard.LeaderboardRepository
import com.avonix.profitness.data.social.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI Modelleri ──────────────────────────────────────────────────────────────

enum class LeaderboardTab { Xp, Achievements, Streak }

/** Sıralama kapsamı: tüm kullanıcılar mı, sadece arkadaşlar (mutual follow) mı? */
enum class LeaderboardScope { Global, Friends }

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
    val selectedTab         : LeaderboardTab       = LeaderboardTab.Xp,
    val selectedScope       : LeaderboardScope     = LeaderboardScope.Global,
    val xpRows              : List<LeaderboardRow> = emptyList(),
    val achievementRows     : List<LeaderboardRow> = emptyList(),
    val streakRows          : List<LeaderboardRow> = emptyList(),
    val friendXpRows        : List<LeaderboardRow> = emptyList(),
    val friendAchievementRows: List<LeaderboardRow> = emptyList(),
    val friendStreakRows    : List<LeaderboardRow> = emptyList(),
    val myXp                : MyPositionSummary    = MyPositionSummary(),
    val myAchievements      : MyPositionSummary    = MyPositionSummary(),
    val myStreak            : MyPositionSummary    = MyPositionSummary(),
    val myFriendXp          : MyPositionSummary    = MyPositionSummary(),
    val myFriendAchievements: MyPositionSummary    = MyPositionSummary(),
    val myFriendStreak      : MyPositionSummary    = MyPositionSummary(),
    val isLoading           : Boolean              = true,
    val error               : String?              = null
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val repo      : LeaderboardRepository,
    private val socialRepo: SocialRepository,
    private val supabase  : SupabaseClient
) : BaseViewModel<LeaderboardState, Nothing>(LeaderboardState()) {

    init { load() }

    fun selectTab(tab: LeaderboardTab) {
        updateState { it.copy(selectedTab = tab) }
    }

    fun selectScope(scope: LeaderboardScope) {
        updateState { it.copy(selectedScope = scope) }
    }

    fun refresh() = load()

    private fun load() {
        val uid = supabase.auth.currentUserOrNull()?.id
        updateState { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val xpDeferred        = async { repo.getXpLeaderboard(100) }
            val achDeferred       = async { repo.getAchievementLeaderboard(100) }
            val streakDeferred    = async { repo.getStreakLeaderboard(100) }
            val myXpDeferred      = async { repo.getMyXpRank() }
            val myAchDeferred     = async { repo.getMyAchievementRank() }
            val myStreakDeferred  = async { repo.getMyStreakRank() }
            val friendXpDeferred  = async { socialRepo.getFriendLeaderboardXp(100) }
            val friendAchDeferred = async { socialRepo.getFriendLeaderboardAchievements(100) }
            val friendStreakDeferred = async { socialRepo.getFriendLeaderboardStreak(100) }

            val xpRes        = xpDeferred.await()
            val achRes       = achDeferred.await()
            val streakRes    = streakDeferred.await()
            val myXpRes      = myXpDeferred.await()
            val myAchRes     = myAchDeferred.await()
            val myStreakRes  = myStreakDeferred.await()
            val friendXpRes  = friendXpDeferred.await()
            val friendAchRes = friendAchDeferred.await()
            val friendStreakRes = friendStreakDeferred.await()

            val firstErr =
                xpRes.exceptionOrNull()?.message
                    ?: achRes.exceptionOrNull()?.message
                    ?: streakRes.exceptionOrNull()?.message
                    ?: myXpRes.exceptionOrNull()?.message
                    ?: myAchRes.exceptionOrNull()?.message
                    ?: myStreakRes.exceptionOrNull()?.message
                    ?: friendXpRes.exceptionOrNull()?.message
                    ?: friendAchRes.exceptionOrNull()?.message
                    ?: friendStreakRes.exceptionOrNull()?.message

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
            val streakRows = streakRes.getOrNull().orEmpty().map { dto ->
                LeaderboardRow(
                    userId      = dto.user_id,
                    displayName = dto.display_name.ifBlank { "Anonim" },
                    avatar      = dto.avatar_url?.takeIf { it.isNotBlank() } ?: "🏋️",
                    score       = dto.current_streak.toLong(),
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

            val myStreak = myStreakRes.getOrNull()?.let {
                MyPositionSummary(
                    position   = it.rank_position,
                    totalUsers = it.total_users,
                    score      = it.current_streak.toLong()
                )
            } ?: MyPositionSummary()

            // ── Arkadaş (mutual follow) leaderboard'ları UI satırlarına çevir ─
            val friendXpList  = friendXpRes.getOrNull().orEmpty()
            val friendAchList = friendAchRes.getOrNull().orEmpty()
            val friendStreakList = friendStreakRes.getOrNull().orEmpty()

            val friendXpRows = friendXpList.map { r ->
                LeaderboardRow(
                    userId      = r.userId,
                    displayName = r.displayName,
                    avatar      = r.avatarUrl?.takeIf { it.isNotBlank() } ?: "🏋️",
                    score       = r.totalXp.toLong(),
                    position    = r.rankPosition.toLong(),
                    isMe        = r.isMe
                )
            }
            val friendAchRows = friendAchList.map { r ->
                LeaderboardRow(
                    userId      = r.userId,
                    displayName = r.displayName,
                    avatar      = r.avatarUrl?.takeIf { it.isNotBlank() } ?: "🏋️",
                    score       = r.achievementCount.toLong(),
                    position    = r.rankPosition.toLong(),
                    isMe        = r.isMe
                )
            }
            val friendStreakRows = friendStreakList.map { r ->
                LeaderboardRow(
                    userId      = r.userId,
                    displayName = r.displayName,
                    avatar      = r.avatarUrl?.takeIf { it.isNotBlank() } ?: "🏋️",
                    score       = r.currentStreak.toLong(),
                    position    = r.rankPosition.toLong(),
                    isMe        = r.isMe
                )
            }

            // Arkadaş kapsamında "benim pozisyonum" = arkadaşlar arasındaki sıram.
            val myFriendXp = friendXpList.firstOrNull { it.isMe }?.let {
                MyPositionSummary(
                    position   = it.rankPosition.toLong(),
                    totalUsers = friendXpList.size.toLong(),
                    score      = it.totalXp.toLong()
                )
            } ?: MyPositionSummary(totalUsers = friendXpList.size.toLong())

            val myFriendAch = friendAchList.firstOrNull { it.isMe }?.let {
                MyPositionSummary(
                    position   = it.rankPosition.toLong(),
                    totalUsers = friendAchList.size.toLong(),
                    score      = it.achievementCount.toLong()
                )
            } ?: MyPositionSummary(totalUsers = friendAchList.size.toLong())

            val myFriendStreak = friendStreakList.firstOrNull { it.isMe }?.let {
                MyPositionSummary(
                    position   = it.rankPosition.toLong(),
                    totalUsers = friendStreakList.size.toLong(),
                    score      = it.currentStreak.toLong()
                )
            } ?: MyPositionSummary(totalUsers = friendStreakList.size.toLong())

            updateState {
                it.copy(
                    xpRows               = xpRows,
                    achievementRows      = achRows,
                    streakRows           = streakRows,
                    friendXpRows         = friendXpRows,
                    friendAchievementRows= friendAchRows,
                    friendStreakRows     = friendStreakRows,
                    myXp                 = myXp,
                    myAchievements       = myAch,
                    myStreak             = myStreak,
                    myFriendXp           = myFriendXp,
                    myFriendAchievements = myFriendAch,
                    myFriendStreak       = myFriendStreak,
                    isLoading            = false,
                    error                = firstErr
                )
            }
        }
    }
}
