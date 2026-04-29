### SECURITY AUDIT: Profitness App Full Security Review

**Risk Assessment:** Critical

#### Executive Summary

Bu audit, staged diff yerine uygulama geneli için yürütüldü; `git diff --cached` boş olduğu için kapsam mevcut Android kodu, build/CI akışı, generated artifact, Supabase schema/policy/function izinleri ve read-only Supabase metadata incelemesidir. Üretim verisini değiştiren test yapılmadı.

Başlangıç riski Critical olarak doğrulandı. Ana nedenler: lokal `.mcp.json` içinde açık metin Supabase access token bulunması, mobil APK içine Gemini API key gömülmesi, Supabase tarafında 54 adet `SECURITY DEFINER` fonksiyonunun `anon` ve `authenticated` rollere executable olması, private challenge şifrelerinin plaintext saklanıp tüm authenticated kullanıcılara direct table read ile açılması, public storage bucket listing ve bucket-only write/delete policy nedeniyle avatar objelerinin cross-user overwrite/delete riskidir.

#### Scope & Evidence

**Static review**

* Android build, manifest, auth, navigation, repository, cache, AI, analytics ve CI dosyaları incelendi.
* `./gradlew.bat :app:lintDebug --no-daemon` çalıştırıldı; lint 1 error, 80 warning, 8 hint ile başarısız oldu.
* `./gradlew.bat :app:dependencies --configuration debugRuntimeClasspath --no-daemon` başarılı çalıştı.
* `./gradlew.bat :app:assembleDebug --no-daemon` başarılı çalıştı; debug APK üretildi.
* Generated `BuildConfig.java` içinde Supabase URL, Supabase anon/publishable key ve Gemini API key gömülü olduğu doğrulandı. Değerler rapora yazılmadı.

**Supabase read-only review**

* 26 `public` tablo ve 8 `storage` tablo RLS enabled durumda.
* 54 `SECURITY DEFINER` fonksiyonu bulundu; tamamı `anon` ve `authenticated` tarafından executable.
* 3 `SECURITY DEFINER` fonksiyonu mutable/missing `search_path` advisor bulgusu verdi.
* `profile-photos` bucket public, file size limit yok, allowed MIME type listesi yok.
* `profile-photos` storage policies bucket-only; object path ownership kontrolü yok.
* Public table grants `anon` ve `authenticated` için çok geniş: direct `SELECT`, `INSERT`, `UPDATE`, `DELETE`, `TRUNCATE`, `REFERENCES`, `TRIGGER` grantleri mevcut.

**Not executed**

* Staging aktif adversarial testler yürütülmedi; doğrulanmış staging URL/test hesapları sağlanmadığı için cross-user mutasyon, OTP abuse, reset replay ve bucket overwrite/delete denemeleri yapılmadı.

---

## Findings

#### F-01: Supabase Access Token Exposed in Local MCP Config

**Severity:** Critical

**Location:** `.mcp.json:9`

**The Exploit:** `.mcp.json` içinde açık metin `sbp_...` formatlı Supabase access token bulundu. Bu dosya gitignore kapsamında olsa bile local workspace, terminal history, screen share, backup, malware veya yanlışlıkla commit riski üzerinden token ele geçirilebilir. Token scope'una bağlı olarak saldırgan Supabase project metadata, schema, function, migration veya secret yönetimi üzerinde yetki kazanabilir.

**The Fix:**

```bash
# Immediate operational remediation
1. Supabase dashboard üzerinden ilgili access token'ı rotate/revoke edin.
2. Token kullanım loglarını ve son erişimleri inceleyin.
3. Token'ı repo içindeki JSON dosyalarında tutmayın; OS secret store veya runtime-only environment variable kullanın.
4. Local secret scan'i pre-commit ve CI seviyesinde zorunlu hale getirin.
```

---

#### F-02: Gemini API Key Embedded in Android APK

**Severity:** Critical

**Location:** `local.properties:10-12`, `app/build.gradle.kts:47-49`, generated `app/build/generated/source/buildConfig/debug/.../BuildConfig.java`

**The Exploit:** `GEMINI_API_KEY`, `BuildConfig` üzerinden APK içine gömülüyor. Android istemcide bulunan her secret decompile/JADX/APKTool ile çıkarılabilir. Ayrıca `GeminiRepositoryImpl.kt` API key'i query parameter olarak gönderiyor; bu key proxy, telemetry, debug log, crash report veya network inspection yüzeylerinde sızabilir. Saldırgan key'i çıkarıp doğrudan Gemini API çağrılarıyla maliyet yaratabilir, rate limit tüketebilir veya uygulama adına abuse üretebilir.

