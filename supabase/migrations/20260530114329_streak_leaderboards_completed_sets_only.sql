-- Count only actual completed set rows in streak leaderboards.
-- Draft set rows can have weight/duration but no reps_actual, so they should not
-- create an extra streak day.

create or replace function private.get_leaderboard_streak(p_limit integer default 100)
returns table(
  user_id uuid,
  display_name text,
  avatar_url text,
  current_streak integer,
  rank_position bigint
)
language sql
stable security definer
set search_path to 'public'
as $function$
  with dates as (
    select distinct wl.user_id, wl.date::date as day
    from public.workout_logs wl
    where wl.user_id is not null
      and wl.date is not null
    union
    select distinct sc.user_id, sc.date::date as day
    from public.set_completions sc
    where sc.user_id is not null
      and sc.date is not null
      and sc.reps_actual is not null
  ),
  ordered as (
    select
      d.user_id,
      d.day,
      lag(d.day) over (partition by d.user_id order by d.day desc) as newer_day,
      max(d.day) over (partition by d.user_id) as latest_day
    from dates d
  ),
  segmented as (
    select
      o.user_id,
      o.latest_day,
      sum(
        case when o.newer_day is not null and (o.newer_day - o.day) >= 7 then 1 else 0 end
      ) over (
        partition by o.user_id
        order by o.day desc
        rows between unbounded preceding and current row
      ) as break_count
    from ordered o
  ),
  streaks as (
    select
      s.user_id,
      case
        when (((now() at time zone 'Europe/Istanbul')::date - max(s.latest_day)) < 7)
          then count(*) filter (where s.break_count = 0)::integer
        else 0
      end as current_streak
    from segmented s
    group by s.user_id
  ),
  ranked as (
    select
      st.user_id,
      st.current_streak,
      row_number() over (
        order by st.current_streak desc, st.user_id
      ) as pos
    from streaks st
    where st.current_streak > 0
  )
  select
    r.user_id,
    coalesce(p.display_name, 'Anonim') as display_name,
    p.avatar_url,
    r.current_streak,
    r.pos as rank_position
  from ranked r
  left join public.profiles p on p.user_id = r.user_id
  order by r.pos
  limit greatest(1, least(100, p_limit));
$function$;

create or replace function private.get_my_rank_streak()
returns table(
  current_streak integer,
  rank_position bigint,
  total_users bigint
)
language sql
stable security definer
set search_path to 'public'
as $function$
  with dates as (
    select distinct wl.user_id, wl.date::date as day
    from public.workout_logs wl
    where wl.user_id is not null
      and wl.date is not null
    union
    select distinct sc.user_id, sc.date::date as day
    from public.set_completions sc
    where sc.user_id is not null
      and sc.date is not null
      and sc.reps_actual is not null
  ),
  ordered as (
    select
      d.user_id,
      d.day,
      lag(d.day) over (partition by d.user_id order by d.day desc) as newer_day,
      max(d.day) over (partition by d.user_id) as latest_day
    from dates d
  ),
  segmented as (
    select
      o.user_id,
      o.latest_day,
      sum(
        case when o.newer_day is not null and (o.newer_day - o.day) >= 7 then 1 else 0 end
      ) over (
        partition by o.user_id
        order by o.day desc
        rows between unbounded preceding and current row
      ) as break_count
    from ordered o
  ),
  streaks as (
    select
      s.user_id,
      case
        when (((now() at time zone 'Europe/Istanbul')::date - max(s.latest_day)) < 7)
          then count(*) filter (where s.break_count = 0)::integer
        else 0
      end as current_streak
    from segmented s
    group by s.user_id
  ),
  ranked as (
    select
      st.user_id,
      st.current_streak,
      row_number() over (
        order by st.current_streak desc, st.user_id
      ) as pos
    from streaks st
    where st.current_streak > 0
  )
  select
    coalesce(r.current_streak, 0) as current_streak,
    coalesce(r.pos, 0) as rank_position,
    (select count(*) from ranked) as total_users
  from (select auth.uid() as uid) a
  left join ranked r on r.user_id = a.uid;
$function$;

create or replace function private.get_friend_leaderboard_streak(p_limit integer default 100)
returns table(
  user_id uuid,
  username text,
  display_name text,
  avatar_url text,
  current_streak integer,
  rank_position integer,
  is_me boolean
)
language sql
stable security definer
set search_path to 'public'
as $function$
  with me as (select auth.uid() as uid),
  friend_ids as (
    select f1.following_id as fid
    from public.user_follows f1
    where f1.follower_id = (select uid from me)
      and exists (
        select 1
        from public.user_follows f2
        where f2.follower_id = f1.following_id
          and f2.following_id = (select uid from me)
      )
    union
    select (select uid from me)
  ),
  dates as (
    select distinct wl.user_id, wl.date::date as day
    from public.workout_logs wl
    where wl.user_id in (select fid from friend_ids where fid is not null)
      and wl.date is not null
    union
    select distinct sc.user_id, sc.date::date as day
    from public.set_completions sc
    where sc.user_id in (select fid from friend_ids where fid is not null)
      and sc.date is not null
      and sc.reps_actual is not null
  ),
  ordered as (
    select
      d.user_id,
      d.day,
      lag(d.day) over (partition by d.user_id order by d.day desc) as newer_day,
      max(d.day) over (partition by d.user_id) as latest_day
    from dates d
  ),
  segmented as (
    select
      o.user_id,
      o.latest_day,
      sum(
        case when o.newer_day is not null and (o.newer_day - o.day) >= 7 then 1 else 0 end
      ) over (
        partition by o.user_id
        order by o.day desc
        rows between unbounded preceding and current row
      ) as break_count
    from ordered o
  ),
  streaks as (
    select
      s.user_id,
      case
        when (((now() at time zone 'Europe/Istanbul')::date - max(s.latest_day)) < 7)
          then count(*) filter (where s.break_count = 0)::integer
        else 0
      end as current_streak
    from segmented s
    group by s.user_id
  ),
  ranked as (
    select
      p.user_id,
      p.username,
      p.display_name,
      p.avatar_url,
      st.current_streak,
      row_number() over (
        order by st.current_streak desc, p.created_at asc, p.user_id
      )::integer as rank_position
    from public.profiles p
    join streaks st on st.user_id = p.user_id
    where st.current_streak > 0
  )
  select
    r.user_id,
    r.username,
    r.display_name,
    r.avatar_url,
    r.current_streak,
    r.rank_position,
    (r.user_id = (select uid from me)) as is_me
  from ranked r
  order by r.rank_position
  limit greatest(1, least(100, p_limit));
$function$;

grant execute on function public.get_leaderboard_streak(integer) to authenticated;
grant execute on function public.get_my_rank_streak() to authenticated;
grant execute on function public.get_friend_leaderboard_streak(integer) to authenticated;
