# Profitness Android + Supabase Altyapi Denetimi

Denetim tarihi: 2026-05-17, Europe/Istanbul  
Kapsam: Android uygulama kodu, Room/offline sync, Supabase migrations, Edge Functions, Storage, canli Supabase metadata/advisor/log kontrolleri.  
Sinir: Bu denetim yalniz rapordur. Kotlin, SQL, proje yapilandirmasi veya canli Supabase semasi degistirilmedi. Hassas degerler ve token/env icerikleri rapora yazilmadi.

## Architectural Health Score

**Genel skor: 72 / 100**

| Alan | Puan | Yorum |
| --- | ---: | --- |
| Android mimari | 23 / 35 | Hilt, Room, Compose, ViewModel/StateFlow ve repository yapisi urun icin uygun; ancak presentation katmaninda `SupabaseClient` sizintisi, buyuk ekran/ViewModel dosyalari ve zayif typed error modeli devam ediyor. |
| Supabase/Postgres guvenlik ve performans | 30 / 35 | Yeni hardening ciddi ilerleme saglamis: tum public tablolar RLS acik, anon RPC yuzeyi keskin sekilde daraltilmis, Edge Function JWT modeli daha iyi. Kalan advisor bulgulari ve webhook legacy fallback kapatilmazsa kurumsal kabulde soru isareti olur. |
| Offline/sync/error handling | 14 / 20 | Room-first yaklasim ve `SyncCoordinator` TTL/in-flight dedupe iyi yonde; fakat durable queue, conflict policy, retry/backoff ve domain-safe hata modeli tamamlanmamis. |
| Test/observability/operasyonel olgunluk | 5 / 10 | `lintDebug` basarili; Supabase advisor/log kontrolleri yapilabiliyor. Buna karsin test kaynaklari yok, `testDebugUnitTest` NO-SOURCE, runtime observability henuz urun standardinda degil. |

**Stack suitability:** Hilt + Room + Compose + Supabase bu urun icin dogru secim. Son yapisal degisiklikler ozellikle Supabase tarafini MVP guvenlik seviyesinden daha olgun bir seviyeye tasimis. Kurumsal urun seviyesinin onundeki ana engel artik veritabani aciklari degil; Android katman sinirlari, test eksikligi, sync dayanıkliligi ve network/latency butcesi.

### Risk Heatmap

| Risk | Sicaklik | Durum |
| --- | --- | --- |
| Presentation katmanina Supabase SDK sizintisi | High | Degismemis. En az 20 presentation referansi var. |
| Buyuk Compose/Screen/ViewModel dosyalari | High | Degismemis/yuksek. En buyuk dosyalar 1.8k-3k satir bandinda. |
| Public/anon RPC yuzeyi | Medium-Low | Iyilesmis. Canli DB'de anon executable public function sayisi 1'e dusmus. |
| Private security definer fonksiyon yuzeyi | Medium | `private` schema anon'a kapali; ancak 45 private fonksiyon authenticated role'a executable. Wrapper/body ayrimi bilincli tutulmali. |
| Billing/AI Edge Function service role kullanimi | Medium-Low | Service role Edge Function icinde sinirli; JWT required fonksiyonlar dogru. Webhook public olmak zorunda, HMAC var; legacy secret fallback kapatilmasi gerekir. |
| Offline sync partial failure/conflict | Medium-High | Delta pull ve dedupe iyi; durable operation log ve conflict contract eksik. |
| Test/QA kaniti | High | Unit/instrumentation test klasorleri yok; Gradle test task'i NO-SOURCE. |
| Supabase advisor bulgulari | Medium | `function_search_path_mutable`, `rls_enabled_no_policy`, FK index, RLS initplan ve duplicate index uyarilari kalmis. |

## Since Last Audit: Ne Degisti?

