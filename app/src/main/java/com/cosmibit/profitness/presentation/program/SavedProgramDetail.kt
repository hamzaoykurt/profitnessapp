package com.cosmibit.profitness.presentation.program

import com.cosmibit.profitness.presentation.components.coverHueFor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cosmibit.profitness.core.theme.*
import com.cosmibit.profitness.domain.model.Program
import com.cosmibit.profitness.domain.model.ProgramType
import com.cosmibit.profitness.presentation.workout.Exercise

/**
 * Kayıtlı programın SALT-OKUNUR tam ekran detayı: hazır program detayıyla aynı kabuk ([ProgramDetailShell]).
 * Alt çubuk: ikincil cam "Düzenle" + tek c1 birincil (aktif değilse "Aktif yap", aktifse "BAŞLA").
 */
@Composable
fun SavedProgramDetail(
    program: Program,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onSetActive: () -> Unit,
    onStart: () -> Unit
) {
    val theme = LocalAppTheme.current
    val catalog = rememberExerciseCatalog()
    var viewerExercise by remember { mutableStateOf<Exercise?>(null) }
    ProgramExerciseSheet(viewerExercise) { viewerExercise = null }

    val workoutDays = program.days.count { !it.isRestDay }
    val totalExercises = program.days.sumOf { it.exercises.size }
    val typeLabel = when (program.type) {
        ProgramType.AI -> "AI"
        ProgramType.MANUAL -> theme.t("Manuel", "Manual")
        else -> theme.t("Şablon", "Template")
    }

    ProgramDetailShell(
        coverIconKey = program.name,
        eyebrow = if (program.isActive) theme.t("Aktif program · $typeLabel", "Active program · $typeLabel") else typeLabel,
        title = program.name,
        blocking = false,
        onDismiss = onDismiss,
        bottomBar = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                val shape = RoundedCornerShape(28.dp)
                Row(
                    Modifier.weight(1f).height(56.dp).sinemaRaised(shape, SinemaRaise.Mini)
                        .sinemaClickable(role = Role.Button, onClick = onEdit, scale = 0.97f),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Edit, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(theme.t("Düzenle", "Edit"), color = Color.White, fontSize = SinemaType.BodySm, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                }
                Box(Modifier.weight(1.4f)) {
                    if (program.isActive) {
                        SinemaGoButton(label = theme.t("Başla", "Start"), icon = SinemaIcons.Play, onClick = onStart)
                    } else {
                        SinemaGoButton(label = theme.t("Aktif yap", "Set active"), icon = SinemaIcons.Check, onClick = onSetActive)
                    }
                }
            }
        }
    ) {
        val planDays = program.days.filter { !it.isRestDay && it.exercises.isNotEmpty() }.map { d ->
            DetailDay(d.title, d.exercises.map { DetailEx(theme.exerciseDisplayName(it.exerciseName), it.sets, it.reps, it.restSeconds, it.targetDurationSeconds) })
        }
        ProgramBigStats(
            buildList {
                add("$workoutDays" to theme.t("Antrenman günü", "Training days"))
                if (totalExercises > 0) add("$totalExercises" to theme.t("Hareket", "Exercises"))
                val totalSets = program.days.sumOf { d -> d.exercises.sumOf { it.sets } }
                if (totalSets > 0) add("$totalSets" to theme.t("Set", "Sets"))
            }
        )
        if (planDays.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            ProgramSectionLabel(theme.t("Plan", "Plan"))
            Spacer(Modifier.height(8.dp))
            ProgramPlanDays(planDays, hue = coverHueFor(program.name)) { ex ->
                viewerExercise = catalog.toViewerExercise(ex.name, ex.sets, ex.reps, ex.restSeconds, ex.durationSeconds)
            }
        }
    }
}
