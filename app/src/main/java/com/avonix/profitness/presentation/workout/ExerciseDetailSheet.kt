package com.avonix.profitness.presentation.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avonix.profitness.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailSheet(
    exercise: Exercise,
    onDismiss: () -> Unit
) {
    val theme = LocalAppTheme.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = theme.bg1,
        dragHandle = { BottomSheetDefaults.DragHandle(color = theme.stroke) },
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.FitnessCenter,
                        null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        exercise.name.uppercase(),
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        exercise.target,
                        color = theme.text2,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Stats row
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(label = "SET", value = exercise.sets.toString(), accent = accent, modifier = Modifier.weight(1f))
                StatTile(label = "TEKrar", value = exercise.reps, accent = accent, modifier = Modifier.weight(1f))
                StatTile(label = "DİNLENME", value = "${exercise.restSeconds}s", accent = accent, modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "NASIL YAPILIR",
                color = theme.text2,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(12.dp))

            // How-to steps (placeholder — will be replaced with exercises.description from DB)
            val steps = listOf(
                "Doğru başlangıç pozisyonunu al ve postürüne dikkat et.",
                "Hareketi kontrollü ve yavaş bir tempoda gerçekleştir.",
                "Hedef kas grubunu kasılırken hissetmeye odaklan.",
                "Baskı anında nefes ver, geri dönüşte nefes al.",
                "Egzersizler arasında belirtilen dinlenme süresine uy."
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                steps.forEachIndexed { i, step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(theme.bg2)
                            .border(1.dp, theme.stroke, RoundedCornerShape(10.dp))
                            .padding(14.dp, 10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            "${i + 1}",
                            color = accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.width(20.dp)
                        )
                        Text(
                            step,
                            color = theme.text1,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, accent: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val theme = LocalAppTheme.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(theme.bg2)
            .border(1.dp, theme.stroke, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            color = theme.text2,
            fontSize = 8.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            color = accent,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black
        )
    }
}
