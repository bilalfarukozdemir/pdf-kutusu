package com.yerel.pdfkutusu.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.yerel.pdfkutusu.cekirdek.PdfHatasi
import java.io.Closeable
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Sayfanin nokta (1/72 inc) cinsinden gorsel boyutu; `/Rotate` uygulanmistir. */
data class NoktaBoyutu(val genislik: Float, val yukseklik: Float)

/**
 * Bir PDF sayfasini bitmap'e cevirme yetenegi.
 *
 * Arayuz olarak tanimlanmasinin sebebi test edilebilirlik: [PdfKartici]
 * gercek [PdfRenderer]'a degil bu arayuze bagli oldugu icin karartma
 * boru hatti JVM birim testlerinde de calistirilabilir.
 */
interface SayfaRasterlestirici {

    fun ac(kaynak: File): Oturum

    interface Oturum : Closeable {
        val sayfaSayisi: Int
        fun noktaBoyutu(indeks: Int): NoktaBoyutu
        fun rasterlestir(indeks: Int, dpi: Int): Bitmap
    }
}

/**
 * Android'in yerlesik [PdfRenderer] motoru. Ek kutuphane gerektirmez.
 *
 * Kisitlar:
 *  - Yalnizca sifresiz dosyalari acar. Sifreli belgeler once
 *    [RasterHazirligi] ile cozulup gecici bir kopyaya yazilir.
 *  - Dosya tanimlayicisi (fd) aranabilir olmali; bu yuzden her zaman
 *    uygulamaya ozel dizindeki gercek bir dosyayla calisiriz.
 */
class PdfRendererRasterlestirici : SayfaRasterlestirici {

    override fun ac(kaynak: File): SayfaRasterlestirici.Oturum = try {
        PdfRendererOturumu(kaynak)
    } catch (hata: Exception) {
        throw PdfHatasi.BozukBelge("Sayfa görüntüsü oluşturulamadı: ${kaynak.name}", hata)
    }

    private class PdfRendererOturumu(kaynak: File) : SayfaRasterlestirici.Oturum {

        private val tanimlayici: ParcelFileDescriptor =
            ParcelFileDescriptor.open(kaynak, ParcelFileDescriptor.MODE_READ_ONLY)
        private val motor = PdfRenderer(tanimlayici)

        override val sayfaSayisi: Int get() = motor.pageCount

        override fun noktaBoyutu(indeks: Int): NoktaBoyutu {
            val sayfa = motor.openPage(indeks)
            try {
                return NoktaBoyutu(sayfa.width.toFloat(), sayfa.height.toFloat())
            } finally {
                sayfa.close()
            }
        }

        override fun rasterlestir(indeks: Int, dpi: Int): Bitmap {
            val sayfa = motor.openPage(indeks)
            try {
                val olcek = olcekHesapla(sayfa.width, sayfa.height, dpi)
                val enPiksel = max(1, (sayfa.width * olcek).roundToInt())
                val boyPiksel = max(1, (sayfa.height * olcek).roundToInt())

                val bitmap = Bitmap.createBitmap(enPiksel, boyPiksel, Bitmap.Config.ARGB_8888)
                // PdfRenderer saydam bolgeleri bos birakir; PDF'in "kagidi"
                // beyazdir, bu yuzden once beyaza boyuyoruz. Aksi halde
                // JPEG'e cevirirken saydam alanlar siyaha doner.
                Canvas(bitmap).drawColor(Color.WHITE)
                sayfa.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                // Sayfanin tamami beyazla dolduruldugu icin alfa kanali gereksiz.
                // Bunu belirtmek, JPEG'e cevirirken PDF'e bos bir yumusak maske
                // (soft mask) eklenmesini onler; dosya kucuk ve sade kalir.
                bitmap.setHasAlpha(false)
                return bitmap
            } finally {
                sayfa.close()
            }
        }

        override fun close() {
            runCatching { motor.close() }
            runCatching { tanimlayici.close() }
        }
    }

    private companion object {
        /**
         * Bellek butcesi. ARGB_8888'de piksel basina 4 bayt dustugu icin
         * 32 milyon piksel ~128 MB demektir; A0 boyutunda bir sayfa 200 DPI'da
         * bu sinira denk gelir. Daha buyuk sayfalarda cozunurlugu dusururuz;
         * karartmanin GUVENLIGI cozunurluge bagli degildir (piksel zaten
         * siyaha boyanir), yalnizca okunabilirlik etkilenir.
         */
        const val AZAMI_PIKSEL = 32_000_000L

        fun olcekHesapla(genislikNokta: Int, yukseklikNokta: Int, dpi: Int): Float {
            val istenen = dpi / 72f
            val piksel = genislikNokta.toLong() * yukseklikNokta.toLong() *
                (istenen * istenen).toLong().coerceAtLeast(1L)
            if (piksel <= AZAMI_PIKSEL) return istenen
            val kucultme = sqrt(AZAMI_PIKSEL.toDouble() / piksel.toDouble()).toFloat()
            return max(0.5f, min(istenen, istenen * kucultme))
        }
    }
}

/**
 * [PdfRenderer] sifreli PDF acamaz. Bu yardimci, gerekiyorsa belgeyi
 * PDFBox ile cozup gecici bir kopya uretir; gerekmiyorsa kaynagi oldugu
 * gibi dondurur.
 */
object RasterHazirligi {

    /**
     * @return (rasterlenecek dosya, gecici mi) ciftinde gecici dosya
     *   cagiran tarafindan silinmelidir.
     */
    fun hazirla(kaynak: File, parola: String?, gecicilerDizini: File): Pair<File, Boolean> {
        val sifreli = runCatching { BelgeErisimi.sifreliMi(kaynak) }.getOrDefault(false)
        if (!sifreli) return kaynak to false

        val gecici = File.createTempFile("cozulmus_", ".pdf", gecicilerDizini)
        BelgeErisimi.ac(kaynak, parola).use { belge ->
            belge.isAllSecurityToBeRemoved = true
            belge.save(gecici)
        }
        return gecici to true
    }
}
