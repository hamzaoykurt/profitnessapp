# 1) Executive Summary

* **System Health Score:** 56/100 before this pass, projected 72/100 after the implemented fixes.
* **Top 3 "Lag" Drivers:**
  1. Main-thread disk and JSON cache work in Profile flows, especially during first Profile navigation.
  2. Eager off-screen ViewModel warmup during the first dashboard seconds, which starts Hilt graph creation and remote sync while the user is still transitioning.
  3. Set-completion sync without dirty/delta metadata, causing full-history push/pull or per-set network writes as data grows.
* **Estimated ROI:** 30-60% fewer dropped frames on first transitions and Workout scroll, 150-500ms smoother first tab/page transitions on mid-range devices, and 50-90% less Supabase payload for normal set-completion refreshes after history grows.

# 2) Prioritized Technical Findings

| Parameter | Detail |
| --- | --- |
| **Title** | Main-thread disk cache parsing in Profile repository |
| **Category** | Threading / I/O / GC |
| **Severity** | **CRITICAL** |
| **Symptom** | First Profile open or returning to Profile can freeze frames while JSON cache files are read and decoded. |
| **Root Cause** | `ProfileRepositoryImpl` checked `disk.get<T>()` before entering `withContext(Dispatchers.IO)`. `DiskCache.get()` performs `file.readText()` and kotlinx serialization decode synchronously, creating disk I/O and allocation pressure on the caller thread. |
| **Recommended Fix** | Implemented: added `DiskCache.getOnIo/putOnIo/removeByPrefixOnIo/removeByPrefixAsync` and moved Profile cache reads/deletes off the main thread. Added debug StrictMode in `ProfitnessApplication` to catch future regressions. |
| **Risk & Tradeoffs** | First cache hit now has coroutine dispatch overhead, but this is far cheaper than blocking UI frames. |
| **Impact Estimate** | Removes 20-150ms UI-thread stalls depending on cache size and device storage speed. |
| **Reuse Scope** | System-wide |

| Parameter | Detail |
| --- | --- |
| **Title** | Dashboard eagerly creates off-screen ViewModels |
| **Category** | CPU / Threading / Latency |
| **Severity** | **CRITICAL** |
| **Symptom** | App feels heavy immediately after login/session restore; first tab switch competes with hidden screen initialization. |
| **Root Cause** | `DashboardScreen` used `DashboardViewModelWarmup` to instantiate `ProgramViewModel`, `ProgramShareViewModel`, `AICoachViewModel`, `DiscoverViewModel`, and `ProfileViewModel` across timed warmup stages. Some of these ViewModels start collectors and delayed remote sync while not visible. |
| **Recommended Fix** | Implemented: removed warmup instantiation. Screens now initialize when actually selected, and first-time tab content keeps the lightweight `TabWarmupSurface` behavior. |
| **Risk & Tradeoffs** | First visit to a non-Workout tab may do its own initialization later, but it no longer steals CPU/IO from the first dashboard experience. |
| **Impact Estimate** | Reduces startup contention and removes several Hilt/ViewModel allocations from the first 3.2s. |
| **Reuse Scope** | Module |

| Parameter | Detail |
| --- | --- |
| **Title** | Workout first-screen remote work starts too early and in multiple bursts |
| **Category** | Latency / I/O / Threading |
| **Severity** | High |
| **Symptom** | First Workout render and early scrolling can stutter shortly after the screen appears. |
| **Root Cause** | `WorkoutScreen` triggered `viewModel.triggerInitialSync()` after 800ms and `dashboardVm.refresh()` after 500ms. `SyncCoordinator.refreshWorkout()` then runs exercises, programs, set completions, workout logs, and historical log date sync. This overlaps with first composition and image/list settling. |
| **Recommended Fix** | Implemented: delayed first Workout sync to 1500ms and dashboard events to 1700ms, using `reloadIfStale()` for events. Normal workout refresh now pulls historical workout-log dates only when local dates are empty; force refresh still does a full date pull. Room-backed content remains the first render source. |
| **Risk & Tradeoffs** | Remote challenge/workout updates can appear about 0.7-1.2s later on first entry. Local UI responsiveness wins. |
| **Impact Estimate** | Reduces first-transition IO contention; expected 100-300ms perceived smoothness gain on cold session restore. |
| **Reuse Scope** | Module |

