create table if not exists public.set_completions (
  user_id uuid not null references auth.users(id) on delete cascade,
  exercise_id uuid not null references public.exercises(id) on delete cascade,
  program_day_id text not null,
  set_index integer not null,
  date date not null,
  weight_kg real check (weight_kg is null or weight_kg >= 0),
  reps_actual integer check (reps_actual is null or reps_actual >= 0),
  duration_seconds integer check (duration_seconds is null or duration_seconds >= 0),
  distance_meters real check (distance_meters is null or distance_meters >= 0),
  elevation_meters real check (elevation_meters is null or elevation_meters >= 0),
  incline_percent real,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now(),
  primary key (user_id, exercise_id, program_day_id, set_index, date)
);

alter table public.set_completions enable row level security;

grant select, insert, update, delete on public.set_completions to authenticated;

drop policy if exists "set_completions_select_own" on public.set_completions;
create policy "set_completions_select_own"
  on public.set_completions
  for select
  to authenticated
  using (auth.uid() = user_id);

drop policy if exists "set_completions_insert_own" on public.set_completions;
create policy "set_completions_insert_own"
  on public.set_completions
  for insert
  to authenticated
  with check (auth.uid() = user_id);

drop policy if exists "set_completions_update_own" on public.set_completions;
create policy "set_completions_update_own"
  on public.set_completions
  for update
  to authenticated
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

drop policy if exists "set_completions_delete_own" on public.set_completions;
create policy "set_completions_delete_own"
  on public.set_completions
  for delete
  to authenticated
  using (auth.uid() = user_id);

alter table public.exercises
  add column if not exists sport_type text not null default '',
  add column if not exists tracking_mode text not null default '';

alter table public.program_exercises
  add column if not exists target_duration_seconds integer check (target_duration_seconds is null or target_duration_seconds >= 0),
  add column if not exists target_distance_meters real check (target_distance_meters is null or target_distance_meters >= 0),
  add column if not exists target_elevation_meters real check (target_elevation_meters is null or target_elevation_meters >= 0),
  add column if not exists target_incline_percent real;

alter table public.set_completions
  add column if not exists elevation_meters real check (elevation_meters is null or elevation_meters >= 0),
  add column if not exists incline_percent real;

alter table public.group_challenges
  add column if not exists sport_type text,
  add column if not exists event_exercise_id uuid references public.exercises(id) on delete set null;

update public.exercises
set sport_type = case
    when lower(coalesce(name,'') || ' ' || coalesce(name_en,'')) ~ '(bisiklet|bike|cycling|cycle|spinning)' then 'cycling'
    when lower(coalesce(name,'') || ' ' || coalesce(name_en,'')) ~ '(swim|swimming|yüz|yuz)' then 'swimming'
    when lower(coalesce(name,'') || ' ' || coalesce(name_en,'')) ~ '(rowing|rower|outdoor row|kürek|kurek|ergometer)' then 'rowing'
    when lower(coalesce(name,'') || ' ' || coalesce(name_en,'')) ~ '(run|running|jog|treadmill|koş|kos)' then 'running'
    when lower(coalesce(name,'') || ' ' || coalesce(name_en,'')) ~ '(walk|walking|hike|hiking|yürüyüş|yuruyus|yürü|yuru)' then 'walking_hiking'
    when lower(coalesce(name,'') || ' ' || coalesce(name_en,'')) ~ '(jump rope|ip atlama|hiit|burpee|mountain climber)' then 'jump_rope_hiit'
    when lower(coalesce(name,'') || ' ' || coalesce(name_en,'')) ~ '(yoga|pilates|mobility|stretch|esneme)' then 'yoga_pilates'
    when lower(coalesce(name,'') || ' ' || coalesce(name_en,'')) ~ '(boxing|shadow boxing|boks|mma|kickbox)' then 'boxing'
    when lower(coalesce(name,'') || ' ' || coalesce(name_en,'')) ~ '(football|soccer|futbol|halı saha|hali saha)' then 'football'
    when lower(coalesce(name,'') || ' ' || coalesce(name_en,'')) ~ '(basketball|basket|tennis|tenis)' then 'basketball_tennis'
    else 'strength'
  end,
  tracking_mode = case
    when lower(coalesce(name,'') || ' ' || coalesce(name_en,'')) ~ '(yoga|pilates|mobility|stretch|esneme|boxing|shadow boxing|boks|hiit|jump rope|ip atlama)' then 'duration'
    when lower(coalesce(category,'') || ' ' || coalesce(name,'') || ' ' || coalesce(name_en,'')) ~ '(kardiyo|cardio|run|running|jog|treadmill|bisiklet|bike|cycling|swim|rowing|rower|walk|hike|yürüyüş|yuruyus)' then 'duration_distance'
    else 'strength'
  end
