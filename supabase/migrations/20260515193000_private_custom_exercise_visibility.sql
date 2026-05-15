-- Keep custom exercises private to the user who created them.

alter table public.exercises
  add column if not exists created_by uuid;

create index if not exists exercises_created_by_idx
  on public.exercises (created_by);

alter table public.exercises enable row level security;

grant select, insert on public.exercises to authenticated;

drop policy if exists "Exercises readable by authenticated" on public.exercises;
create policy "Exercises readable by authenticated"
  on public.exercises
  for select
  to authenticated
  using (created_by is null or created_by = auth.uid());

drop policy if exists "Users insert own custom exercises" on public.exercises;
create policy "Users insert own custom exercises"
  on public.exercises
  for insert
  to authenticated
  with check (
    created_by = auth.uid()
    and length(trim(name)) between 2 and 120
    and length(trim(name_en)) <= 120
    and length(trim(target_muscle)) between 2 and 80
    and length(trim(category)) between 2 and 80
    and sets_default between 1 and 20
    and reps_default between 1 and 200
  );
