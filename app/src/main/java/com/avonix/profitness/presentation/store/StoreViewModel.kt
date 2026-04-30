package com.avonix.profitness.presentation.store

import androidx.lifecycle.viewModelScope
import com.avonix.profitness.core.BaseViewModel
import com.avonix.profitness.data.store.BillingProduct
import com.avonix.profitness.data.store.BillingUsage
import com.avonix.profitness.data.store.UserPlan
import com.avonix.profitness.data.store.UserPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── State ─────────────────────────────────────────────────────────────────────

data class StoreState(
    val plan       : UserPlan = UserPlan.FREE,
    val credits    : Int      = UserPlanRepository.FREE_STARTER_CREDITS,
    val status     : String   = "free",
    val products   : List<BillingProduct> = emptyList(),
    val recentUsage: List<BillingUsage> = emptyList(),
    val pendingOrderId: String? = null,
    val pendingOrderMessage: String? = null,
    val sandboxAvailable: Boolean = false,
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
        viewModelScope.launch {
            planRepository.billingSnapshotFlow.collect { snapshot ->
                updateState {
                    it.copy(
                        plan = snapshot.plan,
                        credits = snapshot.credits,
                        status = snapshot.status,
                        products = snapshot.products,
                        recentUsage = snapshot.recentUsage
                    )
                }
            }
        }
        viewModelScope.launch {
            runCatching { planRepository.refresh() }
        }
    }

    fun setYearly(isYearly: Boolean) {
        updateState { it.copy(isYearly = isYearly) }
    }

    fun purchasePlan(plan: UserPlan) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            runCatching { planRepository.upgradePlan(plan, uiState.value.isYearly) }
                .onSuccess { result ->
                    updateState {
                        it.copy(
                            pendingOrderId = result.orderId,
                            pendingOrderMessage = result.message,
                            sandboxAvailable = result.sandboxAvailable
                        )
                    }
                    sendEvent(StoreEvent.ShowToast(result.message))
                }
                .onFailure {
                    sendEvent(StoreEvent.ShowToast(it.message ?: "Bir hata oluştu, tekrar dene."))
                }
            updateState { it.copy(isLoading = false) }
        }
    }

    fun cancelPlan() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            runCatching { planRepository.downgradeFree() }
                .onSuccess {
                    sendEvent(StoreEvent.ShowToast("İptal işlemi ödeme sağlayıcısı bağlanınca yönetilecek."))
                }
                .onFailure {
                    sendEvent(StoreEvent.ShowToast(it.message ?: "Bir hata oluştu, tekrar dene."))
                }
            updateState { it.copy(isLoading = false) }
        }
    }

    fun purchaseCredits(amount: Int) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            runCatching { planRepository.addCredits(amount) }
                .onSuccess { result ->
                    updateState {
                        it.copy(
                            pendingOrderId = result.orderId,
                            pendingOrderMessage = result.message,
                            sandboxAvailable = result.sandboxAvailable
                        )
                    }
                    sendEvent(StoreEvent.ShowToast(result.message))
                }
                .onFailure {
                    sendEvent(StoreEvent.ShowToast("Bir hata oluştu, tekrar dene."))
                }
            updateState { it.copy(isLoading = false) }
        }
    }

    fun completeSandboxCheckout() {
        val orderId = uiState.value.pendingOrderId ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            runCatching { planRepository.completeSandboxCheckout(orderId) }
                .onSuccess { result ->
                    updateState {
                        it.copy(
                            pendingOrderId = null,
                            pendingOrderMessage = null,
                            sandboxAvailable = false
                        )
                    }
                    sendEvent(StoreEvent.ShowToast(result.message))
                }
                .onFailure {
                    sendEvent(StoreEvent.ShowToast(it.message ?: "Sandbox satın alma tamamlanamadı."))
                }
            updateState { it.copy(isLoading = false) }
        }
    }

    fun dismissPendingOrder() {
        updateState {
            it.copy(
                pendingOrderId = null,
                pendingOrderMessage = null,
                sandboxAvailable = false
            )
        }
    }
}
