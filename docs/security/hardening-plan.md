# Profitness Security Hardening Plan

Last updated: 2026-05-17

## Guiding principle

Security changes must not make an existing product flow worse. A fix is acceptable only when the protected flow still works for the user, dependent flows keep their contract, and rollout has a clear verification and rollback path.

This means:

- Do not remove a key, redirect, webhook path, or RPC behavior unless the replacement path is already wired, tested, and deployable.
- Prefer staged migrations and feature-compatible changes over one-shot breaking flips.
- Keep mobile-public values public by design: `SUPABASE_URL`, `SUPABASE_ANON_KEY`, and the Android-restricted Maps key are not treated as critical secrets.
- Never print or commit local secret values.

## Current access check

- Supabase MCP access was verified on 2026-05-13 with a read-only table/RLS inventory query.
- Supabase SQL access was re-verified on 2026-05-15 after revoking the old local personal access token.
- Supabase SQL execution was verified with metadata-only function and migration-history queries.
- The hardening migration was applied to the remote database on 2026-05-16 as `harden_security_controls`.
- Remote keeps the old `apply_paid_billing_order(p_order_id, p_provider, p_provider_session_id)` signature for compatibility and also has the stricter `apply_paid_billing_order_verified(...)` path for signed billing flows.

## Completed locally

- Sanitized local `.mcp.json` by removing the Supabase MCP access-token argument without printing the value.
- Removed the stale local `GEMINI_API_KEY` entry from `local.properties` without printing the value.
- Kept `.mcp.json` and `local.properties` untracked/ignored.
- Changed GitHub Actions from debug APK release to release AAB generation.
- Added production release signing requirements with `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD`.
- Hardened GitHub Actions and Gradle release signing secret parsing so accidental copied newlines in GitHub secrets do not break release keystore lookup.
- Verified GitHub Actions release build succeeded on commit `f730544`; it produced GitHub Release `v14.0` with a release-signed AAB.
- Captured Android release signing SHA-256 for App Links: `A5:90:AE:C9:93:5F:56:DF:A1:28:65:05:61:2C:B0:DB:F8:65:A5:F8:C0:DE:A7:A1:AA:B2:DC:DD:CA:CC:7F:E2`.
- Preserved phone testing by adding an internal-test APK artifact. It is release-signed, produced on push builds, and uploaded as a short-lived workflow artifact, not published as a public debug APK release.
- Stopped publishing ProGuard/R8 `mapping.txt` files as GitHub Release assets. Mapping files are useful for crash deobfuscation but should not be public release downloads for this public repository.
- Added Android App Links support for password reset.
- Kept legacy `profitness://reset-password` handling during rollout so already-issued recovery emails do not break.
- Disabled Android backup with `android:allowBackup="false"`.
- Reworked `billing-webhook` to verify raw body HMAC signatures with timestamp tolerance and constant-time comparison.
- Kept an explicitly configurable legacy static-secret fallback for staged provider rollout; disable it with `BILLING_WEBHOOK_ALLOW_LEGACY_SECRET=false` after the provider sends HMAC signatures.
- Added webhook event replay/idempotency behavior via `billing_webhook_events`.
- Added paid-order SKU and amount assertions before entitlement application.
- Added strict AI idempotency behavior so one idempotency key cannot launch parallel Gemini calls.
- Restricted accepted AI tools and inline media use at the Edge Function boundary.
- Hardened the legacy `gemini-generate` Edge Function so old clients remain compatible but also pass through the same credit/idempotency reservation path.
- Added Android-side AI upload size and MIME checks for JPEG, PNG, WebP, and PDF.
- Added online event URL normalization so only safe `http`/`https` links can be stored/opened.
- Removed remaining `anon` table grants found on app public tables.
- Removed anonymous execute from public RPC wrappers that require an authenticated user.
- Tightened private-schema function execute defaults so internal helpers are not callable through default PUBLIC grants.
- Added profile photo upload MIME/size checks before reading the file into memory.
- Added a database constraint that allows avatar emoji/text values or app-owned `profile-photos` Storage URLs, but rejects external HTTP avatar URLs.
- Added a DB migration to harden `_challenge_my_progress`, `apply_paid_billing_order`, and `reserve_ai_usage`.
- Applied the hardening migration to Supabase production and deployed updated `billing-webhook`, `billing-sandbox-complete`, and `ai-generate` Edge Functions.