- Supabase hardening migration'lari eklenmis ve canli DB'de uygulanmis gorunuyor: anon table grant revoke, public RPC execute grant sikilastirma, private function execute grant sikilastirma, avatar URL constraint, AI credit/billing hardening.
- Canli grant kontrolunde public function sayisi 74; anon executable public function sayisi 1; private schema anon executable 0. Bu onceki kritik RPC yetki genislemesi riskini ciddi sekilde dusuruyor.
- Edge Functions canli listesinde hassas cagri fonksiyonlari `verify_jwt=true`: `ai-generate`, `gemini-generate`, `billing-status`, `billing-checkout`, `billing-sandbox-complete`. Public kalanlar `billing-webhook` ve `email-assets`; bu model mimari olarak kabul edilebilir.
- Profil fotografi ve online event URL tarafinda yeni savunmalar var: MIME/5 MB kontrolu, public profile-photo bucket siniri, avatar URL check constraint ve `http/https` URL normalize edici.
- Android tarafinda buyuk dosya, presentation SDK coupling, untyped `Result<T>` ve test eksikligi riski ayni agirlikta devam ediyor.

## Structural Findings

### 1. Presentation katmani Supabase SDK'ya dogrudan bagli

- **Pillar:** Android mimari
- **Severity:** High
- **Kanıt:** `presentation/workout/WorkoutViewModel.kt:127`, `presentation/aicoach/AICoachViewModel.kt:78`, `presentation/program/ProgramViewModel.kt:85`, `presentation/profile/ProfileViewModel.kt:109`, `presentation/profile/ExerciseProgressionScreen.kt:81`, `presentation/discover/DiscoverViewModel.kt:64`. `rg` taramasinda presentation altinda 20 `SupabaseClient` referansi bulundu.
- **Violation:** Presentation katmani auth/session ve postgrest detaylarini biliyor. ViewModel'ler business flow yerine Supabase session resolution ve network orchestration tasiyor.
- **Why it matters:** Test yazmak, offline-first davranisi izole etmek ve SDK degisimlerini absorbe etmek zorlasiyor. UI state hatalari dogrudan backend SDK davranisina bagimli kaliyor.
- **Senior Fix:** SDK'yi data katmaninda tut; presentation sadece use case veya repository port'u gorsun.

```kotlin
interface CurrentUserProvider {
    suspend fun requireUserId(): UserId
}

class StartWorkoutUseCase(
    private val userProvider: CurrentUserProvider,
    private val workouts: WorkoutRepository
) {
    suspend operator fun invoke(input: StartWorkoutInput): DomainResult<WorkoutSession> {
        val userId = userProvider.requireUserId()
        return workouts.start(userId, input)
    }
}
```

- **Refactor Effort:** M-L. En once `WorkoutViewModel`, `AICoachViewModel`, `ProgramViewModel`, `ProfileViewModel` icin auth/session abstraction cikarilmali.

### 2. Ekran ve ViewModel dosyalari kurumsal bakim esigini asiyor

- **Pillar:** Android mimari
- **Severity:** High
- **Kanıt:** `ProgramBuilderScreen.kt` 2983 satir, `ChallengeDetailOverlay.kt` 2249 satir, `ProfileScreen.kt` 2219 satir, `StoreScreen.kt` 1916 satir, `WorkoutViewModel.kt` 1879 satir, `CreateChallengeOverlay.kt` 1804 satir.
- **Violation:** Tek dosyada route, layout, state mapping, form validation, network-trigger ve rendering birikiyor.
- **Why it matters:** Degisiklik etkisi tahmin edilemez hale gelir; Compose recomposition hatalari, preview/test yazimi ve code review kalitesi duser.
- **Senior Fix:** Dosya bolme UI refactor'u davranis degistirmeden yapilmali: route/state/effect/components ayrimi.

```kotlin
@Composable
fun ProgramBuilderRoute(viewModel: ProgramViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProgramBuilderScreen(
        state = state,
        onAction = viewModel::onAction
    )
}
```

- **Refactor Effort:** L. Once `ProgramBuilderScreen` ve `WorkoutViewModel`, sonra `ProfileScreen` ve challenge overlay'leri.

### 3. `Result<T>` ve `runCatching` domain-safe hata modeli yerine gecmis

- **Pillar:** Offline/sync/error handling
- **Severity:** Medium-High
- **Kanıt:** `rg` sonucunda 203 `Result<` ve 219 `runCatching` kullanimi. Ornekler: `data/program/ProgramRepository.kt`, `data/profile/ProfileRepository.kt`, `data/sync/SyncManager.kt:236`, `presentation/aicoach/AICoachViewModel.kt:194`.
- **Violation:** Kotlin `Result` exception'i tasir ama domain kararlarini tipli hale getirmez. UI genellikle "basarisiz" gorur; auth expired, quota exceeded, conflict, network timeout, validation, RLS denied ayrimi kaybolur.
- **Why it matters:** Billing/AI, sync ve auth akislari kullaniciya farkli aksiyon gerektirir. Untyped hata modeli support maliyetini ve retry/rollback bug'larini artirir.
- **Senior Fix:** `DomainError` sealed hierarchy ve `DomainResult` kullan.

