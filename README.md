# nubiaSMS

Nubia / MyOS telefonlar için, sistemin kendi tasarım diliyle yazılmış SMS uygulaması.
Google Mesajlar'ın yerine **varsayılan SMS uygulaması** olarak çalışır.

- Paket: `com.nubiasms.messages`
- Ana ekran adı: **Mesajlar**
- Hedef cihaz: Nubia Z80 Ultra (NX741J), Global MyOS, Android 15+ (minSdk 26)
- Arayüz: Jetpack Compose + Material 3, MyOS'a göre yeniden ayarlanmış
- Tasarım dili: **[TASARIM.md](TASARIM.md)**

## Özellikler

- Konuşma listesi, sohbet ekranı, yeni mesaj (kişi araması ile)
- Okundu/okunmadı (varsayılan uygulama değilken bile uygulama içinde doğru görünür)
- Ortak MyOS arama sayfası: arama geçmişi, tüm mesajlarda tam metin arama
- Arşiv ve çöp kutusu (çöpteki konuşmalar **30 gün** sonra kalıcı silinir)
- **Spam** klasörü: reklam SMS'leri gelen kutusu yerine Spam'e düşer, bildirim ve rozet üretmez.
  Kod, sipariş ve kargo mesajları gelen kutusunda kalır. Gönderen bazında "Spam değil" /
  "Spam olarak bildir" (`sms/PromoDetector.kt`, `sms/SpamSenders.kt`)
- Uzun basınca toplu işlem: okundu/okunmadı, arşivle, çöp kutusu
- Kaydırma: sağa arşivle, sola çöp kutusu; her taşımada "Geri al"
- Mesaja uzun basınca: kopyala, bağlantıyı kopyala, yönlendir, yıldız, bilgi, çoklu seçim, sil
- Yıldızlı mesajlar, mesaj bilgisi (gönderildi/alındı, durum, SIM, mesaj merkezi)
- Sohbet içinde arama, Kişi ekle / Ayrıntılar (telefonun Kişiler uygulamasına yönlendirir)
- OTP algılama, mesajda kod kartı, bildirimde **"Kodu kopyala"**
- Bildirimden **Yanıtla** ve **Okundu**; ikon rozeti (okunmamış sayısı)
- Gizli **Geliştirici** bölümü: gecikmeli test SMS

## Derleme ve kurulum

Bu Mac'te varsayılan Java 27; Gradle 8.11 onunla çalışmaz. **Her zaman JDK 17** kullan:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
```

Telefonda denemek için **release** derlemesini kur. Debug Compose yavaş çalıştığı için
akıcılık testlerinde yanıltır. Release, R8 açık; `keystore.properties` yoksa debug anahtarıyla imzalı:

```bash
./gradlew :app:assembleRelease && adb install -r app/build/outputs/apk/release/app-release.apk
```

### Güncellemeler (GitHub Releases)

Public Release: `update.json` + `nubiasms-release.apk`. `local.properties`:

```
update.github.owner=SENIN_GITHUB_KULLANICI
update.github.repo=nubiaSMS
update.apk.asset=nubiasms-release.apk
```

Kalıcı imza: kökte `keystore.properties` (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). Tag `v*` → Actions Release. Her yayında `versionCode` artır. Debug→release geçişinde bir kez kaldır-kur.

Birim testleri:

```bash
./gradlew :app:testDebugUnitTest
```

İlk kurulumdan sonra uygulamayı açıp izinleri ver ve listedeki **"Varsayılan yap"** ile
varsayılan SMS uygulaması yap. Okundu eşitleme, silme ve 30 günlük çöp temizliği
ancak varsayılan uygulamayken çalışır.

## Test SMS (Geliştirici)

Ayarlar → en alttaki **"nubiaSMS x.y.z"** yazısına 7 kez dokun → **Geliştirici** →
**Test SMS gönder**. Gönderen, metin ve gecikme (Hemen / 10 sn / 30 sn / 1 dk / 5 dk) seçilir.
Mesaj, gerçek SMS ile **aynı yoldan** (`sms/IncomingSms.kt`) geçer. Uygulamayı kapatıp
bildirimi, kilit ekranını ve rozeti gerçek koşulda görebilirsin.

- Uygulamayı "Zorla durdur" ile kapatırsan planlanmış test iptal olur.
- Numaradan gelen test mesajında "Yanıtla" gerçek SMS gönderir; test ederken gönderme.
- Test mesajları gerçek gelen kutusuna düşer, iş bitince çöpe at.

## Klasör yapısı

```
app/src/main/java/com/nubiasms/messages/
  sms/            SMS sağlayıcısı, gelen/giden, arşiv/çöp, yıldız, okundu durumu
  notify/         Bildirim, rozet, bildirim düğmeleri (kopyala, yanıtla, okundu)
  otp/            Doğrulama kodu algılama
  autofill/       OTP otomatik doldurma servisi
  debug/          Test SMS
  ui/theme/       Renk, yazı, şekil token'ları
  ui/components/  MyOS bileşenleri (kart, arama, menü, seçim, snackbar, yazma çubuğu)
  ui/conversations, thread, compose, search, starred, settings
  ui/navigation/  Sayfa yolları + MyOS geçişleri
