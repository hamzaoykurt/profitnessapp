-- Prevent one creator from publishing the same program more than once while it is live.
-- Other creators can still publish matching program content because creator_id is part of
-- both uniqueness rules.

with ranked_by_source as (
  select
    id,
    row_number() over (
      partition by creator_id, original_program_id
      order by created_at asc nulls last, id asc
    ) as rn
  from public.shared_programs
  where original_program_id is not null
    and visibility = 'public'
    and deleted_at is null
)
update public.shared_programs sp
set visibility = 'unpublished',
    deleted_at = coalesce(sp.deleted_at, now()),
    updated_at = now()
from ranked_by_source r
where sp.id = r.id
  and r.rn > 1;

with ranked_by_content as (
  select
    id,
    row_number() over (
      partition by creator_id, content_hash
      order by created_at asc nulls last, id asc
    ) as rn
  from public.shared_programs
  where content_hash is not null
    and content_hash <> ''
    and visibility = 'public'
    and deleted_at is null
)
update public.shared_programs sp
set visibility = 'unpublished',
    deleted_at = coalesce(sp.deleted_at, now()),
    updated_at = now()
from ranked_by_content r
where sp.id = r.id
  and r.rn > 1;

create unique index if not exists shared_programs_one_live_per_owner_source_idx
  on public.shared_programs (creator_id, original_program_id)
  where original_program_id is not null
    and visibility = 'public'
    and deleted_at is null;

create unique index if not exists shared_programs_one_live_per_owner_content_idx
  on public.shared_programs (creator_id, content_hash)
  where content_hash is not null
    and content_hash <> ''
    and visibility = 'public'
    and deleted_at is null;

comment on index public.shared_programs_one_live_per_owner_source_idx is
  'Allows only one live public share per creator and source program.';

comment on index public.shared_programs_one_live_per_owner_content_idx is
  'Allows only one live public share per creator and canonical program content hash.';
