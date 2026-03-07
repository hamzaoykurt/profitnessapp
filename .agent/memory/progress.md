# Progress — Profitness

_Son güncelleme: 2026-03-06_

## Genel Durum: 🟡 Aktif Geliştirme

---

## ✅ Tamamlananlar

### Altyapı & Kurulum

- [x] Android projesi oluşturuldu (`com.avonix.profitness`)
- [x] Kotlin + Jetpack Compose + Material 3 entegrasyonu
- [x] Hilt (KSP) dependency injection kurulumu
- [x] Gradle Kotlin DSL yapılandırması (`compileSdk=35`, `targetSdk=35`, `minSdk=31`)
- [x] Compose Navigation entegrasyonu
- [x] Coroutines + Flow altyapısı
- [x] Coil görüntü yükleme entegrasyonu
- [x] Space Grotesk font (Google Fonts)
- [x] DataStore Preferences entegrasyonu (`datastore-preferences:1.1.1`)

### UI & Tema — v1: Custom Foundation (kaldırıldı)

- [x] Custom `ProfitnessTheme` oluşturuldu
- [x] **Solid Forge System** (opak, yüksek kontrastlı — v3'e geçildi, kaldırıldı)
- [x] **CinematicExerciseCard** — yapısal restaurasyon, korunuyor (locked)

### UI & Tema — v2: Matte Obsidian (kaldırıldı)

- [x] Obsidian renk sistemi (#080808 zemin, amber rim light)
- [x] ObsidianCard (1dp rim light + ambient occlusion)
- [x] Integrated dock navigation → floating pill'e geçildi

### UI & Tema — v3: Neon Forge Dark ✅

- [x] **Color.kt v3.0** — `Lime #CBFF4D`, `Amber #FFA726`, `Surface0–3`, renkli kart kategorileri, legacy aliases
- [x] **WorkoutScreen** — dairesel progress halkası (CircularProgressRing), 7-günlük DaySelector, stat pill'leri, kategori renkleri
- [x] **DashboardScreen** — `AppBackground` (accent bloom + warm glow), `AppNavBar` (floating pill + indicator dot)
- [x] **ProfileScreen** — ProfileHero (avatar+XP bar+level badge), BigStatCard ×4, WeeklyActivityChart, Settings rows, avatar/isim düzenleme, streak takvimi
- [x] **CinematicExerciseCard** — kategori badge pill (Lime/Cyan/Purple), Lime complete butonu (yapı korundu)
- [x] **GlassPanel** — ForgeCard (tema-aware bg, accent rim light, derin gölge), `glassCard` Modifier, tüm legacy aliases
- [x] **AICoachScreen** — 15+ keyword-mapped canned responses, hızlı öneri chip'leri, Lime gradient mesaj balonları, Oracle avatarı, typing indicator
- [x] **ProgramBuilderScreen** — AI Builder (loading→Oracle taslağı), şablon detay dialog, Manuel Mimar, snackbar geri bildirim, kaydedilen programlar listesi
- [x] **NewsScreen** — 12 demo makale, kategori filtresi, detay görünümü, bookmark toggle, okuma süresi, HTML renderer, çeviri desteği

### UI & Tema — v4: Dual-Mode (Dark + Light) ✅

- [x] **Light Theme Redesign — Warm & Earthy**
  - `AppTheme.kt`: Light palette yenilendi (`bg0=#FAF8F5` → `bg3=#DDD2C2`, warm text hierarchy)
  - `AccentPreset`: `lightColor` + `onLightColor` eklendi (her preset için doygun/okunabilir varyant)
  - `effectiveAccentColor` / `effectiveOnAccentColor` extension property'leri
  - `Theme.kt`: `lightColorScheme` artık `effectiveAccentColor` kullanıyor
  - `GlassPanel.kt`: Tamamen tema-aware (`theme.bg2`, warm shadow, tema-adaptive shimmer)
  - `PageAccentBloom.kt`: Light modda alpha değerleri %50 azaltıldı
  - `DashboardScreen.kt` (`AppBackground` + `AppNavBar`): Tema-aware shadow/bloom
  - `NewsScreen.kt`: 18+ `Snow` hardcoded → `theme.text0/text1/text2` (image overlay'ler korundu)
  - `ProgramBuilderScreen.kt`: Tüm `Surface*`, `Snow`, `Mist`, `TextMuted` → `theme.*`
  - `AICoachScreen.kt`: Quick chips + SANCTUARY label tema-aware
  - `WorkoutScreen.kt`: RestDayView kart ve metinler tema-aware
  - `PremiumButton.kt`: `GhostButton` tamamen tema-aware

- [x] **DataStore Theme Persistence**
  - `ThemeRepository.kt` (yeni): `@Singleton` DataStore-backed tema kayıt/yükleme
  - `MainActivity.kt`: Inject + `LaunchedEffect` yükleme + `lifecycleScope` async kayıt
  - `libs.versions.toml` + `build.gradle.kts`: `datastore-preferences:1.1.1` bağımlılığı

### Dokümantasyon

- [x] `AGENTS.md` — güncel proje talimatları (dual-mode tema kuralları dahil)
- [x] `.agent/memory/projectbrief.md`
- [x] `.agent/memory/productContext.md`
- [x] `.agent/memory/systemPatterns.md`
- [x] `.agent/memory/techContext.md`
- [x] `.agent/memory/activeContext.md`
- [x] `.agent/memory/progress.md` (bu dosya)

---

## ⬜ Henüz Başlanmadı

### Veri Katmanı

- [ ] Room DB kurulumu — `WorkoutEntity`, `ExerciseEntity`, `SetEntity`, DAO'lar, `AppDatabase`
- [ ] `WorkoutRepository` implementation (CRUD)
- [ ] Use case katmanı (`GetTodayWorkoutUseCase`, `SaveSetUseCase`, vb.)
- [ ] `WorkoutViewModel` — Room'dan gerçek veri

### Ağ

- [ ] Retrofit API servisleri (Gemini / AI endpoint)
- [ ] `AIRepository` + `AIViewModel`
- [ ] API anahtar yönetimi (`local.properties` → `BuildConfig`)

### Test

- [ ] Unit testler (ViewModel, Repository, Use Cases)
- [ ] UI testler (ComposeTestRule)

### Release

- [ ] Signing config (`keystore.properties`)
- [ ] ProGuard kuralları doğrulaması
- [ ] Play Store asset'leri (icon, feature graphic, screenshots)

---

## Bilinen Sorunlar / Açık Konular

| Sorun | Durum | Not |
|-------|-------|-----|
| Room migration stratejisi tanımsız | ⚠️ Açık | v1 için `fallbackToDestructiveMigration` yeterli |
| Release signing eksik | ⚠️ Açık | Release öncesi gerekli |
| AI yanıtları canned (gerçek LLM yok) | ⚠️ Açık | Retrofit/Gemini entegrasyonu planlandı |
| WorkoutScreen verisi hardcoded | ⚠️ Açık | Room entegrasyonu sonrası gerçek veri |
| CinematicExerciseCard yapısı locked | ℹ️ Bilinçli | Kullanıcı onaylı, değiştirilmez |

---

## Bağımlılık Versiyonları

Bkz. `app/build.gradle.kts` ve `gradle/libs.versions.toml`.

Güncel önemli versiyonlar:
- AGP: `8.11.2`
- Kotlin: `2.0.0`
- Compose BOM: `2024.12.01`
- Hilt: `2.51.1`
- DataStore: `1.1.1`
- Coil: `2.6.0`
