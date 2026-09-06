# UI redesign acceptance ledger

The three user briefs are authoritative. Earlier experiments and historical memory entries are not acceptance evidence. Nothing is complete merely because it compiles.

## Material contract

- Utility: opaque, quiet, no decorative lighting.
- Elevated: opaque face, continuous tonal light, at most one restrained shadow. No top strip, orange outline, blur or animated glow.
- Signature: elevated material with one broad static low-opacity light spill; never every card.
- Controls: shallow inward depth; no raised pedestal or orange lower ledge.
- Floating chrome: readable opaque fallback until optical glass is visually proven. No content visible through the composer.
- Existing data, routes, timers, selections and useful transitions remain intact.

## Coverage

| Flow | Shared foundation / source audit | Remaining acceptance checks |
|---|---|---|
| Auth, reset password | Shared palette, CTA, inset inputs | Error, loading, keyboard, large font |
| Onboarding | Shared palette, selected controls | All steps, validation, back behavior |
| Workout | Elevated list, explicit set/reps or timed target, detail-only imagery, inset day selection | Expanded activity/strength/timed sets, timers |
| Exercise detail | Shared palette, aspect-fit media | Multiple media, empty media, safe area |
| Program Studio | Single creation control, create sheet, compact filters, elevated program cards | Ready/manual/AI/edit/save/error paths |
| Exercise pickers | Shared controls and list material | Search, selection, manual entry, keyboard |
| Oracle setup | Shared input/control foundation | All settings and validation |
| Oracle conversation | Single compact program summary, one primary action, quiet composer, real active-program counts; duplicated energy copy and initial suggestion chips removed | Recovery/readiness are not calculated; do not invent them. History, keyboard and error-state QA remain |
| Profile | Signature hero, unboxed XP and stat row, shared metrics, readable metadata sizing | Settings sheets, long names |
| Edit profile | Shared profile surface | Avatar, save/loading/error/keyboard |
| Performance detail | Shared profile surface | Calculators, input states, charts, large font |
| Exercise progression | Shared profile surface | Empty/chart/history filters |
| Achievements | Shared profile surface | Locked/unlocked/detail states |
| Public profile | Shared profile surface | Follow, activity, challenge, empty/private states |
| Weight tracking | Shared surface foundation | Chart, log, edit, insight/error/input |
| Discover/share | Shared cards, controls, apply action | Tabs, filters, reactions, share form |
| Friends | Elevated rows, inset search | Search, follow, empty/error |
| Leaderboard | Shared elevated/inset surfaces | Tabs, own position, empty/error |
| Challenges | Shared cards, reduced filter controls | Joined/discover, filters, status states |
| Challenge create/detail | Shared surfaces, inset form controls, unified CTAs, quiet dialogs | Invitation, join/results and validation states |
| Store | Shared active offer/energy surfaces | Paywall, purchase/cancel/pending/error; do not purchase during QA |
| Navigation/global overlays | Opaque dock, inset active item, preserved gesture | Every tab, keyboard, safe area, timers/toasts |

## Verification

Latest debug APK build, lint and unit tests succeeded. Three Oracle summary unit tests pass (unloaded/loading, loaded empty, stale completion IDs). The installed APK was launched on `emulator-5554`; dark workout/profile and light workout/profile/Oracle/Program Studio were inspected. Current evidence is in `build/profitness-final-qa.png`, `build/profile-qa2.png`, `build/profile-light-final.png`, `build/workout-light-final2.png`, and `build/program-light-final.png`. A short emulator timing sample is not a release-performance claim. Remaining rows must not be marked complete from shared-token propagation alone.

## Soft Glass Hardware revision — 2026-09-05

> Superseded in part by direct user feedback: the `9daf7d1` orb/frosted navigation looked like cheap fake glass and was not the remembered premium version. Current navigation follows the `2bd8570` sliding-indicator structure with an opaque sculpted dock. Content/floating surfaces are opaque rather than pretending to blur. The circular workout completion ring is restored.

- New user reference supersedes the overly-flat matte interpretation: use broad off-canvas ambient light, translucent soft-glass/metal faces, and a shallow inward press response. Do not render literal glow circles in the page background.
- Historical commit `9daf7d1` was inspected as the accepted premium navigation reference. Its frosted shell, top reflection, inner depth, accent bleed, 3D orb and expanding selected label were adapted to the current navigation/translation/drag contracts.
- Workout now uses an editorial protocol header, linear completion rail and numbered exercise rows. Program Studio and profile hero use tighter geometry and reduced vertical bulk.
- Latest `compileDebugKotlin`, `assembleDebug`, `testDebugUnitTest` and `lintDebug` succeeded. Installed dark-mode evidence: `build/profitness-glass-nav2.png`, `build/profitness-program-redesign.png`, and `build/profitness-profile-redesign2.png`.

## Premium gradient/material convergence — 2026-09-06

- Historical source audit covered the immediately preceding `v17.9`/`v17.8` material system and the older `279f108` expanding-label navigation. The current implementation combines their useful geometry/motion without restoring the rejected frosted orb.
- Navigation is again a compact floating capsule. Only the active tab expands to expose its label; inactive tabs remain icon-only. The active face uses a restrained accent reflection, thin top light and one shadow instead of a full-width gray dock or literal glowing circle.
- Page lighting uses two static, off-canvas radial sources. Their centers cannot appear as on-screen circles. Shared elevated surfaces use a continuous graphite/opal face, broad key light and shallow lower depth in one cached pass.
- Workout keeps circular completion progress. The day selector is an inset rail with a sculpted selected face; exercise cards use the same material family and a dimensional index plaque.
- Emulator evidence: `build/profitness-premium-v3.png`, `build/profitness-program-v3.png`, `build/profitness-ai-v3.png`, `build/profitness-profile-v3.png`, `build/profitness-light-v3.png`.
- Verified: `:app:compileDebugKotlin`, `:app:assembleDebug`, `:app:testDebugUnitTest`, and `:app:lintDebug`. This does not close the remaining state-by-state coverage rows above.

## Contrast, icon and affordance revision — 2026-09-06

- User screenshot feedback supersedes the low-contrast gray treatment. Dark canvas/surfaces now use a blue-black near-black hierarchy; elevated cards add one restrained gradient rim so separation does not depend on near-identical fill colors.
- Global type roles are heavier and tighter. Primary navigation and visible profile metrics use filled icons; key icons sit in deliberate plates instead of floating as small gray glyphs.
- Oracle header actions are 42dp sculpted controls. Its composer is an inset surface with an Oracle plate and an always-present 44dp filled send control.
- Manual saved programs no longer repeat a `MANUEL` badge. Activate is a true primary button; edit/share/more are 40dp controls. Exercise cards expose a 46dp play control and an expanded full-width `Hareketi gör` action.
- Compile, assemble, unit tests and lint pass. Fresh emulator screenshots are still required: the previous emulator stopped and restart failed at WHPX setup with Windows access error `0x80070005`. Earlier screenshots must not be used as evidence for this revision.
