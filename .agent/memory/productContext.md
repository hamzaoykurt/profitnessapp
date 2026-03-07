# Product Context — Profitness

## Projenin Var Oluş Sebebi

Mevcut fitness uygulamaları (MyFitnessPal, Hevy, Strong) ya çok karmaşık UI'a sahip ya da estetik açıdan tatmin edici değil. Profitness, **premium görsel deneyimi** üstün UX mühendisliği ile birleştiren, yapay zeka destekli bir alternatif sunmayı hedefliyor.

---

## Çözülen Problemler

| Problem | Profitness Çözümü |
|---------|-------------------|
| Karmaşık antrenman kaydı | Hızlı "tap to log" arayüzü |
| Motivasyon eksikliği | AI Coach ile bağlamsal geri bildirim |
| Jenerik tasarım | Dual-mode bespoke UI sistemi |
| Veri kalıcılığı | DataStore (tema) + Room DB (workout, planlanan) |
| Tema esnekliği | Light & Dark mode, 6 accent renk seçeneği, kalıcı tercih |

---

## Kullanıcı Deneyimi Hedefleri

- **İlk İzlenim:** Uygulamayı açan kullanıcı "premium" hissi almalı (< 3 saniye)
- **Günlük Kullanım:** Antrenman kayıt süresi < 30 saniye
- **Alışkanlık:** Günlük açılış teşviki (streak, motivasyon mesajı)
- **AI Chat:** Kullanıcı sorularına < 2s yanıt; bağlamsal ve kişiselleştirilmiş
- **Tema Tercihi:** Seçilen tema (dark/light + accent) uygulama kapansa da korunmalı

---

## Ekran Akışı (Navigation)

```
MainActivity (NavHost)
├── WorkoutScreen        — Günlük egzersiz planı, set/rep takibi
├── ProgramBuilderScreen — Program şablonları, AI builder, manuel builder
├── AICoachScreen        — Oracle AI sohbet arayüzü
├── NewsScreen           — Muse — bilim/beslenme/motivasyon içerikleri
└── ProfileScreen        — İstatistikler, ayarlar, tema/dil/bildirim tercihleri
```

---

## Tasarım Prensipleri

1. **Dual-Mode First:** Her bileşen hem dark hem light temada doğal görünmeli
   - Dark: Neon Forge — `#0A0A0F` zemin, elektrik neon accent renkler
   - Light: Warm & Earthy — `#FAF8F5` zemin, doygun/okunabilir accent varyantlar
2. **Motion:** Her geçiş animasyonlu; idle animasyonlar hayat katar
3. **Density:** Bilgi yoğunluğu kontrollü — overload yok
4. **Tactile Feel:** Dokunuş geri bildirimi, scale/spring efektleri
5. **Readability:** Light modda kontrast en az WCAG AA seviyesinde olmalı

---

## Tema Sistemi Kullanıcı Kontrolü

Kullanıcı `ProfileScreen → Ayarlar` üzerinden:
- **Dark / Light mod** geçişi yapabilir
- **6 accent rengi** (LIME, PURPLE, CYAN, ORANGE, PINK, BLUE) seçebilir
- **Dil** (TR / EN) değiştirebilir
- **Bildirimler** açıp/kapatabilir

Tüm tercihler `ThemeRepository` (DataStore Preferences) ile kalıcı olarak saklanır.

---

## Hedef Kullanıcı

- Orta-ileri düzey fitness enthusiast'lar
- Kendi programını oluşturmak isteyen bireyler
- 18–35 yaş, teknoloji meraklısı, estetiğe önem veren kullanıcılar
- Hem "dark mode severler" hem de "light mode tercih edenler"
