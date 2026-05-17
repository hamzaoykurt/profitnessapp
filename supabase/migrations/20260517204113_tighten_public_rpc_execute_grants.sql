revoke execute on function public.invite_friends_to_challenge(uuid, uuid[]) from public;
grant execute on function public.invite_friends_to_challenge(uuid, uuid[]) to authenticated;

revoke execute on function public.list_visible_challenges(integer, integer) from public;
grant execute on function public.list_visible_challenges(integer, integer) to authenticated;

revoke execute on function public.list_my_events_for_date(date) from public;
grant execute on function public.list_my_events_for_date(date) to authenticated;

revoke execute on function public.list_my_upcoming_events(integer) from public;
grant execute on function public.list_my_upcoming_events(integer) to authenticated;

revoke execute on function public.list_user_created_challenges(uuid, integer) from public;
grant execute on function public.list_user_created_challenges(uuid, integer) to authenticated;

revoke execute on function public.list_user_shared_programs(uuid, integer) from public;
grant execute on function public.list_user_shared_programs(uuid, integer) to authenticated;

revoke execute on function public.get_friend_leaderboard_streak(integer) from public;
revoke execute on function public.get_leaderboard_streak(integer) from public;
revoke execute on function public.get_my_rank_streak() from public;
