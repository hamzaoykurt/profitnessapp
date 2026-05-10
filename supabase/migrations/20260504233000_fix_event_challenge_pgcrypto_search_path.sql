-- pgcrypto is installed in the extensions schema on Supabase.
-- Keep event challenge RPC functions able to resolve crypt/gen_salt.
alter function private.create_event_challenge(
  text, text, text, text, text, text, text,
  double precision, double precision, text, text, bigint,
  text, text, jsonb,
  double precision, double precision, text
) set search_path = public, extensions, pg_temp;

alter function public.create_event_challenge(
  text, text, text, text, text, text, text,
  double precision, double precision, text, text, bigint,
  text, text, jsonb,
  double precision, double precision, text
) set search_path = public, private, extensions, pg_temp;