**The Fix:**

```kotlin
// Client should call your backend, not Gemini directly.
interface AiGateway {
    suspend fun generateCoachReply(request: CoachRequest): CoachReply
}
```

* Gemini çağrılarını Supabase Edge Function veya ayrı backend'e taşıyın.
* Backend'de Supabase JWT doğrulayın, kullanıcı bazlı rate limit uygulayın, abuse telemetry ekleyin.
* Mevcut Gemini key'i rotate edin.
* Key'i mümkünse API/provider seviyesinde endpoint, quota ve referrer/app integrity kontrolleriyle kısıtlayın.

---

#### F-03: 54 SECURITY DEFINER RPC/Helper Functions Executable by anon/authenticated

**Severity:** Critical

**Location:** Supabase DB, `public` schema functions

**The Exploit:** Tüm `SECURITY DEFINER` fonksiyonları `anon` ve `authenticated` roller tarafından executable durumda. `SECURITY DEFINER`, fonksiyonu caller değil owner yetkisiyle çalıştırır; bu yüzden her exposed fonksiyon potansiyel privilege escalation yüzeyidir. Özellikle:

* `_challenge_stat_value(p_user_id, p_target_type)` arbitrary user UUID ile stat okuyabilir.
* `_challenge_my_progress(p_challenge_id, p_user_id)` arbitrary kullanıcı/challenge progress bilgisi döndürebilir.
* `increment_active_days(p_user_id)` arbitrary `profiles.active_days` mutasyonu yapabilir.
* Trigger/event helper fonksiyonları API üzerinden callable olmamalıdır.

**The Fix:**

```sql
-- Default deny for function execution
REVOKE EXECUTE ON ALL FUNCTIONS IN SCHEMA public FROM anon, authenticated;

-- Grant only intentionally public RPCs
GRANT EXECUTE ON FUNCTION public.create_challenge(...) TO authenticated;

-- Move private helpers/triggers away from exposed API schema
CREATE SCHEMA IF NOT EXISTS private;
ALTER FUNCTION public._challenge_stat_value(uuid, text) SET SCHEMA private;
```

* Her `SECURITY DEFINER` fonksiyonuna explicit auth guard ekleyin.
* `auth.uid()` ile caller identity doğrulayın; user-supplied UUID parametresine güvenmeyin.
* Tüm definer fonksiyonlarda `SET search_path = public, pg_temp` veya daha dar sabit search path kullanın.
* Trigger/event helper fonksiyonlarını `public` API schema dışına taşıyın ve execute grantlerini kaldırın.

---

#### F-04: Private Challenge Passwords Stored Plaintext and Readable by All Authenticated Users

**Severity:** Critical

**Location:** Supabase DB `public.group_challenges.password`, RLS policy `Challenges readable by authenticated`, functions `create_challenge`, `create_event_challenge`, `join_challenge`

**The Exploit:** `group_challenges.password` plaintext saklanıyor. Direct table `SELECT` policy yalnızca `auth.role() = 'authenticated'` kontrolü yapıyor; bu, herhangi bir giriş yapmış kullanıcının tüm challenge satırlarını okuyabilmesi anlamına gelir. Saldırgan PostgREST/Supabase client ile `group_challenges` tablosunu sorgulayıp private challenge şifrelerini, event location/geo bilgilerini ve challenge metadata'yı alabilir. `join_challenge` plaintext karşılaştırma yaptığı için DB read elde eden saldırgan private challenge'a doğrudan katılabilir.

**The Fix:**

```sql
-- Do not store plaintext challenge passwords.
ALTER TABLE public.group_challenges
  ADD COLUMN password_hash text;

-- Example creation path
UPDATE public.group_challenges
SET password = NULL;

-- RLS should not expose private challenge secrets.
DROP POLICY "Challenges readable by authenticated" ON public.group_challenges;
CREATE POLICY "Challenges readable by intended audience"
ON public.group_challenges
FOR SELECT
TO authenticated
USING (
  is_public = true
  OR creator_id = auth.uid()
  OR EXISTS (
    SELECT 1 FROM public.group_participants gp
    WHERE gp.challenge_id = id AND gp.user_id = auth.uid()
  )
);
```

