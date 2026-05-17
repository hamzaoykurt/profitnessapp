alter table public.group_challenges
  add column if not exists max_participants integer check (max_participants is null or max_participants >= 2);

update public.exercises
set sport_type = 'indoor_football',
    tracking_mode = 'duration'
where lower(coalesce(name,'') || ' ' || coalesce(name_en,'') || ' ' || coalesce(category,'')) ~ '(halı saha|hali saha|halisaha|futsal)';

insert into public.exercises (name, name_en, target_muscle, category, sets_default, reps_default, description, sport_type, tracking_mode, met_value)
values
  ('Halı Saha Maçı', 'Indoor Football Match', 'Kondisyon', 'Sosyal Sporlar', 1, 60, 'Halı saha ve futsal gibi sosyal futbol etkinlikleri için süre bazlı aktivite.', 'indoor_football', 'duration', 7.0),
  ('Voleybol', 'Volleyball', 'Kondisyon', 'Sosyal Sporlar', 1, 45, 'Voleybol ve benzeri sosyal takım sporları için süre bazlı aktivite.', 'volleyball', 'duration', 4.0)
on conflict do nothing;

drop function if exists public.create_challenge(text, text, text, bigint, date, date, text, text);
drop function if exists private.create_challenge(text, text, text, bigint, date, date, text, text);

create function private.create_challenge(
  p_title text,
  p_description text,
  p_target_type text,
  p_target_value bigint,
  p_start_date date,
  p_end_date date,
  p_visibility text default 'public'::text,
  p_password text default null::text,
  p_max_participants integer default null::integer
)
returns uuid
language plpgsql
security definer
set search_path to 'public', 'pg_temp'
as $function$
declare
  me uuid := auth.uid();
  v_id uuid;
  v_base bigint;
  v_password text := nullif(trim(coalesce(p_password, '')), '');
begin
  if me is null then raise exception 'unauthenticated'; end if;
  if length(trim(coalesce(p_title, ''))) not between 3 and 120 then raise exception 'invalid_title'; end if;
  if p_target_type not in ('total_workouts','total_xp','current_streak','total_duration_minutes','total_distance_m','total_distance_km') then
    raise exception 'invalid_target_type';
  end if;
  if p_visibility not in ('public','private') then raise exception 'invalid_visibility'; end if;
  if p_end_date < p_start_date then raise exception 'invalid_date_range'; end if;
  if p_target_value <= 0 then raise exception 'invalid_target_value'; end if;
  if p_max_participants is not null and p_max_participants < 2 then raise exception 'invalid_max_participants'; end if;

  insert into public.group_challenges (
    title, description, target_type, target_value,
    start_date, end_date, visibility, password, password_hash,
    creator_id, participants_count, max_participants
  ) values (
    trim(p_title), coalesce(trim(p_description), ''), p_target_type, p_target_value,
    p_start_date, p_end_date, p_visibility, null,
    case when v_password is null then null else crypt(v_password, gen_salt('bf')) end,
    me, 0, p_max_participants
  ) returning id into v_id;

  v_base := public._challenge_stat_value(me, p_target_type);
  insert into public.group_participants (challenge_id, user_id, progress, baseline_value)
  values (v_id, me, 0, v_base);

  return v_id;
end;
$function$;

create function public.create_challenge(
  p_title text,
  p_description text,
  p_target_type text,
  p_target_value bigint,
  p_start_date date,
  p_end_date date,
  p_visibility text default 'public'::text,
  p_password text default null::text,
  p_max_participants integer default null::integer
)
returns uuid
language sql
set search_path to 'public', 'private', 'pg_temp'
as $function$
  select private.create_challenge($1, $2, $3, $4, $5, $6, $7, $8, $9)
$function$;

create or replace function private.join_challenge(
  p_challenge_id uuid,
  p_password text default null
)
returns boolean
language plpgsql
security definer
set search_path to 'public', 'pg_temp'
as $function$
declare
  me uuid := auth.uid();
  v_chal record;
  v_base bigint;
  v_has_invite boolean := false;
  v_already_joined boolean := false;
