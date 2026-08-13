package com.yerel.pdfkutusu

import com.yerel.pdfkutusu.pdf.SayfaDuzeni
import com.yerel.pdfkutusu.pdf.SayfaYerlesimi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sayfa yerlesimi ve orneklem matematigi.
 *
 * Saf JVM testi: [SayfaYerlesimi] hicbir Android grafik sinifina dokunmuyor.
 * En kritik guvence: **en-boy orani hicbir duzende bozulmaz.**
 */
class SayfaYerlesimiTesti {

    /** Sartnamedeki tolerans: %1. */
    private fun oranEsit(beklenen: Float, gercek: Float, mesaj: String) {
        val sapma = kotlin.math.abs(beklenen - gercek) / beklenen
        assertTrue("$mesaj (beklenen=$beklenen gercek=$gercek sapma=%${sapma * 100})", sapma <= 0.01f)
    }

    // ------------------------------------------------------------- A4 sigdir

    @Test
    fun `A4 sigdirmada en boy orani korunur`() {
        val ornekler = listOf(
            4000 to 3000, // 4:3 yatay
            3000 to 4000, // 3:4 dikey
            1920 to 1080, // 16:9
            1080 to 1920,
            1000 to 1000, // kare
            5000 to 500, // asiri genis panorama
            500 to 5000, // asiri uzun
        )
        for ((en, boy) in ornekler) {
            val yerlesim = SayfaYerlesimi.a4Sigdir(en, boy, 10)
            oranEsit(en.toFloat() / boy, yerlesim.cizimOrani, "Oran bozuldu: ${en}x$boy")
        }
    }

    @Test
    fun `yatay gorselde sayfa da yatay olur`() {
        val yatay = SayfaYerlesimi.a4Sigdir(4000, 3000, 10)
        assertTrue("Yatay görselde sayfa yatay olmalı", yatay.sayfaEn > yatay.sayfaBoy)

        val dikey = SayfaYerlesimi.a4Sigdir(3000, 4000, 10)
        assertTrue("Dikey görselde sayfa dikey olmalı", dikey.sayfaBoy > dikey.sayfaEn)
    }

    @Test
    fun `kare gorsel dikey sayfaya yerlesir`() {
        val yerlesim = SayfaYerlesimi.a4Sigdir(1000, 1000, 0)
        assertTrue(yerlesim.sayfaBoy > yerlesim.sayfaEn)
        oranEsit(1f, yerlesim.cizimOrani, "Kare bozuldu")
    }

    @Test
    fun `gorsel sayfa icinde ortalanir`() {
        val yerlesim = SayfaYerlesimi.a4Sigdir(1000, 1000, 10)
        val sagBosluk = yerlesim.sayfaEn - (yerlesim.cizimX + yerlesim.cizimEn)
        val altBosluk = yerlesim.sayfaBoy - (yerlesim.cizimY + yerlesim.cizimBoy)
        assertEquals(yerlesim.cizimX, sagBosluk, 0.01f)
        assertEquals(yerlesim.cizimY, altBosluk, 0.01f)
    }

    @Test
    fun `kenar bosluguna uyulur`() {
        val bosluksuz = SayfaYerlesimi.a4Sigdir(1000, 1000, 0)
        val bosluklu = SayfaYerlesimi.a4Sigdir(1000, 1000, 20)
        assertTrue("Boşluk arttıkça çizim küçülmeli", bosluklu.cizimEn < bosluksuz.cizimEn)

        val beklenenBosluk = 20 * SayfaYerlesimi.MM_NOKTA
        assertTrue(
            "Sol boşluk en az istenen kadar olmalı",
            bosluklu.cizimX >= beklenenBosluk - 0.01f,
        )
    }

    @Test
    fun `gorsel her zaman sayfaya sigar`() {
        val ornekler = listOf(5000 to 500, 500 to 5000, 4000 to 3000, 100 to 100)
        for ((en, boy) in ornekler) {
            for (bosluk in SayfaYerlesimi.KENAR_BOSLUKLARI_MM) {
                val y = SayfaYerlesimi.a4Sigdir(en, boy, bosluk)
                assertTrue("${en}x$boy taştı", y.cizimEn <= y.sayfaEn + 0.01f)
                assertTrue("${en}x$boy taştı", y.cizimBoy <= y.sayfaBoy + 0.01f)
                assertTrue(y.cizimX >= -0.01f && y.cizimY >= -0.01f)
            }
        }
    }

