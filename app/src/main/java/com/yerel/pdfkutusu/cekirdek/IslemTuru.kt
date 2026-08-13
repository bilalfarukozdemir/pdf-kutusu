package com.yerel.pdfkutusu.cekirdek

/**
 * Uygulamanin destekledigi islemler.
 *
 * [dosyaEki] cikti dosya adinda kullanilir ve bilerek ASCII'dir; boylece
 * uretilen dosya adinin orta parcasi her cihazda ayni gorunur. Kullanicinin
 * kendi dosya adindaki Turkce karakterler ise korunur.
 */
enum class IslemTuru(val etiket: String, val dosyaEki: String) {
    BIRLESTIR("Birleştir", "birlestir"),
    BOL("Böl", "bol"),
    SIRALA("Sırala", "sirala"),
    DONDUR("Döndür", "dondur"),
    SIKISTIR("Sıkıştır", "sikistir"),
    FILIGRAN("Filigran", "filigran"),
    KARART("Karart", "karart"),
    OCR("OCR", "ocr"),
    RESIMDEN_PDF("Resimden PDF", "resimden"),
    ;

    companion object {
        fun adindan(ad: String): IslemTuru? = entries.firstOrNull { it.name == ad }
    }
}
