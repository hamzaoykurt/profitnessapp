import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { adminClient } from "../_shared/auth.ts";
import { jsonResponse, preflight } from "../_shared/cors.ts";

type WebhookBody = {
  provider?: string;
  eventId?: string;
  type?: string;
  orderId?: string;
  providerSessionId?: string;
  payload?: Record<string, unknown>;
};

Deno.serve(async (req: Request) => {
  const preflightResponse = preflight(req);
  if (preflightResponse) return preflightResponse;

  if (req.method !== "POST") {
    return jsonResponse(405, { code: "method_not_allowed", message: "Method not allowed." });
  }

  const expectedSecret = Deno.env.get("BILLING_WEBHOOK_SECRET");
  if (!expectedSecret || req.headers.get("x-webhook-secret") !== expectedSecret) {
    return jsonResponse(401, { code: "unauthorized", message: "Webhook doğrulanamadı." });
  }

  const body = await req.json().catch(() => ({})) as WebhookBody;
  const provider = body.provider?.trim() || "manual";
  const eventId = body.eventId?.trim() || crypto.randomUUID();
  const eventType = body.type?.trim() || "";
  const orderId = body.orderId?.trim() || "";

  if (!eventType || !orderId) {
    return jsonResponse(400, { code: "invalid_webhook", message: "Webhook payload eksik." });
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
    const { data, error } = await supabase.rpc("apply_paid_billing_order", {
      p_order_id: orderId,
      p_provider: provider,
      p_provider_session_id: body.providerSessionId ?? null,
    });

    if (error || data?.ok !== true) {
      return jsonResponse(400, { code: data?.code ?? "order_apply_failed", message: "Sipariş uygulanamadı." });
    }
    return jsonResponse(200, { ok: true, result: data });
  }

  return jsonResponse(200, { ok: true, ignored: true });
});
