package com.avonix.profitness.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.avonix.profitness.data.local.dao.ExerciseDao
import com.avonix.profitness.data.local.dao.ProgramDao
import com.avonix.profitness.data.local.dao.SetCompletionDao
import com.avonix.profitness.data.local.dao.WeightLogDao
import com.avonix.profitness.data.local.dao.WorkoutDao
import com.avonix.profitness.data.local.entity.ExerciseEntity
import com.avonix.profitness.data.local.entity.ExerciseLogEntity
import com.avonix.profitness.data.local.entity.ProgramDayEntity
import com.avonix.profitness.data.local.entity.ProgramEntity
import com.avonix.profitness.data.local.entity.ProgramExerciseEntity
import com.avonix.profitness.data.local.entity.SetCompletionEntity
import com.avonix.profitness.data.local.entity.WeightLogEntity
import com.avonix.profitness.data.local.entity.WorkoutLogEntity

@Database(
    entities = [
        ProgramEntity::class,
        ProgramDayEntity::class,
        ProgramExerciseEntity::class,
        ExerciseEntity::class,
        WorkoutLogEntity::class,
        ExerciseLogEntity::class,
        SetCompletionEntity::class,
        WeightLogEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun programDao(): ProgramDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun setCompletionDao(): SetCompletionDao
    abstract fun weightLogDao(): WeightLogDao

    companion object {
        const val NAME = "profitness.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS set_completions (
                        user_id TEXT NOT NULL,
                        exercise_id TEXT NOT NULL,
                        program_day_id TEXT NOT NULL,
                        set_index INTEGER NOT NULL,
                        date TEXT NOT NULL,
                        PRIMARY KEY (user_id, exercise_id, program_day_id, set_index, date)
                    )
                """)
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_set_completions_user_day_date
                    ON set_completions (user_id, program_day_id, date)
                """)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS weight_logs (
                        id          TEXT NOT NULL PRIMARY KEY,
                        user_id     TEXT NOT NULL,
                        weight_kg   REAL NOT NULL,
                        note        TEXT NOT NULL DEFAULT '',
                        recorded_at TEXT NOT NULL,
                        synced      INTEGER NOT NULL DEFAULT 0
                    )
                """)
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_weight_logs_user_recorded
                    ON weight_logs (user_id, recorded_at)
                """)
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE set_completions ADD COLUMN weight_kg REAL DEFAULT NULL")
                database.execSQL("ALTER TABLE set_completions ADD COLUMN reps_actual INTEGER DEFAULT NULL")
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_set_completions_user_exercise_date
                    ON set_completions (user_id, exercise_id, date)
                """)
            }
        }

        /**
         * Migration 4→5: Index isimlerini Room'un auto-generated naming convention'ıyla hizalar.
         *
         * Migration 1-2-3-4 sırasıyla oluşturulan index'ler Room'un beklediği isimlerden farklıydı
         * (ör: "index_set_completions_user_day_date" vs Room'un beklediği
         * "index_set_completions_user_id_program_day_id_date"). Bu uyumsuzluk Room'un schema
         * validation'ında IllegalStateException'a ve uygulama crash'ine yol açıyordu.
         *
         * Çözüm: Eski index'leri bırak ve Room'un entity annotation'larından türettiği
         * isimlerle yeniden oluştur. IF EXISTS / IF NOT EXISTS güvenceleri ile hem migration
         * yolundan hem de fresh-install v4'ten gelen cihazları kapsar.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // set_completions — eski index'leri kaldır
                database.execSQL("DROP INDEX IF EXISTS index_set_completions_user_day_date")
                database.execSQL("DROP INDEX IF EXISTS index_set_completions_user_exercise_date")
                // set_completions — Room'un beklediği isimlerle yeniden oluştur
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_set_completions_user_id_program_day_id_date " +
                    "ON set_completions (user_id, program_day_id, date)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_set_completions_user_id_exercise_id_date " +
                    "ON set_completions (user_id, exercise_id, date)"
                )
                // weight_logs — eski index'i kaldır, yeniden oluştur
                database.execSQL("DROP INDEX IF EXISTS index_weight_logs_user_recorded")
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_weight_logs_user_id_recorded_at " +
                    "ON weight_logs (user_id, recorded_at)"
                )
            }
        }
    }
}
