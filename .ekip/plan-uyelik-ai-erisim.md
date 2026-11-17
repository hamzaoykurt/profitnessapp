# Üyelik → AI erişimi (temel, 2026-11-17)
Tek kaynak (istemci): `domain/ai/AiAccessPolicy.kt` (AiFeature, AiAccess, isAiLocked). Test: `AiAccessPolicyTest`.

| Plan | AI | Not |
|---|---|---|
| FREE | YOK: kilo analizi, AI Koç sohbeti, AI ile Oluştur/düzenle, hareket/antrenman AI analizi, AI Koç→program | Sekmeler görünür; "Pro ile aç" → Mağaza |
| PRO | Hepsi | Aylık plan kotası, kota bitince Enerji |
| ELITE | Pro + kişisel profil/hafıza + öncelik | Daha yüksek kota |

AI eylemleri (sunucu aracı · kota bitince Enerji · Pro/Elite kotası):
- AI Koç mesajı ORACLE_CHAT · 10 mesajda 1 · GÜNLÜK 250 / 10000
- AI ile Oluştur PROGRAM_GENERATE_TEXT · 6 · 30 / 160; dosyalı PROGRAM_GENERATE_MEDIA · 10 · 12 / 90
- AI ile düzenle PROGRAM_EDIT · 4 · 40 / 160; AI Koç→program ORACLE_TO_PROGRAM · 4 · 30 / 160
- Kilo/Hareket/Antrenman analizi *_ANALYSIS · 2 · GÜNLÜK 20 / 10000 (diğerleri AYLIK)
Kaynak: supabase/migrations/20260519193000_rebalance_energy_plan_hierarchy.sql (reserve_ai_usage).

Kural: HİÇBİR AI çağrısı ekran açılışında/otomatik yapılmaz; yalnız kullanıcı tıklaması.
- Kilo takibi: "AI ile analiz et" düğmesi; sonuç DiskCache'te (imza = kayıt sayısı + son kayıt). Yeniden açılışta kredi yok; kayıt değişince "Analizi güncelle".
- Hareket analizi (Hareketi gör, Hareket performansı), AI Koç, AI ile Oluştur/düzenle: zaten tıklamayla; plan kilidi eklendi.
- Kilitli UI: Kilo kartı (kilit + PRO İLE AÇ), AI Koç (mesaj çubuğu yerine kilit kartı, öneriler önizleme), AI ile Oluştur (kilit ikonu, Paywall).
- Plan yüklenmeden kilit gösterilmez (Elite yanlış kilitlenmesin).

Sorumluluk: SUNUCU gerçek yetki + kredi (reserve/complete/refund_ai_usage, ai-generate 402). İSTEMCİ yalnız UI kilidi/maliyet satırı.
AÇIK: sunucu bugün FREE'yi reddetmiyor (Enerji ile izin; yeni hesaba 32 Enerji) → öneri: supabase/proposals/20261117_free_plan_no_ai.sql (uygulanmadı).
AÇIK: Onboarding AI program üretimi Free kullanıcıda çalışıyor (dokunulmadı; ürün kararı).
AÇIK: Hareket analizi sonuçları kalıcı önbellekte değil (yalnız oturum belleği); tıklamasız harcama yok.
AÇIK: Ücretli planda kota kullanımı istemcide bilinmiyor → düğme kredi 0'da pasif yapılmadı; sunucu 402 → Paywall + iade.

Google Play Billing fazında bağlanacaklar: billing-checkout (Play satın alma token doğrulama), billing-webhook (RTDN: yenileme/iptal → user_entitlements.status),
billing-status (plan/kota/bakiye özeti; istemci kalan kotayı da almalı), Mağaza plan metinleri ↔ AiFeature tablosu, Elite hafıza özelliğinin gerçek uygulanması.
