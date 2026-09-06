# Orbit Personal OS — İsteğe Bağlı Fitness Senkronizasyonu

Fitness, antrenman verilerinin ana ve tek doğru kaynağıdır. Orbit yalnızca kullanıcı bağlantıya açıkça izin verdikten, yetkili ve uygun premium Orbit hesabını bağladıktan ve senkronizasyonu açtıktan sonra yeniden hesaplanmış özeti alır.

## Veri akışı

1. Kullanıcı `Ayarlar → Entegrasyonlar → Orbit Personal OS` bölümünden kısa ömürlü ve imzalı bir bağlantı durumu ister.
2. Orbit hesap yetkilendirmesini tamamlar ve HMAC ile imzalanmış veriyi `orbit-fitness-callback` fonksiyonuna gönderir.
3. Callback, bağlantı durumunu yalnızca bir kez kullanır. Android uygulamasında Orbit erişim anahtarı saklanmaz. Premium yetkisi yalnızca Orbit sunucusundan kabul edilir.
4. Fitness'ta bir antrenman tamamlama veya düzeltme işlemi önce yerel olarak kaydedilir. Ardından birleştirilmiş arka plan görevi normal Fitness kayıtlarını gönderir ve `orbit-fitness-integration` fonksiyonunu çağırır.
5. Edge Function haftalık özeti gerçek `workout_logs`, `exercise_logs` kayıtlarından ve aktif programdan yeniden oluşturur. Ayrı bir sayaç artırılmaz veya azaltılmaz.

## Sunucu kurulumu

`20260906120000_orbit_fitness_integration.sql` migration dosyasını ve iki Edge Function'ı dağıt. Ardından aşağıdaki Edge Function secret'larını güvenli şekilde tanımla:

- `ORBIT_FITNESS_CONNECT_URL`: Orbit hesap bağlama adresi.
- `ORBIT_FITNESS_SYNC_URL`: Minimum antrenman özetini kabul eden Orbit sunucu adresi.
- `ORBIT_FITNESS_DISCONNECT_URL`: Sunucu tarafında bağlantıyı iptal eden isteğe bağlı adres.
- `ORBIT_FITNESS_SERVER_SECRET`: Sunucudan sunucuya yapılan isteklerde kullanılan bearer secret.
- `ORBIT_FITNESS_REQUEST_SECRET`: Fitness → Orbit istek gövdesini ve zaman bilgisini HMAC ile imzalayan secret.
- `ORBIT_FITNESS_STATE_SECRET`: Kısa ömürlü ve tek kullanımlık bağlantı durumunu imzalayan secret.
- `ORBIT_FITNESS_WEBHOOK_SECRET`: Callback isteklerindeki `timestamp.raw_body` HMAC-SHA256 imzasını doğrulayan secret.

Canlı ortamda kullanılacak Orbit adresleri:

- Bağlanma: `https://<orbit-host>/api/integrations/profitness/connect`
- Senkronizasyon: `https://<orbit-host>/api/integrations/profitness/sync`
- Bağlantıyı kesme: `https://<orbit-host>/api/integrations/profitness/disconnect`

`SERVER`, `REQUEST` ve `WEBHOOK` secret'ları birbirinden farklı olmalıdır. Değerler yalnızca Supabase Edge Function secret kasasında ve Cloudflare Worker secret kasasında tutulmalıdır. Android `BuildConfig` içine, kaynak koda, Git'e, loglara, ekran görüntülerine veya sohbet mesajlarına kesinlikle eklenmemelidir.

Orbit callback isteği `x-webhook-timestamp` ve `x-webhook-signature` başlıklarıyla birlikte aşağıdaki veriyi gönderir:

```json
{
  "eventId": "stable-event-id",
  "type": "connection.updated",
  "state": "state returned by the connect URL",
  "fitnessUserId": "Fitness user id bound inside the signed state",
  "orbitAccountId": "orbit-account-id",
  "accountLabel": "name@example.com",
  "authorized": true,
  "fitnessSyncEntitled": true,
  "manageUrl": "https://orbit.example/settings/integrations"
}
```

Daha sonra gönderilen premium yetki değişikliği veya bağlantı iptali olaylarında `state` alanı bulunmayabilir. İmzalı webhook isteği, daha önce bağlanmış `orbitAccountId` üzerinden ilgili kullanıcıyla eşleştirilir.

## Doğrulama tablosu

| # | Senaryo | Beklenen davranış |
|---|---|---|
| 1 | Yalnızca Fitness kullanan kişi | Entegrasyon durumu belirlendikten sonra normal açılış ve antrenman sırasında Orbit'e bağımlılık veya istek olmaz. |
| 2 | Orbit bağlı değil | Ayarlar ekranında “Bağlı değil” gösterilir; antrenmanlar normal çalışır. |
| 3 | Hesap bağlı fakat premium yetkisi yok | Senkronizasyon anahtarı kapalıdır; sunucu senkronizasyon denemelerini reddeder. |
| 4 | Hesap bağlı, premium yetkili ve senkronizasyon açık | Minimum antrenman özeti Orbit'e gönderilebilir. |
| 5 | Bugünkü antrenmanı tamamlama | Fitness önce kendi kaydını oluşturur; Orbit günün ve haftanın yeniden hesaplanmış durumunu alır. |
| 6 | Birden fazla antrenman | Tamamlanan seanslar ayrı gerçek antrenman kayıtlarından hesaplanır. |
| 7 | Tamamlamayı geri alma veya düzeltme | Yeni özet eski türetilmiş değerin yerini alır; ayrıca tutulan bir azaltma sayacı yoktur. |
| 8 | Orbit'e ulaşılamıyor | Fitness işlemi başarılı olur; entegrasyon kullanıcıyı engellemeyen geçici bir hata kaydeder. |
| 9 | Bağlantıyı kesme | Bağlantı ve bekleyen durumlar kaldırılır; Fitness geçmişi korunur. |
| 10 | Yeniden bağlanma | Kullanıcı anahtarına göre yapılan güncelleme tek bağlantıyı geri yükler ve özet ana veriden yeniden hesaplanır. |
| 11 | Aynı gönderimin veya callback'in tekrarlanması | Benzersiz idempotency ve olay anahtarları aynı işlemin ikinci kez uygulanmasını engeller. |
| 12 | Premium yetkisinin değişmesi | Önbellekteki yetkiyi yalnızca imzalı Orbit callback istekleri değiştirebilir. |
| 13 | Yetkisiz erişim | JWT'den alınan kullanıcı filtresi, yalnızca veri sahibine izin veren SELECT RLS kuralı ve yalnızca sunucudan yazma kuralı uygulanır. |
| 14 | Mobil ayarlar | Compose ekranı Bağlan / Yönet / Bağlantıyı Kes seçeneklerini ve 54 dp ana işlem alanını gösterir. |
| 15 | Masaüstü ayarları | Sunucu sözleşmesi istemciden bağımsızdır. Bu repoda masaüstü Fitness hedefi bulunmadığı için masaüstü görsel kontrolü Orbit istemcisinde yapılır. |

JVM politika testlerinde bu kuralları karşılayan 15 bağlantı ve durum testi bulunur. Veritabanı RLS kuralları, webhook imza doğrulaması ve uçtan uca veri gönderimi; canlı ortamdan önce bağlı bir test Supabase projesinde de denenmelidir.
