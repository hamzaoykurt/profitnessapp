# Active Context — Profitness

_Son güncelleme: 2026-09-08_

## Şu Anki Odak

### Gün bazlı hareket tamamlama izolasyonu — 2026-09-08

- Haftalık set tamamlamaları artık yalnız `exerciseId` ile değil, `programDayId + exerciseId` anahtarıyla gruplanıyor; aynı hareket farklı program günlerinde bağımsız tutuluyor.
- Workout ekranındaki set tikleri, otomatik hareket tamamlama, aktivite tamamlama ve optimistik geri alma yolları aynı gün-bazlı anahtarı kullanıyor.
- Aynı hareketin pazartesi ve cuma kayıtlarının birleşmediğini doğrulayan regresyon testi eklendi.
- `:app:compileDebugKotlin` kaynak derlemesi başarılı. Tam `testDebugUnitTest`, OneDrive dosya modu nedeniyle `processDebugJavaRes` / `transformDebugClassesWithAsm` aşamasında testlere ulaşmadan duruyor.

### Orbit kullanıcı arayüzü devre dışı — 2026-09-07

- Profil Ayarları içindeki `Entegrasyonlar → Orbit Personal OS` satırı ve bağlantı bottom sheet'i ürün kapsamından çıkarıldı.
- `ProfileState`, `ProfileEvent` ve `ProfileViewModel` Orbit durum/bağlantı akışından ayrıldı; profil açılışında veya `ON_RESUME` sırasında Orbit durum isteği yapılmıyor.
- Workout repository içindeki completion/uncomplete sonrası Orbit bildirimleri kaldırıldı. Entegrasyon kaynakları, Supabase taslağı ve dokümantasyon ileride sağlık/wearable verisi senaryosu için repoda pasif olarak tutuluyor.
- Geçici sahte build yapılandırmasıyla `:app:compileDebugKotlin` başarılı; mevcut deprecated ikon uyarıları bu değişiklikle ilgili değil.

### Minimal typography + page headers (2026-09-06)

- Uygulama genelindeki Space Grotesk/Inter yönü bırakılıp daha karakterli geometrik Sora ailesine geçildi; display/headline ağırlıkları ve negatif tracking azaltıldı, body stilleri Regular ağırlıkta tutuldu.
- Keşfet ve Programlar ekranlarındaki içeriği tekrar eden alt açıklamalar kaldırıldı. `Program Studio` adı `Programlar` olarak sadeleştirildi; Profil başlığı global kompakt headline ölçeğine bağlandı.

### Bottom navigation gesture + motion finalization (2026-09-06)

- Drag sırasında seçili etiket artık görünmez bırakılarak alan ayırmıyor; bütün sekmeler gerçekten kompakt moda geçiyor.
- Dock ekran genişliğine göre 292–312dp aralığında ve 64dp sabit gövde yüksekliğinde. Etiket açıkken görünür içerik boşlukları optik dağıtılıyor; sürüklemede etiketsiz ikonlar sabit 32dp aralıkla merkeze toplanıyor.
- Drag başında geniş pill eski seçimden koparak kompakt moda daralıyor; x konumu parmağı kontrollü gecikmeyle izliyor, eşiklerde ekran seçimi güncelleniyor ve bırakınca yeni etikete genişliyor.
- Reddedilen split/merge ve ghost katmanları tamamen kaldırıldı. Seçili menü yüzeyi tek parça kalır; x, genişlik, ikon ve etiket aynı 220ms eğriyle birlikte hareket eder. Drag sırasında sayfa değiştirilmez, yalnız bırakılan son sekme bir kez açılır.
- Workout ekranında status bar yüksekliğine çizilen opak `bg0` kapağı kaldırıldı; ortak ambient arka plan kamera/status bar alanına kesintisiz uzanıyor.
- Workout seri alanı kamera/status bar inset'inin altına taşındı ve nokta + metin görünümünden çıkarıldı. Accent yansımalı hero kapsül, ayrı ateş plakası, motivasyon alt metni ve gün sayacıyla ana bağlılık bileşeni olarak yeniden tasarlandı.
- Egzersiz `Geri al` işleminde completion ve set Room akışlarının birbirini geçici olarak ezmesi önlendi; UI anında incomplete kalırken yerel kayıtlar önce temizlenir, uzak rollback arka planda tamamlanır.
- Indicator hedefleri gerçek tema fontuyla önceden ölçülen etiket genişliğinden hesaplanıyor; x 190ms, genişlik 175ms tek-shot geçiş kullanıyor ve layout sırasında yeniden başlamıyor.
- Uç sekmelerde indicator kenara clamp edildiğinde ikon+etiket içeriği de aynı merkez farkıyla kaydırılıyor. 9dp iç padding, 5dp gap ve 22dp glyph ile komşu ikonlara güvenli mesafe korunuyor.
- Eski etiket hareket başında 20ms'de söner, yeni etiket pill hedefe yaklaşırken açılır; yazının seçili yüzeyden kopuk veya kırpılmış göründüğü ara kare kaldırıldı.
- Emulator QA'da beş yerleşimin tamamı ardışık doğrulandı (`build/nav-optical-0.png` … `build/nav-optical-4.png`); son daraltılmış dock ve 64dp gövde profil/program ekranlarında tekrar kontrol edildi.
- Profil XP alanı inset premium modüle dönüştürüldü; seviye, kalan XP badge'i ve animasyonlu gradient ilerleme aynı hiyerarşide toplandı.
- Program düzenleme alt eylemleri 64dp `AI ile düzenle` ikincil kontrolü ve geniş gradient `Kaydet` ana kontrolü olarak yenilendi.
- `:app:assembleDebug`, `:app:testDebugUnitTest` ve `:app:lintDebug` başarılı.
- Android Studio'da görülen tek seferlik `:app:processDebugResources` / `compile_and_runtime_not_namespaced...` hatası kaynak kod veya XML hatası değil: görev hem normal hem `--rerun-tasks` ile başarılı, ardından tam `assembleDebug` başarılı. IDE/Gradle ara çıktı kilidi veya stale build sonucu olarak değerlendirildi.

