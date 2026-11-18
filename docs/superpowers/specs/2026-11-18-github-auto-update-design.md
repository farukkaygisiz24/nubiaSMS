# GitHub otomatik güncelleme — tasarım

**Tarih:** 2026-11-18  
**Kapsam:** `nubiaPhone` (`com.nubiaphone.dialer`) + `nubiaSMS` / aramalr (`com.nubiasms.messages`)  
**Durum:** onay bekliyor

## Intent

Sen ve aynı Nubia modelini kullanan arkadaşın uygulamaları sideload ile kullanıyorsunuz. Sen sürüm yayınlayınca arkadaşın telefonunda da güncelleme görünsün; elle APK göndermek / `adb` istemiyoruz. Yayın kanalı **GitHub Releases**.

## Goals

- Her iki uygulamada da GitHub Release üzerinden yeni APK’yı keşfet, indir, sistem kurulum ekranına ver.
- Güncelleme çıkınca ekranda kısa uyarı: **“Yeni güncelleme mevcut”** + **Güncelle** (changelog / uzun detay yok).
- Kullanıcı **2 veya daha fazla sürüm geride** kalırsa güncelleme **ısrarlı** olsun (sık hatırlatma, kolay kaçış yok) ama **uygulamayı kilitlemesin** — arama ve SMS her zaman kullanılabilir kalsın.
- 1 sürüm gerideyken uyarı gösterilsin ama şimdilik kapatılabilir / sonra denebilir.
- Sen tag/Release ile yayınlayınca her iki telefonda da aynı mekanizma çalışsın.
- Release imzası sabit olsun; üstüne yazarak güncelleme mümkün olsun.

## Non-goals

- Google Play / Play Core in-app update.
- Root / system-app sessiz kurulum (Android izin vermez).
- OTA firmware / MyOS sistem güncellemesi taklidi (sadece uygulama güncellemesi; dil olarak “sistem güncellemesi gibi” his).
- Üçüncü kişi mağaza, ücretli dağıtım, analytics.
- Ortak bir Maven/AAR kütüphanesi yayınlamak (v1’de şart değil).

## Anti-goals

- Her release’te imza anahtarını değiştirmek (arkadaşın güncelleyemez).
- Private repo + her telefona token gömmek (bakım ve sızıntı riski).
- Güncelleme yüzünden telefon veya SMS’i kullanılamaz hale getirmek (anti-goal’un özü: bu uygulamalar varsayılan arama/SMS olabilir).
- Güncelleme kontrolünün arama / SMS kritik yolunu bozması (gelen arama, mesaj alma, tuş takımı, konuşma listesi).

## Constraints

- Sideload: `REQUEST_INSTALL_PACKAGES` + kullanıcıya bir kez “bilinmeyen kaynak / bu uygulamadan yükle” izni.
- Bugün her iki APK **debug keystore** ile imzalı. İlk “güncellenebilir” public release’ten önce **kalıcı release keystore** şart; debug’tan release’e geçişte arkadaşın bir kez kaldırıp yeniden kurması gerekebilir (veya debug key ile devam edip keystore’u yedeklemek — builder çağrısı aşağıda).
- `nubiaPhone` henüz git remote yok; `aramalr`’da da GitHub remote yok. Public repo(lar) oluşturulacak.
- JDK 17 ile release derleme (mevcut README kuralı).
- TASARIM.md / MyOS dili: güncelleme UI’si mevcut kart / dialog diline uysun; yeni “AI purple” tema yok.

## Approaches considered

| Yol | Özet | Trade-off |
|---|---|---|
| **1. GitHub Releases + APK** (seçildi) | Tag → Release → APK asset; uygulama API ile bakar | Public repo gerekir; imza disiplini şart |
| 2. `latest.json` + raw APK | Elle JSON | Unutulur; “kaç sürüm geride” daha dağınık |
| 3. Hazır AppUpdater lib | Hızlı iskelet | 2-sürüm zorunlu kuralı + MyOS UI yine custom |

**Seçim:** 1 — partner kararı (2026-11-18).

## Product behavior

### Sürüm geriliği

- Kaynak: GitHub’daki **en son Release** metadata’sındaki `versionCode` vs yereldeki `PackageInfo.versionCode`.
- `lag = latestVersionCode - installedVersionCode`
  - `lag <= 0` → bir şey gösterme
  - `lag == 1` → **soft**: “Yeni güncelleme mevcut” + Güncelle; **Sonra** ile kapatılır
  - `lag >= 2` → **ısrarlı (ama kilit yok)**: aynı metin + Güncelle; “Sonra” yok veya çok gömülü değil — dialog/banner her ana ekran açılışında tekrar çıkar, fakat **altındaki arama/SMS UI her zaman kullanılabilir**. Tam ekran blok / `UpdateGate` kilidi yok.

**Partner kararı (2026-11-18):** Telefon ve SMS varsayılan uygulama olabileceği için güncelleme yüzünden uygulamayı kullanılamaz kilitlemek istemiyoruz. “2 sürüm geride → mecbur hissettir” = ısrarlı hatırlatma + Güncelle ile indirme; uygulama kilidi değil.

### Ne zaman kontrol

