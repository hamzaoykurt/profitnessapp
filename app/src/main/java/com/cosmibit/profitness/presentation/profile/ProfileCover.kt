package com.cosmibit.profitness.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cosmibit.profitness.core.theme.LocalAppTheme
import com.cosmibit.profitness.core.theme.Sinema
import com.cosmibit.profitness.core.theme.SinemaRadius
import com.cosmibit.profitness.core.theme.SinemaType
import com.cosmibit.profitness.core.theme.accentHi
import com.cosmibit.profitness.core.theme.sinemaClickable
import com.cosmibit.profitness.core.theme.t
import com.cosmibit.profitness.data.profile.ProfileCover
import com.cosmibit.profitness.data.profile.ProfileCoverStore
import com.cosmibit.profitness.presentation.workout.SceneImage
import com.cosmibit.profitness.core.ui.categoryImageUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

// ── Hazır kapaklar ────────────────────────────────────────────────────────────

enum class CoverPattern { None, Diagonal, Rings, Grid, Dots }

/** Hazır kapak: "photo" mevcut salon fotoğrafı; diğerleri koyu tür-renkli gradyan + ince geometrik desen (telifsiz, bundled). */
data class CoverPreset(val id: String, val labelTr: String, val labelEn: String, val hue: Color?, val pattern: CoverPattern)

val CoverPresets: List<CoverPreset> = listOf(
    CoverPreset("photo", "Salon", "Gym", null, CoverPattern.None),
    CoverPreset("amber", "Amber", "Amber", Color(0xFFF2B33D), CoverPattern.Diagonal),
    CoverPreset("coral", "Mercan", "Coral", Color(0xFFFF6F61), CoverPattern.Rings),
    CoverPreset("violet", "Mor", "Violet", Color(0xFF8B6CF0), CoverPattern.Grid),
    CoverPreset("teal", "Turkuaz", "Teal", Color(0xFF2CC5B5), CoverPattern.Dots),
    CoverPreset("blue", "Gece", "Night", Color(0xFF4C7DF0), CoverPattern.Diagonal),
    CoverPreset("green", "Orman", "Forest", Color(0xFF3FBF6E), CoverPattern.Rings),
    CoverPreset("crimson", "Kızıl", "Crimson", Color(0xFFE0455A), CoverPattern.Grid),
    CoverPreset("graphite", "Grafit", "Graphite", Color(0xFF9AA3B2), CoverPattern.Dots),
)

fun coverPresetById(id: String?): CoverPreset = CoverPresets.firstOrNull { it.id == id } ?: CoverPresets.first()

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ProfileCoverViewModel @Inject constructor(
    private val store: ProfileCoverStore,
    private val supabase: SupabaseClient
) : ViewModel() {
    val cover: StateFlow<ProfileCover> = store.cover
    var saving by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    private fun uid() = supabase.auth.currentSessionOrNull()?.user?.id ?: supabase.auth.currentUserOrNull()?.id

    init { viewModelScope.launch { uid()?.let { store.load(it) } } }

    fun selectPreset(id: String) { uid()?.let { store.selectPreset(it, id.takeIf { p -> p != "photo" }) } }

    fun pickPhoto(uri: Uri) {
        val id = uid() ?: return
        viewModelScope.launch {
            saving = true; error = null
            store.saveCustom(id, uri).onFailure { error = "Fotoğraf kapak olarak kaydedilemedi." }
            saving = false
        }
    }
}

// ── Çizim ─────────────────────────────────────────────────────────────────────

/** Kapak görseli (karartma çağıranda): yerel foto > sunucu foto > hazır kapak (gradyan) > varsayılan salon fotoğrafı. */
@Composable
fun ProfileCoverImage(cover: ProfileCover, modifier: Modifier = Modifier, parallax: () -> Float = { 0f }) {
    val ctx = LocalContext.current
    val custom = cover.customPath?.let(::File)
    when {
        custom != null -> AsyncImage(
            model = remember(custom.path, custom.lastModified()) {
                ImageRequest.Builder(ctx).data(custom).memoryCacheKey("${custom.path}:${custom.lastModified()}").crossfade(true).build()
            },
            contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier
        )
        !cover.remoteUrl.isNullOrBlank() -> AsyncImage(
            model = cover.remoteUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier
        )
        else -> {
            val preset = coverPresetById(cover.preset)
            if (preset.hue == null) SceneImage(
                url = categoryImageUrl(""),
                modifier = modifier.graphicsLayer { translationY = parallax() },
                requestWidthPx = 1080, requestHeightPx = 960,
                fallbackGradient = true
            ) else PresetCoverCanvas(preset, modifier)
        }
    }
}