### Optional Orbit Personal OS Fitness Sync (2026-09-06)

- Profil Ayarları içine ayrı ve isteğe bağlı `Entegrasyonlar → Orbit Personal OS` akışı eklendi; onboarding ve workout UI değiştirilmedi.
- `data/integration/orbit` altında interface-first repository, güvenli Edge Function istemcisi, sunucu durumundan türeyen entitlement kapıları ve Fitness yazımlarını engellemeyen coalesced arka plan coordinator bulunuyor.
- Fitness kaynak veri olmaya devam ediyor. Orbit özeti aktif program ile gerçek `workout_logs` + `exercise_logs` kayıtlarından timezone-aware olarak yeniden hesaplanıyor; bağımsız sayaç tutulmuyor.
- Supabase migration owner-only SELECT RLS, istemciye kapalı delivery/webhook/link-attempt tabloları, service-role-only summary RPC ve tek kullanımlık imzalı link state ekliyor. Orbit entitlement yalnız HMAC doğrulanmış Orbit callback'inden yazılıyor.
- Edge Functions: `orbit-fitness-integration` (status/connect/manage flags/disconnect/sync) ve JWT doğrulaması kapalı fakat HMAC + süre + one-time state ile korunan `orbit-fitness-callback`.
- Android `compileDebugKotlin`, `assembleDebug`, 15 Orbit policy testi + mevcut 3 Oracle testi başarılı. Deno/Supabase CLI yerel makinede kurulu olmadığı için Edge Function typecheck ve migration staging uygulaması henüz yapılmadı.
- Son APK veri silmeden `emulator-5554` üzerine kuruldu. Profilde Entegrasyonlar satırı ve bağlantı sheet'i mobilde görsel/erişilebilirlik ağacıyla doğrulandı; kanıtlar `build/orbit-row.png` ve `build/orbit-sheet-final.png`.
- Kurulum, secret sözleşmesi ve 15 maddelik doğrulama matrisi `ORBIT_INTEGRATION.md` içinde.

Kullanıcının üç ayrıntılı brifi yeniden tam olarak okundu. Yenileme sürüyor; aşağıdaki geçmiş denemeler güncel tasarım kararı veya tamamlanma kanıtı değildir. Güncel kapsam ve eksikler `UI_REDESIGN_AUDIT.md` içinde takip edilir. Hedef sakin utility / hacimli elevated / sınırlı signature yüzey ayrımıdır; her yere glass veya düz siyah kart değil.

### Premium gradient/material convergence (2026-09-06)

