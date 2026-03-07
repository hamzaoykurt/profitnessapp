# Active Context — Profitness

_Son güncelleme: 2026-03-06_

## Şu Anki Odak

**Light Theme Redesign (Warm & Earthy)** tamamlandı. "Neon Forge" dark temasının yanına, krem/bej tonlarına dayalı, okunabilir ve estetik bir aydınlık tema eklendi. Tema tercihi artık DataStore ile kalıcı olarak saklanıyor.

---

## Son Tamamlanan Değişiklikler

### Light Theme Redesign — Warm & Earthy (2026-03-06)

**Sorun:** Light mode açıldığında renkler tersine çevrilmiş gibi görünüyordu — dark hardcoded renklerin (`Snow`, `Surface*`, vb.) light bg üzerinde kullanılması, neon accent'lerin light temada kontrast sorunu.

**Uygulanan Değişiklikler:**

- **`AppTheme.kt`:**
  - Light palette tamamen yenilendi → Warm & Earthy (`bg0=#FAF8F5`, `bg1=#F2EDE7`, `bg2=#E8E0D5`, `bg3=#DDD2C2`, `stroke=#CEC0AD`, `text0=#1A1410`, `text1=#5C4E3E`, `text2=#9E8A72`)
  - `AccentPreset`'e `lightColor` ve `onLightColor` alanları eklendi (LIME dark=`#CBFF4D` → light=`#5C8A00`, vb.)
  - `effectiveAccentColor` ve `effectiveOnAccentColor` extension property'leri eklendi

- **`Theme.kt`:**
  - `lightColorScheme`'de `effectiveAccentColor` kullanılıyor (doygun, okunabilir)
  - Warm secondary/tertiary renkleri (`#EA580C`, `#D97706`)

- **`GlassPanel.kt`:**
  - `ForgeCard`: `Surface2` hardcoded → `theme.bg2`; shadow light modda warm brown tonu
  - `glassCard`: depth shadow `Color.Black` → tema-aware warm brown; shimmer opaklığı artırıldı

- **`PageAccentBloom.kt`:**
  - Light modda tüm glow alpha değerleri ~50% azaltıldı (neon glow warm bg'de garip görünüyor)

- **`DashboardScreen.kt`:**
  - `AppBackground`: Light modda bloom alpha'ları düşürüldü
  - `AppNavBar`: Shadow `Color.Black` → tema-aware; depth gradient warm brown

- **`NewsScreen.kt`:**
  - Tema bg üzerindeki tüm `Snow` → `theme.text0/text1/text2` (18+ kullanım)
  - `Snackbar`, header "MUSE" title, article card text, reader content, nav buttons
  - Image overlay üzerindeki `Snow` kullanımları intentionally korundu (fotoğraf üzeri = always dark)

- **`ProgramBuilderScreen.kt`:**
  - `Surface1/2/3`, `SurfaceStroke`, `Snow`, `Mist`, `TextMuted`, `TextSecondary` → `theme.*`
  - `ArchitectGrid`, `ProgramCard`, `SavedProgramTile`, `ProgramDetailDialog`, `DialogStat`, `AIBuilderScreen`, `ManualBuilderScreen`, `DetailHeader` — hepsi güncellendi

- **`AICoachScreen.kt`:**
  - Quick chip background `Surface1` → `theme.bg1`; "SANCTUARY" label `Snow` → `theme.text2`

- **`WorkoutScreen.kt`:**
  - `RestDayView`: `Surface2` → `theme.bg2`; `TextPrimary/TextSecondary` → `theme.text0/text1`

- **`PremiumButton.kt`:**
  - `GhostButton`: `Depth2` → `theme.bg2`; `BorderSoft` → `theme.stroke`; `Mist/Fog` → `theme.text1/text2`

### DataStore Theme Persistence (2026-03-06)

**Sorun:** Uygulama tamamen kapanınca tema tercihi sıfırlanıyordu (sadece `rememberSaveable` = process death).

**Uygulanan:**
- **`ThemeRepository.kt`** (yeni dosya): `@Singleton`, `@Inject constructor(@ApplicationContext)`, DataStore Preferences ile `isDark` + `accent.ordinal` kayıt/yükleme
- **`MainActivity.kt`**: `ThemeRepository` inject ediliyor; `LaunchedEffect(persisted)` ile first-launch yükleme; her tema değişiminde `lifecycleScope.launch { themeRepository.saveTheme(newState) }`
- **`libs.versions.toml`**: `datastore = "1.1.1"` versiyonu eklendi
- **`app/build.gradle.kts`**: `androidx.datastore.preferences` bağımlılığı eklendi

---

## Aktif Kararlar & Öğrenmeler

- **`effectiveAccentColor` kullan:** Composable'larda `MaterialTheme.colorScheme.primary` otomatik doğru rengi getiriyor (Theme.kt'de zaten ayarlandı). Ama doğrudan `AccentPreset.color`'a değil `theme.effectiveAccentColor`'a başvur.
- **Image Overlay İstisnası:** Hero card, CinematicExerciseCard gibi fotoğraf overlay'lerinde `Snow` kullanmak mantıklı — arka plan her zaman dark photo scrim'i. Bu kullanımlar korunuyor.
- **DataStore + rememberSaveable birlikte:** Rotation için `rememberSaveable` (anında), process kill için DataStore. `LaunchedEffect` ile ilk yükleme yapılıyor.
- **Light mode'da hardcoded constant kullanma:** `Snow`, `Mist`, `Fog`, `Surface0-3`, `Depth1-3`, `TextPrimary`, `TextSecondary`, `TextMuted`, `ObsidianBase` vb. hepsi dark-only sabitler. Light tema desteği gerektiren her yerde `LocalAppTheme.current.*` kullan.
- **Warm Earthy tone seçimi:** Light mode için soğuk gri yerine sıcak krem/bej (`#FAF8F5` bazlı) seçildi — fitness uygulaması için daha doğal ve inviting hissi.

---

## Bir Sonraki Adımlar

### UI & Tema
- [x] Color System v3 (Neon Forge)
- [x] WorkoutScreen — dairesel progress + gün seçici
- [x] DashboardScreen — yüzen pill navigation
- [x] ProfileScreen — büyük bold stats + haftalık grafik
- [x] CinematicExerciseCard — kategori badge
- [x] GlassPanel — yenilenmiş kart sistemi
- [x] AICoachScreen — Neon Forge tema
- [x] ProgramBuilderScreen — stil tutarlılık
- [x] NewsScreen — içerik zenginleştirme + filtreleme
- [x] **Light Theme Redesign** — Warm & Earthy palette
- [x] **DataStore persistence** — tema tercihi kalıcı

### Veri Katmanı (Sonraki Milestone)
- [ ] Room DB kurulumu — Entity (Workout, Exercise, Set), DAO, Database
- [ ] WorkoutRepository — CRUD operasyonları
- [ ] Use case katmanı (GetWorkouts, SaveWorkout, vb.)

### Ağ
- [ ] Retrofit API servisleri (AI endpoint — Gemini)
- [ ] API anahtar yönetimi (local.properties)

### Test
- [ ] Unit testler (ViewModel, Repository)
- [ ] UI testler (ComposeTestRule)

### Release
- [ ] Signing config
- [ ] ProGuard kuralları doğrulaması
- [ ] Play Store asset'leri (ikon, screenshots)
