### 1) Optimization Summary

Audit date: 2026-04-27

Current optimization health: medium risk. The database is not large right now; Supabase table counts are small (`exercises` 123 rows, `exercise_logs` 94, `workout_logs` 37, `profiles` 4). The visible lag is more likely caused by app-side work during screen transitions: eager ViewModel initialization, background sync calls, broad Compose state emissions, N+1 remote queries, and expensive UI drawing while `AnimatedContent` is transitioning between tabs.

Top 3 highest-impact improvements:

1. Stop tab changes from triggering immediate remote sync/RPC work, especially Workout, Program, Discover, and Dashboard challenge calls.
2. Replace N+1 Supabase sync/query paths with batched `in (...)` / RPC reads and add the indexes/RLS policy fixes reported by Supabase advisors.
3. Split hot UI state so timers, draft inputs, and dashboard/challenge data do not recompose large screens every second or during unrelated changes.

Biggest risk if no changes are made: the app will feel increasingly slower as users accumulate workouts/programs, because current latency comes from repeated remote queries and full-screen recomposition patterns, not from the current tiny DB size alone.

### 2) Findings (Prioritized)

## Finding 1: Tab transitions trigger remote refresh and compose two heavy screens at once

* **Title**: Tab transitions trigger remote refresh and compose two heavy screens at once
* **Category**: Frontend / Network / DB
* **Severity**: High
* **Impact**: Improves page-transition latency, UI thread smoothness, DB/API load, battery.
* **Evidence**: `app/src/main/java/com/avonix/profitness/presentation/dashboard/DashboardScreen.kt` uses `AnimatedContent` for tab transitions and calls `workoutViewModel.refresh()` / `forceRefresh()` from `LaunchedEffect(selectedTab)` when returning to Workout. `WorkoutScreen.kt` also refreshes on lifecycle resume. `WorkoutViewModel.refresh()` calls both workout and program remote sync.
* **Why it’s inefficient**: `AnimatedContent` can keep old and new screens composed during animation. At the same time, entering or resuming a tab starts remote sync work, Room flow emissions, and recompositions. This makes navigation compete with network/DB work on exactly the frame window where smoothness matters most.
* **Recommended fix**: Debounce/throttle tab refreshes, move sync to a background app-level coordinator, and use stale-while-revalidate: show local Room data immediately, then sync after the transition settles. Avoid `forceRefresh()` on ordinary tab return unless a write happened.
* **Tradeoffs / Risks**: Data may appear stale for a short period unless the UI shows subtle syncing state. Requires defining freshness windows per screen.
* **Expected impact estimate**: High; likely visible reduction in tab-transition jank, especially on older Android devices.
* **Removal Safety**: Needs Verification
* **Reuse Scope**: service-wide

## Finding 2: Program screen eagerly creates DiscoverViewModel and loads Discover feed

* **Title**: Program screen eagerly creates DiscoverViewModel and loads Discover feed
* **Category**: Frontend / Network / Cost
* **Severity**: High
* **Impact**: Reduces unnecessary RPC calls, startup work, memory, and recompositions when opening Program.
* **Evidence**: `app/src/main/java/com/avonix/profitness/presentation/program/ProgramBuilderScreen.kt` creates `DiscoverViewModel` and collects its state near the top of the Program screen. `DiscoverViewModel.init` immediately calls `loadFirstPage()` and `loadMyShared()`, which hit Discover RPC/feed paths.
* **Why it’s inefficient**: Opening Program should not automatically fetch the Discover feed. This couples two expensive feature areas and makes Program tab transitions pay for social/feed work even if the user never shares or browses public programs.
* **Recommended fix**: Extract share-only operations into a small `ProgramShareViewModel` or lazy-create `DiscoverViewModel` only when the share sheet opens. Keep feed loading inside Discover tab.
* **Tradeoffs / Risks**: Requires moving share result events out of Discover state or adding a lightweight shared use case.
* **Expected impact estimate**: High for Program tab entry; removes at least two avoidable network calls and associated state updates.
* **Removal Safety**: Likely Safe
* **Reuse Scope**: module

