# Active Context — Profitness

_Son güncelleme: 2026-03-15_

## Şu Anki Odak

FAZ 1 tamamlandı. Sıradaki: **FAZ 2 — Program Sistemi** (Hazır şablonlar + Manuel builder + Düzenleme)

---

## Son Tamamlanan Değişiklikler

### FAZ 1 — Veritabanı Şeması + Auth + Exercise Seed (2026-03-15)

**Supabase migrations (MCP ile uygulandı):**
- `001_initial_schema` — 19 tablo, tüm tablolarda RLS aktif, `handle_new_user()` trigger (profiles + user_stats + user_credits otomatik oluşturma)
- `002_seed_exercises` — 91 hareket, 8 kategori (Bacak, Core, Sırt, Göğüs, Omuz, Biceps, Kardiyo, Triceps)

**Android kod değişiklikleri:**
- `gradle/libs.versions.toml` — `supabase = "2.6.1"`, `ktor = "2.3.12"`, `kotlin-serialization` plugin eklendi
- `app/build.gradle.kts` — kotlin-serialization plugin, buildConfig=true, SUPABASE_URL + SUPABASE_ANON_KEY buildConfigField, Supabase BOM + modüller bağımlılıkları
- `core/BaseViewModel.kt` — `<S>` → `<S : Any, E : Any>`, `SharedFlow<E>` + `sendEvent()` eklendi
- `presentation/workout/WorkoutViewModel.kt` — `, Nothing` type param eklendi
- `data/auth/AuthRepository.kt` — yeni interface (signIn, signUp, signOut, isLoggedIn, sendPasswordReset)
- `data/auth/AuthRepositoryImpl.kt` — Supabase gotrue implementasyonu, `withContext(Dispatchers.IO)` + `runCatching`
- `di/AppModule.kt` — `object` → `abstract class`, `@Binds` AuthRepository, `companion object` SupabaseClient provider
- `presentation/auth/AuthViewModel.kt` — `AuthRepository` inject, `AuthEvent.NavigateToDashboard`, Türkçe hata mesajları
- `.agent/AGENTS.md` — light mode kuralları kaldırıldı, Supabase/Gemini kuralları, 6 memory protokolü eklendi

**Önemli kararlar:**
- Session persistence şimdilik in-memory (Supabase gotrue default). Process kill → tekrar login. DataStore-based SessionStorage FAZ 2+ eklenmeli.
- `AppModule` artık `abstract class` — `@Binds` zorunluluğu nedeniyle

**Dikkat (FAZ 2'yi etkiler):**
- `exercises` tablosu doldu (91 hareket), FAZ 2 ExercisePicker bunları kullanabilir
- `programs`, `program_days`, `program_exercises` tabloları boş, FAZ 2'de doldurulacak
- `is_active` flag'i programs tablosunda var → anasayfa bağlantısı FAZ 3'te

---

## Aktif Kararlar & Öğrenmeler

- **Interface-first repository:** ViewModel her zaman interface'e inject edilir. `AuthRepositoryImpl` direkt kullanılmaz.
- **Extension mapper zorunlu:** `fun Dto.toDomain()` — ayrı mapper class yasak.
- **Supabase IO dispatcher:** Tüm Supabase çağrıları `withContext(Dispatchers.IO)` + `runCatching`.
- **BaseViewModel<S,E>:** Navigation/toast için `sendEvent()`, state'e flag ekleme.
- **Dark-only tema:** Light mode FAZ 8B'de kaldırılacak. Yeni kod yazarken light bileşeni ekleme.
- **CinematicExerciseCard LOCKED:** Yapısal değişiklik yasak, sadece veri bağlantısı yapılacak.

---

## Bir Sonraki Adımlar

- [ ] **FAZ 2A:** Hazır program şablonları — gerçekçi 16 program, DB'ye seed, seçince `programs` tablosuna kopyala
- [ ] **FAZ 2B:** Manuel program oluşturma — 7 gün max, otomatik başlık algoritması, `exercises` tablosundan hareket seçme
- [ ] **FAZ 2C:** Mevcut programı düzenleme — CRUD, gün ekle/sil/sırala
- [ ] Session persistence — DataStore-based `SessionStorage` entegrasyonu