- Uygulama açılışında (cold start / ana aktivite görünür olunca), ağ varsa.
- Soft’ta “Sonra” → aynı oturumda tekrar gösterme; sonraki açılışta soft yine.
- Israrlı (`lag >= 2`): her ana ekran / aktivite ön plana gelince tekrar göster; kullanıcı dialog’u kapatırsa (sistem geri veya dışarı dokunma — builder: kapatılabilir overlay) arama/SMS’e devam eder; bir sonraki ön plana gelişte yine çıkar.
- Ayarlar’da manuel “Güncellemeleri kontrol et” satırı (her iki uygulama).
- Arka plan: v1’de WorkManager zorunlu değil; açılış + manuel yeterli. (Builder isterse günde 1 sessiz kontrol ekleyebilir.)

### İndirme / kurulum

1. Kullanıcı **Güncelle** → APK indir (progress göster, basit yüzde veya indeterminate).
2. İndirme bitince `FileProvider` + paket kurulum Intent’i (`ACTION_VIEW` / `PackageInstaller` session — builder seçer; hedef API 35 uyumlu olsun).
3. Sistem kurulum UI’si açılır; kullanıcı onaylar.
4. Kurulum sonrası uygulama yeniden açılır; `lag` yeniden hesaplanır.

### Hata durumları

- Ağ yok / GitHub down: sessizce geç (ne soft ne ısrarlı zorla). Son başarılı kontrolde `lag >= 2` biliniyorsa, online olunca ısrarlı diyaloğu tekrar göster; offline’da uygulamayı kilitleme.
- İndirme bozulursa: tekrar dene; uygulama kullanılabilir kalsın.
- İmza uyuşmazlığı / paket çakışması: kısa hata; Ayarlar/About’ta sürüm + repo linki yeter.

### UI taslağı (metin)

```
┌─────────────────────────────┐
│  Yeni güncelleme mevcut     │
│                             │
│  [ Güncelle ]               │
│  [ Sonra ]     ← yalnız soft│
└─────────────────────────────┘
```

Israrlıda “Sonra” yok; dialog kapatılsa bile uygulama çalışır, bir sonraki açılışta yine çıkar. Changelog yok.

## Yayın modeli (GitHub)

### Repolar

- Ayrı public repo’lar (öneri): örn. `nubiaPhone` ve `nubiaSMS` (veya mevcut `aramalr` adı).
- Her uygulama kendi `owner/repo` sabitini `BuildConfig` / kaynak sabiti olarak taşır.

### Release içeriği

Her GitHub Release şunları içerir:

1. **APK asset** — sabit ad: `app-release.apk` (veya `nubiaphone-release.apk` / `nubiasms-release.apk`; repo başına bir sabit).
2. **`update.json` asset** — uygulama bunu parse eder:

```json
{
  "versionCode": 4,
  "versionName": "0.1.3"
}
```

- Tag önerisi: `v0.1.3` (`versionName` ile hizalı). Asıl karşılaştırma **`versionCode`**.
- Release notları opsiyonel; uygulama göstermez.

### Nasıl yayınlanır

- Tercih: GitHub Actions — `v*` tag push → JDK 17 release build → imzala → Release oluştur → APK + `update.json` yükle.
- Elle `gh release create` de kabul; Actions bir kez kurulunca asıl yol o olsun.

### API

- Public repo: unauthenticated `GET /repos/{owner}/{repo}/releases/latest` (+ asset indir).
- Rate limit için User-Agent ve makul cache (son başarılı cevabı kısa süre tut) — builder detayı.
- `update.json` browser_download_url üzerinden indirilir.

## Uygulama mimarisi (her iki app)

Aynı mantık, paket adına göre kopya (v1). İleride ortak modül opsiyonel.

Önerilen parçalar (isimler builder’a bırakılabilir):

- `update/UpdateConfig` — owner, repo, asset adları
- `update/UpdateChecker` — latest `update.json` + lag
- `update/UpdateDownloader` — APK’yı cache dir’e
- `update/UpdateInstaller` — install intent
- `update/UpdatePrompt` / Compose dialog veya banner — soft vs ısrarlı (kilitsiz)
- Manifest: `REQUEST_INSTALL_PACKAGES`, `FileProvider`

Ana aktivite üzerinde non-blocking prompt. Hiçbir ekranı (`MainActivity`, `InCallActivity`, sohbet) güncelleme yüzünden etkileşimsiz yapma. Gelen arama / InCall / SMS receiver her zaman çalışır.

## İmza (builder call — sabitlenmiş öneri)

- Her uygulama için **kalıcı** release keystore (gitignore + güvenli yedek).
- GitHub Actions’ta secret olarak store/password/alias.
- Mevcut telefona kurulu debug-imzalı APK’lar, ilk release-imzalı sürümde **üzerine yazılmaz**. Geçiş: bir kez kaldır → release APK kur (arkadaşa da aynı). Bundan sonra güncellemeler sorunsuz.
- Debug imzasıyla “güncelleme kanalı” açmak önerilmez (CI ve arkadaş dağıtımı kırılgan).

## İlk kurulum (arkadaş)

- Public Release sayfasındaki APK, veya senin gönderdiğin ilk link.
- “Bu kaynaktan uygulama yükle” izni.
- Sonrası otomatik kanal.

## What’s left to the builder

- Exact class/file layout, OkHttp vs HttpURLConnection, PackageInstaller vs ACTION_VIEW.
- WorkManager eklenip eklenmeyeceği (v1 opsiyonel).
- Repo adları / GitHub org kullanıcı adı (partner hesabı).
- Actions workflow YAML detayı.
- MyOS dialog vs full-screen card seçimi (TASARIM.md’ye uyum).
- `versionCode` bump disiplinini README’ye yazmak.

## Out of scope follow-ups

- Delta/patch OTA.
- Çok kanallı (beta/stable).
- Ortak update SDK repo’su.
