# Değişiklik günlüğü

Biçim [Keep a Changelog](https://keepachangelog.com/tr/1.1.0/) esas alınmıştır.
Sürümleme [Semantic Versioning](https://semver.org/lang/tr/) izler.

## [Yayımlanmamış]

### Eklendi

- **Son açılanlar şeridi.** Ana ekranda, açtığınız son 20 PDF. Dokununca
  görüntüleyicide açılır. Yanındaki **PDF aç** düğmesi uygulamanın içinden
  belge seçmenin yolu.

  Yeni izin gerektirmez. Dosya seçicide seçtiğiniz belge için sisteme
  *belgeye özel* kalıcı okuma yetkisi alınır (`takePersistableUriPermission`);
  bu bir uygulama izni değildir, depolamaya genel erişim vermez. Cihazda
  doğrulandı: uygulama tamamen kapatılıp yeniden açıldıktan sonra da belge
  listeden açılabiliyor.

  Başka bir uygulamadan (e-posta, mesajlaşma) gelen belgede yetki geçicidir
  ve kalıcılaştırılamaz — gönderen uygulama o bayrağı vermez. Bu kayıtlar
  listede **"geçici erişim"** olarak işaretlenir, kullanıcı dokunup hata
  almadan önce bilir.

  Liste silinebilir: tek tek (✕) ve topluca. Bu bilerek işlem günlüğünün
  tersi bir kural: günlük değiştirilemez, çünkü ne yapıldığının kaydıdır;
  son açılanlar ise hangi belgeleri okuduğunuzu gösterir ve mahremiyet
  alanına girer. Ayrı bir depoda tutulur, günlük tablosuna dokunulmaz.

- **Üretilen dosyaya dokununca görüntüleyicide açılıyor.** Dosyalar
  ekranındaki PDF kartları artık tıklanabilir; çıktıyı kontrol etmek için
  dışa aktarıp başka uygulamada açmak gerekmiyor.

## [1.1.0] — 2026-08-14

### Eklendi

- **PDF okuyucu.** Uygulama artık `ACTION_VIEW` ve `ACTION_SEND` ile gelen
  PDF'leri açıyor; "birlikte aç" listesinde çıkıyor ve varsayılan okuyucu
  yapılabiliyor. Sürekli dikey okuma, parmakla yakınlaştırma, sayfa göstergesi,
  okurken ekranın sönmemesi. Okuyucudan doğrudan paylaşma ve araçlara devretme.
  Yeni izin gerektirmiyor.

  Akıcılık: yakınlaştırma görüntüyü ölçeklemek yerine sayfayı o çözünürlükte
  yeniden çiziyor (metin net kalıyor); her sayfanın ucuz bir sürümü önbellekte
  tutuluyor, böylece çizim hiçbir zaman beklemiyor; yakınlaştırma yalnızca iki
  parmak ekrandayken olayları tüketiyor, tek parmak kaydırması listeye
  dokunulmadan gidiyor. Cihazda ölçülen: sayfa başına 13 ms (1080 px),
  önbellekten okuma çağrı başına 0,025 ms.

  Dayanıklılık: bozuk/boş/PDF olmayan dosya, şifreli belge, aranabilir olmayan
  dosya tanımlayıcısı, geri çekilmiş URI izni ve bellek yetersizliği ayrı ayrı
  ele alınıyor ve cihaz testleriyle doğrulanıyor.

  Sayfa göstergesine dokunup numara yazarak istenen sayfaya gidilebiliyor;
  aralık dışı numara için "Git" pasif kalır.

  Kaydırma ve yakınlaştırma elde yazıldı. Sayfa konumları mutlak piksel olarak
  tutuluyor; sayfalar kendi `Layout`umuzda ölçülüp yerleştiriliyor. Hazır
  kapsayıcılar yakınlaştırılmış (görünümden geniş) sayfayı ya ortalıyor ya da
  dokunma alanını görünümden koparıyordu — ikisi de cihazda ölçülüp
  belgelendirildi. Yerleşim aritmetiği ayrı bir sınıfa alındı ve 24 birim
  testiyle sabitlendi: yakınlaştırma odağının ekranda sabit kalması, küçük
  adımların sapma biriktirmemesi ve kaydırma sınırının ölçekle birlikte
  büyümesi test edilerek doğrulanıyor.

  Savurma (fling) animasyonu artık yeni bir parmak hareketi ya da ölçek
  değişikliğinde durduruluyor. Önceden animasyon piksel cinsinden yörüngesini
  yazmaya devam ettiği için, kaydırma bitmeden yakınlaştırmaya başlayan
  kullanıcı belgede sayfalarca sürükleniyordu (cihazda ölçüldü: bırakılan
  yerin 2,5 katı ileri).

- **Çıktıyı paylaşma.** Sonuç kartından ve Dosyalar ekranından dosya doğrudan
  başka bir uygulamaya gönderilebiliyor (`FileProvider`, geçici `content://`
  okuma izni). Yeni izin gerektirmez; paylaşıma yalnızca `cikti/` klasörü açılır.
- **Çıktı adını düzenleme.** Kaydetmeden ya da paylaşmadan önce dosya adı sonuç
  kartında değiştirilebiliyor. Uzantı korunur, ad temizlenir, çakışma uyarısı
  verilir; değişiklik diske de yansır.
- Yapımcı bilgisi (vitrincim.com) Hakkında ekranında ve ana ekranın altında;
  dokunulabilir bağlantı olarak. Tarayıcıyı açar, veri göndermez.

### Değişti

- **R8 küçültmesi açıldı: 31,9 MB → 21,0 MB.** Küçültülmüş APK cihazda
  doğrulandı; 28 enstrümante testin tamamı bu derlemeye karşı geçiyor.
  `-PkucultR8=false` ile kapatılabilir.
- Release derlemesi `keystore.properties` varsa onunla, yoksa debug anahtarıyla
  imzalanıyor. `testBuildType` Gradle özelliğiyle seçilebiliyor, böylece
  enstrümante testler release derlemesine karşı da koşabiliyor.

**Testler**

- 119 birim testi (Robolectric + saf JVM)
- 28 enstrümante test, gerçek cihazda — hem debug hem de yayımlanan
  küçültülmüş (R8) sürüm derlemesine karşı
- Okuyucunun kaydırma/yakınlaştırma aritmetiği ayrı ayrı test ediliyor;
  testlerin hatayı gerçekten yakaladığı, eski hatalı formül geri konularak
  doğrulandı

## [1.0.0] — 2026-08-14

İlk sürüm. Tamamen çevrimdışı çalışan kişisel PDF araç kutusu.

### Eklendi

**Araçlar**

- **Resimden PDF** — birden çok görseli tek PDF'te topla. EXIF yönü uygulanır,
  EXIF verisi (GPS, cihaz modeli, çekim tarihi) çıktıya geçmez, saydam PNG
  beyaza düzleştirilir. A4'e sığdır / görüntü boyutu düzenleri, sürükle-bırak
  sıralama, kalite seçimi ve tahmini boyut.
- **Birleştir** — birden fazla PDF'i sırayla tek dosyada topla.
- **Böl** — sayfa aralığı seçerek ayır; her aralığı ayrı dosyaya çıkarma seçeneği.
- **Sırala** — sayfaları sürükle-bırak ile yeniden diz, sayfa çıkar.
- **Döndür** — 90/180/270°, tüm sayfalar ya da seçili aralık.
- **Sıkıştır** — gömülü görselleri yeniden örnekle ve yeniden kodla; üç kalite
  düzeyi ve tahmini boyut.
- **Filigran** — çapraz ya da döşeli metin filigranı; punto, saydamlık, açı, renk.
- **Karart** — sayfayı ≥200 DPI rasterize edip seçilen alanları piksellere opak
  siyah boyar. Metin PDF'in içerik akışından gerçekten kalkar.
- **OCR** — ML Kit'in paketli Latin modeliyle cihaz üstü metin tanıma; panoya
  kopyalama ve `.txt` kaydetme.
- Sayfa önizleme, Android'in yerleşik `PdfRenderer` motoruyla (ek kütüphane yok).

**Altyapı**

- Sıfır izin. `AgIzniDenetimi` Gradle görevi, birleşmiş manifestte yetenek veren
  bir izin kalırsa derlemeyi durdurur; `assembleDebug` bu göreve bağlıdır.
- Salt-ekleme işlem günlüğü (Room). DAO'da `@Update`/`@Delete` yok.
- Storage Access Framework ile dosya seçme ve dışa aktarma; kaynak dosyaya asla
  yazılmaz.
- Çıktı adlandırma: `<orijinal-ad>__<islem>__<yyyyMMdd-HHmmss>.pdf`. Dosya adı
  temizleyicisi yol gezinmesini ve kontrol karakterlerini eler, **Türkçe
  karakterleri korur**.
- Paketli Noto Sans (statik, OFL 1.1); filigranda yalnızca kullanılan harfler
  gömülür. Kodlanamayan karakter (emoji, CJK) işlemi çökertmez, değiştirilir.
- Şifreli PDF'lerde parola akışı; çıktı şifresiz üretilir ve bu açıkça söylenir.
- İşlem öncesi risk uyarıları: form alanı, imza, şifreleme, gömülü olmayan yazı tipi.
- Compose + Material 3, sistem ayarını izleyen açık/koyu tema, dinamik renk.
- Her ekranda boş / yükleniyor / başarılı / kurtarılabilir hata durumları;
  uzun işlemlerde ilerleme ve iptal.
- İlk açılışta düşük riskli kullanım uyarısı.

**Testler**

- 89 birim testi (Robolectric + saf JVM)
- 10 enstrümante test, gerçek cihazda
- Zorunlu karartma doğrulaması hem birim hem cihaz testinde
- EXIF sızıntısı, EXIF dönüşü ve saydam PNG doğrulamaları piksel düzeyinde

[Yayımlanmamış]: https://github.com/bilalfarukozdemir/pdf-kutusu/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/bilalfarukozdemir/pdf-kutusu/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/bilalfarukozdemir/pdf-kutusu/releases/tag/v1.0.0