begin
  if me is null then raise exception 'unauthenticated'; end if;
  select * into v_chal from public.group_challenges where id = p_challenge_id for update;
  if v_chal is null then raise exception 'challenge_not_found'; end if;
  if v_chal.end_date < current_date then raise exception 'challenge_ended'; end if;

  select exists(
    select 1 from public.group_participants
    where challenge_id = p_challenge_id and user_id = me
  ) into v_already_joined;

  if not v_already_joined
     and v_chal.max_participants is not null
     and v_chal.participants_count >= v_chal.max_participants then
    raise exception 'challenge_full';
  end if;

  select exists(
    select 1 from public.challenge_invites
    where challenge_id = p_challenge_id
      and invitee_id = me
      and status = 'pending'
  ) into v_has_invite;

  if v_chal.visibility = 'private' and not v_has_invite then
    if v_chal.password_hash is not null then
      if crypt(coalesce(p_password, ''), v_chal.password_hash) <> v_chal.password_hash then
        raise exception 'wrong_password';
      end if;
    elsif v_chal.password is not null and v_chal.password <> coalesce(p_password, '') then
      raise exception 'wrong_password';
    end if;
  end if;

  v_base := public._challenge_stat_value(me, v_chal.target_type);
  insert into public.group_participants (challenge_id, user_id, progress, baseline_value)
  values (p_challenge_id, me, 0, v_base)
  on conflict (challenge_id, user_id) do nothing;

  update public.challenge_invites
     set status = 'accepted'
   where challenge_id = p_challenge_id
     and invitee_id = me
     and status = 'pending';

  return true;
end;
$function$;

drop function if exists public.list_visible_challenges(integer, integer);
drop function if exists private.list_visible_challenges(integer, integer);

create function private.list_visible_challenges(
  p_limit integer default 50,
  p_offset integer default 0
)
returns table(
  id uuid,
  title text,
  description text,
  target_type text,
  target_value bigint,
  start_date date,
  end_date date,
  participants_count integer,
  max_participants integer,
  visibility text,
  creator_id uuid,
  creator_name text,
  creator_avatar text,
  is_joined boolean,
  is_invited boolean,
  my_progress bigint,
  is_completed boolean,
  created_at timestamptz,
  kind text,
  event_mode text,
  sport_type text,
  event_exercise_id uuid,
  event_date date,
  event_time time,
  event_timezone text,
  event_location text,
  event_geo_lat double precision,
  event_geo_lng double precision,
  event_end_geo_lat double precision,
  event_end_geo_lng double precision,
  event_end_location text,
  event_online_url text,
  movements_count integer,
  my_completed_count integer
)
language plpgsql
stable security definer
set search_path to 'public'
as $function$
declare
  me uuid := auth.uid();
