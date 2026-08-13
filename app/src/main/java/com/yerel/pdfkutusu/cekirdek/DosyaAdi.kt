package com.yerel.pdfkutusu.cekirdek

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Guvenli dosya adi uretimi ve temizligi.
 *
 * Tasarim kurallari:
 *  - **Turkce karakterler korunur.** cgiosuCGIOSU ve tum aksanli harfler
 *    oldugu gibi kalir; hicbiri "sadelestirilmez".
 *  - Yol ayiricilari (`/`, `\`) ve `..` gibi dizin gezinme parcalari atilir.
 *  - ASCII kontrol karakterleri (0x00-0x1F, 0x7F) silinir.
 *  - Dosya sistemlerinde sorun cikaran `: * ? " < > |` karakterleri `_` olur.
 *  - Taban ad [AZAMI_TABAN_UZUNLUK] karakterle sinirlanir.
 *
 * Not: Tum kucuk/buyuk harf donusumleri [Locale.ROOT] ile yapilir. Turkce
 * yerelde `"I".lowercase()` -> `"ı"` oldugu icin uzanti karsilastirmalarinda
 * yerel bagimliligi kabul edilemez.
 */
object DosyaAdi {

    /** Uzantisiz taban adin azami karakter sayisi. */
    const val AZAMI_TABAN_UZUNLUK = 80

    /** Uzantinin azami karakter sayisi. */
    const val AZAMI_UZANTI_UZUNLUK = 12

    private const val VARSAYILAN_AD = "belge"

    private val ZAMAN_BICIMI: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT)

    // \p{Cntrl} Java'da yalnizca ASCII 0x00-0x1F ve 0x7F esler; Turkce harfler etkilenmez.
    private val KONTROL_KARAKTERLERI = Regex("\\p{Cntrl}")
    private val YASAKLI_KARAKTERLER = Regex("[:*?\"<>|]")
    private val BOSLUK_YIGINI = Regex("\\s+")

    /**
     * Ham bir metni guvenli bir dosya adina cevirir.
     *
     * @param ham kullanicidan ya da SAF'tan gelen ad; null olabilir
     * @param varsayilan hicbir anlamli karakter kalmazsa kullanilacak ad
     */
    fun guvenli(ham: String?, varsayilan: String = VARSAYILAN_AD): String {
        if (ham.isNullOrBlank()) return varsayilan

        // 1) Yol gezinmesini kes: "../../etc/gizli.pdf" -> "gizli.pdf"
        val sonParca = ham
            .split('/', '\\')
            .map { it.trim() }
            .lastOrNull { it.isNotEmpty() && it != "." && it != ".." }
            ?: return varsayilan

        // 2) Kontrol karakterlerini sil
        var ad = KONTROL_KARAKTERLERI.replace(sonParca, "")

        // 3) Dosya sistemi icin sorunlu karakterleri degistir
        ad = YASAKLI_KARAKTERLER.replace(ad, "_")

        // 4) Bosluklari tekile indir; bas/sondaki bosluk ve noktalari kirp
        ad = BOSLUK_YIGINI.replace(ad, " ").trim().trim('.').trim()

        if (ad.isEmpty() || ad == "." || ad == "..") return varsayilan

        // 5) Uzunluk siniri - uzantiyi koruyarak taban adi kirp
        val taban = kirp(tabani(ad), AZAMI_TABAN_UZUNLUK)
        val uzanti = kirp(uzantisi(ad), AZAMI_UZANTI_UZUNLUK)

        if (taban.isEmpty()) return varsayilan
        return if (uzanti.isEmpty()) taban else "$taban.$uzanti"
    }

    /** Uzantiyi atarak taban adi dondurur. `"rapor.final.pdf"` -> `"rapor.final"` */
    fun tabani(dosyaAdi: String): String {
        val nokta = dosyaAdi.lastIndexOf('.')
        return if (nokta > 0) dosyaAdi.substring(0, nokta) else dosyaAdi
    }

    /** Uzantiyi noktasiz dondurur. Yoksa bos metin. */
    fun uzantisi(dosyaAdi: String): String {
        val nokta = dosyaAdi.lastIndexOf('.')
        if (nokta <= 0 || nokta == dosyaAdi.length - 1) return ""
        val uzanti = dosyaAdi.substring(nokta + 1)
        // Uzanti icinde bosluk varsa muhtemelen uzanti degildir ("v1.2 son surum")
        return if (uzanti.contains(' ')) "" else uzanti
    }

    /**
     * Sartnameye gore cikti adi uretir:
     * `<orijinal-ad>__<islem>__<yyyyMMdd-HHmmss>.<uzanti>`
     */
    fun cikti(
        kaynakDosyaAdi: String?,
        islem: IslemTuru,
        zaman: LocalDateTime = LocalDateTime.now(),
        uzanti: String = "pdf",
        ekBilgi: String? = null,
    ): String {
        val guvenliKaynak = guvenli(kaynakDosyaAdi)
        val taban = kirp(tabani(guvenliKaynak), AZAMI_TABAN_UZUNLUK).ifEmpty { VARSAYILAN_AD }
        val ek = ekBilgi?.let { "-" + guvenli(it, "").replace(' ', '-') }.orEmpty()
        return "${taban}__${islem.dosyaEki}${ek}__${zaman.format(ZAMAN_BICIMI)}.$uzanti"
    }

    /**
     * [dizin] icinde [istenenAd] doluysa `ad (1).pdf`, `ad (2).pdf` ... dener.
     * Var olan bir dosyanin uzerine yazmayi engeller.
     */
    fun cakismayan(dizin: File, istenenAd: String): File {
        val ilk = File(dizin, istenenAd)
        if (!ilk.exists()) return ilk

        val taban = tabani(istenenAd)
        val uzanti = uzantisi(istenenAd)
        val ek = if (uzanti.isEmpty()) "" else ".$uzanti"
        var sayac = 1
        while (sayac < 10_000) {
            val aday = File(dizin, "$taban ($sayac)$ek")
            if (!aday.exists()) return aday
            sayac++
        }
        // Pratikte ulasilmaz; yine de determinist bir cikis birakiyoruz.
        return File(dizin, "$taban (${System.nanoTime()})$ek")
    }

    private fun kirp(metin: String, azami: Int): String {
        if (metin.length <= azami) return metin
        var kesim = azami
        // Yuzey cifti (surrogate pair) ortasindan bolmeyelim.
        if (kesim > 0 && Character.isHighSurrogate(metin[kesim - 1])) kesim--
        return metin.substring(0, kesim).trim().trim('.').trim()
    }
}
