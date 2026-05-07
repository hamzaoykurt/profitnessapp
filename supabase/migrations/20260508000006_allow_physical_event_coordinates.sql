-- A physical event created from Places autocomplete is valid when it has
-- either a display location or resolved coordinates.
alter table public.group_challenges
  drop constraint if exists group_challenges_event_mode_fields_chk;

alter table public.group_challenges
  add constraint group_challenges_event_mode_fields_chk check (
    kind <> 'event'
    or (event_mode = 'online' and nullif(trim(event_online_url), '') is not null)
    or (
      event_mode = 'physical'
      and (
        nullif(trim(event_location), '') is not null
        or (event_geo_lat is not null and event_geo_lng is not null)
      )
    )
    or event_mode = 'movement_list'
  );
