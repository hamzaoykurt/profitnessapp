# Profitness App — Full Optimization Audit
**Date:** 2026-03-02
**Auditor:** Senior Optimization Engineer (Claude)
**Stack:** Android, Jetpack Compose, Kotlin, Hilt, Coil, Coroutines
**Codebase size:** ~20 Kotlin files, single-module app

---

## 1) Optimization Summary

### Current Health
The codebase is a UI-heavy Compose app in early/mid development. The fundamentals (Hilt DI, ViewModel, StateFlow, LazyColumn) are set up correctly. However, several rendering, state management, and dead-code issues exist that will hurt frame rate, battery life, and long-term maintainability if unaddressed.

### Top 3 Highest-Impact Improvements
1. **Remove or offload `blur(80.dp)` on the animated full-screen `AtmosphericBloom` box** — this is a GPU-intensive RenderEffect running at 60fps on a full-screen composable. It alone can cause sustained dropped frames and excess battery drain.
2. **Wire `AuthViewModel` to the auth button** — the ViewModel, its delay logic, and all state management are completely bypassed at runtime. The UI skips directly to the dashboard without any auth logic executing. This is a functional bug as well as wasted memory.
3. **Fix `BaseViewModel.updateState` read-modify-write race** — using `.value = update(.value)` instead of `.update { }` creates a non-atomic state mutation; concurrent coroutines will produce lost updates.

### Biggest Risk If No Changes Are Made
The `blur(80.dp)` + continuous `rememberInfiniteTransition` combination in `AtmosphericBloom` will cause chronic jank on mid-range devices — the AICoach screen (the most feature-rich screen) is the most likely to receive user reports about performance. The auth bypass is a correctness bug that must be fixed before production.

---

## 2) Findings (Prioritized)

---

### F-01 · `blur(80.dp)` on Animated Full-Screen Background
- **Category:** Frontend / CPU / Memory
- **Severity:** Critical
- **Impact:** Frame drops, GPU overdraw, battery drain — sustained while the AI Coach tab is visible
- **Evidence:** `AICoachScreen.kt:231` — `AtmosphericBloom()` composable, `Modifier.blur(80.dp)` applied to a `Box` that also has `graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)` with two `rememberInfiniteTransition` animating values
- **Why it's inefficient:**
  `Modifier.blur()` in Jetpack Compose on Android API 31+ uses `RenderEffect.createBlurEffect()`. When combined with a `graphicsLayer`, this creates an off-screen rendering layer. Because `scale` animates from `1f → 1.2f` and `alpha` from `0.1f → 0.25f` (both `infiniteRepeatable`), the full-screen off-screen buffer is re-rendered on **every frame** at 60fps. A full-screen 80dp blur is one of the most expensive operations the GPU can perform per frame.
- **Recommended Fix:**
  - Replace with a static (non-animated) gradient background drawn via `drawWithCache`. The subtle atmospheric effect will remain without per-frame GPU cost.
  - If animation is required, reduce the blur radius to ≤ 20dp, constrain the animated Box to a small portion of the screen (e.g., 200×200dp), and use `key(Unit)` to avoid unnecessary recompositions.
  - Alternatively, pre-render the bloom into a `Bitmap` at startup and animate only the alpha at the composable level, not at the GPU shader level.
- **Tradeoffs / Risks:** Visual appearance will change slightly; the bloom effect will be less dynamic. Test on low-end devices before and after.
- **Expected Impact:** 30–60% reduction in GPU frame time on the AI Coach screen; eliminates a class of jank complaints.
- **Removal Safety:** Safe (visual change only, no logic impact)
- **Reuse Scope:** Local file (`AICoachScreen.kt`)

---

### F-02 · `AuthViewModel` Is Never Called — Dead Runtime Logic
- **Category:** Reliability / Dead Code
- **Severity:** Critical
- **Impact:** Auth logic is completely bypassed; all users can proceed without any credential check
- **Evidence:** `AuthScreen.kt:129–133`:
  ```kotlin
  ObsidianButton(
      text = if (isLogin) "GİRİŞ YAP" else "KAYIT OL",
      onClick = onNavigateToDashboard,  // <-- bypasses ViewModel entirely
      modifier = Modifier.fillMaxWidth()
  )
  ```
  `viewModel.onLoginClick()` and `viewModel.onRegisterClick()` are never called. `AuthViewModel.kt` defines both functions and a `delay(1000)` simulation, but they are dead code at runtime.
- **Why it's inefficient:** The ViewModel is instantiated by Hilt (memory allocation, injection overhead) but does nothing. The auth error state, loading state, and `isSuccess` flag are all unused. This is both a bug and wasted allocation.
- **Recommended Fix:**
  Change `onClick = onNavigateToDashboard` to:
  ```kotlin
  onClick = {
      if (isLogin) viewModel.onLoginClick(email, password)
      else viewModel.onRegisterClick(email, password)
  }
  ```
  Then observe `state.isSuccess` with a `LaunchedEffect` to trigger navigation, and show `state.error` in a snackbar or text view.
- **Tradeoffs / Risks:** Demo mode will no longer auto-navigate. The existing demo logic (`email.contains("@") && pass.length >= 6`) means any valid-looking credentials still work.
- **Expected Impact:** Eliminates a functional bug; no performance change.
- **Removal Safety:** Needs Verification (functional change)
- **Reuse Scope:** `AuthScreen.kt`, `AuthViewModel.kt`

