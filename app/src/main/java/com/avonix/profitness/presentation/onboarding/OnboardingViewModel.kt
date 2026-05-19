package com.avonix.profitness.presentation.onboarding

import androidx.lifecycle.viewModelScope
import com.avonix.profitness.core.BaseViewModel
import com.avonix.profitness.data.ai.AiAccessException
import com.avonix.profitness.data.ai.AiToolType
import com.avonix.profitness.data.ai.GeminiRepository
import com.avonix.profitness.data.profile.ProfileRepository
import com.avonix.profitness.data.program.ManualDayInput
import com.avonix.profitness.data.program.ManualExerciseInput
import com.avonix.profitness.data.program.ProgramRepository
import com.avonix.profitness.domain.model.ExerciseItem
import com.avonix.profitness.domain.model.ExerciseNameRules
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class OnboardingState(
    val step        : Int     = 0,
    val name        : String  = "",
    val avatar      : String  = "🏋️",
    val gender      : String  = "male",
    val birthDigits : String  = "",
    val heightText  : String  = "",
    val weightText  : String  = "",
    val fitnessGoal : String  = "",
    val sportBranch : String  = "Fitness",
    val experienceLevel: String = "Başlangıç",
    val weeklyDays  : String  = "4",
    val isSaving    : Boolean = false,
    val isGeneratingProgram: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val nameError   : String? = null,
    val avatarError : String? = null,
    val programError: String? = null
)

