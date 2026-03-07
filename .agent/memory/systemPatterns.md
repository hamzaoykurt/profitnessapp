# System Patterns — Profitness

## Genel Mimari

```
┌─────────────────────────────────────────┐
│              UI Layer                    │
│  Composables → ViewModel → UI State     │
├─────────────────────────────────────────┤
│            Domain Layer                  │
│         Use Cases / Interactors         │
├─────────────────────────────────────────┤
│             Data Layer                   │
│  Repository → (Room DB / DataStore /    │
│                Retrofit API)            │
└─────────────────────────────────────────┘
```

- **Pattern:** MVVM + Clean Architecture
- **State:** Unidirectional Data Flow (UDF) — ViewModel exposes `StateFlow<UiState>`
- **DI:** Hilt — `@Singleton` repositories, `@AndroidEntryPoint` on Activity

---

## Navigation Mimarisi

- **Single Activity:** `MainActivity` → `NavHost`
- **Route Tanımı:** Sealed class `DashboardTab`
- **Nav Bileşeni:** `AppNavBar` — tema-aware yüzen pill, aktif ikon üstünde nokta göstergesi
- **Ana Ekranlar:** Workout, Program (Studio), AI Coach (Oracle), News (Muse), Profile

---

## Tema Sistemi — v4 Dual-Mode Neon Forge (GÜNCEL)

### Genel Yapı

```
ProfitnessTheme(themeState: AppThemeState)
    ↓
darkColorScheme  OR  lightColorScheme
    ↓
MaterialTheme  +  LocalAppTheme (CompositionLocal)
```

### AppThemeState

```kotlin
data class AppThemeState(
    val isDark: Boolean = true,
    val accent: AccentPreset = AccentPreset.LIME,
    val language: AppLanguage = AppLanguage.TR,
    val notificationsEnabled: Boolean = true
)
```

- `rememberSaveable(AppThemeStateSaver)` — rotation'a karşı anlık state
- `ThemeRepository` (DataStore) — process kill'e karşı kalıcı depolama

### AccentPreset

Her preset'in iki renk versiyonu var:

| Preset | Dark (neon) | Light (saturated) |
|--------|-------------|-------------------|
| LIME   | `#CBFF4D`   | `#5C8A00`         |
| PURPLE | `#A855F7`   | `#7C3AED`         |
| CYAN   | `#00E5D3`   | `#0891B2`         |
| ORANGE | `#F97316`   | `#EA580C`         |
| PINK   | `#EC4899`   | `#DB2777`         |
| BLUE   | `#3B82F6`   | `#2563EB`         |

`theme.effectiveAccentColor` → dark modda `.color`, light modda `.lightColor` döner.
`theme.effectiveOnAccentColor` → dark modda `.onColor`, light modda `.onLightColor` döner.

### Surface / Text Token'ları

`LocalAppTheme.current` extension property olarak:

| Token    | Dark       | Light (Warm Earthy) |
|----------|------------|---------------------|
| `bg0`    | `#0A0A0F`  | `#FAF8F5`           |
| `bg1`    | `#111117`  | `#F2EDE7`           |
| `bg2`    | `#18181F`  | `#E8E0D5`           |
| `bg3`    | `#21212A`  | `#DDD2C2`           |
| `stroke` | `#2A2A35`  | `#CEC0AD`           |
| `text0`  | `#F8F8F8`  | `#1A1410`           |
| `text1`  | `#9A9AB0`  | `#5C4E3E`           |
| `text2`  | `#5A5A72`  | `#9E8A72`           |

### Image Overlay İstisnası

`Snow (#F8F8F8)` **fotoğraf scrim üzerinde** kullanılabilir (hero card, CinematicExerciseCard).
Bu bağlamlarda arka plan her zaman `Color.Black.copy(alpha)` — tema durumundan bağımsız.

---

## Theme Persistence Pattern

```kotlin
// ThemeRepository.kt — @Singleton, DataStore-backed
val themeFlow: Flow<AppThemeState>       // okuma
suspend fun saveTheme(state: AppThemeState)  // yazma

// MainActivity.kt
@Inject lateinit var themeRepository: ThemeRepository

// İlk yükleme (process kill sonrası)
LaunchedEffect(persisted) {
    persisted?.let { themeState = themeState.copy(isDark = it.isDark, accent = it.accent) }
}

// Her değişimde async kaydet
onThemeChange = { newState ->
    themeState = newState
    lifecycleScope.launch { themeRepository.saveTheme(newState) }
}
```

---

## Renk Kullanım Kuralları

```kotlin
// ✅ Doğru — tema-aware token
val theme = LocalAppTheme.current
Text("Hello", color = theme.text0)
Box(Modifier.background(theme.bg2))
val accent = theme.effectiveAccentColor

// ✅ Doğru — MaterialTheme da tema-aware
Text("Hello", color = MaterialTheme.colorScheme.onBackground)

// ❌ Yanlış — hardcoded dark constant (light modda görünmez/yanlış renk)
Text("Hello", color = Snow)           // Snow = #F8F8F8 — light bg'de görünmez
Box(Modifier.background(Surface2))    // Surface2 = #18181F — light modda siyah kart
Text("Hello", color = TextPrimary)    // TextPrimary = #F8F8F8 — aynı sorun

// ✅ İstisna — fotoğraf scrim üstü
Text("TITLE", color = Snow)           // CinematicExerciseCard içinde, Color.Black scrim üstünde ✓
```