---

### F-03 · `BaseViewModel.updateState` — Non-Atomic Read-Modify-Write
- **Category:** Concurrency / Reliability
- **Severity:** High
- **Impact:** Lost state updates when multiple coroutines concurrently update state
- **Evidence:** `BaseViewModel.kt:14–16`:
  ```kotlin
  protected fun updateState(update: (State) -> State) {
      _uiState.value = update(_uiState.value)  // non-atomic read + write
  }
  ```
- **Why it's inefficient:**
  `MutableStateFlow.value = ...` is an atomic write, but reading `_uiState.value` first and then writing the result is not atomic as a whole. If two coroutines call `updateState` concurrently (e.g., an error handler and a timer), the second write may overwrite the first. `MutableStateFlow` provides `update {}` which is an atomic compare-and-swap loop.
- **Recommended Fix:**
  ```kotlin
  protected fun updateState(update: (State) -> State) {
      _uiState.update(update)
  }
  ```
- **Tradeoffs / Risks:** None — `StateFlow.update {}` is a direct drop-in that is safer.
- **Expected Impact:** Eliminates potential state corruption; no performance overhead.
- **Removal Safety:** Safe
- **Reuse Scope:** `BaseViewModel.kt` — affects all ViewModels (`AuthViewModel`)

---

### F-04 · `SetRow` Contains a Dead `animateFloatAsState` (Both Targets = `1f`)
- **Category:** CPU / Frontend / Dead Code
- **Severity:** High
- **Impact:** Wastes a Compose animation slot and a Coroutine per `SetRow` instance; no visual effect
- **Evidence:** `CinematicExerciseCard.kt:279–283`:
  ```kotlin
  val scale by animateFloatAsState(
      if (isDone) 1f else 1f,  // ← both branches return 1f!
      spring(Spring.DampingRatioMediumBouncy),
      label = "set_scale"
  )
  ```
  The `scale` value is always `1f`. It is never applied via a `Modifier.scale()` anywhere in `SetRow`.
- **Why it's inefficient:** `animateFloatAsState` creates an `Animatable` and a coroutine animation subscription per composable. Even though the animation never changes value, the subscription object occupies memory and the lambda runs on every composition.
- **Recommended Fix:** Remove the entire `scale` / `bgAlpha` `animateFloatAsState` block in `SetRow`. The `bgAlpha` animation (`.08f → .15f`) is valid and should be kept, but `scale` should be deleted entirely.
- **Tradeoffs / Risks:** None — no visual change since scale = 1f is identity.
- **Expected Impact:** Small but real: eliminates N dead animation subscriptions (N = total sets across all exercises in a workout, typically 12–20).
- **Removal Safety:** Safe
- **Reuse Scope:** `CinematicExerciseCard.kt`

---

### F-05 · `RestTimerChip` Infinite Transition Runs Even When Timer Is Idle
- **Category:** CPU / Frontend
- **Severity:** High
- **Impact:** Wasted animation work every frame even when no timer is running; affects every expanded exercise card
- **Evidence:** `CinematicExerciseCard.kt:331–336`:
  ```kotlin
  val pulseAnim = rememberInfiniteTransition(label = "pulse")
  val pulseScale by pulseAnim.animateFloat(
      initialValue = 1f, targetValue = 1.06f,
      animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
      label = "pulse_scale"
  )
  ```
  Then on line 351: `.then(if (isRunning) Modifier.scale(pulseScale) else Modifier)`. The modifier is only applied when `isRunning`, but the `infiniteRepeatable` animation **always runs** regardless of `isRunning`.
- **Why it's inefficient:** `rememberInfiniteTransition` creates a perpetual animation frame subscription. Even when `isRunning = false`, the animation ticks at 60fps internally. This runs for every expanded exercise card simultaneously.
- **Recommended Fix:**
  Gate the infinite transition with `if (isRunning)`:
  ```kotlin
  val pulseScale by if (isRunning) {
      val t = rememberInfiniteTransition(label = "pulse")
      t.animateFloat(1f, 1.06f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse_scale")
  } else {
      remember { mutableStateOf(1f) }
  }
  ```
  Alternatively, use `animateFloatAsState` that returns to `1f` when `!isRunning`.
- **Tradeoffs / Risks:** Minor API change within the composable.
- **Expected Impact:** Eliminates continuous per-frame animation work on idle timer chips. With 4 exercises expanded, this is 4× the savings.
- **Removal Safety:** Safe
- **Reuse Scope:** `CinematicExerciseCard.kt`

---

### F-06 · `global var responseCounters` — Mutable Global State, Not Thread-Safe
- **Category:** Concurrency / Reliability
- **Severity:** High
- **Impact:** Race condition under rapid consecutive sends; state leaks across sessions
- **Evidence:** `AICoachScreen.kt:80`:
  ```kotlin
  private var responseCounters = mutableMapOf<String, Int>()
  ```
  This is a plain Kotlin `var` at file scope (effectively a singleton). It is mutated inside `getResponse()` which can be called from a coroutine on any thread.
- **Why it's inefficient:**
  1. Not thread-safe: `HashMap` mutation from coroutines can corrupt the map.
  2. It persists across `AICoachScreen` recompositions — if the screen leaves and re-enters composition, the counters retain their old values, producing unexpected behavior.
  3. As a global, it cannot be tested in isolation.