where sport_type = '' or tracking_mode = '';

insert into public.exercises (name, name_en, target_muscle, category, sets_default, reps_default, description, sport_type, tracking_mode, met_value)
values
  ('Outdoor Walk', 'Outdoor Walk', 'Kardiyovasküler', 'Kardiyo', 1, 30, 'Süre, mesafe ve yükselti takibi için yürüyüş/hiking aktivitesi.', 'walking_hiking', 'duration_distance', 3.5),
  ('Yoga Flow', 'Yoga Flow', 'Mobilite', 'Mobilite', 1, 30, 'Süre bazlı yoga ve mobilite akışı.', 'yoga_pilates', 'duration', 2.5),
  ('Football Training', 'Football Training', 'Kondisyon', 'Takım Sporları', 1, 60, 'Futbol/halı saha kondisyonu için süre bazlı aktivite.', 'football', 'duration', 7.0),
  ('Basketball Training', 'Basketball Training', 'Kondisyon', 'Takım Sporları', 1, 45, 'Basketbol veya tenis kondisyonu için süre bazlı aktivite.', 'basketball_tennis', 'duration', 6.5)
on conflict do nothing;

create or replace function private.challenge_event_progress_value(p_challenge_id uuid, p_user_id uuid)
returns bigint
language plpgsql
stable security definer
set search_path to 'public', 'pg_temp'
as $function$
declare
  c record;
  v_progress bigint := 0;
begin
  select * into c from public.group_challenges where id = p_challenge_id;
  if c is null then return 0; end if;

  select case c.target_type
      when 'total_duration_minutes' then coalesce(sum(coalesce(sc.duration_seconds, 0)) / 60, 0)::bigint
      when 'total_distance_km' then coalesce(sum(coalesce(sc.distance_meters, 0)) / 1000, 0)::bigint
      when 'total_distance_m' then coalesce(sum(coalesce(sc.distance_meters, 0)), 0)::bigint
      when 'total_workouts' then count(distinct sc.date)::bigint
      else coalesce(sum(coalesce(sc.duration_seconds, 0)) / 60, 0)::bigint
    end
    into v_progress
  from public.set_completions sc
  join public.exercises e on e.id = sc.exercise_id
  where sc.user_id = p_user_id
    and sc.date between c.start_date and c.end_date
    and coalesce(sc.reps_actual, 1) is not null
    and (
      (c.event_exercise_id is not null and sc.exercise_id = c.event_exercise_id)
      or (c.sport_type is not null and e.sport_type = c.sport_type)
    );

  return coalesce(v_progress, 0);
end;
$function$;

create or replace function public._challenge_my_progress(p_challenge_id uuid, p_user_id uuid)
returns table(progress bigint, target_value bigint, is_completed boolean)
language plpgsql
stable security definer
set search_path to 'public'
as $function$
declare
  c record;
  gp record;
  v_prog bigint := 0;
  v_target bigint := 0;
begin
  select * into c from group_challenges where id = p_challenge_id;
  if c is null then
    progress := 0; target_value := 0; is_completed := false; return next; return;
  end if;

  select * into gp
    from group_participants
   where challenge_id = p_challenge_id and user_id = p_user_id;

  v_target := coalesce(c.target_value, 0)::bigint;

  if c.kind = 'event' and c.event_mode = 'movement_list' then
    if gp.user_id is not null then
      select count(*)::bigint into v_prog
        from challenge_movement_completions
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

