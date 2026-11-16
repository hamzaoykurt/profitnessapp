package com.cosmibit.profitness.presentation.program

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmibit.profitness.core.theme.*

/** Program detayı ortak parçaları (hazır + kayıtlı program): özet blokları, gün kartları, kas yükü, açıklama. */
internal data class DetailEx(val name: String, val sets: Int, val reps: Int, val restSeconds: Int, val durationSeconds: Int?)
internal data class DetailDay(val title: String, val exercises: List<DetailEx>, val muscles: List<String> = emptyList())

/** Gün içi hareketlerin kas grubu alanından en çok geçen 2-3 grup (UI katmanı; veri yoksa boş -> çip gösterilmez). */
internal fun topMuscles(raw: List<String>, theme: AppThemeState): List<String> =
    raw.map { it.trim() }.filter { it.isNotEmpty() }
        .groupingBy { theme.fitnessTermDisplayName(it) }.eachCount()
        .entries.sortedByDescending { it.value }.take(3).map { it.key }
internal data class DetailMuscle(val label: String, val pct: String, val fraction: Float)

private val CardShape = RoundedCornerShape(SinemaRadius.R24)
private val RowShape = RoundedCornerShape(SinemaRadius.R20)

/** Bölüm etiketi: mercan nokta + küçük büyük harf etiket. */
@Composable
internal fun ProgramSectionLabel(text: String) {
    val theme = LocalAppTheme.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        SinemaLabelDot()
        Spacer(Modifier.width(8.dp))
        Text(text.sinemaUpper(theme), color = Sinema.textSecondary(theme), style = Sinema.Label, maxLines = 1)
    }
}