- `v17.9`, `v17.8` ve eski `279f108` premium navigasyon yaklaşımı karşılaştırıldı. Kör geri dönüş yapılmadı; eski yüzen kapsül ve seçili sekmenin etiketle genişlemesi güncel gesture/veri sözleşmesiyle birleştirildi.
- Bottom navigation artık ekran genişliğini kaplayan etiketli blok değildir. Kompakt yüzen gövde, koyu tonal gradient, üst yansıma, tek shadow ve accent-tinted genişleyen seçili pill kullanır. Pasif sekmeler yalnız ikon olarak kalır.
- Sayfa glow kaynakları viewport dışına taşındı. Accent ve soğuk mineral ışık geniş falloff olarak çizilir; ekranda dekoratif daire ve animasyonlu glow bulunmaz.
- Elevated yüzeyler tek cached paint pass içinde graphite/opal face, dışarıda başlayan soft key light, alt derinlik ve ince üst yansıma kullanır. `ForgeCard` glow parametresi yalnız dışarıdan gelen düşük alfa accent yansıması üretir.
- Workout dairesel progress ringini korur. Gün seçici düz accent blok yerine inset rail üzerinde koyu/opal, kenardan accent yansımalı seçili kontrol kullanır. Exercise kartları 20dp sculpted face, kontrollü elevation ve fiziksel index plaque aldı.
- Emulator QA: dark workout, Program Studio, Oracle ve profile; light profile incelendi. Kanıtlar `build/profitness-premium-v3.png`, `build/profitness-program-v3.png`, `build/profitness-ai-v3.png`, `build/profitness-profile-v3.png`, `build/profitness-light-v3.png`.
- `:app:compileDebugKotlin`, `:app:assembleDebug`, `:app:testDebugUnitTest` ve `:app:lintDebug` başarılı.

### Contrast, icon and action affordance revision (2026-09-06)

- Kullanıcının altı ekran görüntüsündeki geri bildirime göre dark palette gri ağırlığından blue-black/near-black katmanlara çekildi; `text2` okunurluğu artırıldı. Elevated kartlara sürekli tonal yüzeyin yanında tek ince gradient rim eklendi, böylece kart/canvas ayrımı yalnız renk farkına bırakılmıyor.
- Global Typography daha tok ve sıkı hale getirildi: display/headline ağırlıkları ExtraBold, title ağırlıkları Bold, body ağırlıkları Medium; negatif headline tracking ve daha sıkı line-height kullanılıyor.
- Ana bottom navigation ve görünür profil metrikleri `Icons.Filled` ailesine geçirildi. Profilde çıplak küçük ikon yerine accent-tinted 40dp icon plate kullanılıyor; hero stat ikonları büyütülüp kendi anlam rengiyle gösteriliyor.
- Oracle top bar history/new/settings kontrolleri 42dp sculpted yüzey oldu; enerji göstergesi büyütüldü. Composer inset yüzey, solda Oracle icon plate ve sağda her durumda görünür 44dp filled send control aldı.
- Saved program kartlarında `MANUEL` badge kaldırıldı. `Aktif et` gerçek filled CTA oldu; aktif status pill'e, edit/share/more ise 40dp sculpted icon button'lara dönüştü.
- Exercise kartındaki küçük info aksiyonu 46dp filled play control oldu; expanded alanda ayrıca full-width `Hareketi gör` butonu yer alıyor.
- `:app:compileDebugKotlin`, `:app:assembleDebug`, `:app:testDebugUnitTest` ve `:app:lintDebug` başarılı. Yeni görsel QA, Windows WHPX `0x80070005` hatası nedeniyle bu turda tamamlanamadı; önceki screenshot'lar yeni revizyonun kanıtı sayılmaz.

### Soft Glass Hardware yönü (2026-09-05)

> Düzeltme: Kullanıcı `9daf7d1` küreli cam navigasyonun aradığı premium sürüm olmadığını ve sahte cam hissi verdiğini belirtti. Bu yön artık kabul edilmiş tasarım değildir. Navigasyon `2bd8570` çizgisindeki kayan seçili yüzeye uyarlandı; zemin ve içerik yüzeyleri şeffaflık taklidi yerine opak hacimli malzeme kullanır. Workout dairesel ilerleme göstergesi geri getirildi.

