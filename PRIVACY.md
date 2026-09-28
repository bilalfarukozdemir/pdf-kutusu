# Gizlilik Politikası

**Yürürlük tarihi:** 2026-09-23

PDF Kutusu, çevrimdışı çalışan, açık kaynak bir Android PDF aracıdır.
Bu belge, uygulamanın cihazda tuttuğu veriyi ve kullanıcının başlattığı
paylaşımların nasıl çalıştığını anlatır.

<details>
<summary><b>In English</b></summary>

PDF Kutusu ("PDF Box") is an offline, open-source Android PDF tool. This
document explains what the app stores locally and how user-initiated sharing
works.

**No network collection.** The app has no `INTERNET` permission. This isn't a
policy choice — a Gradle task (`AgIzniDenetimi`) fails the build if any
network-capable permission survives manifest merging, and CI re-verifies the
produced APK with `aapt dump permissions` on every push. There is no server
this app could send data to even if it wanted to.

**File access.** All file access goes through Android's Storage Access
Framework (SAF): you pick a file with the system picker, the app reads/writes
only that file, only while you've granted access. The app never scans your
storage on its own and requests no broad storage permissions
(`READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `MANAGE_EXTERNAL_STORAGE`
are all explicitly stripped from the manifest).

**On-device OCR.** Text recognition uses ML Kit's *bundled* model, packaged
inside the APK. Nothing is downloaded, and no image or text data leaves the
device for OCR to work.

**No telemetry, no analytics, no ads, no accounts.** There is no crash
reporter, no analytics SDK, no ad SDK, and no sign-in of any kind.

**Paid review services (Google Play Billing).** The Play Store release offers
optional feature-request and open-source PR reviews, each with a written reply
we aim to review and reply in writing within 14 days. The services do not
promise code changes or PR merges. Google
Play handles payment details; the app never sees or stores your card, billing
address, or payment method. A short request reference and product ID are stored
locally until you mark the email request as sent. Enabling Google Play Billing adds the
`com.android.vending.BILLING` permission, which is **not** a network
permission: it lets the app talk to the on-device Play Store app over local
IPC (inter-process communication), and Play Store itself does the network
work. The app still has no `INTERNET` permission.

**Email requests are user-controlled.** After purchase, the app can open a
pre-filled email draft in an email app on your device. It does not send the
message itself, read the message you write, or access your email account. The
draft contains the request reference and a blank area for your feature request
or PR link. The email provider processes the message only if you choose to send
it; its own privacy policy applies. The short request reference stays on the
device until you tap the confirmation button in PDF Kutusu.

**Contact.** Questions or concerns: open an issue on
[GitHub](https://github.com/bilalfarukozdemir/pdf-kutusu/issues).

</details>

---

## Uygulamanın topladığı veri: yok

PDF Kutusu kişisel veriyi bir sunucuya göndermez. Bekleyen satın alınmış destek
hakkı için ürün kimliği ve kısa talep kodu cihazda saklanır; e-posta taslağı
yalnızca kullanıcı gönderirse e-posta sağlayıcısı üzerinden iletilir.
Uygulamanın **`INTERNET` izni yoktur**, dolayısıyla uygulamanın kendisinin
bağlanabileceği bir sunucu bulunmaz.

Bu, elle uyulan bir kural değil, derleme zamanında zorlanan bir kısıt:

1. `app/src/main/AndroidManifest.xml`, ML Kit'in bağımlılık zincirinin
   (`play-services-basement`, `com.google.android.datatransport`) kendi
   manifestlerinde tanımladığı `INTERNET`, `ACCESS_NETWORK_STATE` ve benzeri
   ağ izinlerini `tools:node="remove"` direktifiyle siler.
2. `app/build.gradle.kts` içindeki `AgIzniDenetimi` adlı özel Gradle görevi,
   birleşmiş (merged) manifestte `INTERNET`, `ACCESS_NETWORK_STATE` veya
   listelenen diğer izinlerden biri kalırsa **derlemeyi durdurur**;
   `assembleDebug`, `packageDebug` ve `check` görevleri buna bağlıdır,
   atlanamaz.
3. CI, her push'ta üretilen APK'yı `aapt dump permissions` ile ayrıca
   doğrular.

Kod tabanında da ağa çıkan bir kütüphane (ör. OkHttp, Retrofit, Ktor,
`HttpURLConnection`) kullanılmıyor — proje bunu gerektirecek hiçbir ağ
işlemi yapmıyor.

Kendiniz doğrulamak isterseniz:

```bash
aapt dump permissions app/build/outputs/apk/debug/app-debug.apk
```

Çıktıda `android.permission.INTERNET` ve benzeri ağ izinleriyle başlayan
hiçbir satır görünmemelidir. Görünmesi beklenen iki satır vardır: biri
androidx.core'un eklediği, uygulamanın kendi paket adıyla imzalı olan,
kullanıcıya gösterilmeyen ve hiçbir sistem kaynağına erişim vermeyen
`signature` seviyesindeki `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`; diğeri
Google Play Billing kütüphanesinin eklediği `com.android.vending.BILLING`
iznidir — o da bir ağ izni değildir, yukarıda "Öncelikli inceleme hizmetleri"
bölümünde açıklandığı
gibi cihazdaki Play Store uygulamasıyla süreçler-arası iletişim (IPC) içindir
ve kullanıcıya bir izin ekranında gösterilmez.

## Dosyalarınız cihazdan çıkmaz

Tüm dosya erişimi Android'in **Storage Access Framework (SAF)**'i üzerinden
yapılır: dosyayı sistem seçicisinden siz seçersiniz, uygulama yalnızca o
dosyaya, yalnızca izin verdiğiniz sürece erişir. Uygulama arka planda
depolamanızı taramaz ve genel depolama izni istemez —
`READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE` ve
`MANAGE_EXTERNAL_STORAGE` izinlerinin hepsi manifestten açıkça kaldırılmıştır.

Kaynak dosyanız hiçbir işlemde değiştirilmez; işlemler geçici bir çalışma
kopyası üzerinde yapılır ve sonuç, yine SAF ile sizin seçtiğiniz konuma
yazılır.

## Cihaz üzerinde OCR

Metin tanıma (OCR), ML Kit'in **paketli (bundled)** modeliyle çalışır — model
APK'nın içinde gelir. "Unbundled" varyantın aksine hiçbir şey indirilmez;
OCR çalışırken görsel veya metin verisi cihazdan çıkmaz.

## Telemetri, analitik, reklam, hesap — yok

Uygulamada çökme raporlama (crash reporting), analitik SDK'sı, reklam SDK'sı
ya da herhangi bir hesap/oturum açma mekanizması bulunmaz.

## Öncelikli inceleme hizmetleri (Google Play Billing)

Play Store sürümünde isteğe bağlı özellik isteği incelemesi, açık kaynak PR
incelemesi veya ikisini içeren bir hizmet paketi sunulur. Her hizmet için 14
gün içinde inceleme ve yazılı yanıt hedeflenir; kod değişikliği, özellik uygulaması ya
da PR'ın birleştirilmesi vaat edilmez. Bu özellik kullanıldığında:

- Ödeme süreci **tamamen Google Play tarafından yürütülür.** Kart bilgisi,
  fatura adresi ve ödeme yöntemi gibi hiçbir bilgi bu uygulamaya ulaşmaz ve
  uygulama tarafından saklanmaz.
- Google Play Billing'in eklediği `com.android.vending.BILLING` izni bir
  **ağ izni değildir.** Bu izin, uygulamanın cihazdaki Play Store
  uygulamasıyla süreçler-arası iletişim (IPC) yoluyla konuşmasını sağlar; asıl
  ağ trafiğini Play Store uygulaması yürütür. Uygulamanın kendisi hâlâ
  `INTERNET` izni istemez.
- Satın alma **isteğe bağlıdır**; PDF araçlarının hiçbirini kilitlemez.

## E-posta taslağı

Satın alınmış inceleme hakkı, e-posta uygulamasında önceden doldurulmuş bir
taslak açabilir. Uygulama mesajı otomatik göndermez, düzenlediğiniz metni
okumaz ve e-posta hesabınıza erişmez. İçeriği yalnızca siz e-posta
uygulamasındaki gönder düğmesine basarsanız geliştiriciye iletilir. Mesajın
taşınması ve saklanması e-posta sağlayıcısının kurallarına tabidir.

## İletişim

Sorularınız veya endişeleriniz için
[GitHub Issues](https://github.com/bilalfarukozdemir/pdf-kutusu/issues)
üzerinden ulaşabilirsiniz.
