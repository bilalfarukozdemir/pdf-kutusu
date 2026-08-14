package com.yerel.pdfkutusu

import com.yerel.pdfkutusu.depo.KayitDeposu
import com.yerel.pdfkutusu.depo.SonAcilanBelge
import com.yerel.pdfkutusu.depo.SonAcilanlar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Son acilan belgeler listesi.
 *
 * Saf JVM testi: siralama, kapasite ve ayristirma Android'e hic dokunmuyor.
 *
 * Kapasite disina dusen kayitlarin **dondurulmesi** onemli: cagiran taraf
 * onlarin kalici URI yetkisini birakiyor. Dondurulmezse sistemin paket basina
 * tuttugu yetki kotasi zamanla dolar ve yeni belge eklenemez olur.
 */
class SonAcilanlarTesti {

    private class SahteDepo : KayitDeposu {
        var kayitlar: Set<String> = emptySet()
        override fun oku(): Set<String> = kayitlar
        override fun yaz(kayitlar: Set<String>) {
            this.kayitlar = kayitlar
        }
    }

    private fun belge(
        no: Int,
        zaman: Long = no.toLong(),
        kalici: Boolean = true,
    ) = SonAcilanBelge("content://belge/$no", "rapor-$no.pdf", zaman, kalici)

    private fun liste() = SonAcilanlar(SahteDepo())

    @Test
    fun `yeni belge listenin basina gelir`() {
        val liste = liste()
        liste.ekle(belge(1, zaman = 100))
        liste.ekle(belge(2, zaman = 200))
        liste.ekle(belge(3, zaman = 300))

        assertEquals(
            listOf("content://belge/3", "content://belge/2", "content://belge/1"),
            liste.listele().map { it.uri },
        )
    }

    @Test
    fun `ayni belge yeniden acilinca kopyalanmaz`() {
        val liste = liste()
        liste.ekle(belge(1, zaman = 100))
        liste.ekle(belge(2, zaman = 200))
        val dusen = liste.ekle(belge(1, zaman = 300))

        assertEquals("Kopya olusmus", 2, liste.listele().size)
        assertEquals("Basa gelmeliydi", "content://belge/1", liste.listele().first().uri)
        assertTrue("Yeniden ekleme dusen kayit saymaz", dusen.isEmpty())
    }

    @Test
    fun `kapasite asilinca en eski duser ve dondurulur`() {
        val liste = liste()
        for (no in 1..SonAcilanlar.AZAMI) liste.ekle(belge(no, zaman = no.toLong()))
        assertEquals(SonAcilanlar.AZAMI, liste.listele().size)

        val dusen = liste.ekle(belge(999, zaman = 9_999))

        assertEquals(SonAcilanlar.AZAMI, liste.listele().size)
        assertEquals("Tam olarak bir kayit dusmeliydi", 1, dusen.size)
        assertEquals("En eski dusmeliydi", "content://belge/1", dusen.first().uri)
        assertFalse(liste.listele().any { it.uri == "content://belge/1" })
        assertEquals("content://belge/999", liste.listele().first().uri)
    }

    @Test
    fun `silmek yalnizca o kaydi kaldirir`() {
        val liste = liste()
        liste.ekle(belge(1))
        liste.ekle(belge(2))
        liste.ekle(belge(3))

        liste.sil("content://belge/2")

        assertEquals(
            listOf("content://belge/3", "content://belge/1"),
            liste.listele().map { it.uri },
        )
    }

    @Test
    fun `olmayan kaydi silmek listeyi bozmaz`() {
        val liste = liste()
        liste.ekle(belge(1))
        liste.sil("content://baska/7")
        assertEquals(1, liste.listele().size)
    }

    @Test
    fun `temizlemek hepsini siler ve dondurur`() {
        val liste = liste()
        liste.ekle(belge(1))
        liste.ekle(belge(2))

        val silinenler = liste.temizle()

        assertEquals(2, silinenler.size)
        assertTrue(liste.listele().isEmpty())
    }

    @Test
    fun `bos listeyi temizlemek cokmez`() {
        assertTrue(liste().temizle().isEmpty())
    }

    // ---- Kayit bicimi ----

    @Test
    fun `bozuk kayitlar yok sayilir`() {
        val depo = SahteDepo()
        val ayirac = 31.toChar()
        depo.kayitlar = setOf(
            "bu bir kayit degil",
            "zamanSayiDegil${ayirac}1${ayirac}content://a${ayirac}a.pdf",
            "100${ayirac}1$ayirac${ayirac}bos-uri.pdf",
            "100${ayirac}1${ayirac}content://saglam${ayirac}saglam.pdf",
        )
        val liste = SonAcilanlar(depo)

        val okunan = liste.listele()
        assertEquals("Yalnizca saglam kayit kalmaliydi", 1, okunan.size)
        assertEquals("content://saglam", okunan.first().uri)
    }

    @Test
    fun `adin icindeki ayirac kaydi bozmaz`() {
        val liste = liste()
        val ayirac = 31.toChar()
        liste.ekle(SonAcilanBelge("content://x", "kotu${ayirac}ad.pdf", 1, true))

        val okunan = liste.listele().single()
        assertEquals("content://x", okunan.uri)
        assertFalse("Ayirac ada sizmis", okunan.ad.contains(ayirac))
        assertEquals("kotu ad.pdf", okunan.ad)
    }

    @Test
    fun `turkce karakterli ad korunur`() {
        val liste = liste()
        liste.ekle(SonAcilanBelge("content://y", "Çalışma Raporu Ğüş.pdf", 1, true))
        assertEquals("Çalışma Raporu Ğüş.pdf", liste.listele().single().ad)
    }

    @Test
    fun `kalici bayragi saklanir`() {
        val liste = liste()
        liste.ekle(belge(1, zaman = 100, kalici = true))
        liste.ekle(belge(2, zaman = 200, kalici = false))

        val okunan = liste.listele().associateBy { it.uri }
        assertTrue(okunan.getValue("content://belge/1").kalici)
        assertFalse(okunan.getValue("content://belge/2").kalici)
    }

    @Test
    fun `yerel dosya uriler de saklanabilir`() {
        val liste = liste()
        val yol = "file:///data/user/0/com.yerel.pdfkutusu/files/cikti/a__birlestir__1.pdf"
        liste.ekle(SonAcilanBelge(yol, "a__birlestir__1.pdf", 1, true))
        assertEquals(yol, liste.listele().single().uri)
    }
}
