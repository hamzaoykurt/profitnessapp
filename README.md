# Profitness

**Profitness** is a premium Android fitness app built entirely with Kotlin and Jetpack Compose. It features a dark, glassmorphic design system ("Neon Forge"), AI-powered coaching, workout tracking, program building, and performance analytics.

---

## Features

| Screen | Description |
|---|---|
| **Workout (FORGE)** | Daily workout view with exercise cards, progress tracking, circular progress ring, and day selector |
| **Program Builder (PLAN)** | Pre-built program templates with muscle distribution charts; AI-powered and manual program builders |
| **AI Coach (ORACLE)** | Conversational AI fitness coach for personalized guidance |
| **News (MUSE)** | Curated fitness and health news feed |
| **Profile (USER)** | Performance metrics, weekly activity chart, trophy gallery, and theme customization |

---

## Design System — Neon Forge

The app uses a custom dark design system with a fully dynamic accent color.

**Surface hierarchy:**

| Token | Dark | Light |
|---|---|---|
| `bg0` | `#0A0A0F` | `#F5F5FA` |
| `bg1` | `#111117` | `#EDEDE4` |
| `bg2` | `#18181F` | `#E4E4EE` |
| `bg3` | `#21212A` | `#DADAE8` |

**Accent presets:** LIME · PURPLE · CYAN · ORANGE · PINK · BLUE

**Glass card system (`Modifier.glassCard`):**
A reusable Modifier extension (defined in `GlassPanel.kt`) that applies a frosted-glass layered background — semi-transparent base, accent color bleed, top shimmer, bottom depth shadow, and an accent-tinted border. Used across the bottom nav bar, day selector buttons, metric cards, program cards, and the weekly activity section.

---

## Architecture

```
app/
├── core/
│   └── theme/          # AppThemeState, AccentPreset, color tokens, ProfitnessTheme
├── di/                 # Hilt AppModule
├── presentation/
│   ├── auth/           # Login / register
│   ├── dashboard/      # Root shell: AppNavBar, DashboardScreen, AppBackground
│   ├── navigation/     # NavGraph, Routes
│   ├── workout/        # WorkoutScreen, DaySelector, ExerciseCards
│   ├── program/        # ProgramBuilderScreen, AI + Manual builders, TemplateDetailDialog
│   ├── aicoach/        # AICoachScreen
│   ├── news/           # NewsScreen
│   ├── profile/        # ProfileScreen, PerformanceDetailScreen, MetricCard, WeeklyActivity
│   └── components/     # GlassPanel (ForgeCard, glassCard), CinematicExerciseCard, PremiumButton
└── MainActivity.kt
```

**Pattern:** Single-activity, ViewModel + StateFlow, Hilt DI, Compose Navigation.

---

## Tech Stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| DI | Hilt |
| Async | Kotlin Coroutines |
| Image loading | Coil |
| Fonts | Google Fonts (Space Grotesk) |
| Build | Gradle KTS + KSP |

**Min SDK:** 31 (Android 12)
**Target SDK:** 34
**Language:** Kotlin

---

## Getting Started

1. Clone the repo
2. Open in Android Studio (latest stable)
3. Run the `app` configuration on a device or emulator running Android 12+

```bash
./gradlew assembleDebug        # Build debug APK
./gradlew assembleRelease      # Build release APK
./gradlew installDebug         # Build and install on connected device
```

---

## Bottom Navigation Bar

The nav bar is a floating pill with an **expanding selected item** animation:

- **Selected:** Icon circle (accent fill) + animated label expanding horizontally
- **Inactive:** Semi-transparent glass circle with subtle stroke border
- **Background:** Layered frosted glass — `bg0 @ 76%` base · accent bleed · top shimmer · bottom depth shadow · accent-tinted border · `30dp` drop shadow

---

## License

© 2024 Avonix. All rights reserved.
