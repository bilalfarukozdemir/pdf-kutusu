# Katkıda bulunma

Katkılara açığım. Bu dosya, bir PR'ın kabul edilmesi için nelerin gerektiğini
ve **nelerin tartışmaya kapalı olduğunu** anlatır.

---

## Değiştirilemeyecek kararlar

Bu projenin varlık sebebi bunlar. Bunları gevşeten PR'lar, ne kadar iyi
yazılmış olursa olsun kapatılır.

### 1. Ağ ve yetenek izni eklenmez

`app/src/main/AndroidManifest.xml` içinde hiçbir **ağ** izni **talep edilmez**
(`INTERNET`, `ACCESS_NETWORK_STATE` vb.). Oradaki satırlar `tools:node="remove"`
direktifidir — bağımlılıkların eklediği izinleri siler.

`app/build.gradle.kts` içindeki `AgIzniDenetimi` görevi, birleşmiş manifestte
yetenek veren bir izin bulursa derlemeyi durdurur ve `assembleDebug` bu
göreve bağlıdır.

Bir bağımlılık izin enjekte ediyorsa çözüm **izin listesine istisna eklemek
değildir**. Önce issue açın; ya bağımlılığı değiştiririz ya da kaldırırız.

Bu kuralın istisnaları iki ayrı listede, ayrı gerekçelerle yaşar — birbirine
karıştırılmaz:

- `IZIN_VERILEN_DESENLER`: `signature` seviyesinde, kullanıcıya hiç
  gösterilmeyen, hiçbir kaynağa erişim vermeyen izinler. Tek örneği
  androidx.core'un eklediği, uygulamanın kendi paket adıyla isimlendirilmiş
  `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.
- `BILINCLI_KABUL_EDILEN`: proje sahibinin **bilerek, gerekçesiyle** kabul
  ettiği, gerçek bir yetenek veren istisnalar. Tek örneği
  `com.android.vending.BILLING` (Google Play Billing — "bağış" özelliği
  için gerekli; internete kendi çıkmaz, cihazdaki Play Store uygulamasıyla
  IPC üzerinden konuşur, kullanıcıya izin ekranında gösterilmez). Bu liste
  "artık her yeni izin buraya eklenebilir" anlamına gelmez — her yeni giriş
  ayrı bir tartışma ve gerekçe gerektirir; varsayılan hâlâ reddir. Yasaklı
  ağ/depolama izinleri (`YASAKLI_IZINLER`) bu listeye hiçbir koşulda giremez.

### 2. Karartma yalnızca rasterize ederek yapılır

Mevcut sayfanın üstüne siyah dikdörtgen çizmek **sahte karartmadır**: metin
içerik akışında kalır ve kopyalanabilir. [`PdfKartici`](app/src/main/java/com/yerel/pdfkutusu/pdf/PdfKartici.kt)
sayfayı en az 200 DPI'da bitmap'e çevirir, piksellere opak siyah boyar, sayfayı
o bitmap'ten yeniden kurar ve meta verileri temizler.

`isAntiAlias = false`, JPEG blok hizalaması ve meta veri temizliği aynen kalır.

Zorunlu test: `KarartmaMetinYoklugTesti` + `KarartmaCihazTesti`.

### 3. İşlem günlüğü salt-eklemedir

[`IslemGunluguDao`](app/src/main/java/com/yerel/pdfkutusu/veri/IslemGunluguDao.kt)
içinde `@Update` ve `@Delete` işaretli metot **yoktur ve olmayacaktır**. Tek
istisna `tumunuTemizle()`: ya hepsi durur ya hiçbiri.

### 4. Kapsam dışı olanlar

- **Office → PDF dönüşümü.** LibreOffice Android'de çalışmaz.
- **İmza akışı** (imzacı davet etme, onay kaydı, teslimat takibi). Sunucu ve
  kimlik doğrulama gerektirir.
- **Hesap, abonelik, telemetri, analitik, bulut senkronu, reklam.**
- **AGPL lisanslı kütüphane** (MuPDF, iText).

---

## Geliştirme ortamı

| Gereksinim | Sürüm |
|---|---|
| JDK | 17 |
| Android SDK Platform | 35 |
| Build-Tools | 35.0.0 |
| Gradle | 8.10.2 (wrapper ile gelir) |

```bash
git clone https://github.com/bilalfarukozdemir/pdf-kutusu.git
cd pdf-kutusu
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

`local.properties` sürüm kontrolüne girmez; yoksa `ANDROID_HOME` kullanılır.

