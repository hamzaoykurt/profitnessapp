-- Query indexes for hot workout and set-completion reads used by the mobile app.

create index if not exists set_completions_user_exercise_date_idx
  on public.set_completions (user_id, exercise_id, date desc);

create index if not exists set_completions_user_day_date_idx
  on public.set_completions (user_id, program_day_id, date desc);

create index if not exists workout_logs_user_date_idx
  on public.workout_logs (user_id, date desc);

create index if not exists workout_logs_user_day_date_idx
  on public.workout_logs (user_id, program_day_id, date desc);
