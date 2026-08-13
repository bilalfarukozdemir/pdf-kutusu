package com.yerel.pdfkutusu

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.yerel.pdfkutusu.pdf.BelgeErisimi
import com.yerel.pdfkutusu.pdf.GorselGirdisi
import com.yerel.pdfkutusu.pdf.PdfRendererRasterlestirici
import com.yerel.pdfkutusu.pdf.ResimdenPdf
import com.yerel.pdfkutusu.pdf.ResimdenPdfAyarlari
import com.yerel.pdfkutusu.pdf.SayfaDuzeni
import com.yerel.pdfkutusu.pdf.SikistirmaKalitesi
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Resimden PDF'in piksel duzeyindeki guvenceleri.
 *
 * Bu dogrulamalar bilerek **enstrumante** testtir. Robolectric'in varsayilan
 * (LEGACY) grafik kipinde `Canvas.drawBitmap` bos gecer, `Bitmap.compress`
 * yer tutucu yazar ve `getPixel` sifir doner; ayni testler birim testi olarak
 * yazilsaydi **kod bozukken de gecerdi**. Yon, saydamlik ve EXIF sizintisi
 * ancak gercek bir kodlayiciyla kanitlanabilir.
 *
 * Saf mantik (yon eslemesi, sayfa yerlesimi, orneklem) birim testinde:
 * `ExifYonuTesti`, `SayfaYerlesimiTesti`.
 */
@RunWith(AndroidJUnit4::class)
class ResimdenPdfCihazTesti {

    private lateinit var baglam: Context
    private lateinit var dizin: File
    private lateinit var geciciler: File

    @Before
    fun hazirla() {
        baglam = ApplicationProvider.getApplicationContext()
        PDFBoxResourceLoader.init(baglam)
        dizin = File(baglam.cacheDir, "resim-testi").apply {
            deleteRecursively()
            mkdirs()
        }
        geciciler = File(dizin, "gecici").apply { mkdirs() }
    }

    // =====================================================================
    // 1. Sayfa sayisi
    // =====================================================================

    @Test
    fun sayfaSayisiGorselSayisinaEsittir() {
        val girdiler = (1..3).map { sira ->
            GorselGirdisi(jpegOlustur("g$sira.jpg", 400, 300) { tuval ->
                tuval.drawColor(Color.rgb(40 * sira, 90, 200))
            }, "g$sira.jpg")
        }
        val cikti = File(dizin, "uc-sayfa.pdf")

        val sonuc = ResimdenPdf.olustur(girdiler, ResimdenPdfAyarlari(), cikti, geciciler)

        assertEquals(3, sonuc.sayfaSayisi)
        assertTrue(sonuc.atlananlar.isEmpty())
        assertEquals(3, BelgeErisimi.sayfaSayisi(cikti))
    }

    // =====================================================================
    // 2. EXIF donusu
    // =====================================================================

    @Test
    fun exifDonusuUygulanirVeSayfaYonuDuzelir() {
        // 200x100 YATAY gorsel; kirmizi blok SOL-UST ceyrekte.
        val kaynak = jpegOlustur("yatay.jpg", 200, 100) { tuval ->
            tuval.drawRect(0f, 0f, 100f, 50f, Paint().apply { color = Color.RED })
        }
        // EXIF: 6 = 90 derece saat yonunde dondur.
        ExifInterface(kaynak.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }

        val cikti = File(dizin, "donmus.pdf")
        ResimdenPdf.olustur(
            girdiler = listOf(GorselGirdisi(kaynak, "yatay.jpg")),
            ayarlar = ResimdenPdfAyarlari(
                duzen = SayfaDuzeni.GORUNTU_BOYUTU,
                dpi = 72,
                kalite = SikistirmaKalitesi.YUKSEK,
            ),
            cikti = cikti,
            gecicilerDizini = geciciler,
        )

        PdfRendererRasterlestirici().ac(cikti).use { oturum ->
            val boyut = oturum.noktaBoyutu(0)
            assertTrue(
                "Sayfa dikey olmaliydi (90 derece dondu): ${boyut.genislik}x${boyut.yukseklik}",
                boyut.yukseklik > boyut.genislik,
            )

            val goruntu = oturum.rasterlestir(0, 144)
            try {
                // 90 CW sonrasi sol-ust blok SAG-UST'e gelir.
                val sagUst = goruntu.getPixel(
                    (goruntu.width * 0.75f).toInt(),
                    (goruntu.height * 0.15f).toInt(),
                )
                val solUst = goruntu.getPixel(
                    (goruntu.width * 0.25f).toInt(),
                    (goruntu.height * 0.15f).toInt(),
                )
                assertTrue(
                    "Sağ üst kırmızı olmalıydı: ${renkYazi(sagUst)}",
                    Color.red(sagUst) > 150 && Color.green(sagUst) < 100 && Color.blue(sagUst) < 100,
                )
                assertTrue(
                    "Sol üst beyaz olmalıydı: ${renkYazi(solUst)}",
                    parlaklik(solUst) > 200,
                )
            } finally {
                goruntu.recycle()
            }
        }
    }

