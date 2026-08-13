package com.yerel.pdfkutusu.pdf

import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination

/**
 * Bir belgeden secili sayfalari, verilen sirayla yeni bir belgeye kopyalar.
 *
 * Bolme ve siralama islemlerinin ortak temeli.
 *
 * Onemli davranis notu: [PDDocument.importPage] sayfa icerik akisini hedef
 * belgeye kopyalar, ancak kaynaklar (font, gorsel) hala **kaynak belgeye ait
 * COS nesnelerine** isaret eder. PDFBox kaydetme sirasinda bu nesneleri de
 * yazar; bu yuzden **kaynak belge, hedef kaydedilene kadar acik kalmalidir.**
 * Bu, PDFBox'in kendi `Splitter` sinifinin de izledigi yontemdir.
 */
object SayfaKopyalayici {

    /**
     * @param kaynak acik kalmasi gereken kaynak belge
     * @param indeksler 0-tabanli sayfa indeksleri; sira aynen korunur,
     *   ayni indeks birden fazla kez verilebilir (sayfa cogaltma)
     * @return cagiranin kaydedip kapatmasi gereken yeni belge
     */
    fun kopyala(
        kaynak: PDDocument,
        indeksler: List<Int>,
        ilerleme: IlerlemeDinleyicisi = IlerlemeYok,
    ): PDDocument {
        val hedef = PDDocument()
        try {
            indeksler.forEachIndexed { sira, indeks ->
                val sayfa = kaynak.getPage(indeks)
                sayfaAktar(hedef, sayfa)
                ilerleme(Ilerleme(sira + 1, indeksler.size, "Sayfa ${indeks + 1}"))
            }
        } catch (hata: Throwable) {
            hedef.close()
            throw hata
        }
        return hedef
    }

    /**
     * Tek bir sayfayi **var olan** bir hedef belgeye aktarir; sayfa kutulari,
     * dondurme acisi ve kaynaklar acikca kopyalanir.
     *
     * Karartma gibi karma islemlerde (bazi sayfalar rasterlenir, bazilari
     * oldugu gibi kalir) hedef belge disaridan yonetildigi icin bu fonksiyon
     * disari acik.
     */
    fun sayfaAktar(hedef: PDDocument, sayfa: PDPage): PDPage {
        val aktarilan = hedef.importPage(sayfa)
        aktarilan.mediaBox = sayfa.mediaBox
        aktarilan.cropBox = sayfa.cropBox
        aktarilan.rotation = sayfa.rotation
        aktarilan.resources = sayfa.resources

        // Baglanti (link) acikliklari baska bir belgedeki sayfalara isaret
        // edebilir. Kopyalanan belgede o sayfalar olmayabilecegi icin hedef
        // referanslarini kopariyoruz; aksi halde acilan PDF'te bozuk
        // baglantilar ya da okuyucu hatalari olusur.
        runCatching {
            val aciklamalar = aktarilan.annotations
            for (aciklama in aciklamalar) {
                aciklama.page = null
                if (aciklama is PDAnnotationLink) {
                    val hedefNokta = aciklama.destination
                    if (hedefNokta is PDPageDestination) {
                        hedefNokta.page = null
                    }
                    aciklama.action = null
                }
            }
            aktarilan.annotations = aciklamalar
        }.onFailure {
            // Bozuk aciklama sozlukleri yuzunden tum islemi kaybetmeyelim.
            aktarilan.cosObject.removeItem(COSName.ANNOTS)
        }
        return aktarilan
    }
}
