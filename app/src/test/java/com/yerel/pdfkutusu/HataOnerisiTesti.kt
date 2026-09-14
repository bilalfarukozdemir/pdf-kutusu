package com.yerel.pdfkutusu

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.yerel.pdfkutusu.cekirdek.PdfHataKimligi
import com.yerel.pdfkutusu.cekirdek.PdfHatasi
import com.yerel.pdfkutusu.ui.ortak.oneriUret
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Hata kartindaki oneri satiri arayuzun diliyle gelmeli.
 *
 * [PdfHatasi.oneri] `cekirdek/` icinde sabit Turkce metindir; ekranda
 * `oneriUret` ile turun R.string karsiligi gosterilir. Ingilizce arayuzde
 * Turkce oneri kalmadigi ve Turkce karsiliklarin cekirdekteki metinle ayni
 * oldugu (hicbir oneri kaybolmadan) burada dogrulanir.
 */
@RunWith(RobolectricTestRunner::class)
class HataOnerisiTesti {

    private val baglam: Context
        get() = ApplicationProvider.getApplicationContext()

    /** Her alt tur en az bir kez; PdfHatasi'na yeni tur eklenirse buraya da eklenmeli. */
    private fun tumHatalar(): List<PdfHatasi> = listOf(
        PdfHatasi.ParolaGerekli(),
        PdfHatasi.ParolaYanlis(),
        PdfHatasi.GecersizAralik("aralik"),
        PdfHatasi.GecersizAralik(PdfHataKimligi.AralikNedeni.Bos),
        PdfHatasi.BozukBelge("bozuk"),
        PdfHatasi.DosyaOkunamadi("okunamadi"),
        PdfHatasi.GirdiYok(),
        PdfHatasi.GirdiYok("secilmedi"),
        PdfHatasi.Iptal(),
        PdfHatasi.Beklenmeyen(IllegalStateException("x")),
    )

    @Test
    @Config(qualifiers = "en-rUS")
    fun `ingilizce arayuzde oneri satirinda turkce kalmaz`() {
        for (hata in tumHatalar()) {
            val oneri = hata.oneriUret(baglam)
            // Hepsi null donerse asagidaki kontrol bos yere gecerdi.
            assertEquals(
                "${hata::class.simpleName} onerisi kayboldu ya da fazladan geldi",
                hata.oneri != null,
                oneri != null,
            )
            if (oneri == null) continue
            assertFalse(
                "${hata::class.simpleName} onerisi Turkce kaldi: $oneri",
                TURKCE_HARF.containsMatchIn(oneri) || oneri == hata.oneri,
            )
        }
    }

    @Test
    @Config(qualifiers = "tr-rTR")
    fun `turkce arayuzde oneri cekirdekteki metinle ayni`() {
        for (hata in tumHatalar()) {
            assertEquals(hata::class.simpleName, hata.oneri, hata.oneriUret(baglam))
        }
    }

    private companion object {
        val TURKCE_HARF = Regex("[çğıöşüÇĞİÖŞÜ]")
    }
}