* Password/hash alanını client-facing table read yüzeyinden tamamen kaldırın.
* Challenge listing için sanitized view/RPC kullanın.
* `pgcrypto.crypt()` veya backend tarafında modern password hashing kullanın.

---

#### F-05: Public Storage Bucket Listing and Cross-User Object Overwrite/Delete

**Severity:** Critical

**Location:** Supabase Storage bucket `profile-photos`, `storage.objects` policies `profile_photos_select`, `profile_photos_insert`, `profile_photos_update`, `profile_photos_delete`

**The Exploit:** `profile-photos` bucket public ve advisor public listing uyarısı verdi. Insert/update/delete policies sadece `bucket_id = 'profile-photos'` kontrolü yapıyor; object path owner kontrolü yok. Her authenticated kullanıcı başka kullanıcının avatar objesini overwrite veya delete edebilir. Public listing ile object path enumerasyonu da mümkün.

**The Fix:**

```sql
DROP POLICY IF EXISTS profile_photos_insert ON storage.objects;
DROP POLICY IF EXISTS profile_photos_update ON storage.objects;
DROP POLICY IF EXISTS profile_photos_delete ON storage.objects;

CREATE POLICY "profile photos owner insert"
ON storage.objects FOR INSERT TO authenticated
WITH CHECK (
  bucket_id = 'profile-photos'
  AND (storage.foldername(name))[1] = auth.uid()::text
);

CREATE POLICY "profile photos owner update"
ON storage.objects FOR UPDATE TO authenticated
USING (
  bucket_id = 'profile-photos'
  AND (storage.foldername(name))[1] = auth.uid()::text
)
WITH CHECK (
  bucket_id = 'profile-photos'
  AND (storage.foldername(name))[1] = auth.uid()::text
);

CREATE POLICY "profile photos owner delete"
ON storage.objects FOR DELETE TO authenticated
USING (
  bucket_id = 'profile-photos'
  AND (storage.foldername(name))[1] = auth.uid()::text
);
```

* Object path formatını `<auth.uid()>/avatar.webp` gibi owner-bound standardize edin.
* Bucket file size limit ve allowed MIME types tanımlayın.
* Public listing'i kaldırın; gerekirse signed URL veya public read ama no-list pattern kullanın.

---

#### F-06: Overbroad anon/authenticated Table Grants Make RLS the Only Barrier

**Severity:** High

**Location:** Supabase DB `public` and `storage` table grants

**The Exploit:** `anon` ve `authenticated` roller birçok table üzerinde `SELECT`, `INSERT`, `UPDATE`, `DELETE`, `TRUNCATE`, `REFERENCES`, `TRIGGER` grantlerine sahip. RLS enabled olsa da bu modelde tek bariyer policy doğruluğudur. Bir `USING true`, `WITH CHECK true`, missing owner predicate veya future migration hatası doğrudan exploit edilebilir.

**The Fix:**

```sql
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM anon, authenticated;

-- Re-grant minimum required table access, or prefer RPC/view only.
GRANT SELECT ON public.profiles TO authenticated;
```

* Her tablo için least privilege grant matrisi çıkarın.
* Mutasyonları direct table yerine owner-checking RPC'lere taşıyın.
* RLS regression testlerini CI'a ekleyin.

---

#### F-07: Authenticated Users Can Insert Into Canonical exercises with WITH CHECK true

**Severity:** High

**Location:** Supabase DB `public.exercises`, policy `Exercises insertable by authenticated`

**The Exploit:** `exercises` için INSERT policy `WITH CHECK true`. Her authenticated kullanıcı canonical exercise catalog'a içerik ekleyebilir. Saldırgan spam, offensive content, prompt injection metinleri, UI bozacak stringler veya malicious metadata ekleyerek tüm kullanıcıların katalog deneyimini etkileyebilir.

**The Fix:**

```sql
DROP POLICY IF EXISTS "Exercises insertable by authenticated" ON public.exercises;

-- Prefer moderated request table
CREATE TABLE IF NOT EXISTS public.exercise_requests (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  requester_id uuid NOT NULL DEFAULT auth.uid(),
  name text NOT NULL,
  status text NOT NULL DEFAULT 'pending'
);
```

* Exercise creation'ı admin-only yapın.
* Kullanıcı önerileri için moderation queue kullanın.
* Input length, allowed enum ve content validation ekleyin.

---

