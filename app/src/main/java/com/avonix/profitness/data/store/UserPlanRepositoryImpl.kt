package com.avonix.profitness.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.planDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "profitness_user_plan")

@Singleton
class UserPlanRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPlanRepository {

    private object Keys {
        val PLAN    = stringPreferencesKey("plan")
        val CREDITS = intPreferencesKey("ai_credits")
    }

    override val planFlow: Flow<UserPlan> = context.planDataStore.data.map { prefs ->
        UserPlan.valueOf(
            prefs[Keys.PLAN] ?: UserPlan.FREE.name
        )
    }

    override val creditsFlow: Flow<Int> = context.planDataStore.data.map { prefs ->
        prefs[Keys.CREDITS] ?: UserPlanRepository.FREE_STARTER_CREDITS
    }

    override suspend fun upgradePlan(plan: UserPlan) {
        context.planDataStore.edit { prefs ->
            prefs[Keys.PLAN] = plan.name
            // Ücretli planlarda sınırsız AI — kredi bakiyesini temizle
            if (plan != UserPlan.FREE) prefs[Keys.CREDITS] = 0
        }
    }

    override suspend fun addCredits(amount: Int) {
        context.planDataStore.edit { prefs ->
            val current = prefs[Keys.CREDITS] ?: UserPlanRepository.FREE_STARTER_CREDITS
            prefs[Keys.CREDITS] = (current + amount).coerceAtLeast(0)
        }
    }

    override suspend fun consumeCredit(): Boolean {
        val currentPlan = planFlow.first()
        // Ücretli plan → sınırsız, kredi tüketme
        if (currentPlan != UserPlan.FREE) return true

        var consumed = false
        context.planDataStore.edit { prefs ->
            val current = prefs[Keys.CREDITS] ?: 0
            if (current > 0) {
                prefs[Keys.CREDITS] = current - 1
                consumed = true
            }
        }
        return consumed
    }

    override suspend fun downgradeFree() {
        context.planDataStore.edit { prefs ->
            prefs[Keys.PLAN]    = UserPlan.FREE.name
            prefs[Keys.CREDITS] = UserPlanRepository.FREE_STARTER_CREDITS
        }
    }

    override suspend fun refundCredit() {
        val currentPlan = planFlow.first()
        if (currentPlan != UserPlan.FREE) return   // no-op for paid plans
        context.planDataStore.edit { prefs ->
            val current = prefs[Keys.CREDITS] ?: 0
            prefs[Keys.CREDITS] = current + 1
        }
    }
}
