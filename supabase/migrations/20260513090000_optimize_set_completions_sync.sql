-- Keep set completion refreshes delta-friendly and avoid per-row auth.uid()
-- evaluation in RLS policies.

create index if not exists set_completions_user_updated_at_idx
  on public.set_completions (user_id, updated_at);

create or replace function public.set_set_completions_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

revoke execute on function public.set_set_completions_updated_at() from public;
revoke execute on function public.set_set_completions_updated_at() from anon;
revoke execute on function public.set_set_completions_updated_at() from authenticated;

drop trigger if exists set_completions_set_updated_at on public.set_completions;
create trigger set_completions_set_updated_at
  before update on public.set_completions
  for each row
  execute function public.set_set_completions_updated_at();

drop policy if exists "Users can view own set completions" on public.set_completions;
drop policy if exists "Users can insert own set completions" on public.set_completions;
drop policy if exists "Users can update own set completions" on public.set_completions;
drop policy if exists "Users can delete own set completions" on public.set_completions;
drop policy if exists "set_completions_select_own" on public.set_completions;
drop policy if exists "set_completions_insert_own" on public.set_completions;
drop policy if exists "set_completions_update_own" on public.set_completions;
drop policy if exists "set_completions_delete_own" on public.set_completions;

create policy "set_completions_select_own"
  on public.set_completions
  for select
  to authenticated
  using ((select auth.uid()) = user_id);

create policy "set_completions_insert_own"
  on public.set_completions
  for insert
  to authenticated
  with check ((select auth.uid()) = user_id);

create policy "set_completions_update_own"
  on public.set_completions
  for update
  to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

create policy "set_completions_delete_own"
  on public.set_completions
  for delete
  to authenticated
  using ((select auth.uid()) = user_id);