## Open rollout tasks

| Priority | Task | Status | Notes |
| --- | --- | --- | --- |
| Critical | Rotate the Supabase access token that was present in `.mcp.json` | Done | User revoked the old token in Supabase; local `.mcp.json` no longer stores token-like args. |
| High | Add Android release signing secrets to GitHub Actions | Done | User added `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD`. |
| High | Verify GitHub release signing workflow | Done | Release workflow succeeded on commit `f730544` and published `v14.0`. |
| High | Publish Android App Links `assetlinks.json` | Done | `https://cosmibit.com/.well-known/assetlinks.json` returns the expected JSON with `application/json`. |
| High | Add HTTPS reset redirect to Supabase Auth URL Configuration | Done | `https://cosmibit.com/reset-password` is allowed. Do not remove the legacy redirect until one release cycle passes. |
| High | Set GitHub reset link host config | Done | `RESET_PASSWORD_LINK_HOST=cosmibit.com` and `RESET_PASSWORD_REDIRECT_URL=https://cosmibit.com/reset-password` were added; workflow accepts either repository variable or secret. |
| High | Configure provider webhook to sign `timestamp.rawBody` with HMAC-SHA256 | Pending provider integration | Headers expected: `x-webhook-timestamp`, `x-webhook-signature`, and stable event id via body or `x-webhook-id`; then set `BILLING_WEBHOOK_ALLOW_LEGACY_SECRET=false`. |
| High | Add provider amount metadata to products if numeric amount checks are required | Pending provider integration | Existing label check is backward-compatible; `metadata.amount_minor` and `metadata.currency` make it stricter. |
| Medium | Remove old public mapping release assets | Pending GitHub cleanup | Existing `v14.0`, `v14.1`, and `v14.2` releases still expose `Profitness_v14.x-mapping.txt`. Keep AAB assets; delete only mapping assets. Future releases no longer publish mapping files. |
| High | Apply and verify `harden_security_controls` migration in Supabase | Done | Remote migration history shows `20260515212124_harden_security_controls`. Function grants were verified after apply. |
| Medium | Deploy updated Edge Functions | Done | `billing-webhook` v1, `billing-sandbox-complete` v2, `ai-generate` v4, and `gemini-generate` v6 are active. |
| Medium | Tighten private schema function grants | Done | Internal helpers remain callable for authenticated app flows; default PUBLIC/anon execute was removed. |
| Medium | Restrict profile photo/avatar input | Done | Mobile upload preflight checks file type/size; DB rejects external HTTP avatar URLs. |
| Medium | Remove legacy custom scheme reset filter | Deferred | Only after all active reset emails and deployed clients have moved to verified HTTPS links. |
| Medium | Finalize privacy policy and terms | Deferred until final branding | A professional Turkish privacy policy base exists at `docs/legal/privacy-policy.md`; finalize app name, publisher name, contact email, privacy policy, and terms together before store release. |

## Safe rollout sequence

1. Rotate the old Supabase access token.
2. Create or use a Supabase staging/dev branch.
3. Apply `20260513073048_harden_security_controls.sql` to staging.
4. Deploy updated Edge Functions to staging.
5. Run integration tests:
   - password reset with HTTPS App Link
   - old `profitness://reset-password` recovery link still opens during rollout
   - billing paid webhook with valid HMAC succeeds
   - billing webhook with stale timestamp fails
   - duplicate webhook event returns duplicate/ok without double entitlement
   - SKU mismatch fails
   - amount mismatch fails
   - same AI `idempotencyKey` in parallel yields one provider call and one conflict
- challenge detail/listing still shows progress for visible participants
   - profile photo upload accepts normal JPEG/PNG/WebP images and rejects oversized/unsupported files without crashing
6. Publish production `assetlinks.json`.
7. Add the HTTPS reset URL to Supabase Auth URL Configuration.
8. Set GitHub release variables for reset link host and redirect URL.
9. Apply DB migration to production.
10. Deploy Edge Functions to production.
11. Build/release AAB through GitHub Actions.
12. Monitor Supabase Auth, API, Postgres, and Edge Function logs for at least one release window.
13. Remove legacy custom-scheme handling in a later release after confirming no active dependency remains.

