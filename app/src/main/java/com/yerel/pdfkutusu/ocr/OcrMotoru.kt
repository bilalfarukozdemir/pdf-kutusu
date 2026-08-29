package com.yerel.pdfkutusu.ocr

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.yerel.pdfkutusu.R
import com.yerel.pdfkutusu.cekirdek.PdfHatasi
import com.yerel.pdfkutusu.pdf.Ilerleme
import com.yerel.pdfkutusu.pdf.IlerlemeDinleyicisi
import com.yerel.pdfkutusu.pdf.IlerlemeYok
import com.yerel.pdfkutusu.pdf.SayfaRasterlestirici
import java.io.Closeable
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class SayfaMetni(
    val sayfaIndeksi: Int,
    val metin: String,
) {
    val bosMu: Boolean get() = metin.isBlank()
}

data class OcrSonucu(
    val sayfalar: List<SayfaMetni>,
) {
    /** @param baglam verilirse sayfa basligi yerellestirilir; verilmezse sabit Turkce metin kullanilir. */
    fun tumMetin(baglam: Context? = null): String = sayfalar.joinToString("\n\n") { sayfa ->
        val baslik = baglam?.getString(R.string.ocr_metin_sayfa_basligi, sayfa.sayfaIndeksi + 1)
            ?: "--- Sayfa ${sayfa.sayfaIndeksi + 1} ---"
        "$baslik\n${sayfa.metin.trim()}"
    }

    val karakterSayisi: Int get() = sayfalar.sumOf { it.metin.length }
    val bosMu: Boolean get() = sayfalar.all { it.bosMu }
}

/**
 * Cihaz ustu metin tanima.
 *
 * ML Kit'in **paketli (bundled)** Latin alfabesi modelini kullanir. Model APK
 * icinde gelir; calisma zamaninda hicbir sey indirilmez, hicbir sey
 * yuklenmez. Uygulamanin INTERNET izni zaten yoktur.
 *
 * Turkce Latin alfabesiyle yazilir, bu yuzden paketli Latin modeli
 * yeterlidir. Ancak model Turkce'ye ozel egitilmedigi icin `ı/i` ve `ş/s`
 * ayrimlarinda hata payi vardir; arayuz bunu belirtir.
 */
class OcrMotoru : Closeable {

    private val taniyici = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Secilen sayfalari sirayla tanir.
     *
     * @param dpi 300 civari degerler kucuk puntolarda belirgin sekilde daha
     *   iyi sonuc verir; 200'un altina inmek onerilmez.
     */
    suspend fun sayfalariTani(
        kaynak: File,
        sayfaIndeksleri: List<Int>,
        rasterlestirici: SayfaRasterlestirici,
        dpi: Int = 300,
        ilerleme: IlerlemeDinleyicisi = IlerlemeYok,
        baglam: Context? = null,
    ): OcrSonucu {
        if (sayfaIndeksleri.isEmpty()) {
            throw PdfHatasi.GirdiYok(
                baglam?.getString(R.string.ocr_hata_en_az_bir_sayfa) ?: "Metin çıkarmak için en az bir sayfa seçin.",
            )
        }
        val sonuclar = mutableListOf<SayfaMetni>()
        rasterlestirici.ac(kaynak).use { oturum ->
            sayfaIndeksleri.forEachIndexed { sira, indeks ->
                if (indeks < 0 || indeks >= oturum.sayfaSayisi) {
                    throw PdfHatasi.GecersizAralik(
                        baglam?.getString(R.string.ocr_hata_gecersiz_sayfa, oturum.sayfaSayisi, indeks + 1)
                            ?: "Belge ${oturum.sayfaSayisi} sayfa, ${indeks + 1}. sayfa istendi.",
                    )
                }
                ilerleme(
                    Ilerleme(
                        sira,
                        sayfaIndeksleri.size,
                        baglam?.getString(R.string.ocr_ilerleme_taraniyor, indeks + 1) ?: "Sayfa ${indeks + 1} taranıyor",
                    ),
                )
                val bitmap = oturum.rasterlestir(indeks, dpi)
                try {
                    sonuclar += SayfaMetni(indeks, tani(bitmap))
                } finally {
                    runCatching { bitmap.recycle() }
                }
                ilerleme(
                    Ilerleme(
                        sira + 1,
                        sayfaIndeksleri.size,
                        baglam?.getString(R.string.ocr_ilerleme_bitti, indeks + 1) ?: "Sayfa ${indeks + 1} bitti",
                    ),
                )
            }
        }
        return OcrSonucu(sonuclar)
    }

    /**
     * ML Kit `Task` API'siyle calisir. Play Services coroutine koprusu
     * (`kotlinx-coroutines-play-services`) yerine dogrudan
     * [suspendCancellableCoroutine] kullaniyoruz; boylece bir bagimlilik daha
     * eksiliyor.
     */
    suspend fun tani(bitmap: Bitmap): String = suspendCancellableCoroutine { devam ->
        val gorsel = InputImage.fromBitmap(bitmap, 0)
        taniyici.process(gorsel)
            .addOnSuccessListener { sonuc ->
                if (devam.isActive) devam.resume(sonuc.text)
            }
            .addOnFailureListener { hata ->
                if (devam.isActive) devam.resumeWithException(PdfHatasi.Beklenmeyen(hata))
            }
    }

    override fun close() {
        runCatching { taniyici.close() }
    }
}