- **Recommended Fix:**
  Move `responseCounters` into the `AICoachScreen` composable as `remember { mutableStateMapOf<String, Int>() }`, or better, extract `getResponse()` and its state into a ViewModel.
- **Tradeoffs / Risks:** Counter will reset when the screen is destroyed (which is actually the correct behavior for a demo).
- **Expected Impact:** Eliminates a thread-safety bug.
- **Removal Safety:** Safe
- **Reuse Scope:** `AICoachScreen.kt`

---

### F-07 · `SimpleDateFormat` Allocated Per `ChatMessage` Instance
- **Category:** Memory / CPU
- **Severity:** Medium
- **Impact:** Unnecessary object allocation for every chat message; `SimpleDateFormat` is heavyweight
- **Evidence:** `AICoachScreen.kt:48`:
  ```kotlin
  data class ChatMessage(
      ...
      val timestamp: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
  )
  ```
  Every `ChatMessage` instance constructs a new `SimpleDateFormat` and a new `Date` for the default value.
- **Why it's inefficient:** `SimpleDateFormat` is a heavyweight object (it parses the format string on construction). It is also not thread-safe. Creating one per message is wasteful when a shared instance suffices.
- **Recommended Fix:**
  ```kotlin
  private val MESSAGE_TIME_FORMATTER = SimpleDateFormat("HH:mm", Locale.getDefault())

  data class ChatMessage(
      ...
      val timestamp: String = MESSAGE_TIME_FORMATTER.format(Date())
  )
  ```
  Or use `java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))` (API 26+ — minSdk is 31, so this is safe).
- **Tradeoffs / Risks:** Shared `SimpleDateFormat` is not thread-safe; use `DateTimeFormatter` (which is thread-safe) to avoid needing synchronization.
- **Expected Impact:** Eliminates ~N `SimpleDateFormat` allocations per session (low absolute impact, good hygiene).
- **Removal Safety:** Safe
- **Reuse Scope:** `AICoachScreen.kt`

---

### F-08 · `AuthLiquidField.drawBehind` Creates a New `Brush` on Every Draw Pass
- **Category:** CPU / Frontend
- **Severity:** Medium
- **Impact:** Repeated Brush object allocation on every frame the field is drawn
- **Evidence:** `AuthScreen.kt:171–179`:
  ```kotlin
  .drawBehind {
      drawRect(
          brush = Brush.horizontalGradient(
              listOf(Color.Transparent, AmberRimLight.copy(0.25f), Color.Transparent)
          ),
          size = Size(size.width, 1.dp.toPx())
      )
  }
  ```
  `Brush.horizontalGradient(...)` creates a new `LinearGradient` shader on every call to the draw lambda. `drawBehind` is called every time the composable draws.
- **Why it's inefficient:** `Brush.horizontalGradient` allocates a new `LinearGradient` object on every draw call. The brush content never changes, so this allocation is unnecessary.
- **Recommended Fix:** Replace `drawBehind` with `drawWithCache`:
  ```kotlin
  .drawWithCache {
      val brush = Brush.horizontalGradient(
          listOf(Color.Transparent, AmberRimLight.copy(0.25f), Color.Transparent)
      )
      onDrawBehind {
          drawRect(brush = brush, size = Size(size.width, 1.dp.toPx()))
      }
  }
  ```
  `drawWithCache` recalculates only when the size changes.
- **Tradeoffs / Risks:** None.
- **Expected Impact:** Eliminates brush allocation on every draw; measurable on low-end devices.
- **Removal Safety:** Safe
- **Reuse Scope:** `AuthScreen.kt`

---

### F-09 · `ArchitectGrid` Canvas Recalculates Loop Bounds on Every Recomposition
- **Category:** CPU / Frontend
- **Severity:** Medium
- **Impact:** Repeated division and loop overhead every time `ProgramBuilderScreen` redraws
- **Evidence:** `ProgramBuilderScreen.kt:136–148`:
  ```kotlin
  @Composable
  private fun ArchitectGrid() {
      Canvas(modifier = Modifier.fillMaxSize()) {
          val step = 40.dp.toPx()
          val strokeWidth = 0.5.dp.toPx()
          ...
          for (x in 0..(size.width / step).toInt()) { drawLine(...) }
          for (y in 0..(size.height / step).toInt()) { drawLine(...) }
      }
  }
  ```
  The grid is entirely static but recalculates `step`, `strokeWidth`, loop bounds, and draws all lines on every recomposition.
- **Why it's inefficient:** `Canvas` with a loop inside runs the full loop body on every frame. The grid pattern never changes. This is exactly what `drawWithCache` + a pre-built `Path` is designed for.
- **Recommended Fix:**
  ```kotlin
  @Composable
  private fun ArchitectGrid() {
      val color = SurfaceStroke.copy(0.3f)
      Canvas(modifier = Modifier.fillMaxSize().drawWithCache {
          val step = 40.dp.toPx()
          val strokeWidth = 0.5.dp.toPx()
          val path = Path().apply {
              for (x in 0..(size.width / step).toInt()) {
                  moveTo(x * step, 0f); lineTo(x * step, size.height)
              }
              for (y in 0..(size.height / step).toInt()) {
                  moveTo(0f, y * step); lineTo(size.width, y * step)
              }
          }
          onDrawBehind { drawPath(path, color, style = Stroke(strokeWidth)) }
      }) {}
  }
  ```
  The `Path` is only rebuilt when screen size changes.
