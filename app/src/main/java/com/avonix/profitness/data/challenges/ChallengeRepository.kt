package com.avonix.profitness.data.challenges

import com.avonix.profitness.domain.challenges.ChallengeDetail
import com.avonix.profitness.domain.challenges.ChallengeSummary
import com.avonix.profitness.domain.challenges.ChallengeTargetType
import com.avonix.profitness.domain.challenges.ChallengeVisibility
import com.avonix.profitness.domain.challenges.CreateEventChallengeRequest
import com.avonix.profitness.domain.challenges.UpdateEventChallengeRequest

interface ChallengeRepository {

    /** Aktif public challenge'lar + benim join/progress durumum. */
    suspend fun listPublicChallenges(limit: Int = 50, offset: Int = 0): Result<List<ChallengeSummary>>

    /** Oturum sahibinin katıldığı (aktif + bitmiş) challenge'lar. */
    suspend fun listMyChallenges(): Result<List<ChallengeSummary>>

    /** Tek challenge detayı + katılımcı leaderboard (top 50) + event modunda movements. */
    suspend fun getChallengeDetail(challengeId: String): Result<ChallengeDetail>

    /** Yeni metric challenge yarat. Döner: yeni challenge id. */
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

    /** Yeni event challenge yarat (physical / online / movement_list). Döner: yeni challenge id. */
    suspend fun createEventChallenge(req: CreateEventChallengeRequest): Result<String>

    /** Challenge'a katıl. Private ise password zorunlu. */
    suspend fun joinChallenge(challengeId: String, password: String? = null): Result<Boolean>

    /** Challenge'dan ayrıl. */
    suspend fun leaveChallenge(challengeId: String): Result<Boolean>

    /**
     * Tüm aktif challenge'larımdaki progress'i sunucuda yeniden hesaplat.
     * Workout bitince çağrılır. Döner: yeni tamamlanan challenge sayısı.
     */
    suspend fun refreshMyProgress(): Result<Int>

    /** Event movement'ı tamamlandı olarak işaretle (idempotent). */
    suspend fun completeMovement(challengeId: String, movementId: String): Result<Unit>

    /** Event movement tamamlama işaretini kaldır (idempotent). */
    suspend fun uncompleteMovement(challengeId: String, movementId: String): Result<Unit>

    /** Tek seferde birden çok movement'ı tamamla. Döner: yeni kayıt sayısı. */
    suspend fun completeMultipleMovements(
        challengeId: String,
        movementIds: List<String>
    ): Result<Int>

    /** Verilen gün için katıldığım event challenge'lar (dashboard banner). */
    suspend fun listMyEventsForDate(dateIso: String): Result<List<ChallengeSummary>>

    /** Yaklaşan event'ler (bugünden p_days sonrasına kadar). */
    suspend fun listMyUpcomingEvents(days: Int = 7): Result<List<ChallengeSummary>>

    /** Physical/Online event'lerde manuel ilerleme ekler. Döner: yeni toplam manual_progress. */
    suspend fun addManualProgress(challengeId: String, amount: Long): Result<Long>

    /** Sahip → event challenge alanlarını günceller. */
    suspend fun updateEventChallenge(req: UpdateEventChallengeRequest): Result<Unit>

    /** Sahip → event challenge'ı tamamen siler. */
    suspend fun deleteEventChallenge(challengeId: String): Result<Unit>
}