```kotlin
sealed interface DomainError {
    data object Unauthorized : DomainError
    data object Offline : DomainError
    data class QuotaExceeded(val remainingCredits: Int?) : DomainError
    data class Conflict(val entity: String) : DomainError
    data class Unexpected(val cause: Throwable) : DomainError
}

typealias DomainResult<T> = Either<DomainError, T>
```

- **Refactor Effort:** M. Once AI/billing ve sync repository'leri; sonra presentation state mapping.

### 4. Sync mimarisi iyilesmis, fakat durable conflict/retry modeli eksik

- **Pillar:** Offline/sync/error handling
- **Severity:** Medium-High
- **Kanıt:** `data/sync/SyncCoordinator.kt:30` in-flight dedupe, `SyncCoordinator.kt:119-120` TTL, `SyncManager.kt:236-255` delta `pullSetCompletions`, `SyncManager.kt:331-360` dirty push. Room `AppDatabase.kt:32-33` version 13, `exportSchema=false`.
- **Violation:** Sync operasyonlari fonksiyonel olarak var ama kalici operation queue, retry budget, conflict resolution ve partial failure checkpoint modeli net degil.
- **Why it matters:** Zayif baglanti, uygulama kill, ayni hesabin iki cihazda kullanimi ve billing/AI yan etkileri birlikte dusunuldugunde "son yazan kazanir" veya sessiz veri kaybi riski dogar.
- **Senior Fix:** Dirty flag yanina operation log ve conflict contract ekle.

```kotlin
@Entity(tableName = "sync_operations")
data class SyncOperationEntity(
    @PrimaryKey val id: String,
    val aggregate: String,
    val payloadJson: String,
    val attemptCount: Int,
    val nextRunAt: Long,
    val idempotencyKey: String
)
```

- **Refactor Effort:** L. Workout/set completion ile baslayip weight/program tarafina genisletilmeli.

### 5. Room migration ve regression test kaniti yok

- **Pillar:** Test/observability/operasyonel olgunluk
- **Severity:** High
- **Kanıt:** `app/src/test`, `app/src/androidTest`, `baselineprofile/src/androidTest` bulunamadi. `./gradlew.bat :app:testDebugUnitTest --no-daemon` basarili ama `NO-SOURCE`. `AppDatabase.kt:33` `exportSchema=false`.
- **Violation:** Room migration'lari manuel yaziliyor ama schema export ve migration testleri yok.
- **Why it matters:** Fresh install ve eski cihaz upgrade yollari urun icin kritik. Bir Room migration hatasi uygulamayi acilista dusurebilir ve local-first veriyi riske atar.
- **Senior Fix:** Schema export'u ac, CI'da migration testleri calistir.

```kotlin
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @Test fun migrate12To13_preservesSetCompletions() {
        helper.createDatabase(AppDatabase.NAME, 12).close()
        helper.runMigrationsAndValidate(
            AppDatabase.NAME,
            13,
            true,
            AppDatabase.MIGRATION_12_13
        )
    }
}
```

- **Refactor Effort:** M. Ilk hedef: Room migration tests + sync coordinator unit tests + AI/billing repository fake tests.

### 6. Supabase RPC/grant yuzeyi artik daha iyi, kalan yuzey explicit allowlist'e baglanmali

- **Pillar:** Supabase/Postgres guvenlik
- **Severity:** Medium
- **Kanıt:** `20260517201854_revoke_anon_table_grants.sql:1-3`, `20260517204113_tighten_public_rpc_execute_grants.sql:1-21`, `20260517211800_tighten_private_function_execute_grants.sql:3-11`. Canli SQL kontrolde public anon executable function = 1: `public.check_email_registered(p_email text)`. Private anon executable = 0; private authenticated executable = 45.
- **Violation:** Kritik risk buyuk olcude giderilmis; kalan risk, fonksiyon erisimlerinin genis default grant yerine dokumante edilmis allowlist'e baglanmamasi.
- **Why it matters:** `security definer` fonksiyonlar RLS'i bilincli by-pass eder. Yanlis grant, auth kullanicisini beklenenden fazla yetkili hale getirebilir.
- **Senior Fix:** Public wrapper'lari tek tek grant'le, body/helper fonksiyonlari private tut, anon fonksiyon icin rate-limit ve minimal response uygula.

