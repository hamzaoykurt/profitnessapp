-- Global and friend leaderboards for longest workout streak.

create index if not exists user_stats_longest_streak_idx
  on public.user_stats (longest_streak desc, current_streak desc, user_id);

create or replace function private.get_leaderboard_streak(p_limit integer default 100)
returns table(
  user_id uuid,
  display_name text,
  avatar_url text,
  longest_streak integer,
  rank_position bigint
)
language sql
stable security definer
set search_path to 'public'
as $function$
  with ranked as (
    select
      us.user_id,
      coalesce(us.longest_streak, 0) as longest_streak,
      row_number() over (
        order by coalesce(us.longest_streak, 0) desc, coalesce(us.current_streak, 0) desc, us.user_id
      ) as pos
    from public.user_stats us
  )
  select
    r.user_id,
    coalesce(p.display_name, 'Anonim') as display_name,
    p.avatar_url,
    r.longest_streak,
    r.pos as rank_position
  from ranked r
  left join public.profiles p on p.user_id = r.user_id
  order by r.pos
  limit greatest(1, least(100, p_limit));
$function$;

create or replace function public.get_leaderboard_streak(p_limit integer default 100)
returns table(
  user_id uuid,
  display_name text,
  avatar_url text,
  longest_streak integer,
  rank_position bigint
)
language sql
stable
set search_path to 'public', 'private', 'pg_temp'
as $function$
  select * from private.get_leaderboard_streak($1)
$function$;

create or replace function private.get_my_rank_streak()
returns table(
  longest_streak integer,
  rank_position bigint,
  total_users bigint
)
language sql
stable security definer
set search_path to 'public'
as $function$
  with ranked as (
    select
      user_id,
      coalesce(longest_streak, 0) as longest_streak,
      row_number() over (
        order by coalesce(longest_streak, 0) desc, coalesce(current_streak, 0) desc, user_id
      ) as pos
    from public.user_stats
  )
  select
    coalesce(r.longest_streak, 0) as longest_streak,
    coalesce(r.pos, 0) as rank_position,
    (select count(*) from public.user_stats) as total_users
  from (select auth.uid() as uid) a
  left join ranked r on r.user_id = a.uid;
$function$;

create or replace function public.get_my_rank_streak()
returns table(
  longest_streak integer,
  rank_position bigint,
  total_users bigint
)
language sql
stable
set search_path to 'public', 'private', 'pg_temp'
as $function$
  select * from private.get_my_rank_streak()
$function$;

create or replace function private.get_friend_leaderboard_streak(p_limit integer default 100)
returns table(
  user_id uuid,
  username text,
  display_name text,
  avatar_url text,
  longest_streak integer,
  rank_position integer,
  is_me boolean
)
language sql
security definer
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
  ranked as (
    select
      p.user_id,
      p.username,
      p.display_name,
      p.avatar_url,
      coalesce(us.longest_streak, 0) as longest_streak,
      row_number() over (
        order by coalesce(us.longest_streak, 0) desc, coalesce(us.current_streak, 0) desc, p.created_at asc
      )::int as rank_position
    from public.profiles p
    left join public.user_stats us on us.user_id = p.user_id
    where p.user_id in (select fid from friend_ids where fid is not null)
  )
  select
    r.user_id,
    r.username,
    r.display_name,
    r.avatar_url,
    r.longest_streak,
    r.rank_position,
    (r.user_id = (select uid from me)) as is_me
  from ranked r
  order by r.rank_position
  limit greatest(1, least(100, p_limit));
$function$;

create or replace function public.get_friend_leaderboard_streak(p_limit integer default 100)
returns table(
  user_id uuid,
  username text,
  display_name text,
  avatar_url text,
  longest_streak integer,
  rank_position integer,
  is_me boolean
)
language sql
set search_path to 'public', 'private', 'pg_temp'
as $function$
  select * from private.get_friend_leaderboard_streak($1)
$function$;

grant execute on function public.get_leaderboard_streak(integer) to authenticated;
grant execute on function public.get_my_rank_streak() to authenticated;
grant execute on function public.get_friend_leaderboard_streak(integer) to authenticated;
