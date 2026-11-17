-- ════════════════════════════════════════════════════════════════════════════
-- ÖNERİ — UYGULANMADI (ONAY GEREKİYOR). Canlı Supabase'e dokunulmadı.
-- Model: ÜCRETSİZ + PREMIUM (tek abonelik, aylık/yıllık). Enerji/kredi akışı kalkar.
--   FREE    : yalnız AI program oluşturma/içe aktarma (PROGRAM_GENERATE_TEXT / _MEDIA), hesap başına ömür boyu 2 hak.
--             Diğer tüm AI araçları reddedilir (reason = 'premium_required').
--   PREMIUM : tüm AI araçları; "sınırsız" ama adil kullanım tavanı: günlük N çağrı + dakikada M çağrı.
-- Uyum: 20260430090000_billing_entitlements.sql (user_entitlements, usage_counters, ai_usage_events,
--       user_credit_accounts, credit_ledger, refund_ai_usage) ve 20260519193000_rebalance_energy_plan_hierarchy.sql
--       (reserve_ai_usage). İmza aynı kalır (ai-generate / gemini-generate değişmez; allowed=false -> 402, code=reason).
-- İstemci: UserPlan.fromServer "PRO"/"ELITE"/"PREMIUM" -> PREMIUM; billing-status yanıtına freeAiProgramsLeft eklenmeli.
--
-- GÜVENLİ GEÇİŞ SIRASI
--   1) İstemci (bu sürüm) yayında: eski plan adlarını PREMIUM sayar, Enerji UI'ı yok, sayaç alanı opsiyonel.
--   2) Bu dosyadaki ADIM A (şema + ayarlar) -> ADIM B (plan eşleme) -> ADIM C (reserve_ai_usage) tek migration'da.
--   3) billing-status: freeAiProgramsLeft = greatest(0, free_ai_program_grants - kullanılan) döndür (ADIM D notu).
--   4) billing_products: sub_premium_monthly / sub_premium_yearly ekle; sub_pro_* / sub_elite_* / credits_* pasifle
--      (silme yok). Play Billing fazında gerçek ürün kimlikleri.
--   5) Enerji bakiyeleri: SİLİNMEZ (credit_ledger denetim izi). Satın alınmış Enerjisi olanlar için karar gerekli
--      (ör. bakiye > 0 olanlara 1 ay Premium veya iade). Yeni hesaplara başlangıç Enerjisi verilmez (ADIM A).
-- GERİ ALMA
--   reserve_ai_usage'ı 20260519193000 dosyasındaki tanımla yeniden oluştur; plan CHECK'ine 'PREMIUM' eklenmiş
--   olması zararsız; ADIM B'de PREMIUM'a çevrilen satırlar migration öncesi yedek tablodan (billing_plan_backup_20261117)
--   geri yazılır. Enerji tabloları dokunulmadığı için bakiyeler aynen döner.
-- ════════════════════════════════════════════════════════════════════════════

-- ── ADIM A: ayarlar ─────────────────────────────────────────────────────────
create table if not exists public.ai_access_settings (
    id boolean primary key default true check (id),
    free_ai_program_grants integer not null default 2 check (free_ai_program_grants >= 0),  -- ücretsiz ömür boyu hak
    premium_daily_cap integer not null default 200 check (premium_daily_cap > 0),           -- adil kullanım: günlük
    premium_per_minute_cap integer not null default 6 check (premium_per_minute_cap > 0),   -- hız sınırı
    updated_at timestamptz not null default now()
);
insert into public.ai_access_settings (id) values (true) on conflict (id) do nothing;
alter table public.ai_access_settings enable row level security;
revoke all on table public.ai_access_settings from anon, authenticated;
grant all on table public.ai_access_settings to service_role;

-- Yeni hesap başlangıç Enerjisi: ensure_billing_account içindeki starter tutar 0 yapılmalı
-- (fonksiyonun güncel gövdesi 20260519122728_raise_starter_credits_to_four_ai_programs.sql'de; aynı gövde yalnız
--  starter sabiti 0 olacak şekilde yeniden oluşturulmalı — burada kopyalanmadı).

