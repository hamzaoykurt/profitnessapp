import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { adminClient, authenticatedUser } from "../_shared/auth.ts";
import { jsonResponse, preflight } from "../_shared/cors.ts";

Deno.serve(async (req: Request) => {
  const preflightResponse = preflight(req);
  if (preflightResponse) return preflightResponse;

  if (req.method !== "POST") {
    return jsonResponse(405, { code: "method_not_allowed", message: "Method not allowed." });
  }

  try {
    const { user } = await authenticatedUser(req);
    const supabase = adminClient();

    await supabase.rpc("ensure_billing_account", { p_user_id: user.id });

    const [{ data: entitlements }, { data: account }, { data: products }, { data: recentUsage }] =
      await Promise.all([
        supabase.from("user_entitlements").select("*").eq("user_id", user.id).maybeSingle(),
        supabase.from("user_credit_accounts").select("*").eq("user_id", user.id).maybeSingle(),
        supabase.from("billing_products").select("*").eq("active", true).order("sort_order"),
        supabase
          .from("ai_usage_events")
          .select("tool,status,entitlement_source,credit_cost,created_at")
          .eq("user_id", user.id)
          .order("created_at", { ascending: false })
          .limit(10),
      ]);

    return jsonResponse(200, {
      plan: entitlements?.plan ?? "FREE",
      status: entitlements?.status ?? "free",
      currentPeriodEnd: entitlements?.current_period_end ?? null,
      credits: account?.balance ?? 0,
      products: products ?? [],
      recentUsage: recentUsage ?? [],
    });
  } catch (error) {
    const code = error instanceof Error ? error.message : "unknown_error";
    if (code === "unauthorized") {
      return jsonResponse(401, { code: "unauthorized", message: "Oturum gerekli." });
    }
    return jsonResponse(503, { code: "billing_status_unavailable", message: "Kullanım durumu alınamadı." });
  }
});