## Finding 3: Remote sync uses N+1 queries for programs and workout logs

* **Title**: Remote sync uses N+1 queries for programs and workout logs
* **Category**: Network / DB / Algorithm
* **Severity**: High
* **Impact**: Improves sync latency, throughput, battery, and Supabase request cost.
* **Evidence**: `app/src/main/java/com/avonix/profitness/data/sync/SyncManager.kt` fetches all programs, then for each program queries `program_days`, then for each day queries `program_exercises`. It also fetches workout logs, then queries `exercise_logs` once per workout log.
* **Why it’s inefficient**: Network round trips scale with entity count. A user with 20 programs and 100 workouts can generate hundreds of Supabase requests from one sync path.
* **Recommended fix**: Batch by IDs: fetch programs once, days with `program_id IN (...)`, exercises with `program_day_id IN (...)`, workout exercise logs with `workout_log_id IN (...)`. Consider one RPC returning nested program/workout payloads if PostgREST client ergonomics are weak.
* **Tradeoffs / Risks**: Batched reads need careful payload mapping and should preserve local deletion/upsert semantics.
* **Expected impact estimate**: High; can reduce sync request count from O(programs + days + workouts) to O(1) to O(4) per sync type.
* **Removal Safety**: Safe
* **Reuse Scope**: service-wide

## Finding 4: Profile weekly completion ratios perform per-log DB queries

* **Title**: Profile weekly completion ratios perform per-log DB queries
* **Category**: DB / Algorithm / Reuse Opportunity
* **Severity**: High
* **Impact**: Improves Profile screen load time, Supabase DB load, and mobile network usage.
* **Evidence**: `app/src/main/java/com/avonix/profitness/data/profile/ProfileRepositoryImpl.kt` `getWeeklyCompletionRatios()` fetches weekly workout logs, then inside a loop queries `program_exercises` and `exercise_logs` per log.
* **Why it’s inefficient**: This is an N+1 query pattern on a screen that is commonly opened. It scales with workout count and duplicates logic that could be aggregated once.
* **Recommended fix**: Replace the loop with one RPC or two batched queries: fetch all weekly logs, all exercises for involved `program_day_id`s, and all exercise logs for involved `workout_log_id`s. Compute ratios from grouped maps. Prefer a SQL function if this is purely aggregate data.
* **Tradeoffs / Risks**: RPC aggregation moves logic to DB and needs tests for partial workouts and rest days.
* **Expected impact estimate**: High for active users; likely turns many network calls into one or two.
* **Removal Safety**: Safe
* **Reuse Scope**: module

## Finding 5: Profile loads overlapping full-history workout data

* **Title**: Profile loads overlapping full-history workout data
* **Category**: DB / Memory / Algorithm
* **Severity**: High
* **Impact**: Reduces Profile load latency, memory, and repeated parsing/serialization.
* **Evidence**: `ProfileViewModel.loadProfile()` launches parallel calls for profile, stats, ratios, workout dates, achievements, unlocked achievements, and local streak. `ProfileRepositoryImpl.getUserStats()` queries all user workout logs to calculate dates/streak, while `getWorkoutDates()` separately queries workout dates again.
* **Why it’s inefficient**: Parallelism hides some latency but duplicates work. As history grows, full-history scans and JSON decoding become the dominant Profile load cost.
* **Recommended fix**: Introduce a single `ProfileDashboardSnapshot` query/RPC that returns stats, workout dates needed for the calendar, weekly ratios, and streak inputs. Add limits/windows where full history is not required. Cache by user and invalidation token after workout writes.
* **Tradeoffs / Risks**: Snapshot APIs can become too broad; keep the contract versioned and focused on Profile needs.
* **Expected impact estimate**: Medium to High; strongest for users with long workout history.
* **Removal Safety**: Needs Verification
* **Reuse Scope**: module

