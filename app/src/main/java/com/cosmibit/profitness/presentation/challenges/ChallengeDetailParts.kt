package com.cosmibit.profitness.presentation.challenges

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cosmibit.profitness.core.theme.LocalAppTheme
import com.cosmibit.profitness.core.theme.Sinema
import com.cosmibit.profitness.core.theme.SinemaCapsule
import com.cosmibit.profitness.core.theme.SinemaRadius
import com.cosmibit.profitness.core.theme.SinemaRaise
import com.cosmibit.profitness.core.theme.SinemaType
import com.cosmibit.profitness.core.theme.accentColor
import com.cosmibit.profitness.core.theme.accentHi
import com.cosmibit.profitness.core.theme.effectiveAccentColor
import com.cosmibit.profitness.core.theme.rewardGold
import com.cosmibit.profitness.core.theme.sinemaCard
import com.cosmibit.profitness.core.theme.sinemaRaised
import com.cosmibit.profitness.core.theme.sinemaUpper
import com.cosmibit.profitness.core.theme.strings
import com.cosmibit.profitness.core.theme.t
import com.cosmibit.profitness.core.ui.SinemaFitText
import com.cosmibit.profitness.domain.challenges.ChallengeLeaderboardEntry
import com.cosmibit.profitness.domain.challenges.ChallengeSummary
import com.cosmibit.profitness.domain.challenges.ChallengeVisibility
import com.cosmibit.profitness.presentation.components.AppBackButton
import com.cosmibit.profitness.presentation.components.safeTopPadding
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/*
 * Etkinlik / challenge detay sayfası parçaları (yalnız bu ekrana özel).
 * Kural: çerçeve / kenar çizgisi / halka YOK; ayrım sinemaRaised dolgu + gölge ile. Metin ≥12sp, dokunma ≥48dp.
 * Tür rengi (hue) challengeCoverKey -> coverHueFor'dan gelir; yalnız yerel ışık, tint ve ilerleme çubuğunda kullanılır.
 */

// Sıralama madalya tonları (yalnız rank rozeti; ortak token dosyası bu işte değiştirilmiyor).
private val MedalSilver = Color(0xFFC9CED6)
private val MedalBronze = Color(0xFFD9A07A)

/** Tint üstünde okunur ön plan: aynı hue, beyaza çekilmiş. */
internal fun Color.cdOnTint(): Color = lerp(this, Color.White, 0.42f)

internal enum class ChallengePhase { Upcoming, Live, Ended, Completed }

internal fun challengePhaseOf(c: ChallengeSummary, today: LocalDate = LocalDate.now()): ChallengePhase {
    val start = runCatching { LocalDate.parse(c.startDateIso.take(10)) }.getOrNull()
    val end = runCatching { LocalDate.parse(c.endDateIso.take(10)) }.getOrNull()
    return when {
        c.isCompleted -> ChallengePhase.Completed
        end != null && today.isAfter(end) -> ChallengePhase.Ended
        start != null && today.isBefore(start) -> ChallengePhase.Upcoming
        else -> ChallengePhase.Live
    }
}