#### F-08: Password Reset and Auth Hardening Gaps

**Severity:** High

**Location:** `app/src/main/java/com/avonix/profitness/data/auth/AuthRepositoryImpl.kt`, `app/src/main/java/com/avonix/profitness/MainActivity.kt:82`, `app/src/main/java/com/avonix/profitness/presentation/navigation/AppNavigation.kt`, Supabase Auth settings

**The Exploit:** Supabase advisor leaked password protection'ın disabled olduğunu bildirdi. Client-side password validation minimum 6 karakter. Reset flow custom scheme `profitness://reset-password?code=...` kullanıyor; custom scheme Android'de başka uygulamalar tarafından register edilebilir. Reset code route path içinde taşındığı için navigation state/back stack/logging yüzeylerinde kalma riski artar.

**The Fix:**

* Supabase Auth leaked password protection'ı etkinleştirin.
* Password policy'yi server-side enforce edin; yalnız client validation'a güvenmeyin.
* Custom scheme yerine verified Android App Links kullanın.
* Reset token/code'u route path yerine one-shot in-memory state ile tüketin.
* Expired/replayed reset code ve malformed deep link testlerini staging'de otomatikleştirin.

---

#### F-09: Sensitive Local Health/AI Data Stored Without Encryption and Backups Allowed

**Severity:** High

**Location:** `app/src/main/AndroidManifest.xml:16`, `app/src/main/java/com/avonix/profitness/data/cache/DiskCache.kt`, `app/src/main/java/com/avonix/profitness/data/ai/ChatSessionManager.kt`, Room DB usage

**The Exploit:** `allowBackup="true"` açık. Health/fitness verileri, kilo kayıtları, AI chat history ve preference/cache verileri plain Room DB, filesDir disk cache veya plain SharedPreferences içinde kalabilir. Fiziksel cihaz erişimi, Android backup extraction, debug build, rooted device veya malware ile PII/health data sızabilir.

**The Fix:**

```xml
<application
    android:allowBackup="false"
    android:dataExtractionRules="@xml/data_extraction_rules" />
```

* Sensitive cache için EncryptedFile/EncryptedSharedPreferences veya SQLCipher değerlendirin.
* Backup gerekiyorsa health/AI/cache DB dosyalarını `dataExtractionRules` ile exclude edin.
* Logout sırasında user-scoped cache ve AI history temizliğini enforce edin.

---

#### F-10: Debug Signing and Debug APK Release Pipeline

**Severity:** High

**Location:** `.github/workflows/build-apk.yml:49-69`, `app/build.gradle.kts:63-66`

**The Exploit:** CI secrets `local.properties` dosyasına yazılıyor ve workflow `assembleDebug` çalıştırıyor. Gradle release signing config debug keystore kullanıyor. Debug-signed APK release artifact olarak publish edilirse tersine mühendislik, tamper, unauthorized redistribution ve supply-chain confusion riski artar.

**The Fix:**

```yaml
- name: Build Release APK
  run: ./gradlew assembleRelease
```

* Release için ayrı keystore ve CI secret kullanın.
* Debug APK'yı public release artifact olarak yayınlamayın.
* Secrets'i intermediate file'a yazmak yerine Gradle properties/environment injection kullanın ve artifact cleanup ekleyin.
* Play/App Signing veya kurumsal signing pipeline kullanın.

---

#### F-11: Client-Side Mutations Rely on RLS Instead of Owner Predicate Defense-in-Depth

**Severity:** High

**Location:** `ProgramRepositoryImpl.kt`, `WorkoutRepositoryImpl.kt`, `WeightRepositoryImpl.kt`

**The Exploit:** Bazı client mutation çağrıları yalnız resource id ile update/delete yapıyor ve user ownership kontrolünü tamamen RLS'e bırakıyor. Örnek yüzeyler: program update/delete, active program set, program day/exercise cleanup, workout log finish/delete. RLS doğruysa bloklanır; ancak herhangi bir policy/grant regression veya security definer bypass durumunda IDOR exploit doğrudan başka kullanıcının objesine mutasyona dönüşür.

**The Fix:**

```kotlin
supabase.from("programs")
    .update(updatePayload) {
        filter {
            eq("id", programId)
            eq("user_id", userId)
        }
    }
```

* Client query'lerinde owner predicate ekleyin.
* Mutasyonları DB RPC içinde `auth.uid()` ownership check ile merkezi hale getirin.
* Cross-user read/write/delete regression testleri ekleyin.

