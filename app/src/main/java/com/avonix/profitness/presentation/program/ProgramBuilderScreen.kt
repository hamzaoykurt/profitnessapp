package com.avonix.profitness.presentation.program

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.avonix.profitness.core.theme.*
import com.avonix.profitness.presentation.components.ForgeCard
import com.avonix.profitness.presentation.components.glassCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Data ─────────────────────────────────────────────────────────────────────

data class SavedProgram(
    val name: String,
    val days: Int,
    val focus: String,
    val icon: ImageVector
)

data class TemplateProgram(
    val title: String,
    val desc: String,
    val days: Int,
    val sessions: Int,
    val detail: String
)

private val MUSCLE_DIST_LABELS    = listOf("ÜST VÜCUT", "ALT VÜCUT", "CORE")
private val MUSCLE_DIST_PCT       = listOf("85%", "40%", "60%")
private val MUSCLE_DIST_FRACTIONS = listOf(0.85f, 0.4f, 0.6f)

private val TEMPLATES = listOf(
    TemplateProgram(
        "Push / Pull / Legs",
        "Hipertrofi odaklı 6 günlük klasik split.",
        6, 2,
        "📌 Pazartesi: Göğüs, Omuz, Triceps\n📌 Salı: Sırt, Biceps, Ön Kol\n📌 Çarşamba: Bacak + Core\n📌 Perşembe: Göğüs, Omuz, Triceps\n📌 Cuma: Sırt, Biceps, Ön Kol\n📌 Cumartesi: Bacak + Core"
    ),
    TemplateProgram(
        "Upper / Lower Split",
        "Güç ve hacim dengeli 4 günlük split.",
        4, 2,
        "📌 Pazartesi: Üst Vücut (Güç)\n📌 Salı: Alt Vücut (Güç)\n📌 Perşembe: Üst Vücut (Hacim)\n📌 Cuma: Alt Vücut (Hacim)"
    )
)

sealed class BuilderMode {
    object Choose : BuilderMode()
    object AI : BuilderMode()
    object Manual : BuilderMode()
}

