package com.yerel.pdfkutusu.test

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.File

/**
 * Testler icin bilinen icerikli PDF uretir.
 *
 * Metin bilerek ASCII: standart-14 Helvetica WinAnsi kodlamasi kullanir ve
 * `ğ/ş/ı` harflerini kodlayamaz. Turkce karakter davranisi ayri olarak
 * [com.yerel.pdfkutusu.DosyaAdiTesti] ve filigran yolunda sinaniyor.
 */
object TestPdfUretici {

    /** A4 sayfa olculeri (nokta): 595 x 842. */
    val SAYFA_BOYUTU: PDRectangle = PDRectangle.A4

    /** Metnin cizildigi taban cizgisi (sol alt kokenli PDF koordinati). */
    const val METIN_X = 72f
    const val METIN_Y = 700f
    const val PUNTO = 24f

    fun olustur(hedef: File, sayfaMetinleri: List<String>): File {
        PDDocument().use { belge ->
            for (metin in sayfaMetinleri) {
                val sayfa = PDPage(PDRectangle(SAYFA_BOYUTU.width, SAYFA_BOYUTU.height))
                belge.addPage(sayfa)
                PDPageContentStream(belge, sayfa).use { akis ->
                    akis.beginText()
                    akis.setFont(PDType1Font.HELVETICA, PUNTO)
                    akis.newLineAtOffset(METIN_X, METIN_Y)
                    akis.showText(metin)
                    akis.endText()
                }
            }
            belge.save(hedef)
        }
        return hedef
    }

    /**
     * Metnin bulundugu bandi, sol-ust kokenli normalize koordinatlarda dondurur.
     * Karartma testinde "bu bolgeyi karart" demek icin kullanilir.
     */
    fun metinBandi(): FloatArray {
        val yukseklik = SAYFA_BOYUTU.height
        // PDF koordinati alt-sol kokenli; ekran/bitmap koordinati ust-sol.
        val ustNormal = (yukseklik - (METIN_Y + PUNTO * 1.2f)) / yukseklik
        val altNormal = (yukseklik - (METIN_Y - PUNTO * 0.6f)) / yukseklik
        return floatArrayOf(0f, ustNormal, 1f, altNormal)
    }
}
