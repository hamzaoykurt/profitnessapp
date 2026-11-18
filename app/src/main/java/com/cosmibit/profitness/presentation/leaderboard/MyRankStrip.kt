package com.cosmibit.profitness.presentation.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.cosmibit.profitness.core.theme.AppLanguage
import com.cosmibit.profitness.core.theme.LocalAppTheme
import com.cosmibit.profitness.core.theme.Sinema
import com.cosmibit.profitness.core.theme.SinemaRadius
import com.cosmibit.profitness.core.theme.SinemaRaise
import com.cosmibit.profitness.core.theme.SinemaType
import com.cosmibit.profitness.core.theme.accentHi
import com.cosmibit.profitness.core.theme.sinemaClickable
import com.cosmibit.profitness.core.theme.sinemaRaised
import com.cosmibit.profitness.core.theme.t
import com.cosmibit.profitness.data.leaderboard.LeaderboardRepository
import com.cosmibit.profitness.domain.progress.RankTiers
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class MyRankState(val loaded: Boolean = false, val xp: Int = 0, val position: Long = 0L, val totalUsers: Long = 0L)

/** Yalnız okuma: genel XP sıralamasındaki yerin (get_my_rank_xp). Keşfet "Senin sıran" şeridi için. */
@HiltViewModel
class MyRankViewModel @Inject constructor(
    private val repository: LeaderboardRepository
) : ViewModel() {
    private val _state = MutableStateFlow(MyRankState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val r = repository.getMyXpRank().getOrNull()
            _state.value = MyRankState(loaded = true, xp = r?.xp ?: 0, position = r?.rank_position ?: 0L, totalUsers = r?.total_users ?: 0L)
        }
    }
}

/** Rank metalik rengi (Profil rozetleriyle aynı aile; eşik adları [RankTiers]'tan). */
internal fun rankStripBrush(rank: String): Brush = when (rank.lowercase()) {
    "silver" -> Brush.linearGradient(listOf(Color(0xFFC0C4CC), Color(0xFF8E949E)))
    "gold" -> Brush.linearGradient(listOf(Color(0xFFF7D774), Color(0xFFC9962E)))
    "platinum" -> Brush.linearGradient(listOf(Color(0xFFE3F2FA), Color(0xFF9FC3D6)))
    "diamond" -> Brush.linearGradient(listOf(Color(0xFFD7FAFF), Color(0xFF67C9E0)))
    else -> Brush.linearGradient(listOf(Color(0xFFE0A877), Color(0xFF9C6236)))
}

/**
 * Keşfet "Senin sıran" ince şeridi (≈56dp): rank rozeti + sıra + "Sıralamayı gör". Veri yoksa boş durum metni.
 * Dokununca Sıralama (Leaderboard) açılır.
 */
@Composable
fun MyRankStrip(onOpen: () -> Unit, modifier: Modifier = Modifier, viewModel: MyRankViewModel = hiltViewModel()) {
    val theme = LocalAppTheme.current
    val s by viewModel.state.collectAsStateWithLifecycle()
    val rank = RankTiers.rankFor(s.xp)
    val fmt = java.text.NumberFormat.getIntegerInstance(
        if (theme.language == AppLanguage.ENGLISH) java.util.Locale.ENGLISH else java.util.Locale.forLanguageTag("tr-TR")
    )
    val title = when {
        !s.loaded -> theme.t("Senin sıran", "Your rank")
        s.position > 0 -> theme.t("Senin sıran #${fmt.format(s.position)} · $rank", "Your rank #${fmt.format(s.position)} · $rank")
        else -> theme.t("Antrenman yap, sıralamaya gir", "Train to join the ranking")
    }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .sinemaClickable(onClick = onOpen, scale = 0.98f, role = Role.Button)
            .sinemaRaised(RoundedCornerShape(SinemaRadius.R20), SinemaRaise.Mini)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$title. " + theme.t("Sıralamayı gör", "See ranking") },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(rankStripBrush(rank)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Leaderboard, null, tint = Color(0xFF1A1B1F), modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(title, color = Sinema.text(theme), fontSize = SinemaType.Body, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
        Text(theme.t("Sıralamayı gör", "See ranking"), color = theme.accentHi, fontSize = SinemaType.Caption, fontWeight = FontWeight.Bold, maxLines = 1)
        Icon(Icons.Rounded.ChevronRight, null, tint = Sinema.textSecondary(theme), modifier = Modifier.size(20.dp))
    }
}