design-refs/      Telefondan alınan ölçümler, token'lar, ikon mockup'ları
```

## Kurallar

### Tasarım
1. Yeni ekran ya da bileşen yazmadan önce **[TASARIM.md](TASARIM.md)**'yi oku. Önce var olan
   `ui/components` bileşenini kullan, yenisini sonra yaz.
2. Ölçüler **telefondan ölçülür** (ekran görüntüsü, 3,25 px/dp), tahmin edilmez.
   Token ile ekran çelişirse ekran kazanır.
3. Renk **sistem paletinden** gelir. Sabit vurgu rengi yazma.
4. Material varsayılanlarını olduğu gibi bırakma: crossfade, koyu snackbar, gölgeli kart,
   kalın başlık, büyük arama kutusu MyOS'ta yok.
5. Nubia/ZTE uygulamalarından **kod, görsel, ikon kopyalanmaz**. Çin ROM ve sistem
   uygulamaları sadece referans; alınan yalnızca ölçü ve renk değeridir.
6. İkon: adaptive, zemin arka plan katmanında, nesne 66 dp güvenli alanda, monochrome katmanı var.

### Davranış
7. Geri alınabilir taşımalar (arşiv, çöp) onaysız yapılır ve **"Geri al"** sunulur.
   Geri alınamaz işlemler (kalıcı silme, mesaj silme) **her zaman onay sorar**.
8. Gelen her SMS `IncomingSms.handle()` üzerinden geçer: gerçek de test de. Bu yolu ikiye bölme.
9. Bildirimde en fazla 2 düğme.
10. Kullanıcının sistem ayarlarına uygulama dokunmaz (varsayılan SMS, otomatik doldurma,
    ekran döndürme). Sadece sistemin seçim ekranı açılır. Otomatik doldurma kullanıcının
    seçimiyle **Proton Pass** olarak kalır.
11. Sağlayıcıya yazan işler (okundu, silme) Android'de sadece varsayılan SMS uygulamasına
    açıktır. Değilse sessizce yok sayılır; bu durumda uygulama içi durum (`LocalReadState`,
    `ConversationState`) yine doğru gösterilmeli.

### Çalışma şekli
12. Her değişiklikten sonra release derle, telefona kur, **ekranda gör**. "Derleniyor" yetmez.
13. Telefonda yazılı/kalıcı etkisi olan şeyler (mesaj silme, gerçek SMS gönderme)
    test sırasında yapılmaz. Test SMS ve "Geri al" ile denenir.
14. Aynı klasörde birden fazla oturum çalışıyorsa önce hangi dosyaları alacağını söyle,
    o dosyalarda başkası varken düzenleme yapma, telefonu sırayla kullan.
15. Kod yorumları İngilizce, arayüz metinleri Türkçe.

## Lisans / not

Kişisel proje. Nubia, MyOS ve ZTE adları ilgili sahiplerine aittir; bu uygulama
onlarla bağlantılı değildir.
