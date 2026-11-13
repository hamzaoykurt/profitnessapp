package com.cosmibit.profitness.presentation.program

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.cosmibit.profitness.core.theme.t
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.cosmibit.profitness.core.theme.LocalAppTheme
import com.cosmibit.profitness.core.theme.SinemaCapsuleIconButton
import com.cosmibit.profitness.core.theme.SinemaIcons
import com.cosmibit.profitness.data.local.dao.ExerciseDao
import com.cosmibit.profitness.data.local.entity.ExerciseEntity
import com.cosmibit.profitness.domain.model.ExerciseNameRules
import com.cosmibit.profitness.presentation.workout.Exercise
import com.cosmibit.profitness.presentation.workout.ExerciseDetailSheet
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Hareket adı (TR/EN) -> katalog kaydı. Görsel yalnızca katalogdaki image_url'den gelir; eşleşmezse boş kalır. */
class ExerciseCatalog(private val byKey: Map<String, ExerciseEntity>) {
    fun find(name: String): ExerciseEntity? {
        if (name.isBlank()) return null
        ExerciseNameRules.splitAlternatives(name).forEach { part ->
            byKey[ExerciseNameRules.normalizedKey(part)]?.let { return it }
        }
        return byKey[ExerciseNameRules.normalizedKey(name)]
    }

    companion object {
        val Empty = ExerciseCatalog(emptyMap())
        fun from(list: List<ExerciseEntity>): ExerciseCatalog {
            val m = HashMap<String, ExerciseEntity>(list.size * 2)
            list.forEach { e ->
                m.putIfAbsent(ExerciseNameRules.normalizedKey(e.name), e)
                if (e.nameEn.isNotBlank()) m.putIfAbsent(ExerciseNameRules.normalizedKey(e.nameEn), e)
            }
            return ExerciseCatalog(m)
        }
    }
}

@HiltViewModel
class ExerciseCatalogViewModel @Inject constructor(exerciseDao: ExerciseDao) : ViewModel() {
    val catalog: StateFlow<ExerciseCatalog> = exerciseDao.observeAll()
        .map { ExerciseCatalog.from(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseCatalog.Empty)
}

@Composable
fun rememberExerciseCatalog(): ExerciseCatalog {
    val vm: ExerciseCatalogViewModel = hiltViewModel()
    return vm.catalog.collectAsStateWithLifecycle().value
}

/** Program hareketi (şablon / paylaşılan JSON) -> ExerciseDetailSheet'in beklediği [Exercise]. */
fun ExerciseCatalog.toViewerExercise(
    name: String,
    sets: Int?,
    reps: Int?,
    restSeconds: Int?,
    durationSeconds: Int? = null
): Exercise {
    val e = find(name)
    val repsText = when {
        durationSeconds != null && durationSeconds > 0 -> "$durationSeconds sn"
        reps != null && reps > 0 -> reps.toString()
        else -> ""
    }
    return Exercise(
        id = e?.id ?: "program-view-$name",
        name = e?.name ?: name,
        target = e?.targetMuscle.orEmpty(),
        sets = sets ?: e?.setsDefault ?: 0,
        reps = repsText,
        image = e?.imageUrl.orEmpty(),
        category = e?.category ?: "Strength",
        restSeconds = restSeconds ?: 60,
        exerciseTableId = e?.id.orEmpty(),
        sportType = e?.sportType.orEmpty(),
        trackingMode = e?.trackingMode.orEmpty()
    )
}

/**
 * Program bağlamında hareket gösterimi: [exercise] doluysa ExerciseDetailSheet açılır.
 * Kilo geçmişi boş olduğundan sheet AI analiz bölümünü zaten göstermez (weightHistory.isNotEmpty koşulu).
 */
@Composable
fun ProgramExerciseSheet(exercise: Exercise?, onDismiss: () -> Unit) {
    if (exercise != null) {
        ExerciseDetailSheet(
            exercise = exercise,
            onDismiss = onDismiss,
            weightHistory = emptyList(),
            aiInsight = "",
            isAiLoading = false,
            isFree = true
        )
    }
}

/** Satır sonu cam oynat düğmesi (48dp). */
@Composable
fun ExercisePlayButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    SinemaCapsuleIconButton(
        icon = SinemaIcons.Play,
        description = theme.t("Hareketi gör", "How to"),
        onClick = onClick,
        selected = false,
        size = 48.dp,
        modifier = modifier
    )
}
