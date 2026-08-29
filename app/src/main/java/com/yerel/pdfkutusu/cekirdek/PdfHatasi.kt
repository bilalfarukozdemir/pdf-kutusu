package com.yerel.pdfkutusu.cekirdek

/**
 * Kullaniciya gosterilebilir, kurtarilabilir hatalar.
 *
 * Cogu alt sinif [kullaniciMesaji] ve [oneri] alanlarini, cagiran tarafin
 * (Context'i olan bir katmanin) zaten yerellestirdigi hazir metinle doldurur.
 *
 * Bazi durumlarda hatayi fırlatan kod Context'e erisemez (`cekirdek/` saf
 * Kotlin'dir) ya da metin cagiri noktasinda hicbir zaman verilmemistir
 * (ornegin bu sinifin kendi varsayilan degerleri). Bu durumlarda [kimlik] dolu
 * gelir; UI katmani [kimlik] varsa onu R.string karsiligina cevirir, yoksa
 * [kullaniciMesaji]/[oneri]'yi oldugu gibi kullanir (bkz. `ui/ortak/PdfHataMetni.kt`).
 */
sealed class PdfHatasi(
    val kullaniciMesaji: String,
    val oneri: String? = null,
    val kimlik: PdfHataKimligi? = null,
    neden: Throwable? = null,
) : Exception(kullaniciMesaji, neden) {

    class ParolaGerekli : PdfHatasi(
        kullaniciMesaji = "Bu PDF parola korumalı.",
        oneri = "Belgenin açılış parolasını girin.",
        kimlik = PdfHataKimligi.ParolaGerekli,
    )

    class ParolaYanlis : PdfHatasi(
        kullaniciMesaji = "Parola doğrulanamadı.",
        oneri = "Parolayı kontrol edip tekrar deneyin. Büyük/küçük harfe dikkat edin.",
        kimlik = PdfHataKimligi.ParolaYanlis,
    )

    /**
     * @param mesaj cagiran taraf tarafindan zaten yerellestirilmis metin
     * @param kimlik yalnizca [SayfaAraligi] gibi Context'i olmayan cagiranlar icin;
     *   dolu geldiginde UI [mesaj] yerine kimlikten uretilen metni gosterir
     */
    class GecersizAralik private constructor(
        mesaj: String,
        kimlik: PdfHataKimligi.AralikNedeni?,
    ) : PdfHatasi(
        kullaniciMesaji = mesaj,
        oneri = "Örnek: 1-3, 5, 8-10",
        kimlik = kimlik,
    ) {
        constructor(mesaj: String) : this(mesaj, kimlik = null)
        constructor(kimlik: PdfHataKimligi.AralikNedeni) : this(TEKNIK_MESAJ, kimlik)

        private companion object {
            /**
             * Kimlik-tabanli olusumda [kullaniciMesaji] yalnizca istisnanin
             * kendi `message` alani icindir (loglama); ekranda gosterilecek
             * gercek metin UI'da [kimlik] uzerinden uretilir.
             */
            const val TEKNIK_MESAJ = "Geçersiz sayfa aralığı."
        }
    }

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

    /**
     * @param mesaj verilmezse (ornegin [AracViewModel.calistir] icindeki genel
     *   savunma kontrolu gibi Context'e erisimin dogal olmadigi yerlerde)
     *   metin UI'da [PdfHataKimligi.GirdiSecilmedi] uzerinden uretilir
     */
    class GirdiYok(mesaj: String? = null) : PdfHatasi(
        kullaniciMesaji = mesaj ?: "Önce en az bir PDF seçin.",
        kimlik = if (mesaj == null) PdfHataKimligi.GirdiSecilmedi else null,
    )

    class Iptal : PdfHatasi(
        kullaniciMesaji = "İşlem iptal edildi.",
        kimlik = PdfHataKimligi.Iptal,
    )

    class Beklenmeyen(neden: Throwable) : PdfHatasi(
        kullaniciMesaji = "İşlem tamamlanamadı: ${neden.message ?: neden::class.java.simpleName}",
        oneri = "Aynı hata tekrarlıyorsa dosya bu araçla işlenemiyor olabilir.",
        kimlik = PdfHataKimligi.Beklenmeyen(neden.message ?: neden::class.java.simpleName),
        neden = neden,
    )
}