- **Tradeoffs / Risks:** Minor API refactor. `drawWithCache` recalculates on size changes (e.g., rotation), which is correct.
- **Expected Impact:** Eliminates O(W+H) loop iterations per frame on ProgramBuilderScreen.
- **Removal Safety:** Safe
- **Reuse Scope:** `ProgramBuilderScreen.kt`

---

### F-10 · `SanctuaryTypingIndicator` Creates 3 `rememberInfiniteTransition` Instances Inside `repeat()`
- **Category:** Frontend / Memory
- **Severity:** Medium
- **Impact:** Suboptimal key association for animation state inside a `repeat` block
- **Evidence:** `AICoachScreen.kt:353–360`:
  ```kotlin
  repeat(3) { i ->
      val infinite = rememberInfiniteTransition(label = "dots_$i")
      val alpha by infinite.animateFloat(...)
      Box(Modifier.size(4.dp).clip(CircleShape).background(Lime.copy(alpha)))
  }
  ```
  Using `rememberInfiniteTransition` inside a `repeat()` loop relies on slot-based composition ordering. If the repeat count changes, slots shift, causing wrong animations to be matched to wrong dots.
- **Why it's inefficient:** The typing indicator appears and disappears (`isTyping` state). When it enters/exits composition, all 3 transition objects are created/destroyed. Since the repeat count is fixed (3), it works correctly, but the pattern is fragile. Also, 3 separate infinite transitions consume 3 coroutine frame callbacks instead of 1.
- **Recommended Fix:**
  Create a single `rememberInfiniteTransition` and derive 3 offset floats from it:
  ```kotlin
  val transition = rememberInfiniteTransition(label = "dots")
  val dot0 by transition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "d0")
  val dot1 by transition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse), label = "d1")
  val dot2 by transition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse), label = "d2")
  listOf(dot0, dot1, dot2).forEach { alpha ->
      Box(Modifier.size(4.dp).clip(CircleShape).background(Lime.copy(alpha)))
  }
  ```
- **Tradeoffs / Risks:** Minor refactor, no visual change.
- **Expected Impact:** Reduces animation frame subscriptions from 3 to 1 for the typing indicator.
- **Removal Safety:** Safe
- **Reuse Scope:** `AICoachScreen.kt`

---

### F-11 · `ForgeCard` Applies Double `.shadow()` When Glow Is Active
- **Category:** Frontend / CPU
- **Severity:** Medium
- **Impact:** Two hardware shadow passes per card when `glowColor != Transparent`
- **Evidence:** `GlassPanel.kt:44–58`:
  ```kotlin
  .shadow(elevation = elevation, shape = shape, ...)
  .then(
      if (glowColor != Color.Transparent)
          Modifier.shadow(elevation = 18.dp, shape = shape, ...)
      else Modifier
  )
  ```
  When `isCompleted = true` on `CinematicExerciseCard`, both shadows are applied, meaning two RenderNode shadow passes for every completed exercise card.
- **Why it's inefficient:** Each `.shadow()` modifier creates a separate `RenderNode` elevation shadow. Two shadows per card multiplied across exercise cards is measurable overdraw.
- **Recommended Fix:**
  Merge into a single shadow call with a blended `spotColor`:
  ```kotlin
  .shadow(
      elevation = elevation,
      shape = shape,
      spotColor = if (glowColor != Color.Transparent) glowColor.copy(0.35f) else Color.Black.copy(0.8f),
      ambientColor = Color.Black.copy(0.4f)
  )
  ```
- **Tradeoffs / Risks:** Slightly different visual output; the glow effect may appear weaker. Can be compensated by increasing the elevation value.
- **Expected Impact:** Halves shadow rendering cost for all `ForgeCard` instances with glow enabled.
- **Removal Safety:** Likely Safe (visual tweak)
- **Reuse Scope:** `GlassPanel.kt` — applies to all card uses

---

### F-12 · Dead `scrollState` Variable in `NewsScreen`
- **Category:** Dead Code / Memory
- **Severity:** Medium
- **Impact:** Unnecessary `ScrollState` object allocated per `NewsScreen` composition
- **Evidence:** `NewsScreen.kt:68`:
  ```kotlin
  val scrollState = rememberScrollState()
  ```
  The outer `NewsScreen` composable uses `LazyColumn` (which has its own scroll state), not `Column` + `verticalScroll`. `scrollState` is never passed to any modifier or scrollable.
- **Why it's inefficient:** `rememberScrollState()` allocates a `ScrollState` object and registers it with Compose's snapshot system, adding unnecessary memory overhead.
- **Recommended Fix:** Remove line 68 entirely.
- **Tradeoffs / Risks:** None.
- **Expected Impact:** Small allocation savings; eliminates dead code confusion.
- **Removal Safety:** Safe
- **Reuse Scope:** `NewsScreen.kt`

---

### F-13 · Redundant Dependency: `converter-gson` (Retrofit) Without Any Retrofit Usage
- **Category:** Build / Cost
- **Severity:** Medium
- **Impact:** Unnecessary APK size increase (~100–150KB); unused dependency in classpath
- **Evidence:** `app/build.gradle.kts:74`:
  ```kotlin
  implementation(libs.converter.gson)
  ```
  Grep of the entire codebase reveals zero usage of Retrofit, `GsonConverterFactory`, or any `@GET`/`@POST`/`@Retrofit` annotations. Firebase is the planned backend and is not yet integrated.