> İlk `testDebugUnitTest` temiz bir makinede ~5 dakika sürer: Robolectric
> `android-all-instrumented-*.jar` (~100 MB) indirir. Sonrakiler ~10 saniye.

---

## Kod tarzı

**Tanımlayıcılar ve yorumlar Türkçedir.** Bu bilinçli bir tercihtir; tutarlılık
için lütfen uyun.

```kotlin
fun sayfalariTani(kaynak: File, sayfaIndeksleri: List<Int>): OcrSonucu
```

- Kod içi tanımlayıcılarda ASCII kullanın (`sikistir`, `dondur`). Bu değişmedi.
- **Kullanıcıya görünen metin koda hardcoded yazılmaz.** Varsayılan dil
  Türkçe; uygulama Türkçe ve İngilizce'yi destekler (bkz. `LocaleManager`
  tabanlı dil seçici, "Hakkında" ekranı). Her kullanıcıya görünen metin bir
  anahtarla `app/src/main/res/values/strings.xml` (TR) içine ve karşılığıyla
  `app/src/main/res/values-en/strings.xml` (EN) içine eklenir; Composable
  içinde `stringResource(R.string.anahtar)`, Composable dışında (ViewModel,
  `pdf/*`) `context.getString(R.string.anahtar)` kullanılır.
- Yorumlar *neden*i anlatsın, *ne*yi değil. Kodun kendisi ne yaptığını söylüyor.
- 4 boşluk girinti, satır sonu virgülü (trailing comma), ~100 karakter satır.
- Yeni bir bağımlılık eklemeden önce issue açın. Her bağımlılık, izin denetiminin
  aşılması için yeni bir yüzeydir.

### Katman kuralları

- `cekirdek/` — saf Kotlin, Android'e dokunmaz.
- `pdf/` — PDF motoru, **coroutine bilmez**. Fonksiyonlar bloklayıcıdır ve bir
  `IlerlemeDinleyicisi` alır; iptal, dinleyicinin istisna fırlatmasıyla olur.
  Bu sayede JVM birim testinde doğrudan çalıştırılabilir.
- `ui/` — Compose. İş mantığı buraya sızmasın.
- Android grafik gerektiren mantığı **saf bir çekirdeğe ayırın**
  (örn. `ExifYonu`, `SayfaYerlesimi`) ki gerçek birim testi yazılabilsin.

---

## Test beklentileri

Davranış değiştiren her PR test getirmelidir.

**Birim testi (`src/test`)** — saf mantık: dosya adı temizliği, sayfa aralığı,
yerleşim matematiği, EXIF yön eşlemesi.

**Enstrümante test (`src/androidTest`)** — piksel ya da gerçek kodlayıcı
gerektiren her şey.

> Bu ayrım önemli: Robolectric'in varsayılan (LEGACY) grafik kipinde
> `Canvas.drawBitmap` boş geçer, `Bitmap.compress` yer tutucu yazar ve
> `getPixel` sıfır döner. Yön, saydamlık ve EXIF sızıntısı gibi konular birim
> testi olarak yazılırsa **kod bozukken de geçer**. Bunları `androidTest`'e
> koyun.

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest    # cihaz/emülatör gerekir
```

Bazı MIUI/HyperOS cihazlarda Gradle'ın kurulumu aralıklı olarak reddedilir.
O zaman:

```bash
adb install -r -t app/build/outputs/apk/debug/app-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.yerel.pdfkutusu.test/androidx.test.runner.AndroidJUnitRunner
```

---

## PR göndermeden önce

- [ ] `./gradlew testDebugUnitTest` geçiyor
- [ ] `./gradlew assembleDebug` geçiyor (izin denetimi dahil)
- [ ] Davranış değiştiyse test eklendi
- [ ] Kullanıcıya görünen yeni metinler `values/strings.xml` (TR) ve
      `values-en/strings.xml` (EN) içine anahtarla eklendi, kodda hardcoded
      metin bırakılmadı
- [ ] Yeni bağımlılık yoksa — varsa önce issue açıldı
- [ ] README / CHANGELOG gerekiyorsa güncellendi

---

## Hata bildirimi

Issue açarken şunları yazın: cihaz modeli, Android sürümü, uygulama sürümü,
hangi araç, ne beklediniz, ne oldu.

**Belge eklemeyin.** Bu bir gizlilik aracı; sorununuzu yeniden üreten *sentetik*
bir dosya üretip onu ekleyin.

Güvenlikle ilgili bir sorun bulduysanız [SECURITY.md](SECURITY.md) dosyasına
bakın.
