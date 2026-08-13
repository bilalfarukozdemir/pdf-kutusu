package com.yerel.pdfkutusu.cekirdek

/**
 * Kullaniciya gosterilebilir, kurtarilabilir hatalar.
 *
 * Her hatanin [mesaj] alani dogrudan arayuzde gosterilecek sekilde Turkce
 * yazilmistir; [oneri] varsa kullanicinin ne yapabilecegini soyler.
 */
sealed class PdfHatasi(
    val kullaniciMesaji: String,
    val oneri: String? = null,
    neden: Throwable? = null,
) : Exception(kullaniciMesaji, neden) {

    class ParolaGerekli : PdfHatasi(
        kullaniciMesaji = "Bu PDF parola korumalı.",
        oneri = "Belgenin açılış parolasını girin.",
    )

    class ParolaYanlis : PdfHatasi(
        kullaniciMesaji = "Parola doğrulanamadı.",
        oneri = "Parolayı kontrol edip tekrar deneyin. Büyük/küçük harfe dikkat edin.",
    )

    class GecersizAralik(mesaj: String) : PdfHatasi(
        kullaniciMesaji = mesaj,
        oneri = "Örnek: 1-3, 5, 8-10",
    )

    class BozukBelge(mesaj: String, neden: Throwable? = null) : PdfHatasi(
        kullaniciMesaji = mesaj,
        oneri = "Dosya bozuk ya da desteklenmeyen bir biçimde olabilir. Başka bir kopyayla deneyin.",
        neden = neden,
    )

    class DosyaOkunamadi(mesaj: String, neden: Throwable? = null) : PdfHatasi(
        kullaniciMesaji = mesaj,
        oneri = "Dosyayı yeniden seçmeyi deneyin; kaynak uygulama erişimi geri çekmiş olabilir.",
        neden = neden,
    )

    class GirdiYok(mesaj: String = "Önce en az bir PDF seçin.") : PdfHatasi(
        kullaniciMesaji = mesaj,
    )

    class Iptal : PdfHatasi(
        kullaniciMesaji = "İşlem iptal edildi.",
    )

    class Beklenmeyen(neden: Throwable) : PdfHatasi(
        kullaniciMesaji = "İşlem tamamlanamadı: ${neden.message ?: neden::class.java.simpleName}",
        oneri = "Aynı hata tekrarlıyorsa dosya bu araçla işlenemiyor olabilir.",
        neden = neden,
    )
}