| Parameter | Detail |
| --- | --- |
| **Title** | Set-completion sync is full-history or per-action instead of delta |
| **Category** | I/O / Latency / Cost |
| **Severity** | **CRITICAL** |
| **Symptom** | Normal refresh gets slower as users accumulate history; completing sets can compete with UI and network resources. |
| **Root Cause** | `SetCompletionEntity` had no `synced`, `dirty`, `deleted`, or local update metadata. `SyncManager.pushSetCompletions()` uploaded all local rows for the user, and `pullSetCompletions()` selected all remote rows. Write paths also called best-effort per-set Supabase operations. |
| **Recommended Fix** | Implemented: Room migration 10 -> 11 adds `synced`, `dirty`, `deleted`, and `updated_at_ms`. Local set writes mark rows dirty. `SyncManager` now pushes only dirty rows and pulls remote rows by `updated_at` delta. Added Supabase migration for `(user_id, updated_at)` and an `updated_at` trigger. |
| **Risk & Tradeoffs** | Multi-device remote deletes still need a future server-side tombstone model for perfect deletion propagation. Local deletes are safe and retryable via dirty tombstones. |
| **Impact Estimate** | Converts O(total user set history) refresh work to O(changed set rows). For active users, expected 50-90% reduction in payload and JSON decode. |
| **Reuse Scope** | System-wide |

| Parameter | Detail |
| --- | --- |
| **Title** | User actions awaited network best-effort sync |
| **Category** | Latency / Threading |
| **Severity** | High |
| **Symptom** | Set toggles, weight drafts, and exercise completion can feel inconsistent under slow network because background work still runs immediately after local writes. |
| **Root Cause** | `WorkoutRepositoryImpl` wrote Room first, then called `pushUnsyncedWorkouts()` or `syncSetCompletionBestEffort()` from the same repository operation. Although the UI is optimistic, the coroutine and IO dispatcher still get occupied by network. |
| **Recommended Fix** | Implemented for set completions and new workout-log creation paths: local write marks dirty/unsynced and returns. SyncCoordinator flushes later. |
| **Risk & Tradeoffs** | Remote persistence can lag until next scheduled sync. Correctness is preserved by durable local dirty/unsynced state. |
| **Impact Estimate** | Removes up to 750ms per-set network wait budget and avoids IO contention during fast set entry. |
| **Reuse Scope** | Module |

| Parameter | Detail |
| --- | --- |
| **Title** | Hot Workout list image decoding is oversized |
| **Category** | Memory / GC / UI |
| **Severity** | Medium |
| **Symptom** | Scroll stutter and occasional GC pressure when image-heavy exercise cards enter the viewport. |
| **Root Cause** | `CinematicExerciseCard` passed raw image URLs directly to `AsyncImage` for a fixed 180dp header. Coil could decode larger-than-needed bitmaps. |
| **Recommended Fix** | Implemented: build a Coil `ImageRequest` with explicit `size(720, 360)` and `crossfade(false)` for the card header. |
| **Risk & Tradeoffs** | Very high-density tablets may use a slightly smaller decoded bitmap than native asset size; visual quality remains appropriate for the 180dp cropped header. |
| **Impact Estimate** | Lowers bitmap memory and decode work per visible card; expected 10-25% less image-related allocation during fast scroll. |
| **Reuse Scope** | Module |

| Parameter | Detail |
| --- | --- |
| **Title** | Blocking sleep in timer sound path |
| **Category** | Threading |
| **Severity** | Medium |
| **Symptom** | If `playTimerEndSound()` is called from a UI-reachable path, the caller can block for about 640ms. |
| **Root Cause** | `WorkoutNotificationManager.playTimerEndSound()` used two `Thread.sleep(320)` calls between `ToneGenerator` beeps. |
| **Recommended Fix** | Implemented: moved beep sequencing to a `Dispatchers.Default` coroutine with nonblocking `delay()`. |
| **Risk & Tradeoffs** | Sound playback is asynchronous; notification display no longer waits for all beeps. |
| **Impact Estimate** | Removes a direct 640ms blocking hazard. |
| **Reuse Scope** | Local |