## Finding 6: Supabase advisors report missing FK indexes, slow RLS policies, duplicate policies, and duplicate indexes

* **Title**: Supabase advisors report missing FK indexes, slow RLS policies, duplicate policies, and duplicate indexes
* **Category**: DB / Scalability / Cost
* **Severity**: High
* **Impact**: Improves query planning, RLS evaluation cost, write/delete performance, and index maintenance overhead.
* **Evidence**: Supabase performance advisors reported `unindexed_foreign_keys` on many FK columns including `exercise_logs.workout_log_id`, `program_days.program_id`, `program_exercises.program_day_id`, `workout_logs.user_id`, and social/challenge FKs. Advisors also reported `auth_rls_initplan` for policies using `auth.uid()` directly, `multiple_permissive_policies`, and duplicate indexes on `group_participants`, `profiles`, and `user_follows`.
* **Why it’s inefficient**: Missing FK indexes make joins, deletes, and filtered reads slower as tables grow. Direct `auth.uid()` in RLS can be re-evaluated per row instead of once. Duplicate indexes increase write cost and storage without improving reads.
* **Recommended fix**: Add indexes for FK columns that are used in joins/filters. Rewrite RLS predicates from `auth.uid()` to `(select auth.uid())` where appropriate. Merge overlapping permissive policies by action/role. Drop duplicate indexes only after verifying names, constraints, and query usage.
* **Tradeoffs / Risks**: Indexes speed reads but add write/storage cost. Dropping duplicate indexes is safe only after confirming no constraint dependency and no active query-plan reliance.
* **Expected impact estimate**: High as data grows; low to medium immediately because current row counts are small.
* **Removal Safety**: Needs Verification
* **Reuse Scope**: service-wide

## Finding 7: Workout rest timer emits whole screen state and updates notification every second

* **Title**: Workout rest timer emits whole screen state and updates notification every second
* **Category**: Frontend / CPU / I/O
* **Severity**: Medium
* **Impact**: Reduces recompositions, main-thread work, and notification update overhead during workouts.
* **Evidence**: `app/src/main/java/com/avonix/profitness/presentation/workout/WorkoutViewModel.kt` updates `WorkoutScreenState` every second while rest timer is active and calls `notificationManager.updateRestTimer()` every tick. `WorkoutScreen.kt` collects the full UI state.
* **Why it’s inefficient**: A one-second timer should not invalidate large workout screen state, exercise cards, dashboard data, and derived UI. Notification updates every second can also be expensive depending on Android version/device.
* **Recommended fix**: Split timer state into a separate `StateFlow<RestTimerState>` collected only by timer UI. Throttle notification updates or only update when visible/foreground constraints require it. Use `derivedStateOf` for timer text.
* **Tradeoffs / Risks**: Need to preserve notification accuracy and workout lifecycle behavior.
* **Expected impact estimate**: Medium; high during active rest timers on lower-end devices.
* **Removal Safety**: Safe
* **Reuse Scope**: module

## Finding 8: Heavy Compose drawing and image cards are used in hot lists/transitions

* **Title**: Heavy Compose drawing and image cards are used in hot lists/transitions
* **Category**: Frontend / CPU / Memory
* **Severity**: Medium
* **Impact**: Improves scroll FPS, tab animation smoothness, GPU overdraw, and memory churn.
* **Evidence**: `CinematicExerciseCard.kt`, `GlassPanel.kt`, `PageAccentBloom.kt`, and `DashboardScreen.kt` use gradients, shadows, `drawWithCache`, `animateContentSize`, large cards, and remote images. Workout cards are displayed inside `LazyColumn` while the screen also collects broad state.
* **Why it’s inefficient**: Rich drawing is acceptable in small doses, but it becomes costly when list items recompose due to timer/draft/state updates or during `AnimatedContent` transitions.
* **Recommended fix**: Keep decorative backgrounds static per screen, reduce shadow/blur/gradient layers inside lazy list items, remember image requests with explicit size, disable crossfade in hot lists, and make exercise card inputs stable. Profile with Compose recomposition counts before removing visual polish.
* **Tradeoffs / Risks**: Visual design may become flatter if over-applied; optimize only the cards and backgrounds observed in hot paths.
* **Expected impact estimate**: Medium; visible FPS improvement if recomposition hotspots are confirmed.
* **Removal Safety**: Needs Verification
* **Reuse Scope**: module

