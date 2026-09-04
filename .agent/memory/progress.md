# Progress — Profitness

_Son güncelleme: 2026-09-04_

## Genel Durum: 🟡 Aktif Geliştirme — AI program içe aktarma ayrıntıları tamamlandı

---

## ✅ Tamamlananlar

### CosmiBit Marka Kimliği

- [x] Görünen şirket adı ve e-posta şablonları `CosmiBit` olarak güncellendi
- [x] Android namespace/application ID ve Kotlin paketleri `com.cosmibit.profitness` olarak taşındı
- [x] ProGuard, baseline profile, App Links ve proje dokümantasyonu yeni kimlikle eşitlendi

### Ana Ekran Premium Sadeleştirme

- [x] Karşılama, gün öneki, günlük stat pill'leri ve kart kategori rozetleri ana yüzeyden kaldırıldı
- [x] Streak ve gün seçici bileşenleri kompaktlaştırıldı
- [x] Çift dilli kart metinleri seçili dilde tekil gösterime geçirildi
- [x] Gün/hareket/grup ayrıntıları bilgi panelinde korundu

### AI Program İçe Aktarma ve Yapısal Alanlar

- [x] Başlangıç kilosu AI çıktısından Supabase/Room/program/antrenman ekranına uçtan uca bağlandı
- [x] Süperset, dev set ve devreler ortak grup kimliğiyle saklanıyor
- [x] Grup turu ve tur arası dinlenme saklanıp gösteriliyor
- [x] Isınma/ana antrenman/core/finisher/aktif toparlanma bölümleri korunuyor
- [x] Gün ve hareket uygulama notları korunuyor
- [x] Uzun program girişlerinde istek ve çıktı limitleri genişletildi; katalog boyutu odaklı hale getirildi
- [x] Canlı Supabase şeması ve AI Edge Functions güncellendi

### Altyapı & Kurulum

- [x] Android projesi oluşturuldu (`com.cosmibit.profitness`)
- [x] Kotlin + Jetpack Compose + Material 3 entegrasyonu
- [x] Hilt (KSP) dependency injection kurulumu
- [x] Gradle Kotlin DSL yapılandırması (`compileSdk=35`, `targetSdk=35`, `minSdk=31`)
- [x] Compose Navigation entegrasyonu
- [x] Coroutines + Flow altyapısı
- [x] Coil görüntü yükleme entegrasyonu
- [x] Space Grotesk font (Google Fonts)
- [x] DataStore Preferences entegrasyonu (`datastore-preferences:1.1.1`)
- [x] Supabase SDK (`supabase-bom:2.6.1`, gotrue-kt, postgrest-kt, storage-kt)
- [x] Ktor Android client (`ktor-client-android:2.3.12`)
- [x] Kotlin Serialization plugin

### UI & Tema — v3: Neon Forge Dark ✅

