-- Profiles may store an emoji/text avatar or a profile photo served from the
-- app-owned Supabase Storage bucket. External HTTP avatar URLs are rejected.
alter table public.profiles
drop constraint if exists profiles_avatar_url_safe;

alter table public.profiles
add constraint profiles_avatar_url_safe
check (
  avatar_url is null
  or btrim(avatar_url) = ''
  or avatar_url !~* '^https?://'
  or avatar_url like 'https://dkcriptafzdrynsilxku.supabase.co/storage/v1/object/public/profile-photos/%'
);
