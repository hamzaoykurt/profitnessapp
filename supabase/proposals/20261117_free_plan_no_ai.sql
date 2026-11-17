-- GEÇERSİZ (2026-11-17): yerine supabase/proposals/20261117_free_premium_ai_access.sql (Ücretsiz + Premium modeli).
-- ÖNERİ (UYGULANMADI, ONAY GEREKİYOR): Free planda AI'ı sunucuda kapat.
-- Bugünkü durum (20260519193000_rebalance_energy_plan_hierarchy.sql, reserve_ai_usage):
--   FREE için v_free_limit = 0 ama Enerji bakiyesi yetiyorsa İZİN VERİLİYOR (yeni hesaba 32 başlangıç Enerjisi;
--   20260519122728_raise_starter_credits_to_four_ai_programs.sql). Yani Free kullanıcı istemciyi atlatıp AI kullanabilir.
-- Öneri: reserve_ai_usage içinde plan çözümlendikten hemen SONRA (status kontrolünün altına, "case p_tool" öncesine)
-- aşağıdaki bloğu ekleyen yeni bir migration (fonksiyonun tam gövdesi yeniden yazılarak; create or replace).
-- ai-generate/gemini-generate değişmez: allowed=false -> 402, code = 'plan_required' döner; istemci Paywall gösterir.
-- Karar gereken: Onboarding'deki AI program üretimi (Free yeni kullanıcı) ve mevcut Free bakiyeler ne olacak?

/*  --- reserve_ai_usage içine eklenecek blok ---
    if v_plan = 'FREE' then
        insert into public.ai_usage_events (
            user_id, tool, status, entitlement_source, credit_cost, error_code, idempotency_key
        )
        values (p_user_id, p_tool, 'denied', 'denied', 0, 'plan_required', p_idempotency_key);
        return jsonb_build_object(
            'allowed', false,
            'reason', 'plan_required',
            'message', 'AI özellikleri Pro ve Elite planlarında.',
            'plan', v_plan
        );
    end if;
*/

-- İsteğe bağlı: yeni Free hesaplara başlangıç Enerjisi verilmemesi (ensure_billing_account içindeki starter tutarı 0).
