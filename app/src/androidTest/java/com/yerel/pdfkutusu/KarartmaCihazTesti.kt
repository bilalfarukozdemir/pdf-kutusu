package com.yerel.pdfkutusu

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.yerel.pdfkutusu.cekirdek.Ozet
import com.yerel.pdfkutusu.pdf.KarartmaAlani
import com.yerel.pdfkutusu.pdf.PdfKartici
import com.yerel.pdfkutusu.pdf.PdfMetinCikarici
import com.yerel.pdfkutusu.pdf.PdfRendererRasterlestirici
import com.yerel.pdfkutusu.test.TestPdfUretici
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ZORUNLU TESTIN CIHAZ UZERINDEKI KARSILIGI.
 *
 * Birim testi ([KarartmaMetinYoklugTesti]) boru hattini sahte bir
 * rasterlestiriciyle dogruluyor. Burada ayni dogrulama **gercek**
 * `android.graphics.pdf.PdfRenderer` ile yapiliyor; ustune bir de karartilan
 * bolgenin piksellerinin gercekten siyah oldugu olculuyor.
 */
@RunWith(AndroidJUnit4::class)
class KarartmaCihazTesti {

    private val gizliDize = "12345678901"
    private val ikinciSayfaMetni = "IKINCI SAYFA KALSIN"

    private lateinit var baglam: Context
    private lateinit var calismaDizini: File

    @Before
    fun hazirla() {
        baglam = ApplicationProvider.getApplicationContext()
        PDFBoxResourceLoader.init(baglam)
        calismaDizini = File(baglam.cacheDir, "karartma-testi").apply {
            deleteRecursively()
            mkdirs()
        }
    }

    @Test
    fun gercekRendererIleKarartilanDizeCiktidaBulunmaz() {
        val kaynak = TestPdfUretici.olustur(
            File(calismaDizini, "gizli.pdf"),
            listOf(gizliDize, ikinciSayfaMetni),
        )
        val kaynakOzeti = Ozet.sha256(kaynak)

        // On kosul: dize kaynakta gercekten okunabiliyor.
        assertTrue(
            "Test kurulumu hatali: dize kaynakta yok",
            PdfMetinCikarici.cikar(kaynak).contains(gizliDize),
        )

        val band = TestPdfUretici.metinBandi()
        val cikti = File(calismaDizini, "karartilmis.pdf")

        val sonuc = PdfKartici.karart(
            kaynak = kaynak,
            alanlar = listOf(KarartmaAlani(0, band[0], band[1], band[2], band[3])),
            cikti = cikti,
            rasterlestirici = PdfRendererRasterlestirici(),
            gecicilerDizini = calismaDizini,
            dpi = PdfKartici.ASGARI_DPI,
        )

        // ---- 1) Metin gercekten yok ----
        val ciktiMetni = PdfMetinCikarici.cikar(cikti)
        assertFalse(
            "KARARTMA BASARISIZ: dize ciktidan okunabiliyor -> " + ciktiMetni.take(200),
            ciktiMetni.contains(gizliDize),
        )

        // ---- 2) Karartilmayan sayfa dokunulmadi ----
        assertTrue(
            "Karartilmayan sayfanin metni de gitmis",
            ciktiMetni.contains(ikinciSayfaMetni),
        )
        assertEquals(2, sonuc.toplamSayfa)
        assertEquals(listOf(0), sonuc.karartilanSayfalar)
        assertEquals(PdfKartici.ASGARI_DPI, sonuc.kullanilanDpi)

        // ---- 3) Orijinal degismedi ----
        assertEquals("Kaynak dosya degistirildi", kaynakOzeti, Ozet.sha256(kaynak))

        // ---- 4) Karartilan bolgenin pikselleri gercekten siyah ----
        PdfRendererRasterlestirici().ac(cikti).use { oturum ->
            val goruntu = oturum.rasterlestir(0, 150)
            try {
                val orta = ((band[1] + band[3]) / 2f)
                val y = (goruntu.height * orta).toInt().coerceIn(0, goruntu.height - 1)
                // Bandin genisligi boyunca birkac noktadan orneklem al.
                for (oran in listOf(0.2f, 0.4f, 0.5f, 0.6f, 0.8f)) {
                    val x = (goruntu.width * oran).toInt().coerceIn(0, goruntu.width - 1)
                    val piksel = goruntu.getPixel(x, y)
                    val parlaklik = (
                        Color.red(piksel) * 0.299 +
                            Color.green(piksel) * 0.587 +
                            Color.blue(piksel) * 0.114
                        )
                    assertTrue(
                        "Karartilan bolgede beklenen siyah yok (x=" + x + ", y=" + y +
                            ", parlaklik=" + parlaklik + ")",
                        parlaklik < 48.0,
                    )
                }

                // Sayfanin altinda (karartilmayan bolge) hala beyaz kagit olmali:
                // tum sayfayi karartmadigimizi kanitlar.
                val altY = (goruntu.height * 0.9f).toInt().coerceIn(0, goruntu.height - 1)
                val altPiksel = goruntu.getPixel(goruntu.width / 2, altY)
                val altParlaklik = (
                    Color.red(altPiksel) * 0.299 +
                        Color.green(altPiksel) * 0.587 +
                        Color.blue(altPiksel) * 0.114
                    )
                assertTrue(
                    "Sayfanin tamami karartilmis gorunuyor (alt parlaklik=" + altParlaklik + ")",
                    altParlaklik > 200.0,
                )
            } finally {
                goruntu.recycle()
            }
        }

        // ---- 5) Karartilan sayfada hic metin akisi kalmadi ----
        com.yerel.pdfkutusu.pdf.BelgeErisimi.ac(cikti).use { belge ->
            val ilkSayfa = PdfMetinCikarici.sayfadanCikar(belge, 0).trim()
            assertTrue(
                "Karartilan sayfada metin akisi kalmis: " + ilkSayfa,
                ilkSayfa.isEmpty(),
            )
            // Meta veri temizligi
            assertEquals(null, belge.documentInformation.author)
            assertEquals(null, belge.documentInformation.title)
        }
    }
}
