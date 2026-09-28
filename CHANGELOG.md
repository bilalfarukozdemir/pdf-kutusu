# Değişiklik günlüğü

Biçim [Keep a Changelog](https://keepachangelog.com/tr/1.1.0/) esas alınmıştır.
Sürümleme [Semantic Versioning](https://semver.org/lang/tr/) izler.

## [Yayımlanmamış]

### Değişti

- **"Geliştiriciyi Destekle" bağış modeli, "öncelikli inceleme hizmeti"
  modeline bıraktı.** Play ürünleri `destek_kahve` / `destek_ogun` /
  `destek_comert` (bir kahve, bir öğün, cömert destek) yerine
  `priority_request_review`, `priority_pr_review`, `priority_review_bundle`
  oldu: özellik isteği incelemesi, açık kaynak PR incelemesi ve ikisini birden
  içeren paket. Arayüzdeki üç seçeneğin adı da buna göre değişti. Sürüm
  1.3.3'e (vc7) çıkarıldı; **henüz Play'e gönderilmedi.**

- **Satın alma tek başına bir sonuç olmaktan çıktı; artık bir talep
  kaydının başlangıcı.** Satın alma Play tarafından onaylanınca önce cihazda
  kalıcı bir talebe çevriliyor, ancak ondan sonra tüketiliyor. Tüketilen
  para; kalıcı kalan hizmet hakkı.

  - **Talep kodu satın alma jetonundan türetiliyor.** `purchaseToken`
    SHA-256 ile özetleniyor, ilk 6 baytı 12 haneli büyük harfli onaltılık
    kod olarak kullanılıyor. Jetonun kendisi hiçbir yere yazılmıyor ve
    ekranda da gösterilmiyor; aynı satın alma için kod her seferinde aynı
    çıkıyor. E-postada gönderilen şey bu kod, jeton değil.
  - **Kayıt cihazda tutuluyor.** `destek_talep_kayitlari` paylaşılan
    tercihlerinde `urunId:referans` çiftleri olarak saklanıyor. Yazma
    senkron `commit()` ile yapılıyor: kayıt tutulamazsa satın alma tüketilmiyor
    ve akış hata durumunda kalıyor.
  - **E-postayı kullanıcı gönderiyor.** Bekleyen talep için "E-posta
    taslağını aç" düğmesi, seçilen hizmete göre hazır konu ve gövdeli bir
    taslağı `ACTION_SENDTO` / `mailto` niyetiyle kullanıcının kendi e-posta
    uygulamasında açıyor. Uygulama e-posta göndermez ve `INTERNET` izni yoktur.
    Cihazda e-posta uygulaması yoksa ekranda geliştirici adresi ve talep kodu
    yazılı bir uyarı çıkıyor. Taslak kapatılırsa talep listede kalıyor —
    kayıt, ancak "E-postayı gönderdim" düğmesine basılınca siliniyor.

- **`Tesekkur` durumu kaldırıldı.** Satın alma sonrası çıkan "Teşekkür
  ederiz" ekranı artık yok; akış bekleme listesine, yani alınan hizmete geri
  dönüyor. Satın alınan hizmet zaten ekranda göründüğü için ayrı bir teşekkür
  ekranı gereksizdi.

- **"Mağaza yok" kalıcı bir uyarı olmaktan çıktı, geçici bir duruma
  dönüştü.** Play bağlantısı kurulamaz veya ürünler sorgulanamazsa ekran
  "destek seçenekleri alınamadı" diyor ve **Tekrar dene** düğmesi sunuyor.
  Önceden bu, cihazda Play Store bulunmadığı anlamına geliyordu. Bekleyen
  talep varsa yeni ürün listesi yüklenmese bile talep tamamlanabiliyor.

- **Bağlanma artık tekrar çağrılabilir.** Kurulmuş bir Billing bağlantısı
  doğrudan ürün sorgusuna düşüyor, hâlâ kurulmakta olan bağlantı ikinci bir
  bağlantı başlatmıyor; bağlantı koparsa saklanan talepler korunuyor. Ürün
  sorgusu sırasında daha önce tüketilmemiş satın almalar da geriye dönük
  talebe çevriliyor; yarım kalan bir akış uygulama kapansa da kaybolmuyor.

- **Dokümanlar yeni modele göre düzeltildi.** `PRIVACY.md` (e-posta
  taslağının kullanıcı kontrollü dış aktarımı), `README.md` (hizmet tanımı,
  fiyat hedefleri, ürünlerin Play Console'da hâlâ taslak olduğu bilgisi),
  `CONTRIBUTING.md` (Billing izninin gerekçesi) ve "Hakkında" ekranının izinler
  ile "bilerek yapılmayanlar" metinleri.

### Düzeltildi

- **Enstrümante testler derlenmiyordu.** `BagisAkisiCihazTesti`, sealed
  interface'ten kaldırılan `BagisDurumu.Tesekkur` durumuna ve silinen
  `hakkinda_bagis_kahve` / `hakkinda_bagis_tesekkur` metinlerine referans
  veriyordu. Bu bir derleme hatasıydı ama CI onu yakalamıyordu: hata yalnızca
  `connectedAndroidTest` çalıştırıldığında ortaya çıkıyor ve CI'da cihaz
  yok. Testler yeni modele göre düzeltildi — ürün kimlikleri
  `URUN_KIMLIKLERI` ile aynı yapıldı, `Tesekkur` testinin yerine bekleyen
  talep akışını doğrulayan blok geldi (talep kodunun görünmesi, taslak
  açılması, "E-postayı gönderdim" ile kaydın silinmesi). Ayrıca ürün kimliği →
  yerel ad eşitliği denetimi eklendi: `bagisEtiketi` bilinmeyen kimlikte
  kimliğin kendisini gösterdiği için eski `destek_kahve` gibi kimlikler derleme
  hatası vermeden sessizce yanlış metni test ediyordu.

- **CI artık enstrümante testleri de derliyor.** `derleme.yml` işine
  `:app:assembleDebugAndroidTest` adımı eklendi. Testler koşmuyor — cihaz ya da
  emülatör yok — ama derleniyor; böylece bu tür bir kırık boru hattında
  sessizce yeşil görünmüyor. Cihaz gerektirmez.

## [1.3.2] — 2026-09-14

### Değişti

- **Varsayılan dil İngilizce.** Telefonun dili için çeviri olmadığında
  uygulama artık İngilizce açılıyor; önceden Türkçe açılıyordu (Hintçe,
  Portekizce gibi telefonlarda uygulama Türkçe görünüyordu). Türkçe
  telefonlarda Türkçe açılmaya devam ediyor. İngilizce (varsayılan) metinler
  `values/`, Türkçe metinler `values-tr/` altında.

### Düzeltildi

- **Play'den kurulan uygulamada dil seçici.** Play uygulamayı telefona dil
  dil bölerek kuruyor ve yalnızca telefonun kendi dillerini gönderiyordu; bu
  yüzden örneğin Hintçe bir telefonda "Hakkında" ekranından English seçmek
  hiçbir şeyi değiştirmiyordu. AAB dil bölmesi kapatıldı, iki dil de her
  kuruluma giriyor.
- **Hata kartındaki öneri satırı** (parola, bozuk belge, okunamayan dosya,
  geçersiz aralık, beklenmeyen hata) İngilizce arayüzde de Türkçe
  görünüyordu. Artık arayüzün dilinde.

## [1.3.1] — 2026-08-31

### Değişti

- **Hedef Android sürümü Android 16 (API 36).** Google Play'in zorunlu
  hedefleme şartı gereği `compileSdk` ve `targetSdk` 35'ten 36'ya çıkarıldı.
  Uygulamanın davranışında bilinçli bir değişiklik yok.

## [1.3.0] — 2026-08-29

### Eklendi

- **İngilizce dil desteği.** Uygulama arayüzü artık Türkçe ve İngilizce
  arasında sistem diline göre otomatik geçiş yapıyor; uygulama içinden de
  manuel dil seçimi mümkün.

- **İsteğe bağlı bağış (Google Play Billing).** "Hakkında" ekranına
  "Geliştiriciyi Destekle" bölümü eklendi: üç fiyat basamağından biriyle
  tek seferlik (tekrarlanabilir) bağış yapılabiliyor. Ödeme tamamen Google
  Play üzerinden yürüyor; uygulama kart, kimlik ya da fatura bilgisi
  görmüyor. Bağış isteğe bağlı ve hiçbir özelliği kilitlemiyor. Eklenen
  `com.android.vending.BILLING` izni bir ağ izni değildir — uygulamanın
  `INTERNET` izni yok sayılmaya devam ediyor (bkz. [PRIVACY.md](PRIVACY.md)).

## [1.2.0] — 2026-08-14

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

**Testler**

- 131 birim testi (Robolectric + saf JVM)
- 28 enstrümante test, gerçek cihazda — hem debug hem de yayımlanan
  küçültülmüş (R8) sürüm derlemesine karşı
- Son açılanlar listesinin sıralama, kapasite ve ayrıştırma mantığı ayrı
  ayrı test ediliyor; kapasite aşımında düşen kaydın döndürülmesi de dahil
  (URI yetkisi bırakılabilsin diye)

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

[Yayımlanmamış]: https://github.com/bilalfarukozdemir/pdf-kutusu/compare/v1.3.2...HEAD
[1.3.2]: https://github.com/bilalfarukozdemir/pdf-kutusu/compare/v1.3.1...v1.3.2
[1.3.1]: https://github.com/bilalfarukozdemir/pdf-kutusu/compare/v1.3.0...v1.3.1
[1.3.0]: https://github.com/bilalfarukozdemir/pdf-kutusu/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/bilalfarukozdemir/pdf-kutusu/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/bilalfarukozdemir/pdf-kutusu/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/bilalfarukozdemir/pdf-kutusu/releases/tag/v1.0.0
