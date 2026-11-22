import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { adminClient, authenticatedUser, truthyEnv } from "../_shared/auth.ts";
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

    const [
      { data: entitlements },
      { data: account },
      { data: products },
      { data: recentUsage },
      { data: settings },
      { data: freeCounter },
    ] = await Promise.all([
      supabase.from("user_entitlements").select("*").eq("user_id", user.id).maybeSingle(),
      supabase.from("user_credit_accounts").select("*").eq("user_id", user.id).maybeSingle(),
      supabase.from("billing_products").select("*").eq("active", true).order("sort_order"),
      supabase
        .from("ai_usage_events")
        .select("tool,status,entitlement_source,credit_cost,created_at")
        .eq("user_id", user.id)
        .order("created_at", { ascending: false })
        .limit(10),
      supabase
        .from("ai_access_settings")
        .select("free_ai_program_grants")
        .eq("id", true)
        .maybeSingle(),
      supabase
        .from("usage_counters")
        .select("used_count")
        .eq("user_id", user.id)
        .eq("tool", "AI_PROGRAM_FREE_GRANT")
        .eq("period", "lifetime")
        .maybeSingle(),
    ]);

    const rawPlan = entitlements?.plan ?? "FREE";
    const rawStatus = entitlements?.status ?? "free";
    const end = entitlements?.current_period_end ? new Date(entitlements.current_period_end) : null;
    const expired = end !== null && !Number.isNaN(end.getTime()) && end.getTime() < Date.now();
    const activeStatus = rawStatus === "active" || rawStatus === "trialing";
    const isEffectiveFree = rawPlan === "FREE" || !activeStatus || expired;
    const plan = isEffectiveFree ? "FREE" : "PREMIUM";
    const status = expired && activeStatus ? "expired" : rawStatus;
    const freeGrantLimit = settings?.free_ai_program_grants ?? 2;
    const freeGrantUsed = freeCounter?.used_count ?? 0;
    const freeAiProgramsLeft = plan === "FREE" ? Math.max(0, freeGrantLimit - freeGrantUsed) : null;

    return jsonResponse(200, {
      plan,
      status,
      currentPeriodEnd: entitlements?.current_period_end ?? null,
      source: entitlements?.source ?? "system",
      freeAiProgramsLeft,
      credits: account?.balance ?? 0,
      products: products ?? [],
      recentUsage: recentUsage ?? [],
      sandboxAvailable: truthyEnv("BILLING_SANDBOX_ENABLED"),
    });
  } catch (error) {
    const code = error instanceof Error ? error.message : "unknown_error";
    if (code === "unauthorized") {
      return jsonResponse(401, { code: "unauthorized", message: "Oturum gerekli." });
    }
    return jsonResponse(503, { code: "billing_status_unavailable", message: "Kullanım durumu alınamadı." });
  }
});
