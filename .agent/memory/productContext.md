# Product Context — Profitness

## Projenin Var Oluş Sebebi

Mevcut fitness uygulamaları (MyFitnessPal, Hevy, Strong) ya çok karmaşık UI'a sahip ya da estetik açıdan tatmin edici değil. Profitness, **premium görsel deneyimi** üstün UX mühendisliği ile birleştiren, yapay zeka destekli bir alternatif sunmayı hedefliyor.

---

## Çözülen Problemler

| Problem | Profitness Çözümü |
|---------|-------------------|
| Karmaşık antrenman kaydı | Hızlı "tap to log" arayüzü |
| Motivasyon eksikliği | AI Coach (Oracle) + Commitment Mode + streak sistemi |
| Jenerik tasarım | Neon Forge Dark — premium bespoke UI |
| Veri kalıcılığı | Supabase PostgreSQL backend |
| AI koçluk | Gemini API — gerçek LLM yanıtları |
| Sosyal motivasyon | Program paylaşma, grup challenge'ları |
| Disiplin eksikliği | Commitment Mode — sanal ceza sistemi (XP/kredi kaybı) |

---

## Kullanıcı Deneyimi Hedefleri

- **İlk İzlenim:** Uygulamayı açan kullanıcı "premium" hissi almalı (< 3 saniye)
- **Günlük Kullanım:** Antrenman kayıt süresi < 30 saniye
- **Alışkanlık:** Streak sistemi + Commitment Mode ile günlük açılış teşviki
- **AI Chat:** Kullanıcı sorularına gerçek Gemini yanıtı; bağlamsal ve kişiselleştirilmiş
- **Sosyal:** Program paylaşma, beğenme, grup challenge leaderboard

---

## Ekran Akışı (Navigation)

```
MainActivity (NavHost)
├── AuthScreen           — Kayıt / Giriş / Şifremi Unuttum
├── WorkoutScreen        — Günlük egzersiz planı, set/rep takibi, timer
├── ProgramBuilderScreen — Program şablonları, AI builder, manuel builder
├── AICoachScreen        — Oracle AI sohbet arayüzü (Gemini)
├── NewsScreen           — Muse — bilim/beslenme/motivasyon içerikleri
├── ProfileScreen        — İstatistikler, başarımlar, rank, ayarlar
├── DiscoverScreen       — Sosyal — başkalarının programları
├── ChallengeScreen      — Grup etkinlikleri, leaderboard
└── SubscriptionScreen   — Kredi satın alma, abonelik
```

---

## Tasarım Prensipleri

1. **Dark Only:** Sadece Neon Forge Dark — `#0A0A0F` zemin, elektrik neon accent renkler
2. **Motion:** Her geçiş animasyonlu; idle animasyonlar hayat katar
3. **Density:** Bilgi yoğunluğu kontrollü — overload yok
4. **Tactile Feel:** Dokunuş geri bildirimi, scale/spring efektleri
5. **Readability:** WCAG AA kontrast oranı dark modda korunmalı

---

## Commitment Mode (Disiplin Modu)

Sanal ceza sistemi — gerçek para yok:
- Kullanıcı haftalık hedef gün sayısı belirler (commitment_contracts tablosu)
- Kaçırılan gün: −50 XP
- 3 gün üst üste: streak sıfırlama
- 7 gün üst üste: −1 kredi
- Hedef tutturanlar: +100 XP/hafta + özel başarım

---

## Kredi & Abonelik Sistemi

- Başlangıçta 3 ücretsiz AI kredisi (`is_trial_used` flag ile hesap bazlı)
- AI Chat: 3 kredi/mesaj
- AI Program Oluşturma: 1 kredi
- Kredi paketleri: 10 / 50 / 100 kredi (Google Play Billing)

---

## Hedef Kullanıcı

- Orta-ileri düzey fitness enthusiast'lar
- Kendi programını oluşturmak isteyen bireyler
- 18–35 yaş, teknoloji meraklısı, estetiğe önem veren kullanıcılar
- AI koçluk ve sosyal motivasyon arayan kullanıcılar
