create index if not exists challenge_invites_inviter_idx
on public.challenge_invites (inviter_id);

drop policy if exists "challenge_invites_select_related" on public.challenge_invites;
create policy "challenge_invites_select_related"
on public.challenge_invites
for select
to authenticated
using (
  inviter_id = (select auth.uid())
  or invitee_id = (select auth.uid())
  or exists (
    select 1
    from public.group_challenges gc
    where gc.id = challenge_invites.challenge_id
      and gc.creator_id = (select auth.uid())
  )
);

drop policy if exists "challenge_invites_insert_owner" on public.challenge_invites;
create policy "challenge_invites_insert_owner"
on public.challenge_invites
for insert
to authenticated
with check (
  inviter_id = (select auth.uid())
  and exists (
    select 1
    from public.group_challenges gc
    where gc.id = challenge_invites.challenge_id
      and gc.creator_id = (select auth.uid())
  )
);