/** Kalan gün / başlangıç metni (ilerleme bloğunun altında). */
@Composable
private fun countdownText(c: ChallengeSummary, phase: ChallengePhase): String {
    val theme = LocalAppTheme.current
    val today = LocalDate.now()
    val start = runCatching { LocalDate.parse(c.startDateIso.take(10)) }.getOrNull()
    val end = runCatching { LocalDate.parse(c.endDateIso.take(10)) }.getOrNull()
    return when (phase) {
        ChallengePhase.Completed -> theme.t("Hedef tamamlandı", "Target completed")
        ChallengePhase.Ended -> theme.t("Süre doldu", "Time is up")
        ChallengePhase.Upcoming -> {
            val d = if (start != null) ChronoUnit.DAYS.between(today, start) else 0L
            if (d <= 0L) theme.t("Bugün başlıyor", "Starts today") else theme.t("$d gün sonra başlıyor", "Starts in $d days")
        }
        ChallengePhase.Live -> {
            val d = if (end != null) ChronoUnit.DAYS.between(today, end) else 0L
            if (d <= 0L) theme.t("Bugün son gün", "Last day today") else theme.t("$d gün kaldı", "$d days left")
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  HERO: zemin sayfa rengi, tür rengi yalnız sağda yerel ışık; hayalet ikon başlığın sağında (buton/çiple çakışmaz)
// ═══════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChallengeDetailHero(
    c: ChallengeSummary,
    isEvent: Boolean,
    hue: Color,
    ghostIcon: ImageVector,
    targetIcon: ImageVector,
    onBack: () -> Unit,
    menu: (@Composable () -> Unit)?
) {
    val theme = LocalAppTheme.current
    val phase = challengePhaseOf(c)
    Box(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                val r = minOf(size.height * 0.55f, size.width * 0.7f)
                drawRect(
                    Brush.radialGradient(
                        0f to hue.copy(alpha = 0.20f), 0.55f to hue.copy(alpha = 0.07f), 1f to Color.Transparent,
                        center = Offset(size.width * 0.84f, size.height * 0.56f), radius = r
                    )
                )
            }
    ) {
        Column(Modifier.fillMaxWidth().safeTopPadding().padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppBackButton(onClick = onBack, accent = theme.effectiveAccentColor, size = 48.dp)
                Spacer(Modifier.weight(1f))
                menu?.invoke()
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        c.title,
                        style = Sinema.Title.copy(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold),
                        color = Sinema.text(theme), maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    if (c.creatorName.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DetailAvatar(c.creatorAvatar, c.creatorName, 28.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                theme.t("Oluşturan", "By"),
                                color = Sinema.textSecondary(theme), fontSize = SinemaType.BodySm, maxLines = 1
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                c.creatorName, color = Sinema.text(theme), fontSize = SinemaType.BodySm,
                                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                // hayalet ikon: başlık bloğunun sağında, kendi sütununda (üst satır düğmeleri / çiplerle çakışmaz)
                Icon(
                    ghostIcon, contentDescription = null,
                    tint = hue.cdOnTint().copy(alpha = 0.25f),
                    modifier = Modifier.padding(start = 8.dp).size(84.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            FlowRow(
                Modifier.padding(start = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val kind = if (isEvent) "${theme.strings.eventLabel} · ${c.targetType.displayLabel(theme)}" else c.targetType.displayLabel(theme)
                DetailChip(tint = hue) { fg ->
                    Icon(targetIcon, null, tint = fg, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(kind, color = fg, fontSize = SinemaType.Caption, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                val (label, dot, chipTint) = when (phase) {
                    ChallengePhase.Completed -> Triple(theme.t("Tamamlandı", "Completed"), theme.rewardGold, theme.rewardGold)
                    ChallengePhase.Live -> Triple(theme.t("Devam ediyor", "In progress"), theme.accentHi, theme.accentHi)
                    ChallengePhase.Upcoming -> Triple(theme.t("Yakında", "Upcoming"), hue, null)
                    ChallengePhase.Ended -> Triple(theme.t("Sona erdi", "Ended"), Sinema.TextSecondary.copy(0.6f), null)
                }
                DetailChip(tint = chipTint) { fg ->
                    Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        label, color = if (phase == ChallengePhase.Ended) Sinema.TextSecondary else fg,
                        fontSize = SinemaType.Caption, fontWeight = FontWeight.Bold, maxLines = 1
                    )
                }
                if (c.visibility == ChallengeVisibility.Private) {
                    DetailChip(tint = null) { _ ->
                        Icon(Icons.Rounded.Lock, null, tint = Sinema.TextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(theme.t("Özel", "Private"), color = Sinema.TextSecondary, fontSize = SinemaType.Caption, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Bilgi çipi (tıklanmaz): yükseltilmiş mini blok, isteğe bağlı tür tint'i; ön plan tint'in açık tonu. */
@Composable
private fun DetailChip(tint: Color?, content: @Composable RowScope.(Color) -> Unit) {
    SinemaCapsule(
        height = 32.dp, shape = CircleShape, tint = tint,
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) { fg -> content(tint?.cdOnTint() ?: fg) }
}

@Composable
internal fun DetailAvatar(avatar: String?, name: String, size: Dp) {
    val theme = LocalAppTheme.current
    Box(
        Modifier.size(size).clip(CircleShape).background(lerp(Sinema.Page, Color.White, 0.10f)),
        contentAlignment = Alignment.Center
    ) {
        if (avatar != null && avatar.startsWith("http")) {
            AsyncImage(model = avatar, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape))
        } else {
            Text(
                avatar?.takeIf { it.isNotBlank() } ?: name.take(1).uppercase().ifBlank { "?" },
                color = Sinema.text(theme), fontSize = if (size >= 36.dp) SinemaType.BodyLg else SinemaType.Caption,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  İLERLEME: kahraman rakam + yüzde rozeti + tür renginde gradyan çubuk + kalan
// ═══════════════════════════════════════════════════════════════════════════
@Composable
internal fun ChallengeProgressCard(c: ChallengeSummary, unit: String, hue: Color) {
    val theme = LocalAppTheme.current
    val numFmt = remember { java.text.NumberFormat.getIntegerInstance(java.util.Locale("tr")) }
    val phase = challengePhaseOf(c)
    val joined = c.isJoined || c.isCompleted
    val pct = if (c.isCompleted) 1f else c.progressPct.coerceIn(0f, 1f)
    val onTint = hue.cdOnTint()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp)
            .sinemaCard(theme, RoundedCornerShape(SinemaRadius.R24), tint = hue)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text(
            (if (joined) theme.t("İlerlemen", "Your progress") else theme.t("Hedef", "Target")).sinemaUpper(theme),
            style = Sinema.Label, color = onTint
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                SinemaFitText(
                    numFmt.format(if (joined) c.myProgress else c.targetValue),
                    style = Sinema.Display, baseSp = 60f, floorSp = 30f,
                    color = Sinema.text(theme), modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    if (joined) "/ ${numFmt.format(c.targetValue)} $unit" else unit,
                    color = Sinema.textSecondary(theme), fontSize = SinemaType.BodyLg, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )
            }
            if (joined) {
                Spacer(Modifier.width(8.dp))
                SinemaCapsule(
                    height = 36.dp, shape = CircleShape, tint = if (c.isCompleted) theme.rewardGold else hue,
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) { _ ->
                    Text(
                        "%${(pct * 100).toInt()}",
                        color = (if (c.isCompleted) theme.rewardGold else hue).cdOnTint(),
                        style = Sinema.Title.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold), maxLines = 1
                    )
                }
            }
        }
        if (joined) {
            Spacer(Modifier.height(14.dp))
            HueProgressBar(pct, hue, 10.dp)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (joined) {
                val left = (c.targetValue - c.myProgress).coerceAtLeast(0L)
                val leftText = if (c.isCompleted || left == 0L) theme.t("Hedefe ulaştın", "Target reached")
                else theme.t("${numFmt.format(left)} $unit kaldı", "${numFmt.format(left)} $unit to go")
                Icon(
                    if (c.isCompleted || left == 0L) Icons.Rounded.EmojiEvents else Icons.Rounded.Flag, null,
                    tint = if (c.isCompleted || left == 0L) theme.rewardGold else onTint, modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    leftText, color = Sinema.text(theme), fontSize = SinemaType.BodySm, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            Icon(Icons.Rounded.Schedule, null, tint = Sinema.textSecondary(theme), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                countdownText(c, phase), color = Sinema.textSecondary(theme), fontSize = SinemaType.BodySm,
                fontWeight = FontWeight.Medium, maxLines = 1
            )
        }
    }
}

/** Tür renginde ilerleme çubuğu: iz hue α.16 (boşken bile renkli), dolgu koyu->açık hue gradyan. */
@Composable
internal fun HueProgressBar(progress: Float, hue: Color, height: Dp, modifier: Modifier = Modifier) {
    val p = progress.coerceIn(0f, 1f)
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(hue.copy(alpha = 0.16f))) {
        if (p > 0f) {
            Box(
                Modifier.fillMaxWidth(p).fillMaxHeight().clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(lerp(hue, Color.Black, 0.18f), hue.cdOnTint())))
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  ÜÇ İSTATİSTİK: yükseltilmiş mini bloklar (Katılımcı amber / Başlangıç mercan / Bitiş nötr)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
internal fun ChallengeStatBlocks(participants: String, start: String, end: String) {
    val theme = LocalAppTheme.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatBlock(Icons.Rounded.People, participants, theme.t("Katılımcı", "Participants"), theme.accentColor, Modifier.weight(1f))
        StatBlock(Icons.Rounded.PlayArrow, start, theme.t("Başlangıç", "Start"), theme.accentHi, Modifier.weight(1f))
        StatBlock(Icons.Rounded.Flag, end, theme.t("Bitiş", "End"), null, Modifier.weight(1f))
    }
}

@Composable
private fun StatBlock(icon: ImageVector, value: String, label: String, tint: Color?, modifier: Modifier) {
    val theme = LocalAppTheme.current
    Column(
        modifier
            .sinemaRaised(RoundedCornerShape(SinemaRadius.R20), SinemaRaise.Card, tint = tint)
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        IconDisk(icon, tint, 32.dp)
        Spacer(Modifier.height(10.dp))
        Text(
            value, color = Sinema.text(theme),
            style = Sinema.Title.copy(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold),
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(label.sinemaUpper(theme), color = Sinema.textSecondary(theme), style = Sinema.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Yuvarlak ikon diski (mini yükseltilmiş, isteğe bağlı tint). */
@Composable
internal fun IconDisk(icon: ImageVector, tint: Color?, size: Dp = 40.dp) {
    Box(
        Modifier.size(size).sinemaRaised(CircleShape, SinemaRaise.Mini, tint = tint),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint?.cdOnTint() ?: Sinema.Text, modifier = Modifier.size(size * 0.5f))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  ETKİNLİK BİLGİSİ: tek kart içinde ikon diskli satırlar (çizgi yok; ayrım boşlukla)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
internal fun DetailInfoRow(
    icon: ImageVector,
    iconTint: Color?,
    label: String,
    value: String,
    trailing: (@Composable () -> Unit)? = null
) {
    val theme = LocalAppTheme.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconDisk(icon, iconTint, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label.sinemaUpper(theme), color = Sinema.textSecondary(theme), style = Sinema.Label, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(value, color = Sinema.text(theme), fontSize = SinemaType.Body, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

/** "Haritada gör" / "Aç" yumuşak ikincil çip (≥48dp dokunma hedefi SinemaCapsule'dan). */
@Composable
internal fun DetailSoftAction(label: String, icon: ImageVector = Icons.Rounded.Map, onClick: () -> Unit) {
    SinemaCapsule(
        onClick = onClick, height = 36.dp, shape = CircleShape, soft = true,
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) { fg ->
        Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = fg, fontSize = SinemaType.Caption, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** Rota seçeneği: etiketli, tür rengi tint'li düğme. */
@Composable
internal fun RouteOption(icon: ImageVector, label: String, hue: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SinemaCapsule(
        modifier, onClick = onClick, height = 48.dp, shape = RoundedCornerShape(SinemaRadius.R16), tint = hue,
        contentPadding = PaddingValues(horizontal = 8.dp), horizontalArrangement = Arrangement.Center
    ) { _ ->
        val fg = hue.cdOnTint()
        Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = fg, fontSize = SinemaType.Caption, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  SIRALAMA: ayrı yükseltilmiş satırlar; "Sen" satırı çerçevesiz, yumuşak tint dolgu + rozet
// ═══════════════════════════════════════════════════════════════════════════
@Composable
internal fun LeaderboardRowV2(
    rank: Int,
    entry: ChallengeLeaderboardEntry,
    unit: String,
    target: Long,
    hue: Color
) {
    val theme = LocalAppTheme.current
    val numFmt = remember { java.text.NumberFormat.getIntegerInstance(java.util.Locale("tr")) }
    val me = entry.isMe
    val meTint = theme.accentColor
    val nameIsSen = entry.displayName.isBlank() || entry.displayName == "Sen"
    val name = if (me && nameIsSen) theme.t("Sen", "You") else entry.displayName.ifBlank { theme.t("Anonim", "Anonymous") }
    val frac = if (target > 0) (entry.progress.toFloat() / target).coerceIn(0f, 1f) else 0f
    val valueText = numFmt.format(entry.progress)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .sinemaRaised(RoundedCornerShape(SinemaRadius.R20), SinemaRaise.Card, tint = if (me) meTint else null)
            .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$rank. $name, $valueText $unit"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RankBadge(rank)
        Spacer(Modifier.width(10.dp))
        DetailAvatar(entry.avatarUrl, entry.displayName, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name, color = Sinema.text(theme), fontSize = SinemaType.Body,
                    fontWeight = if (me) FontWeight.ExtraBold else FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                )
                if (me && !nameIsSen) {
                    Spacer(Modifier.width(6.dp))
                    SinemaCapsule(height = 24.dp, shape = CircleShape, tint = meTint, contentPadding = PaddingValues(horizontal = 8.dp)) { _ ->
                        Text(theme.t("Sen", "You"), color = meTint.cdOnTint(), fontSize = SinemaType.Caption, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                    }
                }
            }
            if (entry.isCompleted) {
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.EmojiEvents, null, tint = theme.rewardGold, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(theme.t("Tamamladı", "Completed"), color = theme.rewardGold, fontSize = SinemaType.Caption, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(valueText, color = Sinema.text(theme), style = Sinema.Title.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold), maxLines = 1)
                if (unit.isNotBlank()) {
                    Spacer(Modifier.width(4.dp))
                    Text(unit, color = Sinema.textSecondary(theme), fontSize = SinemaType.Caption, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.padding(bottom = 2.dp))
                }
            }
            if (target > 0) {
                Spacer(Modifier.height(6.dp))
                HueProgressBar(frac, hue, 4.dp, Modifier.width(64.dp))
            }
        }
    }
}

@Composable
private fun RankBadge(rank: Int) {
    val theme = LocalAppTheme.current
    val medal = when (rank) {
        1 -> Sinema.Gold
        2 -> MedalSilver
        3 -> MedalBronze
        else -> null
    }
    Box(
        Modifier.size(32.dp).sinemaRaised(CircleShape, SinemaRaise.Mini, tint = medal),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$rank", color = medal?.let { lerp(it, Color.White, 0.25f) } ?: Sinema.textSecondary(theme),
            fontSize = SinemaType.BodySm, fontWeight = FontWeight.ExtraBold, maxLines = 1
        )
    }
}

@Composable
internal fun LeaderboardEmpty(canJoin: Boolean) {
    val theme = LocalAppTheme.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .sinemaCard(theme, RoundedCornerShape(SinemaRadius.R24))
            .padding(vertical = 24.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconDisk(Icons.Rounded.People, theme.accentColor, 56.dp)
        Spacer(Modifier.height(12.dp))
        Text(theme.t("Henüz katılımcı yok", "No participants yet"), color = Sinema.text(theme), fontSize = SinemaType.Body, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (canJoin) {
            Spacer(Modifier.height(4.dp))
            Text(theme.t("İlk katılan sen ol.", "Be the first to join."), color = Sinema.textSecondary(theme), fontSize = SinemaType.BodySm, textAlign = TextAlign.Center)
        }
    }
}