- [x] **Color.kt v3.0** — `Lime #CBFF4D`, `Amber #FFA726`, `Surface0–3`, renkli kart kategorileri, legacy aliases
- [x] **WorkoutScreen** — dairesel progress halkası (CircularProgressRing), 7-günlük DaySelector, stat pill'leri, kategori renkleri
- [x] **DashboardScreen** — `AppBackground` (accent bloom + warm glow), `AppNavBar` (floating pill + indicator dot)
- [x] **ProfileScreen** — ProfileHero (avatar+XP bar+level badge), BigStatCard ×4, WeeklyActivityChart, Settings rows, avatar/isim düzenleme, streak takvimi
- [x] **CinematicExerciseCard** — fotoğraf scrim korunarak premium glow, iç derinlik ve basma tepkisi
- [x] **GlassPanel** — ForgeCard (tema-aware bg, accent rim light, derin gölge), `glassCard` Modifier, tüm legacy aliases
- [x] **AICoachScreen** — canned responses (FAZ 4'te Gemini ile değiştirilecek), hızlı öneri chip'leri, Lime gradient mesaj balonları
- [x] **ProgramBuilderScreen** — AI Builder, şablon detay dialog, Manuel Mimar, snackbar geri bildirim, kaydedilen programlar listesi
- [x] **NewsScreen** — 12 demo makale, kategori filtresi, detay görünümü, bookmark toggle, HTML renderer

### FAZ 1 — Veritabanı Şeması + Auth + Exercise Seed ✅

- [x] **Supabase migration 001** — 19 tablo (profiles, exercises, programs, program_days, program_exercises, workout_logs, exercise_logs, chat_sessions, chat_messages, achievements, user_achievements, user_stats, user_credits, credit_transactions, shared_programs, program_reactions, group_challenges, group_participants, commitment_contracts), tüm tablolarda RLS
- [x] **PostgreSQL trigger** — `handle_new_user()` — signup'ta profiles + user_stats + user_credits otomatik oluşturma
- [x] **Supabase migration 002** — 91 exercise seed (8 kategori, met_value, TR/EN isim, default set/rep)
- [x] **`BaseViewModel<S, E>`** — typed state + one-time events (SharedFlow)
- [x] **`AuthRepository` interface** — signIn, signUp, signOut, isLoggedIn, sendPasswordReset
- [x] **`AuthRepositoryImpl`** — Supabase gotrue implementasyonu
- [x] **`AppModule`** — abstract class, @Binds AuthRepository, SupabaseClient provider
- [x] **`AuthViewModel`** — AuthRepository inject, AuthEvent.NavigateToDashboard, Türkçe hata mesajları
- [x] **`app/build.gradle.kts`** — BuildConfig, SUPABASE_URL + SUPABASE_ANON_KEY, bağımlılıklar

### Dokümantasyon

- [x] `.agent/AGENTS.md` — güncel (dual-mode tema, Supabase kuralları, 6 memory protokolü)
- [x] `.agent/memory/projectbrief.md` — 10 faz, Supabase backend, success criteria
- [x] `.agent/memory/productContext.md` — Commitment Mode, sosyal özellikler, abonelik
- [x] `.agent/memory/systemPatterns.md` — interface-first repo, extension mapper, BaseViewModel<S,E>
- [x] `.agent/memory/techContext.md` — Supabase + Ktor bağımlılıkları, secrets yönetimi
- [x] `.agent/memory/activeContext.md` — FAZ 1 tamamlandı, FAZ 2 sıradaki
- [x] `.agent/memory/progress.md` (bu dosya)

---

## 🔄 Devam Edenler

Şu an aktif faz yok — FAZ 2 kullanıcı onayı bekleniyor.

---

## ⏳ Bekleyenler

| Faz | İçerik |
|-----|--------|
| FAZ 2 | Program sistemi (hazır şablonlar + manuel builder + düzenleme) |
| FAZ 3 | Workout takibi backend bağlantısı + streak + timer + bug fix |
| FAZ 4 | Gemini AI entegrasyonu (chat + program builder) |
| FAZ 5 | Profil + analitik + başarımlar + rank |
| FAZ 6 | Sosyal özellikler (program paylaşma + grup challenge) |
| FAZ 7 | Abonelik + kredi sistemi + Google Play Billing |
| FAZ 7.5 | Commitment Mode (Disiplin Modu — sanal ceza sistemi) |
| FAZ 8 | Auth redesign + tema iyileştirmeleri + haberler + çeviri |
| FAZ 9 | Optimizasyon (21 bulgu) + güvenlik |

---

## Bilinen Sorunlar

### 2026-09-04 — Premium yüzey sistemi + Polar Glass Light

- [x] Görünüm ayarlarına kalıcı Koyu/Açık seçici eklendi
- [x] Eski sıcak açık palet kaldırıldı; serin nötr Polar Glass paleti tanımlandı
- [x] `ForgeCard` ve `glassCard` için katmanlı yüzey, iç rim, kontrollü glow ve tema-özel gölge
- [x] Egzersiz kartlarına içe çöken basma tepkisi ve dinamik elevation
- [x] Alt navigasyon ve tablet rail yalnız ikon olacak şekilde sadeleştirildi
- [x] Workout, AI Coach, timer, program picker ve toast yüzeylerindeki eski sabit koyu tokenlar tema-aware hale getirildi
- [x] Discover, challenge, profil düzenleme, onboarding ve sosyal aksiyonlarda accent üstü metin/ikon kontrastı dinamik hale getirildi
- [x] Discover açık mod arka planındaki sabit siyah gradient kaldırıldı
- [x] Kart cam/rim katmanları içerik üstüne taşındı; çift gölge ve görünür nefes alan glow eklendi
- [x] Gün seçiciye bevel highlight, iç alt gölge ve elevation collapse eklendi
- [x] AI Chat kullanıcı/Oracle balonları ile mesaj giriş yüzeyi dark/light için yeniden işlendi
- [x] `:app:compileDebugKotlin` doğrulaması yapıldı

| Sorun | Dosya | Öncelik |
|-------|-------|---------|
| Session persistence in-memory | `AuthRepositoryImpl.kt` | Orta — FAZ 2+ DataStore SessionStorage |
| AI yanıtları canned | `AICoachScreen.kt` | Düşük — FAZ 4'te Gemini ile değiştirilecek |
| WorkoutScreen verisi hardcoded | `WorkoutScreen.kt` | Düşük — FAZ 3'te DB bağlanacak |
| CinematicExerciseCard veri/etkileşim sözleşmesi | `CinematicExerciseCard.kt` | Korunur; tema ve premium görsel malzeme geliştirilebilir |
| Release signing eksik | `app/build.gradle.kts` | Düşük — release öncesi |

---

## Bağımlılık Versiyonları

- AGP: `8.11.2`
- Kotlin: `2.0.0`
- Compose BOM: `2024.12.01`
- Hilt: `2.51.1`
- DataStore: `1.1.1`
- Coil: `2.6.0`
- Supabase BOM: `2.6.1`
- Ktor: `2.3.12`
