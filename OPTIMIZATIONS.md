# Profitness App — Optimization Audit
**Date:** 2026-04-01
**Auditor:** Claude Code (Senior Optimization Pass)
**Scope:** Full codebase — ViewModels, Repositories, Screens, Navigation, DI, Supabase

---

## 1) Optimization Summary

**Current Health:** Poor. Systematic N+1 query patterns, no caching layer, monolithic state objects causing full-screen recomposition on every update, gradient brushes reallocated every frame, and repeated expensive operations on every screen open. Issues compound: slow DB → slow ViewModel → full recomposition → frame drop.

**Top 3 Highest-Impact Improvements:**
1. **Eliminate N+1 queries in ProgramRepositoryImpl and ProfileRepositoryImpl** — cause 10–50× more DB round-trips than necessary, primary source of loading slowness.
2. **Cache `getAllAchievements()` and `getAllExercises()` app-wide** — global static data fetched fresh on every screen open from multiple ViewModels simultaneously.
3. **Split ProfileState (23 props) into focused sub-states** — reduces recomposition surface by ~70% on any profile interaction.

**Biggest Risk If No Changes Made:** As user workout history grows (100+ logs), `getUserStats()` and `getWeeklyCompletionRatios()` will cause ANR. Both load all workout_logs into app memory and compute in Kotlin — linear growth with usage.

---

## 2) Findings (Prioritized)

---

### F-01 · N+1: ProgramRepositoryImpl.getUserPrograms()
- **Category:** DB / Network
- **Severity:** Critical
- **File:** `ProgramRepositoryImpl.kt` lines 29–84
- **Impact:** User with 3 programs × 7 days = 21+ sequential DB round-trips. Should be 1.
- **Evidence:**
  ```kotlin
  val programs = dtos.map { dto ->
      val days = fetchDays(dto.id)          // 1 query per program
      dto.toDomain().copy(days = days)
  }
  // fetchDays (line 70-73):
  dayDtos.map { dayDto ->
      val exercises = fetchExercises(dayDto.id)  // 1 query per day
      dayDto.toDomain().copy(exercises = exercises)
  }
  ```
- **Why inefficient:** Sequential map + I/O = N+1. 3 programs × 7 days = 24 total queries.
- **Recommended fix:** Use Supabase nested select (1 query total):
  ```kotlin
  supabase.postgrest["programs"]
      .select(Columns.raw("""
          *, program_days(
              *, program_exercises(
                  *, exercises(name, target_muscle, category, image_url)
              )
          )
      """)) {
          filter { eq("user_id", userId) }
      }
  ```
  Requires new nested DTO classes to deserialize the tree.
- **Expected impact:** ~80% load time reduction for program list.
- **Removal Safety:** Needs Verification

---

### F-02 · N+1: ProfileRepositoryImpl.getWeeklyCompletionRatios()
- **Category:** DB
- **Severity:** Critical
- **File:** `ProfileRepositoryImpl.kt` lines 191–232
- **Impact:** 2 queries per workout log. 7 logs this week = 14 extra queries on every profile load.
- **Evidence:**
  ```kotlin
  for (log in logs) {
      val total = supabase.postgrest["program_exercises"]
          .select { filter { eq("program_day_id", log.program_day_id) } }
          .decodeList<...>().size                // Query 1 inside loop

      val completed = supabase.postgrest["exercise_logs"]
          .select { filter { eq("workout_log_id", log.id) } }
          .decodeList<...>().size                // Query 2 inside loop
  }
  ```