```sql
revoke execute on all functions in schema public from public, anon, authenticated;
grant execute on function public.list_my_challenges() to authenticated;
grant execute on function public.check_email_registered(text) to anon, authenticated;
```

- **Refactor Effort:** S-M. Mevcut hardening dogru yonde; sadece allowlist ve audit dokumantasyonu tamamlanmali.

### 7. Supabase advisor bulgulari kapatilmadan "enterprise ready" denmemeli

- **Pillar:** Supabase/Postgres guvenlik ve performans
- **Severity:** Medium
- **Kanıt:** Canli advisor bulgulari:
  - Security: `rls_enabled_no_policy` -> `billing_webhook_events`, `usage_counters`; `function_search_path_mutable` -> `public.set_set_completions_updated_at`; leaked password protection disabled.
  - Performance: unindexed FK'ler -> `billing_orders_sku_fkey`, `billing_orders_user_id_fkey`, `credit_ledger_user_id_fkey`, `group_challenges_event_exercise_id_fkey`, `set_completions_exercise_id_fkey`.
  - Performance: RLS initplan uyarilari -> `group_challenges`, `group_participants`, `shared_program_recipients`, `user_follows`, `exercises`.
  - Duplicate index: `public.exercises` icin `exercises_created_by_idx` ve `idx_exercises_created_by`.
- **Violation:** Linter bulgulari dusuk veri hacminde sorun cikarmayabilir; fakat buyume ve kurumsal denetimde kabul kriteridir.
- **Why it matters:** RLS initplan ve eksik FK indexleri veri buyudukce p95 latency'yi bozar. Search path mutable fonksiyonlar guvenlik standardinda kapatilmalidir.
- **Senior Fix:**

```sql
alter function public.set_set_completions_updated_at()
set search_path = public, pg_temp;

create index concurrently if not exists billing_orders_user_id_idx
on public.billing_orders(user_id);

drop index concurrently if exists public.idx_exercises_created_by;
```

