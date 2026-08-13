package com.yerel.pdfkutusu

import com.yerel.pdfkutusu.cekirdek.PdfHatasi
import com.yerel.pdfkutusu.cekirdek.SayfaAraligi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Sayfa araligi ayristirici.
 *
 * Kullanici 1-tabanli konusur, kod 0-tabanli calisir. Sinir durumlari
 * (tek sayfa, tum sayfalar, gecersiz aralik) bu katmanda yakalanmali;
 * PDF motoruna hatali indeks gitmemeli.
 */
class SayfaAraligiTesti {

    // ------------------------------------------------------------- gecerli

    @Test
    fun `tek sayfa`() {
        assertEquals(listOf(0), SayfaAraligi.ayristir("1", 10))
        assertEquals(listOf(4), SayfaAraligi.ayristir("5", 10))
        assertEquals(listOf(9), SayfaAraligi.ayristir("10", 10))
    }

    @Test
    fun `tek sayfalik belgede tek sayfa`() {
        assertEquals(listOf(0), SayfaAraligi.ayristir("1", 1))
        assertEquals(listOf(0), SayfaAraligi.ayristir("1-1", 1))
        assertEquals(listOf(0), SayfaAraligi.ayristir("tumu", 1))
    }

    @Test
    fun `kapali aralik`() {
        assertEquals(listOf(0, 1, 2), SayfaAraligi.ayristir("1-3", 10))
        assertEquals(listOf(6, 7, 8), SayfaAraligi.ayristir("7-9", 10))
    }

    @Test
    fun `tum sayfalar`() {
        assertEquals((0..9).toList(), SayfaAraligi.ayristir("1-10", 10))
        assertEquals((0..9).toList(), SayfaAraligi.ayristir("tumu", 10))
        assertEquals((0..9).toList(), SayfaAraligi.ayristir("tümü", 10))
        assertEquals((0..9).toList(), SayfaAraligi.ayristir("*", 10))
    }

    @Test
    fun `acik uclu araliklar`() {
        assertEquals(listOf(7, 8, 9), SayfaAraligi.ayristir("8-", 10))
        assertEquals(listOf(0, 1, 2), SayfaAraligi.ayristir("-3", 10))
    }

    @Test
    fun `karisik ifade sirali ve tekrarsiz doner`() {
        assertEquals(listOf(0, 1, 2, 4, 7, 8), SayfaAraligi.ayristir("5, 1-3, 8-9", 10))
        // Ustuste binen araliklar tekillenir
        assertEquals(listOf(0, 1, 2, 3), SayfaAraligi.ayristir("1-3, 2-4", 10))
    }

    @Test
    fun `bosluk ve alternatif ayiricilar hosgorulur`() {
        assertEquals(listOf(0, 2), SayfaAraligi.ayristir(" 1 , 3 ", 10))
        assertEquals(listOf(0, 2), SayfaAraligi.ayristir("1;3", 10))
        assertEquals(listOf(0, 2), SayfaAraligi.ayristir("1,3,", 10))
        // Kisa/uzun tire de kabul edilir (kopyala-yapistir gercegi)
        assertEquals(listOf(0, 1, 2), SayfaAraligi.ayristir("1–3", 10))
        assertEquals(listOf(0, 1, 2), SayfaAraligi.ayristir("1—3", 10))
    }

    // ------------------------------------------------------------- gecersiz

    @Test
    fun `bos ifade reddedilir`() {
        assertThrows(PdfHatasi.GecersizAralik::class.java) { SayfaAraligi.ayristir("", 10) }
        assertThrows(PdfHatasi.GecersizAralik::class.java) { SayfaAraligi.ayristir("   ", 10) }
        assertThrows(PdfHatasi.GecersizAralik::class.java) { SayfaAraligi.ayristir(",,,", 10) }
    }

    @Test
    fun `sinir disi sayfa reddedilir`() {
        assertThrows(PdfHatasi.GecersizAralik::class.java) { SayfaAraligi.ayristir("11", 10) }
        assertThrows(PdfHatasi.GecersizAralik::class.java) { SayfaAraligi.ayristir("0", 10) }
        assertThrows(PdfHatasi.GecersizAralik::class.java) { SayfaAraligi.ayristir("1-11", 10) }
        assertThrows(PdfHatasi.GecersizAralik::class.java) { SayfaAraligi.ayristir("2", 1) }
    }

    @Test
    fun `ters aralik reddedilir`() {
        val hata = assertThrows(PdfHatasi.GecersizAralik::class.java) {
            SayfaAraligi.ayristir("5-2", 10)
        }
        assertEquals(true, hata.kullaniciMesaji.contains("5"))
    }

    @Test
    fun `sayi olmayan girdi reddedilir`() {
        assertThrows(PdfHatasi.GecersizAralik::class.java) { SayfaAraligi.ayristir("abc", 10) }
        assertThrows(PdfHatasi.GecersizAralik::class.java) { SayfaAraligi.ayristir("1-a", 10) }
        assertThrows(PdfHatasi.GecersizAralik::class.java) { SayfaAraligi.ayristir("1-2-3", 10) }
        assertThrows(PdfHatasi.GecersizAralik::class.java) { SayfaAraligi.ayristir("-", 10) }
        assertThrows(PdfHatasi.GecersizAralik::class.java) { SayfaAraligi.ayristir("3.5", 10) }
    }

    @Test
    fun `cok buyuk sayi reddedilir`() {
        assertThrows(PdfHatasi.GecersizAralik::class.java) {
            SayfaAraligi.ayristir("99999999999999999999", 10)
        }
    }

    @Test
    fun `sayfasiz belge reddedilir`() {
        assertThrows(PdfHatasi.GecersizAralik::class.java) { SayfaAraligi.ayristir("1", 0) }
    }

    // ------------------------------------------------------------- bicimleme

    @Test
    fun `bicimleme okunabilir metin uretir`() {
        assertEquals("1-3, 5", SayfaAraligi.bicimle(listOf(0, 1, 2, 4)))
        assertEquals("1", SayfaAraligi.bicimle(listOf(0)))
        assertEquals("1-10", SayfaAraligi.bicimle((0..9).toList()))
        assertEquals("2, 4, 6", SayfaAraligi.bicimle(listOf(1, 3, 5)))
        assertEquals("", SayfaAraligi.bicimle(emptyList()))
    }

    @Test
    fun `ayristirma ve bicimleme birbirinin tersi`() {
        val ifade = "1-3, 5, 8-10"
        val indeksler = SayfaAraligi.ayristir(ifade, 10)
        assertEquals(ifade, SayfaAraligi.bicimle(indeksler))
    }
}