- Kullanıcının yeni referansı, arka planda ayrı dairesel objeler yerine viewport dışından yayılan geniş ışık; yüzeylerde yarı geçirgen soft-glass ve kontrollere basıldığında içe çöken yumuşak 3D malzeme istediğini netleştirdi.
- Git geçmişindeki `9daf7d1` (`iOS-quality tab transitions, 3D glass navbar`) incelendi. Alt navigasyonun frosted gövde, üst cam yansıması, alt iç gölge, accent bleed, 3D orb ve açılan seçili etiket yaklaşımı güncel veri/gesture sözleşmesine uyarlandı.
- Ana bilgi hiyerarşisi eskiye döndürülmedi: workout başlığı kompakt editoryal protokol düzeni, doğrusal ilerleme ve numaralı egzersiz sıraları kullanıyor; Program Studio ve profil daha sıkı dikey ritme sahip.
- Ortak elevated yüzeyler düşük alfa metal/cam katmanına, butonlar diffuse key-light + pressed inner-depth renderer'ına geçirildi. Arka plan ışıkları görünür olacak kadar güçlendirildi ve merkezleri ekran dışına taşındı; dairesel obje görünümü oluşmuyor.
- Son `compileDebugKotlin`, `assembleDebug`, `testDebugUnitTest` ve `lintDebug` başarılı. APK emulator-5554 üzerinde doğrulandı; son kanıt `build/profitness-glass-nav2.png`.

---

## Son Tamamlanan Değişiklikler

### Matte Editorial Yön Değişimi (2026-09-04)

- Görsel QA sonrasında glass ana tasarım dili olmaktan çıkarıldı; tekrarlanan ince rim, üst çizgi ve diagonal specular katmanları kartlardan kaldırıldı.
- Workout/program/profile kartları borderless near-black tonal yüzeylere geçti; accent geniş turuncu yıkama yerine küçük ikon ve seçili aksiyonlarda kalıyor.
- Alt navigasyon transparan/glass pill yerine opak near-black dock ve solid seçili hücre oldu; mevcut sürükleme ve indicator animasyonu korundu.
- AI chat kök arka planı ile app bar birleştirildi; üstteki ani siyah kesim kaldırıldı.
- Oracle ve kullanıcı balonları outline/sol accent çizgisi olmadan ayrı matte yüzeylere geçti; composer ve hızlı chip'ler solid hale getirildi.
- AI enerji bilgi kartı borderless nötr yüzeye geçirildi; global sayfa accent bloom'u azaltıldı.
- `:app:compileDebugKotlin` başarılı.

### Quiet Glass Premium UI Düzeltmesi (2026-09-04)

- İkinci görsel QA turunda dark yüzeyler gri yerine near-black/cool-blue katmanlara alındı; turuncu/accent geniş dolgulardan çekilip ikon, kenar ve lokal glow'a sınırlandı.
- 3D hacim dış gölgeye bırakılmadı: kart/buton gövdesine üst-sol specular gradient, alt derinlik ve alt-sağ accent pool eklendi. Bu katmanlar tek cached draw pass içinde çalışıyor.
- Light yüzey ayrımı güçlendirildi: canvas metalik mavi-griye, kartlar yarı saydam beyaz → cool-slate gradient'e geçirildi.
- Dışa taşan kaideli/neumorphic CTA sistemi kaldırıldı; primary, secondary, icon ve geri kontrolleri içe çöken sakin yüzeylere geçirildi.
- Ortak kart yüzeylerindeki üst üste 2–3 shadow, yoğun beyaz shimmer ve sürekli glow katmanları tek sınırlı shadow + tek `drawWithCache` geçişine indirildi.
- Light tema lila/porselen parlamadan temiz soğuk-nötr gri/beyaz sisteme taşındı; metin ve pasif ikon kontrastı artırıldı.
- Ana antrenman kartları image-free özet kart oldu; büyük `HAREKETİ GÖR` aksiyonu gerçek görseli `ContentScale.Fit` ile detay ekranında açıyor.
- Kart listesindeki her öğe için çalışan sonsuz ambient glow animasyonu ve görünür listedeki Coil görsel istekleri kaldırıldı; expand/progress/timer gibi işlevsel animasyonlar korundu.
- Chat balonları ve mesaj composer'ı aynı sakin cam/inset malzeme diline geçirildi; alt nav camı ve gölgeleri hafifletildi.
- `:app:compileDebugKotlin` başarılı.

### Katmanlı Premium Malzeme Geçişi (2026-09-04)