## Finding 9: State collection is not consistently lifecycle-aware

* **Title**: State collection is not consistently lifecycle-aware
* **Category**: Frontend / Memory / Reliability
* **Severity**: Medium
* **Impact**: Reduces background collection, off-screen recompositions, and wasted work.
* **Evidence**: `DashboardScreen.kt`, `WorkoutScreen.kt`, and `NewsScreen.kt` use plain `collectAsState()` in several places. Other files already use `collectAsStateWithLifecycle()`, so the project has the dependency and pattern available.
* **Why it’s inefficient**: Plain collection can continue work when lifecycle state is not appropriate for UI updates. In a tabbed app, this increases the chance that hidden or transitioning screens still process state emissions.
* **Recommended fix**: Standardize screen-level flow collection on `collectAsStateWithLifecycle()`. For always-on global state such as the rest timer, collect only the minimal state needed by the visible component.
* **Tradeoffs / Risks**: Some flows may intentionally be active off-screen; document those exceptions.
* **Expected impact estimate**: Medium; strongest when combined with timer/state splitting.
* **Removal Safety**: Safe
* **Reuse Scope**: service-wide

## Finding 10: AI chat sends full conversation history without a cap and lacks explicit timeout/retry policy

* **Title**: AI chat sends full conversation history without a cap and lacks explicit timeout/retry policy
* **Category**: Network / Cost / Reliability
* **Severity**: Medium
* **Impact**: Reduces API latency, token/cost growth, memory, and hanging request risk.
* **Evidence**: `app/src/main/java/com/avonix/profitness/presentation/aicoach/AICoachViewModel.kt` stores conversation history in a mutable list and sends `conversationHistory.toList()` to Gemini. `GeminiRepositoryImpl.kt` maps the full history into request contents. `AppModule.kt` provides a Ktor Android `HttpClient` without explicit `HttpTimeout` or bounded retry/backoff policy.
* **Why it’s inefficient**: Each message increases future request size. Long sessions become slower and more expensive. Missing request timeout/retry policy can leave poor mobile network behavior to defaults.
* **Recommended fix**: Keep a rolling message window plus optional summary. Add Ktor `HttpTimeout` and a small retry policy with jitter for transient failures. Add request-size metrics.
* **Tradeoffs / Risks**: Summarization may lose nuance unless tested with representative coaching conversations.
* **Expected impact estimate**: Medium; grows with session length.
* **Removal Safety**: Safe
* **Reuse Scope**: module

## Finding 11: News feature is removed from navigation but still present as dead/passive code

* **Title**: News feature is removed from navigation but still present as dead/passive code
* **Category**: Build / Maintainability / Dead Code
* **Severity**: Medium
* **Impact**: Reduces maintenance surface, build time, binary size, translation load, and future accidental background work risk.
* **Evidence**: `DashboardScreen.kt` no longer exposes a News tab, but `NewsScreen.kt`, `NewsViewModel.kt`, `NewsRepository.kt`, news strings, and Profile notification UI remnants still exist. `ProfileScreen.kt` still has a `newsAlerts` toggle and combines it into notification permission handling.
* **Why it’s inefficient**: The feature is not currently causing screen-transition lag because it is not mounted in the main tab UI. However, keeping a complete removed feature increases code size, maintenance cost, and accidental reactivation risk.
* **Recommended fix**: If product decision is still uncertain, put News behind an explicit compile/runtime feature flag and remove Profile notification remnants now. If decision is final, delete News screen, ViewModel, repository, DI bindings/imports, strings, icons, and tests together.
* **Tradeoffs / Risks**: Full removal loses a ready-to-reenable feature. Feature-flagging keeps some build/maintenance cost.
* **Expected impact estimate**: Low runtime impact today, medium maintainability/build impact.
* **Removal Safety**: Needs Verification
* **Reuse Scope**: module