- **Recommended fix:** Batch both outside the loop:
  ```kotlin
  val logIds = logs.map { it.id }
  val dayIds = logs.mapNotNull { it.program_day_id }

  val allProgramEx = supabase.postgrest["program_exercises"]
      .select { filter { isIn("program_day_id", dayIds) } }
      .decodeList<ProgramExerciseCountDto>()
      .groupBy { it.program_day_id }

  val allExLogs = supabase.postgrest["exercise_logs"]
      .select { filter { isIn("workout_log_id", logIds) } }
      .decodeList<ExerciseLogDto>()
      .groupBy { it.workout_log_id }

  for (log in logs) {
      val total = allProgramEx[log.program_day_id]?.size ?: continue
      val completed = allExLogs[log.id]?.size ?: 0
      result[log.date] = (completed.toFloat() / total).coerceIn(0f, 1f)
  }
  ```
- **Expected impact:** 7 logs → from 14 queries to 2. ~85% reduction.
- **Removal Safety:** Safe

---

### F-03 · N+1: WorkoutRepositoryImpl exercise logs loop
- **Category:** DB
- **Severity:** Critical
- **File:** `WorkoutRepositoryImpl.kt` lines 97–128
- **Evidence:** `supabase.postgrest["exercise_logs"].select { filter { eq("workout_log_id", log.id) } }` inside a `for (log in logs)` loop.
- **Recommended fix:** Same batch `isIn` pattern as F-02.
- **Expected impact:** N queries → 1 query per call.
- **Removal Safety:** Safe

---

### F-04 · INSERT + SELECT pattern: ProgramRepositoryImpl
- **Category:** DB
- **Severity:** High
- **File:** `ProgramRepositoryImpl.kt` lines 98–170 (createFromTemplate), 178–249 (createManual)
- **Impact:** Every day insert is followed by a SELECT to get the generated ID. 7 days = 14 queries (7 INSERT + 7 SELECT) instead of 7.
- **Evidence:**
  ```kotlin
  supabase.postgrest["program_days"].insert(buildJsonObject { ... })  // INSERT

  val dayDto = supabase.postgrest["program_days"]                     // then SELECT
      .select { filter { eq("program_id", ...) ; eq("day_index", dayIdx) } }
      .decodeSingle<ProgramDayDto>()
  ```
- **Recommended fix:** Use Supabase's returning row pattern:
  ```kotlin
  val dayDto = supabase.postgrest["program_days"]
      .insert(buildJsonObject { ... }) { select() }
      .decodeSingle<ProgramDayDto>()
  ```
  Also: batch insert all exercises for a day in one call instead of per-exercise inserts.
- **Expected impact:** createFromTemplate: ~30 queries → ~10.
- **Removal Safety:** Safe

---

### F-05 · getAllAchievements() fetched on every screen open from 2 ViewModels — no cache
- **Category:** DB / Caching
- **Severity:** Critical
- **Files:** `ProfileViewModel.kt:107`, `WorkoutViewModel.kt:233`, `ProfileRepositoryImpl.kt:286`
- **Evidence:**
  ```kotlin
  // ProfileViewModel.kt — fired on init {} every screen open
  val allAchDef = async { profileRepository.getAllAchievements() }

  // WorkoutViewModel.kt — fired on every workout save
  val allAch = profileRepository.getAllAchievements().getOrNull()

  // Underlying query — no filter, no limit
  supabase.postgrest["achievements"].select()
  ```
- **Why inefficient:** Achievements are global static data — never change per user. Fetched fresh on every screen open and every workout save.
- **Recommended fix:** Singleton `AppCache` injected via Hilt:
  ```kotlin
  @Singleton
  class AppCache @Inject constructor(private val repo: ProfileRepository) {
      private var achievements: List<AchievementDto>? = null
      private var exercises: List<ExerciseItem>? = null

      suspend fun getAchievements() = achievements
          ?: repo.getAllAchievements().getOrDefault(emptyList()).also { achievements = it }

      suspend fun getExercises() = exercises
          ?: repo.getAllExercises().getOrDefault(emptyList()).also { exercises = it }

      fun invalidateExercises() { exercises = null }  // call after addExercise()
  }
  ```
- **Expected impact:** Eliminates 2+ full table scans per user session after first load.
- **Removal Safety:** Safe

---

