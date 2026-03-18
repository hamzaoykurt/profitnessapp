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

## Backend Mimari Kararları (FAZ 1 — 2026-03-15)

### Repository Pattern (Interface-First)

```kotlin
// data/auth/AuthRepository.kt — SADECE INTERFACE
interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
    fun isLoggedIn(): Boolean
    suspend fun sendPasswordReset(email: String): Result<Unit>
}

// data/auth/AuthRepositoryImpl.kt — IMPL, sadece Hilt'e görünür
class AuthRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : AuthRepository { ... }

// di/AppModule.kt — Hilt bağlaması
@Binds @Singleton abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
```

**Kural:** ViewModel her zaman interface'e inject edilir, asla `Impl`'e.

### Extension Function Mapper (Mapper Class Yasak)

```kotlin
// data/auth/dto/UserDto.kt
@Serializable data class UserDto(val id: String, val email: String)

// Mapper — extension function olarak, ayrı mapper class değil
fun UserDto.toDomain() = User(id = id, email = email)
```

**Kural:** `fun Dto.toDomain()` şeklinde extension function. `UserMapper`, `ProgramMapper` gibi ayrı mapper class yazmak yasak.

### AppModule: abstract class + companion object

```kotlin
// di/AppModule.kt
@Module @InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    companion object {
        @Provides @Singleton
        fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(GoTrue)
            install(Postgrest)
            install(Storage)
        }
    }
}
```

**Kural:** `@Binds` için abstract class gerekli. `@Provides` ise `companion object` içinde.

### Supabase Çağrı Kuralları

```kotlin
// ✅ Doğru
override suspend fun signIn(email: String, password: String): Result<Unit> =
    withContext(Dispatchers.IO) {
        runCatching {
            supabase.gotrue.signInWith(Email) { this.email = email; this.password = password }
        }
    }

// ❌ Yanlış — IO dispatcher yok, runCatching yok
supabase.gotrue.signInWith(Email) { ... }
```

**Kurallar:**
- Tüm Supabase çağrıları `withContext(Dispatchers.IO)` içinde
- Hata yönetimi `runCatching {}` ile, `try/catch` değil
- Repository `Result<T>` döner, exception fırlatmaz

### BaseViewModel<S, E> Pattern

```kotlin
// State (S) + one-time Events (E)
abstract class BaseViewModel<S : Any, E : Any>(initialState: S) : ViewModel() {
    val uiState: StateFlow<S>             // UI continuous state
    val events: SharedFlow<E>             // one-time: navigation, toast

    protected fun updateState(update: (S) -> S)
    protected fun sendEvent(event: E)
}

// Events gerektiren ViewModel
sealed class AuthEvent { object NavigateToDashboard : AuthEvent() }
class AuthViewModel : BaseViewModel<AuthState, AuthEvent>(AuthState())

// Events gerektirmeyen ViewModel
class WorkoutViewModel : BaseViewModel<WorkoutScreenState, Nothing>(WorkoutScreenState())
```

**Kural:** Navigation veya toast gibi one-time event'ler için `sendEvent()` kullan, `uiState`'e flag ekleme.

---

## Tema Sistemi — Dark Only: Neon Forge (GÜNCEL)

> **NOT:** Light mode FAZ 8B'de tamamen kaldırılacak. Şu an kod içinde hâlâ light palette var ama yeni kod yazan hiç light bileşeni eklememeli.

### Genel Yapı

```
ProfitnessTheme(themeState: AppThemeState)
    ↓
darkColorScheme  (light mode kaldırılıyor — FAZ 8B)
    ↓
MaterialTheme  +  LocalAppTheme (CompositionLocal)
```

### AccentPreset

| Preset | Dark (neon) |
|--------|-------------|
| LIME   | `#CBFF4D`   |
| PURPLE | `#A855F7`   |
| CYAN   | `#00E5D3`   |
| ORANGE | `#F97316`   |
| PINK   | `#EC4899`   |
| BLUE   | `#3B82F6`   |

### Surface / Text Token'ları (Dark Only)

`LocalAppTheme.current` extension property olarak:

| Token    | Dark       |
|----------|------------|
| `bg0`    | `#0A0A0F`  |
| `bg1`    | `#111117`  |
| `bg2`    | `#18181F`  |
| `bg3`    | `#21212A`  |
| `stroke` | `#2A2A35`  |
| `text0`  | `#F8F8F8`  |
| `text1`  | `#9A9AB0`  |
| `text2`  | `#5A5A72`  |

### Image Overlay İstisnası

`Snow (#F8F8F8)` **fotoğraf scrim üzerinde** kullanılabilir (hero card, CinematicExerciseCard).
Bu bağlamlarda arka plan her zaman `Color.Black.copy(alpha)`.

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
| Neon Forge Dark-only → Dual-Mode → Dark-Only | Light mod kaldırılacak (FAZ 8B). Şimdilik kod içinde var ama aktif kullanılmayacak |
| `rememberSaveable` + DataStore | Rotation: hızlı state; process kill: kalıcı storage |
| Interface-first repository | Test edilebilirlik, DI swap, backend bağımsızlığı |
| Extension mapper, no mapper class | `fun Dto.toDomain()` — boilerplate azaltır |
| BaseViewModel<S,E> | Typed state + one-time navigation/toast events |
| AppModule abstract class | `@Binds` abstract method + `@Provides` companion object |
| Random() yasak | Recomposition'da flicker — deterministik hash kullan |
| CinematicExerciseCard locked | Kullanıcı onaylı, yapısal değişiklik yasak |
| Legacy alias sistemi | Eski ekranlar kırılmasın, zamanı gelince temizlenir |
| Floating pill nav | Ref. görsele uyum — yüzen pill, accent dot active state |