## Finding 12: Local Room queries for programs lack supporting indexes and use broad SELECT patterns

* **Title**: Local Room queries for programs lack supporting indexes and use broad SELECT patterns
* **Category**: DB / I/O
* **Severity**: Medium
* **Impact**: Improves local query latency as offline data grows.
* **Evidence**: `ProgramDao.kt` frequently queries `programs` by `user_id`, `is_active`, and `created_at`, but `ProgramEntity.kt` has no local Room indexes. Several DAO queries use `SELECT *` where only subsets are needed.
* **Why it’s inefficient**: Room will scan more local rows than necessary for active/current program queries. Wide selects decode unused columns and increase invalidation work.
* **Recommended fix**: Add Room indexes such as `(user_id, is_active)` and `(user_id, created_at)` to `ProgramEntity`. Introduce projection queries for lightweight lists if program payload grows.
* **Tradeoffs / Risks**: Requires Room schema migration and migration tests.
* **Expected impact estimate**: Medium later; low today because local data is small.
* **Removal Safety**: Safe
* **Reuse Scope**: module

## Finding 13: Achievement unlock/check paths duplicate work and emit multiple state updates

* **Title**: Achievement unlock/check paths duplicate work and emit multiple state updates
* **Category**: DB / Reuse Opportunity
* **Severity**: Medium
* **Impact**: Reduces DB calls, event churn, and UI recompositions after workouts/profile loads.
* **Evidence**: `WorkoutViewModel.checkAndUnlockAchievements()` fetches all achievements and unlocked keys, then calls unlock per achievement. `ProfileViewModel.checkAchievements()` also filters pending achievements and calls `profileRepository.unlockAchievement()` per item, updating state after each unlock. Repository unlock performs lookup/existence checks per achievement.
* **Why it’s inefficient**: Unlocking multiple achievements creates repeated DB calls and repeated UI/event emissions. Logic is split across Workout and Profile paths.
* **Recommended fix**: Create one achievement service/use case with `unlockAchievements(userId, keys)` that performs one batched lookup/existence check and one state update/event batch.
* **Tradeoffs / Risks**: Must preserve exact achievement notification behavior and idempotency.
* **Expected impact estimate**: Medium after active workouts; also reduces bug surface.
* **Removal Safety**: Safe
* **Reuse Scope**: service-wide

## Finding 14: Very large composable/ViewModel files make optimization harder and increase recomposition blast radius

* **Title**: Very large composable/ViewModel files make optimization harder and increase recomposition blast radius
* **Category**: Frontend / Maintainability / Over-Abstracted Code
* **Severity**: Medium
* **Impact**: Improves maintainability, testability, and ability to isolate performance hotspots.
* **Evidence**: Large files include `ProgramBuilderScreen.kt` (~2400 lines), `ChallengesScreen.kt` (~2100 lines), `ProfileScreen.kt` (~1900 lines), `NewsScreen.kt` (~1500 lines), `AICoachScreen.kt` (~1000 lines), and `WorkoutScreen.kt` (~1000 lines).
* **Why it’s inefficient**: Monolithic screens tend to share broad state, collect more than they need, and make local recomposition optimization difficult. They also increase review cost and drift risk.
* **Recommended fix**: Split by user workflow and state owner: top-level route, pure UI sections, dialogs/sheets, and small state adapters. Prefer passing stable UI models into leaf composables instead of entire screen state.
* **Tradeoffs / Risks**: Refactors can create churn; do it only around active performance work and keep behavior unchanged.
* **Expected impact estimate**: Medium indirect impact; enables faster future optimization.
* **Removal Safety**: Needs Verification
* **Reuse Scope**: module

