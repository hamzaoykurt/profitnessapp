# Profitness Android + Supabase Altyapi Denetimi

Tarih: 2026-05-13
Kapsam: Android uygulama kodu, Room modeli/migration zinciri, Supabase migration'lari, Edge Function'lar, canli Supabase metadata okumalari.
Kisit: Kotlin, SQL ve proje konfigurasyonunda refactor uygulanmadi; yalnizca rapor uretildi. Secret, token ve env degerleri rapora yazilmadi.

## Architectural Health Score

Genel skor: **62 / 100**

| Pilar | Puan | Degerlendirme |
| --- | ---: | --- |
| Android mimari | 21 / 35 | Hilt, Room, ViewModel/StateFlow ve repository yapisi var; ancak Supabase SDK presentation katmanina sizmis, bazi ekranlar/VM'ler kurumsal bakim sinirinin cok uzerinde. |
| Supabase/Postgres guvenlik ve performans | 25 / 35 | RLS yaygin, billing/AI service role kullanimi server tarafinda; fakat RPC execute yuzeyi fazla genis, public/private wrapper modeli sertlestirilmeli, webhook dogrulamasi olgun degil. |
| Offline/sync/error handling | 13 / 20 | Room-first yazim, dirty/synced alanlari ve delta pull yonunde iyi adimlar var; conflict modeli, retry/backoff, idempotent sync state ve domain-safe hata modeli eksik. |
| Test/observability/operasyonel olgunluk | 3 / 10 | Compile smoke calisiyor ama test kaynagi yok; analytics NoOp, crash/perf observability ve Supabase advisor otomasyonu yok. |

Stack suitability: Kotlin + Compose + Hilt + Room + Supabase bu urun icin uygun; mevcut risk teknik secimden degil, sinirlarin buyurken korunmamasindan geliyor. Mimariyi kurumsal seviyeye tasimak icin ana is, yeni teknoloji eklemek degil: SDK sinirlarini data layer'a hapsetmek, sync'i deterministik hale getirmek, RPC izinlerini daraltmak ve test/observability temellerini koymak.

Risk heatmap:

| Alan | Risk | Not |
| --- | --- | --- |
| Presentation katmani | High | `SupabaseClient` birden cok ViewModel/Screen icinde dogrudan kullaniliyor. |
| Public RPC yuzeyi | High | Canli metadata: 69 public function icinde 8 anon executable; 42 private function icinde 8 anon executable. |
| Offline sync | High | Yerel dirty queue var ama conflict/retry/partial failure protokolu urun standardinda degil. |
| Edge billing/AI | Medium | Service role sadece Edge/server tarafinda; olumlu. Webhook imzasi/timestamp ve audit modeli zayif. |
| Test/observability | High | Test dosyasi bulunmadi; `:app:testDebugUnitTest` `NO-SOURCE`. |

## Structural Findings

### 1. Presentation katmanina Supabase SDK siziyor

Pillar: Android mimari
Severity: High
Evidence: `AppModule.kt` global `SupabaseClient` sagliyor (`app/src/main/java/com/avonix/profitness/di/AppModule.kt:122`), sonra presentation siniflari bunu dogrudan aliyor: `WorkoutViewModel.kt:125`, `AICoachViewModel.kt:78`, `ProgramViewModel.kt:85`, `WeightTrackingViewModel.kt:83`, `ProfileViewModel.kt:104`, `ExerciseProgressionScreen.kt:80`. `WorkoutViewModel` icinde session erisimi cok sayida methoda dagilmis (`WorkoutViewModel.kt:590`, `WorkoutViewModel.kt:660`, `WorkoutViewModel.kt:1197`, `WorkoutViewModel.kt:1964`).

Violation: ViewModel'ler domain/use-case kontrati yerine Supabase auth/session detayini biliyor. Bu, UI state ve repository sorumlulugunu karistiriyor.

Why it Matters: Auth lifecycle degisimi, token refresh, RLS hata davranisi veya SDK upgrade'i presentation katmanini kirar. Testlerde Supabase mock'lamak zorlasir; offline-first Room akisi ile remote session akisi ayni yerde birbirine baglanir.