-- ── ADIM B: plan eşleme (PRO/ELITE -> PREMIUM) ───────────────────────────────
create table if not exists public.billing_plan_backup_20261117 as
    select user_id, plan, status from public.user_entitlements where plan in ('PRO', 'ELITE');

alter table public.user_entitlements drop constraint if exists user_entitlements_plan_check;
alter table public.user_entitlements add constraint user_entitlements_plan_check
    check (plan in ('FREE', 'PREMIUM', 'PRO', 'ELITE'));   -- eski adlar geçiş süresince kabul
update public.user_entitlements set plan = 'PREMIUM' where plan in ('PRO', 'ELITE');
-- billing_products.plan CHECK'i için aynı genişletme (sub_premium_* ürünleri eklenince).

-- ── ADIM C: reserve_ai_usage (Enerji yok) ────────────────────────────────────
create or replace function public.reserve_ai_usage(
    p_user_id uuid,
    p_tool text,
    p_idempotency_key text default null
)
returns jsonb
language plpgsql
security invoker
set search_path = public, pg_temp
as $$
declare
    v_now timestamptz := now();
    v_event public.ai_usage_events%rowtype;
    v_plan text := 'FREE';
    v_status text := 'free';
    v_settings public.ai_access_settings%rowtype;
    v_counter_tool text;
    v_period text;
    v_period_start timestamptz;
    v_limit integer;
    v_used integer := 0;
    v_recent integer := 0;