## Finding 15: Disk cache has no size/TTL bounds and prefix invalidation scans files

* **Title**: Disk cache has no size/TTL bounds and prefix invalidation scans files
* **Category**: Caching / I/O / Reliability
* **Severity**: Low
* **Impact**: Reduces long-term disk growth risk and worst-case cache invalidation cost.
* **Evidence**: `app/src/main/java/com/avonix/profitness/data/cache/DiskCache.kt` reads/writes whole JSON files, has no visible global max size/TTL enforcement, and `removeByPrefix()` scans files in the cache directory.
* **Why it’s inefficient**: This is fine for small cache counts, but stale data and prefix scans can become slow or consume storage if more features use the helper.
* **Recommended fix**: Add per-entry metadata with TTL, max file count/bytes, and a lightweight index if cache usage grows. Keep current simple cache for small static datasets.
* **Tradeoffs / Risks**: More cache machinery can be overkill today.
* **Expected impact estimate**: Low now; preventive reliability improvement.
* **Removal Safety**: Likely Safe
* **Reuse Scope**: service-wide

### 3) Quick Wins (Do First)

* Remove or feature-flag the Program screen's eager `DiscoverViewModel`; lazy-load Discover/share state only when needed.
* Replace `collectAsState()` with `collectAsStateWithLifecycle()` in active screens, except explicitly always-on global state.
* Debounce tab-triggered `refresh()` / `forceRefresh()` and do not sync during the first frames of tab transition.
* Remove the Profile `newsAlerts` notification remnant if News remains hidden.
* Add Supabase FK indexes for the highest-use paths first: `workout_logs.user_id`, `exercise_logs.workout_log_id`, `program_days.program_id`, `program_exercises.program_day_id`, `program_exercises.exercise_id`.
* Rewrite RLS policies flagged by advisors to use `(select auth.uid())`.
* Split rest timer state from `WorkoutScreenState`.
* Add `HttpTimeout` to Gemini/Ktor client and cap AI chat history.

### 4) Deeper Optimizations (Do Next)

* Redesign `SyncManager` around batched pulls or RPC snapshots for programs, workout logs, and exercise logs.
* Build a `ProfileDashboardSnapshot` endpoint/query that returns stats, streak, workout dates, and weekly ratios without duplicate full-history scans.
* Consolidate achievement unlock logic into one idempotent batch use case.
* Introduce an app-level sync scheduler with freshness windows, backoff, and user-action invalidation instead of screen-entry sync.
* Refactor large Compose screens incrementally around hot paths: Workout, Program, Profile, Discover/Challenges.
* Add Compose compiler metrics and runtime recomposition tracing to catch unstable models and over-broad state collection.
* Review duplicate and unused Supabase indexes after query metrics are available, then drop only verified duplicates.

### 5) Validation Plan

Benchmarks:

* Measure tab transition frame times with Android Studio Profiler or Macrobenchmark: Workout -> Program, Program -> Workout, Program -> Discover, Profile -> Workout.
* Add a synthetic seeded user with 50 programs, 500 workout logs, and 5,000 exercise logs to compare sync/profile behavior before and after batching.
* Time `SyncManager.pullPrograms()`, `pullWorkoutLogs()`, `ProfileRepositoryImpl.getWeeklyCompletionRatios()`, and `ProfileViewModel.loadProfile()` with structured logs.

Profiling strategy:

* Enable Compose recomposition counts for Workout, Program, Profile, and Dashboard timer components.
* Use Network Inspector to count requests per tab open and per sync.
* Use Supabase query logs and `EXPLAIN` for high-use program/workout/profile queries.
* Compare CPU main-thread time during `AnimatedContent` transitions before/after refresh throttling.