begin
  return query
  select
    gc.id, gc.title, gc.description, gc.target_type, gc.target_value::bigint,
    gc.start_date, gc.end_date, gc.participants_count, gc.max_participants, gc.visibility, gc.creator_id,
    coalesce(nullif(p.display_name, ''), p.username, 'Anonim'),
    p.avatar_url,
    (gp.user_id is not null),
    exists (
      select 1 from public.challenge_invites ci
      where ci.challenge_id = gc.id
        and ci.invitee_id = me
        and ci.status = 'pending'
    ),
    coalesce((select mp.progress from public._challenge_my_progress(gc.id, me) mp), 0),
    coalesce((select mp.is_completed from public._challenge_my_progress(gc.id, me) mp), false),
    gc.created_at,
    gc.kind, gc.event_mode, gc.sport_type, gc.event_exercise_id,
    gc.event_date, gc.event_time, gc.event_timezone, gc.event_location,
    gc.event_geo_lat, gc.event_geo_lng,
    gc.event_end_geo_lat, gc.event_end_geo_lng, gc.event_end_location,
    gc.event_online_url,
    coalesce((select count(*)::int from public.challenge_movements m where m.challenge_id = gc.id), 0),
    coalesce((select count(*)::int
                from public.challenge_movement_completions c
               where c.challenge_id = gc.id and c.user_id = me), 0)
  from public.group_challenges gc
  left join public.profiles p on p.user_id = gc.creator_id
  left join public.group_participants gp on gp.challenge_id = gc.id and gp.user_id = me
  where gc.end_date >= current_date
    and (
      gc.visibility = 'public'
      or exists (
        select 1 from public.challenge_invites ci
        where ci.challenge_id = gc.id
          and ci.invitee_id = me
          and ci.status = 'pending'
      )
    )
  order by
    case when exists (
      select 1 from public.challenge_invites ci
      where ci.challenge_id = gc.id and ci.invitee_id = me and ci.status = 'pending'
    ) then 0 else 1 end,
    gc.created_at desc
  limit greatest(1, least(100, p_limit))
  offset greatest(0, p_offset);
end;
$function$;

create function public.list_visible_challenges(
  p_limit integer default 50,
  p_offset integer default 0
)
returns table(
  id uuid,
  title text,
  description text,
  target_type text,
  target_value bigint,
  start_date date,
  end_date date,
  participants_count integer,
  max_participants integer,
  visibility text,
  creator_id uuid,
  creator_name text,
  creator_avatar text,
  is_joined boolean,
  is_invited boolean,
  my_progress bigint,
  is_completed boolean,
  created_at timestamptz,
  kind text,
  event_mode text,
  sport_type text,
  event_exercise_id uuid,
  event_date date,
  event_time time,
  event_timezone text,
  event_location text,
  event_geo_lat double precision,
  event_geo_lng double precision,
  event_end_geo_lat double precision,
  event_end_geo_lng double precision,
  event_end_location text,
  event_online_url text,
  movements_count integer,
  my_completed_count integer
)
language sql
stable
set search_path to 'public', 'private', 'pg_temp'
as $function$
  select * from private.list_visible_challenges(p_limit, p_offset)
$function$;

drop function if exists public.list_my_challenges();
drop function if exists private.list_my_challenges();

create function private.list_my_challenges()
returns table(
  id uuid,
  title text,
  description text,
  target_type text,
  target_value bigint,
  start_date date,
  end_date date,
  participants_count integer,
  max_participants integer,
  visibility text,
  creator_id uuid,
  creator_name text,
  creator_avatar text,
  my_progress bigint,
  is_completed boolean,
  joined_at timestamptz,
  kind text,
  event_mode text,
  sport_type text,
  event_exercise_id uuid,
  event_date date,
  event_time time,
  event_timezone text,
  event_location text,
  event_geo_lat double precision,
  event_geo_lng double precision,
  event_end_geo_lat double precision,
  event_end_geo_lng double precision,
  event_end_location text,
  event_online_url text,
  movements_count integer,
  my_completed_count integer
)
language plpgsql
stable security definer
set search_path to 'public'
as $function$
declare
  me uuid := auth.uid();
begin
  return query
  select
    gc.id, gc.title, gc.description, gc.target_type, gc.target_value::bigint,
    gc.start_date, gc.end_date, gc.participants_count, gc.max_participants, gc.visibility, gc.creator_id,
    coalesce(nullif(p.display_name, ''), p.username, 'Anonim'),
    p.avatar_url,
    coalesce((select mp.progress from public._challenge_my_progress(gc.id, me) mp), 0),
    coalesce((select mp.is_completed from public._challenge_my_progress(gc.id, me) mp), false),
    gp.joined_at,
    gc.kind, gc.event_mode, gc.sport_type, gc.event_exercise_id, gc.event_date, gc.event_time,
    gc.event_timezone, gc.event_location, gc.event_geo_lat, gc.event_geo_lng,
    gc.event_end_geo_lat, gc.event_end_geo_lng, gc.event_end_location,
    gc.event_online_url,
    coalesce((select count(*)::int from public.challenge_movements m where m.challenge_id = gc.id), 0),
    coalesce((select count(*)::int
                from public.challenge_movement_completions cmc
               where cmc.challenge_id = gc.id and cmc.user_id = me), 0)
  from public.group_participants gp
  join public.group_challenges gc on gc.id = gp.challenge_id
  left join public.profiles p on p.user_id = gc.creator_id
  where gp.user_id = me
  order by (gp.completed_at is null) desc, gc.end_date desc, gp.joined_at desc;