Senior Fix:

```kotlin
interface CurrentUserProvider {
    suspend fun requireUserId(): UserId
}

class SupabaseCurrentUserProvider @Inject constructor(
    private val supabase: SupabaseClient
) : CurrentUserProvider {
    override suspend fun requireUserId(): UserId =
        supabase.auth.currentSessionOrNull()?.user?.id
            ?.let(::UserId)
            ?: throw AuthFailure.NotAuthenticated
}

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val syncWorkout: SyncWorkoutUseCase,
    private val currentUser: CurrentUserProvider
) : ViewModel()
```

Refactor Effort: Medium. Once `CurrentUserProvider` and use-case boundaries exist, migration can be feature-by-feature.

### 2. Buyuk ekran/VM dosyalari bakim sinirini asmis

Pillar: Android mimari
Severity: Medium-High
Evidence: En buyuk dosyalar: `ProgramBuilderScreen.kt` 3098 satir, `ChallengeDetailOverlay.kt` 2318 satir, `ProfileScreen.kt` 2308 satir, `WorkoutViewModel.kt` 2014 satir, `StoreScreen.kt` 1973 satir. `ProgramBuilderScreen.kt:423` ana composable icinde navigation, hilt VM alma, context ve lifecycle gibi farkli sorumluluklari topluyor.

Violation: Feature UI, interaction state, side-effect ve domain orchestration tek dosyalarda birikmis.

Why it Matters: Her degisiklik genis recomposition yuzeyi, merge conflict ve regresyon riski dogurur. Kurumsal urunde feature sahipligi, snapshot testleri ve design review icin dosya sinirlari daha ince olmali.

Senior Fix:

```kotlin
@Composable
fun ProgramBuilderRoute(
    viewModel: ProgramViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProgramBuilderScreen(
        state = state,
        actions = ProgramBuilderActions.from(viewModel),
        onBack = onBack
    )
}
```

Refactor Effort: High. Ilk fazda route/screen/state/actions ayrimi, ikinci fazda sheet ve section dosyalarina bolme onerilir.

### 3. `Result<T>` ve `runCatching` domain-safe hata modeli icin yetersiz

Pillar: Offline/sync/error handling
Severity: Medium
Evidence: Repository kontratlari yaygin olarak `Result<T>` donuyor (`ChallengeRepository.kt:14`, `DiscoverRepository.kt:10`, `AuthRepository.kt:4`). Implementasyonlarda `runCatching` genis yakalama seklinde kullaniliyor (`ChallengeRepositoryImpl.kt:33`, `AuthRepositoryImpl.kt:26`, `GeminiRepositoryImpl.kt:41`). UI tarafinda string parse veya generic hata kullanimi var (`AuthViewModel.kt:147`, `AuthViewModel.kt:265`).

Violation: Domain hatalari tipli degil; network, auth, validation, RLS/permission ve conflict ayni kanaldan akiyor.

Why it Matters: UI state loading/error/data tutarliligini bozabilir. Retriable/non-retriable ayrimi, telemetry ve kullaniciya dogru mesaj verme zorlasir.

Senior Fix:

```kotlin
sealed interface AppFailure {
    data object NotAuthenticated : AppFailure
    data object Offline : AppFailure
    data class PermissionDenied(val operation: String) : AppFailure
    data class Validation(val field: String, val reason: String) : AppFailure
    data class Remote(val code: String, val retryable: Boolean) : AppFailure
}

typealias AppResult<T> = Either<AppFailure, T>
```

Refactor Effort: Medium. Once error mapper is added at repository boundary, ViewModel state can become deterministic.

### 4. Room-first sync dogru yonde, fakat conflict ve partial failure modeli eksik

