-- Profil kapağı sunucu eşitlemesi (katkısal migration)
-- Not: public.profiles UPDATE politikası zaten mevcut ("Users manage own profile", FOR ALL).
-- Depolama bucket'ı ('profile-photos') avatarlar ile paylaşıldığından bucket boyut/MIME sınırı değiştirilmedi.

-- 1) Sütunlar ve kısıtlar
alter table public.profiles add column if not exists cover_url text;
alter table public.profiles add column if not exists cover_preset text;
alter table public.profiles add constraint profiles_cover_preset_check
    check (cover_preset is null or cover_preset in ('photo','amber','coral','violet','teal','blue','green','crimson','graphite'));
alter table public.profiles add constraint profiles_cover_url_check
    check (cover_url is null or length(cover_url) <= 1024);

-- 2) Depolama RLS kuralları: covers/<user_id>/... (SELECT + INSERT + UPDATE + DELETE)
create policy "profile_cover_select_own" on storage.objects
    for select to authenticated
    using (
        bucket_id = 'profile-photos'
        and (storage.foldername(name))[1] = 'covers'
        and (storage.foldername(name))[2] = (select auth.uid())::text
    );

create policy "profile_cover_insert_own" on storage.objects
    for insert to authenticated
    with check (
        bucket_id = 'profile-photos'
        and (storage.foldername(name))[1] = 'covers'
        and (storage.foldername(name))[2] = (select auth.uid())::text
        and lower(storage.extension(name)) in ('jpg', 'jpeg', 'webp')
    );

create policy "profile_cover_update_own" on storage.objects
    for update to authenticated
    using (
        bucket_id = 'profile-photos'
        and (storage.foldername(name))[1] = 'covers'
        and (storage.foldername(name))[2] = (select auth.uid())::text
    )
    with check (
        bucket_id = 'profile-photos'
        and (storage.foldername(name))[1] = 'covers'
        and (storage.foldername(name))[2] = (select auth.uid())::text
        and lower(storage.extension(name)) in ('jpg', 'jpeg', 'webp')
    );

create policy "profile_cover_delete_own" on storage.objects
    for delete to authenticated
    using (
        bucket_id = 'profile-photos'
        and (storage.foldername(name))[1] = 'covers'
        and (storage.foldername(name))[2] = (select auth.uid())::text
    );
