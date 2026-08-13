package com.yerel.pdfkutusu.onizleme

import android.graphics.Bitmap
import android.util.LruCache
import com.yerel.pdfkutusu.pdf.ResimdenPdf
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gorsel kucuk resimleri.
 *
 * Kucuk resimler bilerek [ResimdenPdf.bitmapHazirla] uzerinden uretiliyor:
 * boylece onizlemede gordugunuz yon, ciktidaki yonle **ayni kod yolundan**
 * geliyor. Ayri bir cozucu yazilsaydi EXIF yonu iki yerde ele alinir ve
 * onizleme ile cikti birbirinden ayrisabilirdi.
 */
class GorselOnizlemeDeposu {

    private val onbellek = object : LruCache<String, Bitmap>(ONBELLEK_BAYT) {
        override fun sizeOf(anahtar: String, deger: Bitmap): Int = deger.byteCount
    }

    suspend fun kucukResim(dosya: File, hedefKenarPx: Int = 240): Bitmap? {
        val anahtar = "${dosya.absolutePath}|${dosya.lastModified()}|$hedefKenarPx"
        onbellek.get(anahtar)?.let { return it }

        return withContext(Dispatchers.IO) {
            runCatching { ResimdenPdf.bitmapHazirla(dosya, hedefKenarPx) }
                .getOrNull()
                ?.also { onbellek.put(anahtar, it) }
        }
    }

    fun gecersizKil(dosya: File) {
        onbellek.snapshot().keys
            .filter { it.startsWith(dosya.absolutePath + "|") }
            .forEach { onbellek.remove(it) }
    }

    fun temizle() = onbellek.evictAll()

    private companion object {
        /** ~24 MB; kucuk resimler 240 px oldugu icin fazlasiyla yeter. */
        const val ONBELLEK_BAYT = 24 * 1024 * 1024
    }
}
