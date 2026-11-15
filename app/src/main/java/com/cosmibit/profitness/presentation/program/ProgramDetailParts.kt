package com.cosmibit.profitness.presentation.program

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmibit.profitness.core.theme.*

/** Program detayı ortak parçaları (hazır + kayıtlı program): dev rakam satırı, plan, kas yükü, açıklama. */
internal data class DetailEx(val name: String, val sets: Int, val reps: Int, val restSeconds: Int, val durationSeconds: Int?)
internal data class DetailDay(val title: String, val exercises: List<DetailEx>)
internal data class DetailMuscle(val label: String, val pct: String, val fraction: Float)

@Composable
internal fun ProgramSectionLabel(text: String) {
    val theme = LocalAppTheme.current
    Text(text.sinemaUpper(theme), color = Sinema.textSecondary(theme), style = Sinema.Label, maxLines = 1)
}

/** Kartsız dev rakam satırı: iki yatay çizgi arasında. */
@Composable
internal fun ProgramBigStats(items: List<Pair<String, String>>) {
    val theme = LocalAppTheme.current
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Sinema.line(theme)))
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            items.forEach { (value, label) ->
                Column(Modifier.weight(1f)) {
                    Text(value, color = Sinema.text(theme), fontSize = 52.sp, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false, lineHeight = 56.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(label.sinemaUpper(theme), color = Sinema.textSecondary(theme), fontSize = SinemaType.Caption, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Sinema.line(theme)))
    }
}

/** Gün listesi: ilk gün açık, diğerleri kapalı başlık satırı. */
@Composable
internal fun ProgramPlanDays(days: List<DetailDay>, onOpen: (DetailEx) -> Unit) {
    val theme = LocalAppTheme.current
    var openIdx by rememberSaveable { mutableStateOf(listOf(0)) }
    days.forEachIndexed { idx, day ->
        val open = idx in openIdx
        if (idx > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Sinema.line(theme)))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .sinemaClickable(role = Role.Button, scale = 0.98f, onClick = { openIdx = if (open) openIdx - idx else openIdx + idx }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(theme.t("GÜN ${idx + 1}", "DAY ${idx + 1}"), color = Sinema.textSecondary(theme), fontSize = SinemaType.Caption, fontWeight = FontWeight.SemiBold)
                Text(day.title, color = Sinema.text(theme), fontSize = SinemaType.Body, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(
                theme.t("${day.exercises.size} hareket", "${day.exercises.size} exercises"),
                color = Sinema.textSecondary(theme), fontSize = SinemaType.Caption, maxLines = 1, modifier = Modifier.padding(horizontal = 8.dp)
            )
            Icon(
                if (open) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null,
                tint = Sinema.textSecondary(theme), modifier = Modifier.size(24.dp)
            )
        }
        if (open) {
            day.exercises.forEach { ex ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        .sinemaClickable(role = Role.Button, scale = 0.98f, onClick = { onOpen(ex) }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(ex.name, color = Sinema.text(theme), fontSize = SinemaType.Body, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Column(Modifier.padding(start = 10.dp), horizontalAlignment = Alignment.End) {
                        Text(
                            if (ex.durationSeconds != null) "${ex.durationSeconds} ${theme.t("sn", "sec")}" else "${ex.sets}×${ex.reps}",
                            color = Sinema.text(theme), fontSize = SinemaType.Body, fontWeight = FontWeight.Bold, maxLines = 1
                        )
                        Text("${ex.restSeconds} ${theme.t("sn dinlenme", "s rest")}", color = Sinema.textSecondary(theme), fontSize = SinemaType.Caption, maxLines = 1)
                    }
                    ExercisePlayButton(onClick = { onOpen(ex) }, modifier = Modifier.padding(start = 8.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Kas yükü: ilk 4 çubuk (accentHi) + "+N kas grubu". */
@Composable
internal fun ProgramMuscleLoad(muscles: List<DetailMuscle>) {
    val theme = LocalAppTheme.current
    muscles.take(4).forEach { m ->
        Column(Modifier.padding(bottom = 12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(m.label, color = Sinema.text(theme), fontSize = SinemaType.Caption, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(m.pct, color = Sinema.text(theme), fontSize = SinemaType.Caption, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Sinema.line(theme))) {
                Box(Modifier.fillMaxWidth(m.fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(theme.accentHi))
            }
        }
    }
    if (muscles.size > 4) {
        val n = muscles.size - 4
        Text(theme.t("+$n kas grubu", "+$n muscle groups"), color = Sinema.textSecondary(theme), fontSize = SinemaType.Caption)
    }
}

/** Açıklama: 4 satır + "Devamını oku". */
@Composable
internal fun ProgramDescription(text: String) {
    val theme = LocalAppTheme.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    var overflow by remember { mutableStateOf(false) }
    Text(
        text, color = Sinema.textSecondary(theme), fontSize = SinemaType.Body, lineHeight = 21.sp,
        maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis,
        onTextLayout = { if (!expanded) overflow = it.hasVisualOverflow }
    )
    if (overflow || expanded) {
        Text(
            if (expanded) theme.t("Daha az göster", "Show less") else theme.t("Devamını oku", "Read more"),
            color = Sinema.text(theme), fontSize = SinemaType.BodySm, fontWeight = FontWeight.Bold,
            modifier = Modifier.heightIn(min = 48.dp).sinemaClickable(role = Role.Button, onClick = { expanded = !expanded }).padding(top = 12.dp)
        )
    }
}
