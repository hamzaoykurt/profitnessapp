create or replace function private.can_read_challenge(p_challenge_id uuid, p_user_id uuid)
returns boolean
language sql
stable security definer
set search_path to 'public', 'pg_temp'
as $function$
  select p_user_id is not null and exists (
    select 1
    from public.group_challenges c
    where c.id = p_challenge_id
      and (
        c.visibility = 'public'
        or c.creator_id = p_user_id
        or exists (
          select 1
          from public.group_participants gp
          where gp.challenge_id = c.id
            and gp.user_id = p_user_id
        )
        or exists (
          select 1
          from public.challenge_invites ci
          where ci.challenge_id = c.id
            and ci.invitee_id = p_user_id
            and ci.status = 'pending'
        )
      )
  );
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
    gc.start_date, gc.end_date, gc.participants_count, gc.visibility, gc.creator_id,
    coalesce(nullif(p.display_name, ''), p.username, 'Anonim'),
    p.avatar_url,
    (gp.user_id is not null),
    exists (
      select 1
      from public.challenge_invites ci
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
set search_path to 'public'
as $function$
  select * from private.list_visible_challenges(p_limit, p_offset);
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
  v_is_invited boolean := false;
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

  select exists(
    select 1
    from public.challenge_invites ci
    where ci.challenge_id = p_challenge_id
      and ci.invitee_id = me
      and ci.status = 'pending'
  ) into v_is_invited;

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
    'is_invited', v_is_invited,
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