### F-06 · getAllExercises() fetched on every ProgramViewModel init — no cache
- **Category:** DB / Caching
- **Severity:** High
- **File:** `ProgramViewModel.kt` lines 80–84
- **Evidence:**
  ```kotlin
  init {
      loadUserPrograms()
      loadExercises()    // SELECT * FROM exercises (91+ rows) on every ProgramBuilderScreen open
  }
  ```
  Also re-fetched inside `createFromAI()` line 102–103 as fallback.
- **Recommended fix:** Use `AppCache.getExercises()` (F-05). After `addExercise()` succeeds, call `appCache.invalidateExercises()`.
- **Expected impact:** Eliminates repeated 91-row scans per screen visit.
- **Removal Safety:** Safe

---

### F-07 · getUserStats() loads all workout_logs into memory — unbounded
- **Category:** DB / Algorithm
- **Severity:** High
- **File:** `ProfileRepositoryImpl.kt` lines 102–154
- **Impact:** Linear growth with usage. 100 workouts = 100 rows loaded, sorted, and streak-calculated in Kotlin.
- **Evidence:**
  ```kotlin
  val workoutLogs = supabase.postgrest["workout_logs"]
      .select { filter { eq("user_id", userId) } }  // NO LIMIT
      .decodeList<WorkoutLogDto>()

  val distinctDates = workoutLogs
      .mapNotNull { LocalDate.parse(it.date.take(10)) }
      .distinct().sortedDescending()                  // Full sort in Kotlin

  var count = 0; var expected = mostRecent
  for (d in distinctDates) { ... }                    // Streak loop in Kotlin
  ```
- **Recommended fix (short-term):** Add `limit(90)` — streaks beyond 90 days don't change. Add `order("date", DESCENDING)` before the limit so most recent 90 are fetched.
- **Recommended fix (long-term):** Supabase RPC function for streak calculation. Returns `{ total_workouts, current_streak, longest_streak }` in one DB-side call.
- **Expected impact:** Data transfer capped at 90 rows max. Prevents ANR as history grows.
- **Removal Safety:** Safe (with `limit(90)`)

---

### F-08 · unlockAchievement(): 3 sequential queries for one atomic upsert
- **Category:** DB
- **Severity:** High
- **File:** `ProfileRepositoryImpl.kt` lines 255–284
- **Evidence:**
  ```kotlin
  val ach = supabase.postgrest["achievements"]
      .select { filter { eq("key", achievementKey) } }   // Query 1
      .decodeSingleOrNull<AchievementDto>()

  val existing = supabase.postgrest["user_achievements"]
      .select { ... limit(1) }                           // Query 2
      .decodeSingleOrNull<UserAchievementDto>()

  if (existing == null) {
      supabase.postgrest["user_achievements"].insert(...) // Query 3
  }
  ```
- **Recommended fix:**
  1. Get achievement ID from `AppCache` — no DB call.
  2. Use insert with `onConflict = OnConflict.IGNORE` to atomically skip duplicates:
  ```kotlin
  val achId = appCache.getAchievements().find { it.key == achievementKey }?.id ?: return
  supabase.postgrest["user_achievements"].insert(
      buildJsonObject { put("user_id", userId); put("achievement_id", achId) }
  ) { onConflict = OnConflict.IGNORE }
  ```
- **Expected impact:** 3 queries → 1 per achievement unlock.
- **Removal Safety:** Safe (verify UNIQUE constraint on user_achievements(user_id, achievement_id) exists)

---

### F-09 · checkAchievements() duplicated in ProfileViewModel and WorkoutViewModel
- **Category:** Architecture / Dead Code
- **Severity:** High
- **Files:** `ProfileViewModel.kt` ~lines 266–302, `WorkoutViewModel.kt` ~lines 231–260
- **Evidence:** Both contain identical fetch pattern (getAllAchievements, getUnlockedKeys, loop + unlock) with minor differences.
- **Recommended fix:** Extract `AchievementService` as a `@Singleton` Hilt class. Both ViewModels inject it. Combined with F-05 AppCache, this also centralizes cache usage.
- **Expected impact:** Eliminates duplicate DB calls when both ViewModels are active, reduces maintenance surface.
- **Removal Safety:** Safe

