# Güvenlik

## Tehdit modeli — ne vaat ediliyor, ne edilmiyor

### Vaat edilen

**Dosyalarınız cihazdan çıkmaz.** Uygulamanın `INTERNET` izni yoktur. Bu bir söz
değil, üç katmanlı bir kısıttır:

1. Manifest, bağımlılıkların eklediği ağ izinlerini `tools:node="remove"` ile siler.
2. `AgIzniDenetimi` Gradle görevi, birleşmiş manifestte yetenek veren tek bir
   izin kalırsa **derlemeyi durdurur**; `assembleDebug` ve `packageDebug` bu
   göreve bağlıdır, atlanamaz.
3. CI her push'ta üretilen APK'yı `aapt dump permissions` ile denetler.

Kendiniz doğrulayabilirsiniz:

```bash
aapt dump permissions app/build/outputs/apk/debug/app-debug.apk
```

Çıktıda `android.permission.*` ile başlayan **hiçbir satır olmamalıdır**. Tek
görünen satır `com.yerel.pdfkutusu.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`
olacaktır: androidx.core'un eklediği, uygulamanın kendi paket adıyla
isimlendirilmiş `signature` seviyesinde bir izindir, kullanıcıya gösterilmez ve
hiçbir sistem kaynağına erişim vermez.

**Karartma gerçektir.** Karartılan sayfa rasterize edilir; metin PDF'in içerik
akışından tamamen kalkar. Sahte karartma (üstüne dikdörtgen çizme)
desteklenmez ve testle engellenir.

**Görsellerin EXIF verisi sızmaz.** Resimden PDF, görselleri yeniden kodlar;
GPS koordinatı, cihaz modeli ve çekim tarihi çıktıya geçmez. Bu, ham PDF
baytlarında arama yapan bir cihaz testiyle doğrulanır.

**Orijinal dosyanız değiştirilmez.** Kaynak URI'ye asla yazılmaz.

### Vaat EDİLMEYEN

- **Bu bir adli/kurumsal karartma aracı değildir.** README'deki düşük riskli
  kullanım uyarısı geçerlidir: resmî, hukuki veya regüle belgeler için tek
  başına güvenmeyin. Çıktıyı her zaman açıp kontrol edin.
- **Cihazınız ele geçirilmişse koruma sağlamaz.** Root erişimi olan bir saldırgan
  uygulamaya özel dizini okuyabilir.
- **Şifreli PDF'in parolası korunmaz.** Parola girip açtığınızda çalışma kopyası
  şifresiz hâle gelir ve **çıktı şifresizdir**. Parola hiçbir yere kaydedilmez,
  ama çıktının kendisini siz korumalısınız.
- **Karartılan sayfa görüntüye çevrilir.** OCR uygulanabilir; karartılan alan
  siyah olduğu için okunamaz, ama sayfanın geri kalanı metin olarak yeniden
  çıkarılabilir. Bu beklenen davranıştır.
- **Debug APK dağıtımı için güvence verilmez.** Debug derlemeler
  `android:debuggable="true"` taşır; ADB erişimi olan biri uygulama verisini
  inceleyebilir. Günlük kullanım için release derlemesi yapın.

---

## Açık bildirin

Bu kişisel ölçekli, açık kaynak bir araçtır; gizli güvenlik süreci yoktur.

**Bulduğunuz sorunu doğrudan issue olarak açın.** Böylece herkes aynı anda
görür ve düzeltir.

Yalnızca şu durumda önce özel iletişim kurun: kullanıcının dosyalarını
sızdıran, henüz düzeltilmemiş somut bir açık bulduysanız. O zaman
[GitHub Security Advisory](https://github.com/bilalfarukozdemir/pdf-kutusu/security/advisories/new)
üzerinden bildirin.

### Bildirirken

- Cihaz modeli, Android sürümü, uygulama sürümü
- Yeniden üretme adımları
- **Gerçek belgenizi eklemeyin.** Sorunu yeniden üreten sentetik bir dosya üretin.

---

## Kapsam

| Kapsamda | Kapsam dışı |
|---|---|
| Ağ izninin bir yolla geri gelmesi | Root'lu cihazda veri erişimi |
| Karartmanın metni gerçekten kaldırmaması | Kullanıcının çıktıyı kontrol etmemesi |
| EXIF / meta verinin çıktıya sızması | Üçüncü taraf kütüphanelerin kendi CVE'leri (yukarı akışa bildirin) |
| Kaynak dosyanın değiştirilmesi | Fiziksel cihaz erişimi |
| Uygulama alanı dışına yazma | Debug derlemesinin `debuggable` olması (bilinen) |

---

## Desteklenen sürümler

Proje tek geliştiricili ve kişisel ölçektedir. Yalnızca `main` dalının son hâli
desteklenir; eski sürümlere geri yama yapılmaz.
