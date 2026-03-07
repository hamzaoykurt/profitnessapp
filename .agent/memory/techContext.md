# Tech Context — Profitness

## Geliştirme Ortamı

- **IDE:** Android Studio (Ladybug veya üzeri önerilir)
- **OS:** Windows 11
- **JDK:** 17 (compileOptions'da sabit)
- **Gradle:** Kotlin DSL (`build.gradle.kts`)
- **Build System:** Gradle 8.x (AGP `8.11.2`)

---

## Bağımlılıklar

### Ana Bağımlılıklar

| Kütüphane | Versiyon | Kullanım Amacı |
|-----------|----------|----------------|
| `androidx.compose.bom` | `2024.12.01` | Compose sürüm yönetimi |
| `material3` | BOM | UI component kütüphanesi |
| `material-icons-extended` | BOM | Genişletilmiş ikon seti |
| `navigation-compose` | `2.7.7` | In-app navigasyon |
| `hilt-android` + `hilt-compiler` | `2.51.1` | Dependency Injection (KSP) |
| `hilt-navigation-compose` | `1.2.0` | Hilt + Compose Navigation |
| `lifecycle-viewmodel-compose` | `2.8.2` | ViewModel Compose entegrasyonu |
| `compose-animation` | BOM | Geçiş ve animasyon desteği |
| `kotlinx-coroutines-android` | `1.8.1` | Async işlemler |
| `datastore-preferences` | `1.1.1` | Tema tercihi kalıcı depolama |
| `coil-compose` | `2.6.0` | Asenkron görüntü yükleme |
| `ui-text-google-fonts` | BOM | Space Grotesk font |

### Planlanan (Henüz Eklenmedi)

| Kütüphane | Kullanım Amacı |
|-----------|----------------|
| `room` + `room-compiler` | Workout/exercise lokal veritabanı |
| `retrofit` + `converter-gson` | AI endpoint REST API iletişimi |

### Test Bağımlılıkları

| Kütüphane | Kullanım Amacı |
|-----------|----------------|
| `junit:4.13.2` | Unit testler |
| `ui-test-junit4` | Compose UI testleri |
| `ui-test-manifest` | Debug manifest |

---

## SDK Yapılandırması

```kotlin
compileSdk = 35
targetSdk  = 35
minSdk     = 31   // Android 12+
```

---

## Proje Yapısı

```
profitnessapp/
├── .agent/
│   ├── AGENTS.md
│   ├── memory/
│   │   ├── activeContext.md
│   │   ├── productContext.md
│   │   ├── progress.md
│   │   ├── projectbrief.md
│   │   ├── systemPatterns.md
│   │   └── techContext.md
│   └── workflows/
│       └── update_memory_bank.md
├── app/
│   ├── build.gradle.kts
│   └── src/main/java/com/avonix/profitness/
│       ├── MainActivity.kt              ← Single Activity, theme state owner
│       ├── ProfitnessApplication.kt     ← @HiltAndroidApp
│       ├── core/
│       │   └── theme/
│       │       ├── AppTheme.kt          ← AppThemeState, AccentPreset, surface/text extensions
│       │       ├── Color.kt             ← Named color constants + legacy aliases
│       │       ├── PageAccentBloom.kt   ← Radial glow overlay composable
│       │       ├── Theme.kt             ← ProfitnessTheme composable
│       │       ├── ThemeRepository.kt   ← DataStore persistence (NEW)
│       │       └── Type.kt              ← Typography (Space Grotesk)
│       ├── di/
│       │   └── AppModule.kt             ← Hilt @Module (placeholder)
│       ├── presentation/
│       │   ├── aicoach/
│       │   │   └── AICoachScreen.kt
│       │   ├── components/
│       │   │   ├── CinematicExerciseCard.kt  ← LOCKED
│       │   │   ├── GlassPanel.kt             ← ForgeCard + glassCard modifier
│       │   │   └── PremiumButton.kt          ← ForgeButton + GhostButton
│       │   ├── dashboard/
│       │   │   └── DashboardScreen.kt        ← AppBackground + AppNavBar
│       │   ├── navigation/
│       │   │   └── AppNavigation.kt
│       │   ├── news/
│       │   │   └── NewsScreen.kt
│       │   ├── profile/
│       │   │   └── ProfileScreen.kt
│       │   ├── program/
│       │   │   └── ProgramBuilderScreen.kt
│       │   └── workout/
│       │       └── WorkoutScreen.kt
│       └── data/                         ← (Planlandı — Room, Retrofit)
├── gradle/
│   └── libs.versions.toml
└── build.gradle.kts
```

---

## Emülatör / Test Cihazı

- Geliştirme: API 34/35, "Medium Phone" AVD
- Minimum desteklenen: API 31 (Android 12)

---

## Bilinen Kısıtlar

- `minSdk = 31` — Android 11 altı cihazlar desteklenmiyor
- Room migration stratejisi tanımlı değil — v1 için `fallbackToDestructiveMigration` kullanılacak
- Release signing config eksik — Play Store öncesi gerekli
- AI yanıtları canned (gerçek LLM yok) — Retrofit/Gemini entegrasyonu planlandı

---

## Ortam Değişkenleri / Secrets

- API anahtarları `local.properties`'e eklenmeli, VCS'e commit edilmemeli
- Örnek: `AI_API_KEY=sk-...`
- `BuildConfig` alanı olarak expose edilmeli (`buildConfigField` in `build.gradle.kts`)
