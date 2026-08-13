package com.yerel.pdfkutusu.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File

/**
 * PDF'in **icerik akisindaki** metni okur (OCR degil).
 *
 * Iki isi var:
 *  1. Arayuzde "bu sayfada secilebilir metin var mi?" sorusunu yanitlamak -
 *     OCR gerekip gerekmedigini kullaniciya soylemek icin.
 *  2. Karartma dogrulama testinin olcum araci olmak: karartilan sayfadan
 *     metin cikarildiginda gizlenen dizenin **bulunmamasi** gerekir.
 */
object PdfMetinCikarici {

    fun cikar(dosya: File, parola: String? = null): String =
        BelgeErisimi.ac(dosya, parola).use { cikar(it) }

    fun cikar(belge: PDDocument): String {
        val ayiklayici = PDFTextStripper().apply {
            sortByPosition = true
            lineSeparator = "\n"
            paragraphEnd = "\n"
        }
        return ayiklayici.getText(belge)
    }

    /** @param sayfaIndeksi 0-tabanli */
    fun sayfadanCikar(belge: PDDocument, sayfaIndeksi: Int): String {
        val ayiklayici = PDFTextStripper().apply {
            sortByPosition = true
            startPage = sayfaIndeksi + 1
            endPage = sayfaIndeksi + 1
        }
        return ayiklayici.getText(belge)
    }

    /** Sayfada anlamli miktarda secilebilir metin var mi? */
    fun metinKatmaniVarMi(belge: PDDocument, sayfaIndeksi: Int): Boolean =
        runCatching { sayfadanCikar(belge, sayfaIndeksi).trim().length >= 16 }.getOrDefault(false)
}
