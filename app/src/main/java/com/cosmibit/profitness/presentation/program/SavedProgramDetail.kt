package com.cosmibit.profitness.presentation.program

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
import com.cosmibit.profitness.core.ui.categoryImageUrl
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

    val cover = remember(program) {
        program.days.asSequence().flatMap { it.exercises.asSequence() }
            .firstOrNull { it.imageUrl.isNotBlank() }?.imageUrl.orEmpty()
    }.ifBlank { categoryImageUrl("") }
    val workoutDays = program.days.count { !it.isRestDay }
    val totalExercises = program.days.sumOf { it.exercises.size }
    val typeLabel = when (program.type) {
        ProgramType.AI -> "AI"
        ProgramType.MANUAL -> theme.t("Manuel", "Manual")
        else -> theme.t("Şablon", "Template")
    }

    ProgramDetailShell(
        coverUrl = cover,
        eyebrow = if (program.isActive) theme.t("Aktif program · $typeLabel", "Active program · $typeLabel") else typeLabel,
        title = program.name,
        blocking = false,
        onDismiss = onDismiss,
        bottomBar = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                val shape = RoundedCornerShape(28.dp)
                Row(
                    Modifier.weight(1f).height(56.dp).clip(shape)
                        .background(Color.White.copy(0.18f)).border(1.dp, Color.White.copy(0.2f), shape)
                        .sinemaClickable(role = Role.Button, onClick = onEdit, scale = 0.97f),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Edit, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(theme.t("Düzenle", "Edit"), color = Color.White, fontSize = SinemaType.BodySm, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                }
                Box(Modifier.weight(1.4f)) {
                    if (program.isActive) {
                        SinemaGoButton(label = theme.t("BAŞLA", "START"), icon = SinemaIcons.Play, onClick = onStart)
                    } else {
                        SinemaGoButton(label = theme.t("Aktif yap", "Set active"), icon = SinemaIcons.Check, onClick = onSetActive)
                    }
                }
            }
        }
    ) {
        Text(
            theme.t(
                "$workoutDays antrenman günü" + if (totalExercises > 0) " · $totalExercises egzersiz" else "",
                "$workoutDays training days" + if (totalExercises > 0) " · $totalExercises exercises" else ""
            ),
            color = Sinema.textSecondary(theme), fontSize = SinemaType.Body
        )
        if (program.isActive) {
            Spacer(Modifier.height(12.dp))
            // c3 = işaretle (rozet)
            Row(
                Modifier.height(28.dp).clip(RoundedCornerShape(SinemaRadius.R12)).background(Color.White.copy(0.14f)).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(SinemaIcons.Bolt, null, tint = Sinema.Text, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(theme.t("AKTİF PROGRAM", "ACTIVE PROGRAM"), color = Sinema.Text, fontSize = SinemaType.Caption, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
        program.days.filter { !it.isRestDay && it.exercises.isNotEmpty() }.forEachIndexed { dayIdx, day ->
            Spacer(Modifier.height(20.dp))
            Text(
                "${dayIdx + 1} · ${day.title}".sinemaUpper(theme),
                color = Sinema.textSecondary(theme), style = Sinema.Label, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            if (day.notes.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(day.notes, color = Sinema.textSecondary(theme), fontSize = SinemaType.Caption)
            }
            Spacer(Modifier.height(8.dp))
            Column(Modifier.fillMaxWidth().sinemaCard(theme, RoundedCornerShape(SinemaRadius.R24)).padding(horizontal = 16.dp, vertical = 4.dp)) {
                day.exercises.forEachIndexed { i, ex ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Sinema.line(theme)))
                    val open = {
                        viewerExercise = catalog.toViewerExercise(ex.exerciseName, ex.sets, ex.reps, ex.restSeconds, ex.targetDurationSeconds)
                    }
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).sinemaClickable(role = Role.Button, onClick = open, scale = 0.98f).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            theme.exerciseDisplayName(ex.exerciseName), color = Sinema.text(theme), fontSize = SinemaType.Body,
                            fontWeight = FontWeight.Bold, maxLines = 2, modifier = Modifier.weight(1f)
                        )
                        Text(
                            if (ex.targetDurationSeconds != null) "${ex.targetDurationSeconds} ${theme.t("SN", "SEC")}" else "${ex.sets}×${ex.reps}",
                            color = Sinema.textSecondary(theme), style = Sinema.Label, maxLines = 1,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                        ExercisePlayButton(onClick = open, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}