Metrics to compare before/after:

* P50/P95 tab transition duration and dropped frames.
* Number of Supabase requests on cold app start, Program tab open, Workout tab return, Profile open.
* Profile screen time-to-content.
* Sync request count and total sync duration.
* Compose recompositions per second during active rest timer.
* Gemini request payload size and latency by conversation length.
* Local DB query duration for active program and workout history reads.

Test cases:

* User with no data, small data, and large seeded data.
* Offline app start, then reconnect and sync.
* Program create/edit/delete followed by Workout tab return.
* Workout completion that unlocks multiple achievements.
* Long AI chat session with old messages summarized/windowed.
* News feature disabled/hidden: verify no News UI, notification toggle, ViewModel, or repository call is reachable.

### 6) Optimized Code / Patch (when possible)

No production code changes were applied. The snippets below are recommended implementation shapes only.

## Batch program sync shape

```kotlin
// Instead of: programs -> per program days -> per day exercises
val programIds = programs.map { it.id }
val days = supabase.from("program_days")
    .select {
        filter { isIn("program_id", programIds) }
    }
    .decodeList<ProgramDayDto>()

val dayIds = days.map { it.id }
val exercises = supabase.from("program_exercises")
    .select {
        filter { isIn("program_day_id", dayIds) }
    }
    .decodeList<ProgramExerciseDto>()

val daysByProgram = days.groupBy { it.programId }
val exercisesByDay = exercises.groupBy { it.programDayId }
```

What changes: request count becomes constant per sync phase instead of growing with each program/day.

## Lazy Discover/share state in Program

```kotlin
var shareSheetOpen by rememberSaveable { mutableStateOf(false) }

if (shareSheetOpen) {
    val shareViewModel: ProgramShareViewModel = hiltViewModel()
    val shareState by shareViewModel.state.collectAsStateWithLifecycle()
    ShareProgramSheet(
        state = shareState,
        onDismiss = { shareSheetOpen = false },
        onShare = shareViewModel::shareProgram
    )
}
```

What changes: Program tab no longer initializes Discover feed or my-shared-programs RPCs before the user opens sharing.

## Split rest timer state

```kotlin
data class RestTimerState(
    val isActive: Boolean = false,
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 0
)

private val _restTimer = MutableStateFlow(RestTimerState())
val restTimer: StateFlow<RestTimerState> = _restTimer.asStateFlow()
```

What changes: one-second timer ticks update only timer UI, not the entire workout screen state.

## Supabase RLS pattern

```sql
-- Prefer this pattern in policies flagged by Supabase advisors:
using (user_id = (select auth.uid()))

-- Instead of:
using (user_id = auth.uid())
```

What changes: Postgres can evaluate the auth function once through an init plan instead of per row.

## High-use index examples

```sql
create index concurrently if not exists workout_logs_user_id_idx
on public.workout_logs (user_id);

create index concurrently if not exists exercise_logs_workout_log_id_idx
on public.exercise_logs (workout_log_id);

create index concurrently if not exists program_days_program_id_idx
on public.program_days (program_id);

create index concurrently if not exists program_exercises_program_day_id_idx
on public.program_exercises (program_day_id);
```

What changes: joins, deletes, and filtered reads on these relationships stop relying on table scans as data grows.

## AI history window and timeout shape

```kotlin
private const val MAX_CONTEXT_MESSAGES = 12

val context = conversationHistory
    .takeLast(MAX_CONTEXT_MESSAGES)
    .toList()
```

```kotlin
install(HttpTimeout) {
    requestTimeoutMillis = 30_000
    connectTimeoutMillis = 10_000
    socketTimeoutMillis = 30_000
}
```

What changes: request size and latency stay bounded, and poor network behavior fails predictably.