create or replace function private.create_event_challenge(
  p_title text,
  p_description text default null::text,
  p_event_mode text default 'physical'::text,
  p_event_date text default null::text,
  p_event_time text default null::text,
  p_event_timezone text default 'UTC'::text,
  p_event_location text default null::text,
  p_geo_lat double precision default null::double precision,
  p_geo_lng double precision default null::double precision,
  p_online_url text default null::text,
  p_target_type text default null::text,
  p_target_value bigint default null::bigint,
  p_visibility text default 'public'::text,
  p_password text default null::text,
  p_movements jsonb default '[]'::jsonb,
  p_end_geo_lat double precision default null::double precision,
  p_end_geo_lng double precision default null::double precision,
  p_end_location text default null::text,
  p_sport_type text default null::text,
  p_event_exercise_id uuid default null::uuid
)
returns text
language plpgsql
security definer
set search_path to 'public', 'extensions', 'pg_temp'
as $function$
declare
  v_user_id uuid := auth.uid();
  v_challenge_id uuid;
  v_event_date date;
  v_event_time time;
  v_target_type text;
  v_target_value integer;
  v_movement jsonb;
  v_password text := nullif(trim(coalesce(p_password, '')), '');
  v_movements jsonb := coalesce(p_movements, '[]'::jsonb);
  v_sport_type text := nullif(trim(coalesce(p_sport_type, '')), '');
begin
  if v_user_id is null then raise exception 'not_authenticated'; end if;
  if length(trim(coalesce(p_title, ''))) not between 3 and 120 then raise exception 'invalid_title'; end if;
  if p_event_mode not in ('physical','online','movement_list') then raise exception 'invalid_event_mode'; end if;
  if coalesce(p_visibility, 'public') not in ('public','private') then raise exception 'invalid_visibility'; end if;

  v_event_date := case when p_event_date is not null and p_event_date <> '' then p_event_date::date else current_date end;
  v_event_time := case when p_event_time is not null and p_event_time <> '' then p_event_time::time else null end;

  v_target_type := coalesce(
    p_target_type,
    case when p_event_mode = 'movement_list' then 'movements_completed' else 'total_distance_m' end
  );
  v_target_value := coalesce(
    p_target_value::integer,
    case when p_event_mode = 'movement_list' then greatest(jsonb_array_length(v_movements), 1) else 1000 end
  );
  if v_target_value <= 0 then raise exception 'invalid_target_value'; end if;

  insert into public.group_challenges (
    creator_id, kind, title, description,
    event_mode, sport_type, event_exercise_id,
    event_date, event_time, event_timezone,
    event_location, event_geo_lat, event_geo_lng, event_online_url,
    event_end_geo_lat, event_end_geo_lng, event_end_location,
    target_type, target_value,
    visibility, password, password_hash,
    start_date, end_date,
    participants_count
  ) values (
    v_user_id, 'event', trim(p_title), p_description,
    p_event_mode, v_sport_type, p_event_exercise_id,
    v_event_date, v_event_time, coalesce(p_event_timezone, 'UTC'),
    p_event_location, p_geo_lat, p_geo_lng, p_online_url,
    p_end_geo_lat, p_end_geo_lng, p_end_location,
    v_target_type, v_target_value,
    coalesce(p_visibility, 'public'), null,
    case when v_password is null then null else crypt(v_password, gen_salt('bf')) end,
    v_event_date, v_event_date,
    0
  ) returning id into v_challenge_id;

  insert into public.group_participants (challenge_id, user_id, baseline_value)
  values (v_challenge_id, v_user_id, 0);

  for v_movement in select * from jsonb_array_elements(v_movements) loop
    insert into public.challenge_movements (
      challenge_id, exercise_id, sort_index,
      suggested_sets, suggested_reps, suggested_dur_s
    ) values (
      v_challenge_id,
      (v_movement->>'exercise_id')::uuid,
      (v_movement->>'sort_index')::integer,
      case when v_movement->>'suggested_sets' not in ('null', '') and v_movement->>'suggested_sets' is not null then (v_movement->>'suggested_sets')::integer end,
      case when v_movement->>'suggested_reps' not in ('null', '') and v_movement->>'suggested_reps' is not null then (v_movement->>'suggested_reps')::integer end,
      case when v_movement->>'suggested_dur_s' not in ('null', '') and v_movement->>'suggested_dur_s' is not null then (v_movement->>'suggested_dur_s')::integer end
    );
  end loop;

  return v_challenge_id::text;
