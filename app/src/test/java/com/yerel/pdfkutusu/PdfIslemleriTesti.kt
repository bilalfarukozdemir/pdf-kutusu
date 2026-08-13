package com.yerel.pdfkutusu

import androidx.test.core.app.ApplicationProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.yerel.pdfkutusu.cekirdek.Ozet
import com.yerel.pdfkutusu.cekirdek.PdfHatasi
import com.yerel.pdfkutusu.pdf.BelgeErisimi
import com.yerel.pdfkutusu.pdf.BirlestirmeGirdisi
import com.yerel.pdfkutusu.pdf.PdfBirlestirici
import com.yerel.pdfkutusu.pdf.PdfBolucu
import com.yerel.pdfkutusu.pdf.PdfDondurucu
import com.yerel.pdfkutusu.pdf.PdfMetinCikarici
import com.yerel.pdfkutusu.pdf.PdfSiralayici
import com.yerel.pdfkutusu.test.TestPdfUretici
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * PDF islemlerinin cekirdek davranislari.
 *
 * Robolectric altinda calisir: PdfBox-Android font metriklerini AAR
 * assets'inden okudugu icin gercek bir Android baglami gerekiyor.
 */
@RunWith(RobolectricTestRunner::class)
class PdfIslemleriTesti {

    @get:Rule
    val gecici = TemporaryFolder()

    @Before
    fun hazirla() {
        PDFBoxResourceLoader.init(ApplicationProvider.getApplicationContext())
    }

    private fun pdf(ad: String, vararg sayfalar: String): File =
        TestPdfUretici.olustur(gecici.newFile(ad), sayfalar.toList())

    private fun ciktiDosyasi(ad: String): File = File(gecici.root, ad)

    // =====================================================================
    // BIRLESTIRME
    // =====================================================================

    @Test
    fun `birlestirme sonrasi sayfa sayisi girdilerin toplamidir`() {
        val a = pdf("a.pdf", "A1", "A2")
        val b = pdf("b.pdf", "B1", "B2", "B3")
        val c = pdf("c.pdf", "C1")
        val cikti = ciktiDosyasi("birlesik.pdf")

        val sayfaSayisi = PdfBirlestirici.birlestir(
            girdiler = listOf(
                BirlestirmeGirdisi(a, "a.pdf"),
                BirlestirmeGirdisi(b, "b.pdf"),
                BirlestirmeGirdisi(c, "c.pdf"),
            ),
            cikti = cikti,
        )

        assertEquals(6, sayfaSayisi)
        assertEquals(6, BelgeErisimi.sayfaSayisi(cikti))
    }

    @Test
    fun `birlestirme sirayi korur`() {
        val a = pdf("a.pdf", "BIRINCI")
        val b = pdf("b.pdf", "IKINCI")
        val cikti = ciktiDosyasi("sirali.pdf")

        PdfBirlestirici.birlestir(
            listOf(BirlestirmeGirdisi(b, "b.pdf"), BirlestirmeGirdisi(a, "a.pdf")),
            cikti,
        )

        BelgeErisimi.ac(cikti).use { belge ->
            assertTrue(PdfMetinCikarici.sayfadanCikar(belge, 0).contains("IKINCI"))
            assertTrue(PdfMetinCikarici.sayfadanCikar(belge, 1).contains("BIRINCI"))
        }
    }

    @Test
    fun `birlestirme kaynak dosyalari degistirmez`() {
        val a = pdf("a.pdf", "A1", "A2")
        val b = pdf("b.pdf", "B1")
        val aOzetOnce = Ozet.sha256(a)
        val bOzetOnce = Ozet.sha256(b)

        PdfBirlestirici.birlestir(
            listOf(BirlestirmeGirdisi(a, "a.pdf"), BirlestirmeGirdisi(b, "b.pdf")),
            ciktiDosyasi("cikti.pdf"),
        )

        assertEquals("Kaynak a.pdf degistirildi", aOzetOnce, Ozet.sha256(a))
        assertEquals("Kaynak b.pdf degistirildi", bOzetOnce, Ozet.sha256(b))
    }

