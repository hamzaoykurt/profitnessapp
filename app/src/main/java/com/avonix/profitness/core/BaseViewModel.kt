package com.avonix.profitness.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base ViewModel with typed UI state (S) and one-time events (E).
 *
 * ViewModels that don't need events use Nothing as the event type:
 *   class FooViewModel : BaseViewModel<FooState, Nothing>(FooState())
 *
 * ViewModels with navigation/toast events define a sealed class:
 *   sealed class AuthEvent { object NavigateToDashboard : AuthEvent() }
 *   class AuthViewModel : BaseViewModel<AuthState, AuthEvent>(AuthState())
 */
abstract class BaseViewModel<S : Any, E : Any>(initialState: S) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<E>(extraBufferCapacity = 16)
    val events: SharedFlow<E> = _events.asSharedFlow()

    protected fun updateState(update: (S) -> S) {
        _uiState.update(update)
    }

    protected fun sendEvent(event: E) {
        viewModelScope.launch { _events.emit(event) }
    }
}
