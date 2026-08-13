package com.yerel.pdfkutusu

import com.yerel.pdfkutusu.cekirdek.DosyaAdi
import com.yerel.pdfkutusu.cekirdek.IslemTuru
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guvenli dosya adi temizleyicisi.
 *
 * En kritik beklenti: **Turkce karakterler bozulmadan gecer.** Bir temizleyici
 * "guvenlik" adina Turkce harfleri silerse kullanicinin dosya adlari taninmaz
 * hale gelir.
 *
 * Not: kacis dizileri yerine bilerek [Char] yapicilari kullaniliyor; kaynak
 * dosyanin kendisinde ham kontrol karakteri bulunmasin.
 */
class DosyaAdiTesti {

    private val TERS_BOLU = 92.toChar()
    private val KONTROL = charArrayOf(
        0.toChar(), // NUL
        7.toChar(), // BEL
        27.toChar(), // ESC
        31.toChar(), // US
        127.toChar(), // DEL
    ).concatToString()

    // ------------------------------------------------------- Turkce karakter

    @Test
    fun `turkce karakterler oldugu gibi korunur`() {
        val ad = "Şirket Görüşme Tutanağı ÇİĞDEM ışık.pdf"
        assertEquals(ad, DosyaAdi.guvenli(ad))
    }

    @Test
    fun `tum turkce harfler tek tek korunur`() {
        val harfler = "çğıöşüÇĞİÖŞÜ"
        val sonuc = DosyaAdi.guvenli(harfler + ".pdf")
        harfler.forEach { harf ->
            assertTrue("'" + harf + "' harfi kayboldu: " + sonuc, sonuc.contains(harf))
        }
    }

    @Test
    fun `turkce karakterli ad uzunluk sinirinda bozulmaz`() {
        val uzunTaban = "ğ".repeat(200)
        val sonuc = DosyaAdi.guvenli(uzunTaban + ".pdf")
        assertEquals("ğ".repeat(DosyaAdi.AZAMI_TABAN_UZUNLUK) + ".pdf", sonuc)
    }

    // --------------------------------------------------------- yol gezinmesi

    @Test
    fun `yol gezinmesi kesilir`() {
        assertEquals("gizli.pdf", DosyaAdi.guvenli("../../etc/gizli.pdf"))
        assertEquals(
            "gizli.pdf",
            DosyaAdi.guvenli(".." + TERS_BOLU + ".." + TERS_BOLU + "Windows" + TERS_BOLU + "gizli.pdf"),
        )
        assertEquals("belge.pdf", DosyaAdi.guvenli("/mnt/sdcard/Belgeler/belge.pdf"))
    }

    @Test
    fun `yalnizca nokta nokta verilirse varsayilana duser`() {
        assertEquals("belge", DosyaAdi.guvenli(".."))
        assertEquals("belge", DosyaAdi.guvenli("../../.."))
        assertEquals("belge", DosyaAdi.guvenli("/"))
        assertEquals("belge", DosyaAdi.guvenli(""))
        assertEquals("belge", DosyaAdi.guvenli(null))
        assertEquals("belge", DosyaAdi.guvenli("   "))
    }

    @Test
    fun `sonuc hicbir zaman yol ayirici icermez`() {
        val girdiler = listOf(
            "a/b/c.pdf",
            "a" + TERS_BOLU + "b" + TERS_BOLU + "c.pdf",
            "..//..//x.pdf",
            "/",
            TERS_BOLU.toString(),
        )
        girdiler.forEach { girdi ->
            val sonuc = DosyaAdi.guvenli(girdi)
            assertFalse("'/' sizdi: " + sonuc, sonuc.contains('/'))
            assertFalse("ters bolu sizdi: " + sonuc, sonuc.contains(TERS_BOLU))
        }
    }

    // ------------------------------------------------------------- uzunluk