@Composable
fun ProgramBuilderScreen() {
    val savedPrograms = remember { mutableStateListOf<SavedProgram>() }
    var mode by remember { mutableStateOf<BuilderMode>(BuilderMode.Choose) }
    var snackbarMsg by remember { mutableStateOf<String?>(null) }

    val theme = LocalAppTheme.current
    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        // ── Grid Background (Architect Blueprint) ───────────────────────────
        ArchitectGrid()
        PageAccentBloom()

        Crossfade(targetState = mode, label = "builder_fade") { m ->
            when (m) {
                is BuilderMode.Choose -> BuilderChooseScreen(
                    savedPrograms = savedPrograms,
                    onMode = { mode = it }
                )
                is BuilderMode.AI -> AIBuilderScreen(
                    onBack = { mode = BuilderMode.Choose },
                    onSave = { name ->
                        savedPrograms.add(SavedProgram(name, 6, "Oracle Optimized", Icons.Rounded.AutoAwesome))
                        snackbarMsg = "\"$name\" kaydedildi ✓"
                        mode = BuilderMode.Choose
                    }
                )
                is BuilderMode.Manual -> ManualBuilderScreen(
                    onBack = { mode = BuilderMode.Choose },
                    onSave = { name ->
                        savedPrograms.add(SavedProgram(name, 4, "Custom Protocol", Icons.Rounded.Construction))
                        snackbarMsg = "Program kaydedildi ✓"
                        mode = BuilderMode.Choose
                    }
                )
            }
        }

        // Custom Snackbar
        snackbarMsg?.let { msg ->
            LaunchedEffect(msg) {
                delay(2500)
                snackbarMsg = null
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 110.dp)
                    .padding(24.dp, 0.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(20.dp, 14.dp)
            ) {
                Text(msg, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// ── Architect Components ─────────────────────────────────────────────────────

@Composable
private fun ArchitectGrid() {
    val gridColor = SurfaceStroke.copy(0.3f)
    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val step = 40.dp.toPx()
                val strokeWidth = 0.5.dp.toPx()
                val gridPath = Path().apply {
                    var x = 0f
                    while (x <= size.width) { moveTo(x, 0f); lineTo(x, size.height); x += step }
                    var y = 0f
                    while (y <= size.height) { moveTo(0f, y); lineTo(size.width, y); y += step }
                }
                onDrawBehind {
                    drawPath(gridPath, gridColor, style = Stroke(strokeWidth))
                }
            }
    )
}

@Composable
private fun BuilderChooseScreen(
    savedPrograms: List<SavedProgram>,
    onMode: (BuilderMode) -> Unit
) {
    var selectedTemplate by remember { mutableStateOf<TemplateProgram?>(null) }

    selectedTemplate?.let { tpl ->
        TemplateDetailDialog(template = tpl, onDismiss = { selectedTemplate = null })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(0.dp, 0.dp, 0.dp, 140.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, 80.dp, 24.dp, 40.dp)
            ) {
                Text("ARCHITECT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 6.sp, fontWeight = FontWeight.ExtraLight)
                Spacer(Modifier.height(8.dp))
                Text("STUDIO", style = MaterialTheme.typography.displayLarge, color = Snow, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(16.dp))
                Text("Hassas protokoller tasarla. Oracle analiziyle verimliliği en üst düzeye çıkar.", color = Mist, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Light)
            }
        }

        item {
            Column(
                modifier = Modifier.padding(24.dp, 0.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                BlueprintSelectionCard(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Oracle AI",
                    sub = "OPTIMIZED PROTOCOL",
                    desc = "Yapay zeka, fiziksel verilerini analiz ederek en verimli 8 haftalık planı hazırlar.",
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = { onMode(BuilderMode.AI) }
                )
                BlueprintSelectionCard(
                    icon = Icons.Rounded.Draw,
                    title = "Manual",
                    sub = "PRECISION ARCHITECT",
                    desc = "Her egzersiz, set ve dinlenme süresi üzerinde tam kontrol.",
                    accent = CardCyan,
                    onClick = { onMode(BuilderMode.Manual) }
                )
            }
        }

        if (savedPrograms.isNotEmpty()) {
            item {
                Text(
                    "AKTİF PROTOKOLLER",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(24.dp, 48.dp, 24.dp, 16.dp)
                )
            }
            items(savedPrograms) { prog ->
                SavedBlueprintTile(prog)
            }
        }

        item {
            Text(
                "STANDART TASLAKLAR",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(24.dp, 48.dp, 24.dp, 16.dp)
            )
        }

        items(TEMPLATES) { tpl ->
            TemplateBlueprintTile(tpl, onClick = { selectedTemplate = tpl })
        }
    }
}

@Composable
private fun BlueprintSelectionCard(
    icon: ImageVector,
    title: String,
    sub: String,
    desc: String,
    accent: Color,
    onClick: () -> Unit
) {
    val iSource = remember { MutableInteractionSource() }
    val isPressed by iSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "scale")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .scale(scale)
            .clip(RoundedCornerShape(24.dp))
            .background(Surface1.copy(0.6f))
            .border(1.dp, SurfaceStroke, RoundedCornerShape(24.dp))
            .clickable(iSource, null, onClick = onClick)
            .drawBehind {
                drawRect(accent.copy(0.1f), Offset(0f, 0f), size.copy(width = 4.dp.toPx()))
            }
            .padding(24.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(sub, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text(title, color = Snow, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text(desc, color = Mist, fontSize = 13.sp, lineHeight = 20.sp, fontWeight = FontWeight.Light)
        }
        Icon(Icons.Rounded.NorthEast, null, tint = accent.copy(0.5f), modifier = Modifier.align(Alignment.TopEnd).size(20.dp))
    }
}

@Composable
private fun SavedBlueprintTile(prog: SavedProgram) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp, 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface1.copy(0.4f))
            .border(1.dp, SurfaceStroke, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(prog.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(prog.name, color = Snow, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${prog.days} GÜN • ${prog.focus.uppercase()}", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Text("AKTİF", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun TemplateBlueprintTile(template: TemplateProgram, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp, 8.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent)
            .border(1.dp, SurfaceStroke.copy(0.5f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(template.title, color = Snow, fontWeight = FontWeight.Bold)
                Text(template.desc, color = Mist, fontSize = 12.sp, fontWeight = FontWeight.Light)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${template.days} GÜN", color = Fog, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Rounded.ChevronRight, null, tint = SurfaceStroke)
            }
        }
    }
}

@Composable
private fun TemplateDetailDialog(template: TemplateProgram, onDismiss: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val theme  = LocalAppTheme.current
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(accent, theme, RoundedCornerShape(32.dp))
                .padding(32.dp)
        ) {
            Column {
                Text("SPECIFICATION", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                Spacer(Modifier.height(12.dp))
                Text(template.title, style = MaterialTheme.typography.headlineMedium, color = Snow, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(24.dp))
                
                // Muscle Distribution Simulation (Architect Style)
                Text("KAS YÜKÜ DAĞILIMI", color = Mist, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(12.dp))
                repeat(3) { i ->
                    Column(Modifier.padding(0.dp, 4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(MUSCLE_DIST_LABELS[i], color = Snow, fontSize = 10.sp)
                            Text(MUSCLE_DIST_PCT[i], color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Surface3)) {
                            Box(Modifier.fillMaxWidth(MUSCLE_DIST_FRACTIONS[i]).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                Text(template.detail, color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp, fontWeight = FontWeight.Light)
                Spacer(Modifier.height(32.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("PROTOKOLÜ UYGULA", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

// ── AI Builder Screen (Simplified/Refined) ───────────────────────────────────

@Composable
private fun AIBuilderScreen(onBack: () -> Unit, onSave: (String) -> Unit) {
    var prompt by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(2500)
            isLoading = false
            showResult = true
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(0.dp, 0.dp, 0.dp, 120.dp)) {
        DetailHeader(title = "Oracle AI", sub = "PROTOKOL ÜRETİMİ", onBack = onBack)
        
        Spacer(Modifier.height(40.dp))

        if (showResult) {
            LazyColumn(modifier = Modifier.padding(24.dp, 0.dp)) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Surface1.copy(0.4f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f), RoundedCornerShape(24.dp))
                            .padding(24.dp)
                    ) {
                        Column {
                            Text("ANALİZ SONUCU", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Spacer(Modifier.height(16.dp))
                            Text("PUSH / PULL / LEGS • 8 HAFTA", color = Snow, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("Oracle seviyeniz için 6 günlük yüksek frekanslı bir hipertrofi planı oluşturdu. Progresif yükleme için temel setler hazırlandı.", color = Mist, fontSize = 14.sp, fontWeight = FontWeight.Light)
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { onSave("Oracle Optimized Plan") },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("PROTOKOLÜ KAYDET", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, 0.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Surface1.copy(0.4f))
                    .border(1.dp, SurfaceStroke, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Text("TASARIM PARAMETRELERİ", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(16.dp))
                    BasicTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Snow, fontWeight = FontWeight.Light),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                        decorationBox = { inner ->
                            if (prompt.isEmpty()) Text("Antrenman sıklığı, hedefin ve seviyeni belirt...", color = TextMuted, fontSize = 15.sp)
                            inner()
                        }
                    )
                }
            }
            
            Spacer(Modifier.weight(1f))
            
            Box(modifier = Modifier.padding(24.dp, 0.dp)) {
                Button(
                    onClick = { if (prompt.isNotBlank()) isLoading = true },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (prompt.isNotBlank()) MaterialTheme.colorScheme.primary else Surface2),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    else Text("PROTOKOLÜ ANALİZ ET", color = if (prompt.isNotBlank()) MaterialTheme.colorScheme.onPrimary else TextMuted, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

// ── Manual Builder (Simplified UI) ───────────────────────────────────────────

@Composable
private fun ManualBuilderScreen(onBack: () -> Unit, onSave: (String) -> Unit) {
    data class ManualDay(val name: String)
    val days = remember { mutableStateListOf(ManualDay("DAY 01"), ManualDay("DAY 02")) }

    Column(modifier = Modifier.fillMaxSize().padding(0.dp, 0.dp, 0.dp, 120.dp)) {
        DetailHeader(title = "Manual", sub = "HASSAS TASARIM", onBack = onBack)
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(days) { day ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface1.copy(0.4f))
                        .border(1.dp, SurfaceStroke, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ViewQuilt, null, tint = CardCyan, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(day.name, color = Snow, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Rounded.Add, null, tint = CardCyan)
                    }
                }
            }
            
            item {
                TextButton(onClick = { days.add(ManualDay("DAY 0${days.size + 1}")) }, modifier = Modifier.fillMaxWidth()) {
                    Text("+ YENİ GÜN", color = CardCyan, fontWeight = FontWeight.Bold)
                }
            }
        }

        Button(
            onClick = { onSave("Custom Strategic Plan") },
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(24.dp, 0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("PROJEYİ KAYDET", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun DetailHeader(title: String, sub: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp, 48.dp, 12.dp, 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null, tint = Mist) }
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
            Text(title, style = MaterialTheme.typography.displayMedium, color = Snow, fontWeight = FontWeight.Black)
        }
    }
}