---

#### F-12: Raw Backend Error Messages Surfaced to UI

**Severity:** Medium

**Location:** `DiscoverViewModel.kt`, `FriendsViewModel.kt`, `ProgramViewModel.kt`

**The Exploit:** Çok sayıda `err.message` / `e.message` doğrudan UI state'e taşınıyor. Supabase/PostgREST raw error'ları tablo, kolon, policy, constraint, RPC ve internal backend detaylarını sızdırabilir. Saldırgan malformed inputlarla hata yüzeyini kullanarak schema discovery ve policy inference yapabilir.

**The Fix:**

```kotlin
fun Throwable.toUserSafeMessage(): String =
    when (this) {
        is java.net.SocketTimeoutException -> "Connection timed out."
        else -> "Something went wrong. Please try again."
    }
```

* Internal error detaylarını telemetry'ye redacted gönderin.
* UI için allowlisted, localized, generic error mapper kullanın.
* Auth error'larda enumeration-safe mesaj standardı uygulayın.

---

#### F-13: Analytics Logs Can Leak PII in Logcat

**Severity:** Medium

**Location:** `app/src/main/java/com/avonix/profitness/core/analytics/AnalyticsTracker.kt:21-23`

**The Exploit:** `Log.d` ile event adı ve properties loglanıyor. Properties içine email, UUID, workout details, challenge name/password veya backend error girerse debug/release varyantına bağlı olarak Logcat, crash attachments veya support captures üzerinden PII sızabilir.

**The Fix:**

```kotlin
if (BuildConfig.DEBUG) {
    Log.d("Analytics", "Event: $eventName")
}
```

* Properties'i raw loglamayın.
* Debug guard ve redaction allowlist kullanın.
* Release build'de analytics logging'i kapatın veya privacy-safe telemetry SDK'ya taşıyın.

---

#### F-14: AI Privacy and Network Resilience Gaps

**Severity:** Medium

**Location:** `GeminiRepositoryImpl.kt`, `ChatSessionManager.kt`, `AICoachPreferences.kt`, `AppModule.kt`

**The Exploit:** AI coach flow doğrudan mobil istemciden Gemini'ye gidiyor; full chat history ve user promptları üçüncü parti API'ye gönderiliyor. `HttpClient(Android)` explicit timeout olmadan kurulmuş. Saldırgan büyük prompt/media ile latency, quota consumption veya UI degradation yaratabilir; privacy açısından consent, retention ve redaction kontrolleri backend seviyesinde enforce edilmiyor.

**The Fix:**

* AI çağrılarını backend gateway'e taşıyın.
* Prompt/token/media size limitlerini server-side enforce edin.
* `HttpTimeout` yapılandırın.
* AI chat retention, user deletion ve consent kontrollerini dokümante ve test edin.

---

#### F-15: Lint Failure Can Cause Runtime Crash on Supported Devices

**Severity:** Medium

**Location:** `app/src/main/java/com/avonix/profitness/data/ai/ChatSessionManager.kt:30`

**The Exploit:** `ids.removeLast()` API 35 gerektiriyor; `minSdk` 31. API 31-34 cihazlarda ilgili path çalışırsa `NoSuchMethodError` ile crash oluşabilir. Güvenlik etkisi doğrudan exploit değil, availability ve logout/cache cleanup gibi akışların yarıda kalması riskidir.

**The Fix:**

```kotlin
if (ids.size > MAX_SESSIONS) {
    ids.removeAt(ids.lastIndex)
}
```

---

#### F-16: Dependency Drift and Missing Security Regression Tests

**Severity:** Medium

**Location:** `build.gradle.kts`, `app/build.gradle.kts`, test tree

**The Exploit:** Android lint çok sayıda outdated dependency uyarısı verdi: Android Gradle Plugin, Kotlin, Supabase, Ktor, Room, Navigation, Lifecycle, Compose BOM ve diğer runtime bileşenleri geride. Repo içinde tracked unit/instrumentation security regression testleri görülmedi. Bu durum RLS, auth, storage ve privacy regresyonlarının CI'da yakalanmamasına neden olur.

**The Fix:**

* OSV/OWASP Dependency Check veya Gradle dependency verification ile CI audit ekleyin.
* Supabase RLS/RPC/storage policy regression testleri ekleyin.
* Auth reset, logout cache isolation ve cross-user IDOR senaryolarını staging test suite'e alın.

