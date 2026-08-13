package com.yerel.pdfkutusu.ui.model

import com.yerel.pdfkutusu.cekirdek.PdfHatasi
import com.yerel.pdfkutusu.pdf.BelgeOzeti
import com.yerel.pdfkutusu.pdf.Ilerleme
import java.io.File

/** Islenmeye hazir tek bir girdi. */
data class GirdiOgesi(
    val dosya: File,
    val gorunenAd: String,
    val boyut: Long,
    val sha256: String,
    val ozet: BelgeOzeti,
    /** Parola girilerek acildiysa true; ciktinin sifresiz olacagini soyleriz. */
    val sifresiKaldirildi: Boolean = false,
) {
    val sayfaSayisi: Int get() = ozet.sayfaSayisi
}

/** Sifreli belge icin bekleyen parola sorusu. */
data class ParolaIstegi(
    val dosya: File,
    val gorunenAd: String,
    val boyut: Long,
)

/** Basarili bir islemin ciktisi. */
data class IslemCiktisi(
    val dosyalar: List<File>,
    val sayfaSayisi: Int?,
    val notlar: List<String> = emptyList(),
    val ozetSatiri: String = "",
)

/**
 * Her arac ekraninin durumu.
 *
 * Sartnamedeki dort durum bu tek veri sinifindan turetilir:
 *  - **bos**        : [bosMu]
 *  - **yukleniyor** : [dosyaYukleniyor] ya da [calisiyor]
 *  - **basarili**   : [sonuc] != null
 *  - **hata**       : [hata] != null (hepsi kurtarilabilir; ekran acik kalir)
 */
data class AracEkranDurumu(
    val girdiler: List<GirdiOgesi> = emptyList(),
    val dosyaYukleniyor: Boolean = false,
    val calisiyor: Boolean = false,
    val ilerleme: Ilerleme? = null,
    val sonuc: IslemCiktisi? = null,
    val hata: PdfHatasi? = null,
    val bilgi: String? = null,
    val parolaIstegi: ParolaIstegi? = null,
    val parolaHatasi: String? = null,
) {
    val bosMu: Boolean
        get() = girdiler.isEmpty() && !dosyaYukleniyor && !calisiyor && sonuc == null

    val mesgulMu: Boolean get() = dosyaYukleniyor || calisiyor

    val toplamSayfa: Int get() = girdiler.sumOf { it.sayfaSayisi }

    val uyarilar: List<String>
        get() = girdiler.flatMap { it.ozet.uyarilar }.distinct()

    val ilkGirdi: GirdiOgesi? get() = girdiler.firstOrNull()
}
