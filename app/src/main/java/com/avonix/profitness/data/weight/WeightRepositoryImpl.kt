package com.avonix.profitness.data.weight

import com.avonix.profitness.data.local.dao.WeightLogDao
import com.avonix.profitness.data.local.entity.WeightLogEntity
import com.avonix.profitness.data.weight.dto.WeightLogDto
import com.avonix.profitness.data.weight.dto.toDto
import com.avonix.profitness.data.weight.dto.toEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WeightRepositoryImpl @Inject constructor(
    private val dao      : WeightLogDao,
    private val supabase : SupabaseClient
) : WeightRepository {

    // ── Reaktif ───────────────────────────────────────────────────────────────

    override fun observeAll(userId: String): Flow<List<WeightLogEntity>> =
        dao.observeAll(userId)

    // ── Read ──────────────────────────────────────────────────────────────────

    override suspend fun getRecent(userId: String, limit: Int) = withContext(Dispatchers.IO) {
        runCatching { dao.getRecent(userId, limit) }
    }

    override suspend fun getInRange(userId: String, from: String, to: String) =
        withContext(Dispatchers.IO) {
            runCatching { dao.getInRange(userId, from, to) }
        }

    // ── Write ─────────────────────────────────────────────────────────────────

    override suspend fun addEntry(entry: WeightLogEntity) = withContext(Dispatchers.IO) {
        runCatching {
            dao.upsert(entry)
            // Optimistic remote push — başarısız olursa synced=false kalır, daha sonra pushPending ile yeniden denenir
            runCatching {
                supabase.from("weight_logs").upsert(entry.toDto())
                dao.markSynced(listOf(entry.id))
            }
        }
    }

    override suspend fun updateEntry(entry: WeightLogEntity) = withContext(Dispatchers.IO) {
        runCatching {
            dao.upsert(entry.copy(synced = false))
            runCatching {
                supabase.from("weight_logs").upsert(entry.toDto())
                dao.markSynced(listOf(entry.id))
            }
        }
    }

    override suspend fun deleteEntry(id: String, userId: String) = withContext(Dispatchers.IO) {
        runCatching {
            dao.delete(id, userId)
            runCatching {
                supabase.from("weight_logs")
                    .delete { filter { eq("id", id); eq("user_id", userId) } }
            }
        }
    }

    // ── Sync ──────────────────────────────────────────────────────────────────

    override suspend fun pullFromRemote(userId: String) = withContext(Dispatchers.IO) {
        runCatching {
            val remote = supabase.from("weight_logs")
                .select {
                    filter { eq("user_id", userId) }
                    order("recorded_at", Order.DESCENDING)
                    limit(200)
                }
                .decodeList<WeightLogDto>()

            remote.forEach { dto -> dao.upsert(dto.toEntity()) }
        }
    }

    override suspend fun pushPending(userId: String) = withContext(Dispatchers.IO) {
        runCatching {
            val pending = dao.getUnsynced(userId)
            if (pending.isEmpty()) return@runCatching
            supabase.from("weight_logs").upsert(pending.map { it.toDto() })
            dao.markSynced(pending.map { it.id })
        }
    }
}
