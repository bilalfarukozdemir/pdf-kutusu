# Üçüncü taraf bileşenler

PDF Kutusu MIT lisanslıdır. Aşağıdaki bileşenler kendi lisanslarını korur.

**AGPL lisanslı hiçbir bileşen kullanılmamıştır.** MuPDF ve iText bilerek
dışarıda bırakıldı; ikisi de AGPL'dir ve bu projenin lisans modeliyle
bağdaşmaz.

---

## APK ile birlikte dağıtılan varlıklar

### Noto Sans (statik sürüm)

```
app/src/main/assets/fonts/NotoSans-Regular.ttf   621.572 bayt
app/src/main/assets/fonts/OFL.txt                lisans metni
```

- Copyright The Noto Project Authors
- **SIL Open Font License, Version 1.1**
- Kaynak: <https://github.com/notofonts/notofonts.github.io> —
  `fonts/NotoSans/hinted/ttf/NotoSans-Regular.ttf`

Filigran metninde Türkçe karakterlerin (ğ, ş, ı, İ, Ğ, Ş) her cihazda doğru
çıkması için paketlendi. PDF'e yalnızca kullanılan harfler gömülür.

OFL 1.1 gereği: yazı tipi dosyası değiştirilmedi ve "Noto" adıyla türetilmiş
bir yazı tipi dağıtılmıyor.

---

## Kod bağımlılıkları

| Bileşen | Sürüm | Lisans |
|---|---|---|
| [PdfBox-Android](https://github.com/TomRoush/PdfBox-Android) | 2.0.27.0 | Apache License 2.0 |
| [ML Kit Text Recognition v2](https://developers.google.com/ml-kit/vision/text-recognition/v2) (paketli model) | 16.0.1 | Apache License 2.0 |
| AndroidX Core, Lifecycle, Activity, Navigation | — | Apache License 2.0 |
| Jetpack Compose + Material 3 | BOM 2024.12.01 | Apache License 2.0 |
| AndroidX Room | 2.6.1 | Apache License 2.0 |
| AndroidX DocumentFile | 1.0.1 | Apache License 2.0 |
| AndroidX ExifInterface | 1.3.7 | Apache License 2.0 |
| Kotlin stdlib + Coroutines | 2.0.21 / 1.9.0 | Apache License 2.0 |

### Yalnızca test

| Bileşen | Sürüm | Lisans |
|---|---|---|
| JUnit 4 | 4.13.2 | Eclipse Public License 1.0 |
| Robolectric | 4.13 | Apache License 2.0 |
| AndroidX Test, Espresso | — | Apache License 2.0 |

Test bağımlılıkları APK'ya girmez.

---

## Platform bileşenleri

`android.graphics.pdf.PdfRenderer` — sayfa önizleme ve karartma için kullanılan
rasterleştirici, Android platformunun parçasıdır (AOSP, Apache License 2.0).
Ayrı bir bağımlılık eklenmemiştir.

---

## Lisans metinlerinin tamamı

- Apache License 2.0: <https://www.apache.org/licenses/LICENSE-2.0>
- SIL Open Font License 1.1: `app/src/main/assets/fonts/OFL.txt`
- Eclipse Public License 1.0: <https://www.eclipse.org/legal/epl-v10.html>
