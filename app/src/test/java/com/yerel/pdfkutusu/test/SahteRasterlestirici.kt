package com.yerel.pdfkutusu.test

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.yerel.pdfkutusu.pdf.NoktaBoyutu
import com.yerel.pdfkutusu.pdf.SayfaRasterlestirici
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * JVM birim testleri icin rasterlestirici.
 *
 * Android'in `PdfRenderer`'i yerel (native) bir bilesendir ve JVM testinde
 * calismaz. Bu sahte uygulama **arayuzun sozlesmesini** birebir yerine getirir:
 * sayfa sayisi, nokta cinsinden gorsel boyut ve istenen DPI'da bir bitmap.
 *
 * Sayfa icerigini gercekten cizmez - ve bu, karartma testi icin bir eksiklik
 * degildir: test, ciktida metnin **bulunmadigini** dogrular. Boru hattinin
 * dogru olmasi, karartilan sayfanin metin akisinin tamamen atilip yerine bir
 * gorsel konmasi demektir; bu, cizilen piksellerden bagimsizdir.
 *
 * Gercek `PdfRenderer` ile ucdan uca karartma dogrulamasi enstrumante testte
 * yapilir: `KarartmaCihazTesti`.
 */
class SahteRasterlestirici : SayfaRasterlestirici {

    var acilanOturumSayisi = 0
        private set

    override fun ac(kaynak: File): SayfaRasterlestirici.Oturum {
        acilanOturumSayisi++
        return Oturum(kaynak)
    }

    private class Oturum(kaynak: File) : SayfaRasterlestirici.Oturum {

        private val belge = PDDocument.load(kaynak)

        override val sayfaSayisi: Int get() = belge.numberOfPages

        override fun noktaBoyutu(indeks: Int): NoktaBoyutu {
            val sayfa = belge.getPage(indeks)
            val kutu = sayfa.cropBox ?: sayfa.mediaBox
            val donme = ((sayfa.rotation % 360) + 360) % 360
            return if (donme == 90 || donme == 270) {
                NoktaBoyutu(kutu.height, kutu.width)
            } else {
                NoktaBoyutu(kutu.width, kutu.height)
            }
        }

        override fun rasterlestir(indeks: Int, dpi: Int): Bitmap {
            val boyut = noktaBoyutu(indeks)
            val olcek = dpi / 72f
            val bitmap = Bitmap.createBitmap(
                max(1, (boyut.genislik * olcek).roundToInt()),
                max(1, (boyut.yukseklik * olcek).roundToInt()),
                Bitmap.Config.ARGB_8888,
            )
            Canvas(bitmap).drawColor(Color.WHITE)
            bitmap.setHasAlpha(false)
            return bitmap
        }

        override fun close() {
            belge.close()
        }
    }
}
