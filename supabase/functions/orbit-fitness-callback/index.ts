import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { adminClient, requiredEnv } from "../_shared/auth.ts";
import { jsonResponse, preflight } from "../_shared/cors.ts";

type CallbackBody = {
  eventId?: string;
  type?: string;
  state?: string;
  orbitAccountId?: string;
  accountLabel?: string;
  authorized?: boolean;
  fitnessSyncEntitled?: boolean;
  manageUrl?: string;
};

const encoder = new TextEncoder();

function decodeBase64Url(value: string): Uint8Array {
  const base64 = value.replaceAll("-", "+").replaceAll("_", "/").padEnd(Math.ceil(value.length / 4) * 4, "=");
  return Uint8Array.from(atob(base64), (char) => char.charCodeAt(0));
}

function hexBytes(value: string): Uint8Array | null {
  const normalized = value.replace(/^sha256=/i, "").trim();
  if (!/^[0-9a-f]+$/i.test(normalized) || normalized.length % 2) return null;
  return Uint8Array.from({ length: normalized.length / 2 }, (_, i) => parseInt(normalized.slice(i * 2, i * 2 + 2), 16));
}

function safeEqual(a: Uint8Array, b: Uint8Array): boolean {
  let diff = a.length ^ b.length;
  for (let i = 0; i < Math.max(a.length, b.length); i++) diff |= (a[i] ?? 0) ^ (b[i] ?? 0);
  return diff === 0;
}

async function hmac(secret: string, value: string): Promise<Uint8Array> {
  const key = await crypto.subtle.importKey("raw", encoder.encode(secret), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
  return new Uint8Array(await crypto.subtle.sign("HMAC", key, encoder.encode(value)));
}

async function verifyRequest(req: Request, raw: string): Promise<boolean> {
  const timestamp = req.headers.get("x-webhook-timestamp") ?? "";
  const received = hexBytes(req.headers.get("x-webhook-signature") ?? "");
  const seconds = Number(timestamp);
  if (!received || !Number.isFinite(seconds) || Math.abs(Date.now() / 1000 - seconds) > 300) return false;
  return safeEqual(await hmac(requiredEnv("ORBIT_FITNESS_WEBHOOK_SECRET"), `${timestamp}.${raw}`), received);
}

async function statePayload(state: string): Promise<{ userId: string; nonce: string } | null> {
  const [payload, signature] = state.split(".");
  if (!payload || !signature) return null;
  if (!safeEqual(await hmac(requiredEnv("ORBIT_FITNESS_STATE_SECRET"), payload), decodeBase64Url(signature))) return null;
  const parsed = JSON.parse(new TextDecoder().decode(decodeBase64Url(payload))) as { userId?: string; nonce?: string; expiresAt?: number };
  return parsed.userId && parsed.nonce && parsed.expiresAt && parsed.expiresAt >= Date.now()
    ? { userId: parsed.userId, nonce: parsed.nonce } : null;
}

Deno.serve(async (req: Request) => {
  const preflightResponse = preflight(req);
  if (preflightResponse) return preflightResponse;
  if (req.method !== "POST") return jsonResponse(405, { code: "method_not_allowed", message: "Method not allowed." });

  const raw = await req.text();
  try {
    if (!await verifyRequest(req, raw)) return jsonResponse(401, { code: "invalid_signature", message: "Orbit imzası doğrulanamadı." });
    const body = JSON.parse(raw || "{}") as CallbackBody;
    const eventId = body.eventId?.trim();
    const orbitAccountId = body.orbitAccountId?.trim();
    if (!eventId || !orbitAccountId) {
      return jsonResponse(400, { code: "invalid_callback", message: "Orbit bağlantı yanıtı eksik." });
    }
    const db = adminClient();
    const state = body.state ? await statePayload(body.state) : null;
    let userId = state?.userId ?? null;
    if (state) {
      const { data: attempt } = await db.from("orbit_link_attempts")
        .update({ consumed_at: new Date().toISOString() })
        .eq("nonce", state.nonce).eq("user_id", state.userId)
        .is("consumed_at", null).gt("expires_at", new Date().toISOString())
        .select("user_id").maybeSingle();
      if (!attempt) return jsonResponse(401, { code: "state_replayed", message: "Orbit bağlantı isteği geçersiz veya kullanılmış." });
    } else {
      const { data: existingConnection } = await db.from("orbit_connections")
        .select("user_id").eq("orbit_account_id", orbitAccountId).maybeSingle();
      userId = existingConnection?.user_id ?? null;
    }
    if (!userId) return jsonResponse(401, { code: "unknown_connection", message: "Orbit bağlantısı doğrulanamadı." });
    const { error: eventError } = await db.from("orbit_webhook_events").insert({
      provider_event_id: eventId, event_type: body.type?.trim() || "connection.updated",
    });
    if (eventError?.code === "23505") return jsonResponse(200, { ok: true, duplicate: true });
    if (eventError) throw new Error("event_record_failed");

    const now = new Date().toISOString();
    const authorized = body.authorized === true;
    const { data: existing } = await db.from("orbit_connections")
      .select("sync_enabled").eq("user_id", userId).maybeSingle();
    const { error } = await db.from("orbit_connections").upsert({
      user_id: userId,
      orbit_account_id: orbitAccountId,
      account_label: body.accountLabel?.trim() || null,
      status: authorized ? "connected" : "reconnect_required",
      sync_enabled: authorized && body.fitnessSyncEntitled === true && existing?.sync_enabled === true,
      fitness_sync_entitled: authorized && body.fitnessSyncEntitled === true,
      entitlement_checked_at: now,
      manage_url: body.manageUrl?.trim() || null,
      last_error_code: authorized ? null : "authorization_revoked",
      updated_at: now,
    }, { onConflict: "user_id" });
    if (error) throw new Error("connection_save_failed");
    return jsonResponse(200, { ok: true });
  } catch (error) {
    const code = error instanceof Error ? error.message : "callback_failed";
    return jsonResponse(503, { code, message: "Orbit bağlantısı kaydedilemedi." });
  }
});
