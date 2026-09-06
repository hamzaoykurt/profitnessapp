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

## Tema Sistemi — Neon Forge + Mineral Light (GÜNCEL)

> **2026-09-04 kararı:** Light mode ürün kapsamındadır. Eski sıcak/kahverengi palet kullanılmaz; açık mod için serin nötr yüzeyler ve düşük opaklıklı glow/gölgeler kullanılır.

### Genel Yapı

```
ProfitnessTheme(themeState: AppThemeState)
    ↓
darkColorScheme / lightColorScheme
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

### Surface / Text Token'ları

`LocalAppTheme.current` extension property olarak:

| Token    | Dark       | Light      |
|----------|------------|------------|
| `bg0`    | `#0A0A0F`  | `#F1F3F6`  |
| `bg1`    | `#111117`  | `#F8F9FB`  |
| `bg2`    | `#18181F`  | `#FFFFFF`  |
| `bg3`    | `#21212A`  | `#E8EBF0`  |
| `stroke` | `#2A2A35`  | `#D5DAE2`  |
| `text0`  | `#F8F8F8`  | `#151820`  |
| `text1`  | `#9A9AB0`  | `#515967`  |
| `text2`  | `#5A5A72`  | `#808896`  |

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

### Surface Roles (GlassPanel.kt)

```kotlin
Modifier.premiumSolidSurface(accent, theme, shape)
Modifier.floatingGlassSurface(accent, theme, shape)
Modifier.insetControlSurface(accent, theme, shape)
```

- `premiumSolidSurface`: standart içerik kartı; opaque katman, ince rim ve dış elevation
- `floatingGlassSurface`: yalnız alt nav, medya chrome'u ve geçici overlay gibi yüzen elemanlar
- `insetControlSurface`: arama/form alanlarında içeri gömülmüş koyu-alt kenar ve üst highlight
- `glassCard` geriye dönük alias olarak solid role yönlenir; yeni kod doğrudan rolü belirtir

### CinematicExerciseCard

```kotlin
CinematicExerciseCard(exercise: Exercise, index: Int)
```

- Gerçek fotoğraf bg + `Color.Black` scrim overlay
- Kategori pill: Strength=Lime, Bodyweight=CardCyan, Cable=CardPurple
- Expand animasyonu (set tracker, complete button)
- Veri/etkileşim sözleşmesi korunur; görsel malzeme, tema uyumu ve tactile efektler ürün yönlendirmesiyle geliştirilebilir

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
- Dark: `radialPeak=0.055f`, light: `radialPeak=0.012f`; sayfa geneli renk yıkaması oluşturmaz
- Tüm ana ekranlarda `AppBackground` üstüne katman

### PremiumButton / GhostButton / PremiumIconButton (PremiumButton.kt)

```kotlin
PremiumButton(text, onClick, modifier, leadingIcon)
GhostButton(text, onClick, modifier, isEnabled)
PremiumIconButton(onClick, icon, contentDescription)
```

- Primary: solid brand accent; secondary/icon: opak nötr tonal yüzey
- Press: `scale(0.98)`, yaklaşık 120ms; bevel, ledge, glow ve bounce kullanılmaz
- `PremiumButton` ve `PremiumIconButton` keyfi renk override kabul etmez; tek interaction accent tema tarafından belirlenir
- Kontroller ayrı alt kaide, rim veya üst specular çizgi kullanmaz
- Kaydırılan yüzeylerde shadow zinciri ve sürekli dekoratif glow kullanılmaz

---

## Tipografi

- **Display/Headline:** `40–48sp` / `28–32sp`, `Bold`
- **Stat Sayıları:** `28–42sp`, `SemiBold/Bold`, tabular hierarchy
- **Labels:** `11–12sp`, `SemiBold`, sınırlı letter spacing; uppercase yalnız gerçek label'larda
- **Body:** `14–16sp`, `Normal/Medium`
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
| Performance Luxury Dark + Mineral Light | Nötr near-black / mineral canvas; mekanik renk tersleme yok |
| Solid / floating glass / inset yüzey rolleri | Glass her yerde kullanılmaz; içerik, chrome ve girişler farklı derinlik modeli taşır |
| Tek-accent aksiyon hiyerarşisi | Bir görünümde yalnız preferred primary renkli; secondary nötr, renkli istisnalar semantik |
| `rememberSaveable` + DataStore | Rotation: hızlı state; process kill: kalıcı storage |
| Interface-first repository | Test edilebilirlik, DI swap, backend bağımsızlığı |
| Extension mapper, no mapper class | `fun Dto.toDomain()` — boilerplate azaltır |
| BaseViewModel<S,E> | Typed state + one-time navigation/toast events |
| AppModule abstract class | `@Binds` abstract method + `@Provides` companion object |
| Random() yasak | Recomposition'da flicker — deterministik hash kullan |
| CinematicExerciseCard sözleşmesi | Veri/etkileşim korunur; görsel katman ürün yönüne göre gelişebilir |
| Legacy alias sistemi | Eski ekranlar kırılmasın, zamanı gelince temizlenir |
| Restrained glass nav | 64px sınıfında, yüksek opaklık, minimal blur hissi, küçük accent active state |