end;
$function$;

create or replace function public.create_event_challenge(
  p_title text,
  p_description text default null::text,
  p_event_mode text default 'physical'::text,
  p_event_date text default null::text,
  p_event_time text default null::text,
  p_event_timezone text default 'UTC'::text,
  p_event_location text default null::text,
  p_geo_lat double precision default null::double precision,
  p_geo_lng double precision default null::double precision,
  p_online_url text default null::text,
  p_target_type text default null::text,
  p_target_value bigint default null::bigint,
  p_visibility text default 'public'::text,
  p_password text default null::text,
  p_movements jsonb default '[]'::jsonb,
  p_end_geo_lat double precision default null::double precision,
  p_end_geo_lng double precision default null::double precision,
  p_end_location text default null::text,
  p_sport_type text default null::text,
  p_event_exercise_id uuid default null::uuid
)
returns text
language sql
set search_path to 'public', 'private', 'extensions', 'pg_temp'
as $function$
  select private.create_event_challenge(
    $1, $2, $3, $4, $5, $6, $7, $8, $9, $10,
    $11, $12, $13, $14, $15, $16, $17, $18, $19, $20
  )
$function$;

create or replace function private.get_challenge_detail(p_challenge_id uuid)
returns jsonb
language plpgsql
stable security definer
set search_path to 'public', 'pg_temp'
as $function$
declare
  me uuid := auth.uid();
  v_challenge record;
  v_my_row record;
  v_leaderboard jsonb;
  v_movements jsonb;
  v_my_prog record;
