create table if not exists public.ai_rate_limits (
    user_id uuid primary key references auth.users(id) on delete cascade,
    window_started_at timestamptz not null default now(),
    request_count integer not null default 0,
    updated_at timestamptz not null default now()
);

alter table public.ai_rate_limits enable row level security;

drop policy if exists "No client access to ai rate limits" on public.ai_rate_limits;
create policy "No client access to ai rate limits"
on public.ai_rate_limits
for all
to anon, authenticated
using (false)
with check (false);

revoke all on table public.ai_rate_limits from anon, authenticated;
grant select, insert, update, delete on table public.ai_rate_limits to service_role;

create or replace function public.check_ai_rate_limit(
    p_user_id uuid,
    p_limit integer default 60,
    p_window_seconds integer default 3600
)
returns boolean
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    current_ts timestamptz := now();
    existing record;
begin
    if p_user_id is null then
        return false;
    end if;

    if p_limit < 1 or p_window_seconds < 60 then
        raise exception 'invalid ai rate limit configuration';
    end if;

    insert into public.ai_rate_limits (user_id, window_started_at, request_count, updated_at)
    values (p_user_id, current_ts, 1, current_ts)
    on conflict (user_id) do nothing;

    select *
      into existing
      from public.ai_rate_limits
     where user_id = p_user_id
     for update;

    if existing.window_started_at <= current_ts - make_interval(secs => p_window_seconds) then
        update public.ai_rate_limits
           set window_started_at = current_ts,
               request_count = 1,
               updated_at = current_ts
         where user_id = p_user_id;
        return true;
    end if;

    if existing.request_count >= p_limit then
        update public.ai_rate_limits
           set updated_at = current_ts
         where user_id = p_user_id;
        return false;
    end if;

    update public.ai_rate_limits
       set request_count = existing.request_count + 1,
           updated_at = current_ts
     where user_id = p_user_id;

    return true;
end;
$$;

revoke all on function public.check_ai_rate_limit(uuid, integer, integer) from public, anon, authenticated;
grant execute on function public.check_ai_rate_limit(uuid, integer, integer) to service_role;
