package com.yerel.pdfkutusu.pdf

import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.yerel.pdfkutusu.cekirdek.PdfHatasi
import java.io.File

/** Birlestirilecek tek bir girdi. */
data class BirlestirmeGirdisi(
    val dosya: File,
    val gorunenAd: String,
    val parola: String? = null,
)

/**
 * Birden fazla PDF'i tek dosyada birlestirir.
 *
 * Sira, [girdiler] listesinin sirasidir. Kaynak dosyalar degistirilmez.
 */
object PdfBirlestirici {

    /**
     * @return cikti belgesinin toplam sayfa sayisi
     */
    fun birlestir(
        girdiler: List<BirlestirmeGirdisi>,
        cikti: File,
        metaVeriTemizle: Boolean = true,
        ilerleme: IlerlemeDinleyicisi = IlerlemeYok,
    ): Int {
        if (girdiler.isEmpty()) throw PdfHatasi.GirdiYok("Birleştirmek için en az bir PDF seçin.")

        PDDocument().use { hedef ->
            val birlestirici = PDFMergerUtility()
            girdiler.forEachIndexed { sira, girdi ->
                ilerleme(Ilerleme(sira, girdiler.size, girdi.gorunenAd))
                BelgeErisimi.ac(girdi.dosya, girdi.parola).use { kaynak ->
                    BelgeErisimi.guvenligiKaldir(kaynak)
                    // appendDocument COS nesnelerini hedefe klonlar; bu yuzden
                    // kaynagi hemen kapatmak guvenlidir.
                    birlestirici.appendDocument(hedef, kaynak)
                }
                ilerleme(Ilerleme(sira + 1, girdiler.size, girdi.gorunenAd))
            }

            if (hedef.numberOfPages == 0) {
                throw PdfHatasi.BozukBelge("Seçilen dosyalarda hiç sayfa bulunamadı.")
            }
            if (metaVeriTemizle) MetaVeriTemizleyici.temizle(hedef)
            hedef.save(cikti)
            return hedef.numberOfPages
        }
    }
}