/** Özet: yan yana küçük raised bloklar (ikon + rakam + etiket); tonlar mercan / amber / karışım. */
@Composable
internal fun ProgramBigStats(items: List<Pair<String, String>>) {
    val theme = LocalAppTheme.current
    val icons = listOf(Icons.Rounded.CalendarMonth, Icons.Rounded.FitnessCenter, Icons.Rounded.Timer)
    val tints = listOf(theme.accentHi, theme.accentColor, lerp(theme.accentHi, theme.accentColor, 0.5f))
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEachIndexed { i, (value, label) ->
            val tint = tints[i % tints.size]
            Column(
                Modifier.weight(1f).fillMaxHeight().then(if (i == 0) Modifier.sinemaRaised(RoundedCornerShape(SinemaRadius.R20), SinemaRaise.Card, fill = theme.accentHi) else Modifier.sinemaCard(theme, RoundedCornerShape(SinemaRadius.R20), tint = tint)).padding(horizontal = 12.dp, vertical = 14.dp)
            ) {
                Icon(icons[i % icons.size], null, tint = if (i == 0) theme.effectiveOnAccentColor else lerp(tint, Color.White, 0.35f), modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(8.dp))
                Text(value, color = if (i == 0) theme.effectiveOnAccentColor else Sinema.text(theme), fontSize = 26.sp, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false)
                Text(label, color = if (i == 0) theme.effectiveOnAccentColor.copy(0.75f) else Sinema.textSecondary(theme), fontSize = SinemaType.Caption, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Gün kartları: her gün ayrı raised kart; ilk gün açık, diğerleri kapalı; hareketler kart içinde alt raised satırlar. */
@Composable
internal fun ProgramPlanDays(days: List<DetailDay>, hue: Color = LocalAppTheme.current.accentHi, onOpen: (DetailEx) -> Unit) {
    val theme = LocalAppTheme.current
    var openIdx by rememberSaveable { mutableStateOf(listOf(0)) }
    val badgeBg = lerp(Color(0xFF1A1D23), hue, 0.24f)
    val badgeFg = lerp(hue, Color.White, 0.40f)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        days.forEachIndexed { idx, day ->
            val open = idx in openIdx
            val rot by animateFloatAsState(if (open) 180f else 0f, tween(SinemaMotion.Standard, easing = FastOutSlowInEasing), label = "day_chev")
            Column(Modifier.fillMaxWidth().sinemaCard(theme, CardShape)) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 72.dp)
                        .sinemaClickable(role = Role.Button, scale = 0.98f, onClick = { openIdx = if (open) openIdx - idx else openIdx + idx })
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(badgeBg), contentAlignment = Alignment.Center) {
                        Text("${idx + 1}", color = badgeFg, fontSize = SinemaType.BodyLg, fontWeight = FontWeight.Black, maxLines = 1)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(day.title, color = Sinema.text(theme), fontSize = SinemaType.Body, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            theme.t("Gün ${idx + 1} · ${day.exercises.size} hareket", "Day ${idx + 1} · ${day.exercises.size} exercises"),
                            color = Sinema.textSecondary(theme), fontSize = SinemaType.Caption, maxLines = 1
                        )
                    }
                    Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Sinema.textSecondary(theme), modifier = Modifier.size(24.dp).rotate(rot))
                }
                if (day.muscles.isNotEmpty()) {
                    Row(Modifier.padding(start = 68.dp, end = 16.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        day.muscles.forEach { m ->
                            Box(Modifier.weight(1f, fill = false).clip(CircleShape).background(softHiFill(theme)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text(m, color = theme.accentHiTextColor, fontSize = SinemaType.Caption, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                AnimatedVisibility(
                    visible = open,
                    enter = expandVertically(tween(SinemaMotion.Standard, easing = FastOutSlowInEasing)) + fadeIn(tween(SinemaMotion.Standard)),
                    exit = shrinkVertically(tween(SinemaMotion.Quick, easing = FastOutSlowInEasing)) + fadeOut(tween(SinemaMotion.Quick))
                ) {
                    Column(Modifier.padding(start = 10.dp, end = 10.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        day.exercises.forEach { ex -> ExerciseRow(ex, onOpen) }
                    }
                }
            }
        }
    }
}

/** Hareket satırı: ad (EN baskın, TR ikinci satır küçük), set×tekrar çipi, dinlenme, ince soft mercan play. */
@Composable
private fun ExerciseRow(ex: DetailEx, onOpen: (DetailEx) -> Unit) {
    val theme = LocalAppTheme.current
    val primary = ex.name.substringBefore(" (")
    val secondary = if (" (" in ex.name) ex.name.substringAfter(" (").removeSuffix(")") else null
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).sinemaRaised(RowShape, SinemaRaise.Mini)
            .sinemaClickable(role = Role.Button, scale = 0.98f, onClick = { onOpen(ex) })
            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(primary, color = Sinema.text(theme), fontSize = SinemaType.Body, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (secondary != null) {
                Text(secondary, color = Sinema.textSecondary(theme), fontSize = SinemaType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Timer, null, tint = Sinema.textSecondary(theme), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("${ex.restSeconds} ${theme.t("sn", "s")}", color = Sinema.textSecondary(theme), fontSize = SinemaType.Caption, maxLines = 1)
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.clip(CircleShape).background(softHiFill(theme)).padding(horizontal = 10.dp, vertical = 5.dp)) {
            Text(
                if (ex.durationSeconds != null) "${ex.durationSeconds} ${theme.t("sn", "sec")}" else "${ex.sets}×${ex.reps}",
                color = theme.accentHiTextColor, fontSize = SinemaType.Caption, fontWeight = FontWeight.ExtraBold, maxLines = 1
            )
        }
        ExercisePlayButton(onClick = { onOpen(ex) }, modifier = Modifier.padding(start = 4.dp))
    }
}

/** Kas yükü: ayrı raised kart; ilk 4 çubuk (accentHi) + "+N kas grubu". */
@Composable
internal fun ProgramMuscleLoad(muscles: List<DetailMuscle>) {
    val theme = LocalAppTheme.current
    Column(Modifier.fillMaxWidth().sinemaCard(theme, CardShape).padding(horizontal = 18.dp, vertical = 16.dp)) {
        muscles.take(4).forEach { m ->
            Column(Modifier.padding(bottom = 12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(m.label, color = Sinema.text(theme), fontSize = SinemaType.Caption, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(m.pct, color = Sinema.text(theme), fontSize = SinemaType.Caption, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color.Black.copy(0.35f))) {
                    Box(Modifier.fillMaxWidth(m.fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).sinemaSolid(theme.accentHi))
                }
            }
        }
        if (muscles.size > 4) {
            val n = muscles.size - 4
            Text(theme.t("+$n kas grubu", "+$n muscle groups"), color = Sinema.textSecondary(theme), fontSize = SinemaType.Caption)
        }
    }
}

/** Açıklama: ayrı raised kart; 4 satır + "Devamını oku". */
@Composable
internal fun ProgramDescription(text: String) {
    val theme = LocalAppTheme.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    var overflow by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().sinemaCard(theme, CardShape).padding(horizontal = 18.dp, vertical = 16.dp)) {
        Text(
            text, color = Sinema.textSecondary(theme), fontSize = SinemaType.Body, lineHeight = 21.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflow = it.hasVisualOverflow }
        )
        if (overflow || expanded) {
            Text(
                if (expanded) theme.t("Daha az göster", "Show less") else theme.t("Devamını oku", "Read more"),
                color = theme.accentHiTextColor, fontSize = SinemaType.BodySm, fontWeight = FontWeight.Bold,
                modifier = Modifier.heightIn(min = 48.dp).sinemaClickable(role = Role.Button, onClick = { expanded = !expanded }).padding(top = 12.dp)
            )
        }
    }
}
