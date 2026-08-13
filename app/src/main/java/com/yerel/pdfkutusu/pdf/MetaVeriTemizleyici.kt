package com.yerel.pdfkutusu.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDDocumentInformation

/**
 * Cikti belgesinin meta verilerini temizler.
 *
 * Karartma icin zorunludur: siyah dikdortgen cizmek isinize yaramaz, eger
 * belge bilgisi alaninda hala "Yazar: ..." ya da eski baslikta gizli bilgi
 * duruyorsa. Diger islemlerde de varsayilan olarak uyguluyoruz; bu araç
 * disari veri sizdirmama sozunun bir parcasi.
 */
object MetaVeriTemizleyici {

    /**
     * @param uretici cikti belgesine yazilacak tek meta veri. Bos birakilirsa
     *   hicbir alan doldurulmaz.
     */
    fun temizle(belge: PDDocument, uretici: String? = "PDF Kutusu (cihaz üstü)") {
        belge.documentInformation = PDDocumentInformation().apply {
            author = null
            title = null
            subject = null
            keywords = null
            creator = null
            producer = uretici
            // Olusturma/degistirme zamanlarini da dusuruyoruz: cihazin saat
            // dilimi bile bir sizinti kanali olabilir.
            creationDate = null
            modificationDate = null
        }
        // XMP paketi ayri yasar; belge bilgisini temizlemek onu silmez.
        belge.documentCatalog.metadata = null
    }
}