Pillar: Offline/sync/error handling
Severity: High
Evidence: Sync merkezi var ve Mutex ile sirali calisiyor (`SyncManager.kt:83`, `SyncManager.kt:94`, `SyncManager.kt:104`). Delta pull baslamis (`SyncManager.kt:238`-`SyncManager.kt:255`). Dirty set kayitlari push ediliyor ve basarili upsert sonrasi lokal temizleniyor (`SyncManager.kt:307`-`SyncManager.kt:340`). Ancak workout push'ta batch upsert sonrasi her satir synced isaretleniyor (`SyncManager.kt:280`-`SyncManager.kt:300`), per-row sonuc/audit yok. `SetCompletionEntity` default'lari migration default'lariyla semantik olarak karisik: DB default `synced=1`, entity default `synced=false`; DB default `dirty=0`, entity default `dirty=true` (`AppDatabase.kt:227`-`AppDatabase.kt:230`, `SetCompletionEntity.kt:27`-`SetCompletionEntity.kt:30`).

Violation: Sync queue state machine acik bir kontrat degil; conflict policy, retry/backoff ve remote tombstone protokolu tam tanimli degil.

Why it Matters: Mobil offline kullanimda network kesintisi, cift cihaz, saat farki ve kismi remote basari durumlari veri kaybi veya sessiz overwrite uretebilir.

Senior Fix:

```kotlin
enum class SyncState { Clean, Dirty, Syncing, Failed, Deleted }

data class SyncEnvelope<T>(
    val entity: T,
    val mutationId: String,
    val state: SyncState,
    val lastErrorCode: String? = null,
    val retryAfterMs: Long? = null
)
```

SQL tarafinda:

```sql
alter table public.set_completions
  add column if not exists client_mutation_id uuid,
  add column if not exists deleted_at timestamptz;

create unique index if not exists set_completions_mutation_id_idx
  on public.set_completions (user_id, client_mutation_id)
  where client_mutation_id is not null;
```

Refactor Effort: High. En guvenli yol once set_completions icin queue protokolunu netlestirmek, sonra workout/weight tarafina yaymak.

### 5. Room schema export ve migration test altyapisi yok

Pillar: Test/observability/operasyonel olgunluk
Severity: Medium
Evidence: Room `version = 11`, `exportSchema = false` (`AppDatabase.kt:21`-`AppDatabase.kt:34`). Migration zinciri AppModule'de explicit eklenmis (`AppModule.kt:97`-`AppModule.kt:112`), bu iyi. Ancak test kaynak klasorlerinde test dosyasi bulunmadi; `:app:testDebugUnitTest` basarili ama `NO-SOURCE`.

Violation: Migration'lar derleniyor ama eski DB snapshot'larindan guncel schema'ya dogrulama yok.

Why it Matters: Offline-first urunde Room migration hatasi kullanicinin lokal verisini kaybettirebilir. `exportSchema=false` bu regresyonlari PR'da yakalamayi zorlastirir.

Senior Fix:

```kotlin
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test fun migrate10To11_preservesDirtySetCompletions() {
        helper.createDatabase(AppDatabase.NAME, 10).apply {
            execSQL("insert into set_completions (...) values (...)")
            close()
        }
        helper.runMigrationsAndValidate(
            AppDatabase.NAME,
            11,
            true,
            AppDatabase.MIGRATION_10_11
        )
    }
}
```

Refactor Effort: Medium. Schema export acildiktan sonra 10->11 ve kritik offline tablolarla baslamak yeterli.

### 6. RPC execute yuzeyi gereksiz genis

Pillar: Supabase/Postgres guvenlik ve performans
Severity: High
Evidence: Canli metadata okumasinda 69 public function icinde 8 `anon` executable, 42 private function icinde 8 `anon` executable goruldu. Anon executable listesinde `create_event_challenge`, `invite_friends_to_challenge`, `list_visible_challenges`, `list_user_shared_programs` ve `list_user_created_challenges` gibi fonksiyon aileleri var. Migration'larda public wrapper modeli goruluyor (`20260508090000_sport_aware_activity_challenges.sql:463`, `20260508090000_sport_aware_activity_challenges.sql:508`, `20260508204000_challenge_invite_visibility.sql:130`). Birden cok security definer function private schema'da tutulmus ve search_path belirlenmis; bu iyi bir pratik (`20260508090000_sport_aware_activity_challenges.sql:101`, `20260508195000_social_challenge_invites.sql:56`, `20260508204000_challenge_invite_visibility.sql:4`).

