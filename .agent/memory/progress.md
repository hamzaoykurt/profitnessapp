# Progress — Profitness

_Son güncelleme: 2026-09-07_

## Genel Durum: Premium çift tema yenilemesi uygulanıyor; kapsamlı cihaz QA'sı devam ediyor

### Orbit görünür akışını kaldırma — 2026-09-07

- [x] Profildeki Orbit Personal OS ayar satırı ve entegrasyon paneli kaldırıldı
- [x] Profile ViewModel'deki Orbit state, event, repository ve bağlantı eylemleri kaldırıldı
- [x] Profil açılış/resume Orbit durum sorguları kapatıldı
- [x] Antrenman tamamlama/geri alma sonrasındaki pasif Orbit ağ tetikleri kaldırıldı
- [x] Entegrasyon altyapısı gelecekteki sağlık/wearable kapsamı için kaynakta pasif tutuldu
- [x] `:app:compileDebugKotlin` başarılı

### Minimal tipografi ve başlık sadeleştirmesi — 2026-09-06

- [x] Global font ailesi jenerik Inter yerine geometrik Sora'ya geçirildi; ağır başlıklar, agresif tracking ve gereksiz body kalınlığı azaltıldı
- [x] Keşfet/Programlar tekrar eden açıklamaları kaldırıldı; `Program Studio` sade `Programlar` başlığına dönüştürüldü
- [x] Profil başlığı ortak kompakt headline stilini kullanıyor

### Alt navigasyon boşluk ve motion son düzeltmesi — 2026-09-06

- [x] Drag sırasında görünmez etiketin ayırdığı genişlik tamamen kaldırıldı
- [x] Dock 292–312dp responsive genişliğe ve 64dp sabit gövdeye geçirildi; sürüklemedeki etiketsiz ikonlar 32dp aralıkla merkeze toplandı
- [x] Drag indicator eski geniş seçimden 52dp kompakt moda yumuşakça daralırken parmağı kontrollü gecikmeyle izliyor
- [x] Kullanıcı tarafından reddedilen split/merge tamamen kaldırıldı; tek parça indicator konum ve genişliği aynı 220ms eğriyle senkron değiştiriyor
- [x] Workout status bar üzerindeki opak kapak kaldırıldı; kamera alanındaki sert siyah kesinti giderildi
- [x] `Serini başlat` alanı status bar inset'inin altındaki premium hero kapsüle dönüştürüldü; ateş plakası, motivasyon metni ve gün sayacı eklendi
- [x] Drag sırasında ağır ekranlar eşiklerde yüklenmiyor; son hedef yalnız parmak bırakıldığında bir kez açılıyor
- [x] Egzersiz `Geri al` işlemindeki uzak silme beklemesi UI'dan ayrıldı; eski Room Flow emisyonlarının optimistik incomplete durumunu geri çevirmesi engellendi
- [x] Tek dokunma indicator animasyonunun layout değişirken sürekli yeniden başlaması kaldırıldı; x 190ms ve genişlik 175ms tek-shot hale getirildi
- [x] Pill hedef genişliği gerçek tema fontuyla önceden ölçülüyor; uç slotlarda içerik pill clamp merkeziyle eşit kaydırılıyor
- [x] Beş seçili durum emulator üzerinde tek tek doğrulandı (`build/nav-optical-0.png` … `build/nav-optical-4.png`); son dock yüksekliği profil/program ekranlarında tekrar kontrol edildi
- [x] Profil XP alanı animasyonlu gradient progress, seviye plakası ve kalan XP badge'i olan ayrı inset modüle yükseltildi
- [x] Program düzenleme altındaki AI/Kaydet alanı güçlü birincil-ikincil eylem hiyerarşisiyle yeniden tasarlandı
- [x] `assembleDebug`, `testDebugUnitTest` ve `lintDebug` başarılı
- [x] Android Studio `processDebugResources` geçici ara çıktı hatası önbelleksiz yeniden üretimle doğrulandı; tam debug build tekrar başarılı

### Optional Orbit Personal OS entegrasyonu — 2026-09-06