---

## Component Patterns

### ForgeCard (GlassPanel.kt)

```kotlin
ForgeCard(modifier, shape, glowColor, elevation) { ... }
```

- `theme.bg2` zemin (dark: `#18181F`, light: `#E8E0D5`)
- Accent-tinted üst rim light
- Tema-aware shadow: dark → `Color.Black`, light → `#6B4E2A` (warm brown)
- `glowColor` parametresi → accent corner wash ve shadow glow

**Aliases:** `GlassPanel`, `GlassCard`, `ObsidianCard`, `ObsidianCardPro`, `ForgeCardPro`, `ForgeCardSmall`

### glassCard Modifier (GlassPanel.kt)

```kotlin
Modifier.glassCard(accent, theme, shape)
```

- `theme.bg1` semi-transparent base (dark: `0.75f`, light: `0.90f`)
- Accent bleed gradient (light modda %50 daha subtle)
- Depth shadow: dark → `Color.Black.copy(0.30f)`, light → `Color(0xFF6B4E2A).copy(0.10f)`
- Accent + stroke border

### CinematicExerciseCard (**LOCKED — YAPISAL DEĞİŞİKLİK YOK**)

```kotlin
CinematicExerciseCard(exercise: Exercise, index: Int)
```

- Gerçek fotoğraf bg + `Color.Black` scrim overlay
- Kategori pill: Strength=Lime, Bodyweight=CardCyan, Cable=CardPurple
- Expand animasyonu (set tracker, complete button)
- **Kullanıcı onaylı — yapı değiştirilemez**

### CircularProgressRing

```kotlin
CircularProgressRing(progress, size, label, trackColor, ringColor)
```

- `theme.effectiveAccentColor` sweep gradient ark, animasyonlu
- WorkoutScreen header + BigStatCard içinde kullanılıyor

### AppNavBar (DashboardScreen.kt)

- `theme.bg0` semi-transparent pill, `theme.stroke` border
- Aktif: `effectiveAccentColor` indicator nokta + Spring scale animasyonu
- Pasif ikon tint: `theme.text2`
- Shadow: dark → `Color.Black`, light → `Color.Black.copy(0.12f)` (subtle)

### PageAccentBloom (core/theme/PageAccentBloom.kt)

- Sağ üst köşe radial glow + diagonal sweep
- Dark: `radialPeak=0.16f`, light: `radialPeak=0.07f` (yarı strength)
- Tüm ana ekranlarda `AppBackground` üstüne katman

### GhostButton / ForgeButton (PremiumButton.kt)

```kotlin
GhostButton(text, onClick, modifier, isEnabled)
```

- `GhostButton`: `theme.bg2` bg, `theme.stroke` border, `theme.text1/text2` label
- `ForgeButton`: `effectiveAccentColor` solid fill, spring scale press, 3D shadow

---

## Tipografi

- **Display/Headline:** `displayLarge/headlineMedium`, `FontWeight.Black`
- **Stat Sayıları:** `28sp`, `FontWeight.Black` — bold minimalist data display
- **Labels:** `9–11sp`, `ExtraBold`, `letterSpacing=1.5–2sp`, UPPERCASE
- **Body:** `16sp`, `FontWeight.Light` / `Normal`, `lineHeight=27sp`
- **Font:** Space Grotesk (Google Fonts — `ui-text-google-fonts`)

---

## Deterministic Grain Kuralı

```kotlin
// ✅ Doğru — recomposition'da flicker yok
val hash = (xi * 7 + yi * 13) % 100
if (hash < 2) drawCircle(...)

// ❌ Yanlış — her recompose'da farklı sonuç
val x = Random.nextFloat() * width
```

---

## Legacy Alias Sistemi

Her yeni bileşen ailesi oluşturulduğunda eski isimler alias olarak tutulur.

```kotlin
@Composable fun GlassPanel(...) = ForgeCard(...)
@Composable fun ObsidianCard(...) = ForgeCard(...)
val Abyss    = Surface0
val Snow     = TextPrimary   // Color.kt'de sabit — composable'da theme.text0 kullan
```

---

## Bilinen Mimari Kararlar

| Karar | Sebep |
|-------|-------|
| Glassmorphic → Solid Forge | Render/tutarsızlık sorunları |
| Solid Forge → Matte Obsidian | Kullanıcı: Apple/Porsche seviye minimallik |
| Matte Obsidian → Neon Forge | Kullanıcı: Referans fitness uygulamalarına eşleşme |
| Neon Forge Dark-only → Dual-Mode | Light mod "tersine çevrilmiş dark" sorunu çözüldü |
| Light palette: Warm & Earthy | Soğuk gri yerine krem/bej — fitness için daha inviting |
| Light accent'ler saturated/koyu | Neon renkler light bg'de kontrast sorunu — readable varyant |
| `rememberSaveable` + DataStore | Rotation: hızlı state; process kill: kalıcı storage |
| Random() yasak | Recomposition'da flicker — deterministik hash kullan |
| CinematicExerciseCard locked | Kullanıcı onaylı, yapısal değişiklik yasak |
| Legacy alias sistemi | Eski ekranlar kırılmasın, zamanı gelince temizlenir |
| Floating pill nav | Ref. görsele uyum — yüzen pill, accent dot active state |