- **Refactor Effort:** S-M. Advisor remediation icin resmi referans: [Supabase Performance and Security Advisors](https://supabase.com/docs/guides/database/database-linter), RLS performans icin [Supabase RLS performance recommendations](https://supabase.com/docs/guides/database/postgres/row-level-security).

### 8. Edge Function modeli guclenmis; billing latency ve webhook legacy fallback kalmis

- **Pillar:** Supabase/Postgres guvenlik ve operasyon
- **Severity:** Medium
- **Kanıt:** `_shared/auth.ts:27-33` service role admin client sadece Edge Function tarafinda; `_shared/auth.ts:35-53` bearer token ile user dogrulama. `ai-generate/index.ts:177-197` request size + authenticated user + reserve usage; `ai-generate/index.ts:268-273` complete/refund flow. `billing-webhook/index.ts:59-72` HMAC verification; `billing-webhook/index.ts:79-81` legacy secret fallback default acik. Canli Edge listesinde `billing-status`, `billing-checkout`, `billing-sandbox-complete`, `ai-generate`, `gemini-generate` JWT required; `billing-webhook` ve `email-assets` public.
- **Violation:** Genel model dogru; ancak webhook HMAC rollout tamamlaninca legacy secret fallback kapatilmali. `billing-status` her cagri icin `ensure_billing_account` + 4 tablo read yapiyor.
- **Why it matters:** Billing endpointleri urun guveni ve latency butcesi icin en kritik yuzey. Loglarda `billing-status` icin tekrarli POST'lar ve bazi multi-second execution sureleri goruldu.
- **Senior Fix:** Billing snapshot'i tek RPC veya materialized read model ile topla; webhook fallback'i env ile kapat; Edge timeout/error metric ekle.

```sql
create or replace function public.get_billing_snapshot()
returns jsonb
language sql
security definer
set search_path = public, pg_temp
as $$
  select jsonb_build_object(
    'entitlement', e,
    'credits', c.balance
  )
  from user_entitlements e
  join user_credit_accounts c on c.user_id = e.user_id
  where e.user_id = auth.uid();
$$;
```

- **Refactor Effort:** M.

### 9. Profile photo ve external URL savunmalari iyi, public storage karari urun politikasi gerektirir

- **Pillar:** Supabase/Postgres guvenlik
- **Severity:** Low-Medium
- **Kanıt:** `ProfilePhotoPickerSecurity.kt:8-44` 5 MB ve MIME kontrolu; `ProfileRepositoryImpl.kt:131-143` `profile-photos` public URL upload; canli Storage bucket `profile-photos` public, 5 MB, MIME allowlist `image/jpeg`, `image/png`, `image/webp`; `20260517211900_restrict_profile_avatar_urls.sql:1-12` external HTTP avatar URL kısıtı; `ChallengeUrlSecurity.kt:5-18` online event URL normalize/validate.
- **Violation:** Teknik savunma iyi; public avatar bucket urun/gizlilik politikasi olarak acik secimdir.
- **Why it matters:** Public bucket'ta yuklenen fotograf URL'si bilen herkes tarafindan erisilebilir. Kullanici silme, avatar visibility ve abuse/moderation davranisi net olmali.
- **Senior Fix:** Public avatar kabul ediliyorsa privacy copy, delete flow ve cache invalidation policy yaz. Daha hassas profil icin signed URL + transform pipeline dusun.
- **Refactor Effort:** S-M.

### 10. Redundant fetch ve network butcesi hala urun riski

- **Pillar:** Android mimari / Supabase performans
- **Severity:** Medium
- **Kanıt:** Canli API loglarinda Ktor/PostgREST tarafindan `workout_logs`, `exercise_logs`, `program_exercises`, `user_stats`, leaderboard RPC'leri ve billing status tekrarli cagri paternleri goruldu. `ProfileRepositoryImpl.kt:246-326` profil ekraninda farkli tablolar icin ardışik fetch'ler; `SyncCoordinator.kt:38-52` workout refresh birden fazla pull/push fonksiyonu calistiriyor.
- **Violation:** Room-first tasarim var ama ekran bazli "hydrate everything" davranisi latency ve pil/network maliyetini buyutuyor.
- **Why it matters:** Sosyal/challenge/feed/billing ozellikleri arttikca p95 ekran acilis suresi ve Supabase quota maliyeti artar.
- **Senior Fix:** Ekran snapshot RPC'leri, local cache invalidation ve lifecycle-aware refresh budget kullan.
- **Refactor Effort:** M.

## Database & Security Deep-Dive

### RLS ve table grants

- Canli `list_tables` sonucunda public tablolarda RLS enabled goruldu.
- Billing ve AI tablolarinda servis tarafli tasarim belirgin: `billing_webhook_events` ve `usage_counters` RLS enabled ama policy yok. Bu server-only ise kabul edilebilir; ancak advisor icin migration comment + explicit revoke + test query ile dokumante edilmeli.
- `billing_products`, `challenge_invites`, `set_completions` anon grant'leri son migration'larla revoke edilmis.

### RPC ve function execution modeli

- Canli fonksiyon ozeti: public functions 74, public anon executable 1, private functions 45, private anon executable 0, private authenticated executable 45.
- Kalan anon RPC: `check_email_registered(p_email text)`. Bu fonksiyon auth UX icin tutulabilir; response minimal olmali, rate-limit/abuse monitoring eklenmeli.
- `security definer` fonksiyonlarda search path cogu yerde set edilmis; advisor yalniz `set_set_completions_updated_at` icin mutable search_path uyariyor.
- `private` schema anon'a kapali; authenticated execute genis oldugu icin public wrapper/body ayrimi dokumante edilmeli.

### Edge Functions

| Function | JWT | Denetim yorumu |
| --- | --- | --- |
| `ai-generate` | true | Authenticated user + service role RPC ile entitlement reserve/complete/refund modeli iyi. |
| `gemini-generate` | true | `ai-generate` ile benzer quota ve request-size kontrolleri var. |
| `billing-status` | true | Dogru auth modeli; read fanout ve tekrarli cagri latency riski var. |
| `billing-checkout` | true | Kullanici baslatmali flow icin dogru. |
| `billing-sandbox-complete` | true | Sandbox flag ve order status kontrolu korunmali. |
| `billing-webhook` | false | Public olmasi normal; HMAC var. Legacy secret fallback kapatilmali. |
| `email-assets` | false | Public asset endpoint olarak kabul edilebilir. |

### Index ve performance

- Yeni index migration'lari var: set completions delta sync ve workout query indexleri eklenmis.
- Kalan advisor FK indexleri uygulanmali: billing orders user/SKU, credit ledger user, challenge event exercise, set completion exercise.
- RLS initplan uyarilari icin `auth.uid()` ve helper fonksiyonlar `(select auth.uid())` veya stable cached pattern ile revize edilmeli.
- Duplicate `exercises.created_by` indexlerinden biri kaldirilmali.

### Storage

- `profile-photos` public bucket: 5 MB, JPEG/PNG/WebP allowlist.
- App-side read guard var; DB avatar URL constraint external HTTP URL'leri engelliyor.
- Public avatar karari privacy/abuse/delete lifecycle ile tamamlanmali.

### Logs ve runtime sinyalleri

- Son 24 saat Edge loglarinda yaygin 5xx paterni gorulmedi.
- `billing-status` cagrilarinda tekrar ve yer yer multi-second execution goruldu; billing snapshot ve client refresh budget onceliklendirilmeli.
- Bir `gemini-generate` 401 auth hatasi goruldu; JWT required model icin beklenen sinyal.
- API loglari tekrarlanan PostgREST fetch paternlerini dogruluyor; kullanici/id detaylari redakte edildi.

## Roadmap to Enterprise Maturity

### Phase 1: Stabilization (1-2 hafta)

1. Supabase advisor temizligi: search_path fix, FK indexleri, duplicate index, RLS initplan policy rewrite, leaked password protection.
2. `check_email_registered` icin abuse guard: rate limit, captcha/turnstile dusunumu veya Edge wrapper.
3. `BILLING_WEBHOOK_ALLOW_LEGACY_SECRET=false` rollout plani; HMAC zorunlu hale getir.
4. Room schema export + migration test altyapisini ac.
5. `WorkoutViewModel`, `AICoachViewModel`, `ProgramViewModel` icin `SupabaseClient` dependency'lerini use case/repository abstraction'a indir.
6. Billing status icin tek snapshot RPC veya cached Edge response tasarla.

### Phase 2: Modernization (2-5 hafta)

1. Buyuk ekranlari route/state/effect/components olarak bol: `ProgramBuilderScreen`, `ProfileScreen`, `ChallengeDetailOverlay`, `StoreScreen`.
2. `DomainError` ve `DomainResult` modelini AI/billing/sync akislariyla baslat.
3. Durable sync operation queue, retry/backoff ve idempotency key modeli ekle.
4. Profile/challenge/dashboard icin Room-first snapshot cache ve ekran refresh budget uygula.
5. Public/private RPC allowlist dokumani ve migration-level access testleri ekle.

### Phase 3: Observability & Operations (2-4 hafta)

1. Edge Functions icin structured logs: request id, user id hash, status code, provider latency, DB latency.
2. Android tarafinda crash + non-fatal + sync failure telemetry; hassas payload redaction zorunlu.
3. CI quality gates: `testDebugUnitTest`, `lintDebug`, Room migration tests, Supabase local migration dry-run.
4. p95 ekran acilis ve network request budget dashboard'u.
5. Billing/AI entitlement invariants icin nightly smoke checks.

## Verification Notes

- Statik taramalar: `SupabaseClient`, `Dispatchers`, `runCatching`, `MutableStateFlow`, `Result<`, `security definer`, `grant/revoke`, `create policy`, `create index`, `service_role`, Edge env access.
- Canli Supabase read-only kontroller: tables, migrations, advisors, Edge Functions, Storage bucket config, API/Edge logs, function grant summary. Mutation yapilmadi.
- `./gradlew.bat :app:testDebugUnitTest --no-daemon`: BUILD SUCCESSFUL, ancak unit test source yok (`NO-SOURCE`).
- `./gradlew.bat :app:lintDebug --no-daemon`: BUILD SUCCESSFUL; lint raporu `0 errors, 88 warnings, 10 hints`.
- Uygulama kodu, migration'lar, Edge Function kodu veya proje ayarlarinda degisiklik uygulanmadi; yalniz bu rapor guncellendi.