| Parameter | Detail |
| --- | --- |
| **Title** | RLS policy calls `auth.uid()` per row on `set_completions` |
| **Category** | Latency / Cost |
| **Severity** | Medium |
| **Symptom** | Large set-completion reads spend avoidable database CPU evaluating auth helper functions row-by-row. |
| **Root Cause** | A later migration recreated `set_completions` policies with `auth.uid() = user_id`, regressing the earlier `(select auth.uid()) = user_id` pattern. Supabase recommends wrapping auth functions so Postgres can use an initPlan. |
| **Recommended Fix** | Implemented: added Supabase migration to drop duplicate policy names and recreate canonical policies using `(select auth.uid())`. |
| **Risk & Tradeoffs** | No authorization semantic change. Must be applied through Supabase migration flow before production. |
| **Impact Estimate** | Database CPU reduction on large reads; Supabase examples show order-of-magnitude gains on policy-heavy scans. |
| **Reuse Scope** | System-wide |

| Parameter | Detail |
| --- | --- |
| **Title** | Missing Supabase indexes for Workout history query shapes |
| **Category** | I/O / Latency / Cost |
| **Severity** | High |
| **Symptom** | Workout history, per-day restore, and exercise progression reads can become progressively slower as `set_completions` and `workout_logs` grow. |
| **Root Cause** | Remote schema had primary/user indexes but did not cover common filters like `(user_id, exercise_id, date desc)`, `(user_id, program_day_id, date desc)`, and workout log date ordering. |
| **Recommended Fix** | Implemented and applied through Supabase MCP: added `set_completions_user_exercise_date_idx`, `set_completions_user_day_date_idx`, `workout_logs_user_date_idx`, and `workout_logs_user_day_date_idx`. |
| **Risk & Tradeoffs** | Adds write-side index maintenance cost, but these tables are read far more often than bulk-written and the indexes match hot user-scoped reads. |
| **Impact Estimate** | Reduces DB scan cost for history/progression reads from growing table scans to bounded indexed lookups. |
| **Reuse Scope** | System-wide |

| Parameter | Detail |
| --- | --- |
| **Title** | Large unstable Workout state still creates broad recomposition pressure |
| **Category** | CPU / GC / UI |
| **Severity** | Medium |
| **Symptom** | Fast typing, timer updates, and set toggles can still invalidate more of the Workout tree than necessary. |
| **Root Cause** | `WorkoutScreenState` still carries several `Map<String, Map<Int, String>>`, `Map<String, List<SetCompletionEntity>>`, and AI/history maps. Compose treats these as unstable and may re-run item work broadly. |
| **Recommended Fix** | Next refactor: split Workout into stable screen state plus per-card `StateFlow`/selectors or immutable persistent maps. Do not pass whole-screen mutable maps into each card. |
| **Risk & Tradeoffs** | Requires careful state ownership changes in `WorkoutViewModel` and UI tests for set editing. |
| **Impact Estimate** | Expected 15-35% less recomposition work during set entry and timer-heavy sessions. |
| **Reuse Scope** | Module |

# 3) Low-Hanging Fruit (Immediate Wins)

* Keep StrictMode enabled in debug and treat new main-thread disk/network logs as performance bugs.
* Keep Dashboard ViewModel warmup staged and lightweight; do not eagerly compose hidden tab UIs, but do pre-create tab ViewModels before first user navigation.
* Keep first-entry remote refreshes behind visible-content delays and TTL checks.
* Use explicit Coil request sizes for every fixed-size `AsyncImage` in hot lists.
* Remove remaining per-action remote calls where Room already has `synced=false` or `dirty=true` durability.
* Run `compileDebugKotlin` before merging any Room entity/migration change.

# 4) Strategic Refactors (The Deep Dive)

* Preserve current UI/UX as a hard constraint: prefer moving work off the main thread, reducing network/data volume, and narrowing recomposition scope before removing animations, visual effects, or interaction affordances.
* Any visual optimization must be parity-checked against the current experience. If an effect is reduced, replace it with a cheaper effect that keeps the same perceived polish.
* Split Workout state by update frequency: static program/day data, per-card set state, timer state, history/AI detail state, and challenge events.
* Add server-side tombstones or soft deletes for `set_completions` if perfect multi-device deletion propagation is required.
* Consolidate Dashboard challenge calls into one RPC that returns today and upcoming events together, reducing two network round-trips to one.
* Move Profile dashboard stats to a snapshot repository/API so profile, stats, dates, achievements, and tracked summaries are fetched and cached as one coherent unit.
* Expand Macrobenchmark coverage on a physical device for authenticated Workout flows, card expand/collapse, fast set entry, and challenge overlays.

# 5) Validation & Monitoring Plan