---

## Auth / JWT / Session Analysis

**Current positive controls**

* Supabase PKCE flow kullanılıyor.
* Current session/user checks mevcut.
* Logout flow çağrısı bulunuyor.

**Risks**

* Leaked password protection disabled.
* Client-side password minimumu zayıf.
* Reset code custom scheme ve navigation route içinde taşınıyor.
* Email existence için `check_email_registered(p_email)` exposed RPC email enumeration yüzeyi oluşturuyor.
* Logout sonrası local Room/cache/SharedPreferences izolasyonu kod seviyesinde kanıtlanmış değil.

**Required staging tests**

* Expired reset code must fail.
* Replayed reset code must fail.
* Malformed deep link must fail closed.
* OTP resend/rate limit behavior must be measured.
* Logout sonrası eski kullanıcının AI chat, profile, program, weight ve avatar cache verisi görünmemeli.
* User B, User A UUID'si ile profile/program/workout/weight/challenge RPC/table çağrısı yaptığında read/write/delete fail olmalı.

---

## DB / RLS / RPC Matrix Summary

| Area | Current State | Risk |
|---|---|---|
| Public tables | 26/26 RLS enabled | Positive baseline |
| Storage tables | 8/8 RLS enabled | Positive baseline |
| Security definer functions | 54 total, 54 executable by anon/authenticated | Critical |
| Missing/mutable search_path | 3 advisor findings | Critical/High |
| Direct table grants | Broad grants to anon/authenticated | High |
| `group_challenges` read | All authenticated can read | Critical |
| `group_participants` read | All authenticated can read | Medium/High privacy risk |
| `shared_program_recipients` read | Authenticated `USING true` | Medium/High privacy risk |
| `user_follows` read | Authenticated `USING true` | Context-dependent privacy risk |
| `exercises` insert | Authenticated `WITH CHECK true` | High |

**Immediate DB remediation order**

1. Revoke function execute grants globally.
2. Re-grant only intended RPCs.
3. Move helper/trigger functions to private schema.
4. Fix challenge password storage and challenge SELECT policy.
5. Fix storage object ownership policies.
6. Reduce table grants to least privilege.
7. Add RLS/RPC regression tests before further schema changes.

---

## Storage / Local Privacy Matrix Summary

| Asset | Current State | Risk |
|---|---|---|
| `profile-photos` bucket | Public | Public listing/object enumeration |
| Storage write policies | Bucket-only | Cross-user overwrite/delete |
| File size limit | Not configured | Abuse/storage exhaustion |
| MIME allowlist | Not configured | Content spoofing |
| Android backup | `allowBackup=true` | Health/AI data backup exposure |
| SharedPreferences | Plain | Local PII/session-adjacent data exposure |
| Disk cache | Plain files | Health/AI cache exposure |
| Room DB | Plain by default | Health data at rest exposure |

---

## Code Quality / Injection / Logging Summary

**Injection**

* Kotlin client largely uses Supabase structured query APIs, reducing classic SQL injection surface.
* Main injection-like risks are stored content abuse: profile fields, exercise names, challenge/program names, AI prompts and raw backend error propagation.
* No WebView or shell command execution surface was identified in reviewed code.

**Logging**

* Analytics debug logging can expose event properties.
* Raw backend error messages can expose schema/policy details to UI.
* Generated/build logs must never print `local.properties` or `BuildConfig` secret values.

**Build supply chain**

* Debug signing is used for release signing config.
* Debug APK release workflow exists.
* Secrets are written into `local.properties` during CI.
* APK decompile confirmed embedded runtime keys/config.

---

## Recommended Security Test Backlog

#### Staging Active Tests

| Test | Expected Result |
|---|---|
| Anonymous call to every mutation RPC | Must fail with auth required |
| Anonymous call to every helper/trigger RPC | Must fail/not exposed |
| User B reads User A `programs`, `workout_logs`, `weight_logs` | Must fail unless intentionally public |
| User B updates/deletes User A program/workout/weight rows | Must fail |
| User B reads private `group_challenges.password` | Must fail; password must not be selectable |
| User B overwrites/deletes User A avatar object | Must fail |
| Public bucket listing without auth | Must fail or expose no object list |
| Exercise spam insert as normal user | Must fail |
| `increment_active_days(User A)` as User B/anon | Must fail |
| `_challenge_stat_value(User A, ...)` as User B/anon | Must fail |
| Reset token replay | Must fail |
| Malformed reset deep link | Must fail closed |
| Logout then login as second user | Must not show prior cache/history |