begin
  if me is null then raise exception 'unauthenticated'; end if;

  select gc.*,
         coalesce(nullif(p.display_name,''), p.username, 'Anonim') as creator_name,
         p.avatar_url as creator_avatar
    into v_challenge
    from public.group_challenges gc
    left join public.profiles p on p.user_id = gc.creator_id
   where gc.id = p_challenge_id;

  if v_challenge is null then raise exception 'challenge_not_found'; end if;
  if not private.can_read_challenge(p_challenge_id, me) then raise exception 'forbidden'; end if;

  select * into v_my_row from public.group_participants
   where challenge_id = p_challenge_id and user_id = me;

  select * into v_my_prog from public._challenge_my_progress(p_challenge_id, me);

  select coalesce(jsonb_agg(row_to_json(lb) order by lb.progress desc, lb.joined_at asc), '[]'::jsonb)
    into v_leaderboard
    from (
      select gp.user_id,
             coalesce(nullif(pr.display_name,''), pr.username, 'Anonim') as display_name,
             pr.avatar_url,
             case
               when v_challenge.kind = 'event' and v_challenge.event_mode = 'movement_list' then
                 coalesce((select count(*) from public.challenge_movement_completions cmc where cmc.challenge_id = p_challenge_id and cmc.user_id = gp.user_id), 0)::bigint
               when v_challenge.kind = 'event' and v_challenge.event_mode in ('physical', 'online') then
                 coalesce((select mp.progress from public._challenge_my_progress(p_challenge_id, gp.user_id) mp), 0)
               else
                 greatest(0, public._challenge_stat_value(gp.user_id, v_challenge.target_type) - gp.baseline_value)
             end as progress,
             gp.joined_at,
             (gp.completed_at is not null) as is_completed,
             (gp.user_id = me) as is_me
      from public.group_participants gp
      left join public.profiles pr on pr.user_id = gp.user_id
      where gp.challenge_id = p_challenge_id
      order by progress desc, gp.joined_at asc
      limit 50
    ) lb;

  select coalesce(jsonb_agg(row_to_json(mv) order by mv.sort_index asc), '[]'::jsonb)
    into v_movements
    from (
      select m.id, m.exercise_id, e.name as exercise_name,
             m.sort_index, m.suggested_sets, m.suggested_reps, m.suggested_dur_s,
             exists (
               select 1 from public.challenge_movement_completions cmc
                where cmc.challenge_id = p_challenge_id
                  and cmc.movement_id = m.id
                  and cmc.user_id = me
             ) as my_completed
      from public.challenge_movements m
      left join public.exercises e on e.id = m.exercise_id
      where m.challenge_id = p_challenge_id
      order by m.sort_index asc
    ) mv;

  return jsonb_build_object(
    'id', v_challenge.id,
    'title', v_challenge.title,
    'description', v_challenge.description,
    'target_type', v_challenge.target_type,
    'target_value', v_challenge.target_value,
    'start_date', v_challenge.start_date,
    'end_date', v_challenge.end_date,
    'visibility', v_challenge.visibility,
    'participants_count', v_challenge.participants_count,
    'creator_id', v_challenge.creator_id,
    'creator_name', v_challenge.creator_name,
    'creator_avatar', v_challenge.creator_avatar,
    'is_joined', (v_my_row.user_id is not null),
    'my_progress', v_my_prog.progress,
    'is_completed', v_my_prog.is_completed,
    'kind', v_challenge.kind,
    'event_mode', v_challenge.event_mode,
    'sport_type', v_challenge.sport_type,
    'event_exercise_id', v_challenge.event_exercise_id,
    'event_date', v_challenge.event_date,
    'event_time', v_challenge.event_time,
    'event_timezone', v_challenge.event_timezone,
    'event_location', v_challenge.event_location,
    'event_geo_lat', v_challenge.event_geo_lat,
    'event_geo_lng', v_challenge.event_geo_lng,
    'event_end_geo_lat', v_challenge.event_end_geo_lat,
    'event_end_geo_lng', v_challenge.event_end_geo_lng,
    'event_end_location', v_challenge.event_end_location,
    'event_online_url', v_challenge.event_online_url,
    'movements', v_movements,
    'leaderboard', v_leaderboard
  );
end;
$function$;

drop function if exists public.list_my_events_for_date(date);
drop function if exists private.list_my_events_for_date(date);

create function private.list_my_events_for_date(p_date date)
returns table(id uuid, title text, description text, target_type text, target_value bigint, start_date date, end_date date, participants_count integer, visibility text, creator_id uuid, creator_name text, creator_avatar text, my_progress bigint, is_completed boolean, joined_at timestamp with time zone, kind text, event_mode text, sport_type text, event_exercise_id uuid, event_date date, event_time time without time zone, event_timezone text, event_location text, event_geo_lat double precision, event_geo_lng double precision, event_online_url text, movements_count integer, my_completed_count integer)
language plpgsql
stable security definer
set search_path to 'public'
as $function$
declare me uuid := auth.uid();
begin
  return query
  select
    gc.id, gc.title, gc.description, gc.target_type, gc.target_value::bigint,
    gc.start_date, gc.end_date, gc.participants_count, gc.visibility, gc.creator_id,
    coalesce(nullif(p.display_name, ''), p.username, 'Anonim'),
    p.avatar_url,
    coalesce((select mp.progress from public._challenge_my_progress(gc.id, me) mp), 0),
    coalesce((select mp.is_completed from public._challenge_my_progress(gc.id, me) mp), false),
    gp.joined_at,
    gc.kind, gc.event_mode, gc.sport_type, gc.event_exercise_id, gc.event_date, gc.event_time,
    gc.event_timezone, gc.event_location, gc.event_geo_lat, gc.event_geo_lng,
    gc.event_online_url,
    coalesce((select count(*)::int from challenge_movements m where m.challenge_id = gc.id), 0),
    coalesce((select count(*)::int
                from challenge_movement_completions cmc
               where cmc.challenge_id = gc.id and cmc.user_id = me), 0)
  from group_participants gp
  join group_challenges   gc on gc.id = gp.challenge_id
  left join profiles p on p.user_id = gc.creator_id
  where gp.user_id = me
    and gc.kind = 'event'
    and gc.event_date = p_date
  order by coalesce(gc.event_time, time '00:00'), gc.created_at asc;
