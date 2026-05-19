-- Preserve saved shared programs after the owner removes them from public discovery.
-- "Delete" now means unpublish: the shared snapshot remains available to users who
-- already saved it, while disappearing from community feeds and the owner's list.

alter table public.shared_programs
  add column if not exists deleted_at timestamptz;

comment on column public.shared_programs.deleted_at is
  'Set when a creator removes the share from public discovery. Saved users may still access the snapshot.';

create or replace function private.get_discover_feed(
  p_sort text default 'newest'::text,
  p_limit integer default 20,
  p_offset integer default 0
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
    p.display_name,
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
    exists(
      select 1
      from public.program_reactions r
      where r.shared_program_id = sp.id
        and r.user_id = v_uid
        and r.type = 'like'
    ),
    exists(
      select 1
      from public.shared_program_saves s
      where s.shared_program_id = sp.id
        and s.user_id = v_uid
    ),
    exists(
      select 1
      from public.programs pr
      where pr.user_id = v_uid
        and pr.content_hash is not null
        and pr.content_hash = sp.content_hash
    ),
    sp.content_hash,
    sp.created_at
  from public.shared_programs sp
  left join public.profiles p on p.user_id = sp.creator_id
  where sp.visibility = 'public'
    and sp.deleted_at is null
  order by
    case when p_sort = 'trending' then sp.likes_count end desc nulls last,
    sp.created_at desc
  limit greatest(1, least(50, p_limit))
  offset greatest(0, p_offset);
end;
$function$;

create or replace function private.list_my_saved_programs(
  p_sort text default 'newest'::text,
  p_limit integer default 20,
  p_offset integer default 0
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
  if v_uid is null then
    raise exception 'unauthorized';
  end if;

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
    exists(
      select 1
      from public.program_reactions r
      where r.shared_program_id = sp.id
        and r.user_id = v_uid
        and r.type = 'like'
    ),
    true,
    exists(
      select 1
      from public.programs pr
      where pr.user_id = v_uid
        and pr.content_hash is not null
        and pr.content_hash = sp.content_hash
    ),
    sp.content_hash,
    sp.created_at
  from public.shared_program_saves s
  join public.shared_programs sp on sp.id = s.shared_program_id
  left join public.profiles p on p.user_id = sp.creator_id
  where s.user_id = v_uid
  order by
    case when p_sort = 'trending' then sp.downloads_count end desc nulls last,
    case when p_sort = 'trending' then sp.likes_count + coalesce(sp.saves_count, 0) end desc nulls last,
    s.created_at desc,
    sp.created_at desc
  limit greatest(1, least(50, p_limit))
  offset greatest(0, p_offset);
end;
$function$;

create or replace function public.list_my_saved_programs(
  p_sort text default 'newest'::text,
  p_limit integer default 20,
  p_offset integer default 0
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
set search_path to 'public', 'private', 'pg_temp'
as $function$
  select * from private.list_my_saved_programs($1, $2, $3)
$function$;

create or replace function private.apply_shared_program(p_shared_id uuid)
returns uuid
language plpgsql
security definer
set search_path to 'public', 'extensions'
as $function$
declare
  v_user uuid := auth.uid();
  v_new_program_id uuid;
  v_data jsonb;
  v_name text;
  v_day jsonb;
  v_day_id uuid;
  v_ex jsonb;
begin
  if v_user is null then
    raise exception 'unauthorized';
  end if;

  select program_data, title
    into v_data, v_name
  from public.shared_programs sp
  where sp.id = p_shared_id
    and (
      (sp.visibility = 'public' and sp.deleted_at is null)
      or sp.creator_id = v_user
      or exists (
        select 1
        from public.shared_program_saves s
        where s.shared_program_id = sp.id
          and s.user_id = v_user
      )
    );

  if v_data is null then
    raise exception 'shared program not found or has no snapshot';
  end if;

  update public.programs
  set is_active = false
  where user_id = v_user
    and is_active = true;

  insert into public.programs(user_id, name, type, is_active, applied_from_shared_id)
  values (v_user, v_name, 'manual', true, p_shared_id)
  returning id into v_new_program_id;

  for v_day in select * from jsonb_array_elements(v_data->'days')
  loop
    insert into public.program_days(program_id, day_index, title, is_rest_day)
    values (
      v_new_program_id,
      (v_day->>'day_index')::int,
      v_day->>'title',
      coalesce((v_day->>'is_rest_day')::boolean, false)
    )
    returning id into v_day_id;

    for v_ex in select * from jsonb_array_elements(v_day->'exercises')
    loop
      if (v_ex->>'exercise_id') is not null
         and exists (select 1 from public.exercises where id = (v_ex->>'exercise_id')::uuid) then
        insert into public.program_exercises(
          program_day_id,
          exercise_id,
          sets,
          reps,
          weight_kg,
          rest_seconds,
          order_index
        )
        values (
          v_day_id,
          (v_ex->>'exercise_id')::uuid,
          coalesce((v_ex->>'sets')::int, 3),
          coalesce((v_ex->>'reps')::int, 12),
          nullif(v_ex->>'weight_kg', '')::numeric,
          coalesce((v_ex->>'rest_seconds')::int, 90),
          coalesce((v_ex->>'order_index')::int, 0)
        );
      end if;
    end loop;
  end loop;

  update public.shared_programs
  set downloads_count = downloads_count + 1
  where id = p_shared_id;

  return v_new_program_id;
end;
$function$;

create or replace function private.delete_shared_program(p_shared_id uuid)
returns boolean
language plpgsql
security definer
set search_path to 'public'
as $function$
declare
  v_user uuid := auth.uid();
  v_creator uuid;
begin
  if v_user is null then
    raise exception 'unauthorized';
  end if;

  select creator_id
    into v_creator
  from public.shared_programs
  where id = p_shared_id;

  if v_creator is null then
    return false;
  end if;

  if v_creator <> v_user then
    raise exception 'not owner';
  end if;

  update public.shared_programs
  set visibility = 'unpublished',
      deleted_at = coalesce(deleted_at, now()),
      updated_at = now()
  where id = p_shared_id;

  return true;
end;
$function$;

create or replace function private.list_my_shared_programs()
returns table(
  id uuid,
  original_program_id uuid,
  title text,
  description text,
  tags text[],
  difficulty text,
  duration_weeks integer,
  days_per_week integer,
  likes_count integer,
  saves_count integer,
  downloads_count integer,
  created_at timestamptz,
  updated_at timestamptz,
  source_exists boolean,
  source_program_name text,
  is_out_of_sync boolean,
  shared_content_hash text,
  source_content_hash text
)
language plpgsql
stable security definer
set search_path to 'public'
as $function$
declare
  v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception 'unauthorized';
  end if;

  return query
  select
    sp.id,
    sp.original_program_id,
    sp.title,
    sp.description,
    coalesce(sp.tags, '{}'),
    sp.difficulty,
    sp.duration_weeks,
    sp.days_per_week,
    sp.likes_count,
    coalesce(sp.saves_count, 0),
    sp.downloads_count,
    sp.created_at,
    sp.updated_at,
    (pr.id is not null) as source_exists,
    pr.name as source_program_name,
    case
      when pr.id is null then false
      else (pr.content_hash is distinct from sp.content_hash)
    end as is_out_of_sync,
    sp.content_hash as shared_content_hash,
    pr.content_hash as source_content_hash
  from public.shared_programs sp
  left join public.programs pr
    on pr.id = sp.original_program_id
   and pr.user_id = v_user
  where sp.creator_id = v_user
    and sp.visibility = 'public'
    and sp.deleted_at is null
  order by sp.updated_at desc nulls last, sp.created_at desc;
end;
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
    and sp.deleted_at is null
  order by sp.created_at desc
  limit greatest(1, least(30, p_limit));
end;
$function$;

revoke execute on function public.list_my_saved_programs(text, integer, integer) from public;
grant execute on function public.list_my_saved_programs(text, integer, integer) to authenticated;
