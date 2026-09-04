# Active Context — Profitness

_Son güncelleme: 2026-09-04_

## Şu Anki Odak

Uygulamanın tüm ekranları ortak premium malzeme sistemiyle yenilendi. Koyu ve açık tema birbirinden bağımsız tasarlandı; glass yalnızca yüzen chrome'da, solid yükseltilmiş yüzeyler içerikte, inset yüzeyler girişlerde kullanılıyor.

---

## Son Tamamlanan Değişiklikler

### Uygulama Geneli Premium Tasarım Sistemi (2026-09-04)

- Auth, onboarding, şifre sıfırlama, antrenman, program, AI Coach, keşfet, arkadaşlar, liderlik, challenge, mağaza, kilo takibi ve profil akışları aynı premium görsel dilde güncellendi.
- `premiumSolidSurface`, `floatingGlassSurface` ve `insetControlSurface` rolleri ayrıştırıldı; glass'ın her kartta kullanılması engellendi.
- `PremiumButton`, `GhostButton`, `PremiumIconButton` ve geri butonuna basma ölçeği, gölge çökmesi, bevel ve kontrollü accent ışığı eklendi.
- Aksiyon hiyerarşisi tek accent olarak sabitlendi: görünümde yalnız tercih edilen primary renkli/tintli, secondary aksiyonlar nötr; hata/uyarı/başarı/rütbe renkleri semantik istisna.
- Program Studio'daki lime/cyan CTA çifti kaldırıldı; AI primary, Manuel neutral yapıldı. Spor/kategori filtreleri, hazır program kartları ve program detayları tek tema accent'ine geçirildi.
- Profil performans kartlarındaki dekoratif gökkuşağı kaldırılarak tek tema accent'i kullanıldı.
- Açık tema bağımsız Mineral Light paletine (`#F1F3F6`, beyaz yüzeyler, slate metin/gölge) taşındı; mekanik renk tersleme kaldırıldı.
- Alt navigasyon yalnız ikon olacak şekilde sadeleştirildi; sayfa geneli bloom azaltıldı ve kart/aksiyon derinliğine odaklanıldı.
- Dark/light emülatör QA'sı auth, dashboard, program, AI, discover, store ve profile akışlarında yapıldı.

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
- **Dual-mode tema (2026-09-04):** Koyu Neon Forge korunur; açık tema eski sıcak paleti kullanmaz. Mineral Light açık paleti (`#F1F3F6`, beyaz yüzeyler, slate metin/gölge) ve tema tokenları zorunludur.
- **Malzeme rolleri (2026-09-04):** İçerik kartı solid/elevated, yüzen chrome kontrollü glass, girişler inset olmalıdır. Sayfa geneline yoğun neon/glass yayılmaz.
- **Aksiyon rengi (2026-09-04):** Primary marka accent'ini, secondary nötr yüzeyi kullanır. Komşu CTA'lara kategori bazlı ayrı renk verilmez; kırmızı/amber/yeşil yalnız gerçek semantik anlam taşır.
- **CinematicExerciseCard:** Veri ve etkileşim sözleşmesini koru; kullanıcı yönlendirmesiyle görsel malzeme, glow ve press derinliği geliştirilebilir.

---

## Bir Sonraki Adımlar

- [ ] **FAZ 2A:** Hazır program şablonları — gerçekçi 16 program, DB'ye seed, seçince `programs` tablosuna kopyala
- [ ] **FAZ 2B:** Manuel program oluşturma — 7 gün max, otomatik başlık algoritması, `exercises` tablosundan hareket seçme
- [ ] **FAZ 2C:** Mevcut programı düzenleme — CRUD, gün ekle/sil/sırala
- [ ] Session persistence — DataStore-based `SessionStorage` entegrasyonu
