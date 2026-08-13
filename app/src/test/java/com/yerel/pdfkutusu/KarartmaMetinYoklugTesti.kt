package com.yerel.pdfkutusu

import androidx.test.core.app.ApplicationProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.yerel.pdfkutusu.cekirdek.Ozet
import com.yerel.pdfkutusu.cekirdek.PdfHatasi
import com.yerel.pdfkutusu.pdf.BelgeErisimi
import com.yerel.pdfkutusu.pdf.KarartmaAlani
import com.yerel.pdfkutusu.pdf.PdfKartici
import com.yerel.pdfkutusu.pdf.PdfMetinCikarici
import com.yerel.pdfkutusu.test.SahteRasterlestirici
import com.yerel.pdfkutusu.test.TestPdfUretici
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * ZORUNLU TEST — karartma metin yoklugu.
 *
 * Sartname: "Icinde bilinen bir dize (orn. 12345678901) olan bir PDF olustur,
 * o alani karart, ciktidan metin cikar, dizenin BULUNMADIGINI dogrula."
 *
 * Bu test, sahte karartma (sayfanin ustune siyah dikdortgen cizme) yontemini
 * yakalar: o yontemde metin icerik akisinda kalir ve [PdfMetinCikarici] onu
 * bulmaya devam eder; test kirmizi olur.
 *
 * Rasterlestirici enjekte edilebilir oldugu icin bu dogrulama JVM'de kosuyor.
 * Gercek `PdfRenderer` ile ayni dogrulama cihaz uzerinde de yapiliyor:
 * `app/src/androidTest/.../KarartmaCihazTesti.kt`.
 */
@RunWith(RobolectricTestRunner::class)
class KarartmaMetinYoklugTesti {

    @get:Rule
    val gecici = TemporaryFolder()

    private val gizliDize = "12345678901"
    private val ikinciSayfaMetni = "IKINCI SAYFA KALSIN"

    private lateinit var gecicilerDizini: File

    @Before
    fun hazirla() {
        PDFBoxResourceLoader.init(ApplicationProvider.getApplicationContext())
        gecicilerDizini = gecici.newFolder("gecici")
    }

    private fun kaynakOlustur(): File = TestPdfUretici.olustur(
        gecici.newFile("kaynak.pdf"),
        listOf(gizliDize, ikinciSayfaMetni),
    )

    private fun metinBandiAlani(sayfa: Int): KarartmaAlani {
        val band = TestPdfUretici.metinBandi()
        return KarartmaAlani(sayfa, band[0], band[1], band[2], band[3])
    }

    // =====================================================================

    @Test
    fun `karartilan sayfada gizli dize ciktida bulunmaz`() {
        val kaynak = kaynakOlustur()

        // On kosul: dize gercekten kaynakta okunabiliyor olmali.
        val kaynakMetni = PdfMetinCikarici.cikar(kaynak)
        assertTrue(
            "Test kurulumu hatali: gizli dize kaynakta okunamiyor",
            kaynakMetni.contains(gizliDize),
        )

        val cikti = File(gecici.root, "karartilmis.pdf")
        val sonuc = PdfKartici.karart(
            kaynak = kaynak,
            alanlar = listOf(metinBandiAlani(sayfa = 0)),
            cikti = cikti,
            rasterlestirici = SahteRasterlestirici(),
            gecicilerDizini = gecicilerDizini,
        )

        assertNotNull(sonuc)
        assertTrue("Cikti dosyasi uretilmedi", cikti.exists() && cikti.length() > 0)

        // ---- ASIL DOGRULAMA ----
        val ciktiMetni = PdfMetinCikarici.cikar(cikti)
        assertFalse(
            "KARARTMA BASARISIZ: gizli dize ciktidan hala okunabiliyor. " +
                "Cikan metin: " + ciktiMetni.take(200),
            ciktiMetni.contains(gizliDize),
        )
    }

