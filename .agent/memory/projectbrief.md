# Project Brief — Profitness

## Proje Tanımı

**Profitness**, kullanıcıların antrenman programlarını takip edebildiği, günlük egzersizlerini yönetebildiği ve yapay zeka destekli koçluk alabileceği premium bir Android fitness uygulamasıdır. Avonix tarafından geliştirilmektedir.

Backend: **Supabase** (PostgreSQL + Auth + Storage + Edge Functions)
AI: **Gemini API**
Tema: **Neon Forge Dark (tek tema — light mode kaldırıldı)**

---

## Kapsam — 10 Faz

| Faz | İçerik | Durum |
|-----|--------|-------|
| FAZ 1 | DB şeması + Auth + Exercise seed | 🔄 Aktif |
| FAZ 2 | Program sistemi (hazır/manuel/düzenleme) | ⏳ Bekliyor |
| FAZ 3 | Workout takibi + streak + timer + bug fix | ⏳ Bekliyor |
| FAZ 4 | Gemini AI entegrasyonu (chat + program builder) | ⏳ Bekliyor |
| FAZ 5 | Profil + analitik + başarımlar + rank | ⏳ Bekliyor |
| FAZ 6 | Sosyal özellikler (paylaşım + grup challenge) | ⏳ Bekliyor |
| FAZ 7 | Abonelik + kredi sistemi + Google Play Billing | ⏳ Bekliyor |
| FAZ 7.5 | Commitment Mode (Disiplin Modu) | ⏳ Bekliyor |
| FAZ 8 | Auth redesign + light mode kaldırma + haberler + çeviri | ⏳ Bekliyor |
| FAZ 9 | Optimizasyon (21 bulgu) + güvenlik | ⏳ Bekliyor |

### Temel Özellikler

- **Antrenman Takibi:** Günlük egzersiz planı, set/tekrar/ağırlık kaydı, set timer
- **Program Yönetimi:** Hazır şablonlar + Gemini AI builder + manuel program oluşturma
- **AI Chat (Oracle):** Gemini tabanlı fitness koçu, konuşma geçmişi, program önerisi
- **İçerik Akışı (Muse):** Bilim, beslenme, motivasyon haberleri; bookmark
- **Profil & İstatistikler:** XP/level, streak, haftalık aktivite, VKİ, başarımlar, rank
- **Sosyal:** Program paylaşma, beğenme, grup challenge, leaderboard
- **Commitment Mode:** Sanal ceza sistemi (XP/kredi kaybı)
- **Abonelik:** Kredi sistemi + Google Play Billing

### Kapsam Dışı

- Canlı antrenman videoları
- Wearable entegrasyonu
- Beslenme takibi (makro/kalori manuel giriş)

---

## Hedef Kullanıcı

- Orta-ileri düzey fitness enthusiast'lar
- Kendi programını oluşturmak isteyen bireyler
- 18–35 yaş, teknoloji meraklısı, AI koçluk ve sosyal motivasyon arayan kullanıcılar

---

## Başarı Kriterleri

1. Uygulama sorunsuz derlenir ve çalışır (Debug + Release)
2. Tüm ana ekranlar işlevsel, Supabase'e bağlı ve görsel olarak tutarlı
3. Auth akışı (register/login/logout) çalışır, RLS aktif
4. AI Chat gerçek Gemini yanıtı döner, konuşma geçmişi kaydedilir
5. XP/streak/başarım sistemi tutarlı ve DB'ye yazılır
6. Kredi sistemi doğru çalışır (3 free → satın alma)
7. Minimum ANR / crash: Crash-free rate ≥ 99%

---

## Teknik Kısıtlar

- **Min SDK:** 31 (Android 12) — `minSdk = 31`
- **Target/Compile SDK:** 35
- Kotlin + Jetpack Compose (XML layout kullanılmaz)
- Hilt ile DI (manuel DI yok)
- Tüm async işlemler Coroutine/Flow tabanlı, Supabase çağrıları `Dispatchers.IO`'da
- Repository: interface + Impl ayrımı, mapper = extension function
- Use Case: sadece CalorieCalculation ve XpCalculation

---

## Proje Durumu

- **UI Katmanı:** ✅ Tamamlandı — tüm ekranlar Neon Forge Dark tema
- **Tema Sistemi:** ✅ Dark only (Neon Forge) + DataStore persistence
- **Backend:** 🔄 FAZ 1 — Supabase migration oluşturuluyor
- **Auth:** 🔄 FAZ 1 — Supabase Auth entegrasyonu
- **AI:** ⏳ FAZ 4 — Gemini API bağlantısı