- `PremiumButton` ve `GhostButton` tek parça gradient olmaktan çıkarıldı: ayrı alt kaide, hareket eden ön yüz, üst highlight ve fiziksel basma mesafesi kullanılıyor.
- Açık tema sıcak olmayan porselen + soft-lila mineral atmosfere taşındı; kartlarda geniş ambient gölge, beyaz iç rim ve hafif mor derinlik var.
- Karanlık temanın varsayılanı Graphite oldu. OLED seçeneğinde dahi içerik katmanları tamamen siyaha gömülmüyor; kartlar üstten aydınlanan grafit tonlarla ayrılıyor.
- `premiumSolidSurface`, `ForgeCard` ve `floatingGlassSurface` çift gölge, üst ışık, alt yoğunluk ve daha belirgin yüzey hiyerarşisi aldı.
- Program Studio'nun dekoratif grid'i kaldırıldı. AI aksiyonu tek primary, Manuel nötr tutuldu; buton ikonları iç plakalara alındı.
- Hazır program kartlarındaki rozet/stat kalabalığı tek kompakt meta satırına indirildi; ikon kutuları inset yüzey oldu.
- `CinematicExerciseCard` için ayrı alt gövde/kaide, güçlendirilmiş medya kabuğu, kontrollü accent glow ve tek-accent stat plakası eklendi; mevcut kart animasyonları ve veri sözleşmesi korundu.
- Açık ve koyu Program Studio ile auth/profile emülatör QA'sı yapıldı; `:app:assembleDebug` başarılı.

### Uygulama Geneli Premium Tasarım Sistemi (2026-09-04)

- Auth, onboarding, şifre sıfırlama, antrenman, program, AI Coach, keşfet, arkadaşlar, liderlik, challenge, mağaza, kilo takibi ve profil akışları aynı premium görsel dilde güncellendi.
- `premiumSolidSurface`, `floatingGlassSurface` ve `insetControlSurface` rolleri ayrıştırıldı; glass'ın her kartta kullanılması engellendi.
- `PremiumButton`, `GhostButton`, `PremiumIconButton` ve geri butonu 0.98 ölçekli hızlı press tepkisine geçirildi; bevel/glow kaldırıldı.
- Aksiyon hiyerarşisi tek accent olarak sabitlendi: görünümde yalnız tercih edilen primary renkli/tintli, secondary aksiyonlar nötr; hata/uyarı/başarı/rütbe renkleri semantik istisna.
- Program Studio'daki lime/cyan CTA çifti kaldırıldı; AI primary, Manuel neutral yapıldı. Spor/kategori filtreleri, hazır program kartları ve program detayları tek tema accent'ine geçirildi.
- Profil performans kartlarındaki dekoratif gökkuşağı kaldırılarak tek tema accent'i kullanıldı.
- Açık tema bağımsız Mineral Light paletine (`#F2F4F6`, opal beyaz yüzeyler, slate metin) taşındı; mekanik renk tersleme kaldırıldı.
- Alt navigasyon yalnız ikon, 64px sınıfında ve yüksek opaklıklı hafif glass olarak sadeleştirildi; sayfa geneli bloom azaltıldı.
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

### Performance Luxury UI Refactor (2026-09-04)

- Yeni ortak token sistemi `PerformanceDesignSystem.kt`: 8pt spacing, 14/16/20/26dp radius, 120/240ms motion ve opak tonal `performanceSurface`.
- Ana palet brife sabitlendi: dark `#090A0B → #171A1E`, light `#F2F4F6 / #FFFFFF / #E9EDF1`, marka aksanı `#F36A21`.
- Normal içerik kartlarında gradient/glow/rim/border kaldırıldı; glass yalnız bottom navigation ve overlay rollerinde yüksek opaklıkla kullanılabilir.
- `PremiumButton` primary accent dolgu, `GhostButton` nötr yüzey ve 0.98/120ms press tepkisi kullanır; bevel, üst çizgi ve glow yoktur.
- Workout kartları image-free kompakt satırdır; tam kart detail/expand için tıklanabilir, teknik bilgisi küçük secondary aksiyondur. Görsel yalnız how-to detail içinde yüklenir.
- Program Studio tek `Yeni program` CTA + oluşturma sheet'i kullanır. Aktif programlar önde, silme overflow menüsündedir; filtreler tek satır + filter sheet'tedir.
- Oracle ilk girişte Performance Intelligence özeti ve danışman aksiyonları gösterir; kredi maliyeti feed'de büyük kart olarak tekrarlanmaz.
- Discover başlık/tabları doğal case kullanır. Challenge filtreleri tek `Filtreler` kontrolünde birleşir.
- Profil hero'da XP/rütbe/seri aynı yüzeyde üç kolon; kart içinde kart ve dekoratif glow azaltıldı.
- Bottom navigation 64px sınıfında, yüksek opaklıklı çok hafif glass; seçili durum küçük accent tint'tir.
- `:app:compileDebugKotlin` başarılı. Görsel cihaz QA'sı bağlı emulator/device olmadığı için bekliyor.