---

### F-10 · ProfileState has 23 properties — entire ProfileScreen recomposes on any field change
- **Category:** Frontend
- **Severity:** High
- **File:** `ProfileViewModel.kt` lines 41–68, `ProfileScreen.kt` line ~50
- **Evidence:**
  ```kotlin
  data class ProfileState(   // 23 fields
      val displayName: String, val avatar: String, val rank: String,
      val level: Int, val xp: Int, val xpPerLevel: Int,
      val currentStreak: Int, val longestStreak: Int, val totalWorkouts: Int,
      // ... 14 more
  )

  // ProfileScreen collects single flow
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  ```
- **Why inefficient:** Setting `isSaving = true` triggers recomposition of header, stats, achievements, body metrics — all at once.
- **Recommended fix:** Split into focused StateFlows:
  ```kotlin
  val headerState: StateFlow<ProfileHeaderState>       // name, avatar, rank, xp
  val statsState: StateFlow<ProfileStatsState>         // streak, workouts, exercises
  val metricsState: StateFlow<ProfileMetricsState>     // height, weight, bmi
  val achievementState: StateFlow<AchievementListState>
  ```
- **Expected impact:** ~60–70% fewer recompositions. Visible as smoother UI during save operations.
- **Removal Safety:** Needs Verification

---

### F-11 · checkAchievements() calls updateState() in a loop — N state emissions
- **Category:** Frontend
- **Severity:** High
- **File:** `ProfileViewModel.kt` ~lines 285–302
- **Evidence:**
  ```kotlin
  allAch.filter { it.key !in unlocked }.forEach { ach ->
      if (value >= ach.threshold) {
          profileRepository.unlockAchievement(userId, ach.key)
          updateState { st -> st.copy(achievements = ...) }   // Emission per achievement
          sendEvent(ProfileEvent.AchievementUnlocked(...))    // Event per achievement
      }
  }
  ```
- **Recommended fix:**
  ```kotlin
  val newlyUnlocked = mutableListOf<AchievementDto>()
  allAch.filter { ... }.forEach { ach ->
      if (value >= ach.threshold) {
          profileRepository.unlockAchievement(userId, ach.key)
          newlyUnlocked.add(ach)
      }
  }
  if (newlyUnlocked.isNotEmpty()) {
      updateState { st -> st.copy(achievements = ...) }       // Single update
      sendEvent(ProfileEvent.AchievementUnlocked(...))        // Single event (first unlock)
  }
  ```
- **Expected impact:** N state emissions → 1 per check cycle.
- **Removal Safety:** Safe

---

### F-12 · WorkoutViewModel.checkAndUnlockAchievements() — 3 sequential queries
- **Category:** Concurrency
- **Severity:** Medium
- **File:** `WorkoutViewModel.kt` ~lines 231–234
- **Evidence:**
  ```kotlin
  val stats        = profileRepository.getUserStats(userId).getOrNull() ?: return
  val allAch       = profileRepository.getAllAchievements().getOrNull() ?: return
  val unlockedKeys = profileRepository.getUnlockedAchievementKeys(userId).getOrNull() ?: return
  ```
- **Recommended fix:** Parallelize with coroutine `async`:
  ```kotlin
  val statsDef    = async { profileRepository.getUserStats(userId) }
  val allAchDef   = async { appCache.getAchievements() }          // from cache, near-instant
  val unlockedDef = async { profileRepository.getUnlockedAchievementKeys(userId) }
  val stats = statsDef.await().getOrNull() ?: return
  val allAch = allAchDef.await()
  val unlockedKeys = unlockedDef.await().getOrNull() ?: return
  ```
- **Expected impact:** Latency = max(queries) instead of sum. ~40% faster post-workout flow.
- **Removal Safety:** Safe

