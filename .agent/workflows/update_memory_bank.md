---
description: Memory Bank dosyalarını güncelle
---

# Memory Bank Güncelleme Workflow'u

Her büyük çalışma sonrasında aşağıdaki adımları takip et:

## Adım 1: activeContext.md Güncelle

`.agent/memory/activeContext.md` dosyasını aç ve şu bölümleri güncelle:

- **Son Tamamlanan Değişiklikler** — yeni tamamlanan iş ekle
- **Aktif Kararlar & Öğrenmeler** — yeni teknik kararlar varsa ekle
- **Bir Sonraki Adımlar** — tamamlananları işaretle, yenilerini ekle
- **Dosya başındaki tarihi** (`_Son güncelleme: YYYY-MM-DD_`) güncelle

## Adım 2: progress.md Güncelle

`.agent/memory/progress.md` dosyasını aç ve şu bölümleri güncelle:

- **Tamamlananlar** bölümüne yeni `[x]` maddeler ekle
- **Devam Eden** bölümünü güncelle
- **Bilinen Sorunlar** tablosunu güncelle (çözüldüyse kaldır, yenileri ekle)
- **Dosya başındaki tarihi** güncelle

## Adım 3: Değişiklikler Mimariyi Etkilediyse

Eğer yeni mimari karar, pattern veya bileşen eklendiyse:

- `.agent/memory/systemPatterns.md` dosyasını güncelle
- **Bilinen Mimari Kararlar** tablosuna yeni satır ekle

## Adım 4: Yeni Bağımlılık Eklendiyse

- `.agent/memory/techContext.md` dosyasının **Bağımlılıklar** tablosuna ekle

---

> **Not:** `projectbrief.md` ve `productContext.md` dosyaları nadiren değişir. Sadece proje kapsamı veya hedef kitle köklü biçimde değişirse güncelle.
