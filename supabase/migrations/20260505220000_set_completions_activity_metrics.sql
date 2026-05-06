alter table public.set_completions
    add column if not exists duration_seconds integer
        check (duration_seconds is null or duration_seconds >= 0),
    add column if not exists distance_meters real
        check (distance_meters is null or distance_meters >= 0);
