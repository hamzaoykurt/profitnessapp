package com.avonix.profitness.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Kullanıcının belirli bir tarih/saatte kaydettiği tek bir tartı ölçümü.
 *
 * Offline-first: Room birincil kaynak; [synced] = false iken Supabase'e henüz push edilmedi.
 */
@Entity(
    tableName = "weight_logs",
    indices   = [Index("user_id", "recorded_at")]
)
data class WeightLogEntity(
    @PrimaryKey
    val id          : String,                     // UUID (istemci üretir)
    @ColumnInfo(name = "user_id")
    val userId      : String,
    @ColumnInfo(name = "weight_kg")
    val weightKg    : Double,
    /** Serbest metin not — isteğe bağlı ("sabah", "spor sonrası" vb.) */
    val note        : String   = "",
    /** ISO-8601 datetime string: "2025-04-10T07:30:00" */
    @ColumnInfo(name = "recorded_at")
    val recordedAt  : String,
    val synced      : Boolean  = false
)