- **Why it's inefficient:** Adds `retrofit2:converter-gson` + Gson to the APK unnecessarily. Also pulls in Retrofit itself (if transitively required). Until a network layer is built, this inflates APK size and confuses build readers.
- **Recommended Fix:** Remove `implementation(libs.converter.gson)` from `app/build.gradle.kts`. Re-add when networking is actually implemented.
- **Tradeoffs / Risks:** None for current functionality.
- **Expected Impact:** ~100–150KB APK size reduction; faster build times marginally.
- **Removal Safety:** Safe
- **Reuse Scope:** `app/build.gradle.kts`

---

### F-14 · Redundant Dependency: `kotlinx-coroutines-play-services` Without Firebase
- **Category:** Build / Cost
- **Severity:** Medium
- **Impact:** Unnecessary APK size; adds Play Services dependency chain
- **Evidence:** `app/build.gradle.kts:71`:
  ```kotlin
  implementation(libs.kotlinx.coroutines.play.services)
  ```
  Firebase Auth and Firestore are commented out in `AppModule.kt`. No `.await()` extension (provided by this library for Play Services Tasks) is called anywhere.
- **Recommended Fix:** Remove until Firebase/Play Services integration is implemented.
- **Tradeoffs / Risks:** None for current functionality.
- **Expected Impact:** Reduced APK size and build graph complexity.
- **Removal Safety:** Safe
- **Reuse Scope:** `app/build.gradle.kts`

---

### F-15 · `material-icons-extended` Hardcoded Outside Version Catalog
- **Category:** Build / Maintainability
- **Severity:** Medium
- **Impact:** Version drift risk; not managed by Compose BOM
- **Evidence:** `app/build.gradle.kts:56`:
  ```kotlin
  implementation("androidx.compose.material:material-icons-extended")
  ```
  All other Compose dependencies use `platform(libs.androidx.compose.bom)` for version management, but this string-literal dependency is unversioned (relying on BOM resolution). If BOM resolution fails or changes, this becomes a transitive conflict.
- **Why it's inefficient:** Unversioned string literals in Gradle files bypass the version catalog. Also note: `material-icons-extended` is a very large artifact (~7MB+). If only a small number of icons are used (e.g., `Icons.Rounded.*`), consider using the standard `material-icons-core` instead.
- **Recommended Fix:**
  Add to `libs.versions.toml`:
  ```toml
  [libraries]
  androidx-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
  ```
  Or, if only core icons are needed, switch to `material-icons-core` (~500KB vs ~7MB).
- **Tradeoffs / Risks:** Switching to `material-icons-core` requires auditing which icons are used (most `Icons.Rounded.*` icons are in core).
- **Expected Impact:** Potentially 5–7MB APK size reduction if switched to core icons.
- **Removal Safety:** Needs Verification (icon audit required)
- **Reuse Scope:** `app/build.gradle.kts`

---

### F-16 · Compose BOM Version `2024.06.00` Is Outdated
- **Category:** Build / Reliability / Performance
- **Severity:** Medium
- **Impact:** Missing performance fixes, bug patches in LazyColumn, Pager, and animation APIs used extensively in this app
- **Evidence:** `gradle/libs.versions.toml:8`:
  ```toml
  composeBom = "2024.06.00"
  ```
- **Why it's inefficient:** Nine months of Compose fixes and optimizations are missed, including improvements to `LazyColumn` scroll performance, `HorizontalPager` (used in `MuseHeroSection`), and `animateFloatAsState` recomposition behavior.
- **Recommended Fix:** Update to the latest stable BOM (as of March 2026). Typical update path:
  ```toml
  composeBom = "2025.xx.xx"
  ```
  Run `./gradlew dependencies` to check for conflicts.
- **Tradeoffs / Risks:** API changes between BOM versions are rare but possible. Test thoroughly after update.
- **Expected Impact:** Qualitative improvement in scroll jank, animation smoothness.
- **Removal Safety:** Needs Verification (test after upgrade)
- **Reuse Scope:** `gradle/libs.versions.toml` — affects all Compose dependencies

---

### F-17 · Massive Legacy Alias Section in `Color.kt` (~50+ Aliases)
- **Category:** Dead Code / Maintainability
- **Severity:** Low
- **Impact:** Build-time constant compilation overhead; developer confusion; increased Kotlin compilation unit size
- **Evidence:** `Color.kt:43–106` — 50+ alias definitions:
  ```kotlin
  val Abyss = Surface0
  val Depth1 = Surface1
  val Emerald300 = Forge300
  val Emerald400 = Forge400
  val EmeraldGlow = AmberGlow   // Emerald = Amber??
  val Black = Void
  ...
  ```
  Confusing aliases like `Emerald500 = Forge500 = Amber` (Emerald is mapped to Amber color values) exist. Some aliases like `BorderSoft = Surface3` are semantically misleading.
