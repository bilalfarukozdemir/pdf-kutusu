package com.yerel.pdfkutusu.test

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.File

/**
 * Enstrumante testler icin bilinen icerikli PDF ureteci.
 *
 * `src/test` altindaki esiyle ayni davranisi verir. Kotlin/Gradle'da birim ve
 * enstrumante kaynak kumeleri birbirini gormedigi icin bilerek iki kopya var;
 * degistirirken ikisini birden guncelleyin.
 */
object TestPdfUretici {

    val SAYFA_BOYUTU: PDRectangle = PDRectangle.A4

    const val METIN_X = 72f
    const val METIN_Y = 700f
    const val PUNTO = 24f

    fun olustur(hedef: File, sayfaMetinleri: List<String>): File {
        hedef.parentFile?.mkdirs()
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

    /** Metnin bulundugu bandi sol-ust kokenli normalize koordinatlarda dondurur. */
    fun metinBandi(): FloatArray {
        val yukseklik = SAYFA_BOYUTU.height
        val ustNormal = (yukseklik - (METIN_Y + PUNTO * 1.2f)) / yukseklik
        val altNormal = (yukseklik - (METIN_Y - PUNTO * 0.6f)) / yukseklik
        return floatArrayOf(0f, ustNormal, 1f, altNormal)
    }
}
