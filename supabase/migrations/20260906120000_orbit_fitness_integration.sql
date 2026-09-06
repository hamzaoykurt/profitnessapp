create table if not exists public.orbit_connections (
  user_id uuid primary key references auth.users(id) on delete cascade,
  orbit_account_id text not null unique,
  account_label text,
  status text not null default 'connected' check (status in ('connected', 'reconnect_required')),
  sync_enabled boolean not null default false,
  fitness_sync_entitled boolean not null default false,
  entitlement_checked_at timestamptz,
  manage_url text,
  last_synced_at timestamptz,
  last_error_code text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.orbit_connections enable row level security;
revoke all on public.orbit_connections from anon, authenticated;
grant select on public.orbit_connections to authenticated;

drop policy if exists "Users can view own Orbit connection" on public.orbit_connections;
create policy "Users can view own Orbit connection"
  on public.orbit_connections for select to authenticated
  using ((select auth.uid()) = user_id);

create table if not exists public.orbit_sync_deliveries (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  idempotency_key text not null,
  status text not null check (status in ('pending', 'delivered', 'failed')),
  error_code text,
  created_at timestamptz not null default now(),
  delivered_at timestamptz,
  unique (user_id, idempotency_key)
);

alter table public.orbit_sync_deliveries enable row level security;
revoke all on public.orbit_sync_deliveries from anon, authenticated;

create table if not exists public.orbit_webhook_events (
  provider_event_id text primary key,
  event_type text not null,
  received_at timestamptz not null default now()
);
alter table public.orbit_webhook_events enable row level security;
revoke all on public.orbit_webhook_events from anon, authenticated;

create table if not exists public.orbit_link_attempts (
  nonce uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  expires_at timestamptz not null,
  consumed_at timestamptz,
  created_at timestamptz not null default now()
);
create index if not exists orbit_link_attempts_user_idx on public.orbit_link_attempts (user_id, expires_at desc);
alter table public.orbit_link_attempts enable row level security;
revoke all on public.orbit_link_attempts from anon, authenticated;

create or replace function public.get_orbit_fitness_summary(
  p_user_id uuid,
  p_timezone text default 'UTC',
  p_now timestamptz default now()
) returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_timezone text;
  v_today date;
  v_week_start date;
  v_program_id uuid;
  v_weekly_target integer := 0;
  v_completed integer := 0;
  v_today_name text;
  v_today_status text := 'not_scheduled';
  v_last_completed timestamptz;
  v_last_completed_name text;
  v_schedule jsonb := '[]'::jsonb;
begin
  select name into v_timezone from pg_catalog.pg_timezone_names where name = p_timezone limit 1;
  v_timezone := coalesce(v_timezone, 'UTC');
  v_today := (p_now at time zone v_timezone)::date;
  v_week_start := v_today - (extract(isodow from v_today)::integer - 1);

  with completed_sessions as (
    select wl.id, wl.date, wl.finished_at, pd.title
    from public.workout_logs wl
    join public.program_days pd on pd.id = wl.program_day_id
    where wl.user_id = p_user_id
      and exists (
        select 1 from public.program_exercises pe
        where pe.program_day_id = wl.program_day_id
      )
      and not exists (
        select 1 from public.program_exercises pe
        where pe.program_day_id = wl.program_day_id
          and not exists (
            select 1 from public.exercise_logs el
            where el.workout_log_id = wl.id
              and el.exercise_id = pe.exercise_id
              and el.is_completed = true
          )
      )
  )
  select count(*) filter (where date between v_week_start and v_week_start + 6)::integer,
         max(coalesce(finished_at, date::timestamp at time zone v_timezone)),
         (array_agg(title order by coalesce(finished_at, date::timestamp at time zone v_timezone) desc))[1]
    into v_completed, v_last_completed, v_last_completed_name
  from completed_sessions;

  select p.id into v_program_id
  from public.programs p
  where p.user_id = p_user_id and p.is_active = true
  order by p.created_at desc limit 1;

  if v_program_id is not null then
    select count(*)::integer,
           coalesce(jsonb_agg(jsonb_build_object(
             'dayIndex', pd.day_index,
             'name', pd.title,
             'isRestDay', pd.is_rest_day
           ) order by pd.day_index), '[]'::jsonb)
      into v_weekly_target, v_schedule
    from public.program_days pd
    where pd.program_id = v_program_id and pd.is_rest_day = false;

    select pd.title into v_today_name
    from public.program_days pd
    where pd.program_id = v_program_id
      and pd.day_index = extract(isodow from v_today)::integer - 1
      and pd.is_rest_day = false
    limit 1;

    if v_today_name is not null then
      if exists (
        select 1 from public.workout_logs wl
        where wl.user_id = p_user_id and wl.date = v_today
          and wl.program_day_id = (
            select pd.id from public.program_days pd
            where pd.program_id = v_program_id
              and pd.day_index = extract(isodow from v_today)::integer - 1
            limit 1
          )
          and not exists (
            select 1 from public.program_exercises pe
            where pe.program_day_id = wl.program_day_id
              and not exists (
                select 1 from public.exercise_logs el
                where el.workout_log_id = wl.id
                  and el.exercise_id = pe.exercise_id
                  and el.is_completed = true
              )
          )
      ) then v_today_status := 'completed';
      elsif exists (
        select 1 from public.workout_logs wl
        where wl.user_id = p_user_id and wl.date = v_today
      ) then v_today_status := 'in_progress';
      else v_today_status := 'scheduled';
      end if;
    end if;
  end if;

  return jsonb_build_object(
    'weekStartsOn', v_week_start,
    'timeZone', v_timezone,
    'weeklyTarget', v_weekly_target,
    'completedThisWeek', v_completed,
    'today', jsonb_build_object('name', v_today_name, 'status', v_today_status),
    'lastCompleted', jsonb_build_object('name', v_last_completed_name, 'completedAt', v_last_completed),
    'schedule', v_schedule
  );
end;
$$;

revoke all on function public.get_orbit_fitness_summary(uuid, text, timestamptz) from public, anon, authenticated;
grant execute on function public.get_orbit_fitness_summary(uuid, text, timestamptz) to service_role;
