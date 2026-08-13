package com.yerel.pdfkutusu

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.yerel.pdfkutusu.cekirdek.DosyaAdi
import com.yerel.pdfkutusu.cekirdek.IslemTuru
import com.yerel.pdfkutusu.depo.CalismaAlani
import com.yerel.pdfkutusu.pdf.BelgeErisimi
import com.yerel.pdfkutusu.pdf.BirlestirmeGirdisi
import com.yerel.pdfkutusu.pdf.PdfBirlestirici
import com.yerel.pdfkutusu.pdf.PdfMetinCikarici
import com.yerel.pdfkutusu.pdf.PdfRendererRasterlestirici
import com.yerel.pdfkutusu.test.TestPdfUretici
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Uctan uca mutlu yol (cihaz uzerinde).
 *
 * Iki PDF sec -> birlestir -> onizlemeyi dogrula -> disa aktar ->
 * cikti dosyasinin var oldugunu ve sayfa sayisinin dogru oldugunu dogrula.
 *
 * Onizleme adimi bilerek **gercek** [android.graphics.pdf.PdfRenderer] ile
 * yapiliyor: birlesmis dosya gercekten acilabiliyor ve cizilebiliyor mu,
 * ancak boyle anlasilir.
 *
 * Dosya secme/kaydetme adimlarinda SAF'in sistem secicisi yerine
 * `ContentResolver`'in `file://` destegi kullaniliyor; uygulamanin gectigi
 * kod yolu (URI -> akis -> kopya) aynen calisiyor.
 */
@RunWith(AndroidJUnit4::class)
class BirlestirmeUctanUcaTesti {

    private lateinit var baglam: Context
    private lateinit var calismaAlani: CalismaAlani
    private lateinit var kaynakDizin: File
    private lateinit var disaAktarimDizini: File

    @Before
    fun hazirla() {
        baglam = ApplicationProvider.getApplicationContext()
        PDFBoxResourceLoader.init(baglam)
        calismaAlani = CalismaAlani(baglam)
        calismaAlani.calismaGirdileriniTemizle()
        calismaAlani.tumCiktilariSil()

        kaynakDizin = File(baglam.cacheDir, "test-kaynaklar").apply {
            deleteRecursively()
            mkdirs()
        }
        disaAktarimDizini = File(
            baglam.getExternalFilesDir(null) ?: baglam.filesDir,
            "test-disa-aktarim",
        ).apply {
            deleteRecursively()
            mkdirs()
        }
    }

    @Test
    fun ikiPdfBirlestirilirOnizlenirVeDisaAktarilir() = runBlocking {
        // ---------------------------------------------------------- 1. hazirlik
        val kaynakA = TestPdfUretici.olustur(
            File(kaynakDizin, "Çalışma Belgesi A.pdf"),
            listOf("BELGE A SAYFA 1", "BELGE A SAYFA 2"),
        )
        val kaynakB = TestPdfUretici.olustur(
            File(kaynakDizin, "Belge B.pdf"),
            listOf("BELGE B SAYFA 1", "BELGE B SAYFA 2", "BELGE B SAYFA 3"),
        )

        // ------------------------------------------------- 2. iki PDF'i "sec"
        val girdiA = calismaAlani.iceriAktar(Uri.fromFile(kaynakA))
        val girdiB = calismaAlani.iceriAktar(Uri.fromFile(kaynakB))

        assertEquals("Çalışma Belgesi A.pdf", girdiA.gorunenAd)
        assertEquals("Belge B.pdf", girdiB.gorunenAd)
        assertNotEquals(
            "Girdi kopyalanmali, kaynak dosyanin kendisi kullanilmamali",
            kaynakA.absolutePath,
            girdiA.dosya.absolutePath,
        )
        assertEquals(2, BelgeErisimi.sayfaSayisi(girdiA.dosya))
        assertEquals(3, BelgeErisimi.sayfaSayisi(girdiB.dosya))

        // ------------------------------------------------------- 3. birlestir
        val ciktiAdi = DosyaAdi.cikti(girdiA.gorunenAd, IslemTuru.BIRLESTIR)
        val cikti = calismaAlani.ciktiDosyasi(ciktiAdi)
        val sayfaSayisi = PdfBirlestirici.birlestir(
            girdiler = listOf(
                BirlestirmeGirdisi(girdiA.dosya, girdiA.gorunenAd),
                BirlestirmeGirdisi(girdiB.dosya, girdiB.gorunenAd),
            ),
            cikti = cikti,
        )

        assertEquals(5, sayfaSayisi)
        assertTrue("Cikti dosyasi olusmadi", cikti.exists() && cikti.length() > 0)
        assertTrue(
            "Cikti adi sartnamedeki bicimde degil: " + cikti.name,
            Regex(""".+__birlestir__\d{8}-\d{6}\.pdf""").matches(cikti.name),
        )

        // -------------------------------------- 4. onizlemeyi dogrula (gercek)
        PdfRendererRasterlestirici().ac(cikti).use { oturum ->
            assertEquals("PdfRenderer farkli sayfa sayisi gordu", 5, oturum.sayfaSayisi)

            val ilkBoyut = oturum.noktaBoyutu(0)
            assertTrue(ilkBoyut.genislik > 0f && ilkBoyut.yukseklik > 0f)

            for (indeks in 0 until oturum.sayfaSayisi) {
                val kucukResim = oturum.rasterlestir(indeks, 96)
                assertTrue(
                    "Sayfa " + indeks + " cizilemedi",
                    kucukResim.width > 0 && kucukResim.height > 0,
                )
                kucukResim.recycle()
            }
        }

        // --------------------------------------------------------- 5. disa aktar
        val hedefDosya = File(disaAktarimDizini, ciktiAdi)
        calismaAlani.disaAktar(cikti, Uri.fromFile(hedefDosya))

        // ------------------------------------------------------------ 6. dogrula
        assertTrue("Disa aktarilan dosya yok", hedefDosya.exists())
        assertEquals(
            "Disa aktarilan dosya boyutu farkli",
            cikti.length(),
            hedefDosya.length(),
        )
        assertEquals(
            "Disa aktarilan dosyanin sayfa sayisi yanlis",
            5,
            BelgeErisimi.sayfaSayisi(hedefDosya),
        )

        val metin = PdfMetinCikarici.cikar(hedefDosya)
        assertTrue(metin.contains("BELGE A SAYFA 1"))
        assertTrue(metin.contains("BELGE B SAYFA 3"))

        // Orijinaller dokunulmamis olmali
        assertEquals(2, BelgeErisimi.sayfaSayisi(kaynakA))
        assertEquals(3, BelgeErisimi.sayfaSayisi(kaynakB))
    }
}
