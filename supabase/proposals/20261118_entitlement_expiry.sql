-- ÖNERİ — UYGULANMADI (ONAY GEREKİYOR). Canlı Supabase'e dokunulmadı.
-- Sorun: user_entitlements.current_period_end geçmiş olsa da (ör. sandbox ELITE, 2026-10-06) plan 'active' sayılıyor.
-- Planı çözen yerler (kod taraması):
--   1) supabase/functions/billing-status/index.ts  -> entitlements.plan / status ham döndürülüyor (süre kontrolü yok)
--   2) public.reserve_ai_usage (20260519193000_rebalance_energy_plan_hierarchy.sql) -> yalnız status in ('active','trialing')
--   3) Diğer okuyucular: ensure_billing_account (yalnız satır açar), harden_security_controls (yetki) — plan çözmüyor.
-- Kural (tek yerde): süre bitmişse ETKİN plan FREE.
--   effective_free := plan = 'FREE'
--                  or status not in ('active','trialing')
--                  or (current_period_end is not null and current_period_end < now())

-- ── A) Tek kaynak SQL yardımcı fonksiyon ─────────────────────────────────────
create or replace function public.effective_plan(p_plan text, p_status text, p_period_end timestamptz)
returns text
language sql
stable   -- now() kullanır
set search_path = public, pg_temp
as $$
  select case
    when p_plan is null or p_plan = 'FREE' then 'FREE'
    when p_status not in ('active', 'trialing') then 'FREE'
    when p_period_end is not null and p_period_end < now() then 'FREE'
    else p_plan
  end
$$;
grant execute on function public.effective_plan(text, text, timestamptz) to authenticated, service_role;

-- ── B) reserve_ai_usage içindeki plan çözümü (20260519193000 satır ~95-103) şu blokla değişmeli ──
--     select plan, status, current_period_end into v_plan, v_status, v_period_end
--       from public.user_entitlements where user_id = p_user_id for update;
--     v_plan := public.effective_plan(v_plan, v_status, v_period_end);
--   (declare: v_period_end timestamptz;) — fonksiyonun geri kalanı aynı. Ücretsiz+Premium önerisi
--   (proposals/20261117_free_premium_ai_access.sql) uygulanacaksa aynı değişiklik oradaki gövdeye de yapılmalı.

-- ── C) Süresi geçmiş satırları kalıcı olarak düzelt (isteğe bağlı, günlük cron ya da tek sefer) ──
-- update public.user_entitlements
--    set status = 'expired', updated_at = now()
--  where plan <> 'FREE' and status in ('active','trialing')
--    and current_period_end is not null and current_period_end < now();

-- ── D) billing-status/index.ts değişikliği (edge function; deploy gerekir) ──
--   const end = entitlements?.current_period_end ? new Date(entitlements.current_period_end) : null;
--   const expired = end !== null && end.getTime() < Date.now();
--   const activeStatus = ["active", "trialing"].includes(entitlements?.status ?? "free");
--   const plan = (!entitlements || entitlements.plan === "FREE" || !activeStatus || expired) ? "FREE" : entitlements.plan;
--   return jsonResponse(200, { plan, status: expired ? "expired" : (entitlements?.status ?? "free"),
--                              currentPeriodEnd: entitlements?.current_period_end ?? null, source: entitlements?.source ?? "system", ... });
-- İstemci savunması zaten var: UserPlanRepositoryImpl -> PlanResolver (period_end geçmiş / status pasif => FREE).

-- GERİ ALMA: drop function public.effective_plan(text, text, timestamptz); reserve_ai_usage önceki tanıma döner.