    @Test
    fun `cok uzun ad kirpilir ve uzanti korunur`() {
        val uzun = "a".repeat(500) + ".pdf"
        val sonuc = DosyaAdi.guvenli(uzun)
        assertEquals(DosyaAdi.AZAMI_TABAN_UZUNLUK + ".pdf".length, sonuc.length)
        assertTrue(sonuc.endsWith(".pdf"))
    }

    @Test
    fun `cok uzun uzanti da kirpilir`() {
        val sonuc = DosyaAdi.guvenli("dosya." + "x".repeat(100))
        assertTrue(sonuc.length <= DosyaAdi.AZAMI_TABAN_UZUNLUK + 1 + DosyaAdi.AZAMI_UZANTI_UZUNLUK)
    }

    // --------------------------------------------------- kontrol karakterleri

    @Test
    fun `kontrol karakterleri silinir`() {
        val ham = "ra" + KONTROL + "por" + KONTROL + ".pdf"
        assertEquals("rapor.pdf", DosyaAdi.guvenli(ham))
    }

    @Test
    fun `dosya sistemi icin sorunlu karakterler alt cizgi olur`() {
        assertEquals("a_b_c_d_e_f_g.pdf", DosyaAdi.guvenli("""a:b*c?d"e<f>g.pdf"""))
    }

    @Test
    fun `bosluk yiginlari tekile iner ve bas son kirpilir`() {
        assertEquals("iki kelime.pdf", DosyaAdi.guvenli("   iki     kelime.pdf   "))
    }

    // ------------------------------------------------------- cikti adlandirma

    @Test
    fun `cikti adi sartnamedeki bicimde uretilir`() {
        val zaman = LocalDateTime.of(2026, 8, 14, 9, 5, 3)
        val sonuc = DosyaAdi.cikti("Çalışma Raporu.pdf", IslemTuru.KARART, zaman)
        assertEquals("Çalışma Raporu__karart__20260814-090503.pdf", sonuc)
    }

    @Test
    fun `cikti adi ek bilgi ve farkli uzanti destekler`() {
        val zaman = LocalDateTime.of(2026, 1, 2, 3, 4, 5)
        assertEquals(
            "not__ocr__20260102-030405.txt",
            DosyaAdi.cikti("not.pdf", IslemTuru.OCR, zaman, uzanti = "txt"),
        )
        assertEquals(
            "not__dondur-90derece__20260102-030405.pdf",
            DosyaAdi.cikti("not.pdf", IslemTuru.DONDUR, zaman, ekBilgi = "90derece"),
        )
    }

    @Test
    fun `cikti adi tehlikeli kaynak adindan da guvenli uretilir`() {
        val zaman = LocalDateTime.of(2026, 8, 14, 9, 5, 3)
        val sonuc = DosyaAdi.cikti("../../gizli.pdf", IslemTuru.BOL, zaman)
        assertEquals("gizli__bol__20260814-090503.pdf", sonuc)
    }

    @Test
    fun `cikti adinda turkce karakter korunur`() {
        val zaman = LocalDateTime.of(2026, 8, 14, 9, 5, 3)
        val sonuc = DosyaAdi.cikti("ığdır şişli.pdf", IslemTuru.FILIGRAN, zaman)
        assertEquals("ığdır şişli__filigran__20260814-090503.pdf", sonuc)
    }

    // ----------------------------------------------------------------- taban

    @Test
    fun `taban ve uzanti dogru ayrilir`() {
        assertEquals("rapor.final", DosyaAdi.tabani("rapor.final.pdf"))
        assertEquals("pdf", DosyaAdi.uzantisi("rapor.final.pdf"))
        assertEquals("uzantisiz", DosyaAdi.tabani("uzantisiz"))
        assertEquals("", DosyaAdi.uzantisi("uzantisiz"))
        // Noktadan sonra bosluk varsa uzanti degildir.
        assertEquals("", DosyaAdi.uzantisi("surum 1.2 son hali"))
    }
}