- [x] Auth, workout/Room/Supabase sync, program modeli, Profil Ayarları ve mevcut Edge Function kalıpları incelendi
- [x] Fitness'i bağımsız bırakan interface-first Orbit repository ve durum/entitlement kapıları eklendi
- [x] Ayarlar → Entegrasyonlar içinde Connect / Manage / Disconnect / Enable Sync UX'i eklendi
- [x] Completion ve uncomplete sonrasında Fitness'i bloklamayan coalesced arka plan sync eklendi
- [x] Gerçek completion kayıtlarından timezone-aware haftalık/today/last-completed özet RPC'si eklendi
- [x] Owner-only RLS, server-only writes, service-role-only RPC, HMAC callback ve tek kullanımlık state/replay koruması eklendi
- [x] Idempotent delivery ve callback event kayıtları eklendi; counter drift modeli kullanılmadı
- [x] 15 Orbit policy testi başarılı; toplam 18 unit test geçti
- [x] `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:app:assembleDebug` başarılı
- [x] `:app:lintDebug` başarılı; APK emulator-5554'e kuruldu ve mobil Settings → Integrations akışı görsel olarak doğrulandı
- [ ] Orbit staging endpoint/secrets sağlandığında migration deploy, Edge Function typecheck ve uçtan uca callback/delivery testi

### Soft Glass Hardware + tarihsel navbar restorasyonu — 2026-09-05

> Son kullanıcı düzeltmesi: 3D orb/frosted navbar reddedildi. `2bd8570` kayan indicator yaklaşımına geçildi; dock opak sculpted yüzey, seçili state kayan iç yüzeydir. İçerik kartları ve floating yüzeyler de fake glass yerine opak gradient hacme geçirildi. Workout circular progress geri döndü ve gerçek hareket/tamamlanma özeti eklendi.

- Kullanıcı referansına göre geniş off-canvas ambient glow, yarı geçirgen metal/cam kart yüzeyi ve basışta içe çöken yumuşak 3D kontrol malzemesi eklendi.
- Git geçmişindeki `9daf7d1` premium navbar uygulaması incelenip güncel `DashboardTab`, çeviri ve drag-to-select yapısına port edildi: frosted gövde, cam yansıması, iç gölge, accent bleed, 3D orb ve seçili etiket geri geldi.
- Workout ekranı dev progress halkasından editoryal başlık + doğrusal progress'e; egzersiz kartları jenerik ikon yerine sıralı numara plakalarına geçti.
- Program Studio yaratma/kart yüzeyleri ve profil hero dikey yoğunluğu sıkılaştırıldı; ortak radius ölçeği küçültüldü.
- `compileDebugKotlin`, debug APK, birim test ve lint başarılı; emülatör görsel QA tamamlandı.

### Son doğrulama — 2026-09-05

- Kullanıcının tüm brifleri tekrar okundu; eski hafıza kayıtları tasarım otoritesi olmaktan çıkarıldı. `UI_REDESIGN_AUDIT.md` tüm ekran/alt akış kabul listesidir.
- Elevated kartlar opak, kesintisiz yönlü ışık ve tek gölge kullanır. Inset rol küçük kontrollere ayrıldı; profil veri kartlarına yanlış taşınan inset düzeltildi.
- Profil kimliği yatay ve kompakt; XP/statlar ana yüzeyde. Egzersiz set/tekrar ayrı veri satırı; bölüm başlıkları doğal case. Alt nav bağımsız rim/saydamlığını kaybetti.
- Ortak geri düğmesi eski özel bevel renderer yerine inset kullanır. Profil düzenleme/veri analizi/başarım başlıkları ve mağaza paywall CTA'sı ortak dilde temizlendi.
- Oracle aktif program gün/hareket/tamamlanma sayıları mevcut Workout state'inden gösterilir; ek ağ/AI çağrısı yok. Üç yeni birim test başarılı. Son assembleDebug/lintDebug başarılı; APK görünür emülatöre kuruldu.
- Eski NO-SOURCE notları tarihsel: artık OracleTrainingSummaryTest içinde 3 test var. Tam alt akış ve gerçek cihaz performans QA'sı hâlâ açık.

- Ortak inset kontroller, opak navigation/composer, okunabilir ikincil metinler ve borderless program AI dialog düzeltmeleri uygulandı.
- Oracle overview gerçek olmayan metrik içermez; ilk girişte welcome/öneri tekrarı ve otomatik kaydırma giderildi.
- Son assembleDebug ve lintDebug başarılı. Lint: 0 hata, 98 uyarı, 10 hint. Unit test görevi NO-SOURCE.
- Açık workout/profil ile koyu profil/Oracle emülatörde incelendi. Son APK hazır ve emülatöre kuruldu.
- Performans tamamlandı sayılmaz: debug emülatör kısa profil kaydırmasında 102 frame/5 deadline miss, p50 25ms. Release/gerçek cihaz karşılaştırması ve kalan alt ekran QA'sı gerekli.