- **Why it's inefficient:** These constants increase the Kotlin compilation unit size. While the JVM inlines `val` constants at compile time for primitives, in Compose the color references propagate through the composition tree as `Color` objects. Each unique reference is one more name for readers to mentally parse.
- **Recommended Fix:**
  1. Run a grep for each alias to confirm usage: any alias with 0 usages is safe to delete.
  2. Create a migration guide mapping old names to new, then remove in batches.
  3. Priority removals: `Emerald*` (all map to Amber/Forge, semantically wrong), `Black`, `DarkSurface`, `DarkSurface2`, `Abyss`, `Depth1/2/3` (duplicates of `Surface0/1/2/3`).
- **Tradeoffs / Risks:** Any alias that IS used elsewhere will cause a compile error after removal — which is detectable and easy to fix.
- **Expected Impact:** Cleaner codebase; ~60 fewer constant definitions; faster IDE autocompletion.
- **Removal Safety:** Needs Verification (grep each alias before removing)
- **Reuse Scope:** `Color.kt` — service-wide

---

### F-18 · Multiple Unused Legacy Alias Composable Functions
- **Category:** Dead Code / Maintainability
- **Severity:** Low
- **Impact:** Increased module size; developer confusion about which API to use
- **Evidence:**
  - `GlassPanel.kt`: `GlassPanel`, `GlassCard`, `ObsidianCard`, `ObsidianCardPro` — all delegate to `ForgeCard`
  - `DashboardScreen.kt:129–131`: `ObsidianBackground`, `ForgeBackground`, `MeshBackground` — all delegate to `AppBackground`
  - `DashboardScreen.kt:247–257`: `ObsidianDock`, `ForgeDock` — delegate to `AppNavBar`
  - `AuthScreen.kt:262–267`: `ForgeDramaticButton` — delegate to `ObsidianButton`, marked "Legacy alias"
  - `PremiumButton.kt:178–185`: `GlassButton` — delegates to `GhostButton`
- **Why it's inefficient:** These functions exist only as legacy bridge names. If no external callers exist (single-module app — confirmed by grep), they are pure dead code. They inflate the public API surface and create ambiguity about the canonical component name.
- **Recommended Fix:** Grep each alias for callers. If unused, delete. If used in 1–2 places, update the call site to the canonical name.
- **Tradeoffs / Risks:** Safe deletion after confirming no callers.
- **Expected Impact:** Cleaner component API; reduced binary size marginally.
- **Removal Safety:** Needs Verification (grep for callers)
- **Reuse Scope:** Module-wide

---

### F-19 · `TemplateDetailDialog` Allocates `listOf(...)` Inside `repeat()` Rendering Loop
- **Category:** Memory / Frontend
- **Severity:** Low
- **Impact:** Minor — 3 new list allocations per recomposition of the dialog
- **Evidence:** `ProgramBuilderScreen.kt:352–360`:
  ```kotlin
  repeat(3) { i ->
      ...
      Text(listOf("ÜST VÜCUT", "ALT VÜCUT", "CORE")[i], ...)
      Text(listOf("85%", "40%", "60%")[i], ...)
      Box(...) {
          Box(Modifier.fillMaxWidth(listOf(0.85f, 0.4f, 0.6f)[i]).fillMaxHeight().background(Lime))
      }
  }
  ```
  3 inline lists are created on every recomposition.
- **Recommended Fix:** Extract to top-level constants:
  ```kotlin
  private val MUSCLE_LABELS = listOf("ÜST VÜCUT", "ALT VÜCUT", "CORE")
  private val MUSCLE_PCT_LABELS = listOf("85%", "40%", "60%")
  private val MUSCLE_FRACTIONS = listOf(0.85f, 0.4f, 0.6f)
  ```
- **Removal Safety:** Safe
- **Reuse Scope:** `ProgramBuilderScreen.kt`

---

### F-20 · `NewsScreen.MuseReader` Always Shows the Same Static Body Text for All Articles
- **Category:** Dead Code / Reliability
- **Severity:** Low
- **Impact:** All articles render identical body text; `article.summary` is not used in the reader view
- **Evidence:** `NewsScreen.kt:252–258`:
  ```kotlin
  repeat(4) {
      Text(
          "Profesyonel spor dünyasında adaptasyon döngüsü...",  // always the same!
          ...
      )
      Spacer(Modifier.height(24.dp))
  }
  ```
  The `article.summary` field (set differently for each `Article` in `DEMO_NEWS`) is ignored.
- **Recommended Fix:** Replace the hardcoded lorem-ipsum text with `article.summary` or a content field. At minimum, stop generating 4 copies of the same paragraph.
- **Removal Safety:** Safe (demo data change)
- **Reuse Scope:** `NewsScreen.kt`

---

### F-21 · `WorkoutScreen` Loses Exercise Completion State on Tab Switch
- **Category:** Reliability / Frontend
- **Severity:** Low (for current demo; High for production)
- **Impact:** User's workout progress (which exercises are ticked) resets every time they switch to another tab and back
- **Evidence:** `WorkoutScreen.kt:132–134`:
  ```kotlin
  val dayStates = remember {
      mutableStateListOf(*DEMO_WORKOUTS.map { WorkoutDayState(it) }.toTypedArray())
  }
  ```
  `remember` scopes state to the composable's lifetime. `DashboardScreen` uses `Crossfade`, which destroys the previous tab's composable when switching. When `WorkoutScreen` leaves composition, `dayStates` is discarded.