    @Test
    fun `karartilan sayfada hicbir metin kalmaz`() {
        val kaynak = kaynakOlustur()
        val cikti = File(gecici.root, "karartilmis2.pdf")

        PdfKartici.karart(
            kaynak = kaynak,
            alanlar = listOf(metinBandiAlani(0)),
            cikti = cikti,
            rasterlestirici = SahteRasterlestirici(),
            gecicilerDizini = gecicilerDizini,
        )

        BelgeErisimi.ac(cikti).use { belge ->
            val ilkSayfaMetni = PdfMetinCikarici.sayfadanCikar(belge, 0).trim()
            assertTrue(
                "Karartilan sayfa gorsele cevrilmeliydi, metin akisi kalmis: " + ilkSayfaMetni,
                ilkSayfaMetni.isEmpty(),
            )
        }
    }

    @Test
    fun `karartilmayan sayfalar dokunulmadan kalir`() {
        val kaynak = kaynakOlustur()
        val cikti = File(gecici.root, "kismi.pdf")

        val sonuc = PdfKartici.karart(
            kaynak = kaynak,
            alanlar = listOf(metinBandiAlani(0)),
            cikti = cikti,
            rasterlestirici = SahteRasterlestirici(),
            gecicilerDizini = gecicilerDizini,
        )

        assertEquals(2, sonuc.toplamSayfa)
        assertEquals(listOf(0), sonuc.karartilanSayfalar)
        assertEquals(2, BelgeErisimi.sayfaSayisi(cikti))

        BelgeErisimi.ac(cikti).use { belge ->
            val ikinci = PdfMetinCikarici.sayfadanCikar(belge, 1)
            assertTrue(
                "Karartilmayan sayfanin metni de silinmis: " + ikinci,
                ikinci.contains(ikinciSayfaMetni),
            )
        }
    }

    @Test
    fun `orijinal dosya degistirilmez`() {
        val kaynak = kaynakOlustur()
        val ozetOnce = Ozet.sha256(kaynak)

        PdfKartici.karart(
            kaynak = kaynak,
            alanlar = listOf(metinBandiAlani(0)),
            cikti = File(gecici.root, "c.pdf"),
            rasterlestirici = SahteRasterlestirici(),
            gecicilerDizini = gecicilerDizini,
        )

        assertEquals("Kaynak dosya degistirildi", ozetOnce, Ozet.sha256(kaynak))
        assertTrue(PdfMetinCikarici.cikar(kaynak).contains(gizliDize))
    }

    @Test
    fun `cikti meta verileri temizlenir`() {
        val kaynak = kaynakOlustur()
        BelgeErisimi.ac(kaynak).use { belge ->
            belge.documentInformation.author = "Gizli Yazar"
            belge.documentInformation.title = "Gizli Baslik"
            belge.documentInformation.creator = "Gizli Uretici"
            belge.save(kaynak)
        }

        val cikti = File(gecici.root, "metasiz.pdf")
        PdfKartici.karart(
            kaynak = kaynak,
            alanlar = listOf(metinBandiAlani(0)),
            cikti = cikti,
            rasterlestirici = SahteRasterlestirici(),
            gecicilerDizini = gecicilerDizini,
        )

        BelgeErisimi.ac(cikti).use { belge ->
            assertEquals(null, belge.documentInformation.author)
            assertEquals(null, belge.documentInformation.title)
            assertEquals(null, belge.documentInformation.creator)
            assertEquals(null, belge.documentCatalog.metadata)
        }
    }

    @Test
    fun `sayfa boyutu korunur`() {
        val kaynak = kaynakOlustur()
        val cikti = File(gecici.root, "boyut.pdf")

        PdfKartici.karart(
            kaynak = kaynak,
            alanlar = listOf(metinBandiAlani(0)),
            cikti = cikti,
            rasterlestirici = SahteRasterlestirici(),
            gecicilerDizini = gecicilerDizini,
        )

        BelgeErisimi.ac(cikti).use { belge ->
            val kutu = belge.getPage(0).mediaBox
            assertEquals(TestPdfUretici.SAYFA_BOYUTU.width, kutu.width, 1f)
            assertEquals(TestPdfUretici.SAYFA_BOYUTU.height, kutu.height, 1f)
        }
    }