Violation: Public RPC'ler ve hatta private function'lar execute grant acisindan least-privilege seviyesinde degil. Private schema Data API'de exposed degilse risk azalir, ama grant hygiene yine de kurumsal standart degil.

Why it Matters: Supabase/PostgREST'te function execute grant'leri saldiri yuzeyini belirler. `auth.uid()` null guard'lari iyi bir ikinci savunmadir; asil savunma gereksiz role'lerden execute'i kaldirmaktir.

Senior Fix:

```sql
revoke execute on all functions in schema private from public, anon, authenticated;
grant usage on schema private to authenticated;

revoke execute on function public.create_event_challenge(...) from anon;
grant execute on function public.create_event_challenge(...) to authenticated;
```

Refactor Effort: Medium. Once function inventory is locked, migrations can revoke by exact signature and add regression SQL checks.

### 7. RLS kapsami genel olarak iyi, fakat policy/grant tutarliligi sertlestirilmeli

Pillar: Supabase/Postgres guvenlik ve performans
Severity: Medium
Evidence: Canli `list_tables` sonucunda public tablolarda RLS enabled gorundu. Billing tablolarinda client write kapali, service role grant'leri ayrilmis (`20260430090000_billing_entitlements.sql:138`-`20260430090000_billing_entitlements.sql:160`). Set completion policy'leri owner check yapiyor (`20260513090000_optimize_set_completions_sync.sql:36`-`20260513090000_optimize_set_completions_sync.sql:59`). Buna karsin canli policy'lerde bazi eski tablolar `roles={public}` ile tanimli (`profiles`, `workout_logs`, `user_stats`, `chat_sessions` aileleri).

Violation: Policy role secimi ve yeni/eskiden gelen policy stilleri karisik. `public` rolu genelde `anon` ve `authenticated` kapsadigi icin guard dogru olsa bile okunabilirlik ve least-privilege zayiflar.

Why it Matters: Yeni migration yazan ekip uyeleri hangi yuzeyin gercekten public oldugunu anlamakta zorlanir. Advisor bulgulari kapatilsa bile audit izleri zayif kalir.

Senior Fix:

```sql
drop policy if exists "Users manage own workout logs" on public.workout_logs;
create policy workout_logs_select_own
on public.workout_logs
for select to authenticated
using ((select auth.uid()) = user_id);
```

Refactor Effort: Medium. Policy'leri tablo basina select/insert/update/delete olarak ayirmak test edilebilirligi artirir.

### 8. Billing/AI Edge Function service role modeli kabul edilebilir, webhook dogrulamasi zayif

Pillar: Supabase/Postgres guvenlik ve performans
Severity: Medium
Evidence: Shared auth helper service role client'i sadece Edge Function icinde uretiyor (`supabase/functions/_shared/auth.ts:29`-`supabase/functions/_shared/auth.ts:34`). Client tarafinda edge cagrilari bearer token ve anon key ile gidiyor (`UserPlanRepositoryImpl.kt:126`-`UserPlanRepositoryImpl.kt:148`). AI function authenticated user dogruluyor, usage reservation yapiyor ve sonra provider'a cikiyor (`ai-generate/index.ts:147`-`ai-generate/index.ts:177`). Billing checkout/status/sandbox authenticatedUser ile korunuyor (`billing-checkout/index.ts:17`-`billing-checkout/index.ts:28`, `billing-status/index.ts:13`-`billing-status/index.ts:23`, `billing-sandbox-complete/index.ts:24`-`billing-sandbox-complete/index.ts:51`). Webhook ise tek shared secret header karsilastirmasi kullaniyor (`billing-webhook/index.ts:22`-`billing-webhook/index.ts:24`) ve timestamp/signature replay modeli yok.