    @Test
    fun `tek dosyayla birlestirme sayfa sayisini korur`() {
        val a = pdf("a.pdf", "A1", "A2", "A3")
        val cikti = ciktiDosyasi("tek.pdf")
        assertEquals(3, PdfBirlestirici.birlestir(listOf(BirlestirmeGirdisi(a, "a.pdf")), cikti))
    }

    @Test
    fun `bos girdi listesi hata verir`() {
        assertThrows(PdfHatasi.GirdiYok::class.java) {
            PdfBirlestirici.birlestir(emptyList(), ciktiDosyasi("olmaz.pdf"))
        }
    }

    // =====================================================================
    // BOLME
    // =====================================================================

    @Test
    fun `bolme secilen araligi cikarir`() {
        val kaynak = pdf("k.pdf", "S1", "S2", "S3", "S4", "S5")
        val cikti = ciktiDosyasi("bolunmus.pdf")

        val parca = PdfBolucu.tekDosyayaCikar(kaynak, "2-4", cikti)

        assertEquals(3, parca.sayfaSayisi)
        assertEquals(3, BelgeErisimi.sayfaSayisi(cikti))
        assertEquals(listOf(1, 2, 3), parca.sayfaIndeksleri)
        BelgeErisimi.ac(cikti).use { belge ->
            assertTrue(PdfMetinCikarici.sayfadanCikar(belge, 0).contains("S2"))
            assertTrue(PdfMetinCikarici.sayfadanCikar(belge, 2).contains("S4"))
        }
    }

    @Test
    fun `bolme sinir durumu tek sayfa`() {
        val kaynak = pdf("k.pdf", "S1", "S2", "S3")
        val cikti = ciktiDosyasi("tek-sayfa.pdf")
        val parca = PdfBolucu.tekDosyayaCikar(kaynak, "2", cikti)

        assertEquals(1, parca.sayfaSayisi)
        assertEquals(1, BelgeErisimi.sayfaSayisi(cikti))
        BelgeErisimi.ac(cikti).use { belge ->
            assertTrue(PdfMetinCikarici.sayfadanCikar(belge, 0).contains("S2"))
        }
    }

    @Test
    fun `bolme sinir durumu tum sayfalar`() {
        val kaynak = pdf("k.pdf", "S1", "S2", "S3")
        val cikti = ciktiDosyasi("hepsi.pdf")
        assertEquals(3, PdfBolucu.tekDosyayaCikar(kaynak, "1-3", cikti).sayfaSayisi)
        assertEquals(3, BelgeErisimi.sayfaSayisi(cikti))
    }

    @Test
    fun `bolme sinir durumu tek sayfalik belge`() {
        val kaynak = pdf("k.pdf", "TEK")
        val cikti = ciktiDosyasi("tekli.pdf")
        assertEquals(1, PdfBolucu.tekDosyayaCikar(kaynak, "1", cikti).sayfaSayisi)
    }

    @Test
    fun `bolme gecersiz aralikta hata verir`() {
        val kaynak = pdf("k.pdf", "S1", "S2", "S3")

        assertThrows(PdfHatasi.GecersizAralik::class.java) {
            PdfBolucu.tekDosyayaCikar(kaynak, "4", ciktiDosyasi("x1.pdf"))
        }
        assertThrows(PdfHatasi.GecersizAralik::class.java) {
            PdfBolucu.tekDosyayaCikar(kaynak, "0-2", ciktiDosyasi("x2.pdf"))
        }
        assertThrows(PdfHatasi.GecersizAralik::class.java) {
            PdfBolucu.tekDosyayaCikar(kaynak, "3-1", ciktiDosyasi("x3.pdf"))
        }
        assertThrows(PdfHatasi.GecersizAralik::class.java) {
            PdfBolucu.tekDosyayaCikar(kaynak, "", ciktiDosyasi("x4.pdf"))
        }
    }

