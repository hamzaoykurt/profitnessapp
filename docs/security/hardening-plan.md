# Profitness Security Hardening Plan

Last updated: 2026-05-15

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
- The new hardening migration in this repo is not applied to the remote database yet.
- Remote currently still has the old `apply_paid_billing_order(p_order_id, p_provider, p_provider_session_id)` signature and `public._challenge_my_progress(p_challenge_id, p_user_id)` as security definer.

## Completed locally

- Sanitized local `.mcp.json` by removing the Supabase MCP access-token argument without printing the value.
- Removed the stale local `GEMINI_API_KEY` entry from `local.properties` without printing the value.
- Kept `.mcp.json` and `local.properties` untracked/ignored.
- Changed GitHub Actions from debug APK release to release AAB generation.
- Added production release signing requirements with `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD`.
- Preserved phone testing by adding a manual internal-test APK artifact. It is release-signed and uploaded as a short-lived workflow artifact, not published as a public debug APK release.
- Added Android App Links support for password reset.
- Kept legacy `profitness://reset-password` handling during rollout so already-issued recovery emails do not break.
- Disabled Android backup with `android:allowBackup="false"`.
- Reworked `billing-webhook` to verify raw body HMAC signatures with timestamp tolerance and constant-time comparison.
- Kept an explicitly configurable legacy static-secret fallback for staged provider rollout; disable it with `BILLING_WEBHOOK_ALLOW_LEGACY_SECRET=false` after the provider sends HMAC signatures.
- Added webhook event replay/idempotency behavior via `billing_webhook_events`.
- Added paid-order SKU and amount assertions before entitlement application.
- Added strict AI idempotency behavior so one idempotency key cannot launch parallel Gemini calls.
- Added a DB migration to harden `_challenge_my_progress`, `apply_paid_billing_order`, and `reserve_ai_usage`.

## Open rollout tasks

| Priority | Task | Status | Notes |
| --- | --- | --- | --- |
| Critical | Rotate the Supabase access token that was present in `.mcp.json` | Done | User revoked the old token in Supabase; local `.mcp.json` no longer stores token-like args. |
| High | Add Android release signing secrets to GitHub Actions | Done | User added `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD`. |
| High | Publish Android App Links `assetlinks.json` | Pending | Domain selected: `cosmibit.com`. Use the release SHA-256 fingerprint from CI. |
| High | Add HTTPS reset redirect to Supabase Auth URL Configuration | Pending | Add exact `https://cosmibit.com/reset-password`. Do not remove the legacy redirect until one release cycle passes. |
| High | Set GitHub reset link host config | Done | `RESET_PASSWORD_LINK_HOST=cosmibit.com` was added; workflow accepts either repository variable or secret. Redirect still safely falls back to the legacy reset redirect until `RESET_PASSWORD_REDIRECT_URL` is set. |
| High | Configure provider webhook to sign `timestamp.rawBody` with HMAC-SHA256 | Pending provider integration | Headers expected: `x-webhook-timestamp`, `x-webhook-signature`, and stable event id via body or `x-webhook-id`; then set `BILLING_WEBHOOK_ALLOW_LEGACY_SECRET=false`. |
| High | Add provider amount metadata to products if numeric amount checks are required | Pending provider integration | Existing label check is backward-compatible; `metadata.amount_minor` and `metadata.currency` make it stricter. |
| High | Apply and verify `harden_security_controls` migration in Supabase | Pending approval | Apply after testing on staging/branch or during a controlled maintenance window. |
| Medium | Deploy updated Edge Functions | Pending approval | Deploy `billing-webhook`, `ai-generate`, and `billing-sandbox-complete` together with the DB migration. |
| Medium | Remove legacy custom scheme reset filter | Deferred | Only after all active reset emails and deployed clients have moved to verified HTTPS links. |

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
6. Publish production `assetlinks.json`.
7. Add the HTTPS reset URL to Supabase Auth URL Configuration.
8. Set GitHub release variables for reset link host and redirect URL.
9. Apply DB migration to production.
10. Deploy Edge Functions to production.
11. Build/release AAB through GitHub Actions.
12. Monitor Supabase Auth, API, Postgres, and Edge Function logs for at least one release window.
13. Remove legacy custom-scheme handling in a later release after confirming no active dependency remains.

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

## Verification already run locally

- `git diff --check` passed for touched files; only line-ending warnings were reported.
- `npx deno check --node-modules-dir=auto` passed for:
  - `supabase/functions/billing-webhook/index.ts`
  - `supabase/functions/ai-generate/index.ts`
  - `supabase/functions/billing-sandbox-complete/index.ts`
- Static grep confirmed the old debug release path and static webhook header are gone from the touched release/webhook paths.
- Supabase MCP read access and SQL metadata queries were successful.

## Current verification blockers

- Local Supabase database is not running on `127.0.0.1:54322`, so `npx supabase migration list --local` could not verify local migration state.
- Android debug Kotlin compilation currently fails in `MainActivity` import/symbol resolution for theme/navigation symbols. This predates the security rollout work in the dirty working tree and should be resolved before shipping.

## Rollback strategy

- Android reset rollout: keep legacy custom-scheme support until HTTPS App Links are proven. If HTTPS configuration fails, set `RESET_PASSWORD_REDIRECT_URL` back to `profitness://reset-password` while fixing domain/Supabase configuration.
- Billing webhook: deploy the HMAC-capable function with legacy fallback enabled first, configure the provider to send HMAC headers, verify signed events, then disable `BILLING_WEBHOOK_ALLOW_LEGACY_SECRET`.
- DB migration: test on staging/branch first. If production issues appear, restore the previous function definitions from migration history and redeploy the previous Edge Function versions.
- CI release: if release signing secrets are missing, the build fails before publishing an artifact. This is intentional and safer than publishing debug-signed builds.
