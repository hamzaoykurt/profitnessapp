import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { adminClient, authenticatedUser, requiredEnv } from "../_shared/auth.ts";
import { jsonResponse, preflight } from "../_shared/cors.ts";

type ActionBody = { action?: string; enabled?: boolean; timeZone?: string; idempotencyKey?: string };

const encoder = new TextEncoder();
const base64Url = (bytes: Uint8Array) => btoa(String.fromCharCode(...bytes))
  .replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");

async function signedState(userId: string): Promise<{ value: string; nonce: string; expiresAt: string }> {
  const nonce = crypto.randomUUID();
  const expiresAtMs = Date.now() + 10 * 60_000;
  const payload = base64Url(encoder.encode(JSON.stringify({
    userId,
    expiresAt: expiresAtMs,
    nonce,
  })));
  const key = await crypto.subtle.importKey(
    "raw", encoder.encode(requiredEnv("ORBIT_FITNESS_STATE_SECRET")),
    { name: "HMAC", hash: "SHA-256" }, false, ["sign"],
  );
  const signature = new Uint8Array(await crypto.subtle.sign("HMAC", key, encoder.encode(payload)));
  return { value: `${payload}.${base64Url(signature)}`, nonce, expiresAt: new Date(expiresAtMs).toISOString() };
}

async function orbitServerHeaders(rawBody: string, idempotencyKey?: string): Promise<Record<string, string>> {
  const timestamp = String(Math.floor(Date.now() / 1000));
  const key = await crypto.subtle.importKey(
    "raw", encoder.encode(requiredEnv("ORBIT_FITNESS_REQUEST_SECRET")),
    { name: "HMAC", hash: "SHA-256" }, false, ["sign"],
  );
  const signature = new Uint8Array(await crypto.subtle.sign("HMAC", key, encoder.encode(`${timestamp}.${rawBody}`)));
  const hex = Array.from(signature, (byte) => byte.toString(16).padStart(2, "0")).join("");
  return {
    "Content-Type": "application/json",
    "Authorization": `Bearer ${requiredEnv("ORBIT_FITNESS_SERVER_SECRET")}`,
    "X-Orbit-Timestamp": timestamp,
    "X-Orbit-Signature": `sha256=${hex}`,
    ...(idempotencyKey ? { "Idempotency-Key": idempotencyKey } : {}),
  };
}

function connectionResponse(row: Record<string, unknown> | null) {
  if (!row) return { status: "not_connected", syncEnabled: false, fitnessSyncEntitled: false };
  return {
    status: row.status,
    accountLabel: row.account_label,
    syncEnabled: row.sync_enabled,
    fitnessSyncEntitled: row.fitness_sync_entitled,
    lastSyncedAt: row.last_synced_at,
    lastErrorCode: row.last_error_code,
    manageUrl: row.manage_url,
  };
}