end;
$function$;

create function public.list_my_events_for_date(p_date date)
returns table(id uuid, title text, description text, target_type text, target_value bigint, start_date date, end_date date, participants_count integer, visibility text, creator_id uuid, creator_name text, creator_avatar text, my_progress bigint, is_completed boolean, joined_at timestamp with time zone, kind text, event_mode text, sport_type text, event_exercise_id uuid, event_date date, event_time time without time zone, event_timezone text, event_location text, event_geo_lat double precision, event_geo_lng double precision, event_online_url text, movements_count integer, my_completed_count integer)
language sql
stable
set search_path to 'public', 'private', 'pg_temp'
as $function$ select * from private.list_my_events_for_date($1) $function$;

drop function if exists public.list_my_upcoming_events(integer);
drop function if exists private.list_my_upcoming_events(integer);

create function private.list_my_upcoming_events(p_days integer default 7)
returns table(id uuid, title text, description text, target_type text, target_value bigint, start_date date, end_date date, participants_count integer, visibility text, creator_id uuid, creator_name text, creator_avatar text, my_progress bigint, is_completed boolean, joined_at timestamp with time zone, kind text, event_mode text, sport_type text, event_exercise_id uuid, event_date date, event_time time without time zone, event_timezone text, event_location text, event_geo_lat double precision, event_geo_lng double precision, event_online_url text, movements_count integer, my_completed_count integer)
language plpgsql
stable security definer
set search_path to 'public'
as $function$
declare me uuid := auth.uid(); v_days int := greatest(1, least(30, p_days));
begin
  return query
  select
    gc.id, gc.title, gc.description, gc.target_type, gc.target_value::bigint,
    gc.start_date, gc.end_date, gc.participants_count, gc.visibility, gc.creator_id,
    coalesce(nullif(p.display_name, ''), p.username, 'Anonim'),
    p.avatar_url,
    coalesce((select mp.progress from public._challenge_my_progress(gc.id, me) mp), 0),
    coalesce((select mp.is_completed from public._challenge_my_progress(gc.id, me) mp), false),
    gp.joined_at,
    gc.kind, gc.event_mode, gc.sport_type, gc.event_exercise_id, gc.event_date, gc.event_time,
    gc.event_timezone, gc.event_location, gc.event_geo_lat, gc.event_geo_lng,
    gc.event_online_url,
    coalesce((select count(*)::int from challenge_movements m where m.challenge_id = gc.id), 0),
    coalesce((select count(*)::int
                from challenge_movement_completions cmc
               where cmc.challenge_id = gc.id and cmc.user_id = me), 0)
  from group_participants gp
  join group_challenges   gc on gc.id = gp.challenge_id
  left join profiles p on p.user_id = gc.creator_id
  where gp.user_id = me
    and gc.kind = 'event'
    and gc.event_date > current_date
    and gc.event_date <= current_date + (v_days || ' days')::interval
  order by gc.event_date asc, coalesce(gc.event_time, time '00:00');
end;
$function$;

create function public.list_my_upcoming_events(p_days integer default 7)
returns table(id uuid, title text, description text, target_type text, target_value bigint, start_date date, end_date date, participants_count integer, visibility text, creator_id uuid, creator_name text, creator_avatar text, my_progress bigint, is_completed boolean, joined_at timestamp with time zone, kind text, event_mode text, sport_type text, event_exercise_id uuid, event_date date, event_time time without time zone, event_timezone text, event_location text, event_geo_lat double precision, event_geo_lng double precision, event_online_url text, movements_count integer, my_completed_count integer)
language sql
stable
set search_path to 'public', 'private', 'pg_temp'
as $function$ select * from private.list_my_upcoming_events($1) $function$;