Violation: Service role client'in server tarafinda kalmasi dogru; webhook dogrulama ise production-grade provider signature standardinda degil.

Why it Matters: Billing webhook'lari finansal state degistirir. Tek header secret sizar veya replay edilirse `apply_paid_billing_order` cagrisi kotuye kullanilabilir; idempotency tablo PK'si iyi ama imza/timestamp eksik.

Senior Fix:

```ts
const signature = req.headers.get("provider-signature") ?? "";
const timestamp = req.headers.get("provider-timestamp") ?? "";
verifyProviderSignature(rawBody, signature, timestamp, webhookSecret);
rejectIfTimestampSkew(timestamp, 5 * 60);
```

Refactor Effort: Medium. Provider secilince native signature library veya documented HMAC formatina gecilmeli.

### 9. Redundant fetch/RPC paterni performans ve battery maliyeti uretiyor

Pillar: Supabase/Postgres guvenlik ve performans
Severity: Medium
Evidence: API loglari son 24 saatte ayni ekran akisi icinde tekrar eden `programs`, `program_days`, `program_exercises`, `set_completions`, `exercises` ve RPC cagrilari gosteriyor; raporda kimlik/path detaylari redakte edildi. Kodda `refreshWorkout` sirasiyla exercises, programs, set push, set pull, workout logs ve dates cekiyor (`SyncCoordinator.kt:35`-`SyncCoordinator.kt:52`). `SyncManager.pullSetCompletions` delta kullanabiliyor (`SyncManager.kt:238`-`SyncManager.kt:255`), fakat diger pull'lar hala daha genis fetch paterni tasiyor.

Violation: Mobile sync'te feature route basina network budget ve cache invalidation kontrati net degil.

Why it Matters: Supabase REST cagrilari pil, data ve cold-start maliyetini artirir. Ayrica ayni auth/session refresh penceresinde fazla is tetiklenirse rate limit ve UI jank riski buyur.

Senior Fix:

```kotlin
data class SyncBudget(
    val route: RouteKey,
    val maxRequests: Int,
    val ttlMs: Long,
    val allowStaleRoom: Boolean = true
)
```

SQL/RPC alternatifi:

```sql
create function public.get_workout_bootstrap(p_since timestamptz)
returns jsonb
language sql
security invoker
as $$
  select jsonb_build_object(
    'programs', ...,
    'set_completions', ...
  );
$$;
```

Refactor Effort: Medium-High. Once metrics exist, only hot routes should be consolidated.

## Database & Security Deep-Dive

RLS:

- Canli public tablolarin tamaminda RLS enabled gorundu.
- Billing/AI hassas tablolarinda client write kapali veya service role odakli gorunuyor: `user_entitlements`, `user_credit_accounts`, `credit_ledger`, `usage_counters`, `ai_usage_events`, `billing_orders`, `billing_webhook_events`.
- `set_completions` icin owner-based select/insert/update/delete policy var; son migration `auth.uid()` per-row optimizasyonunu hedefliyor (`20260513090000_optimize_set_completions_sync.sql:1`, `20260513090000_optimize_set_completions_sync.sql:36`-`20260513090000_optimize_set_completions_sync.sql:59`). Canli policy metadatasinda hala `auth.uid() = user_id` formu gorundu; migration uygulanma sirasi veya canli state yeniden kontrol edilmeli.

Public/private function ayrimi:

- Iyi: privileged body'lerin bir kismi `private` schema + `security definer` + `search_path` ile ayrilmis.
- Risk: canli grant'lerde `anon` executable public/private function'lar var. Uygulama auth'lu kullansa bile DB seviyesinde execute yuzeyi daraltilmali.
- Kritik sayilmadi: service role key Android tarafinda bulunmadi; service role client Edge Function helper icinde.

Index/FK/check:

