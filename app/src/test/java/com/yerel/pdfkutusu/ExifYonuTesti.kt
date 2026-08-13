package com.yerel.pdfkutusu

import com.yerel.pdfkutusu.pdf.ExifYonu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * EXIF yon etiketi -> donusum eslemesi.
 *
 * Etiket degerleri EXIF sartnamesinden gelir (1..8); bilerek sabit sayilarla
 * yaziliyor, boylece test hicbir Android sinifina dokunmadan saf JVM'de kosar.
 *
 * Referans anlam:
 *  1 normal | 2 yatay ayna | 3 180 | 4 dikey ayna
 *  5 transpose | 6 90 CW | 7 transverse | 8 270 CW
 *
 * Uygulama sirasi: **once dondur, sonra yatay aynala.**
 */
class ExifYonuTesti {

    @Test
    fun `normal ve tanimsiz degerler kimlik donusumu verir`() {
        listOf(0, 1, 9, -3, 100).forEach { etiket ->
            val donusum = ExifYonu.donusum(etiket)
            assertTrue("etiket=$etiket kimlik olmaliydi", donusum.kimlikMi)
            assertEquals(0, donusum.donusDerecesi)
            assertFalse(donusum.yatayAyna)
        }
    }

    @Test
    fun `duz donusler`() {
        assertEquals(90, ExifYonu.donusum(6).donusDerecesi)
        assertEquals(180, ExifYonu.donusum(3).donusDerecesi)
        assertEquals(270, ExifYonu.donusum(8).donusDerecesi)

        assertFalse(ExifYonu.donusum(6).yatayAyna)
        assertFalse(ExifYonu.donusum(3).yatayAyna)
        assertFalse(ExifYonu.donusum(8).yatayAyna)
    }

    @Test
    fun `yatay ayna`() {
        val donusum = ExifYonu.donusum(2)
        assertEquals(0, donusum.donusDerecesi)
        assertTrue(donusum.yatayAyna)
    }

    @Test
    fun `dikey ayna 180 artı yatay aynaya esittir`() {
        // Dikey aynalama, 180 donus + yatay aynalama ile ayni sonucu verir.
        val donusum = ExifYonu.donusum(4)
        assertEquals(180, donusum.donusDerecesi)
        assertTrue(donusum.yatayAyna)
    }

    @Test
    fun `transpose 90 artı yatay ayna`() {
        val donusum = ExifYonu.donusum(5)
        assertEquals(90, donusum.donusDerecesi)
        assertTrue(donusum.yatayAyna)
    }

    @Test
    fun `transverse 270 artı yatay ayna`() {
        val donusum = ExifYonu.donusum(7)
        assertEquals(270, donusum.donusDerecesi)
        assertTrue(donusum.yatayAyna)
    }

    @Test
    fun `en boy takasi yalnizca 90 ve 270 de olur`() {
        assertFalse(ExifYonu.donusum(1).enBoyTakas)
        assertFalse(ExifYonu.donusum(2).enBoyTakas)
        assertFalse(ExifYonu.donusum(3).enBoyTakas)
        assertFalse(ExifYonu.donusum(4).enBoyTakas)
        assertTrue(ExifYonu.donusum(5).enBoyTakas)
        assertTrue(ExifYonu.donusum(6).enBoyTakas)
        assertTrue(ExifYonu.donusum(7).enBoyTakas)
        assertTrue(ExifYonu.donusum(8).enBoyTakas)
    }

    @Test
    fun `donus acisi her zaman 90in kati ve 0-270 araliginda`() {
        for (etiket in 0..9) {
            val aci = ExifYonu.donusum(etiket).donusDerecesi
            assertEquals("etiket=$etiket", 0, aci % 90)
            assertTrue("etiket=$etiket aci=$aci", aci in 0..270)
        }
    }
}
