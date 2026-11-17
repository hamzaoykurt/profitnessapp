# Üyelik → AI erişimi (sade model, 2026-11-17)
Ethos: uygulama ücretsiz de yararlı; para yalnız AI için. Enerji/kredi KALKTI. Tek abonelik: PREMIUM (aylık/yıllık).
İstemci tek kaynak: `domain/ai/AiAccessPolicy.kt` (AiFeature, AiAccess, isAiLocked). Test: `AiAccessPolicyTest`.
Plan adı: `UserPlan` = FREE / PREMIUM; `UserPlan.fromServer` sunucunun eski "PRO"/"ELITE" adlarını PREMIUM sayar.

| | ÜCRETSİZ | PREMIUM |
|---|---|---|
| AI-dışı her şey (antrenman, manuel program, Keşfet, paylaşma, challenge/etkinlik, Kilo kaydı + grafik, başarım, istatistik, Hareketi gör/video) | Açık | Açık |
| AI program oluştur / içe aktar (PROGRAM_GENERATE_TEXT/_MEDIA) | Ömür boyu 2 hak (`FREE_AI_PROGRAM_GRANTS`) | Sınırsız* |
| AI ile düzenle, AI Koç sohbeti, AI Koç→program | Kilitli | Sınırsız* |
| Kilo / hareket / antrenman AI analizi, plan değerlendirme | Kilitli | Sınırsız* |
| Kişisel hafıza, öncelik (eski Elite) | — | Dahil |
*Adil kullanım: sunucuda günlük tavan + dakikada hız sınırı (öneride 200/gün, 6/dk; ayarlanabilir).

Kural: hiçbir AI çağrısı otomatik değil; yalnız kullanıcı tıklaması. Kilo analizi sonucu DiskCache'te, kayıt değişince "Analizi güncelle".
Kilitli UI (plan yüklenmeden kilit yok):
- Kilo kartı: kilit + "PREMIUM İLE AÇ"
- AI Koç: mesaj çubuğu yerine kilit kartı
- AI ile Oluştur: hak satırı ("2 ücretsiz AI program hakkın var" / "1 hakkın kaldı"), hak bitince kilit + Paywall
- AI düzenle: "Uygula · Premium"
- Hareket analizi: "PREMIUM İLE AÇ"
Mağaza: tek Premium kartı (aylık/yıllık), Premium avantajları + "Ücretsiz planda da var" listesi, demo notu; Enerji sekmesi yok.
Fiyatlar: Supabase ürünü (geçişte sub_pro_* Premium sayılır) yoksa demo ₺149/₺999; gerçek fiyat Play'den.

Sorumluluk: SUNUCU gerçek yetki + sayaç (reserve/complete/refund_ai_usage; ai-generate 402 + reason).
İSTEMCİ yalnız UI kilidi; kalan hakkı billing-status'tan gösterir (`freeAiProgramsLeft`, şimdilik yok → "2 AI program hakkı").
Sunucu önerisi (UYGULANMADI): `supabase/proposals/20261117_free_premium_ai_access.sql`. İçeriği:
- ayarlar tablosu, PRO/ELITE→PREMIUM, Enerjisiz reserve_ai_usage, geçiş sırası ve geri alma
- refund_ai_usage notu: güncellenmezse hata iadesi hakkı geri vermez

Açık kararlar:
- Satın alınmış Enerji bakiyeleri: Premium süre mi, iade mi?
- Adil kullanım sayıları
- Onboarding'deki AI program üretimi ücretsiz hakkı tüketsin mi?
- Satın alma geçişte sub_pro SKU'su ile; sub_premium_* ürünleri eklenecek

Play Billing fazı:
- billing-checkout: satın alma token doğrulama
- billing-webhook: RTDN → user_entitlements
- billing-status: freeAiProgramsLeft
- Premium ürün kimlikleri, iptal/yenileme