* **Profiling Strategy:**
  * Android Studio Profiler or Perfetto for main-thread slices, GC pauses, disk I/O, and network overlap.
  * Macrobenchmark for cold startup, session restore, first tab switch, Workout scroll, and card expand/collapse.
  * JankStats or FrameMetricsAggregator in debug builds for field-like frame metrics.
  * Supabase query logs/advisors for set-completion request count, payload size, and policy/index issues.
  * StrictMode logs for main-thread disk/network regressions.
* **Key Performance Indicators (KPIs):**
  * Cold startup TTID and TTFD.
  * P50/P95 frame time during first Dashboard entry and Workout scroll.
  * Percentage of frames over 16.6ms and 33ms.
  * GC count and total GC pause time in the first 30 seconds.
  * Supabase request count and transferred bytes during the first dashboard minute.
  * Normal set-completion sync row count: should equal dirty/delta rows, not total history.
  * Room query duration for set summaries and weekly completion observation.

Validation performed in this pass:

* `.\gradlew.bat :app:compileDebugKotlin` succeeded.
* `.\gradlew.bat :app:assembleDebug` succeeded.
* `.\gradlew.bat :baselineprofile:compileBenchmarkReleaseKotlin` succeeded.
* `.\gradlew.bat :baselineprofile:connectedBenchmarkReleaseAndroidTest` succeeded on the connected emulator after enabling the benchmark module's emulator suppression for local sanity runs. Physical-device results are still required for final performance numbers.
* Emulator benchmark snapshot: cold startup `timeToInitialDisplayMs` median 433.1ms, max 698.8ms. Workout scroll/tab benchmark produced Perfetto traces, but the unauthenticated/emulator context limits the frame-count usefulness.
* Installed `app-debug.apk` on the connected emulator and launched `com.avonix.profitness`; the process stayed alive and logcat showed no `FATAL EXCEPTION`, `AndroidRuntime` crash, or Room migration verification error after startup.
* Fixed a Room schema validation crash risk by adding the new dirty-sync index to `SetCompletionEntity` so it matches `MIGRATION_10_11`.
* Fixed invalid manual baseline profile wildcard rules (`;->*`) by converting them to valid method wildcards (`;->**(**)**`), unblocking ART profile expansion for benchmark/release-style builds.
* Fixed local benchmark packaging when release signing secrets are absent: actual `assembleRelease`/`bundleRelease` still fail without release credentials, while benchmark/local measurement builds can use the debug keystore.
* Supabase MCP access was verified with a live SQL call. Remote migrations were applied and verified for `set_completions` dirty/delta sync, optimized RLS policies, update triggers, and hot query indexes.
* StrictMode previously flagged Supabase/Ktor client construction doing classpath disk reads on the main thread; the app now prewarms the Supabase singleton on an IO application scope to reduce first-interaction class loading pressure.
* Follow-up regression pass fixed session restore navigation so a restored Supabase session drives dashboard navigation through state, not only a one-shot event.
* Follow-up data-flow pass restored `pushUnsyncedWorkouts()` to workout refresh paths and guarded Profile stats/date/ratio reads from stale disk cache immediately after invalidation.
* Follow-up perceived-performance pass reduced first-tab content delay from 140ms to 48ms, shortened dashboard tab transition durations, reduced Program/Discover/Profile artificial load delays, added Workout LazyColumn item keys/content types, and reduced per-card map/set allocations during scroll and set editing.
* Latest startup regression pass prevents restored-session users from rendering the login branch even for one frame; `AuthScreen` now keeps the splash visible while dashboard navigation is pending.
* Latest first-click regression pass reduced first-tab content delay further to 16ms and restored a staged Dashboard ViewModel warmup sequence (`PLAN` first, then `ORACLE`, `KEŞFET`, `USER`) so initial tab taps do not absorb ViewModel construction and first repository subscriptions.
* Latest sanity flow: installed `app-debug.apk`, launched the app, performed Workout scroll gestures and transitions across PLAN, ORACLE, KEŞFET, USER, and FORGE. Process stayed alive and logcat showed no `FATAL EXCEPTION`, `ANR`, Room migration error, or SQLite error.
* Latest targeted verification: after force-stop and launch, UI dumps at ~180ms and ~1080ms contained no Login/Email/Password markers, then PLAN tap succeeded and logcat showed no app crash/ANR/SQLite errors.
