# Profitness Gizlilik Politikası

Son güncelleme: 17 Mayıs 2026

Bu Gizlilik Politikası, Profitness mobil uygulamasının kullanıcı verilerini hangi amaçlarla topladığını, nasıl işlediğini, nasıl sakladığını, kimlerle paylaşabileceğini ve kullanıcıların bu verilere ilişkin haklarını açıklar.

Bu politika, Profitness Android uygulaması, uygulama içi özellikler, hesap işlemleri, yapay zeka destekli özellikler, abonelik/kredi işlemleri ve ilgili destek süreçleri için geçerlidir.

İletişim: info@cosmibit.com

## 1. Veri Sorumlusu ve Kapsam

Profitness, kullanıcıların antrenmanlarını planlamasına, takip etmesine, gelişimini izlemesine, etkinlik/challenge özelliklerini kullanmasına ve yapay zeka destekli fitness deneyimlerinden yararlanmasına yardımcı olan bir mobil uygulamadır.

Bu politika kapsamında "kişisel veri", kimliği belirli veya belirlenebilir bir kullanıcıyla ilişkilendirilebilen bilgileri ifade eder.

## 2. İşlediğimiz Veri Kategorileri

Profitness, yalnızca uygulamanın çalışması, kullanıcı hesabının yönetilmesi, güvenliğin sağlanması ve kullanıcının talep ettiği özelliklerin sunulması için gerekli verileri işler.

### 2.1 Hesap ve Kimlik Doğrulama Verileri

Hesap oluşturma, giriş, e-posta doğrulama ve şifre sıfırlama süreçlerinde aşağıdaki veriler işlenebilir:

- E-posta adresi
- Supabase Auth tarafından yönetilen kimlik doğrulama bilgileri
- Kullanıcı kimliği
- Oturum ve güvenlik durumu
- E-posta doğrulama ve şifre sıfırlama kayıtları

Profitness düz metin şifre saklamaz.

### 2.2 Profil Verileri

Kullanıcının isteğine bağlı olarak aşağıdaki profil verileri işlenebilir:

- Görünen ad
- Profil fotoğrafı veya avatar
- Fitness hedefi
- Seviye, XP, seri, başarımlar ve profil ilerleme bilgileri
- Uygulama içinde görünür olabilecek sosyal/profil bilgileri

Profil fotoğrafları Supabase Storage üzerinde saklanır. Uygulama, profil fotoğrafı yükleme öncesinde dosya türü ve boyut kontrolleri uygular.

### 2.3 Antrenman ve Fitness Verileri

Uygulamanın temel işlevleri için aşağıdaki fitness verileri işlenebilir:

- Antrenman programları
- Egzersizler, setler, tekrarlar, ağırlıklar ve tamamlanma durumları
- Antrenman geçmişi
- Egzersiz performans kayıtları
- Kullanıcı istatistikleri, başarımlar, XP, seri ve gelişim hesaplamaları

Bu veriler; kullanıcının ilerlemesini göstermek, hesaplar arası senkronizasyon sağlamak, challenge/leaderboard gibi özellikleri çalıştırmak ve uygulama deneyimini kişiselleştirmek amacıyla kullanılır.

### 2.4 Challenge, Etkinlik ve Sosyal Özellik Verileri

Challenge veya etkinlik özellikleri kullanıldığında aşağıdaki veriler işlenebilir:

- Challenge üyeliği, davetler ve katılım bilgileri
- Challenge ilerleme ve sıralama verileri
- Etkinlik adı, açıklaması, tarihi ve bağlantısı
- Kullanıcının girdiği veya seçtiği başlangıç/bitiş konumu ve ilgili koordinatlar

Mevcut uygulama sürümünde Android cihaz konumu için hassas konum izni talep edilmez. Kullanıcı bir etkinlik konumu girer veya seçerse, bu bilgi etkinlik ve harita özelliklerinin çalışması için saklanabilir.

### 2.5 Yapay Zeka Özelliği Verileri

Kullanıcı yapay zeka özelliklerini kullandığında aşağıdaki veriler işlenebilir:

- Kullanıcının yazdığı istemler ve mesajlar
- Yapay zeka isteğine dahil edilen fitness hedefleri ve tercihler
- Kullanıcının bilinçli olarak yüklediği görsel veya PDF dosyaları
- Yapay zeka kullanım, kredi ve idempotency kayıtları

Yapay zeka istekleri Supabase Edge Functions üzerinden işlenir. Gemini API anahtarı sunucu tarafında saklanır ve Android uygulamasına gömülmez.

Kullanıcılar, yapay zeka özelliklerine hassas kişisel bilgi, kimlik belgesi, sağlık raporu, finansal belge veya paylaşmak istemedikleri içerikleri yüklememelidir.

### 2.6 Ödeme, Abonelik ve Kredi Verileri

Ücretli özellikler veya kredi sistemi kullanıldığında aşağıdaki veriler işlenebilir:

- Seçilen ürün veya paket bilgisi
- Sipariş kimliği ve ödeme durumu
- Hak ediş, abonelik/kredi bakiyesi ve kullanım kayıtları
- Ödeme sağlayıcı tarafından dönen işlem veya oturum kimlikleri

Profitness tam kart numarası veya hassas ödeme kartı bilgisi saklamaz. Ödeme işlemleri ilgili ödeme sağlayıcı tarafından yürütülür.

### 2.7 Teknik, Güvenlik ve İşletim Verileri

Hizmetin güvenli ve istikrarlı çalışması için aşağıdaki teknik veriler işlenebilir:

- Sunucu ve Edge Function logları
- Oturum ve istek güvenliği kayıtları
- Hata ayıklama, kötüye kullanım önleme ve tekrar saldırısı önleme kayıtları
- Kullanım limiti, kredi rezervasyonu ve idempotency kayıtları
- Güvenlik denetimi ve sistem bütünlüğü kayıtları