---

## ✅ Tamamlananlar

### Matte Editorial Düzeltme

- [x] Glass ana tasarım dili terk edildi; kartlardaki ince üst çizgi ve tam çerçeve kaldırıldı
- [x] Workout/program/profile yüzeyleri borderless tonal matte sisteme geçirildi
- [x] Alt nav opak solid dock + solid seçili hücre olarak yeniden tasarlandı
- [x] AI chat üst bar ve içerik arka planı kesintisiz hale getirildi
- [x] Chat balonları, composer, hızlı chip ve enerji kartı borderless matte sisteme geçirildi
- [x] Global accent atmosferi azaltıldı; geniş turuncu yıkama temizlendi
- [x] `:app:compileDebugKotlin` başarılı

### Quiet Glass UI/Performans Revizyonu

- [x] Dark palet gri kutu görünümünden near-black/cool-blue cam katmanlarına geçirildi
- [x] Kart ve buton gövdesine üst-sol specular gradient + alt-sağ lokal glow eklendi
- [x] Primary CTA'larda tam accent dolgu kaldırıldı; accent yalnız metin/ikon/rim/glow olarak kullanılıyor
- [x] Light canvas ve kartlar arasında soğuk metalik tonal ayrım güçlendirildi
- [x] Tüm ortak kart yüzeyleri tek gölge + tek cached draw geçişine indirildi
- [x] Primary/secondary/icon/geri butonlar dışa yükselen kaideden içe basılan yüzeye geçirildi
- [x] Light tema nötr gri-beyaz palet ve daha güçlü metin/ikon kontrastı aldı
- [x] Workout gün seçici kabarık 3D yerine seçili inset kontrol oldu
- [x] Chat balonları ve composer sakin glass/inset sisteme geçirildi
- [x] Alt navigasyon cam yüzeyi ve gölge maliyeti düşürüldü, pasif ikon görünürlüğü artırıldı
- [x] Egzersiz liste kartlarından fotoğraf ve sürekli ambient glow kaldırıldı
- [x] Egzersiz görseli kırpılmadan yalnız how-to detayında yükleniyor
- [x] `:app:compileDebugKotlin` başarılı

### Uygulama Geneli Premium UI — Dark + Light

- [x] Tüm ana ekran ve alt akışlar ortak premium malzeme rollerine geçirildi
- [x] İçerik kartları solid/elevated, yüzen chrome kontrollü glass, arama/form alanları inset yapıldı
- [x] Primary, secondary, icon ve geri kontrollerine tactile 3D basma/elevation tepkisi eklendi
- [x] Keyfi çok renkli CTA'lar kaldırıldı; primary tek tema accent'i, secondary nötr yüzey kullanıyor
- [x] Program Studio filtre/kart/aksiyon gökkuşağı tek-accent sistemine geçirildi
- [x] Profil performans kartlarının dekoratif çok renkli chrome'u tek accent'e indirildi
- [x] Açık tema mekanik tersleme yerine bağımsız Mineral Light paletiyle yeniden tasarlandı
- [x] Alt navigasyon tüm form faktörlerinde yalnız ikon olacak şekilde sadeleştirildi
- [x] Workout metin yoğunluğu azaltıldı; ayrıntılar bilgi panelinde korundu
- [x] Sayfa geneli neon bloom azaltıldı; kart kenarı ve eylem vurguları kontrollü hale getirildi
- [x] Auth, dashboard, program, AI, discover, store ve profile dark/light emülatör QA'sı tamamlandı
- [x] Primary/secondary CTA'lar ayrı kaide + hareketli ön yüz kullanan gerçek 3D kontrollere geçirildi
- [x] Açık tema porselen/soft-lila atmosfere, koyu tema varsayılan Graphite katman sistemine geçirildi
- [x] Program Studio grid'i ve kart rozet kalabalığı kaldırıldı; ikonlar inset plakalara taşındı
- [x] Ana sayfa egzersiz kartlarına ayrı alt kabuk, çift gölge ve tek-accent medya chrome'u eklendi

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