    @Test
    fun `bolme her araligi ayri dosyaya yazar`() {
        val kaynak = pdf("k.pdf", "S1", "S2", "S3", "S4", "S5", "S6")

        val parcalar = PdfBolucu.herAraligiAyriDosyaya(
            kaynak = kaynak,
            aralikIfadesi = "1-2, 4, 5-6",
            ciktiAdiUret = { sira, _ -> ciktiDosyasi("parca-$sira.pdf") },
        )

        assertEquals(3, parcalar.size)
        assertEquals(2, parcalar[0].sayfaSayisi)
        assertEquals(1, parcalar[1].sayfaSayisi)
        assertEquals(2, parcalar[2].sayfaSayisi)
        parcalar.forEach { assertTrue(it.dosya.exists() && it.dosya.length() > 0) }
        assertEquals(2, BelgeErisimi.sayfaSayisi(parcalar[0].dosya))
        assertEquals(1, BelgeErisimi.sayfaSayisi(parcalar[1].dosya))
    }

    @Test
    fun `bolme kaynak dosyayi degistirmez`() {
        val kaynak = pdf("k.pdf", "S1", "S2", "S3")
        val ozetOnce = Ozet.sha256(kaynak)
        PdfBolucu.tekDosyayaCikar(kaynak, "1-2", ciktiDosyasi("b.pdf"))
        assertEquals(ozetOnce, Ozet.sha256(kaynak))
    }

    // =====================================================================
    // DONDURME
    // =====================================================================

    @Test
    fun `dondurme acisi tum sayfalara uygulanir`() {
        val kaynak = pdf("k.pdf", "S1", "S2", "S3")
        val cikti = ciktiDosyasi("donmus.pdf")

        assertEquals(3, PdfDondurucu.dondur(kaynak, 90, cikti))

        BelgeErisimi.ac(cikti).use { belge ->
            for (i in 0 until belge.numberOfPages) {
                assertEquals("Sayfa $i", 90, belge.getPage(i).rotation)
            }
        }
    }

    @Test
    fun `dondurme mevcut aciya eklenir`() {
        val kaynak = pdf("k.pdf", "S1")
        val birKez = ciktiDosyasi("bir.pdf")
        val ikiKez = ciktiDosyasi("iki.pdf")

        PdfDondurucu.dondur(kaynak, 90, birKez)
        PdfDondurucu.dondur(birKez, 180, ikiKez)

        BelgeErisimi.ac(ikiKez).use { belge ->
            assertEquals(270, belge.getPage(0).rotation)
        }
    }

    @Test
    fun `dondurme 360 dereceyi sifira indirger`() {
        val kaynak = pdf("k.pdf", "S1")
        val ilk = ciktiDosyasi("d1.pdf")
        val ikinci = ciktiDosyasi("d2.pdf")

        PdfDondurucu.dondur(kaynak, 270, ilk)
        PdfDondurucu.dondur(ilk, 90, ikinci)

        BelgeErisimi.ac(ikinci).use { belge ->
            assertEquals(0, belge.getPage(0).rotation)
        }
    }

    @Test
    fun `dondurme yalnizca secilen sayfalari etkiler`() {
        val kaynak = pdf("k.pdf", "S1", "S2", "S3")
        val cikti = ciktiDosyasi("kismi.pdf")

        assertEquals(1, PdfDondurucu.dondur(kaynak, 90, cikti, sayfaIndeksleri = listOf(1)))

        BelgeErisimi.ac(cikti).use { belge ->
            assertEquals(0, belge.getPage(0).rotation)
            assertEquals(90, belge.getPage(1).rotation)
            assertEquals(0, belge.getPage(2).rotation)
        }
    }

