# Profitness — Agent Instructions

Profitness is a native Android fitness application built with **Kotlin** and **Jetpack Compose**. The app targets personal training and workout tracking use-cases with a premium, modern aesthetic.

## Project Identity

- **Package:** `com.cosmibit.profitness`
- **Single Activity:** `MainActivity`
- **Min SDK:** 31 (Android 12) | **Target SDK:** 35 | **Compile SDK:** 35
- **Supabase Project ID:** `dkcriptafzdrynsilxku` (region: ap-southeast-1)

## Architecture

The project follows MVVM + Clean Architecture:

- **UI Layer:** Jetpack Compose (Material 3), single-Activity, `NavController`-based navigation
- **ViewModel Layer:** Kotlin Coroutines + `StateFlow` for UDF, `SharedFlow` for one-time events
- **Data Layer:** Feature-based repository pattern; Supabase (PostgreSQL + Auth + Storage) for backend
- **DI:** Hilt (KSP-based) — `@AndroidEntryPoint` on `MainActivity`, `@Singleton` repositories

## Tech Stack

| Category    | Technology                          |
| ----------- | ----------------------------------- |
| Language    | Kotlin (JVM 17)                     |
| UI          | Jetpack Compose, Material 3         |
| Navigation  | Compose Navigation                  |
| DI          | Hilt + KSP                          |
| Async       | Kotlin Coroutines, Flow             |
| Backend     | Supabase (PostgreSQL + Auth + Storage + Edge Functions) |
| AI          | Gemini API (google-ai SDK)          |
| Persistence | DataStore Preferences (theme/settings) |
| Images      | Coil Compose                        |
| Fonts       | Google Fonts — Space Grotesk        |
| Billing     | Google Play Billing Library v6+     |

## Theme System — Performance Luxury Dual Mode

The user's three September 2026 briefs and latest feedback override historical design experiments in the memory bank. Do not revive old neon, pillow, full-rim or glass-everywhere treatments. Track acceptance in `UI_REDESIGN_AUDIT.md`; shared-token propagation and a successful build do not prove every screen is finished.

The app supports **dark and light modes**. Light mode uses a purpose-built cool-neutral palette; it is not a mechanical inversion of dark colors.

### Dark Mode — Graphite
- Background: `#090A0B`; quiet utility surfaces and continuously lit opaque elevated cards
- Default accent: ember orange; existing user-selected colors remain supported

### Light Mode — Mineral
- Background: `#F2F4F6` with opaque opal surfaces and neutral depth; no lilac wash
- Accents: darker readable variants from `AccentPreset.lightColor`
- Shadows, borders, and bloom use lower opacity than dark mode

### Material Roles
- `premiumSolidSurface`: normal content cards; borderless matte tonal surfaces with restrained elevation
- `floatingGlassSurface`: transient overlays only; navigation is a solid dock, not glass
- `insetControlSurface`: recessed search and form controls
- `PremiumButton` / `GhostButton`: quiet primary/neutral controls; selected fields use shallow inward material, never an external lower ledge or exaggerated raised face
- One view uses one interaction accent: only the preferred primary action receives an accent surface; secondary actions stay neutral
- Extra colors are reserved for semantic meaning such as destructive, warning, success, rank, or distinct chart series

### Theme State
```kotlin
AppThemeState(isDark: Boolean, accent: AccentPreset, language: AppLanguage, notificationsEnabled: Boolean)
```
- State owned by `MainActivity` via `rememberSaveable(AppThemeStateSaver)`
- Persisted across full process kills via `ThemeRepository` (DataStore Preferences)
- `isDark` is user-selectable in Appearance settings and persisted

### Key Files
| File | Role |
|------|------|
| `core/theme/Color.kt` | Global named color constants (dark-oriented) |
| `core/theme/AppTheme.kt` | `AppThemeState`, `AccentPreset`, surface/text extensions, `effectiveAccentColor` |
| `core/theme/Theme.kt` | `ProfitnessTheme` composable — dark/light schemes |
| `core/theme/PageAccentBloom.kt` | Radial+sweep accent glow overlay |
| `core/theme/ThemeRepository.kt` | DataStore persistence for appearance settings |
| `presentation/dashboard/DashboardScreen.kt` | `AppBackground`, `AppNavBar` |
| `presentation/components/GlassPanel.kt` | solid, floating-glass, and inset surface roles |
| `presentation/components/PremiumButton.kt` | tactile primary, secondary, and icon controls |

