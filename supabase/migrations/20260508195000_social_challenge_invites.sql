create table if not exists public.challenge_invites (
  id uuid primary key default gen_random_uuid(),
  challenge_id uuid not null references public.group_challenges(id) on delete cascade,
  inviter_id uuid not null references auth.users(id) on delete cascade,
  invitee_id uuid not null references auth.users(id) on delete cascade,
  status text not null default 'pending' check (status in ('pending', 'accepted', 'dismissed')),
  created_at timestamptz not null default now(),
  unique (challenge_id, invitee_id)
);

alter table public.challenge_invites enable row level security;

drop policy if exists "challenge_invites_select_related" on public.challenge_invites;
create policy "challenge_invites_select_related"
on public.challenge_invites
for select
to authenticated
using (
  inviter_id = auth.uid()
  or invitee_id = auth.uid()
  or exists (
    select 1
    from public.group_challenges gc
    where gc.id = challenge_invites.challenge_id
      and gc.creator_id = auth.uid()
  )
);

drop policy if exists "challenge_invites_insert_owner" on public.challenge_invites;
create policy "challenge_invites_insert_owner"
on public.challenge_invites
for insert
to authenticated
with check (
  inviter_id = auth.uid()
  and exists (
    select 1
    from public.group_challenges gc
    where gc.id = challenge_invites.challenge_id
      and gc.creator_id = auth.uid()
  )
);

create index if not exists challenge_invites_invitee_idx
on public.challenge_invites (invitee_id, status, created_at desc);

create index if not exists challenge_invites_challenge_idx
on public.challenge_invites (challenge_id);

create or replace function private.invite_friends_to_challenge(
  p_challenge_id uuid,
  p_user_ids uuid[]
)
returns integer
language plpgsql
security definer
set search_path to 'public', 'pg_temp'
as $function$
declare
  v_uid uuid := auth.uid();
  v_count integer := 0;
begin
  if v_uid is null then
    raise exception 'unauthenticated';
  end if;

  if not exists (
    select 1 from public.group_challenges
    where id = p_challenge_id and creator_id = v_uid
  ) then
    raise exception 'challenge_not_owned';
  end if;

  with targets as (
    select distinct unnest(coalesce(p_user_ids, '{}'::uuid[])) as user_id
  ),
  mutuals as (
    select t.user_id
    from targets t
    where t.user_id <> v_uid
      and exists (
        select 1 from public.user_follows f
        where f.follower_id = v_uid and f.following_id = t.user_id
      )
      and exists (
        select 1 from public.user_follows f
        where f.follower_id = t.user_id and f.following_id = v_uid
      )
  ),
  inserted as (
    insert into public.challenge_invites (challenge_id, inviter_id, invitee_id, status, created_at)
    select p_challenge_id, v_uid, user_id, 'pending', now()
    from mutuals
    on conflict (challenge_id, invitee_id)
    do update set
      inviter_id = excluded.inviter_id,
      status = 'pending',
      created_at = now()
    returning 1
  )
  select count(*)::integer into v_count from inserted;

  return v_count;
end;
$function$;

create or replace function public.invite_friends_to_challenge(
  p_challenge_id uuid,
  p_user_ids uuid[]
)
returns integer
language sql
set search_path to 'public'
as $function$
  select private.invite_friends_to_challenge(p_challenge_id, p_user_ids);
$function$;

create or replace function private.list_visible_challenges(
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

create or replace function public.list_visible_challenges(
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
begin
  if me is null then raise exception 'unauthenticated'; end if;
  select * into v_chal from public.group_challenges where id = p_challenge_id;
  if v_chal is null then raise exception 'challenge_not_found'; end if;
  if v_chal.end_date < current_date then raise exception 'challenge_ended'; end if;

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

create or replace function private.list_user_created_challenges(
  p_user_id uuid,
  p_limit integer default 12
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
  v_mutual boolean := false;
begin
  v_mutual := exists(select 1 from public.user_follows f where f.follower_id = me and f.following_id = p_user_id)
          and exists(select 1 from public.user_follows f where f.follower_id = p_user_id and f.following_id = me);

  return query
  select
    gc.id, gc.title, gc.description, gc.target_type, gc.target_value::bigint,
    gc.start_date, gc.end_date, gc.participants_count, gc.visibility, gc.creator_id,
    coalesce(nullif(p.display_name, ''), p.username, 'Anonim'),
    p.avatar_url,
    (gp.user_id is not null),
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
  where gc.creator_id = p_user_id
    and (gc.visibility = 'public' or v_mutual)
  order by gc.created_at desc
  limit greatest(1, least(30, p_limit));
end;
$function$;

create or replace function public.list_user_created_challenges(
  p_user_id uuid,
  p_limit integer default 12
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
  select * from private.list_user_created_challenges(p_user_id, p_limit);
$function$;

create or replace function private.list_user_shared_programs(
  p_user_id uuid,
  p_limit integer default 12
)
returns table(
  id uuid,
  creator_id uuid,
  creator_display_name text,
  creator_avatar_url text,
  title text,
  description text,
  program_data jsonb,
  tags text[],
  difficulty text,
  duration_weeks integer,
  days_per_week integer,
  likes_count integer,
  saves_count integer,
  downloads_count integer,
  is_liked_by_me boolean,
  is_saved_by_me boolean,
  is_applied_by_me boolean,
  content_hash text,
  created_at timestamptz
)
language plpgsql
stable security definer
set search_path to 'public'
as $function$
declare
  v_uid uuid := auth.uid();
begin
  return query
  select
    sp.id,
    sp.creator_id,
    coalesce(nullif(p.display_name, ''), p.username, 'Anonim'),
    p.avatar_url,
    sp.title,
    sp.description,
    sp.program_data,
    coalesce(sp.tags, '{}'),
    sp.difficulty,
    sp.duration_weeks,
    sp.days_per_week,
    sp.likes_count,
    coalesce(sp.saves_count, 0),
    sp.downloads_count,
    exists(select 1 from public.program_reactions r where r.shared_program_id = sp.id and r.user_id = v_uid and r.type = 'like'),
    exists(select 1 from public.shared_program_saves s where s.shared_program_id = sp.id and s.user_id = v_uid),
    exists(select 1 from public.programs pr where pr.user_id = v_uid and pr.content_hash is not null and pr.content_hash = sp.content_hash),
    sp.content_hash,
    sp.created_at
  from public.shared_programs sp
  left join public.profiles p on p.user_id = sp.creator_id
  where sp.creator_id = p_user_id
    and sp.visibility = 'public'
  order by sp.created_at desc
  limit greatest(1, least(30, p_limit));
end;
$function$;

create or replace function public.list_user_shared_programs(
  p_user_id uuid,
  p_limit integer default 12
)
returns table(
  id uuid,
  creator_id uuid,
  creator_display_name text,
  creator_avatar_url text,
  title text,
  description text,
  program_data jsonb,
  tags text[],
  difficulty text,
  duration_weeks integer,
  days_per_week integer,
  likes_count integer,
  saves_count integer,
  downloads_count integer,
  is_liked_by_me boolean,
  is_saved_by_me boolean,
  is_applied_by_me boolean,
  content_hash text,
  created_at timestamptz
)
language sql
stable
set search_path to 'public'
as $function$
  select * from private.list_user_shared_programs(p_user_id, p_limit);
$function$;
