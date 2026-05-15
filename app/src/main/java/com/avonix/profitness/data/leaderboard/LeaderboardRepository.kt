package com.avonix.profitness.data.leaderboard

import com.avonix.profitness.data.leaderboard.dto.LeaderboardAchievementRowDto
import com.avonix.profitness.data.leaderboard.dto.LeaderboardStreakRowDto
import com.avonix.profitness.data.leaderboard.dto.LeaderboardXpRowDto
import com.avonix.profitness.data.leaderboard.dto.MyAchievementRankDto
import com.avonix.profitness.data.leaderboard.dto.MyStreakRankDto
import com.avonix.profitness.data.leaderboard.dto.MyXpRankDto

interface LeaderboardRepository {

    /** XP bazlı global sıralama — top N kullanıcı. */
    suspend fun getXpLeaderboard(limit: Int = 100): Result<List<LeaderboardXpRowDto>>

    /** Başarım sayısı bazlı global sıralama — top N kullanıcı. */
    suspend fun getAchievementLeaderboard(limit: Int = 100): Result<List<LeaderboardAchievementRowDto>>

    /** Aktif günlük seri bazlı global sıralama — top N kullanıcı. */
    suspend fun getStreakLeaderboard(limit: Int = 100): Result<List<LeaderboardStreakRowDto>>

    /** Oturumdaki kullanıcının XP sıralamasındaki pozisyonu. */
    suspend fun getMyXpRank(): Result<MyXpRankDto>

    /** Oturumdaki kullanıcının başarım sıralamasındaki pozisyonu. */
    suspend fun getMyAchievementRank(): Result<MyAchievementRankDto>

    /** Oturumdaki kullanıcının aktif günlük seri sıralamasındaki pozisyonu. */
    suspend fun getMyStreakRank(): Result<MyStreakRankDto>
}
