create table if not exists public.set_completions (
    user_id uuid not null references auth.users(id) on delete cascade,
    exercise_id uuid not null,
    program_day_id uuid not null,
    set_index integer not null check (set_index >= 0),
    date date not null,
    weight_kg real,
    reps_actual integer check (reps_actual is null or reps_actual > 0),
    updated_at timestamptz not null default now(),
    primary key (user_id, exercise_id, program_day_id, set_index, date)
);

create index if not exists set_completions_user_exercise_date_idx
    on public.set_completions (user_id, exercise_id, date desc);

create index if not exists set_completions_user_day_date_idx
    on public.set_completions (user_id, program_day_id, date desc);

alter table public.set_completions enable row level security;

grant select, insert, update, delete on public.set_completions to authenticated;

drop policy if exists "Users can view own set completions" on public.set_completions;
create policy "Users can view own set completions"
    on public.set_completions
    for select
    to authenticated
    using ((select auth.uid()) = user_id);

drop policy if exists "Users can insert own set completions" on public.set_completions;
create policy "Users can insert own set completions"
    on public.set_completions
    for insert
    to authenticated
    with check ((select auth.uid()) = user_id);

drop policy if exists "Users can update own set completions" on public.set_completions;
create policy "Users can update own set completions"
    on public.set_completions
    for update
    to authenticated
    using ((select auth.uid()) = user_id)
    with check ((select auth.uid()) = user_id);

drop policy if exists "Users can delete own set completions" on public.set_completions;
create policy "Users can delete own set completions"
    on public.set_completions
    for delete
    to authenticated
    using ((select auth.uid()) = user_id);