## 3. Verileri İşleme Amaçlarımız

Veriler aşağıdaki amaçlarla işlenir:

- Kullanıcı hesabı oluşturmak ve yönetmek
- Oturum açma, e-posta doğrulama ve şifre sıfırlama işlemlerini yürütmek
- Antrenman, program, profil, challenge ve ilerleme özelliklerini sağlamak
- Kullanıcı verilerini hesapla senkronize etmek
- Yapay zeka destekli fitness özelliklerini sunmak
- Kredi, abonelik ve ödeme durumunu doğrulamak
- Dolandırıcılık, kötüye kullanım, yetkisiz erişim ve güvenlik risklerini azaltmak
- Hataları analiz etmek, hizmet kalitesini korumak ve teknik sorunları gidermek
- Yasal yükümlülükleri ve platform gerekliliklerini yerine getirmek

## 4. Hukuki Dayanak

Geçerli mevzuata bağlı olarak kişisel veriler aşağıdaki hukuki dayanaklardan biri veya birkaçı kapsamında işlenebilir:

- Kullanıcının açık rızası veya isteğe bağlı özellikleri kullanması
- Kullanıcıya sunulan hizmetin ifası
- Güvenlik, hata giderme, kötüye kullanım önleme ve hizmet geliştirme gibi meşru menfaatler
- Yasal yükümlülüklerin yerine getirilmesi

## 5. Verilerin Paylaşıldığı Hizmet Sağlayıcılar

Profitness, uygulamanın çalışması için üçüncü taraf hizmet sağlayıcılarından yararlanabilir. Bu sağlayıcılar verileri yalnızca ilgili hizmeti sunmak için işler.

Kullanılan başlıca hizmet kategorileri:

- Supabase: kimlik doğrulama, veritabanı, dosya depolama, Edge Functions ve operasyonel loglar
- Google Maps / Places: harita, konum arama ve yer seçimi özellikleri
- Google Gemini: kullanıcının yapay zeka özelliğine gönderdiği istem ve dosyaların işlenmesi
- Ödeme sağlayıcıları: ödeme, abonelik, kredi ve hak ediş doğrulama süreçleri
- Google Play ve Android platform hizmetleri: uygulama dağıtımı ve platform güvenliği

Profitness kişisel verileri satmaz.

## 6. Veri Saklama Süreleri

Kişisel veriler, ilgili özelliğin sunulması, hesabın aktif tutulması, güvenlik ve yasal yükümlülüklerin yerine getirilmesi için gerekli olduğu sürece saklanır.

Hesap, profil, antrenman, challenge ve ödeme/kredi verileri kullanıcı hesabı aktif olduğu sürece tutulabilir. Operasyonel loglar ve güvenlik kayıtları daha kısa sürelerle saklanabilir.

Kullanıcı hesabını silmek veya verilerinin silinmesini talep etmek isterse, yasal, güvenlik, ödeme veya dolandırıcılık önleme sebepleriyle saklanması gereken kayıtlar dışında kişisel veriler silinir veya anonim hale getirilir.

## 7. Güvenlik Önlemleri

Profitness, kullanıcı verilerini korumak için teknik ve organizasyonel güvenlik önlemleri uygular. Bunlara örnek olarak:

- Supabase Row Level Security ve yetki kontrolleri
- Hassas servis anahtarlarının sunucu tarafında saklanması
- Android release signing ve üretim derleme kontrolleri
- Şifre sıfırlama için doğrulanmış Android App Links geçişi
- Billing webhook imza doğrulaması, timestamp toleransı ve tekrar saldırısı önleme
- Yapay zeka kredi/idempotency kontrolleri
- Dosya türü ve boyut kontrolleri
- URL, profil fotoğrafı ve ödeme olayları için sunucu tarafı doğrulamalar

Hiçbir sistem mutlak güvenli değildir; ancak Profitness güvenlik risklerini azaltmak ve tespit edilen sorunları sorumlu şekilde gidermek için makul önlemleri alır.

## 8. Kullanıcı Hakları

Kullanıcılar, geçerli mevzuat kapsamında aşağıdaki haklara sahip olabilir:

- Kişisel verilerine erişim talep etme
- Yanlış veya eksik verilerin düzeltilmesini isteme
- Kişisel verilerin silinmesini talep etme
- İşleme faaliyetleri hakkında bilgi isteme
- Belirli işleme faaliyetlerine itiraz etme
- Rızaya dayalı işlemlerde rızayı geri çekme

Talepler için iletişim adresi:

info@cosmibit.com

## 9. Çocukların Gizliliği

Profitness, yürürlükteki mevzuata göre dijital hizmetlere kendi başına rıza veremeyecek yaştaki çocuklara yönelik olarak tasarlanmamıştır.

Bir çocuğa ait kişisel verinin uygun veli/vası izni olmadan işlendiği öğrenilirse, ilgili verilerin silinmesi için gerekli adımlar atılır.

## 10. Uluslararası Veri İşleme

Profitness'in kullandığı altyapı ve hizmet sağlayıcıları, verileri kullanıcının bulunduğu ülke dışındaki ülkelerde işleyebilir. Bu durumda, geçerli mevzuatın gerektirdiği ölçüde uygun koruma mekanizmaları uygulanır.

## 11. Politika Değişiklikleri

Bu Gizlilik Politikası; uygulama özellikleri, veri işleme faaliyetleri, yasal gereklilikler veya hizmet sağlayıcılar değiştiğinde güncellenebilir.

Güncel politika metni, "Son güncelleme" tarihi ile birlikte yayınlanır.