Deno.serve(async (req: Request) => {
  const preflightResponse = preflight(req);
  if (preflightResponse) return preflightResponse;
  if (req.method !== "POST") return jsonResponse(405, { code: "method_not_allowed", message: "Method not allowed." });

  try {
    const { user } = await authenticatedUser(req);
    const body = await req.json().catch(() => ({})) as ActionBody;
    const action = body.action?.trim() ?? "status";
    const db = adminClient();
    const readConnection = async () => (await db.from("orbit_connections").select("*").eq("user_id", user.id).maybeSingle()).data;

    if (action === "status") return jsonResponse(200, connectionResponse(await readConnection()));

    if (action === "connect") {
      const connectUrl = requiredEnv("ORBIT_FITNESS_CONNECT_URL");
      const callbackUrl = `${requiredEnv("SUPABASE_URL")}/functions/v1/orbit-fitness-callback`;
      const url = new URL(connectUrl);
      const state = await signedState(user.id);
      await db.from("orbit_link_attempts").delete().eq("user_id", user.id).is("consumed_at", null);
      const { error } = await db.from("orbit_link_attempts").insert({ nonce: state.nonce, user_id: user.id, expires_at: state.expiresAt });
      if (error) throw new Error("link_state_save_failed");
      url.searchParams.set("state", state.value);
      url.searchParams.set("callback_url", callbackUrl);
      url.searchParams.set("fitness_user_id", user.id);
      return jsonResponse(200, { ...connectionResponse(await readConnection()), authorizationUrl: url.toString() });
    }

    const connection = await readConnection();
    if (!connection) return jsonResponse(409, { code: "not_connected", message: "Orbit hesabı bağlı değil." });

    if (action === "disconnect") {
      const disconnectUrl = Deno.env.get("ORBIT_FITNESS_DISCONNECT_URL")?.trim();
      if (disconnectUrl) {
        const rawBody = JSON.stringify({ orbitAccountId: connection.orbit_account_id, fitnessUserId: user.id });
        const response = await fetch(disconnectUrl, {
          method: "POST",
          headers: await orbitServerHeaders(rawBody),
          body: rawBody,
        });
        if (!response.ok) throw new Error("orbit_disconnect_failed");
      }
      await db.from("orbit_connections").delete().eq("user_id", user.id);
      await db.from("orbit_link_attempts").delete().eq("user_id", user.id);
      return jsonResponse(200, connectionResponse(null));
    }

    if (action === "set_sync_enabled") {
      if (body.enabled === true && connection.fitness_sync_entitled !== true) {
        return jsonResponse(403, { code: "fitness_sync_not_entitled", message: "Fitness Sync Orbit üyeliğinizde açık değil." });
      }
      const { data, error } = await db.from("orbit_connections")
        .update({ sync_enabled: body.enabled === true, updated_at: new Date().toISOString() })
        .eq("user_id", user.id).select("*").single();
      if (error) throw new Error("connection_update_failed");
      return jsonResponse(200, connectionResponse(data));
    }

    if (action === "sync") {
      if (connection.status !== "connected") return jsonResponse(409, { code: "reconnect_required", message: "Orbit bağlantısını yenileyin." });
      if (connection.sync_enabled !== true || connection.fitness_sync_entitled !== true) {
        return jsonResponse(403, { code: "fitness_sync_not_allowed", message: "Orbit Fitness Sync yetkisi yok." });
      }
      const idempotencyKey = body.idempotencyKey?.trim();
      if (!idempotencyKey) return jsonResponse(400, { code: "idempotency_key_required", message: "İstek kimliği eksik." });
      const { error: deliveryError } = await db.from("orbit_sync_deliveries").insert({
        user_id: user.id, idempotency_key: idempotencyKey, status: "pending",
      });
      if (deliveryError?.code === "23505") {
        const { data: existingDelivery } = await db.from("orbit_sync_deliveries")
          .select("status,created_at").eq("user_id", user.id).eq("idempotency_key", idempotencyKey).maybeSingle();
        if (existingDelivery?.status === "delivered") return jsonResponse(200, connectionResponse(connection));
        const isFreshPending = existingDelivery?.status === "pending" &&
          Date.now() - Date.parse(existingDelivery.created_at) < 2 * 60_000;
        if (isFreshPending) return jsonResponse(202, connectionResponse(connection));
        const { error: retryError } = await db.from("orbit_sync_deliveries")
          .update({ status: "pending", error_code: null, delivered_at: null })
          .eq("user_id", user.id).eq("idempotency_key", idempotencyKey);
        if (retryError) throw new Error("delivery_retry_failed");
      }
      if (deliveryError && deliveryError.code !== "23505") throw new Error("delivery_record_failed");

      const timeZone = (() => {
        try { new Intl.DateTimeFormat("en", { timeZone: body.timeZone ?? "UTC" }); return body.timeZone ?? "UTC"; }
        catch { return "UTC"; }
      })();
      const { data: summary, error: summaryError } = await db.rpc("get_orbit_fitness_summary", {
        p_user_id: user.id, p_timezone: timeZone,
      });
      if (summaryError) {
        await db.from("orbit_sync_deliveries").update({ status: "failed", error_code: "summary_failed" })
          .eq("user_id", user.id).eq("idempotency_key", idempotencyKey);
        throw new Error("summary_failed");
      }

      const rawBody = JSON.stringify({ orbitAccountId: connection.orbit_account_id, fitnessUserId: user.id, summary });
      let response: Response;
      try {
        response = await fetch(requiredEnv("ORBIT_FITNESS_SYNC_URL"), {
          method: "POST",
          headers: { ...(await orbitServerHeaders(rawBody, idempotencyKey)), "X-Fitness-Updated-At": new Date().toISOString() },
          body: rawBody,
        });
      } catch {
        await db.from("orbit_sync_deliveries").update({ status: "failed", error_code: "orbit_network_error" })
          .eq("user_id", user.id).eq("idempotency_key", idempotencyKey);
        await db.from("orbit_connections").update({ last_error_code: "sync_failed", updated_at: new Date().toISOString() }).eq("user_id", user.id);
        return jsonResponse(503, { code: "sync_failed", message: "Orbit geçici olarak kullanılamıyor." });
      }
      if (!response.ok) {
        await db.from("orbit_sync_deliveries").update({ status: "failed", error_code: `orbit_http_${response.status}` })
          .eq("user_id", user.id).eq("idempotency_key", idempotencyKey);
        await db.from("orbit_connections").update({ last_error_code: "sync_failed", updated_at: new Date().toISOString() }).eq("user_id", user.id);
        return jsonResponse(503, { code: "sync_failed", message: "Orbit geçici olarak kullanılamıyor." });
      }
      const now = new Date().toISOString();
      await db.from("orbit_sync_deliveries").update({ status: "delivered", delivered_at: now })
        .eq("user_id", user.id).eq("idempotency_key", idempotencyKey);
      const { data } = await db.from("orbit_connections")
        .update({ last_synced_at: now, last_error_code: null, updated_at: now })
        .eq("user_id", user.id).select("*").single();
      return jsonResponse(200, connectionResponse(data));
    }

    return jsonResponse(400, { code: "invalid_action", message: "Geçersiz Orbit işlemi." });
  } catch (error) {
    const code = error instanceof Error ? error.message : "temporarily_unavailable";
    if (code === "unauthorized") return jsonResponse(401, { code, message: "Oturum gerekli." });
    return jsonResponse(503, { code, message: "Orbit entegrasyonu şu anda kullanılamıyor." });
  }
});