/** Koyu tür-renkli gradyan + ince geometrik desen (Canvas; görsel varlık gerektirmez). */
@Composable
fun PresetCoverCanvas(preset: CoverPreset, modifier: Modifier = Modifier) {
    val hue = preset.hue ?: Color.Gray
    val top = lerp(Color(0xFF15161B), hue, 0.55f)
    val bottom = lerp(Color(0xFF15161B), hue, 0.12f)
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(top, bottom)))
        drawRect(Brush.radialGradient(listOf(hue.copy(0.35f), Color.Transparent), center = Offset(size.width * 0.85f, size.height * 0.1f), radius = size.width * 0.7f))
        val line = Color.White.copy(alpha = 0.07f)
        val step = 28.dp.toPx()
        when (preset.pattern) {
            CoverPattern.Diagonal -> {
                var x = -size.height
                while (x < size.width) { drawLine(line, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = 1.5f); x += step }
            }
            CoverPattern.Rings -> {
                val c = Offset(size.width * 0.8f, size.height * 0.25f)
                var r = step
                while (r < size.width) { drawCircle(line, r, c, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f)); r += step }
            }
            CoverPattern.Grid -> {
                var x = 0f
                while (x < size.width) { drawLine(line, Offset(x, 0f), Offset(x, size.height), 1f); x += step }
                var y = 0f
                while (y < size.height) { drawLine(line, Offset(0f, y), Offset(size.width, y), 1f); y += step }
            }
            CoverPattern.Dots -> {
                var y = step / 2
                while (y < size.height) {
                    var x = step / 2
                    while (x < size.width) { drawCircle(line.copy(alpha = 0.12f), 2.dp.toPx(), Offset(x, y)); x += step }
                    y += step
                }
            }
            CoverPattern.None -> Unit
        }
    }
}

// ── Seçici ────────────────────────────────────────────────────────────────────

/** Kapak seçici: hazır kapaklar ızgarası + "Kendi fotoğrafın" (Android Photo Picker; izin gerekmez). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverPickerSheet(viewModel: ProfileCoverViewModel, onDismiss: () -> Unit) {
    val theme = LocalAppTheme.current
    val cover by viewModel.cover.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.pickPhoto(uri)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Sinema.surface(theme)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(theme.t("Kapak seç", "Choose cover"), color = Sinema.text(theme), fontSize = SinemaType.Title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(SinemaRadius.R20)).background(Sinema.page(theme))
                    .sinemaClickable(onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }, role = Role.Button)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (viewModel.saving) CircularProgressIndicator(Modifier.size(20.dp), color = theme.accentHi, strokeWidth = 2.dp)
                else Icon(Icons.Rounded.AddPhotoAlternate, null, tint = theme.accentHi, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(theme.t("Kendi fotoğrafın", "Your own photo"), color = Sinema.text(theme), fontSize = SinemaType.Body, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (cover.customPath != null || !cover.remoteUrl.isNullOrBlank()) Icon(Icons.Rounded.Check, theme.t("Seçili", "Selected"), tint = theme.accentHi, modifier = Modifier.size(20.dp))
            }
            viewModel.error?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = Sinema.text(theme), fontSize = SinemaType.Caption)
            }
            Spacer(Modifier.height(14.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.height(300.dp)
            ) {
                items(CoverPresets, key = { it.id }) { p ->
                    val selected = cover.customPath == null && cover.remoteUrl == null && coverPresetById(cover.preset).id == p.id
                    val label = theme.t(p.labelTr, p.labelEn)
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(1.25f).clip(RoundedCornerShape(SinemaRadius.R12))
                            .sinemaClickable(onClick = { viewModel.selectPreset(p.id) }, role = Role.Button)
                            .semantics { contentDescription = theme.t("$label kapağı", "$label cover"); this.selected = selected }
                    ) {
                        ProfileCoverImage(ProfileCover(preset = p.id), Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(0.55f))))
                        Text(label, color = Color.White, fontSize = SinemaType.Caption, fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
                        if (selected) Box(
                            Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).clip(CircleShape).background(theme.accentHi),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.Check, null, tint = Color(0xFF1A1B1F), modifier = Modifier.size(16.dp)) }
                    }
                }
            }
        }
    }
}

