package com.yerel.pdfkutusu

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Okuyucudan araclara devredilen belgenin kutusu.
 *
 * Bu testin varlik sebebi somut bir hata: kutu okundugunda bosaliyordu,
 * dolayisiyla okuyucudan gelip Sirala'ya giren kullanici belgeyi goruyor,
 * geri cikip Karart'a girdiginde "PDF sec" ile karsilasiyordu.
 */
class BekleyenGirdiTesti {

    @get:Rule
    val gecici = TemporaryFolder()

    private fun dosya(ad: String): File =
        gecici.newFile(ad).apply { writeText("sahte pdf") }

    @Test
    fun `okumak kutuyu bosaltmaz`() {
        val kutu = BekleyenGirdi()
        val belge = dosya("rapor.pdf")
        kutu.koy(belge, "rapor.pdf")

        // Birden fazla arac ayni belgeyi kullanabilmeli.
        assertNotNull("Ilk okuma bos dondu", kutu.oku())
        assertNotNull("Ikinci okuma bos dondu - kutu bosalmis", kutu.oku())
        assertNotNull("Ucuncu okuma bos dondu", kutu.oku())
        assertEquals("rapor.pdf", kutu.oku()?.second)
    }

    @Test
    fun `bos kutu null doner`() {
        val kutu = BekleyenGirdi()
        assertNull(kutu.oku())
        assertNull(kutu.adi())
        assertFalse(kutu.varMi())
    }

    @Test
    fun `temizlemek kutuyu bosaltir`() {
        val kutu = BekleyenGirdi()
        kutu.koy(dosya("a.pdf"), "a.pdf")
        assertTrue(kutu.varMi())

        kutu.temizle()

        assertNull(kutu.oku())
        assertFalse(kutu.varMi())
    }

    @Test
    fun `yeni belge oncekinin yerine gecer`() {
        val kutu = BekleyenGirdi()
        kutu.koy(dosya("eski.pdf"), "eski.pdf")
        kutu.koy(dosya("yeni.pdf"), "yeni.pdf")

        assertEquals("yeni.pdf", kutu.adi())
    }

    @Test
    fun `dosya silinmisse kutu bos sayilir`() {
        val kutu = BekleyenGirdi()
        val belge = dosya("silinecek.pdf")
        kutu.koy(belge, "silinecek.pdf")
        assertTrue(kutu.varMi())

        // Calisma alani temizlenmis olabilir; kutu olu bir yolu sunmamali.
        assertTrue(belge.delete())

        assertNull("Silinmis dosya icin null donmeliydi", kutu.oku())
        assertFalse(kutu.varMi())
    }

    @Test
    fun `ad ve dosya birlikte dondurulur`() {
        val kutu = BekleyenGirdi()
        val belge = dosya("Çalışma Raporu.pdf")
        kutu.koy(belge, "Çalışma Raporu.pdf")

        val okunan = kutu.oku()
        assertNotNull(okunan)
        assertEquals(belge.absolutePath, okunan!!.first.absolutePath)
        assertEquals("Çalışma Raporu.pdf", okunan.second)
    }
}