begin
    if p_user_id is null or nullif(trim(p_tool), '') is null then
        return jsonb_build_object('allowed', false, 'reason', 'invalid_request', 'message', 'AI isteği geçersiz.');
    end if;
    if p_tool not in ('ORACLE_CHAT', 'PROGRAM_GENERATE_TEXT', 'PROGRAM_GENERATE_MEDIA', 'PROGRAM_EDIT',
                      'WEIGHT_TREND_ANALYSIS', 'EXERCISE_PROGRESS_ANALYSIS', 'WORKOUT_PROGRESS_ANALYSIS', 'ORACLE_TO_PROGRAM') then
        return jsonb_build_object('allowed', false, 'reason', 'unknown_tool', 'message', 'AI aracı tanınmadı.');
    end if;

    -- Idempotency: 20260519193000'deki blokla aynı davranış.
    if nullif(trim(coalesce(p_idempotency_key, '')), '') is not null then
        p_idempotency_key := trim(p_idempotency_key);
        perform pg_advisory_xact_lock(hashtextextended(p_user_id::text || ':' || p_idempotency_key, 0));
        select * into v_event from public.ai_usage_events
         where user_id = p_user_id and idempotency_key = p_idempotency_key limit 1 for update;
        if found then
            return jsonb_build_object(
                'allowed', false,
                'reason', case v_event.status when 'reserved' then 'idempotency_in_progress'
                                              when 'completed' then 'idempotency_completed'
                                              else 'idempotency_previous_failure' end,
                'message', 'Bu AI isteği daha önce işlendi veya işleniyor.',
                'usage_event_id', v_event.id
            );
        end if;
    else
        p_idempotency_key := null;
    end if;

    perform public.ensure_billing_account(p_user_id);
    select plan, status into v_plan, v_status from public.user_entitlements where user_id = p_user_id for update;
    if v_status not in ('active', 'trialing') or v_plan is null or v_plan = 'FREE' then
        v_plan := 'FREE';
    else
        v_plan := 'PREMIUM';   -- PRO / ELITE / PREMIUM
    end if;
    select * into v_settings from public.ai_access_settings where id;

    if v_plan = 'FREE' then
        if p_tool not in ('PROGRAM_GENERATE_TEXT', 'PROGRAM_GENERATE_MEDIA') then
            insert into public.ai_usage_events (user_id, tool, status, entitlement_source, credit_cost, error_code, idempotency_key)
            values (p_user_id, p_tool, 'denied', 'denied', 0, 'premium_required', p_idempotency_key);
            return jsonb_build_object('allowed', false, 'reason', 'premium_required',
                'message', 'Bu AI özelliği Premium''da.', 'plan', v_plan);
        end if;
        v_counter_tool := 'AI_PROGRAM_FREE_GRANT';   -- metin + dosya aynı ömür boyu sayaçta
        v_period := 'lifetime';
        v_period_start := '1970-01-01 00:00:00+00'::timestamptz;
        v_limit := v_settings.free_ai_program_grants;
    else
        -- Hız sınırı: son 1 dakikadaki tüm AI istekleri
        select count(*) into v_recent from public.ai_usage_events
         where user_id = p_user_id and status in ('reserved', 'completed') and created_at > v_now - interval '1 minute';
        if v_recent >= v_settings.premium_per_minute_cap then
            return jsonb_build_object('allowed', false, 'reason', 'rate_limited',
                'message', 'Çok hızlı istek gönderildi. Biraz sonra tekrar dene.', 'plan', v_plan);
        end if;
        v_counter_tool := 'AI_PREMIUM_DAILY';         -- tüm araçlar tek günlük tavanda
        v_period := 'day';
        v_period_start := date_trunc('day', v_now);
        v_limit := v_settings.premium_daily_cap;
    end if;

    insert into public.usage_counters (user_id, tool, period, period_start, used_count, updated_at)
    values (p_user_id, v_counter_tool, v_period, v_period_start, 0, v_now)
    on conflict (user_id, tool, period, period_start) do nothing;
    select used_count into v_used from public.usage_counters
     where user_id = p_user_id and tool = v_counter_tool and period = v_period and period_start = v_period_start for update;

    if v_used >= v_limit then
        insert into public.ai_usage_events (user_id, tool, status, entitlement_source, credit_cost, error_code, idempotency_key)
        values (p_user_id, p_tool, 'denied', 'denied', 0,
                case when v_plan = 'FREE' then 'free_grants_used' else 'fair_use_limit' end, p_idempotency_key);
        return jsonb_build_object(
            'allowed', false,
            'reason', case when v_plan = 'FREE' then 'free_grants_used' else 'fair_use_limit' end,
            'message', case when v_plan = 'FREE'
                then 'Ücretsiz AI program hakların bitti. Sınırsız AI program Premium''da.'
                else 'Bugünkü adil kullanım sınırına ulaştın. Yarın tekrar dene.' end,
            'plan', v_plan,
            'free_ai_programs_left', case when v_plan = 'FREE' then 0 else null end
        );
    end if;

    update public.usage_counters set used_count = used_count + 1, updated_at = v_now
     where user_id = p_user_id and tool = v_counter_tool and period = v_period and period_start = v_period_start;

    -- period/period_start olaya yazılır; refund_ai_usage bu alanlarla sayacı geri alır.
    -- DİKKAT (doğrulandı): refund_ai_usage (20260430090000) sayacı "tool = v_event.tool" ile buluyor; burada sayaç adı
    -- (v_counter_tool) araç adından farklı. AYNI migration'da refund_ai_usage, period='lifetime' ise 'AI_PROGRAM_FREE_GRANT',
    -- period='day' ve plan_limit ise 'AI_PREMIUM_DAILY' sayacını düşürecek şekilde güncellenmeli; yoksa hata iadesi hak geri vermez.
    insert into public.ai_usage_events (user_id, tool, status, entitlement_source, credit_cost, period, period_start, idempotency_key)
    values (p_user_id, p_tool, 'reserved', case when v_plan = 'FREE' then 'free_limit' else 'plan_limit' end,
            0, v_period, v_period_start, p_idempotency_key)
    returning * into v_event;

    return jsonb_build_object(
        'allowed', true,
        'usage_event_id', v_event.id,
        'credit_cost', 0,
        'source', v_event.entitlement_source,
        'plan', v_plan,
        'free_ai_programs_left', case when v_plan = 'FREE' then greatest(0, v_limit - v_used - 1) else null end
    );
end;
$$;

revoke all on function public.reserve_ai_usage(uuid, text, text) from public, anon, authenticated;
grant execute on function public.reserve_ai_usage(uuid, text, text) to service_role;

-- ── ADIM D (edge function notu, kod değil) ───────────────────────────────────
-- billing-status/index.ts: yanıta
--   freeAiProgramsLeft = plan FREE ise greatest(0, ai_access_settings.free_ai_program_grants -
--       usage_counters(tool='AI_PROGRAM_FREE_GRANT', period='lifetime').used_count), PREMIUM ise null
-- eklenmeli; "credits" alanı geçiş süresince 0/eski bakiye dönebilir (istemci artık göstermiyor).
