import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { adminClient, authenticatedUser } from "../_shared/auth.ts";
import { jsonResponse, preflight } from "../_shared/cors.ts";

type CheckoutBody = {
  sku?: string;
};

Deno.serve(async (req: Request) => {
  const preflightResponse = preflight(req);
  if (preflightResponse) return preflightResponse;

  if (req.method !== "POST") {
    return jsonResponse(405, { code: "method_not_allowed", message: "Method not allowed." });
  }

  try {
    const { user } = await authenticatedUser(req);
    const body = await req.json().catch(() => ({})) as CheckoutBody;
    const sku = typeof body.sku === "string" ? body.sku.trim() : "";
    if (!sku) {
      return jsonResponse(400, { code: "invalid_sku", message: "Paket seçimi geçersiz." });
    }

    const { data, error } = await adminClient().rpc("create_pending_billing_order", {
      p_user_id: user.id,
      p_sku: sku,
    });

    if (error || data?.ok !== true) {
      return jsonResponse(400, {
        code: data?.code ?? "checkout_unavailable",
        message: data?.message ?? "Satın alma başlatılamadı.",
      });
    }

    return jsonResponse(200, {
      orderId: data.order_id,
      status: data.status,
      checkoutUrl: null,
      sandboxAvailable: Deno.env.get("BILLING_SANDBOX_ENABLED") === "true",
      message: data.message,
    });
  } catch (error) {
    const code = error instanceof Error ? error.message : "unknown_error";
    if (code === "unauthorized") {
      return jsonResponse(401, { code: "unauthorized", message: "Oturum gerekli." });
    }
    return jsonResponse(503, { code: "checkout_unavailable", message: "Satın alma başlatılamadı." });
  }
});