- **Recommended Fix:** Hoist `dayStates` into a `WorkoutViewModel` so it survives tab navigation. The ViewModel lifecycle is tied to the `NavBackStackEntry` or the `Activity`, not the composable.
- **Tradeoffs / Risks:** Requires creating a `WorkoutViewModel`.
- **Expected Impact:** Critical for production UX; users will not lose their in-session workout progress.
- **Removal Safety:** Needs Verification (architecture change)
- **Reuse Scope:** `WorkoutScreen.kt`, `DashboardScreen.kt`

---

## 3) Quick Wins (Do First)

In order of time-to-implement vs impact:

| # | Finding | Est. Time | Impact |
|---|---------|-----------|--------|
| 1 | **F-04** Remove dead `animateFloatAsState` in `SetRow` | 2 min | High |
| 2 | **F-12** Remove dead `scrollState` in `NewsScreen` | 1 min | Medium |
| 3 | **F-03** Fix `BaseViewModel.updateState` to use `.update {}` | 2 min | High |
| 4 | **F-13** Remove `converter-gson` from `build.gradle.kts` | 1 min | Medium |
| 5 | **F-14** Remove `kotlinx-coroutines-play-services` from `build.gradle.kts` | 1 min | Medium |
| 6 | **F-07** Extract `SimpleDateFormat` to a shared constant | 3 min | Medium |
| 7 | **F-08** Change `drawBehind` to `drawWithCache` in `AuthLiquidField` | 5 min | Medium |
| 8 | **F-10** Consolidate 3 `rememberInfiniteTransition` into 1 in `SanctuaryTypingIndicator` | 5 min | Medium |
| 9 | **F-19** Extract `listOf(...)` constants in `TemplateDetailDialog` | 3 min | Low |
| 10 | **F-02** Wire ViewModel to auth button + observe state for navigation | 15 min | Critical |

---

## 4) Deeper Optimizations (Do Next)

These require more planning or touch architecture:

### A. Replace `AtmosphericBloom` with a Static Pre-rendered Gradient (F-01)
Redesign the background bloom as a static `drawWithCache` gradient. If subtle animation is desired, animate only the overall `alpha` of the entire composable (not per-element) to minimize recomposition scope.

### B. Hoist `WorkoutScreen` State into a ViewModel (F-21)
Create a `WorkoutViewModel` with a `StateFlow<List<WorkoutDayState>>`. This also enables future persistence (DataStore, Room) and makes the workout screen testable.

### C. Audit and Purge Color.kt Aliases (F-17)
Run a codebase-wide grep for each alias name. Prepare a PR that removes all zero-usage aliases in one shot. Semantically incorrect aliases (Emerald = Amber) should be removed regardless of usage count.

### D. Switch `material-icons-extended` → `material-icons-core` (F-15)
Audit which specific icons are used across all screens. Most `Icons.Rounded.*` icons referenced (FitnessCenter, CalendarMonth, AutoAwesome, Newspaper, Person, Email, Lock, CheckCircle, RadioButtonUnchecked, Timer, Hotel, ChevronRight, ArrowBack, Share, Add, ViewQuilt, Construction, Draw, NorthEast, Logout, Security, Language, Notifications) exist in `material-icons-core`. If confirmed, switching saves ~5–7MB from the APK.

### E. Implement Firebase Auth Backend (replaces F-02 demo mode)
The DI module, ViewModel, and state model are already scaffolded. Once Firebase Auth is connected, the ViewModel integration from F-02 is immediately production-ready.

### F. Update Compose BOM (F-16)
Schedule a dependency update sprint. Update BOM, run the app on multiple screen sizes, check for visual/behavior regressions. Priority: `LazyColumn` scrolling, `HorizontalPager` swiping (used in News hero), animation callbacks.

### G. Consolidate `ForgeCard` API Surface (F-18)
Delete all unused alias composables (`GlassPanel`, `GlassCard`, `ObsidianCard`, `ObsidianBackground`, `ForgeDock`, etc.). Standardize on the `ForgeCard*` naming. This reduces the cognitive load for new developers joining the project.

---

## 5) Validation Plan

### Benchmarks
- **Frame rate baseline:** Use Android Studio Profiler → "Frame rendering" on the AICoach tab with `AtmosphericBloom` active. Record: average frame time, dropped frames per 10 seconds.
- **After F-01 fix:** Re-measure. Target: < 8ms average frame time (down from likely 16–25ms on mid-range devices).
- **APK size:** Run `./gradlew assembleRelease` and compare `app-release.apk` size before/after removing `converter-gson` and `material-icons-extended`.

### Profiling Strategy
1. **Android Profiler → CPU → System Trace** on the Workout screen: record while expanding exercise cards. Look for `RenderThread` spikes (shadow + blur).
2. **Android Profiler → Memory** on AICoach screen after 10 messages: confirm `ChatMessage` allocation count, look for growing `SimpleDateFormat` instances.
3. **Compose Layout Inspector** on DashboardScreen: verify `rememberInfiniteTransition` count (should decrease from F-01, F-04, F-05, F-10 fixes).
4. **Slow rendering warnings** (`adb shell dumpsys gfxinfo com.avonix.profitness`): count janky frames before/after.

