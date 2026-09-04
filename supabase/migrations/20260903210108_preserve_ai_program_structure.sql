alter table public.program_days
  add column if not exists notes text not null default '';

alter table public.program_exercises
  add column if not exists section text not null default '',
  add column if not exists notes text not null default '',
  add column if not exists group_id text,
  add column if not exists group_type text not null default 'straight',
  add column if not exists group_label text not null default '',
  add column if not exists group_rounds integer,
  add column if not exists group_rest_seconds integer;

do $$
begin
  if not exists (
    select 1 from pg_constraint where conname = 'program_exercises_group_type_check'
  ) then
    alter table public.program_exercises
      add constraint program_exercises_group_type_check
      check (group_type in ('straight', 'superset', 'giant_set', 'circuit'));
  end if;

  if not exists (
    select 1 from pg_constraint where conname = 'program_exercises_group_rounds_check'
  ) then
    alter table public.program_exercises
      add constraint program_exercises_group_rounds_check
      check (group_rounds is null or group_rounds between 1 and 20);
  end if;

  if not exists (
    select 1 from pg_constraint where conname = 'program_exercises_group_rest_seconds_check'
  ) then
    alter table public.program_exercises
      add constraint program_exercises_group_rest_seconds_check
      check (group_rest_seconds is null or group_rest_seconds between 0 and 3600);
  end if;
end
$$;

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
  if v_user is null then raise exception 'unauthorized'; end if;

  select program_data, title into v_data, v_name
  from public.shared_programs sp
  where sp.id = p_shared_id
    and (
      (sp.visibility = 'public' and sp.deleted_at is null)
      or sp.creator_id = v_user
      or exists (
        select 1 from public.shared_program_saves s
        where s.shared_program_id = sp.id and s.user_id = v_user
      )
    );

  if v_data is null then raise exception 'shared program not found or has no snapshot'; end if;

  update public.programs set is_active = false
  where user_id = v_user and is_active = true;

  insert into public.programs(user_id, name, type, is_active, applied_from_shared_id)
  values (v_user, v_name, 'manual', true, p_shared_id)
  returning id into v_new_program_id;

  for v_day in select * from jsonb_array_elements(v_data->'days') loop
    insert into public.program_days(program_id, day_index, title, is_rest_day, notes)
    values (
      v_new_program_id,
      (v_day->>'day_index')::int,
      v_day->>'title',
      coalesce((v_day->>'is_rest_day')::boolean, false),
      coalesce(v_day->>'notes', '')
    )
    returning id into v_day_id;

    for v_ex in select * from jsonb_array_elements(v_day->'exercises') loop
      if v_ex->>'exercise_id' is not null
         and exists (select 1 from public.exercises where id = (v_ex->>'exercise_id')::uuid) then
        insert into public.program_exercises(
          program_day_id, exercise_id, sets, reps, weight_kg, rest_seconds, order_index,
          section, notes, group_id, group_type, group_label, group_rounds, group_rest_seconds
        )
        values (
          v_day_id,
          (v_ex->>'exercise_id')::uuid,
          coalesce((v_ex->>'sets')::int, 3),
          coalesce((v_ex->>'reps')::int, 12),
          nullif(v_ex->>'weight_kg', '')::numeric,
          coalesce((v_ex->>'rest_seconds')::int, 90),
          coalesce((v_ex->>'order_index')::int, 0),
          coalesce(v_ex->>'section', ''),
          coalesce(v_ex->>'notes', ''),
          nullif(v_ex->>'group_id', ''),
          coalesce(nullif(v_ex->>'group_type', ''), 'straight'),
          coalesce(v_ex->>'group_label', ''),
          nullif(v_ex->>'group_rounds', '')::int,
          nullif(v_ex->>'group_rest_seconds', '')::int
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
