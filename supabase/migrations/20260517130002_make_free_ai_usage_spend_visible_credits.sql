-- Make FREE-plan AI usage spend the visible starter credit balance.
-- Paid plans keep their included plan allowance; free accounts no longer consume
-- hidden free allowances before the credit number shown in the app changes.

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
    v_period text := 'day';
    v_period_start timestamptz;
    v_free_limit integer := 0;
    v_credit_cost integer := 1;
    v_used integer := 0;
    v_balance integer := 0;
    v_source text := 'denied';
begin
    if p_user_id is null or nullif(trim(p_tool), '') is null then
        return jsonb_build_object('allowed', false, 'reason', 'invalid_request', 'message', 'AI isteği geçersiz.');
    end if;

    if nullif(trim(coalesce(p_idempotency_key, '')), '') is not null then
        p_idempotency_key := trim(p_idempotency_key);
        perform pg_advisory_xact_lock(hashtextextended(p_user_id::text || ':' || p_idempotency_key, 0));

        select * into v_event
          from public.ai_usage_events
         where user_id = p_user_id and idempotency_key = p_idempotency_key
         limit 1
         for update;

        if found then
            if v_event.status = 'reserved' then
                return jsonb_build_object(
                    'allowed', false,
                    'reason', 'idempotency_in_progress',
                    'message', 'Bu AI isteği zaten işleniyor.',
                    'usage_event_id', v_event.id
                );
            elsif v_event.status = 'completed' then
                return jsonb_build_object(
                    'allowed', false,
                    'reason', 'idempotency_completed',
                    'message', 'Bu AI isteği daha önce tamamlandı.',
                    'usage_event_id', v_event.id
                );
            else
                return jsonb_build_object(
                    'allowed', false,
                    'reason', 'idempotency_previous_failure',
                    'message', 'Bu idempotency anahtarı daha önce sonuçlandı. Yeni istek için yeni anahtar kullan.',
                    'usage_event_id', v_event.id
                );
            end if;
        end if;
    else
        p_idempotency_key := null;
    end if;

    perform public.ensure_billing_account(p_user_id);

    select plan, status
      into v_plan, v_status
      from public.user_entitlements
     where user_id = p_user_id
     for update;

    if v_status not in ('active', 'trialing') then
        v_plan := 'FREE';
    end if;

    perform 1
      from public.user_credit_accounts
     where user_id = p_user_id
     for update;

    case p_tool
        when 'ORACLE_CHAT' then
            v_period := 'day';
            v_credit_cost := 1;
            v_free_limit := case v_plan when 'ELITE' then 10000 when 'PRO' then 100 else 0 end;
        when 'PROGRAM_GENERATE_TEXT' then
            v_period := case when v_plan = 'FREE' then 'lifetime' else 'month' end;
            v_credit_cost := 8;
            v_free_limit := case v_plan when 'ELITE' then 100 when 'PRO' then 20 else 0 end;
        when 'PROGRAM_GENERATE_MEDIA' then
            v_period := case when v_plan = 'FREE' then 'lifetime' else 'month' end;
            v_credit_cost := 12;
            v_free_limit := case v_plan when 'ELITE' then 60 when 'PRO' then 10 else 0 end;
        when 'PROGRAM_EDIT' then
            v_period := 'month';
            v_credit_cost := 6;
            v_free_limit := case v_plan when 'ELITE' then 100 when 'PRO' then 30 else 0 end;
        when 'WEIGHT_TREND_ANALYSIS', 'EXERCISE_PROGRESS_ANALYSIS', 'WORKOUT_PROGRESS_ANALYSIS' then
            v_period := case when v_plan = 'FREE' then 'week' else 'day' end;
            v_credit_cost := 3;
            v_free_limit := case v_plan when 'ELITE' then 10000 when 'PRO' then 10 else 0 end;
        when 'ORACLE_TO_PROGRAM' then
            v_period := case when v_plan = 'FREE' then 'lifetime' else 'month' end;
            v_credit_cost := 6;
            v_free_limit := case v_plan when 'ELITE' then 100 when 'PRO' then 20 else 0 end;
        else
            return jsonb_build_object('allowed', false, 'reason', 'unknown_tool', 'message', 'AI aracı tanınmadı.');
    end case;

    v_period_start := case v_period
        when 'day' then date_trunc('day', v_now)
        when 'week' then date_trunc('week', v_now)
        when 'month' then date_trunc('month', v_now)
        else '1970-01-01 00:00:00+00'::timestamptz
    end;

    insert into public.usage_counters (user_id, tool, period, period_start, used_count, updated_at)
    values (p_user_id, p_tool, v_period, v_period_start, 0, v_now)
    on conflict (user_id, tool, period, period_start) do nothing;

    select used_count
      into v_used
      from public.usage_counters
     where user_id = p_user_id
       and tool = p_tool
       and period = v_period
       and period_start = v_period_start
     for update;

    if v_used < v_free_limit then
        update public.usage_counters
           set used_count = used_count + 1,
               updated_at = v_now
         where user_id = p_user_id
           and tool = p_tool
           and period = v_period
           and period_start = v_period_start;

        v_source := 'plan_limit';
        insert into public.ai_usage_events (
            user_id, tool, status, entitlement_source, credit_cost, period, period_start, idempotency_key
        )
        values (p_user_id, p_tool, 'reserved', v_source, 0, v_period, v_period_start, p_idempotency_key)
        returning * into v_event;

        return jsonb_build_object(
            'allowed', true,
            'usage_event_id', v_event.id,
            'credit_cost', 0,
            'source', v_source,
            'plan', v_plan
        );
    end if;

    select balance into v_balance
      from public.user_credit_accounts
     where user_id = p_user_id
     for update;

    if v_balance < v_credit_cost then
        insert into public.ai_usage_events (
            user_id, tool, status, entitlement_source, credit_cost, error_code, idempotency_key
        )
        values (p_user_id, p_tool, 'denied', 'denied', v_credit_cost, 'insufficient_entitlement', p_idempotency_key);

        return jsonb_build_object(
            'allowed', false,
            'reason', 'insufficient_entitlement',
            'message', 'Bu işlem için kredi veya abonelik hakkı gerekiyor.',
            'required_credits', v_credit_cost,
            'remaining_credits', v_balance,
            'plan', v_plan
        );
    end if;

    update public.user_credit_accounts
       set balance = balance - v_credit_cost,
           updated_at = v_now
     where user_id = p_user_id
     returning balance into v_balance;

    insert into public.ai_usage_events (
        user_id, tool, status, entitlement_source, credit_cost, idempotency_key
    )
    values (p_user_id, p_tool, 'reserved', 'credit_balance', v_credit_cost, p_idempotency_key)
    returning * into v_event;

    insert into public.credit_ledger (user_id, delta, balance_after, reason, reference_id, metadata)
    values (p_user_id, -v_credit_cost, v_balance, 'ai_usage_reserved', v_event.id, jsonb_build_object('tool', p_tool));

    return jsonb_build_object(
        'allowed', true,
        'usage_event_id', v_event.id,
        'credit_cost', v_credit_cost,
        'source', 'credit_balance',
        'remaining_credits', v_balance,
        'plan', v_plan
    );
end;
$$;

revoke all on function public.reserve_ai_usage(uuid, text, text) from public, anon, authenticated;
grant execute on function public.reserve_ai_usage(uuid, text, text) to service_role;
