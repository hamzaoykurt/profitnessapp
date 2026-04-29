package com.avonix.profitness.presentation.leaderboard

import androidx.lifecycle.viewModelScope
import com.avonix.profitness.core.BaseViewModel
import com.avonix.profitness.core.security.toUserSafeMessage
import com.avonix.profitness.data.leaderboard.LeaderboardRepository
import com.avonix.profitness.data.social.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI Modelleri ──────────────────────────────────────────────────────────────

enum class LeaderboardTab { Xp, Achievements }

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
    val friendXpRows        : List<LeaderboardRow> = emptyList(),
    val friendAchievementRows: List<LeaderboardRow> = emptyList(),
    val myXp                : MyPositionSummary    = MyPositionSummary(),
    val myAchievements      : MyPositionSummary    = MyPositionSummary(),
    val myFriendXp          : MyPositionSummary    = MyPositionSummary(),
    val myFriendAchievements: MyPositionSummary    = MyPositionSummary(),
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
            val myXpDeferred      = async { repo.getMyXpRank() }
            val myAchDeferred     = async { repo.getMyAchievementRank() }
            val friendXpDeferred  = async { socialRepo.getFriendLeaderboardXp(100) }
            val friendAchDeferred = async { socialRepo.getFriendLeaderboardAchievements(100) }

            val xpRes        = xpDeferred.await()
            val achRes       = achDeferred.await()
            val myXpRes      = myXpDeferred.await()
            val myAchRes     = myAchDeferred.await()
            val friendXpRes  = friendXpDeferred.await()
            val friendAchRes = friendAchDeferred.await()

            val firstErr =
                xpRes.exceptionOrNull()?.toUserSafeMessage("Liderlik tablosu yüklenemedi.")
                    ?: achRes.exceptionOrNull()?.toUserSafeMessage("Liderlik tablosu yüklenemedi.")
                    ?: myXpRes.exceptionOrNull()?.toUserSafeMessage("Liderlik tablosu yüklenemedi.")
                    ?: myAchRes.exceptionOrNull()?.toUserSafeMessage("Liderlik tablosu yüklenemedi.")
                    ?: friendXpRes.exceptionOrNull()?.toUserSafeMessage("Liderlik tablosu yüklenemedi.")
                    ?: friendAchRes.exceptionOrNull()?.toUserSafeMessage("Liderlik tablosu yüklenemedi.")

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

            // ── Arkadaş (mutual follow) leaderboard'ları UI satırlarına çevir ─
            val friendXpList  = friendXpRes.getOrNull().orEmpty()
            val friendAchList = friendAchRes.getOrNull().orEmpty()

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

            updateState {
                it.copy(
                    xpRows               = xpRows,
                    achievementRows      = achRows,
                    friendXpRows         = friendXpRows,
                    friendAchievementRows= friendAchRows,
                    myXp                 = myXp,
                    myAchievements       = myAch,
                    myFriendXp           = myFriendXp,
                    myFriendAchievements = myFriendAch,
                    isLoading            = false,
                    error                = firstErr
                )
            }
        }
    }
}
