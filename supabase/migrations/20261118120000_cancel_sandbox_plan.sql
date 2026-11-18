-- Demo (sandbox) aboneliği iptali: kullanıcı yalnız KENDİ satırını, yalnız source='sandbox' iken Ücretsiz'e döndürür.
-- Gerçek (Play) abonelikleri burada iptal edilmez -> {ok:false, reason:'manage_in_store'}.
-- Örüntü: private security definer + public invoker wrapper (20261117120000_list_my_challenges_sport_type.sql ile aynı).
-- ONAY GEREKİYOR: Hamza uygular (istemci bu RPC'yi çağırır; fonksiyon yoksa hata mesajı gösterir).

create or replace function private.cancel_sandbox_plan()
returns jsonb
language plpgsql
volatile
security definer
set search_path to 'public'
as $function$
declare
  me uuid := auth.uid();
  v_row public.user_entitlements%rowtype;
begin
  if me is null then
    return jsonb_build_object('ok', false, 'reason', 'unauthorized');
  end if;

  select * into v_row from public.user_entitlements where user_id = me for update;
  if not found then
    return jsonb_build_object('ok', true, 'plan', 'FREE', 'previousSource', null);   -- zaten Ücretsiz
  end if;

  if v_row.source is distinct from 'sandbox' then
    if v_row.plan = 'FREE' then
      return jsonb_build_object('ok', true, 'plan', 'FREE', 'previousSource', v_row.source);
    end if;
    return jsonb_build_object('ok', false, 'reason', 'manage_in_store', 'plan', v_row.plan, 'previousSource', v_row.source);
  end if;

  update public.user_entitlements
     set plan = 'FREE',
         status = 'free',
         source = 'system',
         current_period_start = null,
         current_period_end = null,
         updated_at = now()
   where user_id = me;

  return jsonb_build_object('ok', true, 'plan', 'FREE', 'previousSource', 'sandbox');
end;
$function$;

revoke all on function private.cancel_sandbox_plan() from public, anon;

create or replace function public.cancel_sandbox_plan()
returns jsonb
language sql
volatile
set search_path to 'public', 'private', 'pg_temp'
as $function$
  select private.cancel_sandbox_plan()
$function$;

revoke all on function public.cancel_sandbox_plan() from public, anon;
grant execute on function public.cancel_sandbox_plan() to authenticated, service_role;
grant usage on schema private to authenticated;
grant execute on function private.cancel_sandbox_plan() to authenticated, service_role;