### Metrics to Compare Before/After
| Metric | Before | Target After |
|--------|--------|--------------|
| AICoach screen avg frame time | Measure | < 8ms |
| AICoach screen dropped frames/10s | Measure | < 2 |
| Release APK size | Measure | −5–8MB (icons) + −150KB (retrofit) |
| `WorkoutDayState` survive tab switch | No | Yes (after F-21) |
| Auth ViewModel logic executed | Never | On every login attempt |

### Test Cases for Correctness
- [ ] Auth: tapping login with invalid email does NOT navigate to dashboard
- [ ] Auth: tapping login with valid credentials DOES navigate and shows loading state
- [ ] AICoach: response counter cycles through all 3 responses before repeating
- [ ] Workout: exercise checked as complete → switch to Profile tab → switch back → completion state preserved
- [ ] WorkoutScreen SetRow: toggling a set does not trigger any scale animation (scale = 1f always was bug)
- [ ] RestTimerChip: countdown timer visually runs; stops on tap; does not drain battery on idle
- [ ] News reader: tapping different articles shows different content (after F-20 fix)

---

## 6) Optimized Code / Patch

### Patch 1 — `BaseViewModel.kt`: Atomic state update
```kotlin
// BEFORE:
protected fun updateState(update: (State) -> State) {
    _uiState.value = update(_uiState.value)
}

// AFTER:
protected fun updateState(update: (State) -> State) {
    _uiState.update(update)
}
```
**What changed:** `.update {}` is an atomic compare-and-swap; eliminates the read-modify-write race condition.

---

### Patch 2 — `CinematicExerciseCard.kt`: Remove dead `scale` animation in `SetRow`
```kotlin
// BEFORE:
val scale by animateFloatAsState(
    if (isDone) 1f else 1f,         // ← dead animation
    spring(Spring.DampingRatioMediumBouncy),
    label = "set_scale"
)
val bgAlpha by animateFloatAsState(...)

// AFTER: (remove scale entirely, keep bgAlpha)
val bgAlpha by animateFloatAsState(
    if (isDone) 0.15f else 0.08f,
    tween(300),
    label = "set_bg"
)
```
**What changed:** Removed a zero-effect animation. `bgAlpha` is preserved (it does change value: 0.08 → 0.15).

---

### Patch 3 — `AICoachScreen.kt`: Fix `responseCounters` + `SimpleDateFormat`
```kotlin
// BEFORE (top-level):
private var responseCounters = mutableMapOf<String, Int>()

data class ChatMessage(
    ...
    val timestamp: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
)

// AFTER:
private val MESSAGE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")  // thread-safe, API 26+

data class ChatMessage(
    ...
    val timestamp: String = java.time.LocalTime.now().format(MESSAGE_FORMATTER)
)

// Inside AICoachScreen composable:
val responseCounters = remember { mutableStateMapOf<String, Int>() }
// Pass responseCounters into getResponse() or use closure capture
```
**What changed:**
1. `SimpleDateFormat` replaced with thread-safe `DateTimeFormatter` (minSdk = 31, API 26 is safe).
2. `responseCounters` moved from global mutable state into composable-scoped `mutableStateMapOf`.

---

### Patch 4 — `NewsScreen.kt`: Remove dead `scrollState`
```kotlin
// BEFORE (line 68):
val scrollState = rememberScrollState()   // ← never used

// AFTER: delete line 68 entirely
```

---

### Patch 5 — `app/build.gradle.kts`: Remove unused dependencies
```kotlin
// REMOVE these lines:
implementation(libs.converter.gson)
implementation(libs.kotlinx.coroutines.play.services)

// KEEP for future use, comment out until Firebase is integrated:
// implementation(libs.converter.gson)            // TODO: restore when Retrofit is used
// implementation(libs.kotlinx.coroutines.play.services)  // TODO: restore when Firebase Auth is added
```

---

### Patch 6 — `AuthScreen.kt`: Wire ViewModel to login button
```kotlin
// BEFORE:
ObsidianButton(
    text = if (isLogin) "GİRİŞ YAP" else "KAYIT OL",
    onClick = onNavigateToDashboard,
    modifier = Modifier.fillMaxWidth()
)

// AFTER:
ObsidianButton(
    text = if (isLogin) "GİRİŞ YAP" else "KAYIT OL",
    onClick = {
        if (isLogin) viewModel.onLoginClick(email, password)
        else viewModel.onRegisterClick(email, password)
    },
    modifier = Modifier.fillMaxWidth()
)

// Add above the Box layout:
LaunchedEffect(state.isSuccess) {
    if (state.isSuccess) onNavigateToDashboard()
}
// Optionally show state.error:
state.error?.let { err ->
    Text(err, color = CriticalRed, modifier = Modifier.padding(0.dp, 8.dp))
}
```

---

### Patch 7 — `AuthScreen.kt`: Replace `drawBehind` with `drawWithCache` in `AuthLiquidField`
```kotlin
// BEFORE:
.drawBehind {
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(Color.Transparent, AmberRimLight.copy(0.25f), Color.Transparent)
        ),
        size = Size(size.width, 1.dp.toPx())
    )
}

// AFTER:
.drawWithCache {
    val rimBrush = Brush.horizontalGradient(
        listOf(Color.Transparent, AmberRimLight.copy(0.25f), Color.Transparent)
    )
    val rimHeight = 1.dp.toPx()
    onDrawBehind {
        drawRect(brush = rimBrush, size = Size(size.width, rimHeight))
    }
}
```
**What changed:** Brush is created once per size change, not per draw call.
