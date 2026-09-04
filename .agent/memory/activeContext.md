# Active Context — Profitness

_Son güncelleme: 2026-09-04_

## Şu Anki Odak

AI program içe aktarma yapısı genişletildi ve canlı backend'e alındı. Yapıştırılan ayrıntılı programlar artık sabit set/tekrar, başlangıç kilosu, bölüm, hareket/gün notu, süperset-dev set-devre bağlantısı, tur ve tur arası dinlenmeyi saklıyor.

---

## Son Tamamlanan Değişiklikler

### CosmiBit Marka ve Paket Geçişi (2026-09-04)

- Şirket adı tüm kullanıcı metinleri, e-posta şablonları ve dokümantasyonda `CosmiBit` olarak güncellendi.
- Android application ID, namespace ve Kotlin paket ağacı `com.cosmibit.profitness` olarak taşındı.
- Baseline profile, ProGuard ve Android App Links kayıtları yeni paket kimliğiyle eşitlendi.

### Ana Ekran Yoğunluk Azaltma (2026-09-04)

- Workout başlığındaki karşılama, gün öneki ve kcal/süre pill'leri kaldırıldı; progress halkası küçültüldü.
- Streak banner tek satırlık kompakt pill'e, gün seçici yalnızca gün etiketlerini gösteren ince bir şeride dönüştürüldü.
- Kartlarda kategori rozeti ve tamamlanan set cümlesi kaldırıldı; hareket/hedef adları seçili dilde tekil gösteriliyor.
- Gün, hareket ve grup ayrıntıları kart bilgi panelinde korunuyor.

### AI Program Ayrıntılarını Koruma (2026-09-04)

- Program AI ve Oracle → Program JSON şemaları `weightKg`, `section`, `notes`, `groupId`, `groupType`, `groupLabel`, `groupRounds`, `groupRestSeconds` alanlarıyla genişletildi.
- Aralıklar için ayrı veri alanı açılmadı; AI tek sabit sayı seçiyor.
- Room 13→14 migration ve Supabase `20260903210108_preserve_ai_program_structure` migration eklendi.
- Canlı Supabase migration uygulandı; birleşmiş `ai-generate` v7 ve `gemini-generate` v8 deploy edildi.
- Uzun metinlerde katalog 72 odaklı harekete indirildi, tek parça 12.000 karakter sınırı kaldırıldı, toplam limit 30.000 ve program çıktısı 8.192 token oldu.
- Program düzenleyici/antrenman ekranında bölüm, grup/tur/dinlenme, not ve planlı kilo görünür; grup içindeki ara hareketlerde egzersiz arası dinlenme 0, grubun sonunda grup dinlenmesi kullanılır.
- `:app:compileDebugKotlin` ve `:app:testDebugUnitTest` başarılı (`NO-SOURCE`).

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
