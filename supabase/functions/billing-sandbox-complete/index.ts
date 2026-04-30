import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { adminClient, authenticatedUser } from "../_shared/auth.ts";
import { jsonResponse, preflight } from "../_shared/cors.ts";

type SandboxBody = {
  orderId?: string;
};

Deno.serve(async (req: Request) => {
  const preflightResponse = preflight(req);
  if (preflightResponse) return preflightResponse;

  if (req.method !== "POST") {
    return jsonResponse(405, { code: "method_not_allowed", message: "Method not allowed." });
  }

  if (Deno.env.get("BILLING_SANDBOX_ENABLED") !== "true") {
    return jsonResponse(403, {
      code: "sandbox_disabled",
      message: "Sandbox satın alma bu ortamda kapalı.",
    });
  }

  try {
    const { user } = await authenticatedUser(req);
    const body = await req.json().catch(() => ({})) as SandboxBody;
    const orderId = typeof body.orderId === "string" ? body.orderId.trim() : "";
    if (!orderId) {
      return jsonResponse(400, { code: "invalid_order", message: "Sipariş bulunamadı." });
    }

    const supabase = adminClient();
    const { data: order, error: orderError } = await supabase
      .from("billing_orders")
      .select("id,user_id,status")
      .eq("id", orderId)
      .eq("user_id", user.id)
      .maybeSingle();

    if (orderError || !order) {
      return jsonResponse(404, { code: "order_not_found", message: "Sipariş bulunamadı." });
    }
    if (order.status !== "pending_provider" && order.status !== "pending_payment") {
      return jsonResponse(409, { code: "order_not_pending", message: "Bu sipariş tamamlanamaz." });
    }

    const { data, error } = await supabase.rpc("apply_paid_billing_order", {
      p_order_id: orderId,
      p_provider: "sandbox",
      p_provider_session_id: `sandbox_${crypto.randomUUID()}`,
    });

    if (error || data?.ok !== true) {
      return jsonResponse(400, { code: data?.code ?? "sandbox_apply_failed", message: "Sandbox satın alma tamamlanamadı." });
    }

    return jsonResponse(200, {
      ok: true,
      message: "Sandbox satın alma tamamlandı. Haklar hesaba işlendi.",
      result: data,
    });
  } catch (error) {
    const code = error instanceof Error ? error.message : "unknown_error";
    if (code === "unauthorized") {
      return jsonResponse(401, { code: "unauthorized", message: "Oturum gerekli." });
    }
    return jsonResponse(503, { code: "sandbox_unavailable", message: "Sandbox satın alma tamamlanamadı." });
  }
});
