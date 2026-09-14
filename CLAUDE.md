# PDF Kutusu

Tamamen çevrimdışı çalışan Android PDF okuyucu ve araç kutusu. **Açık kaynak (MIT)**,
APK sürümleri GitHub Releases'ta yayında.

Kotlin · Gradle (`build.gradle.kts`) · minSdk 26

Ayrıntı `README.md`'de — kod yazmadan önce oku, burada tekrarlamıyorum.

## En önemli kural: uygulamanın internet izni YOK

Bu bir tercih değil, **projenin varlık sebebi**. Kullanıcıya verilen söz şu:
hiçbir dosya cihazdan çıkmaz. Ve bu söz derleme zamanında doğrulanıyor.

Bunun pratik sonucu:

- **`INTERNET` iznini manifest'e ekleme.** Hiçbir gerekçeyle.
- **Ağ erişimi gerektiren kütüphane ekleme.** Analitik, çökme raporu, uzak OCR
  servisi, font indirme, güncelleme kontrolü — hepsi yasak.
- Bir kütüphane eklemen gerekiyorsa önce onun geçişli bağımlılıklarının ağ
  kullanıp kullanmadığını kontrol et, sonra sor.

OCR dahil bütün işlem cihaz üzerinde yapılır.

## Diğer kararlar

- Varsayılan (yedek) dil **İngilizce**; uygulama Play Store'a küresel açılma
  kararı gereğince İngilizce + Türkçe'yi destekler (sistem diline göre otomatik +
  "Hakkında" ekranındaki isteğe bağlı manuel seçici, Android 13+
  `LocaleManager`). Telefonun dili için çeviri yoksa uygulama İngilizce açılır.
  Kullanıcıya görünen metin koda hardcoded yazılmaz; `values/strings.xml` (EN,
  varsayılan) ve `values-tr/strings.xml` (TR) içine anahtarla eklenir —
  ayrıntı `CONTRIBUTING.md`'de.
- AAB dil bölmesi kapalı (`bundle { language { enableSplit = false } }`).
  Açılırsa Play telefona yalnızca telefonun kendi dillerini kurar ve manuel
  dil seçici Play kurulumunda sessizce çalışmaz olur — açma.
- Tema sistem ayarını izler, renkler duvar kâğıdından türetilir (Material You)
- `CHANGELOG.md` sürüm başına güncellenir, `surum/` klasörü sürüm varlıklarını tutar

## Doğrulama

CI `.github/workflows/derleme.yml` üzerinden derleme ve testleri koşuyor.
Değişiklikten sonra yerelde de derle — imza ve izin kısıtları derleme zamanında
kontrol ediliyor, testte değil.

## Açık kaynak

Depo herkese açık, commit geçmişi kalıcı. İmzalama anahtarı, parola veya kişisel
dosya commit'e girmemeli. Katkı kuralları `CONTRIBUTING.md`'de.
