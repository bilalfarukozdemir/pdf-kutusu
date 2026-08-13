package com.yerel.pdfkutusu.onizleme

import android.graphics.Bitmap
import android.util.LruCache
import com.yerel.pdfkutusu.pdf.SayfaRasterlestirici
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Sayfa onizlemeleri.
 *
 * Ayri bir goruntuleme kutuphanesi kurmuyoruz: Android'in yerlesik
 * `PdfRenderer`'i zaten sayfa cizebiliyor. Buradaki katman yalnizca
 * (a) bellek onbellegi ve (b) acik oturum tekrar kullanimi ekliyor - her
 * kucuk resim icin dosyayi yeniden acmak listeleri gozle gorulur sekilde
 * yavaslatiyordu.
 */
class OnizlemeDeposu(private val rasterlestirici: SayfaRasterlestirici) {

    private val onbellek = object : LruCache<String, Bitmap>(ONBELLEK_BAYT) {
        override fun sizeOf(anahtar: String, deger: Bitmap): Int = deger.byteCount
    }

    private val kilit = Mutex()
    private var acikYol: String? = null
    private var acikDegisim: Long = 0
    private var oturum: SayfaRasterlestirici.Oturum? = null

    suspend fun sayfaSayisi(dosya: File): Int = kilit.withLock {
        withContext(Dispatchers.IO) {
            oturumAl(dosya).sayfaSayisi
        }
    }

    /**
     * @param hedefGenislikPx istenen genislik; en/boy orani korunur
     * @return onbellekten ya da yeni cizilmis kucuk resim
     */
    suspend fun kucukResim(dosya: File, sayfaIndeksi: Int, hedefGenislikPx: Int): Bitmap? {
        val anahtar = "${dosya.absolutePath}|${dosya.lastModified()}|$sayfaIndeksi|$hedefGenislikPx"
        onbellek.get(anahtar)?.let { return it }

        return kilit.withLock {
            onbellek.get(anahtar)?.let { return@withLock it }
            withContext(Dispatchers.IO) {
                runCatching {
                    val aktifOturum = oturumAl(dosya)
                    if (sayfaIndeksi !in 0 until aktifOturum.sayfaSayisi) return@runCatching null
                    val boyut = aktifOturum.noktaBoyutu(sayfaIndeksi)
                    val dpi = max(
                        MIN_ONIZLEME_DPI,
                        (72f * hedefGenislikPx / max(1f, boyut.genislik)).roundToInt(),
                    ).coerceAtMost(AZAMI_ONIZLEME_DPI)
                    aktifOturum.rasterlestir(sayfaIndeksi, dpi)
                }.getOrNull()?.also { onbellek.put(anahtar, it) }
            }
        }
    }

    /** Dosya degistiyse (yeni cikti uretildiyse) onbellegi tazele. */
    fun gecersizKil(dosya: File) {
        onbellek.snapshot().keys
            .filter { it.startsWith(dosya.absolutePath + "|") }
            .forEach { onbellek.remove(it) }
        if (acikYol == dosya.absolutePath) kapat()
    }

    fun temizle() {
        onbellek.evictAll()
        kapat()
    }

    private fun kapat() {
        runCatching { oturum?.close() }
        oturum = null
        acikYol = null
        acikDegisim = 0
    }

    private fun oturumAl(dosya: File): SayfaRasterlestirici.Oturum {
        val yol = dosya.absolutePath
        val degisim = dosya.lastModified()
        val mevcut = oturum
        if (mevcut != null && acikYol == yol && acikDegisim == degisim) return mevcut
        kapat()
        val yeni = rasterlestirici.ac(dosya)
        oturum = yeni
        acikYol = yol
        acikDegisim = degisim
        return yeni
    }

    private companion object {
        /** ~48 MB kucuk resim onbellegi. */
        const val ONBELLEK_BAYT = 48 * 1024 * 1024
        const val MIN_ONIZLEME_DPI = 24
        const val AZAMI_ONIZLEME_DPI = 160
    }
}
