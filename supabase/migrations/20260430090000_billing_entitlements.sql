create extension if not exists pgcrypto;

create table if not exists public.billing_products (
    sku text primary key,
    kind text not null check (kind in ('credit_pack', 'subscription')),
    plan text check (plan in ('FREE', 'PRO', 'ELITE')),
    credit_amount integer not null default 0 check (credit_amount >= 0),
    title text not null,
    price_label text not null,
    active boolean not null default true,
    sort_order integer not null default 0,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.user_entitlements (
    user_id uuid primary key references auth.users(id) on delete cascade,
    plan text not null default 'FREE' check (plan in ('FREE', 'PRO', 'ELITE')),
    status text not null default 'free' check (status in ('free', 'active', 'trialing', 'past_due', 'canceled', 'expired')),
    current_period_start timestamptz,
    current_period_end timestamptz,
    source text not null default 'system',
    updated_at timestamptz not null default now()
);

create table if not exists public.user_credit_accounts (
    user_id uuid primary key references auth.users(id) on delete cascade,
    balance integer not null default 5 check (balance >= 0),
    lifetime_purchased integer not null default 0 check (lifetime_purchased >= 0),
    updated_at timestamptz not null default now()
);

create table if not exists public.credit_ledger (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    delta integer not null,
    balance_after integer not null check (balance_after >= 0),
    reason text not null,
    reference_id uuid,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now()
);

create table if not exists public.usage_counters (
    user_id uuid not null references auth.users(id) on delete cascade,
    tool text not null,
    period text not null check (period in ('day', 'week', 'month', 'lifetime')),
    period_start timestamptz not null,
    used_count integer not null default 0 check (used_count >= 0),
    updated_at timestamptz not null default now(),
    primary key (user_id, tool, period, period_start)
);

create table if not exists public.ai_usage_events (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    tool text not null,
    status text not null check (status in ('reserved', 'completed', 'refunded', 'denied')),
    entitlement_source text not null check (entitlement_source in ('free_limit', 'plan_limit', 'credit_balance', 'denied')),
    credit_cost integer not null default 0 check (credit_cost >= 0),
    period text check (period in ('day', 'week', 'month', 'lifetime')),
    period_start timestamptz,
    idempotency_key text,
    error_code text,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    completed_at timestamptz,
    refunded_at timestamptz
);

create unique index if not exists ai_usage_events_user_idempotency_idx
    on public.ai_usage_events (user_id, idempotency_key)
    where idempotency_key is not null;

create table if not exists public.billing_orders (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    sku text not null references public.billing_products(sku),
    status text not null default 'pending_provider'
        check (status in ('pending_provider', 'pending_payment', 'paid', 'failed', 'canceled', 'expired')),
    provider text,
    provider_session_id text,
    checkout_url text,
    amount_label text not null,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.billing_webhook_events (
    provider text not null,
    provider_event_id text not null,
    processed_at timestamptz not null default now(),
    payload jsonb not null,
    primary key (provider, provider_event_id)
);

alter table public.billing_products enable row level security;
alter table public.user_entitlements enable row level security;
alter table public.user_credit_accounts enable row level security;
alter table public.credit_ledger enable row level security;
alter table public.usage_counters enable row level security;
alter table public.ai_usage_events enable row level security;
alter table public.billing_orders enable row level security;
alter table public.billing_webhook_events enable row level security;

drop policy if exists "Products readable by authenticated users" on public.billing_products;
create policy "Products readable by authenticated users"
on public.billing_products for select to authenticated
using (active = true);

drop policy if exists "Users can read own entitlements" on public.user_entitlements;
create policy "Users can read own entitlements"
on public.user_entitlements for select to authenticated
using ((select auth.uid()) = user_id);

drop policy if exists "Users can read own credit account" on public.user_credit_accounts;
create policy "Users can read own credit account"
on public.user_credit_accounts for select to authenticated
using ((select auth.uid()) = user_id);

drop policy if exists "Users can read own credit ledger" on public.credit_ledger;
create policy "Users can read own credit ledger"
on public.credit_ledger for select to authenticated
using ((select auth.uid()) = user_id);

drop policy if exists "Users can read own ai usage events" on public.ai_usage_events;
create policy "Users can read own ai usage events"
on public.ai_usage_events for select to authenticated
using ((select auth.uid()) = user_id);

drop policy if exists "Users can read own billing orders" on public.billing_orders;
create policy "Users can read own billing orders"
on public.billing_orders for select to authenticated
using ((select auth.uid()) = user_id);

revoke all on table public.user_entitlements from anon, authenticated;
revoke all on table public.user_credit_accounts from anon, authenticated;
revoke all on table public.credit_ledger from anon, authenticated;
revoke all on table public.usage_counters from anon, authenticated;
revoke all on table public.ai_usage_events from anon, authenticated;
revoke all on table public.billing_orders from anon, authenticated;
revoke all on table public.billing_webhook_events from anon, authenticated;

grant select on table public.billing_products to authenticated;
grant select on table public.user_entitlements to authenticated;
grant select on table public.user_credit_accounts to authenticated;
grant select on table public.credit_ledger to authenticated;
grant select on table public.ai_usage_events to authenticated;
grant select on table public.billing_orders to authenticated;

grant all on table public.billing_products to service_role;
grant all on table public.user_entitlements to service_role;
grant all on table public.user_credit_accounts to service_role;
grant all on table public.credit_ledger to service_role;
grant all on table public.usage_counters to service_role;
grant all on table public.ai_usage_events to service_role;
grant all on table public.billing_orders to service_role;
grant all on table public.billing_webhook_events to service_role;

insert into public.billing_products (sku, kind, plan, credit_amount, title, price_label, sort_order, metadata)
values
    ('sub_pro_monthly', 'subscription', 'PRO', 0, 'Pro Aylık', '₺149/ay', 10, '{"billing_period":"month"}'),
    ('sub_pro_yearly', 'subscription', 'PRO', 0, 'Pro Yıllık', '₺999/yıl', 11, '{"billing_period":"year"}'),
    ('sub_elite_monthly', 'subscription', 'ELITE', 0, 'Elite Aylık', '₺249/ay', 20, '{"billing_period":"month"}'),
    ('sub_elite_yearly', 'subscription', 'ELITE', 0, 'Elite Yıllık', '₺1.799/yıl', 21, '{"billing_period":"year"}'),
    ('credits_10', 'credit_pack', null, 10, '10 AI Kredisi', '₺29', 30, '{}'),
    ('credits_50', 'credit_pack', null, 50, '50 AI Kredisi', '₺99', 31, '{"badge":"EN POPÜLER"}'),
    ('credits_200', 'credit_pack', null, 200, '200 AI Kredisi', '₺299', 32, '{}')
on conflict (sku) do update
set title = excluded.title,
    price_label = excluded.price_label,
    active = excluded.active,
    sort_order = excluded.sort_order,
    metadata = excluded.metadata,
    updated_at = now();

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
    values (p_user_id, 5)
    on conflict (user_id) do nothing;
end;
$$;

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

    perform public.ensure_billing_account(p_user_id);

    if p_idempotency_key is not null then
        select * into v_event
          from public.ai_usage_events
         where user_id = p_user_id and idempotency_key = p_idempotency_key
         limit 1;
        if found and v_event.status = 'reserved' then
            return jsonb_build_object(
                'allowed', true,
                'usage_event_id', v_event.id,
                'credit_cost', v_event.credit_cost,
                'source', v_event.entitlement_source,
                'plan', v_plan
            );
        end if;
    end if;

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

create or replace function public.complete_ai_usage(p_usage_event_id uuid)
returns void
language plpgsql
security invoker
set search_path = public, pg_temp
as $$
begin
    update public.ai_usage_events
       set status = 'completed',
           completed_at = now()
     where id = p_usage_event_id
       and status = 'reserved';
end;
$$;

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
        update public.usage_counters
           set used_count = greatest(used_count - 1, 0),
               updated_at = now()
         where user_id = v_event.user_id
           and tool = v_event.tool
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

create or replace function public.create_pending_billing_order(
    p_user_id uuid,
    p_sku text
)
returns jsonb
language plpgsql
security invoker
set search_path = public, pg_temp
as $$
declare
    v_product public.billing_products%rowtype;
    v_order public.billing_orders%rowtype;
begin
    perform public.ensure_billing_account(p_user_id);

    select * into v_product
      from public.billing_products
     where sku = p_sku
       and active = true;

    if not found then
        return jsonb_build_object('ok', false, 'code', 'invalid_sku', 'message', 'Paket bulunamadı.');
    end if;

    insert into public.billing_orders (user_id, sku, status, amount_label, metadata)
    values (
        p_user_id,
        p_sku,
        'pending_provider',
        v_product.price_label,
        jsonb_build_object(
            'kind', v_product.kind,
            'plan', v_product.plan,
            'credit_amount', v_product.credit_amount
        )
    )
    returning * into v_order;

    return jsonb_build_object(
        'ok', true,
        'order_id', v_order.id,
        'status', v_order.status,
        'message', 'Ödeme sağlayıcısı bağlanınca satın alma burada devam edecek.'
    );
end;
$$;

create or replace function public.apply_paid_billing_order(
    p_order_id uuid,
    p_provider text,
    p_provider_session_id text default null
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
begin
    select * into v_order
      from public.billing_orders
     where id = p_order_id
     for update;

    if not found then
        return jsonb_build_object('ok', false, 'code', 'order_not_found');
    end if;

    if v_order.status = 'paid' then
        return jsonb_build_object('ok', true, 'status', 'already_paid');
    end if;

    select * into v_product
      from public.billing_products
     where sku = v_order.sku
       and active = true;

    if not found then
        return jsonb_build_object('ok', false, 'code', 'product_not_found');
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

create or replace function public.cancel_user_subscription(
    p_user_id uuid,
    p_reason text default null
)
returns void
language plpgsql
security invoker
set search_path = public, pg_temp
as $$
begin
    perform public.ensure_billing_account(p_user_id);

    update public.user_entitlements
       set plan = 'FREE',
           status = 'canceled',
           source = coalesce(nullif(p_reason, ''), source),
           updated_at = now()
     where user_id = p_user_id;
end;
$$;

revoke all on function public.ensure_billing_account(uuid) from public, anon, authenticated;
revoke all on function public.reserve_ai_usage(uuid, text, text) from public, anon, authenticated;
revoke all on function public.complete_ai_usage(uuid) from public, anon, authenticated;
revoke all on function public.refund_ai_usage(uuid, text) from public, anon, authenticated;
revoke all on function public.create_pending_billing_order(uuid, text) from public, anon, authenticated;
revoke all on function public.apply_paid_billing_order(uuid, text, text) from public, anon, authenticated;
revoke all on function public.cancel_user_subscription(uuid, text) from public, anon, authenticated;

grant execute on function public.ensure_billing_account(uuid) to service_role;
grant execute on function public.reserve_ai_usage(uuid, text, text) to service_role;
grant execute on function public.complete_ai_usage(uuid) to service_role;
grant execute on function public.refund_ai_usage(uuid, text) to service_role;
grant execute on function public.create_pending_billing_order(uuid, text) to service_role;
grant execute on function public.apply_paid_billing_order(uuid, text, text) to service_role;
grant execute on function public.cancel_user_subscription(uuid, text) to service_role;
