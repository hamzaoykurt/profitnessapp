package com.avonix.profitness.data.challenges

import com.avonix.profitness.domain.challenges.ChallengeDetail
import com.avonix.profitness.domain.challenges.ChallengeSummary
import com.avonix.profitness.domain.challenges.ChallengeTargetType
import com.avonix.profitness.domain.challenges.ChallengeVisibility

interface ChallengeRepository {

    /** Aktif public challenge'lar + benim join/progress durumum. */
    suspend fun listPublicChallenges(limit: Int = 50, offset: Int = 0): Result<List<ChallengeSummary>>

    /** Oturum sahibinin katıldığı (aktif + bitmiş) challenge'lar. */
    suspend fun listMyChallenges(): Result<List<ChallengeSummary>>

    /** Tek challenge detayı + katılımcı leaderboard (top 50). */
    suspend fun getChallengeDetail(challengeId: String): Result<ChallengeDetail>

    /** Yeni challenge yarat. Döner: yeni challenge id. */
    suspend fun createChallenge(
        title       : String,
        description : String,
        targetType  : ChallengeTargetType,
        targetValue : Long,
        startDateIso: String,
        endDateIso  : String,
        visibility  : ChallengeVisibility = ChallengeVisibility.Public,
        password    : String? = null
    ): Result<String>

    /** Challenge'a katıl. Private ise password zorunlu. */
    suspend fun joinChallenge(challengeId: String, password: String? = null): Result<Boolean>

    /** Challenge'dan ayrıl. */
    suspend fun leaveChallenge(challengeId: String): Result<Boolean>

    /**
     * Tüm aktif challenge'larımdaki progress'i sunucuda yeniden hesaplat.
     * Workout bitince çağrılır. Döner: yeni tamamlanan challenge sayısı.
     */
    suspend fun refreshMyProgress(): Result<Int>
}
