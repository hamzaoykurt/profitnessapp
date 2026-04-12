package com.avonix.profitness.presentation.store

import androidx.lifecycle.viewModelScope
import com.avonix.profitness.core.BaseViewModel
import com.avonix.profitness.data.store.UserPlan
import com.avonix.profitness.data.store.UserPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── State ─────────────────────────────────────────────────────────────────────

data class StoreState(
    val plan       : UserPlan = UserPlan.FREE,
    val credits    : Int      = UserPlanRepository.FREE_STARTER_CREDITS,
    val isYearly   : Boolean  = false,
    val isLoading  : Boolean  = false
)

// ── One-time Events ───────────────────────────────────────────────────────────

sealed class StoreEvent {
    data class ShowToast(val message: String) : StoreEvent()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class StoreViewModel @Inject constructor(
    private val planRepository: UserPlanRepository
) : BaseViewModel<StoreState, StoreEvent>(StoreState()) {

    init {
        // Plan ve kredi değişikliklerini tek bir combine ile dinle — her iki Flow
        // DataStore'dan geldiği için main-safe, coroutine boundary yok.
        viewModelScope.launch {
            combine(
                planRepository.planFlow,
                planRepository.creditsFlow
            ) { plan, credits -> plan to credits }
                .collect { (plan, credits) ->
                    updateState { it.copy(plan = plan, credits = credits) }
                }
        }
    }

    fun setYearly(isYearly: Boolean) {
        updateState { it.copy(isYearly = isYearly) }
    }

    fun purchasePlan(plan: UserPlan) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            runCatching { planRepository.upgradePlan(plan) }
                .onSuccess {
                    sendEvent(StoreEvent.ShowToast("${plan.displayName} planı aktif edildi!"))
                }
                .onFailure {
                    sendEvent(StoreEvent.ShowToast("Bir hata oluştu, tekrar dene."))
                }
            updateState { it.copy(isLoading = false) }
        }
    }

    fun cancelPlan() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            runCatching { planRepository.downgradeFree() }
                .onSuccess {
                    sendEvent(StoreEvent.ShowToast("Abonelik iptal edildi. Ücretsiz plana geçildi."))
                }
                .onFailure {
                    sendEvent(StoreEvent.ShowToast("Bir hata oluştu, tekrar dene."))
                }
            updateState { it.copy(isLoading = false) }
        }
    }

    fun purchaseCredits(amount: Int) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            runCatching { planRepository.addCredits(amount) }
                .onSuccess {
                    sendEvent(StoreEvent.ShowToast("$amount AI kredisi eklendi!"))
                }
                .onFailure {
                    sendEvent(StoreEvent.ShowToast("Bir hata oluştu, tekrar dene."))
                }
            updateState { it.copy(isLoading = false) }
        }
    }
}
