# Profitness — Agent Instructions

Profitness is a native Android fitness application built with **Kotlin** and **Jetpack Compose**. The app targets personal training and workout tracking use-cases with a premium, modern aesthetic.

## Project Identity

- **Package:** `com.avonix.profitness`
- **Single Activity:** `MainActivity`
- **Min SDK:** 31 (Android 12) | **Target SDK:** 35 | **Compile SDK:** 35

## Architecture

The project follows MVVM + Clean Architecture:

- **UI Layer:** Jetpack Compose (Material 3), single-Activity, `NavController`-based navigation
- **ViewModel Layer:** Kotlin Coroutines + `StateFlow` for UDF
- **Data Layer:** Repository pattern; DataStore for preferences, Room planned for workout data
- **DI:** Hilt (KSP-based) — `@AndroidEntryPoint` on `MainActivity`, `@Singleton` repositories

## Tech Stack

| Category    | Technology                          |
| ----------- | ----------------------------------- |
| Language    | Kotlin (JVM 17)                     |
| UI          | Jetpack Compose, Material 3         |
| Navigation  | Compose Navigation                  |
| DI          | Hilt + KSP                          |
| Async       | Kotlin Coroutines, Flow             |
| Persistence | DataStore Preferences (theme)       |
| Images      | Coil Compose                        |
| Fonts       | Google Fonts — Space Grotesk        |

## Theme System — Dual-Mode Neon Forge (CURRENT)

The app supports **Dark** (default) and **Light** modes with a full dual-palette design:

### Dark Mode — Neon Forge
- Background: `#0A0A0F` → `#21212A` near-black surface hierarchy
- Accents: Neon (LIME `#CBFF4D`, PURPLE `#A855F7`, CYAN `#00E5D3`, etc.)

### Light Mode — Warm & Earthy
- Background: `#FAF8F5` → `#DDD2C2` warm cream hierarchy
- Accents: Saturated, readable variants (LIME → `#5C8A00`, CYAN → `#0891B2`, etc.)
- **All** `Snow`/`Surface*`/`Depth*` hardcoded constants replaced with `theme.*` tokens

### Theme State
```kotlin
AppThemeState(isDark: Boolean, accent: AccentPreset, language: AppLanguage, notificationsEnabled: Boolean)
```
- State owned by `MainActivity` via `rememberSaveable(AppThemeStateSaver)`
- Persisted across full process kills via `ThemeRepository` (DataStore Preferences)
- Propagated down as lambda: `MainActivity → AppNavigation → DashboardScreen → ProfileScreen`

### Key Files
| File | Role |
|------|------|
| `core/theme/Color.kt` | Global named color constants (dark-oriented, legacy aliases) |
| `core/theme/AppTheme.kt` | `AppThemeState`, `AccentPreset` (with `lightColor`/`onLightColor`), surface/text extensions, `effectiveAccentColor` |
| `core/theme/Theme.kt` | `ProfitnessTheme` composable, builds `darkColorScheme`/`lightColorScheme` |
| `core/theme/PageAccentBloom.kt` | Radial+sweep accent glow overlay, alpha-reduced in light mode |
| `core/theme/ThemeRepository.kt` | DataStore persistence for `isDark` + `accent` |
| `presentation/dashboard/DashboardScreen.kt` | `AppBackground`, `AppNavBar` — both theme-aware |
| `presentation/components/GlassPanel.kt` | `ForgeCard`, `glassCard` Modifier — fully theme-aware |

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

1. **Memory Bank First:** Always read ALL files in `.agent/memory/` before starting any task.
2. **Theme-Aware Always:** Never use hardcoded `Color.kt` constants (`Snow`, `Mist`, `Surface*`, `Depth*`, `TextPrimary`, etc.) in composables. Always use `LocalAppTheme.current.text0/text1/text2/bg0/bg1/bg2/bg3/stroke` or `MaterialTheme.colorScheme.primary`.
3. **Effective Accent:** Use `theme.effectiveAccentColor` / `theme.effectiveOnAccentColor` when referencing accent in non-image contexts. These automatically pick the readable variant for light mode.
4. **Image Overlays Exception:** `Snow` (`#F8F8F8`) is acceptable **only** on top of dark photo scrim overlays (hero cards, cinematic exercise cards) where the background is always dark regardless of theme.
5. **UI Library Discipline:** Never build primitive components (buttons, dialogs) from scratch — use Compose Material 3, then style them.
6. **Coroutines:** All async work must be launched from a `ViewModel` using `viewModelScope`. Never from Composable scope (exception: `LaunchedEffect` side effects).
7. **No Placeholders:** If a screen needs real data, use the existing demo data patterns.
8. **CinematicExerciseCard is LOCKED:** Do not change its structure. Color/badge updates only if explicitly requested.
9. **DataStore Usage:** Theme persistence is handled exclusively by `ThemeRepository`. Do not introduce SharedPreferences or other persistence for theme state.
10. **Update Memory Bank:** After significant changes, update `.agent/memory/activeContext.md` and `.agent/memory/progress.md`.