### 2026-09-07 — Genel veri dışa/içe aktarma

- [x] Orbit'teki veri taşıma yaklaşımı incelendi; ProFitness için markadan bağımsız yedekleme akışına uyarlandı
- [x] Profil ekranına `Verileri Dışa Aktar` / `Verileri İçe Aktar` seçenekleri eklendi
- [x] Profil, program, egzersiz, antrenman, set ve kilo verilerini kapsayan sürümlü JSON formatı eklendi
- [x] İçe aktarma silmeden birleştirme, aynı hesap kontrolü ve dosya boyutu sınırı eklendi
- [x] XP/kredi/sıralama/sosyal veriler güvenli biçimde kapsam dışında bırakıldı
- [x] `:app:compileDebugKotlin` başarılı
- [x] Oturum yüklenirken `currentUser` null olduğunda session.user fallback'i eklendi; “Oturum bulunamadı” geri bildirimi düzeltildi

### 2026-09-07 — Sistem temasını takip et

- [x] Kalıcı `DARK / LIGHT / SYSTEM` tema modu eklendi
- [x] Sistem modu Android gece/gündüz ayarını anlık takip edecek şekilde ana tema katmanına bağlandı
- [x] Profil ayarları ve onboarding tema seçimi üç seçeneğe çıkarıldı
- [x] Eski `is_dark` tercihlerinin geriye uyumlu geçişi korundu
- [x] `AppThemeStateSaver` yeni tema modunu koruyacak şekilde güncellendi
- [x] `:app:compileDebugKotlin` başarılı
- [ ] `:app:testDebugUnitTest` kaynaklar ve test sınıfları derlenmesine rağmen iki mevcut testte Gradle/JUnit `ClassNotFoundException` ile çalıştırıcı seviyesinde duruyor; daemon sıfırlama, `--rerun-tasks` ve configuration cache kapatma sonucu değiştirmedi

### 2026-09-06 — Premium gradient + sculpted material uygulaması

- [x] `v17.9`, `v17.8` ve `279f108` navigasyon/material kararları kod seviyesinde karşılaştırıldı
- [x] Tam geniş alt bar yerine kompakt floating capsule ve yalnız seçilince genişleyen label/pill geri getirildi
- [x] Seçili navigasyon yüzeyi kontrollü accent gradient, üst ışık ve tek glow shadow aldı; fake glass/orb kullanılmadı
- [x] Background accent/mineral glow merkezleri viewport dışına taşındı; görünür dekoratif glow dairesi kaldırıldı
- [x] Shared dark graphite ve light opal kart yüzleri continuous gradient, key light ve shallow lower depth ile yeniden işlendi
- [x] Exercise kartları sculpted 20dp yüzey, fiziksel index plaque ve sınırlı dış-kaynak accent reflection aldı
- [x] Workout circular progress korundu; day selector inset rail + restrained selected material oldu
- [x] Dark workout, Program Studio, Oracle, profile ve light profile emülatörde görsel olarak incelendi
- [x] `:app:compileDebugKotlin` ve `:app:assembleDebug` başarılı
- [x] Final `testDebugUnitTest` + `lintDebug`

### 2026-09-06 — Kontrast, ikon ve aksiyon düzeltmesi

- [x] Dark palette gray yerine blue-black/near-black hiyerarşiye taşındı
- [x] Elevated kart ayrımı için tek ince gradient rim ve daha belirgin tonal contrast eklendi
- [x] Typography headline/title/body ağırlık ve tracking sistemi toklaştırıldı
- [x] Ana navigasyon ile profil stat/metric ikonları Filled aileye taşındı
- [x] Oracle header controls ve composer fiziksel, büyük touch-target yüzeylere geçirildi
- [x] Saved program `MANUEL` rozeti kaldırıldı; Activate/Edit/Share/More affordance'ları güçlendirildi
- [x] Exercise detail girişi 46dp play control + expanded full-width CTA oldu
- [x] Compile, assemble, unit test ve lint başarılı
- [ ] Yeni revizyon emülatör visual QA — Windows WHPX erişim hatası (`0x80070005`) nedeniyle bekliyor

### 2026-09-06 — Alt navigasyon ikon, isim ve motion düzeltmesi

