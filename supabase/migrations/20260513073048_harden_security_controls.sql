-- Harden public challenge progress helper against arbitrary user_id probing.
create or replace function public._challenge_my_progress(p_challenge_id uuid, p_user_id uuid)
returns table(progress bigint, target_value bigint, is_completed boolean)
language plpgsql
stable security definer
set search_path to 'public', 'private', 'pg_temp'
as $function$
declare
  me uuid := auth.uid();
  c record;
  gp record;
  v_prog bigint := 0;
  v_target bigint := 0;
begin
  if me is null then
    raise exception 'unauthenticated';
  end if;

  if p_user_id is null or not private.can_read_challenge(p_challenge_id, me) then
    raise exception 'forbidden';
  end if;

  select * into c from public.group_challenges where id = p_challenge_id;
  if c is null then
    progress := 0; target_value := 0; is_completed := false; return next; return;
  end if;

  select * into gp
    from public.group_participants
   where challenge_id = p_challenge_id and user_id = p_user_id;

  if gp.user_id is null and p_user_id <> me then
    raise exception 'forbidden';
  end if;

  v_target := coalesce(c.target_value, 0)::bigint;

  if c.kind = 'event' and c.event_mode = 'movement_list' then
    if gp.user_id is not null then
      select count(*)::bigint into v_prog
        from public.challenge_movement_completions
       where challenge_id = p_challenge_id and user_id = p_user_id;
    end if;
  elsif c.kind = 'event' and c.event_mode in ('physical', 'online') then
    if gp.user_id is not null then
      v_prog := private.challenge_event_progress_value(p_challenge_id, p_user_id)
                + coalesce(gp.manual_progress, 0);
    end if;
    if v_target = 0 then v_target := 1; end if;
  else
    if gp.user_id is not null then
      v_prog := greatest(
        0,
        public._challenge_stat_value(p_user_id, c.target_type) - coalesce(gp.baseline_value, 0)
      );
    end if;
  end if;

  progress := v_prog;
  target_value := v_target;
  is_completed := (gp.completed_at is not null) or (v_target > 0 and v_prog >= v_target);
  return next;
end;
$function$;

revoke all on function public._challenge_my_progress(uuid, uuid) from public, anon, authenticated;

-- Require signed webhook assertions to match the order before applying entitlements.
create or replace function public.apply_paid_billing_order_verified(
    p_order_id uuid,
    p_provider text,
    p_provider_session_id text default null,
    p_sku text default null,
    p_amount_label text default null,
    p_amount_minor bigint default null,
    p_currency text default null
)
returns jsonb
language plpgsql
security invoker
set search_path = public, pg_temp
as $$
declare
    v_order public.billing_orders%rowtype;
    v_product public.billing_products%rowtype;
    v_balance integer;
    v_period_end timestamptz;
    v_expected_amount_minor bigint;
    v_expected_currency text;
begin
    select * into v_order
      from public.billing_orders
     where id = p_order_id
     for update;

    if not found then
        return jsonb_build_object('ok', false, 'code', 'order_not_found');
    end if;

    select * into v_product
      from public.billing_products
     where sku = v_order.sku
       and active = true;

    if not found then
        return jsonb_build_object('ok', false, 'code', 'product_not_found');
    end if;

    if nullif(trim(coalesce(p_sku, '')), '') is null or p_sku <> v_order.sku then
        return jsonb_build_object('ok', false, 'code', 'sku_mismatch');
    end if;

    if nullif(trim(coalesce(p_amount_label, '')), '') is null and p_amount_minor is null then
        return jsonb_build_object('ok', false, 'code', 'amount_required');
    end if;

    if nullif(trim(coalesce(p_amount_label, '')), '') is not null
       and p_amount_label <> v_order.amount_label
       and p_amount_label <> v_product.price_label then
        return jsonb_build_object('ok', false, 'code', 'amount_mismatch');
    end if;

    if (v_product.metadata ? 'amount_minor') and p_amount_minor is not null then
        if (v_product.metadata->>'amount_minor') !~ '^[0-9]+$' then
            return jsonb_build_object('ok', false, 'code', 'product_amount_not_configured');
        end if;
        v_expected_amount_minor := (v_product.metadata->>'amount_minor')::bigint;
        if p_amount_minor <> v_expected_amount_minor then
            return jsonb_build_object('ok', false, 'code', 'amount_mismatch');
        end if;
    end if;

    v_expected_currency := nullif(upper(trim(coalesce(v_product.metadata->>'currency', ''))), '');
    if v_expected_currency is not null
       and nullif(trim(coalesce(p_currency, '')), '') is not null
       and upper(trim(p_currency)) <> v_expected_currency then
        return jsonb_build_object('ok', false, 'code', 'currency_mismatch');
    end if;

    if v_order.status = 'paid' then
        return jsonb_build_object('ok', true, 'status', 'already_paid');
    end if;

    perform public.ensure_billing_account(v_order.user_id);

    if v_product.kind = 'credit_pack' then
        update public.user_credit_accounts
           set balance = balance + v_product.credit_amount,
               lifetime_purchased = lifetime_purchased + v_product.credit_amount,
               updated_at = now()
         where user_id = v_order.user_id
         returning balance into v_balance;

        insert into public.credit_ledger (user_id, delta, balance_after, reason, reference_id, metadata)
        values (
            v_order.user_id,
            v_product.credit_amount,
            v_balance,
            'purchase_paid',
            v_order.id,
            jsonb_build_object('sku', v_product.sku, 'provider', p_provider)
        );
    elsif v_product.kind = 'subscription' then
        v_period_end := case v_product.metadata->>'billing_period'
            when 'year' then now() + interval '1 year'
            else now() + interval '1 month'
        end;

        insert into public.user_entitlements (
            user_id, plan, status, current_period_start, current_period_end, source, updated_at
        )
        values (
            v_order.user_id, v_product.plan, 'active', now(), v_period_end, p_provider, now()
        )
        on conflict (user_id) do update
           set plan = excluded.plan,
               status = excluded.status,
               current_period_start = excluded.current_period_start,
               current_period_end = excluded.current_period_end,
               source = excluded.source,
               updated_at = now();
    end if;

    update public.billing_orders
       set status = 'paid',
           provider = p_provider,
           provider_session_id = coalesce(p_provider_session_id, provider_session_id),
           updated_at = now()
     where id = v_order.id;

    return jsonb_build_object('ok', true, 'status', 'paid', 'sku', v_product.sku);
end;
$$;

revoke all on function public.apply_paid_billing_order_verified(uuid, text, text, text, text, bigint, text) from public, anon, authenticated;
grant execute on function public.apply_paid_billing_order_verified(uuid, text, text, text, text, bigint, text) to service_role;

-- Make AI idempotency strict: one idempotency key cannot launch parallel provider calls.
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
            v_free_limit := case v_plan when 'ELITE' then 10000 when 'PRO' then 100 else 10 end;
        when 'PROGRAM_GENERATE_TEXT' then
            v_period := case when v_plan = 'FREE' then 'lifetime' else 'month' end;
            v_credit_cost := 8;
            v_free_limit := case v_plan when 'ELITE' then 100 when 'PRO' then 20 else 3 end;
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
            v_free_limit := case v_plan when 'ELITE' then 10000 when 'PRO' then 10 else 1 end;
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

        v_source := case when v_plan = 'FREE' then 'free_limit' else 'plan_limit' end;
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
