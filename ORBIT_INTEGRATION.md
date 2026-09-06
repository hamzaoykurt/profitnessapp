# Orbit Personal OS — Optional Fitness Sync

Fitness remains the workout source of truth. Orbit receives only a recomputed summary after the user explicitly links an authorized, entitled Orbit account and enables sync.

## Data flow

1. `Settings → Integrations → Orbit Personal OS` requests a short-lived signed link state.
2. Orbit completes account authorization and calls `orbit-fitness-callback` with an HMAC-signed payload.
3. The callback consumes the state once, stores no Orbit token in the Android app, and accepts entitlement only from Orbit.
4. A Fitness completion/correction is saved locally first. A coalesced background task pushes normal Fitness records, then calls `orbit-fitness-integration`.
5. The Edge Function rebuilds the weekly summary from actual `workout_logs`, `exercise_logs`, and the active program. It never increments a separate counter.

## Server configuration

Deploy migration `20260906120000_orbit_fitness_integration.sql` and both Edge Functions. Configure these Edge Function secrets:

- `ORBIT_FITNESS_CONNECT_URL`: Orbit authorization entrypoint.
- `ORBIT_FITNESS_SYNC_URL`: Orbit server endpoint receiving the minimal summary.
- `ORBIT_FITNESS_DISCONNECT_URL`: optional server-side revocation endpoint.
- `ORBIT_FITNESS_SERVER_SECRET`: server-to-server bearer secret.
- `ORBIT_FITNESS_REQUEST_SECRET`: Fitness → Orbit body/timestamp HMAC signing secret.
- `ORBIT_FITNESS_STATE_SECRET`: signs short-lived, single-use link state.
- `ORBIT_FITNESS_WEBHOOK_SECRET`: verifies `timestamp.raw_body` HMAC-SHA256 callbacks.

Production Orbit endpoint contract:

- Connect: `https://<orbit-host>/api/integrations/profitness/connect`
- Sync: `https://<orbit-host>/api/integrations/profitness/sync`
- Disconnect: `https://<orbit-host>/api/integrations/profitness/disconnect`

`SERVER`, `REQUEST`, and `WEBHOOK` secrets are separate credentials. Values belong only in Supabase Edge Function secrets and Cloudflare Worker secrets; they must never be added to Android `BuildConfig`, source control, logs, screenshots, or chat.

The Orbit callback sends `x-webhook-timestamp` and `x-webhook-signature`, plus:

```json
{
  "eventId": "stable-event-id",
  "type": "connection.updated",
  "state": "state returned by the connect URL",
  "fitnessUserId": "Fitness user id bound inside the signed state",
  "orbitAccountId": "orbit-account-id",
  "accountLabel": "name@example.com",
  "authorized": true,
  "fitnessSyncEntitled": true,
  "manageUrl": "https://orbit.example/settings/integrations"
}
```

Later entitlement/revocation events may omit `state`; the signed webhook is mapped by the already linked `orbitAccountId`.

## Verification matrix

| # | Scenario | Expected behavior |
|---|---|---|
| 1 | Fitness-only user | No Orbit dependency or request during normal startup/workouts after status is known. |
| 2 | Orbit not connected | Settings shows Not connected; workout works normally. |
| 3 | Connected, no entitlement | Sync switch is disabled; server rejects attempts. |
| 4 | Connected, entitled, enabled | Summary delivery is allowed. |
| 5 | Complete today | Fitness saves first; Orbit receives recomputed today/week status. |
| 6 | Multiple workouts | Completed sessions are counted from distinct real workout records. |
| 7 | Uncomplete/correct | A fresh summary replaces the old derived value; no decrement counter exists. |
| 8 | Orbit unavailable | Fitness succeeds; integration records a non-intrusive temporary error. |
| 9 | Disconnect | Link and pending states are removed; Fitness history remains. |
| 10 | Reconnect | User-key upsert restores one link and recomputes from source data. |
| 11 | Duplicate delivery/callback | Unique idempotency/event keys prevent duplicate processing. |
| 12 | Entitlement change | Only signed Orbit callbacks can update the cached entitlement. |
| 13 | Unauthorized access | JWT-derived user filter, owner-only SELECT RLS, and server-only writes apply. |
| 14 | Mobile settings | Compose sheet exposes Connect / Manage / Disconnect and a 54dp primary target. |
| 15 | Desktop settings | Backend contract is client-agnostic; this repository currently has no desktop Fitness target, so desktop visual QA belongs to the consuming Orbit client. |

The JVM policy suite contains 15 matching gate/state tests. Database RLS, webhook signature, and end-to-end delivery should also be exercised against a linked staging Supabase project before production deployment.