- [x] `Programım / Program` çakışması `Antrenman / Programlar` olarak düzeltildi
- [x] Navigasyon glyph'leri minimal ve daha tok Filled ikonlarla değiştirildi
- [x] Seçili kapsülün sağ dik kesilmesi kaldırıldı; accent yüzey uçlara doğru doğal sönümleniyor
- [x] Seçim kapsülü ve ekran geçişleri daha uzun, düşük mesafeli ve smooth hale getirildi
- [x] Profil/ayar geri ve satır aksiyon ikonları Filled aileye geçirildi
- [x] Dark Antrenman ve Programlar ekranları emulator-5554 üzerinde görsel QA'dan geçti
- [x] `compileDebugKotlin`, `assembleDebug`, `testDebugUnitTest`, `lintDebug` başarılı
- [x] Seçili yüzey tek bağımsız hareketli kapsüle çevrildi; hücre içi background kaynaklı dik kırpılma kaldırıldı
- [x] Basılı sürükleme pointer-follow davranışı geri getirildi; hedef ekran bırakıldığında tek seferde seçiliyor
- [x] Sürükleme sırasında yarım etiket oluşmaması için label geçici gizlenip settle sonunda yeniden açılıyor
- [x] Presentation katmanındaki tüm Material ikon referansları tek `Icons.Rounded` ailesinde standardize edildi
- [x] Profil alt menü ikonu kompakt `Person` glyph'ine değiştirildi
- [x] Emulator gesture ara-kare ve settle QA, assemble, unit test ve lint başarılı

### 2026-09-04 — Performance Luxury v4 UI/UX refactor

- [x] Ortak 8pt spacing, radius, motion ve opak tonal surface tokenları eklendi
- [x] Dark/light paletler ve burnt-orange signature accent brife uyarlandı
- [x] Shared kart/buton yüzeylerinden dekoratif gradient, glow, bevel ve gereksiz border kaldırıldı
- [x] Bottom navigation 64px sınıfına indirildi; seçili state küçültüldü
- [x] Workout kartları %25–30 daha kompakt bilgi satırı + küçük Teknik aksiyonu olarak düzenlendi
- [x] Program Studio tek Yeni Program CTA, create sheet, filter sheet ve overflow program aksiyonlarına geçti
- [x] Oracle Performance Intelligence başlangıç merkezi ve sakin mesaj balonları kazandı
- [x] Discover doğal-case tab hiyerarşisine, Challenge tek Filtreler akışına geçti
- [x] Profil hero statları ortak üç kolon yüzeye alındı; dekoratif glow azaltıldı
- [x] `:app:compileDebugKotlin` başarılı
- [x] Güncel APK `emulator-5554` üzerinde dark workout/profile ve light workout/profile/Oracle/Program Studio için görsel regresyon kontrolünden geçti

### 2026-09-04 — Premium yüzey sistemi + Mineral Light

- [x] Görünüm ayarlarına kalıcı Koyu/Açık seçici eklendi
- [x] Eski sıcak açık palet kaldırıldı; serin nötr Mineral Light paleti tanımlandı
- [x] `ForgeCard` ve eski `glassCard` çağrıları opak, kontursuz katmanlı yüzeye yönlendirildi; yalnız signature yüzeylerde kontrollü glow bırakıldı
- [x] Egzersiz kartlarına içe çöken basma tepkisi ve dinamik elevation
- [x] Alt navigasyon ve tablet rail yalnız ikon olacak şekilde sadeleştirildi
- [x] Workout, AI Coach, timer, program picker ve toast yüzeylerindeki eski sabit koyu tokenlar tema-aware hale getirildi
- [x] Discover, challenge, profil düzenleme, onboarding ve sosyal aksiyonlarda accent üstü metin/ikon kontrastı dinamik hale getirildi
- [x] Discover açık mod arka planındaki sabit siyah gradient kaldırıldı
- [x] Son kullanıcı brifi doğrultusunda cam/rim ve çift gölge yaklaşımı geri çekildi; kart yüzeyine tam yayılan sabit tonal ışık ve hafif renk yansıması kullanıldı
- [x] Gün seçiciye bevel highlight, iç alt gölge ve elevation collapse eklendi
- [x] AI Chat kullanıcı/Oracle balonları ile mesaj giriş yüzeyi dark/light için yeniden işlendi
- [x] `:app:compileDebugKotlin`, `:app:assembleDebug` ve `:app:testDebugUnitTest` doğrulamaları yapıldı

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