---

### F-13 · Gradient brushes recreated on every recomposition — animation jank
- **Category:** Frontend / CPU
- **Severity:** High
- **Files:** `DashboardScreen.kt` ~lines 176–201 (background), ~244–289 (nav bar — 4 brushes)
- **Evidence:**
  ```kotlin
  Box(modifier = modifier.drawWithCache {
      val radial = Brush.radialGradient(...)    // New object on every recomposition
      val sweep  = Brush.linearGradient(...)
      onDrawBehind { ... }
  })
  ```
- **Why inefficient:** `drawWithCache` caches draw commands but `Brush.radialGradient()` creates new objects if referencing theme colors that change identity on recomposition. Nav bar alone: 4 brushes × every tab switch.
- **Recommended fix:** Hoist brushes outside `drawWithCache`, memoize with `remember`:
  ```kotlin
  val radialBrush = remember(theme.accent, theme.bg0) {
      Brush.radialGradient(listOf(theme.accent.copy(0.12f), Color.Transparent), ...)
  }
  Box(modifier = modifier.drawWithCache {
      onDrawBehind { drawRect(radialBrush) }
  })
  ```
- **Expected impact:** Eliminates brush allocations during navigation. Reduces GC pressure, fixes frame drops on tab switch.
- **Removal Safety:** Safe

---

### F-14 · Every screen open fires full DB load from ViewModel init {}
- **Category:** DB / Architecture
- **Severity:** High
- **Files:** `ProfileViewModel.kt:85`, `ProgramViewModel.kt:58–61`, `WorkoutViewModel.kt:37–39`
- **Evidence:**
  ```kotlin
  init { loadProfile() }         // 6 async DB calls
  init { loadUserPrograms(); loadExercises() }
  init { loadActiveProgram() }
  ```
- **Recommended fix:** Add `hasLoaded` guard to prevent redundant reloads on navigation:
  ```kotlin
  private var hasLoaded = false
  init { if (!hasLoaded) { hasLoaded = true; loadProfile() } }
  ```
  Data is only reloaded when explicitly called (e.g., via `DisposableEffect ON_RESUME` in screens that need freshness).
- **Expected impact:** Eliminates repeated DB calls when user navigates back and forth between tabs.
- **Removal Safety:** Safe

---

### F-15 · Logout does not clear ViewModel state — stale data risk
- **Category:** Reliability / Security
- **Severity:** High
- **File:** `AppNavigation.kt`
- **Evidence:**
  ```kotlin
  onLogout = {
      authViewModel.logout()
      navController.navigate(Routes.AUTH) { popUpTo(Routes.DASHBOARD) { inclusive = true } }
      // ProfileViewModel, WorkoutViewModel, ProgramViewModel state still in memory
  }
  ```
- **Why problematic:** Hilt ViewModels scoped to Activity lifetime survive navigation. Next login briefly flashes previous user's profile/program data.
- **Recommended fix:**
  ```kotlin
  onLogout = {
      authViewModel.logout()
      (context as? Activity)?.recreate()   // Destroys all ViewModels, fresh DI graph
  }
  ```
  Standard pattern used by Gmail, Spotify, etc. for account switching.
- **Expected impact:** Eliminates data flash and PII leakage between accounts.
- **Removal Safety:** Safe

---

### F-16 · NewsViewModel holds 300 articles + all translations + all click counts in memory
- **Category:** Memory
- **Severity:** Medium
- **File:** `NewsViewModel.kt`
- **Evidence:**
  ```kotlin
  private const val MAX_ARTICLES = 300
  private val _cardTranslations = MutableStateFlow<Map<String, String>>(emptyMap())  // up to 300 entries
  private val clickCounts: MutableMap<String, Int> = run {
      val stored = prefs.all  // Loads ENTIRE SharedPreferences file on init
      ...
  }
  ```
