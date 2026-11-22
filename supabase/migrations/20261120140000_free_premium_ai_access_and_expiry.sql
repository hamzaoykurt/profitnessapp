-- Ücretsiz + Premium AI erişim modeli ve sunucu tarafı abonelik süre bitimi (katkısal migration)
-- Kararlar (Hamza, 2026-11-20):
--   - FREE: yalnızca PROGRAM_GENERATE_TEXT / PROGRAM_GENERATE_MEDIA için ömür boyu 2 hak (onboarding dahil)
--   - PREMIUM: tüm AI araçları; adil kullanım günde 50 / dakikada 6 istek
--   - Enerji/kredi başlangıç bakiyesi 0; mevcut sandbox bakiyeleri yok sayılır (credit_ledger denetim izi korunur)
--   - Mevcut PRO/ELITE satırları effective_plan() ve istemci tarafında şeffaf biçimde PREMIUM sayılır.

-- ── A. Ayarlar tablosu ────────────────────────────────────────────────────────
create table if not exists public.ai_access_settings (
    id boolean primary key default true check (id),
    free_ai_program_grants integer not null default 2 check (free_ai_program_grants >= 0),
    premium_daily_cap integer not null default 50 check (premium_daily_cap > 0),
    premium_per_minute_cap integer not null default 6 check (premium_per_minute_cap > 0),
    updated_at timestamptz not null default now()
);

insert into public.ai_access_settings (id, free_ai_program_grants, premium_daily_cap, premium_per_minute_cap)
values (true, 2, 50, 6)
on conflict (id) do nothing;

alter table public.ai_access_settings enable row level security;
revoke all on table public.ai_access_settings from public, anon, authenticated;
grant all on table public.ai_access_settings to service_role;
create policy "ai_access_settings_deny_client" on public.ai_access_settings
    for all to anon, authenticated
    using (false)
    with check (false);

-- ── B. Yeni hesap başlangıç Enerjisi = 0 ───────────────────────────────────────
create or replace function public.ensure_billing_account(p_user_id uuid)
returns void
language plpgsql
security invoker
set search_path = public, pg_temp
as $$
begin
    if p_user_id is null then
        raise exception 'missing user id';
    end if;

    insert into public.user_entitlements (user_id, plan, status, source)
    values (p_user_id, 'FREE', 'free', 'system')
    on conflict (user_id) do nothing;

    insert into public.user_credit_accounts (user_id, balance)
    values (p_user_id, 0)
    on conflict (user_id) do nothing;
end;
$$;

revoke all on function public.ensure_billing_account(uuid) from public, anon, authenticated;
grant execute on function public.ensure_billing_account(uuid) to service_role;

-- ── C. Etkin plan yardımcı fonksiyonu (süre bitimi + PRO/ELITE -> PREMIUM) ─────
create or replace function public.effective_plan(
    p_plan text,
    p_status text,
    p_period_end timestamptz
)
returns text
language sql
stable
set search_path = public, pg_temp
as $$
  select case
    when p_plan is null or p_plan = 'FREE' then 'FREE'
    when p_status not in ('active', 'trialing') then 'FREE'
    when p_period_end is not null and p_period_end < now() then 'FREE'
    when p_plan in ('PRO', 'ELITE', 'PREMIUM') then 'PREMIUM'
    else 'FREE'
  end
$$;

revoke all on function public.effective_plan(text, text, timestamptz) from public, anon;
grant execute on function public.effective_plan(text, text, timestamptz) to authenticated, service_role;

-- ── D. reserve_ai_usage (Enerji yok, süre bitimi dahil) ────────────────────────
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
    v_period_end timestamptz;
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
    select plan, status, current_period_end
      into v_plan, v_status, v_period_end
      from public.user_entitlements
     where user_id = p_user_id
     for update;

    v_plan := public.effective_plan(v_plan, v_status, v_period_end);
    select * into v_settings from public.ai_access_settings where id;

    if v_plan = 'FREE' then
        if p_tool not in ('PROGRAM_GENERATE_TEXT', 'PROGRAM_GENERATE_MEDIA') then
            insert into public.ai_usage_events (user_id, tool, status, entitlement_source, credit_cost, error_code, idempotency_key)
            values (p_user_id, p_tool, 'denied', 'denied', 0, 'premium_required', p_idempotency_key);
            return jsonb_build_object('allowed', false, 'reason', 'premium_required',
                'message', 'Bu AI özelliği Premium''da.', 'plan', v_plan);
        end if;
        v_counter_tool := 'AI_PROGRAM_FREE_GRANT';
        v_period := 'lifetime';
        v_period_start := '1970-01-01 00:00:00+00'::timestamptz;
        v_limit := v_settings.free_ai_program_grants;
    else
        select count(*) into v_recent from public.ai_usage_events
         where user_id = p_user_id and status in ('reserved', 'completed') and created_at > v_now - interval '1 minute';
        if v_recent >= v_settings.premium_per_minute_cap then
            return jsonb_build_object('allowed', false, 'reason', 'rate_limited',
                'message', 'Çok hızlı istek gönderildi. Biraz sonra tekrar dene.', 'plan', v_plan);
        end if;
        v_counter_tool := 'AI_PREMIUM_DAILY';
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

-- ── E. refund_ai_usage (yeni sayaç adlarıyla uyumlu iade) ──────────────────────
create or replace function public.refund_ai_usage(
    p_usage_event_id uuid,
    p_error_code text default null
)
returns void
language plpgsql
security invoker
set search_path = public, pg_temp
as $$
declare
    v_event public.ai_usage_events%rowtype;
    v_balance integer;
    v_counter_tool text;
begin
    select * into v_event
      from public.ai_usage_events
     where id = p_usage_event_id
       and status = 'reserved'
     for update;

    if not found then
        return;
    end if;

    if v_event.credit_cost > 0 then
        update public.user_credit_accounts
           set balance = balance + v_event.credit_cost,
               updated_at = now()
         where user_id = v_event.user_id
         returning balance into v_balance;

        insert into public.credit_ledger (user_id, delta, balance_after, reason, reference_id, metadata)
        values (
            v_event.user_id,
            v_event.credit_cost,
            v_balance,
            'ai_usage_refunded',
            v_event.id,
            jsonb_build_object('tool', v_event.tool, 'error_code', p_error_code)
        );
    elsif v_event.period is not null and v_event.period_start is not null then
        v_counter_tool := case
            when v_event.entitlement_source = 'free_limit' and v_event.period = 'lifetime' then 'AI_PROGRAM_FREE_GRANT'
            when v_event.entitlement_source = 'plan_limit' and v_event.period = 'day' then 'AI_PREMIUM_DAILY'
            else v_event.tool
        end;

        update public.usage_counters
           set used_count = greatest(used_count - 1, 0),
               updated_at = now()
         where user_id = v_event.user_id
           and tool = v_counter_tool
           and period = v_event.period
           and period_start = v_event.period_start;
    end if;

    update public.ai_usage_events
       set status = 'refunded',
           error_code = p_error_code,
           refunded_at = now()
     where id = v_event.id;
end;
$$;

revoke all on function public.refund_ai_usage(uuid, text) from public, anon, authenticated;
grant execute on function public.refund_ai_usage(uuid, text) to service_role;
