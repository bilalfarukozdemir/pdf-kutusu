package com.yerel.pdfkutusu

import com.yerel.pdfkutusu.satinalma.SatinAlmaOzeti
import com.yerel.pdfkutusu.satinalma.islenecekSatinAlmalar
import com.yerel.pdfkutusu.satinalma.talepKaydiEkle
import com.yerel.pdfkutusu.satinalma.taramaZamaniGeldiMi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SatinAlmaKuraliTesti {

    private val urunler = listOf("priority_request_review", "priority_pr_review")

    @Test
    fun `PURCHASED alim urun ayrintisi gerekmeden islenir`() {
        val alim = SatinAlmaOzeti(listOf("priority_pr_review"), 1, "jeton1")
        assertEquals(listOf(alim), islenecekSatinAlmalar(listOf(alim), urunler))
    }

    @Test
    fun `PENDING alim islenmez ama sonradan PURCHASED olunca islenir`() {
        val bekleyen = SatinAlmaOzeti(listOf("priority_pr_review"), 2, "jeton1")
        assertTrue(islenecekSatinAlmalar(listOf(bekleyen), urunler).isEmpty())
        val tamam = bekleyen.copy(durum = 1)
        assertEquals(listOf(tamam), islenecekSatinAlmalar(listOf(tamam), urunler))
    }

    @Test
    fun `bilinmeyen urun ve tekrar eden jeton elenir`() {
        val bilinmeyen = SatinAlmaOzeti(listOf("baska_urun"), 1, "x")
        val a = SatinAlmaOzeti(listOf("priority_request_review"), 1, "ayni")
        assertEquals(listOf(a), islenecekSatinAlmalar(listOf(bilinmeyen, a, a), urunler))
    }

    @Test
    fun `ayni talep kaydi iki kez yazilmaz`() {
        val ilk = talepKaydiEkle(emptySet(), "priority_pr_review:ABC123")
        val ikinci = talepKaydiEkle(ilk, "priority_pr_review:ABC123")
        assertEquals(1, ikinci.size)
    }

    @Test
    fun `tarama ilk seferde ve aralik dolunca calisir, araliktan once calismaz`() {
        assertTrue(taramaZamaniGeldiMi(null, 1_000))
        assertFalse(taramaZamaniGeldiMi(1_000, 30_000))
        assertTrue(taramaZamaniGeldiMi(1_000, 61_000))
    }
}
