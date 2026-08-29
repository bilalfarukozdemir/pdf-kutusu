package com.yerel.pdfkutusu

import com.yerel.pdfkutusu.satinalma.BagisDurumu
import com.yerel.pdfkutusu.satinalma.BagisSecenegi
import com.yerel.pdfkutusu.satinalma.secenekListesindenDurum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [secenekListesindenDurum] saf durum-turetme mantigini dogrular.
 *
 * BagisYoneticisi bu fonksiyonu hem ilk urun sorgusu basarili oldugunda
 * hem de bir satin alma akisi (iptal/hata/tamamlanma) sonrasinda "onceki
 * secenek listesine don" icin kullanir - iki cagrı yeri de ayni saf
 * fonksiyona dayandigi icin burada tek yerden test edilir.
 */
class BagisDurumuTesti {

    private val kahve = BagisSecenegi(urunId = "destek_kahve", fiyatMetni = "₺39,99")
    private val ogun = BagisSecenegi(urunId = "destek_ogun", fiyatMetni = "₺99,99")
    private val comert = BagisSecenegi(urunId = "destek_comert", fiyatMetni = "₺199,99")

    @Test
    fun `bos liste magaza yok durumuna doner`() {
        val durum = secenekListesindenDurum(emptyList())
        assertEquals(BagisDurumu.MagazaYok, durum)
    }

    @Test
    fun `dolu liste hazir durumuna doner`() {
        val secenekler = listOf(kahve, ogun, comert)
        val durum = secenekListesindenDurum(secenekler)

        assertTrue(durum is BagisDurumu.Hazir)
        assertEquals(secenekler, (durum as BagisDurumu.Hazir).secenekler)
    }

    @Test
    fun `tek secenekli liste de hazir sayilir`() {
        val durum = secenekListesindenDurum(listOf(kahve))
        assertTrue(durum is BagisDurumu.Hazir)
        assertEquals(1, (durum as BagisDurumu.Hazir).secenekler.size)
    }

    @Test
    fun `hazir durumdaki secenek sirasi korunur`() {
        val durum = secenekListesindenDurum(listOf(comert, kahve, ogun)) as BagisDurumu.Hazir
        assertEquals(listOf("destek_comert", "destek_kahve", "destek_ogun"), durum.secenekler.map { it.urunId })
    }

    @Test
    fun `bagis secenegi esitligi urun id ve fiyata gore calisir`() {
        val birinci = BagisSecenegi("destek_kahve", "₺39,99")
        val ikinci = BagisSecenegi("destek_kahve", "₺39,99")
        val farkli = BagisSecenegi("destek_kahve", "₺49,99")

        assertEquals(birinci, ikinci)
        assertTrue(birinci != farkli)
    }
}
