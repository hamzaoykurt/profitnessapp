package com.avonix.profitness.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * AppModule — DI bindings for the app.
 * Currently a placeholder following Clean Architecture;
 * Firebase / Gemini repositories will be bound here in future milestones.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // TODO: Provide Firebase Auth, Firestore, Gemini service bindings here
    // @Provides @Singleton fun provideFirebaseAuth() = Firebase.auth
    // @Provides @Singleton fun provideFirestore() = Firebase.firestore
}