    @Test
    fun `dondurme 90in kati olmayan aciyi reddeder`() {
        val kaynak = pdf("k.pdf", "S1")
        assertThrows(PdfHatasi.GecersizAralik::class.java) {
            PdfDondurucu.dondur(kaynak, 45, ciktiDosyasi("olmaz.pdf"))
        }
    }

    @Test
    fun `dondurme normalize dogru calisir`() {
        assertEquals(0, PdfDondurucu.normalize(0))
        assertEquals(90, PdfDondurucu.normalize(90))
        assertEquals(0, PdfDondurucu.normalize(360))
        assertEquals(90, PdfDondurucu.normalize(450))
        assertEquals(270, PdfDondurucu.normalize(-90))
        assertEquals(180, PdfDondurucu.normalize(-180))
        assertEquals(90, PdfDondurucu.normalize(-270))
    }

    // =====================================================================
    // SIRALAMA
    // =====================================================================

    @Test
    fun `siralama yeni duzeni uygular`() {
        val kaynak = pdf("k.pdf", "BIR", "IKI", "UC")
        val cikti = ciktiDosyasi("sirali.pdf")

        assertEquals(3, PdfSiralayici.sirala(kaynak, listOf(2, 0, 1), cikti))

        BelgeErisimi.ac(cikti).use { belge ->
            assertTrue(PdfMetinCikarici.sayfadanCikar(belge, 0).contains("UC"))
            assertTrue(PdfMetinCikarici.sayfadanCikar(belge, 1).contains("BIR"))
            assertTrue(PdfMetinCikarici.sayfadanCikar(belge, 2).contains("IKI"))
        }
    }

    @Test
    fun `siralama sayfa cikarabilir`() {
        val kaynak = pdf("k.pdf", "BIR", "IKI", "UC")
        val cikti = ciktiDosyasi("eksik.pdf")

        assertEquals(2, PdfSiralayici.sirala(kaynak, listOf(0, 2), cikti))

        BelgeErisimi.ac(cikti).use { belge ->
            assertEquals(2, belge.numberOfPages)
            assertTrue(PdfMetinCikarici.cikar(belge).contains("BIR"))
            assertTrue(PdfMetinCikarici.cikar(belge).contains("UC"))
            assertTrue(!PdfMetinCikarici.cikar(belge).contains("IKI"))
        }
    }

    @Test
    fun `siralama gecersiz indeksi reddeder`() {
        val kaynak = pdf("k.pdf", "BIR", "IKI")
        assertThrows(PdfHatasi.GecersizAralik::class.java) {
            PdfSiralayici.sirala(kaynak, listOf(0, 5), ciktiDosyasi("olmaz.pdf"))
        }
        assertThrows(PdfHatasi.GecersizAralik::class.java) {
            PdfSiralayici.sirala(kaynak, emptyList(), ciktiDosyasi("olmaz2.pdf"))
        }
    }

    // =====================================================================
    // META VERI
    // =====================================================================

    @Test
    fun `cikti meta verileri temizlenir`() {
        val kaynak = pdf("k.pdf", "S1")
        // Kaynaga kimlik bilgisi koy
        BelgeErisimi.ac(kaynak).use { belge ->
            belge.documentInformation.author = "Gizli Yazar"
            belge.documentInformation.title = "Gizli Baslik"
            belge.save(kaynak)
        }
        BelgeErisimi.ac(kaynak).use { belge ->
            assertEquals("Gizli Yazar", belge.documentInformation.author)
        }

        val cikti = ciktiDosyasi("temiz.pdf")
        PdfBolucu.tekDosyayaCikar(kaynak, "1", cikti)

        BelgeErisimi.ac(cikti).use { belge ->
            assertEquals(null, belge.documentInformation.author)
            assertEquals(null, belge.documentInformation.title)
            assertNotEquals("Gizli Baslik", belge.documentInformation.title)
        }
    }
}