## Repository Pattern (Interface-First)

```
data/{feature}/FooRepository.kt        ← interface
data/{feature}/FooRepositoryImpl.kt    ← Supabase impl
data/{feature}/dto/FooDto.kt           ← Supabase JSON model
fun FooDto.toDomain() = ...            ← extension function mapper (NOT a mapper class)
di/AppModule.kt                        ← @Binds interface → impl
```

**Rules:**
- Repository interface and impl are ALWAYS separate files
- ViewModel depends ONLY on the interface, never the impl
- Mappers are always extension functions: `fun Dto.toDomain()` — **mapper class yazmak yasaktır**
- Use Cases only for `CalorieCalculationUseCase` and `XpCalculationUseCase`

## BaseViewModel Pattern

```kotlin
abstract class BaseViewModel<S : Any, E : Any>(initial: S) : ViewModel() {
    private val _state = MutableStateFlow(initial)
    val state = _state.asStateFlow()
    private val _events = MutableSharedFlow<E>()
    val events = _events.asSharedFlow()
    protected fun updateState(block: (S) -> S) = _state.update(block)
    protected fun emitEvent(event: E) = viewModelScope.launch { _events.emit(event) }
}
```

## Supabase Rules

- All Supabase calls must run on `Dispatchers.IO`
- Always wrap with `.catch { }` or `try/catch` — never let network errors crash the app
- Use `supabase.from("table").select().decodeList<Dto>()` pattern
- RLS is active on all tables — user sees only their own data

## Gemini Rules

- System prompt is **immutable** — never change the Oracle persona prompt
- All program suggestions must be validated against strict JSON schema before saving to DB
- JSON schema: `{days: [{title, exercises: [{name, sets, reps, target_muscle}]}]}`
- Gemini calls run on `Dispatchers.IO` with `.catch { }`

## Build Commands

```bash
# Debug build (Windows)
.\gradlew.bat assembleDebug

# Kotlin compile only (fast check)
.\gradlew.bat compileDebugKotlin

# Run on connected device
.\gradlew.bat installDebug

# Unit tests
.\gradlew.bat testDebugUnitTest
```

## Agent Guidelines

1. **Memory Bank First:** Always read ALL files in `.agent/memory/` before starting any task. Start with `activeContext.md`.
2. **Dual Mode:** All non-image surfaces and text must use `AppThemeState`/Material theme tokens. Keep photo scrims deliberately dark.
3. **Effective Accent:** Use `theme.effectiveAccentColor` / `theme.effectiveOnAccentColor` for accent references.
4. **Image Overlays Exception:** `Snow` (`#F8F8F8`) is acceptable **only** on top of dark photo scrim overlays.
5. **UI Library Discipline:** Never build primitive components from scratch — use Compose Material 3.
6. **Coroutines:** All async work launched from `ViewModel` using `viewModelScope`. Supabase calls on `Dispatchers.IO`.
7. **No Placeholders:** Connect to real Supabase data. No hardcoded demo data in new code.
8. **CinematicExerciseCard:** Preserve its interaction/data contract. The list card is image-free; exercise imagery loads only in the dedicated how-to detail so arbitrary aspect ratios are not cropped and scrolling stays light.
9. **DataStore Usage:** Theme persistence via `ThemeRepository` only. No SharedPreferences.
10. **Update Memory Bank:** After each phase, update `.agent/memory/activeContext.md` and `.agent/memory/progress.md`.
11. **No Mapper Classes:** Extension functions only: `fun ProgramDto.toDomain()`.
12. **Interface-First:** Every repository has an interface. ViewModel never references `*Impl` directly.
13. **6 Memory Files Protocol:** See `.agent/memory/` — activeContext, progress, systemPatterns, techContext, productContext, projectbrief.
14. **Material role discipline:** Performance Luxury is the default. Content cards are opaque borderless tonal surfaces; only bottom navigation, floating tools, modals, and rare hero surfaces may use restrained high-opacity glass.
15. **Single-accent action hierarchy:** Never assign arbitrary category colors to neighboring buttons or clickable card chrome. Use the theme primary for the preferred action and neutral surfaces for alternatives; semantic status colors are the exception.
16. **Performance-first depth:** Do not stack shadows, run decorative ambient glow animations, or load full-card images in scrolling lists. Dark depth is primarily tonal; light depth may use one subtle ambient shadow.
