import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { adminClient } from "../_shared/auth.ts";
import { jsonResponse, preflight } from "../_shared/cors.ts";

type WebhookBody = {
  provider?: string;
  eventId?: string;
  type?: string;
  orderId?: string;
  providerSessionId?: string;
  sku?: string;
  amountLabel?: string;
  amountMinor?: number;
  currency?: string;
  payload?: Record<string, unknown>;
};

const encoder = new TextEncoder();
const SIGNATURE_TOLERANCE_SECONDS = Number(Deno.env.get("BILLING_WEBHOOK_TOLERANCE_SECONDS") ?? "300");

function truthy(value: string | undefined): boolean {
  const normalized = value?.trim().replace(/^["']|["']$/g, "").toLowerCase();
  return normalized === "true" || normalized === "1" || normalized === "yes" || normalized === "on";
}

function normalizeSignature(value: string): string {
  return value.trim().replace(/^v\d+=/i, "").replace(/^sha256=/i, "").toLowerCase();
}

function hexToBytes(hex: string): Uint8Array | null {
  if (!/^[0-9a-f]+$/i.test(hex) || hex.length % 2 !== 0) return null;
  const bytes = new Uint8Array(hex.length / 2);
  for (let i = 0; i < bytes.length; i++) {
    bytes[i] = Number.parseInt(hex.slice(i * 2, i * 2 + 2), 16);
  }
  return bytes;
}

function timingSafeEqual(a: Uint8Array, b: Uint8Array): boolean {
  let diff = a.length ^ b.length;
  const maxLength = Math.max(a.length, b.length);
  for (let i = 0; i < maxLength; i++) {
    diff |= (a[i] ?? 0) ^ (b[i] ?? 0);
  }
  return diff === 0;
}

async function hmacSha256Hex(secret: string, value: string): Promise<Uint8Array> {
  const key = await crypto.subtle.importKey(
    "raw",
    encoder.encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  return new Uint8Array(await crypto.subtle.sign("HMAC", key, encoder.encode(value)));
}

async function verifyWebhookSignature(req: Request, rawBody: string): Promise<boolean> {
  const secret = Deno.env.get("BILLING_WEBHOOK_HMAC_SECRET") ?? Deno.env.get("BILLING_WEBHOOK_SECRET");
  const timestampHeader = req.headers.get("x-webhook-timestamp") ?? "";
  const signatureHeader = req.headers.get("x-webhook-signature") ?? "";
  if (!secret?.trim() || !timestampHeader || !signatureHeader) return false;

  const timestamp = Number(timestampHeader);
  if (!Number.isFinite(timestamp)) return false;
  const timestampSeconds = timestamp > 9999999999 ? Math.floor(timestamp / 1000) : Math.floor(timestamp);
  const ageSeconds = Math.abs(Math.floor(Date.now() / 1000) - timestampSeconds);
  if (ageSeconds > SIGNATURE_TOLERANCE_SECONDS) return false;

  const expected = await hmacSha256Hex(secret, `${timestampHeader}.${rawBody}`);
  return signatureHeader
    .split(",")
    .map(normalizeSignature)
    .map(hexToBytes)
    .some((candidate) => candidate != null && timingSafeEqual(expected, candidate));
}

function verifyLegacyWebhookSecret(req: Request): boolean {
  if (!truthy(Deno.env.get("BILLING_WEBHOOK_ALLOW_LEGACY_SECRET") ?? "true")) return false;
  const expected = Deno.env.get("BILLING_WEBHOOK_SECRET");
  const received = req.headers.get("x-webhook-secret");
  if (!expected?.trim() || !received) return false;
  return timingSafeEqual(encoder.encode(expected), encoder.encode(received));
}

Deno.serve(async (req: Request) => {
  const preflightResponse = preflight(req);
  if (preflightResponse) return preflightResponse;

  if (req.method !== "POST") {
    return jsonResponse(405, { code: "method_not_allowed", message: "Method not allowed." });
  }

  const rawBody = await req.text();
  const verifiedWithHmac = await verifyWebhookSignature(req, rawBody);
  if (!verifiedWithHmac && !verifyLegacyWebhookSecret(req)) {
    return jsonResponse(401, { code: "unauthorized", message: "Webhook doğrulanamadı." });
  }

  let body: WebhookBody;
  try {
    body = JSON.parse(rawBody || "{}") as WebhookBody;
  } catch {
    return jsonResponse(400, { code: "invalid_json", message: "Webhook payload geçersiz." });
  }
  const provider = body.provider?.trim() || req.headers.get("x-webhook-provider")?.trim() || "billing";
  const eventId = body.eventId?.trim() || req.headers.get("x-webhook-id")?.trim() || "";
  const eventType = body.type?.trim() || "";
  const orderId = body.orderId?.trim() || "";
  const sku = body.sku?.trim() || "";
  const amountLabel = body.amountLabel?.trim() || "";
  const amountMinor = Number.isInteger(body.amountMinor) ? body.amountMinor : null;
  const currency = body.currency?.trim().toUpperCase() || null;

  if (!eventId || !eventType || !orderId) {
    return jsonResponse(400, { code: "invalid_webhook", message: "Webhook payload eksik." });
  }

  if (eventType === "checkout.paid" || eventType === "invoice.paid") {
    if (!sku || (!amountLabel && amountMinor == null)) {
      return jsonResponse(400, { code: "missing_payment_assertions", message: "Ödeme tutarı veya SKU doğrulanamadı." });
    }
  }

  const supabase = adminClient();
  const { error: eventError } = await supabase
    .from("billing_webhook_events")
    .insert({
      provider,
      provider_event_id: eventId,
      payload: body.payload ?? body,
    });

  if (eventError?.code === "23505") {
    return jsonResponse(200, { ok: true, duplicate: true });
  }
  if (eventError) {
    return jsonResponse(503, { code: "webhook_record_failed", message: "Webhook kaydedilemedi." });
  }

  if (eventType === "checkout.paid" || eventType === "invoice.paid") {
    const { data, error } = await supabase.rpc("apply_paid_billing_order_verified", {
      p_order_id: orderId,
      p_provider: provider,
      p_provider_session_id: body.providerSessionId ?? null,
      p_sku: sku,
      p_amount_label: amountLabel || null,
      p_amount_minor: amountMinor,
      p_currency: currency,
    });

    if (error || data?.ok !== true) {
      return jsonResponse(400, { code: data?.code ?? "order_apply_failed", message: "Sipariş uygulanamadı." });
    }
    return jsonResponse(200, { ok: true, result: data });
  }

  return jsonResponse(200, { ok: true, ignored: true });
});