end;
$function$;

create function public.list_my_challenges()
returns table(
  id uuid,
  title text,
  description text,
  target_type text,
  target_value bigint,
  start_date date,
  end_date date,
  participants_count integer,
  max_participants integer,
  visibility text,
  creator_id uuid,
  creator_name text,
  creator_avatar text,
  my_progress bigint,
  is_completed boolean,
  joined_at timestamptz,
  kind text,
  event_mode text,
  sport_type text,
  event_exercise_id uuid,
  event_date date,
  event_time time,
  event_timezone text,
  event_location text,
  event_geo_lat double precision,
  event_geo_lng double precision,
  event_end_geo_lat double precision,
  event_end_geo_lng double precision,
  event_end_location text,
  event_online_url text,
  movements_count integer,
  my_completed_count integer
)
language sql
stable
set search_path to 'public', 'private', 'pg_temp'
as $function$
  select * from private.list_my_challenges()
$function$;

drop function if exists public.create_event_challenge(text, text, text, text, text, text, text, double precision, double precision, text, text, bigint, text, text, jsonb, double precision, double precision, text);
drop function if exists private.create_event_challenge(text, text, text, text, text, text, text, double precision, double precision, text, text, bigint, text, text, jsonb, double precision, double precision, text);
drop function if exists public.create_event_challenge(text, text, text, text, text, text, text, double precision, double precision, text, text, bigint, text, text, jsonb, double precision, double precision, text, text, uuid);
drop function if exists private.create_event_challenge(text, text, text, text, text, text, text, double precision, double precision, text, text, bigint, text, text, jsonb, double precision, double precision, text, text, uuid);

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
  p_event_exercise_id uuid default null::uuid,
  p_max_participants integer default null::integer
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
  if p_max_participants is not null and p_max_participants < 2 then raise exception 'invalid_max_participants'; end if;

  v_event_date := case when p_event_date is not null and p_event_date <> '' then p_event_date::date else current_date end;
  v_event_time := case when p_event_time is not null and p_event_time <> '' then p_event_time::time else null end;

  v_target_type := coalesce(
    p_target_type,
    case
      when p_event_mode = 'movement_list' then 'movements_completed'
      when v_sport_type in ('football', 'indoor_football', 'volleyball', 'basketball_tennis', 'yoga_pilates', 'boxing', 'jump_rope_hiit') then 'total_duration_minutes'
      else 'total_distance_m'
    end
  );
  v_target_value := coalesce(
    p_target_value::integer,
    case
      when p_event_mode = 'movement_list' then greatest(jsonb_array_length(v_movements), 1)
      when v_sport_type in ('football', 'indoor_football', 'volleyball', 'basketball_tennis', 'yoga_pilates', 'boxing', 'jump_rope_hiit') then 60
      else 1000
    end
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
    participants_count, max_participants
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
    0, p_max_participants
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
  p_event_exercise_id uuid default null::uuid,
  p_max_participants integer default null::integer
)
returns text
language sql
set search_path to 'public', 'private', 'extensions', 'pg_temp'
as $function$
  select private.create_event_challenge(
    $1, $2, $3, $4, $5, $6, $7, $8, $9, $10,
    $11, $12, $13, $14, $15, $16, $17, $18, $19, $20, $21
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
    'max_participants', v_challenge.max_participants,
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
