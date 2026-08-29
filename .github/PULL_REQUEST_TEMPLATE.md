## Ne değişti

<!-- Bir iki cümle. Neden gerektiğini de yazın. -->

## Kontrol listesi

- [ ] `./gradlew testDebugUnitTest` geçiyor
- [ ] `./gradlew assembleDebug` geçiyor (izin denetimi dahil)
- [ ] Davranış değiştiyse test eklendi
- [ ] Piksel/kodlayıcı gerektiren doğrulamalar `androidTest` içinde
      (Robolectric'in LEGACY grafik kipinde bu testler kod bozukken de geçer)
- [ ] Kullanıcıya görünen yeni metinler `values/strings.xml` (TR) **ve**
      `values-en/strings.xml` (EN) içine anahtarla eklendi, kodda hardcoded
      metin yok
- [ ] Yeni bağımlılık yok — varsa önce issue açıldı ve gerekçesi konuşuldu
- [ ] README / CHANGELOG gerekiyorsa güncellendi

## Değiştirilemeyecek kararlara dokunuldu mu

<!-- Aşağıdakilerden herhangi birine dokunduysanız gerekçesini yazın.
     Bkz. CONTRIBUTING.md -->

- [ ] `INTERNET` / ağ izni **eklemedim**, `IZIN_VERILEN_DESENLER` listesine
      **satır eklemedim**
- [ ] `PdfKartici` karartma mantığını (rasterize, `isAntiAlias = false`,
      JPEG blok hizalaması, meta veri temizliği) **değiştirmedim**
- [ ] `IslemGunluguDao`'ya `@Update` / `@Delete` **eklemedim**

## Test edildiği ortam

- Cihaz / emülatör:
- Android sürümü:
