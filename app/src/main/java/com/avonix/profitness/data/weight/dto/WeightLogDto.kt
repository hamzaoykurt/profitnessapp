package com.avonix.profitness.data.weight.dto

import com.avonix.profitness.data.local.entity.WeightLogEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase `weight_logs` tablosu için DTO.
 *
 * Tablo şeması (Supabase'de bir kez çalıştırılacak):
 * ```sql
 * CREATE TABLE IF NOT EXISTS weight_logs (
 *   id           TEXT PRIMARY KEY,
 *   user_id      UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
 *   weight_kg    DOUBLE PRECISION NOT NULL,
 *   note         TEXT DEFAULT '',
 *   recorded_at  TIMESTAMPTZ NOT NULL,
 *   created_at   TIMESTAMPTZ DEFAULT NOW()
 * );
 * ALTER TABLE weight_logs ENABLE ROW LEVEL SECURITY;
 * CREATE POLICY "Users manage own weight logs"
 *   ON weight_logs FOR ALL USING (auth.uid() = user_id);
 * ```
 */
@Serializable
data class WeightLogDto(
    val id          : String,
    @SerialName("user_id")
    val userId      : String,
    @SerialName("weight_kg")
    val weightKg    : Double,
    val note        : String         = "",
    @SerialName("recorded_at")
    val recordedAt  : String
)

fun WeightLogDto.toEntity(): WeightLogEntity = WeightLogEntity(
    id         = id,
    userId     = userId,
    weightKg   = weightKg,
    note       = note,
    recordedAt = recordedAt,
    synced     = true
)

fun WeightLogEntity.toDto(): WeightLogDto = WeightLogDto(
    id         = id,
    userId     = userId,
    weightKg   = weightKg,
    note       = note,
    recordedAt = recordedAt
)
