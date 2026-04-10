package com.avonix.profitness.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.avonix.profitness.data.local.entity.WeightLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightLogDao {

    // ── Write ──────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WeightLogEntity)

    @Update
    suspend fun update(entry: WeightLogEntity)

    @Query("DELETE FROM weight_logs WHERE id = :id AND user_id = :userId")
    suspend fun delete(id: String, userId: String)

    // ── Read — reaktif Flow (Room değişince UI otomatik güncellenir) ───────────

    /** Kullanıcının TÜM kayıtları, en yeniden en eskiye. */
    @Query("""
        SELECT * FROM weight_logs
        WHERE user_id = :userId
        ORDER BY recorded_at DESC
    """)
    fun observeAll(userId: String): Flow<List<WeightLogEntity>>

    /** Son [limit] kayıt — ProfileScreen özet kartı için. */
    @Query("""
        SELECT * FROM weight_logs
        WHERE user_id = :userId
        ORDER BY recorded_at DESC
        LIMIT :limit
    """)
    suspend fun getRecent(userId: String, limit: Int = 30): List<WeightLogEntity>

    /** Belirli tarih aralığındaki kayıtlar (grafik / haftalık özet). */
    @Query("""
        SELECT * FROM weight_logs
        WHERE user_id = :userId
          AND recorded_at >= :fromInclusive
          AND recorded_at <= :toInclusive
        ORDER BY recorded_at ASC
    """)
    suspend fun getInRange(userId: String, fromInclusive: String, toInclusive: String): List<WeightLogEntity>

    /** Henüz Supabase'e push edilmemiş kayıtlar. */
    @Query("SELECT * FROM weight_logs WHERE user_id = :userId AND synced = 0")
    suspend fun getUnsynced(userId: String): List<WeightLogEntity>

    /** Sync tamamlandığında tümünü synced = true yap. */
    @Query("UPDATE weight_logs SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)
}
