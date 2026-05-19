-- Give new FREE users enough visible starter credits for onboarding plus
-- three more text AI program generations.
-- PROGRAM_GENERATE_TEXT costs 8 credits in reserve_ai_usage.

alter table public.user_credit_accounts
    alter column balance set default 32;

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
    values (p_user_id, 32)
    on conflict (user_id) do nothing;
end;
$$;

revoke all on function public.ensure_billing_account(uuid) from public, anon, authenticated;
grant execute on function public.ensure_billing_account(uuid) to service_role;

with eligible as (
    select account.user_id, account.balance as old_balance
      from public.user_credit_accounts account
      left join public.user_entitlements entitlement
        on entitlement.user_id = account.user_id
     where account.balance < 32
       and account.lifetime_purchased = 0
       and coalesce(entitlement.plan, 'FREE') = 'FREE'
       and coalesce(entitlement.status, 'free') = 'free'
       and not exists (
           select 1
             from public.ai_usage_events usage
            where usage.user_id = account.user_id
              and usage.status in ('reserved', 'completed')
       )
),
updated as (
    update public.user_credit_accounts account
       set balance = 32,
           updated_at = now()
      from eligible
     where account.user_id = eligible.user_id
     returning account.user_id, eligible.old_balance, account.balance
)
insert into public.credit_ledger (user_id, delta, balance_after, reason, metadata)
select
    user_id,
    balance - old_balance,
    balance,
    'starter_credit_top_up',
    jsonb_build_object('from', old_balance, 'to', 32, 'reason', 'allow_four_text_program_generations')
  from updated;