    @Test
    fun `sacma kenar boslugu sayfayi bozmaz`() {
        val y = SayfaYerlesimi.a4Sigdir(1000, 1000, 10_000)
        assertTrue(y.cizimEn > 0f)
        assertTrue(y.cizimBoy > 0f)
    }

    // --------------------------------------------------------- goruntu boyutu

    @Test
    fun `goruntu boyutunda sayfa piksel bolu dpi carpi 72`() {
        val y = SayfaYerlesimi.goruntuBoyutu(1500, 3000, 150)
        assertEquals(1500f * 72f / 150f, y.sayfaEn, 0.01f)
        assertEquals(3000f * 72f / 150f, y.sayfaBoy, 0.01f)
        // Kenar boslugu yok
        assertEquals(0f, y.cizimX, 0.001f)
        assertEquals(0f, y.cizimY, 0.001f)
        assertEquals(y.sayfaEn, y.cizimEn, 0.001f)
        assertEquals(y.sayfaBoy, y.cizimBoy, 0.001f)
    }

    @Test
    fun `goruntu boyutunda oran korunur`() {
        for (dpi in SayfaYerlesimi.DPI_SECENEKLERI) {
            val y = SayfaYerlesimi.goruntuBoyutu(1920, 1080, dpi)
            oranEsit(1920f / 1080f, y.cizimOrani, "dpi=$dpi")
        }
    }

    @Test
    fun `yuksek dpi daha kucuk sayfa verir`() {
        val dusuk = SayfaYerlesimi.goruntuBoyutu(1500, 1500, 72)
        val yuksek = SayfaYerlesimi.goruntuBoyutu(1500, 1500, 300)
        assertTrue(yuksek.sayfaEn < dusuk.sayfaEn)
    }

    @Test
    fun `hesapla duzene gore dogru yolu secer`() {
        val a4 = SayfaYerlesimi.hesapla(1000, 1000, SayfaDuzeni.A4_SIGDIR, 10, 150)
        assertEquals(SayfaYerlesimi.A4_EN, a4.sayfaEn, 0.01f)

        val goruntu = SayfaYerlesimi.hesapla(1500, 1500, SayfaDuzeni.GORUNTU_BOYUTU, 10, 150)
        assertEquals(720f, goruntu.sayfaEn, 0.01f)
    }

    @Test
    fun `sifir ve negatif boyutlar cokmez`() {
        listOf(0 to 0, -5 to 10, 10 to -5).forEach { (en, boy) ->
            val a4 = SayfaYerlesimi.a4Sigdir(en, boy, 10)
            assertTrue(a4.cizimEn > 0f && a4.cizimBoy > 0f)
            val goruntu = SayfaYerlesimi.goruntuBoyutu(en, boy, 150)
            assertTrue(goruntu.sayfaEn > 0f && goruntu.sayfaBoy > 0f)
        }
    }

    // ------------------------------------------------------------- orneklem

    @Test
    fun `ornek boyutu 2nin kuvvetidir ve hedefin altina dusmez`() {
        val azami = 1600
        val ornekler = listOf(4000 to 3000, 8000 to 6000, 3200 to 2400, 1600 to 1200)
        for ((en, boy) in ornekler) {
            val ornek = SayfaYerlesimi.ornekBoyutu(en, boy, azami)
            assertTrue("2'nin kuvveti değil: $ornek", ornek > 0 && (ornek and (ornek - 1)) == 0)
            val kalanKenar = maxOf(en, boy) / ornek
            assertTrue(
                "${en}x$boy icin orneklem cok agresif: $kalanKenar < $azami",
                kalanKenar >= azami,
            )
        }
    }

    @Test
    fun `kucuk gorsel orneklenmez`() {
        assertEquals(1, SayfaYerlesimi.ornekBoyutu(800, 600, 1600))
        assertEquals(1, SayfaYerlesimi.ornekBoyutu(1600, 1200, 1600))
    }

    @Test
    fun `cok buyuk gorsel agresif orneklenir`() {
        val ornek = SayfaYerlesimi.ornekBoyutu(20_000, 15_000, 1100)
        assertTrue("Orneklem yetersiz: $ornek", ornek >= 8)
        assertTrue(20_000 / ornek >= 1100)
    }

    @Test
    fun `gecersiz girdide orneklem 1 doner`() {
        assertEquals(1, SayfaYerlesimi.ornekBoyutu(0, 0, 1600))
        assertEquals(1, SayfaYerlesimi.ornekBoyutu(-1, 100, 1600))
        assertEquals(1, SayfaYerlesimi.ornekBoyutu(100, 100, 0))
    }
}