- **Interface-first repository:** ViewModel her zaman interface'e inject edilir. `AuthRepositoryImpl` direkt kullanılmaz.
- **Extension mapper zorunlu:** `fun Dto.toDomain()` — ayrı mapper class yasak.
- **Supabase IO dispatcher:** Tüm Supabase çağrıları `withContext(Dispatchers.IO)` + `runCatching`.
- **BaseViewModel<S,E>:** Navigation/toast için `sendEvent()`, state'e flag ekleme.
- **Dual-mode tema (2026-09-04):** Dark tema nötr near-black tonal katman, light tema mineral gri canvas + opal beyaz surface kullanır; mekanik invert yapılmaz.
- **Fiziksel kontrol derinliği (2026-09-04):** 3D/bevel yerine hızlı 0.98 scale ve tonal state değişimi kullanılır.
- **Malzeme rolleri (2026-09-04):** İçerik kartı solid/elevated, yüzen chrome kontrollü glass, girişler inset olmalıdır. Sayfa geneline yoğun neon/glass yayılmaz.
- **Aksiyon rengi (2026-09-04):** Primary marka accent'ini, secondary nötr yüzeyi kullanır. Komşu CTA'lara kategori bazlı ayrı renk verilmez; kırmızı/amber/yeşil yalnız gerçek semantik anlam taşır.
- **CinematicExerciseCard:** Veri ve etkileşim sözleşmesini koru; listede görsel/glow yok, teknik medya yalnız detail yüzeyinde.

---

## UI doğrulama notu — 2026-09-05

- Son ek referanslar birebir şablon değil: içe dönük kontrol derinliği ve sınırlı geniş ışık geçişi hedefleniyor. Genel listelerde blur/animasyonlu glow yok.
- Tema sheet'inde çift tutamak ve yüksek CTA gölgesi kaldırıldı; gün/tema seçimleri inset ortak yüzeye taşındı. Alt yansıma yatay sönümlenir.
- Oracle başlangıcındaki veriyle beslenmeyen readiness/metric yer tutucuları kaldırıldı. İlk giriş otomatik kaydırması ve tekrar eden hızlı öneriler düzeltildi. Yüklenen primary butonun spinner kontrastı düzeltildi.
- Program AI girişindeki 24dp gölge ve yükleme butonundaki glow kaldırıldı; düzenleme dialog çerçevesi ve CTA gradient rim temizlendi.
- Son assembleDebug ve lintDebug başarılı; lint raporunda hata yok, 98 uyarı/10 hint var. testDebugUnitTest NO-SOURCE: mevcut birim test yok.
- Sanal cihaz yeniden açıldı, son APK veriler korunarak kuruldu. Açık workout/profil ve koyu profil/Oracle gözle kontrol edildi. Bu kontrolden sonra text2 kontrastı artırıldı, floating chrome opaklaştırıldı ve çerçevesi kaldırıldı; Oracle ilk welcome balonu overview varken gizlendi. Son assembleDebug/lintDebug tekrar başarılı.
- Debug emülatör profil kaydırma örneği: 102 frame, 5 deadline miss (%4.90), p50 25ms/p90 34ms. Bu küçük örnek gerçek cihaz 60fps doğrulaması veya eski sürüme göre iyileşme kanıtı değildir. Diğer alt akışların kapsamlı cihaz QA'sı ve release performans ölçümü açık kalır.

## Alt navigasyon rötuşu — 2026-09-06