    // =====================================================================
    // 3. EXIF sizintisi
    // =====================================================================

    @Test
    fun exifVerisiCiktiyaSizmaz() {
        val kaynak = jpegOlustur("konumlu.jpg", 300, 200) { it.drawColor(Color.rgb(200, 200, 120)) }
        ExifInterface(kaynak.absolutePath).apply {
            setLatLong(41.0082, 28.9784) // Istanbul
            setAttribute(ExifInterface.TAG_MAKE, GIZLI_MARKA)
            setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "2019:07:04 13:37:00")
            saveAttributes()
        }

        // On kosul: kaynakta EXIF gercekten var.
        val kaynakBaytlar = kaynak.readBytes()
        assertTrue(
            "Test kurulumu hatalı: kaynakta EXIF yok",
            kaynakBaytlar.icerir("Exif") && kaynakBaytlar.icerir(GIZLI_MARKA),
        )

        val cikti = File(dizin, "temiz.pdf")
        ResimdenPdf.olustur(
            girdiler = listOf(GorselGirdisi(kaynak, "konumlu.jpg")),
            ayarlar = ResimdenPdfAyarlari(),
            cikti = cikti,
            gecicilerDizini = geciciler,
        )

        val ciktiBaytlar = cikti.readBytes()
        assertFalse(
            "EXIF cihaz bilgisi çıktıya sızdı",
            ciktiBaytlar.icerir(GIZLI_MARKA),
        )
        assertFalse("EXIF bölümü çıktıya sızdı", ciktiBaytlar.icerir("Exif"))
        assertFalse("GPS etiketi çıktıya sızdı", ciktiBaytlar.icerir("GPS"))

