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
    version = 3,
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
    }
}
