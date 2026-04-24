package com.avonix.profitness.di

import android.content.Context
import androidx.room.Room
import com.avonix.profitness.BuildConfig
import com.avonix.profitness.data.ai.GeminiRepository
import com.avonix.profitness.data.ai.GeminiRepositoryImpl
import com.avonix.profitness.data.auth.AuthRepository
import com.avonix.profitness.data.auth.AuthRepositoryImpl
import com.avonix.profitness.core.analytics.AnalyticsTracker
import com.avonix.profitness.core.analytics.NoOpAnalyticsTracker
import com.avonix.profitness.data.challenges.ChallengeRepository
import com.avonix.profitness.data.challenges.ChallengeRepositoryImpl
import com.avonix.profitness.data.discover.DiscoverRepository
import com.avonix.profitness.data.discover.DiscoverRepositoryImpl
import com.avonix.profitness.data.local.AppDatabase
import com.avonix.profitness.data.local.dao.ExerciseDao
import com.avonix.profitness.data.local.dao.ProgramDao
import com.avonix.profitness.data.local.dao.SetCompletionDao
import com.avonix.profitness.data.local.dao.WeightLogDao
import com.avonix.profitness.data.local.dao.WorkoutDao
import com.avonix.profitness.data.leaderboard.LeaderboardRepository
import com.avonix.profitness.data.leaderboard.LeaderboardRepositoryImpl
import com.avonix.profitness.data.profile.ProfileRepository
import com.avonix.profitness.data.profile.ProfileRepositoryImpl
import com.avonix.profitness.data.social.SocialRepository
import com.avonix.profitness.data.social.SocialRepositoryImpl
import com.avonix.profitness.data.program.ProgramRepository
import com.avonix.profitness.data.program.ProgramRepositoryImpl
import com.avonix.profitness.data.sync.SyncManager
import com.avonix.profitness.data.store.UserPlanRepository
import com.avonix.profitness.data.store.UserPlanRepositoryImpl
import com.avonix.profitness.data.weight.WeightRepository
import com.avonix.profitness.data.weight.WeightRepositoryImpl
import com.avonix.profitness.data.workout.WorkoutRepository
import com.avonix.profitness.data.workout.WorkoutRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.FlowType
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindProgramRepository(impl: ProgramRepositoryImpl): ProgramRepository

    @Binds @Singleton
    abstract fun bindWorkoutRepository(impl: WorkoutRepositoryImpl): WorkoutRepository

    @Binds @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds @Singleton
    abstract fun bindWeightRepository(impl: WeightRepositoryImpl): WeightRepository

    @Binds @Singleton
    abstract fun bindUserPlanRepository(impl: UserPlanRepositoryImpl): UserPlanRepository

    @Binds @Singleton
    abstract fun bindLeaderboardRepository(impl: LeaderboardRepositoryImpl): LeaderboardRepository

    @Binds @Singleton
    abstract fun bindDiscoverRepository(impl: DiscoverRepositoryImpl): DiscoverRepository

    @Binds @Singleton
    abstract fun bindSocialRepository(impl: SocialRepositoryImpl): SocialRepository

    @Binds @Singleton
    abstract fun bindChallengeRepository(impl: ChallengeRepositoryImpl): ChallengeRepository

    @Binds @Singleton
    abstract fun bindAnalyticsTracker(impl: NoOpAnalyticsTracker): AnalyticsTracker

    companion object {

        // ── Room Database ────────────────────────────────────────────────────

        @Provides @Singleton
        fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
                .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
                .build()

        @Provides @Singleton fun provideProgramDao(db: AppDatabase): ProgramDao = db.programDao()
        @Provides @Singleton fun provideExerciseDao(db: AppDatabase): ExerciseDao = db.exerciseDao()
        @Provides @Singleton fun provideWorkoutDao(db: AppDatabase): WorkoutDao = db.workoutDao()
        @Provides @Singleton fun provideSetCompletionDao(db: AppDatabase): SetCompletionDao = db.setCompletionDao()
        @Provides @Singleton fun provideWeightLogDao(db: AppDatabase): WeightLogDao = db.weightLogDao()

        // ── Supabase ─────────────────────────────────────────────────────────

        @Provides @Singleton
        fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) { flowType = FlowType.PKCE }
            install(Postgrest)
            install(Storage)
        }

        // ── Gemini ───────────────────────────────────────────────────────────

        @Provides @Singleton
        fun provideGeminiRepository(): GeminiRepository {
            val client = HttpClient(Android) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }
            return GeminiRepositoryImpl(client, BuildConfig.GEMINI_API_KEY)
        }
    }
}