    @Test
    fun `cozunurluk 200 DPI altina inemez`() {
        val kaynak = kaynakOlustur()
        val cikti = File(gecici.root, "dpi.pdf")

        val sonuc = PdfKartici.karart(
            kaynak = kaynak,
            alanlar = listOf(metinBandiAlani(0)),
            cikti = cikti,
            rasterlestirici = SahteRasterlestirici(),
            gecicilerDizini = gecicilerDizini,
            dpi = 50,
        )

        assertEquals(PdfKartici.ASGARI_DPI, sonuc.kullanilanDpi)
    }

    @Test
    fun `alan secilmezse hata verir`() {
        val kaynak = kaynakOlustur()
        assertThrows(PdfHatasi.GirdiYok::class.java) {
            PdfKartici.karart(
                kaynak = kaynak,
                alanlar = emptyList(),
                cikti = File(gecici.root, "olmaz.pdf"),
                rasterlestirici = SahteRasterlestirici(),
                gecicilerDizini = gecicilerDizini,
            )
        }
    }

    @Test
    fun `sifir alanli dikdortgen gecersiz sayilir`() {
        val kaynak = kaynakOlustur()
        assertThrows(PdfHatasi.GirdiYok::class.java) {
            PdfKartici.karart(
                kaynak = kaynak,
                alanlar = listOf(KarartmaAlani(0, 0.5f, 0.5f, 0.5f, 0.5f)),
                cikti = File(gecici.root, "olmaz2.pdf"),
                rasterlestirici = SahteRasterlestirici(),
                gecicilerDizini = gecicilerDizini,
            )
        }
    }

    @Test
    fun `sinir disi sayfa indeksi reddedilir`() {
        val kaynak = kaynakOlustur()
        assertThrows(PdfHatasi.GecersizAralik::class.java) {
            PdfKartici.karart(
                kaynak = kaynak,
                alanlar = listOf(metinBandiAlani(sayfa = 9)),
                cikti = File(gecici.root, "olmaz3.pdf"),
                rasterlestirici = SahteRasterlestirici(),
                gecicilerDizini = gecicilerDizini,
            )
        }
    }

    @Test
    fun `ters cizilen dikdortgen duzeltilir`() {
        // Kullanici sagdan sola, asagidan yukari cizerse de calismali.
        val alan = KarartmaAlani(0, sol = 0.8f, ust = 0.9f, sag = 0.2f, alt = 0.1f).duzelt()
        assertEquals(0.2f, alan.sol, 0.001f)
        assertEquals(0.1f, alan.ust, 0.001f)
        assertEquals(0.8f, alan.sag, 0.001f)
        assertEquals(0.9f, alan.alt, 0.001f)
        assertTrue(alan.gecerliMi)
    }

    @Test
    fun `tum sayfalar karartilirsa belgede hic metin kalmaz`() {
        val kaynak = kaynakOlustur()
        val cikti = File(gecici.root, "tamamen.pdf")

        val sonuc = PdfKartici.karart(
            kaynak = kaynak,
            alanlar = listOf(metinBandiAlani(0), metinBandiAlani(1)),
            cikti = cikti,
            rasterlestirici = SahteRasterlestirici(),
            gecicilerDizini = gecicilerDizini,
        )

        assertEquals(listOf(0, 1), sonuc.karartilanSayfalar)
        val ciktiMetni = PdfMetinCikarici.cikar(cikti).trim()
        assertFalse(ciktiMetni.contains(gizliDize))
        assertFalse(ciktiMetni.contains(ikinciSayfaMetni))
    }
}