- Iyi: `programs(user_id, is_active)`, `programs(user_id, created_at)`, challenge invite, participant, movement ve shared program index'leri var.
- Iyi: billing tarafinda idempotency icin `ai_usage_events_user_idempotency_idx` ve webhook event PK modeli var.
- Risk: `set_completions` canli index listesinde yalniz PK gorundu; repo migration'lari `set_completions_user_updated_at_idx`, `set_completions_user_exercise_date_idx`, `set_completions_user_day_date_idx` ekliyor. Canli schema ile migration state farki varsa delta pull performansi beklenenden dusuk kalir.

Redundant fetch/RPC adaylari:

- Workout bootstrap: exercises + programs + days + program_exercises + set_completions + workout_logs cagrilari tek route icinde yineleniyor.
- Discover/profile: feed, shared list, profile/stats ve avatar URL cache-busting akislari icin route-level TTL ve conditional refresh gerek.
- Challenge dashboard: `list_my_events_for_date` ve `list_my_upcoming_events` icin public wrapper'lar auth'lu kullaniliyor; execute grant ve response payload budget birlikte ele alinmali.

## Roadmap to Enterprise Maturity

### Stabilization (0-2 hafta)

1. Presentation katmanindan `SupabaseClient` cikisi icin `CurrentUserProvider` ve feature use-case arayuzlerini ekle.
2. RPC grant inventory migration'i yaz: exact signature revoke/grant, ozellikle anon executable fonksiyonlari kapat.
3. `exportSchema = true` yap, Room schema dizinini version control'a al, 10->11 migration testi ekle.
4. `set_completions` sync state icin default semantigini netlestir; entity default ve DB default uyumunu sagla.
5. Webhook icin provider signature/timestamp dogrulamasini tasarla; sandbox flag'in production ortamda kapali oldugunu deployment checklist'e al.

### Modernization (2-6 hafta)

1. `Result<T>` yerine tipli `AppFailure` modeli ve merkezi remote error mapper kullan.
2. Buyuk ekranlari `Route`, stateless `Screen`, `Section`, `Sheet`, `Actions` parcalarina bol; ilk hedefler `ProgramBuilderScreen`, `WorkoutViewModel`, `ProfileScreen`.
3. Sync queue'yu idempotent mutation id, tombstone ve retry/backoff ile genellestir.
4. Hot route'larda request budget olc; gerekirse bootstrap RPC veya view kullan, ancak view'larda `security_invoker` veya private schema tercih et.
5. Public policy'leri tablo basina role-specific select/insert/update/delete olarak normalize et.

### Observability (6-10 hafta)

1. NoOp analytics yerine privacy-safe event sink ekle; sync failure, RPC failure, billing reservation, AI quota ve migration success/failure event'lerini izle.
2. Crash reporting ve structured logs ekle; PII/secret redaction kurallari yaz.
3. Supabase advisor/log kontrolunu CI veya release checklist'e bagla.
4. Smoke test seti: auth, program CRUD, offline workout write, set completion sync, billing status, AI reservation.
5. Baseline profile ve Compose metrics zaten var; bunlari regression gate olarak raporlayan CI job ekle.

## Dogrulama Notlari

- Canli Supabase metadata okundu: public tablolar RLS enabled; function grant, policy ve index ozetleri incelendi.
- Supabase MCP oturumu yazma yetkisine sahip gorundu: `current_user=postgres`, public schema icin `CREATE/USAGE=true`, `supabase_migrations.schema_migrations` mevcut. Denetim kapsami "rapor-only" oldugu icin `apply_migration` veya DDL uygulanmadi.
- Edge Function loglari son 24 saatte bos dondu; API/Postgres loglari okunabildi ve hassas kimlik/path detaylari rapora yazilmadi.
- Supabase CLI lokal makinede bulunamadi; advisor CLI calistirilamadi.
- `.\gradlew.bat test` ilk denemede Gradle/Kotlin output hashleme hatasi ile dustu.
- `.\gradlew.bat --no-configuration-cache --no-build-cache :app:testDebugUnitTest` basarili calisti, ancak `NO-SOURCE`; repo-local unit test coverage yok.
- `app/src/test`, `app/src/androidTest` ve `baselineprofile/src/androidTest` altinda test dosyasi bulunmadi.
