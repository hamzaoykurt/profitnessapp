-- ÖNERİ — UYGULANMADI (ONAY GEREKİYOR). Canlı Supabase'e dokunulmadı.
-- Profil kapağı sunucu eşitlemesi: açık profilde başkaları da görsün.
-- İstemci hazır: ProfileCoverStore yerel seçimi önceler; yerel seçim yoksa profiles.cover_url / cover_preset okur
-- (sütunlar yokken sorgu hata verir -> yok sayılır). Yükleme/yazma istemci kodu migration sonrası eklenecek.

-- 1) Sütunlar
alter table public.profiles add column if not exists cover_url text;
alter table public.profiles add column if not exists cover_preset text;
alter table public.profiles drop constraint if exists profiles_cover_preset_check;
alter table public.profiles add constraint profiles_cover_preset_check
    check (cover_preset is null or cover_preset in ('photo','amber','coral','violet','teal','blue','green','crimson','graphite'));
alter table public.profiles drop constraint if exists profiles_cover_url_check;
alter table public.profiles add constraint profiles_cover_url_check
    check (cover_url is null or length(cover_url) <= 1024);

-- 2) RLS: kullanıcı yalnız kendi satırını günceller (mevcut profiles update politikası varsa onunla aynı koşul;
--    aynı adla varsa önce kontrol edin — burada yalnız yoksa eklenir).
do $$
begin
    if not exists (
        select 1 from pg_policies where schemaname = 'public' and tablename = 'profiles' and policyname = 'Users update own profile cover'
    ) then
        create policy "Users update own profile cover" on public.profiles
            for update to authenticated
            using (auth.uid() = user_id)
            with check (auth.uid() = user_id);
    end if;
end $$;

-- 3) Depolama: mevcut profil fotoğrafı bucket'ında covers/<user_id>/... yolu.
--    Bucket: 'profile-photos' (avatar yüklemesiyle aynı; ProfileRepositoryImpl.kt'de doğrulandı).
--    Kural: yalnız kendi klasörüne yazar; JPEG/WEBP; ≤ 2 MB (istemci ≤1080px JPEG ~80 üretir, tipik 150-400 KB).
create policy "Users write own cover" on storage.objects
    for insert to authenticated
    with check (
        bucket_id = 'profile-photos'
        and (storage.foldername(name))[1] = 'covers'
        and (storage.foldername(name))[2] = auth.uid()::text
        and lower(storage.extension(name)) in ('jpg', 'jpeg', 'webp')
    );
create policy "Users replace own cover" on storage.objects
    for update to authenticated
    using (bucket_id = 'profile-photos' and (storage.foldername(name))[1] = 'covers' and (storage.foldername(name))[2] = auth.uid()::text);
-- Boyut ve MIME sınırı bucket düzeyinde (tüm bucket'ı etkiler; avatarla uyumlu olmalı):
-- update storage.buckets set file_size_limit = 2097152, allowed_mime_types = array['image/jpeg','image/webp','image/png']
--  where id = 'profile-photos';

-- GERİ ALMA: policy'leri drop; alter table public.profiles drop column cover_url, drop column cover_preset;