        // Belge meta verileri de temiz olmali.
        BelgeErisimi.ac(cikti).use { belge ->
            assertEquals(null, belge.documentInformation.author)
            assertEquals(null, belge.documentInformation.title)
        }
    }

    // =====================================================================
    // 4. Saydam PNG
    // =====================================================================

    @Test
    fun saydamPngBeyazaDuzlesirSiyahaDegil() {
        val kaynak = File(dizin, "saydam.png")
        val bitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
        // Tamamen saydam; ortasinda kirmizi kare.
        Canvas(bitmap).drawRect(50f, 50f, 70f, 70f, Paint().apply { color = Color.RED })
        kaynak.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()

        val cikti = File(dizin, "saydam.pdf")
        ResimdenPdf.olustur(
            girdiler = listOf(GorselGirdisi(kaynak, "saydam.png")),
            ayarlar = ResimdenPdfAyarlari(
                duzen = SayfaDuzeni.GORUNTU_BOYUTU,
                dpi = 72,
                kalite = SikistirmaKalitesi.YUKSEK,
            ),
            cikti = cikti,
            gecicilerDizini = geciciler,
        )

        PdfRendererRasterlestirici().ac(cikti).use { oturum ->
            val goruntu = oturum.rasterlestir(0, 144)
            try {
                val kose = goruntu.getPixel(
                    (goruntu.width * 0.1f).toInt(),
                    (goruntu.height * 0.1f).toInt(),
                )
                assertTrue(
                    "Saydam bölge siyah çıktı (JPEG'de alfa yok): ${renkYazi(kose)}",
                    parlaklik(kose) > 200,
                )
            } finally {
                goruntu.recycle()
            }
        }
    }

    // =====================================================================
    // 5. Bozuk dosya
    // =====================================================================

    @Test
    fun bozukDosyaAtlanirDigerleriIslenir() {
        val saglam1 = jpegOlustur("saglam1.jpg", 300, 200) { it.drawColor(Color.CYAN) }
        val bozuk = File(dizin, "bozuk.jpg").apply {
            writeBytes(ByteArray(4096) { (it % 251).toByte() })
        }
        val saglam2 = jpegOlustur("saglam2.jpg", 300, 200) { it.drawColor(Color.MAGENTA) }

        val cikti = File(dizin, "atlamali.pdf")
        val sonuc = ResimdenPdf.olustur(
            girdiler = listOf(
                GorselGirdisi(saglam1, "saglam1.jpg"),
                GorselGirdisi(bozuk, "bozuk.jpg"),
                GorselGirdisi(saglam2, "saglam2.jpg"),
            ),
            ayarlar = ResimdenPdfAyarlari(),
            cikti = cikti,
            gecicilerDizini = geciciler,
        )

        assertEquals("Sağlam görseller işlenmeliydi", 2, sonuc.sayfaSayisi)
        assertEquals(1, sonuc.atlananlar.size)
        assertEquals("bozuk.jpg", sonuc.atlananlar.first().ad)
        assertTrue(sonuc.atlananlar.first().neden.isNotBlank())
        assertEquals(2, BelgeErisimi.sayfaSayisi(cikti))
    }

    // =====================================================================
    // 6. Buyuk gorsel
    // =====================================================================

    @Test
    fun buyukGorselKucultulerekIslenir() {
        // 4000x3000 = 12 MP; ARGB_8888'de ham hali ~48 MB.
        val kaynak = jpegOlustur("buyuk.jpg", 4000, 3000) { tuval ->
            tuval.drawColor(Color.rgb(30, 120, 90))
        }
        val cikti = File(dizin, "buyuk.pdf")

        val sonuc = ResimdenPdf.olustur(
            girdiler = listOf(GorselGirdisi(kaynak, "buyuk.jpg")),
            ayarlar = ResimdenPdfAyarlari(
                duzen = SayfaDuzeni.GORUNTU_BOYUTU,
                dpi = 72,
                kalite = SikistirmaKalitesi.ORTA, // azamiKenar = 1600
            ),
            cikti = cikti,
            gecicilerDizini = geciciler,
        )

        assertEquals(1, sonuc.sayfaSayisi)
        PdfRendererRasterlestirici().ac(cikti).use { oturum ->
            val boyut = oturum.noktaBoyutu(0)
            // 72 DPI'da sayfa noktasi = piksel; en buyuk kenar 1600'u asmamali.
            assertTrue(
                "Görsel küçültülmemiş: ${boyut.genislik}x${boyut.yukseklik}",
                maxOf(boyut.genislik, boyut.yukseklik) <= 1610f,
            )
            // Oran korunmus olmali (4:3)
            val oran = boyut.genislik / boyut.yukseklik
            assertEquals(4f / 3f, oran, 0.02f)
        }
    }

    // =====================================================================
    // 7. Uctan uca: sec -> sirala -> PDF -> dogrula
    // =====================================================================

    @Test
    fun uctanUcaSecSiralaOlusturDogrula() {
        val a = jpegOlustur("a.jpg", 400, 300) { it.drawColor(Color.RED) }
        val b = jpegOlustur("b.jpg", 300, 400) { it.drawColor(Color.GREEN) }
        val c = jpegOlustur("c.jpg", 400, 400) { it.drawColor(Color.BLUE) }

        // Kullanicinin surukleyerek yaptigi siralamayi taklit ediyoruz: c, a, b
        val sirali = listOf(
            GorselGirdisi(c, "c.jpg"),
            GorselGirdisi(a, "a.jpg"),
            GorselGirdisi(b, "b.jpg"),
        )
        val cikti = File(dizin, "uctan-uca.pdf")

        val sonuc = ResimdenPdf.olustur(sirali, ResimdenPdfAyarlari(), cikti, geciciler)

        assertEquals(3, sonuc.sayfaSayisi)
        assertEquals(3, BelgeErisimi.sayfaSayisi(cikti))

        PdfRendererRasterlestirici().ac(cikti).use { oturum ->
            assertEquals(3, oturum.sayfaSayisi)
            // Ilk sayfa kare gorsel (c) -> A4 dikey sayfaya oturur.
            val ilk = oturum.noktaBoyutu(0)
            assertTrue("İlk sayfa dikey A4 olmalıydı", ilk.yukseklik > ilk.genislik)

            val goruntu = oturum.rasterlestir(0, 96)
            try {
                val orta = goruntu.getPixel(goruntu.width / 2, goruntu.height / 2)
                assertTrue(
                    "İlk sayfa mavi (c.jpg) olmalıydı: ${renkYazi(orta)}",
                    Color.blue(orta) > 150 && Color.red(orta) < 110,
                )
            } finally {
                goruntu.recycle()
            }
        }
    }

    // =====================================================================
    // yardimcilar
    // =====================================================================

    private fun jpegOlustur(ad: String, en: Int, boy: Int, ciz: (Canvas) -> Unit): File {
        val bitmap = Bitmap.createBitmap(en, boy, Bitmap.Config.ARGB_8888)
        val tuval = Canvas(bitmap)
        tuval.drawColor(Color.WHITE)
        ciz(tuval)
        val dosya = File(dizin, ad)
        dosya.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()
        return dosya
    }

    private fun parlaklik(piksel: Int): Double =
        Color.red(piksel) * 0.299 + Color.green(piksel) * 0.587 + Color.blue(piksel) * 0.114

    private fun renkYazi(piksel: Int): String =
        "rgb(${Color.red(piksel)},${Color.green(piksel)},${Color.blue(piksel)})"

    private fun ByteArray.icerir(metin: String): Boolean {
        val hedef = metin.toByteArray(Charsets.US_ASCII)
        if (hedef.isEmpty() || hedef.size > size) return false
        dis@ for (bas in 0..size - hedef.size) {
            for (kayma in hedef.indices) {
                if (this[bas + kayma] != hedef[kayma]) continue@dis
            }
            return true
        }
        return false
    }

    private companion object {
        const val GIZLI_MARKA = "TESTCIHAZ-GIZLI-MARKA"
    }
}
