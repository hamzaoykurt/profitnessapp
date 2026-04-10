package com.avonix.profitness.data.weight

import com.avonix.profitness.data.local.entity.WeightLogEntity
import kotlinx.coroutines.flow.Flow

interface WeightRepository {

    /** Tüm kayıtları reaktif Flow olarak döner (Room → UI). */
    fun observeAll(userId: String): Flow<List<WeightLogEntity>>

    /** Son [limit] kaydı döner. */
    suspend fun getRecent(userId: String, limit: Int = 30): Result<List<WeightLogEntity>>

    /** Tarih aralığındaki kayıtlar (haftalık/aylık grafik için). */
    suspend fun getInRange(userId: String, from: String, to: String): Result<List<WeightLogEntity>>

    /** Yeni kayıt ekler (Room → sonra Supabase'e push). */
    suspend fun addEntry(entry: WeightLogEntity): Result<Unit>

    /** Mevcut kaydı günceller. */
    suspend fun updateEntry(entry: WeightLogEntity): Result<Unit>

    /** Kaydı siler. */
    suspend fun deleteEntry(id: String, userId: String): Result<Unit>

    /** Supabase'den son kayıtları çekip Room'a yazar (pull sync). */
    suspend fun pullFromRemote(userId: String): Result<Unit>

    /** Synced=false kayıtları Supabase'e push eder. */
    suspend fun pushPending(userId: String): Result<Unit>
}