Steps 1, 6, 7, 8, 9, and 10 are complete for the current rollout.

## Flow contracts that must not regress

- Sign in/sign up/email verification behavior remains unchanged.
- Password reset must still route to `ResetPasswordScreen` and exchange the recovery code.
- Existing recovery emails using the custom scheme must keep working during migration.
- AI generation must return the same successful response shape for first requests.
- AI duplicate idempotency requests may return `409`; the client should treat this as "already in progress/completed" instead of spending more credits.
- Billing sandbox completion must still work when `BILLING_SANDBOX_ENABLED` is true.
- Paid billing must never grant entitlement unless order id, SKU, and amount assertion match.
- Challenge list/detail progress must remain visible for users allowed to read the challenge.
- Release artifacts must be signed with the production release key, not debug signing.
- Manual phone-test APKs must remain available through GitHub Actions artifacts, but they must be release-signed and short-lived.
- ProGuard/R8 mapping files must not be published as public GitHub Release assets. If crash deobfuscation is needed, keep them in a restricted location such as Play Console or private build storage.

## Verification already run locally

- `git diff --check` passed for touched files; only line-ending warnings were reported.
- `npx deno check --node-modules-dir=auto` passed for:
  - `supabase/functions/billing-webhook/index.ts`
  - `supabase/functions/ai-generate/index.ts`
  - `supabase/functions/billing-sandbox-complete/index.ts`
- Static grep confirmed the old debug release path and static webhook header are gone from the touched release/webhook paths.
- Supabase MCP read access and SQL metadata queries were successful.
- Remote Supabase migration history includes `harden_security_controls`.
- Remote Edge Function versions verified active:
  - `billing-webhook` v1 with JWT disabled intentionally for provider webhooks.
  - `billing-sandbox-complete` v2 with JWT enabled.
  - `ai-generate` v4 with JWT enabled.
  - `gemini-generate` v6 with JWT enabled and legacy compatibility through the current AI credit gate.
- Endpoint smoke checks returned expected `401` responses for unauthenticated/unsigned requests to `billing-webhook` and `ai-generate`.
- Local secret hygiene was rechecked without printing values: `local.properties` contains only expected mobile-public/API config keys, and `.mcp.json` has no token-like value.
- Supabase metadata checks on 2026-05-17 confirmed all public/storage tables have RLS enabled and no public/private views are exposed.
- GitHub Actions release workflow `25941770568` passed and built the release AAB in 7m 48s.
- GitHub Actions workflow `25942884716` produced the release-signed phone-test APK artifact `profitness-internal-test-apk`.
- GitHub Releases were checked on 2026-05-16: `v14.0`, `v14.1`, and `v14.2` still contain public `mapping.txt` assets from earlier runs.
- `https://cosmibit.com/.well-known/assetlinks.json` was checked on 2026-05-17 and returned the expected Android App Links JSON with `application/json`.

## Current verification blockers

- Local Supabase database is not running on `127.0.0.1:54322`, so `npx supabase migration list --local` could not verify local migration state.
- Old public GitHub Release mapping assets should be deleted for `v14.0`, `v14.1`, and `v14.2`; the workflow has already been changed so future releases do not add new ones.
- Supabase advisors still report existing non-blocking notices: RLS-enabled internal tables without public policies, one mutable search-path trigger helper, and Auth leaked-password protection disabled.

## Rollback strategy

- Android reset rollout: keep legacy custom-scheme support until HTTPS App Links are proven. If HTTPS configuration fails, set `RESET_PASSWORD_REDIRECT_URL` back to `profitness://reset-password` while fixing domain/Supabase configuration.
- Billing webhook: deploy the HMAC-capable function with legacy fallback enabled first, configure the provider to send HMAC headers, verify signed events, then disable `BILLING_WEBHOOK_ALLOW_LEGACY_SECRET`.
- DB migration: test on staging/branch first. If production issues appear, restore the previous function definitions from migration history and redeploy the previous Edge Function versions.
- CI release: if release signing secrets are missing, the build fails before publishing an artifact. This is intentional and safer than publishing debug-signed builds.
