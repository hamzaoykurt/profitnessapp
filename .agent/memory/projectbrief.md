# Project Brief — Profitness

## Proje Tanımı

**Profitness**, kullanıcıların antrenman programlarını takip edebildiği, günlük egzersizlerini yönetebildiği ve yapay zeka destekli koçluk alabileceği premium bir Android fitness uygulamasıdır. Avonix tarafından geliştirilmektedir.

---

## Kapsam

### Temel Özellikler (Core Features)

- **Antrenman Takibi:** Günlük egzersiz planı, set/tekrar/ağırlık kaydı
- **Program Yönetimi:** Hazır şablonlar + AI builder + manuel program oluşturma
- **AI Chat (Oracle):** Yapay zeka destekli fitness koçu (sohbet arayüzü)
- **İçerik Akışı (Muse):** Bilim, beslenme, motivasyon haberleri; okuma + bookmark
- **Profil & İstatistikler:** Haftalık aktivite, streak takvimi, büyük bold stat kartları
- **Tema Sistemi:** Dark (Neon Forge) + Light (Warm & Earthy), 6 accent seçeneği, DataStore ile kalıcı

### Planlanan (Sonraki Milestone)

- **Veri Katmanı:** Room DB ile gerçek workout/exercise kalıcılığı
- **AI Entegrasyonu:** Gemini API — gerçek LLM yanıtları
- **Beslenme Takibi:** Kalori ve makro takibi

### Kapsam Dışı (MVP için)

- Sosyal paylaşım özellikleri
- Canlı antrenman videoları
- Wearable entegrasyonu

---

## Hedef Kullanıcı

- Orta-ileri düzey fitness enthusiast'lar
- Kendi programını oluşturmak isteyen bireyler
- 18–35 yaş, teknoloji meraklısı, hem dark hem light mode kullananlar

---

## Başarı Kriterleri

1. Uygulama sorunsuz derlenir ve çalışır (Debug + Release)
2. Tüm ana ekranlar (Workout, Studio, Oracle, Muse, Profile) işlevsel ve görsel olarak tutarlı
3. Light ve dark tema her ekranda doğal ve okunaklı görünür
4. Tema tercihi (dark/light + accent) uygulama kapatılınca kaybolmaz
5. Veri Room DB'de kalıcı olarak saklanır (planlanan)
6. Minimum ANR / crash: Crash-free rate ≥ 99%

---

## Teknik Kısıtlar

- **Min SDK:** 31 (Android 12) — `minSdk = 31`
- **Target/Compile SDK:** 35
- Kotlin + Jetpack Compose (XML layout kullanılmaz)
- Hilt ile DI (manuel DI yok)
- Tüm async işlemler Coroutine/Flow tabanlı
- Tema state'i: `rememberSaveable` (rotation) + `DataStore` (process kill)

---

## Proje Durumu

Aktif geliştirme aşamasında.
- **UI Katmanı:** Tamamlandı — tüm ekranlar dual-mode tema destekli
- **Tema Sistemi:** Tamamlandı — Dark (Neon Forge) + Light (Warm & Earthy) + DataStore persistence
- **Veri Katmanı:** Planlandı — Room DB entegrasyonu sonraki milestone
- **AI Entegrasyonu:** Planlandı — Gemini API bağlantısı sonraki milestone