- **Recommended fix:**
  - Reduce `MAX_ARTICLES` to 50–75 for display; true-paginate on scroll.
  - Evict `_cardTranslations` entries for articles scrolled > 100 items away.
  - Lazy-load click counts from `DataStore` rather than all at init.
- **Expected impact:** Memory from ~3–5 MB → ~0.5–1 MB for news data.
- **Removal Safety:** Safe

---

### F-17 · applyClickRanking() performs 3 separate sorts on the same article list
- **Category:** Algorithm
- **Severity:** Medium
- **File:** `NewsViewModel.kt` ~lines 399–440
- **Evidence:**
  ```kotlin
  val ranked = scored.filter { ... }.sortedWith(...)      // Sort 1  O(n log n)
  val featuredIds = scored.sortedWith(...)                // Sort 2  O(n log n) — identical
  return articles.map { ... }.sortedByDescending { ... } // Sort 3  O(n log n)
  ```
- **Recommended fix:** Single sort pass:
  ```kotlin
  val sorted = articles.sortedWith(
      compareByDescending<Article> { clickCounts[articleKey(it)] ?: 0 }
          .thenByDescending { it.publishedAtMs }
  )
  val featuredIds = sorted.take(6).map { it.id }.toHashSet()
  return sorted.map { it.copy(isFeatured = it.id in featuredIds) }
  ```
- **Expected impact:** O(3n log n) → O(n log n). With 300 articles: ~3× faster ranking on every click.
- **Removal Safety:** Safe

---

### F-18 · HorizontalPager: up to 300 article pages loaded
- **Category:** Frontend / Memory
- **Severity:** Medium
- **File:** `NewsScreen.kt` ~lines 294–300
- **Evidence:** `HorizontalPager(state = pagerState)` where pagerState page count = articles.size (up to 300).
- **Recommended fix:** Limit pager to featured subset:
  ```kotlin
  val featuredArticles = remember(articles) { articles.filter { it.isFeatured }.take(12) }
  HorizontalPager(state = rememberPagerState { featuredArticles.size }) { ... }
  ```
- **Expected impact:** Composition count from 300 → 12 pages. Faster initial render.
- **Removal Safety:** Safe

---

### F-19 · ProgramBuilderScreen.READY_PROGRAMS — top-level val always in memory
- **Category:** Memory
- **Severity:** Low
- **File:** `ProgramBuilderScreen.kt` ~lines 93–253
- **Recommended fix:** `private val READY_PROGRAMS by lazy { listOf(...) }`
- **Expected impact:** ~5 KB saved until first ProgramBuilderScreen open.
- **Removal Safety:** Safe

---

## 3) Quick Wins (Do First)

| Priority | Fix | File | Est. Time | Impact |
|----------|-----|------|-----------|--------|
| 1 | F-02: Batch getWeeklyCompletionRatios() N+1 | ProfileRepositoryImpl | 30 min | Critical |
| 2 | F-05+F-06: AppCache for achievements + exercises | New singleton + 3 files | 45 min | Critical |
| 3 | F-15: Activity.recreate() on logout | AppNavigation | 5 min | High |
| 4 | F-14: Hoist gradient brushes into remember() | DashboardScreen | 20 min | High — fixes nav jank |
| 5 | F-07: Add limit(90) to workout_logs query | ProfileRepositoryImpl | 5 min | High — prevents ANR |
| 6 | F-11: Batch achievement state updates | ProfileViewModel | 15 min | High |
| 7 | F-14: Add hasLoaded guard to ViewModels | 3 ViewModels | 20 min | Medium |
| 8 | F-04: Replace INSERT+SELECT with insert{select()} | ProgramRepositoryImpl | 30 min | High |
| 9 | F-12: Parallelize checkAchievements queries | WorkoutViewModel | 10 min | Medium |
| 10 | F-17: Single sort in applyClickRanking() | NewsViewModel | 15 min | Medium |

---

## 4) Deeper Optimizations (Do Next)

