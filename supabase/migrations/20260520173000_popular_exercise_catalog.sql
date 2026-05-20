-- Add globally visible common exercises with tracking metadata.

update public.exercises
set sport_type = 'strength',
    tracking_mode = 'strength'
where lower(coalesce(name, '') || ' ' || coalesce(name_en, '') || ' ' || coalesce(category, '')) ~
      '(^|[^a-z])(row|barbell row|dumbbell row|cable row|machine row|t-bar row|inverted row)([^a-z]|$)'
  and lower(coalesce(name, '') || ' ' || coalesce(name_en, '')) !~ '(rowing|rower|ergometer|outdoor row)';

update public.exercises
set sport_type = 'jump_rope_hiit',
    tracking_mode = 'duration_reps'
where lower(coalesce(name, '') || ' ' || coalesce(name_en, '')) ~
      '(jump rope|skipping rope|double under|ip atlama)';

with candidates (
  id, name, name_en, target_muscle, category, sets_default, reps_default,
  description, sport_type, tracking_mode, met_value
) as (
  values
    ('ee962438-0bb2-4c1d-a567-f6baeac725d0'::uuid, 'Row', 'Row', 'Sırt', 'Serbest Ağırlık', 3, 10, 'Genel sırt çekiş hareketi; barbell, dumbbell veya cable row varyasyonları için ağırlık ve tekrar takibi.', 'strength', 'strength', 5.0),
    ('deb060ba-a7bb-49b0-928c-e109a474bfb0'::uuid, 'Barbell Row', 'Barbell Row', 'Sırt', 'Serbest Ağırlık', 4, 8, 'Barbell ile yatay çekiş; ağırlık ve tekrar takibi.', 'strength', 'strength', 5.5),
    ('999699df-2051-4176-b31e-e17cdd142ecc'::uuid, 'Dumbbell Row', 'Dumbbell Row', 'Sırt', 'Serbest Ağırlık', 3, 10, 'Tek kol dumbbell row varyasyonu; ağırlık ve tekrar takibi.', 'strength', 'strength', 5.0),
    ('cb9629af-6654-4e55-8398-b8ba9e4d7d7a'::uuid, 'Seated Cable Row', 'Seated Cable Row', 'Sırt', 'Makine', 3, 12, 'Cable row makinesiyle yatay çekiş; ağırlık ve tekrar takibi.', 'strength', 'strength', 5.0),
    ('d4626198-f5bf-4870-8660-f842e633dd40'::uuid, 'Chest Supported Row', 'Chest Supported Row', 'Sırt', 'Makine', 3, 10, 'Göğüs destekli row; ağırlık ve tekrar takibi.', 'strength', 'strength', 5.0),
    ('fc72bb42-8225-4f17-896a-7c7a2b306e01'::uuid, 'T-Bar Row', 'T-Bar Row', 'Sırt', 'Makine', 4, 8, 'T-bar row ile sırt kalınlığı odaklı çekiş.', 'strength', 'strength', 5.5),
    ('36e9e610-0813-44ac-993a-e5ee86990698'::uuid, 'Machine Row', 'Machine Row', 'Sırt', 'Makine', 3, 12, 'Makine row hareketi; ağırlık ve tekrar takibi.', 'strength', 'strength', 5.0),
    ('63d9ffc8-8cc1-440f-895f-2fcfc4385479'::uuid, 'Wide Grip Cable Row', 'Wide Grip Cable Row', 'Sırt', 'Makine', 3, 12, 'Geniş tutuş cable row; ağırlık ve tekrar takibi.', 'strength', 'strength', 5.0),
    ('0662485d-1201-4906-a11f-e2db0ac1d58b'::uuid, 'Inverted Row', 'Inverted Row', 'Sırt', 'Vücut Ağırlığı', 3, 10, 'Vücut ağırlığıyla yatay çekiş; tekrar takibi.', 'strength', 'strength', 3.8),
    ('24e42741-0126-4ad8-8afe-11899dba2633'::uuid, 'Jump Rope', 'Jump Rope', 'Kardiyovasküler', 'Kardiyo', 1, 500, 'İp atlama; süre ve atlama sayısı takibi.', 'jump_rope_hiit', 'duration_reps', 12.3),
    ('45f46466-789c-4ab6-bfe1-c5b45dab74d3'::uuid, 'Double Unders', 'Double Unders', 'Kardiyovasküler', 'Kardiyo', 1, 100, 'Double-under ip atlama; süre ve tekrar sayısı takibi.', 'jump_rope_hiit', 'duration_reps', 12.3),
    ('53048e3e-19ac-4b97-b304-1465349638a3'::uuid, 'Rowing Machine', 'Rowing Machine', 'Kardiyovasküler', 'Kardiyo', 1, 20, 'Ergometre/kürek makinesi; süre ve mesafe takibi.', 'rowing', 'duration_distance', 7.0),
    ('a0afaa0f-42da-46b5-bee7-6dd20c3f79b9'::uuid, 'Elliptical', 'Elliptical', 'Kardiyovasküler', 'Kardiyo', 1, 30, 'Eliptik bisiklet; süre ve mesafe takibi.', 'running', 'duration_distance', 5.0),
    ('c43022b3-90a4-4f59-a82b-9dd1470c50c0'::uuid, 'Stair Climber', 'Stair Climber', 'Kardiyovasküler', 'Kardiyo', 1, 20, 'Merdiven tırmanma makinesi; süre takibi.', 'walking_hiking', 'duration', 8.8),
    ('b6eba6f4-136d-45c6-ba2a-29671332c437'::uuid, 'Battle Ropes', 'Battle Ropes', 'Kondisyon', 'Kardiyo', 3, 30, 'Battle rope intervalleri; set bazlı süre takibi.', 'jump_rope_hiit', 'duration', 8.0),
    ('963ef88d-4a20-4824-887f-740eed74011e'::uuid, 'Push-Up', 'Push-Up', 'Göğüs', 'Vücut Ağırlığı', 3, 12, 'Vücut ağırlığıyla şınav; tekrar takibi.', 'strength', 'strength', 3.8),
    ('1a53755d-577a-48f8-a4da-33e4122d9dc2'::uuid, 'Pull-Up', 'Pull-Up', 'Sırt', 'Vücut Ağırlığı', 3, 8, 'Vücut ağırlığıyla barfiks; tekrar takibi.', 'strength', 'strength', 4.0),
    ('5b088d04-cb36-4c3d-9575-9e997cb9c47e'::uuid, 'Dips', 'Dips', 'Kol', 'Vücut Ağırlığı', 3, 10, 'Paralel bar dips; tekrar takibi.', 'strength', 'strength', 4.0),
    ('d296177c-71d2-458f-90d6-773cc5f50f2a'::uuid, 'Plank', 'Plank', 'Karın', 'Vücut Ağırlığı', 3, 60, 'Core stabilizasyon hareketi; set bazlı süre takibi.', 'strength', 'duration', 3.3),
    ('8ebc8679-ad54-4549-8f59-da1bb218d696'::uuid, 'Burpee', 'Burpee', 'Kondisyon', 'Vücut Ağırlığı', 3, 12, 'Tam vücut kondisyon hareketi; tekrar takibi.', 'strength', 'strength', 8.0)
)
insert into public.exercises (
  id, name, name_en, target_muscle, category, sets_default, reps_default,
  description, sport_type, tracking_mode, met_value
)
select c.id, c.name, c.name_en, c.target_muscle, c.category, c.sets_default, c.reps_default,
       c.description, c.sport_type, c.tracking_mode, c.met_value
from candidates c
where not exists (
  select 1
  from public.exercises e
  where lower(trim(e.name)) = lower(trim(c.name))
     or lower(trim(coalesce(e.name_en, ''))) = lower(trim(c.name_en))
);