#### Static/CI Tests

* Secret scan: `.mcp.json`, `local.properties`, generated source, APK, workflow logs.
* APKTool/JADX scan for API keys, URLs, debug flags, signing config.
* Android lint must pass.
* Dependency vulnerability scan via OSV/OWASP Dependency Check.
* Supabase advisors must be clean or accepted with documented exceptions.
* RLS policy tests should run on disposable staging DB.

---

## Observations

* RLS is enabled on all listed public and storage tables, which is a good baseline but not sufficient with current broad grants and policies.
* PKCE usage in auth flow is a strong baseline for mobile auth.
* Supabase Edge Functions list is empty; Gemini is currently direct-from-mobile, which concentrates API key and abuse-control risk in the client.
* No tracked test suite was found for auth/RLS/storage regression.
* This audit was originally produced before remediation work. See the remediation addendum below for the fixes applied afterward.

---

## Remediation Addendum - 2026-04-29

**Explicitly not changed by request**

* Supabase access token rotation/revocation was not performed.
* Gemini API key rotation and removing Gemini from the mobile client architecture were not performed.

**Applied fixes**

* Supabase public RPC surface was converted to an allowlisted model: `anon` can no longer execute app mutation/read RPCs except the compatibility-safe `check_email_registered`.
* Public `SECURITY DEFINER` RPCs were moved behind `private` schema bodies with `SECURITY INVOKER` public wrappers; Supabase security advisor no longer reports exposed public `SECURITY DEFINER` RPCs.
* Challenge passwords were migrated from plaintext to `password_hash`; plaintext `group_challenges.password` values were nulled.
* Challenge direct-read policies were narrowed to public challenges, creators, or participants.
* Storage `profile-photos` policies were changed to owner-bound object paths, including backwards-compatible ownership checks for legacy `avatars/<uid>.jpg` objects.
* Storage bucket file size and MIME allowlist were configured for profile photos.
* `exercises` no longer has authenticated `WITH CHECK true` global insert; custom exercises are owner-scoped with `created_by`.
* Android backup was disabled and backup/data extraction exclusions were added for local DB/preferences/cache.
* AI chat session history was made user-scoped to reduce cross-account local data leakage.
* Logcat analytics now logs only event names/keys in debug builds and does not log property values.
* Raw backend errors in major UI surfaces were mapped to user-safe messages.
* Reset password code is no longer placed in the navigation route/back stack.
* The `removeLast()` API 35 lint failure was fixed.
* CI/release build now targets release artifacts and requires release signing secrets instead of publishing debug-signed APKs.

**Verification**

* `./gradlew.bat :app:lintDebug --no-daemon` passed.
* `./gradlew.bat :app:assembleDebug --no-daemon` passed.
* Supabase security advisors now report only `auth_leaked_password_protection` as remaining.
* Read-only SQL checks confirmed: public exposed `SECURITY DEFINER` executable count is 0 for `anon` and 0 for `authenticated`; plaintext challenge password count is 0; profile-photo owner policies are present; old open exercise insert policy is removed.

---

## Remediation Addendum - AI Key Isolation

**Applied fixes**

* Gemini direct-from-mobile calls were removed from the Android client path.
* Android now calls the JWT-protected Supabase Edge Function `gemini-generate`.
* `GEMINI_API_KEY` was removed from Android `BuildConfig` and GitHub Actions APK secret injection.
* The Edge Function validates the Supabase session, enforces request/media size limits, clamps generation config, allowlists inline media MIME types, and returns sanitized provider errors.
* A server-side `public.ai_rate_limits` table and service-role-only `public.check_ai_rate_limit(...)` RPC were added for per-user AI request throttling.
* Supabase local function env files are ignored by Git.

**Required operations outside code**

* Rotate the Gemini API key and store the new value only as a Supabase Edge Function secret named `GEMINI_API_KEY`.
* Rotate/revoke the Supabase MCP access token and re-authenticate MCP/Codex if needed.
* Prefer a Supabase publishable key. Store it as `SUPABASE_PUBLISHABLE_KEY` in `local.properties` and GitHub Actions secrets. Legacy `SUPABASE_ANON_KEY` remains accepted as a compatibility fallback.
* `SUPABASE_URL` does not need to change unless the app is moved to a different Supabase project.