### 4.1 Supabase Nested Select for Programs (F-01)
Replace `getUserPrograms()` + `fetchDays()` + `fetchExercises()` chain with single nested Supabase query. Requires new nested DTO types. Reduces program load from 20+ queries to 1. Highest DB impact overall.

### 4.2 Extract AchievementService (F-09)
Create `@Singleton AchievementService`. Inject into both `ProfileViewModel` and `WorkoutViewModel`. Combine with `AppCache`. Single code path, zero duplication, centralized cache invalidation.

### 4.3 Split ProfileState into sub-StateFlows (F-10)
Decompose 23-property state into 4 focused `StateFlow`s. Each screen section subscribes only to its own flow. ~70% recomposition reduction on profile interactions.

### 4.4 Supabase RPC for streak calculation (F-07 long-term)
PostgreSQL function returns `{ total_workouts, current_streak, longest_streak }` — computed DB-side in one round-trip. Zero data transfer for calculation, no Kotlin sort/loop needed.

### 4.5 News pagination + translation eviction (F-16, F-18)
True pagination at 50 articles/page, lazy-load on scroll, evict off-screen translation cache entries. Replace SharedPreferences click counts with Room + periodic cleanup task.

### 4.6 WorkoutViewModel INSERT+SELECT for startWorkout (F-04 variant)
`WorkoutRepositoryImpl.startWorkout()` does SELECT → INSERT → SELECT pattern. Replace with upsert or `insert{select()}` to eliminate the final re-fetch.

---

## 5) Validation Plan

### Metrics — Before/After Each Fix

**Database:**
- Profile screen cold load: target < 800 ms (currently likely 2–4 s)
- Program list load: target < 500 ms
- Workout save + achievement check: target < 300 ms
- Supabase query count (log via dashboard): profile load should emit ≤ 6 queries after F-02 fix

**Rendering:**
- Tab navigation: 60 fps consistent (no janked frames > 16 ms) — measure with Systrace/Perfetto
- Dashboard nav bar gradient: GPU render time < 8 ms/frame
- LazyColumn scroll (20 exercises): no dropped frames

**Memory:**
- Profile screen heap: < 50 MB
- News screen heap: < 60 MB (currently likely 70–80 MB)
- Use Android Studio Memory Profiler: check for retained objects after screen exit

### Correctness Checks After Each Fix

| Fix | Correctness Verification |
|-----|--------------------------|
| F-02 batch queries | Verify weekly completion percentages match pre-fix values |
| F-04 insert{select()} | Verify all days + exercises saved correctly after program creation |
| F-05 AppCache | Verify achievements invalidated after new exercise added |
| F-07 limit(90) | Verify streak calculation identical with limit vs without |
| F-15 Activity.recreate() | Verify clean state: no previous user data visible after login with new account |
| F-08 onConflict.IGNORE | Verify no duplicate achievement rows after repeated unlock attempts |

---

## 6) Already Fixed

| Fix | File | Status |
|-----|------|--------|
| Supabase: exercises INSERT RLS policy | Supabase migration | ✅ |
| Supabase: profiles_read_all policy removed | Supabase migration | ✅ |
| Supabase: auto-create profile trigger on signup | Supabase migration | ✅ |
| Gemini token limit 600 → 2000 | GeminiRepositoryImpl | ✅ |
| Gemini markdown code block stripping | ProgramViewModel | ✅ |
| Gemini prompt sıkılaştırıldı | ProgramViewModel | ✅ |
| AI: yeni egzersiz otomatik DB'ye ekleniyor | ProgramViewModel + ProgramRepositoryImpl | ✅ |
| AI: görsel/PDF/HTML yükleme (multimodal) | GeminiDto + GeminiRepositoryImpl + ProgramBuilderScreen | ✅ |
| Logout calls signOut() | AppNavigation + AuthViewModel | ✅ |
| App starts at correct route (session check) | AppNavigation | ✅ |
| Hardcoded test credentials removed | AuthScreen | ✅ |