sealed class OnboardingEvent {
    object NavigateToDashboard : OnboardingEvent()
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val programRepository: ProgramRepository,
    private val geminiRepository : GeminiRepository,
    private val supabase         : SupabaseClient
) : BaseViewModel<OnboardingState, OnboardingEvent>(OnboardingState()) {
    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    fun nextStep() {
        val s = uiState.value
        if (s.step == 0 && s.name.isBlank()) {
            updateState { it.copy(nameError = "İsim alanı boş bırakılamaz.") }
            return
        }
        updateState { it.copy(step = (it.step + 1).coerceAtMost(6), nameError = null) }
    }

    fun prevStep() {
        updateState { it.copy(step = (it.step - 1).coerceAtLeast(0), nameError = null, programError = null) }
    }

    fun setName(v: String)        { updateState { it.copy(name = v, nameError = null) } }
    fun setAvatar(v: String)      { updateState { it.copy(avatar = v, avatarError = null) } }
    fun setGender(v: String)      { updateState { it.copy(gender = v) } }
    fun setBirthDigits(v: String) { val d = v.filter { it.isDigit() }; if (d.length <= 8) updateState { it.copy(birthDigits = d) } }
    fun setHeight(v: String)      { val d = v.filter { it.isDigit() }; if (d.length <= 3) updateState { it.copy(heightText = d) } }
    fun setWeight(v: String)      { val d = v.filter { it.isDigit() }; if (d.length <= 3) updateState { it.copy(weightText = d) } }
    fun setFitnessGoal(v: String) { updateState { it.copy(fitnessGoal = v) } }
    fun setSportBranch(v: String) { updateState { it.copy(sportBranch = v) } }
    fun setExperienceLevel(v: String) { updateState { it.copy(experienceLevel = v) } }
    fun setWeeklyDays(v: String)  { updateState { it.copy(weeklyDays = v) } }
    fun setAvatarError(message: String) { updateState { it.copy(avatarError = message, isUploadingAvatar = false) } }

    fun uploadPhoto(imageBytes: ByteArray) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: run {
            updateState { it.copy(avatarError = "FotoÄŸraf yÃ¼klemek iÃ§in giriÅŸ yapmalÄ±sÄ±n.") }
            return
        }
        updateState { it.copy(isUploadingAvatar = true, avatarError = null) }
        viewModelScope.launch {
            profileRepository.uploadProfilePhoto(userId, imageBytes)
                .onSuccess { url ->
                    updateState { it.copy(avatar = url, isUploadingAvatar = false) }
                }
                .onFailure {
                    updateState {
                        it.copy(
                            isUploadingAvatar = false,
                            avatarError = "FotoÄŸraf yÃ¼klenemedi. Tekrar dene."
                        )
                    }
                }
        }
    }

    val bmi: Double
        get() = calculateBmi(uiState.value.heightText.toDoubleOrNull() ?: 0.0, uiState.value.weightText.toDoubleOrNull() ?: 0.0)

    val bodyFatPct: Double
        get() = estimateBodyFat(bmi, uiState.value.gender, uiState.value.birthDigits)

    fun saveAndFinish() {
        updateState { it.copy(isSaving = true, programError = null) }
        viewModelScope.launch {
            saveProfileOnly()
            updateState { it.copy(isSaving = false) }
            sendEvent(OnboardingEvent.NavigateToDashboard)
        }
    }

    fun saveAndGeneratePersonalProgram() {
        if (uiState.value.isGeneratingProgram) return
        updateState { it.copy(isSaving = true, isGeneratingProgram = true, programError = null) }

        viewModelScope.launch {
            val userId = supabase.auth.currentUserOrNull()?.id
            if (userId == null) {
                updateState { it.copy(isSaving = false, isGeneratingProgram = false) }
                sendEvent(OnboardingEvent.NavigateToDashboard)
                return@launch
            }

            saveProfileOnly()

            val baseExercises = programRepository.getAllExercises().getOrNull().orEmpty()
            val prompt = buildPersonalProgramPrompt(uiState.value, bmi, bodyFatPct)
            val systemPrompt = """
Sen bilimsel temelli, güvenli ve kişiye özel antrenman programı oluşturan uzman bir fitness koçusun.
Kullanıcının spor dalı, seviyesi, boy/kilo, BMI, tahmini yağ oranı ve hedefine göre gerçekçi program yaz.
SADECE ham JSON döndür. Markdown, açıklama ve kod bloğu kullanma.
JSON FORMAT:
{"name":"...","days":[{"title":"Gün 1 - ...","isRestDay":false,"exercises":[{"exerciseName":"Bench Press","sets":4,"reps":10,"restSeconds":90,"targetMuscle":"Göğüs","category":"Serbest Ağırlık"}]},{"title":"Dinlenme","isRestDay":true,"exercises":[]}]}
            """.trimIndent()

            val result = geminiRepository.chat(emptyList(), prompt, systemPrompt, AiToolType.PROGRAM_GENERATE_TEXT)
            val rawJson = result.getOrNull()
            if (rawJson == null) {
                val failure = result.exceptionOrNull()
                updateState {
                    it.copy(
                        isSaving = false,
                        isGeneratingProgram = false,
                        programError = when (failure) {
                            is AiAccessException -> failure.message.ifBlank {
                                "Bu işlem için AI kredisi veya abonelik gerekiyor."
                            }
                            else -> "AI program oluşturamadı. Plan ekranından tekrar deneyebilirsin."
                        }
                    )
                }
                return@launch
            }

            val created = createProgramFromAiJson(userId, rawJson, baseExercises)
            if (!created) {
                updateState {
                    it.copy(
                        isSaving = false,
                        isGeneratingProgram = false,
                        programError = "Program ayrıştırılamadı. Plan ekranından AI ile tekrar oluşturabilirsin."
                    )
                }
                return@launch
            }

            updateState { it.copy(isSaving = false, isGeneratingProgram = false) }
            sendEvent(OnboardingEvent.NavigateToDashboard)
        }
    }

    private suspend fun saveProfileOnly() {
        val s = uiState.value
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        val combinedGoal = buildString {
            append(s.fitnessGoal.trim().ifEmpty { "Fit olmak" })
            append(" • Spor: ${s.sportBranch}")
            append(" • Seviye: ${s.experienceLevel}")
            append(" • Haftada ${s.weeklyDays} gün")
        }

        profileRepository.updateProfile(
            userId      = userId,
            displayName = s.name.trim().ifEmpty { "Kullanıcı" },
            avatar      = s.avatar,
            fitnessGoal = combinedGoal,
            heightCm    = s.heightText.toDoubleOrNull() ?: 0.0,
            weightKg    = s.weightText.toDoubleOrNull() ?: 0.0,
            gender      = s.gender,
            birthDate   = birthDigitsToDbDate(s.birthDigits)
        )
    }

    private fun buildPersonalProgramPrompt(state: OnboardingState, bmi: Double, bodyFat: Double): String {
        val age = ageFromBirthDigits(state.birthDigits)
        return """
Kullanıcı profili:
- Spor dalı: ${state.sportBranch}
- Deneyim seviyesi: ${state.experienceLevel}
- Haftalık antrenman günü: ${state.weeklyDays}
- Hedef: ${state.fitnessGoal.ifBlank { "Fit olmak" }}
- Cinsiyet: ${state.gender}
- Yaş: ${age?.toString() ?: "belirtilmedi"}
- Boy: ${state.heightText.ifBlank { "belirtilmedi" }} cm
- Kilo: ${state.weightText.ifBlank { "belirtilmedi" }} kg
- BMI/VKI: ${if (bmi > 0) "%.1f (${bmiLabel(bmi)})".format(bmi) else "belirtilmedi"}
- Tahmini yağ oranı: ${if (bodyFat > 0) "%.1f%%".format(bodyFat) else "belirtilmedi"}

Kurallar:
- Program gerçekten bu profile göre kişiselleştirilsin; hazır jenerik split yazma.
- ${state.sportBranch} dalına özel kuvvet, kondisyon, mobilite ve sakatlık riskini azaltan destek çalışmaları ekle.
- ${state.experienceLevel} seviyesine uygun hacim, set/tekrar ve dinlenme seç.
- BMI ve yağ oranına göre hedefi ayarla: kilo verme/yağ yakma varsa metabolik ama sürdürülebilir yoğunluk; kas kazanma varsa progresif kuvvet ve hipertrofi dengesi.
- Haftalık gün sayısı tam olarak ${state.weeklyDays} antrenman günü olsun, kalan günler dinlenme veya aktif toparlanma olabilir.
- Egzersiz adlarını tek tek, anlaşılır ve uygulanabilir yaz. Bir alanda iki alternatif verme.
- Her egzersiz için sets, reps ve restSeconds sayı olsun.
- Sadece JSON döndür.
        """.trimIndent()
    }

    private suspend fun createProgramFromAiJson(
        userId: String,
        rawJson: String,
        baseExercises: List<ExerciseItem>
    ): Boolean {
        val cleaned = rawJson
            .replace(Regex("```[a-zA-Z]*\\s*"), "")
            .replace("```", "")
            .trim()
        val jsonCandidate = Regex("\\{[\\s\\S]*\\}").find(cleaned)?.value ?: return false
        val rootObj = runCatching { jsonParser.parseToJsonElement(jsonCandidate).jsonObject }.getOrNull() ?: return false
        val programName = rootObj["name"]?.jsonPrimitive?.contentOrNull ?: "Kişisel AI Programı"
        val daysArray = rootObj["days"] as? JsonArray ?: return false

        val currentMap = baseExercises.associateBy { ExerciseNameRules.normalizedKey(it.name) }.toMutableMap()
        val currentMapEn = baseExercises.filter { it.nameEn.isNotBlank() }
            .associateBy { ExerciseNameRules.normalizedKey(it.nameEn) }
            .toMutableMap()

        val days = daysArray.map { dayEl ->
            val dayObj = dayEl.jsonObject
            val title = dayObj["title"]?.jsonPrimitive?.contentOrNull ?: "Gün"
            val isRest = dayObj["isRestDay"]?.jsonPrimitive?.booleanOrNull ?: false
            if (isRest) {
                ManualDayInput(title = title, isRestDay = true)
            } else {
                val exArray = dayObj["exercises"] as? JsonArray
                val exercises = exArray?.mapIndexedNotNull { exIdx, exEl ->
                    val exObj = exEl.jsonObject
                    val exName = exObj["exerciseName"]?.jsonPrimitive?.contentOrNull ?: return@mapIndexedNotNull null
                    val sets = flexInt(exObj, "sets", 3)
                    val reps = flexInt(exObj, "reps", 10)
                    val targetMuscle = exObj["targetMuscle"]?.jsonPrimitive?.contentOrNull ?: "Genel"
                    val category = exObj["category"]?.jsonPrimitive?.contentOrNull ?: "Serbest Ağırlık"
                    val exercise = findExerciseByName(exName, currentMap, currentMapEn)
                        ?: programRepository.addExercise(exName, exName, targetMuscle, category, sets, reps)
                            .getOrNull()
                            ?.also {
                                currentMap[ExerciseNameRules.normalizedKey(it.name)] = it
                                if (it.nameEn.isNotBlank()) currentMapEn[ExerciseNameRules.normalizedKey(it.nameEn)] = it
                            }

                    exercise?.let {
                        ManualExerciseInput(
                            exerciseId = it.id,
                            sets = sets,
                            reps = reps,
                            restSeconds = flexInt(exObj, "restSeconds", 90),
                            orderIndex = exIdx
                        )
                    }
                }.orEmpty()
                ManualDayInput(title = title, isRestDay = false, exercises = exercises)
            }
        }

        if (days.none { !it.isRestDay && it.exercises.isNotEmpty() }) return false
        return programRepository.createManual(userId, programName, days).isSuccess
    }

    private fun flexInt(obj: JsonObject, key: String, default: Int): Int {
        val el = obj[key]?.jsonPrimitive ?: return default
        return el.intOrNull
            ?: el.contentOrNull?.split("-", "/", "–")?.firstOrNull()?.trim()?.toIntOrNull()
            ?: default
    }

    private fun findExerciseByName(
        rawName: String,
        trMap: Map<String, ExerciseItem>,
        enMap: Map<String, ExerciseItem>
    ): ExerciseItem? {
        val key = ExerciseNameRules.normalizedKey(rawName)
        trMap[key]?.let { return it }
        enMap[key]?.let { return it }
        return (trMap.entries + enMap.entries)
            .firstOrNull { it.key.contains(key) || key.contains(it.key) }
            ?.value
    }

    private fun birthDigitsToDbDate(digits: String): String =
        if (digits.length == 8) "${digits.substring(4)}-${digits.substring(2, 4)}-${digits.substring(0, 2)}" else ""

    private fun calculateBmi(heightCm: Double, weightKg: Double): Double =
        if (heightCm > 0 && weightKg > 0) weightKg / ((heightCm / 100.0) * (heightCm / 100.0)) else 0.0

    private fun estimateBodyFat(bmi: Double, gender: String, birthDigits: String): Double {
        if (bmi <= 0) return 0.0
        val age = ageFromBirthDigits(birthDigits) ?: 25
        val sex = if (gender.lowercase() == "male") 1 else 0
        return (1.2 * bmi + 0.23 * age - 10.8 * sex - 5.4).coerceIn(3.0, 60.0)
    }

    private fun ageFromBirthDigits(digits: String): Int? =
        runCatching {
            if (digits.length != 8) return null
            val date = LocalDate.parse(
                "${digits.substring(4)}-${digits.substring(2, 4)}-${digits.substring(0, 2)}",
                DateTimeFormatter.ISO_LOCAL_DATE
            )
            Period.between(date, LocalDate.now()).years.takeIf { it in 8..100 }
        }.getOrNull()

    private fun bmiLabel(bmi: Double): String = when {
        bmi <= 0 -> "Bilinmiyor"
        bmi < 18.5 -> "Zayıf"
        bmi < 25.0 -> "Normal"
        bmi < 30.0 -> "Fazla kilolu"
        else -> "Obez"
    }
}
