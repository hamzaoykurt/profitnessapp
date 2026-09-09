package com.cosmibit.profitness.data.workout

import androidx.room.Room
import com.cosmibit.profitness.data.local.AppDatabase
import com.cosmibit.profitness.data.local.entity.SetCompletionEntity
import com.cosmibit.profitness.data.local.entity.WorkoutLogEntity
import com.cosmibit.profitness.data.local.entity.ExerciseLogEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
@SQLiteMode(SQLiteMode.Mode.LEGACY)
class WorkoutUndoPersistenceTest {
    @Test fun `old workout response cannot overwrite undo or re-completion`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val dao = db.workoutDao()
            val parent = WorkoutLogEntity("log", "user", "monday", "2026-09-07", synced = true)
            val old = ExerciseLogEntity("old", "log", "pullup", 2, 10, synced = false)
            dao.insertLog(parent)
            dao.insertExerciseLog(old)
            dao.markWeeklyExerciseIncomplete("user", "monday", "pullup", "2026-09-07")
            dao.acknowledgeExercise(old)
            dao.replaceWeeklyData("user", "2026-09-07", listOf(parent), listOf(old.copy(synced = true)))
            assertTrue(dao.getWeeklyCompletionPairs("user", "2026-09-07").isEmpty())
            assertTrue(dao.getWorkoutDates("user").isEmpty())
            val deletion = dao.getUnsyncedExerciseLogs().single()
            dao.acknowledgeExercise(deletion)
            dao.replaceWeeklyData("user", "2026-09-07", listOf(parent), listOf(old.copy(synced = true)))
            assertTrue(dao.getWeeklyCompletionPairs("user", "2026-09-07").isEmpty())
            dao.insertExerciseLog(old.copy(id = "new"))
            dao.acknowledgeExercise(deletion)
            dao.replaceWeeklyData("user", "2026-09-07", listOf(parent), listOf(old.copy(synced = true)))
            assertEquals("new", dao.getExerciseLogsForWorkout("log").single().id)
            assertEquals(listOf("2026-09-07"), dao.getWorkoutDates("user"))
        } finally { db.close() }
    }

    private fun row(exercise: String, day: String = "monday", index: Int = 0) =
        SetCompletionEntity("user", exercise, day, index,
            if (day == "monday") "2026-09-07" else "2026-09-08",
            repsActual = 10, synced = true, dirty = false, updatedAtMs = 1)

    @Test fun `undo three exercises survives stale sync another completion and reopening database`() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "undo-regression.db"
        context.deleteDatabase(name)
        fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .allowMainThreadQueries().build()
        var db = open()
        try {
            val dao = db.setCompletionDao()
            val oldRows = listOf(row("run"), row("pullup"), row("pullup", index = 1), row("swim", "tuesday"))
            dao.upsertAll(oldRows)
            assertEquals(3, dao.observeForWeek("user", "2026-09-07").first().toSetCompletionMap().size)
            oldRows.distinctBy { it.exerciseId }.forEach {
                dao.deleteAllForExercise(it.userId, it.exerciseId, it.programDayId, "2026-09-07")
            }
            // An upload started before undo finishes after it.
            oldRows.forEach { dao.acknowledge(it) }
            dao.mergeRemote(oldRows)
            assertTrue(dao.observeForWeek("user", "2026-09-07").first().isEmpty())
            // Successful deletion followed by an older in-flight pull.
            dao.getDirtyForUser("user").forEach { dao.acknowledge(it) }
            dao.mergeRemote(oldRows)
            dao.insert(row("pushup").copy(synced = false, dirty = true))
            assertEquals(setOf("pushup"), dao.observeForWeek("user", "2026-09-07").first().map { it.exerciseId }.toSet())
            db.close()
            db = open()
            assertEquals(setOf("pushup"), db.setCompletionDao().observeForWeek("user", "2026-09-07").first().map { it.exerciseId }.toSet())
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun `late weight and duration drafts cannot complete an undone set`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            val dao = db.setCompletionDao()
            dao.insert(row("pullup"))
            dao.insert(row("run"))
            dao.deleteAllForExercise("user", "pullup", "monday", "2026-09-07")
            dao.deleteAllForExercise("user", "run", "monday", "2026-09-07")
            dao.upsertWeight("user", "pullup", "monday", 0, "2026-09-07", 80f)
            dao.upsertActivityMetrics("user", "run", "monday", 0, "2026-09-07", 720, null)
            assertTrue(dao.observeForWeek("user", "2026-09-07").first().isEmpty())
            dao.upsertRepsActual("user", "pullup", "monday", 0, "2026-09-07", 10)
            assertEquals(listOf("pullup"), dao.observeForWeek("user", "2026-09-07").first().map { it.exerciseId })
        } finally { db.close() }
    }
}