- Alt navigasyon adları `Antrenman` ve `Programlar` olarak ayrıştırıldı; tekrarlayan `Programım / Program` dili kaldırıldı.
- Ana navigasyon ve görünür profil/ayar aksiyonları daha tok `Filled` ikon ailesine geçirildi.
- Seçili kapsül tek parça sürekli çizilen yuvarlatılmış accent yüzeye taşındı; sağdaki dik kesilme ve beyaz gradient durağı kaldırıldı.
- Kapsül genişleme/renk/elevation animasyonları 300–360ms `FastOutSlowInEasing` ile yumuşatıldı; sayfa geçiş mesafesi azaltılıp süre dengelendi.
- Dark `Antrenman` ve `Programlar` ekranları emulator-5554 üzerinde doğrulandı. `compileDebugKotlin`, `assembleDebug`, `testDebugUnitTest` ve `lintDebug` başarılı.
- Takip eden gesture revizyonunda seçili arka plan sekme hücresinden ayrılarak tek bir hareketli kapsül katmanına taşındı. Basılı sürüklemede kapsül pointer merkezini sürekli izler; hover ikonu eşiklerde değişir, ekran/etiket seçimi yalnız bırakıldığında tek seferde commit edilir. Böylece ara karedeki dik kesilme, yarım label ve ardışık layout sıçramaları kaldırıldı.
- Presentation katmanındaki karışık `Filled` kullanımlar temizlendi; tüm Material ikonları yuvarlatılmış `Icons.Rounded` ailesinde birleştirildi. Bottom nav profil glyph'i dolu `AccountCircle` yerine kompakt `Person` oldu.
- Sürükleme başlangıç, ara ve yerleşmiş kareleri emulator üzerinde doğrulandı (`build/nav-drag-start.png`, `build/nav-drag-mid-final2.png`, `build/nav-drag-settled-final.png`). Son assemble, unit test ve lint başarılı.

## Bir Sonraki Adımlar

- [ ] **FAZ 2A:** Hazır program şablonları — gerçekçi 16 program, DB'ye seed, seçince `programs` tablosuna kopyala
- [ ] **FAZ 2B:** Manuel program oluşturma — 7 gün max, otomatik başlık algoritması, `exercises` tablosundan hareket seçme
- [ ] **FAZ 2C:** Mevcut programı düzenleme — CRUD, gün ekle/sil/sırala
- [ ] Session persistence — DataStore-based `SessionStorage` entegrasyonu

---

## Veri dışa/içe aktarma — 2026-09-07

- Profil ayarlarına genel kullanıcı diliyle `Verileri Dışa Aktar` ve `Verileri İçe Aktar` eklendi; Orbit/Mentor markalaması kullanılmıyor.
- Android belge seçici üzerinden sürümlü `profitness-backup` JSON dosyası oluşturuluyor ve okunuyor.
- Yedek kapsamı: temel profil alanları, egzersiz tanımları, program/gün/hareket yapısı, antrenman ve egzersiz logları, set performansı ve kilo geçmişi.
- İçe aktarma silme yapmaz; aynı hesaba ait yedeği mevcut Room kayıtlarıyla ID bazında birleştirir. İçe alınan geçmiş kayıtları sonraki senkron için bekleyen olarak işaretlenir.
- XP, kredi, rütbe/sıralama, başarımlar ve sosyal/challenge verileri kullanıcı tarafından değiştirilebilir yedeğe alınmaz.
- Dosya biçimi hesap kimliği ve sürüm doğrulaması yapar; farklı hesaba ait veya 10 MB üstü dosyalar reddedilir.
- `:app:compileDebugKotlin` başarılı.

### Oturum fallback düzeltmesi (2026-09-07)

- Veri dışa/içe aktarma, GoTrue başlangıçta `currentUserOrNull()` geçici null döndürebildiği için “Oturum bulunamadı” hatasına düşebiliyordu.
- `ProfileViewModel.currentUserId()` artık önce `currentSessionOrNull()?.user?.id`, sonra `currentUserOrNull()?.id` kullanır; dışa/içe aktarma bu yardımcıdan kullanıcı kimliği alır.
- `:app:compileDebugKotlin` başarılı.

## Sistem teması — 2026-09-07

- Tema tercihi artık `ThemeMode.DARK`, `LIGHT` veya `SYSTEM` olarak saklanıyor.
- Eski kurulumlarda kayıtlı `is_dark` değeri otomatik olarak DARK/LIGHT tercihine dönüştürülür; mevcut kullanıcı tercihi değişmez.
- `SYSTEM` seçiliyken `MainActivity`, Android sistem gece modunu Compose üzerinden izler ve efektif `isDark` değerini anlık uygular.
- Profil görünüm sheet'i ve onboarding tema adımı üç seçenekli `Koyu / Açık / Sistem` kontrolünü kullanır.
- `AppThemeStateSaver` yeni alanı geriye uyumlu biçimde saklar.
- `:app:compileDebugKotlin` başarılı.
