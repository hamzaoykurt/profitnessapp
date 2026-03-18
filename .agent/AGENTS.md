# Profitness — Agent Instructions

Profitness is a native Android fitness application built with **Kotlin** and **Jetpack Compose**. The app targets personal training and workout tracking use-cases with a premium, modern aesthetic.

## Project Identity

- **Package:** `com.avonix.profitness`
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

## Theme System — Neon Forge Dark (ONLY)

The app uses **Dark mode only** — light mode has been removed.

### Dark Mode — Neon Forge
- Background: `#0A0A0F` → `#21212A` near-black surface hierarchy
- Accents: Neon (LIME `#CBFF4D`, PURPLE `#A855F7`, CYAN `#00E5D3`, etc.)

### Theme State
```kotlin
AppThemeState(accent: AccentPreset, language: AppLanguage, notificationsEnabled: Boolean)
```
- State owned by `MainActivity` via `rememberSaveable(AppThemeStateSaver)`
- Persisted across full process kills via `ThemeRepository` (DataStore Preferences)
- **No `isDark` toggle** — always dark

### Key Files
| File | Role |
|------|------|
| `core/theme/Color.kt` | Global named color constants (dark-oriented) |
| `core/theme/AppTheme.kt` | `AppThemeState`, `AccentPreset`, surface/text extensions, `effectiveAccentColor` |
| `core/theme/Theme.kt` | `ProfitnessTheme` composable — dark only |
| `core/theme/PageAccentBloom.kt` | Radial+sweep accent glow overlay |
| `core/theme/ThemeRepository.kt` | DataStore persistence for `accent` only |
| `presentation/dashboard/DashboardScreen.kt` | `AppBackground`, `AppNavBar` |
| `presentation/components/GlassPanel.kt` | `ForgeCard`, `glassCard` Modifier |

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
2. **Dark Only:** Never add light mode code. No `if (theme.isDark)` branches. No `lightColorScheme`.
3. **Effective Accent:** Use `theme.effectiveAccentColor` / `theme.effectiveOnAccentColor` for accent references.
4. **Image Overlays Exception:** `Snow` (`#F8F8F8`) is acceptable **only** on top of dark photo scrim overlays.
5. **UI Library Discipline:** Never build primitive components from scratch — use Compose Material 3.
6. **Coroutines:** All async work launched from `ViewModel` using `viewModelScope`. Supabase calls on `Dispatchers.IO`.
7. **No Placeholders:** Connect to real Supabase data. No hardcoded demo data in new code.
8. **CinematicExerciseCard is LOCKED:** Do not change its structure. Data binding and extra buttons only.
9. **DataStore Usage:** Theme persistence via `ThemeRepository` only. No SharedPreferences.
10. **Update Memory Bank:** After each phase, update `.agent/memory/activeContext.md` and `.agent/memory/progress.md`.
11. **No Mapper Classes:** Extension functions only: `fun ProgramDto.toDomain()`.
12. **Interface-First:** Every repository has an interface. ViewModel never references `*Impl` directly.
13. **6 Memory Files Protocol:** See `.agent/memory/` — activeContext, progress, systemPatterns, techContext, productContext, projectbrief.
