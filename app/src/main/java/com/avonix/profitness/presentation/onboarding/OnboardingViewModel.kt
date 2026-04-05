package com.avonix.profitness.presentation.onboarding

import androidx.lifecycle.viewModelScope
import com.avonix.profitness.core.BaseViewModel
import com.avonix.profitness.data.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingState(
    val step        : Int     = 0,
    val name        : String  = "",
    val avatar      : String  = "🏋️",
    val gender      : String  = "male",
    val birthDigits : String  = "",   // DDMMYYYY rakamları
    val heightText  : String  = "",
    val weightText  : String  = "",
    val fitnessGoal : String  = "",
    val isSaving    : Boolean = false,
    val nameError   : String? = null
)

sealed class OnboardingEvent {
    object NavigateToDashboard : OnboardingEvent()
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val supabase         : SupabaseClient
) : BaseViewModel<OnboardingState, OnboardingEvent>(OnboardingState()) {

    fun nextStep() {
        val s = uiState.value
        if (s.step == 0 && s.name.isBlank()) {
            updateState { it.copy(nameError = "İsim alanı boş bırakılamaz.") }
            return
        }
        updateState { it.copy(step = it.step + 1, nameError = null) }
    }

    fun prevStep() {
        updateState { it.copy(step = (it.step - 1).coerceAtLeast(0), nameError = null) }
    }

    fun setName(v: String)        { updateState { it.copy(name = v, nameError = null) } }
    fun setAvatar(v: String)      { updateState { it.copy(avatar = v) } }
    fun setGender(v: String)      { updateState { it.copy(gender = v) } }
    fun setBirthDigits(v: String) { val d = v.filter { it.isDigit() }; if (d.length <= 8) updateState { it.copy(birthDigits = d) } }
    fun setHeight(v: String)      { val d = v.filter { it.isDigit() }; if (d.length <= 3) updateState { it.copy(heightText = d) } }
    fun setWeight(v: String)      { val d = v.filter { it.isDigit() }; if (d.length <= 3) updateState { it.copy(weightText = d) } }
    fun setFitnessGoal(v: String) { updateState { it.copy(fitnessGoal = v) } }

    /** Son adımda profili kaydeder ve dashboard'a yönlendirir. */
    fun saveAndFinish() {
        val s = uiState.value
        updateState { it.copy(isSaving = true) }

        viewModelScope.launch {
            val userId = supabase.auth.currentUserOrNull()?.id
            if (userId != null) {
                val dbBirthDate = if (s.birthDigits.length == 8) {
                    "${s.birthDigits.substring(4)}-${s.birthDigits.substring(2, 4)}-${s.birthDigits.substring(0, 2)}"
                } else ""

                profileRepository.updateProfile(
                    userId      = userId,
                    displayName = s.name.trim().ifEmpty { "Kullanıcı" },
                    avatar      = s.avatar,
                    fitnessGoal = s.fitnessGoal.trim(),
                    heightCm    = s.heightText.toDoubleOrNull() ?: 0.0,
                    weightKg    = s.weightText.toDoubleOrNull() ?: 0.0,
                    gender      = s.gender,
                    birthDate   = dbBirthDate
                )
            }
            updateState { it.copy(isSaving = false) }
            sendEvent(OnboardingEvent.NavigateToDashboard)
        }
    }
}
