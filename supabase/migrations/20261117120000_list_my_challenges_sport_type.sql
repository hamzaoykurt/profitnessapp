-- Katıldıklarım (list_my_challenges) etkinlik alanlarını döndürsün: sport_type, event_exercise_id, event_end_geo_lat/lng, event_end_location.
-- Canlı DB'de liste (Katıldıklarım) ile detay/Keşfet arasında etkinlik türü tutarsızlığı vardı (spor türü dönmüyordu).
-- Canlı tanım (2026-11-17) temel alındı: max_participants canlı tabloda YOK, eklenmedi. Yetkiler canlıyla aynı korunur
-- (postgres, service_role, authenticated; public/anon yok).

drop function if exists public.list_my_challenges();
drop function if exists private.list_my_challenges();

create function private.list_my_challenges()
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
  my_progress bigint,
  is_completed boolean,
  joined_at timestamptz,
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
declare me uuid := auth.uid();
begin
  return query
  select
    gc.id, gc.title, gc.description, gc.target_type, gc.target_value::bigint,
    gc.start_date, gc.end_date, gc.participants_count, gc.visibility, gc.creator_id,
    coalesce(nullif(p.display_name, ''), p.username, 'Anonim'),
    p.avatar_url,
    coalesce((select mp.progress from public._challenge_my_progress(gc.id, me) mp), 0),
    coalesce((select mp.is_completed from public._challenge_my_progress(gc.id, me) mp), false),
    gp.joined_at,
    gc.kind, gc.event_mode, gc.sport_type, gc.event_exercise_id, gc.event_date, gc.event_time,
    gc.event_timezone, gc.event_location, gc.event_geo_lat, gc.event_geo_lng,
    gc.event_end_geo_lat, gc.event_end_geo_lng, gc.event_end_location,
    gc.event_online_url,
    coalesce((select count(*)::int from challenge_movements m where m.challenge_id = gc.id), 0),
    coalesce((select count(*)::int
                from challenge_movement_completions cmc
               where cmc.challenge_id = gc.id and cmc.user_id = me), 0)
  from group_participants gp
  join group_challenges gc on gc.id = gp.challenge_id
  left join profiles p on p.user_id = gc.creator_id
  where gp.user_id = me
  order by (gp.completed_at is null) desc, gc.end_date desc, gp.joined_at desc;
end$function$;

create function public.list_my_challenges()
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
  my_progress bigint,
  is_completed boolean,
  joined_at timestamptz,
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
set search_path to 'public', 'private', 'pg_temp'
as $function$ select * from private.list_my_challenges() $function$;

revoke all on function private.list_my_challenges() from public, anon;
revoke all on function public.list_my_challenges() from public, anon;
grant execute on function private.list_my_challenges() to authenticated, service_role;
grant execute on function public.list_my_challenges() to authenticated, service_role;
