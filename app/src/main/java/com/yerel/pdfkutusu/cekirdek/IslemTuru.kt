package com.yerel.pdfkutusu.cekirdek

/**
 * Uygulamanin destekledigi islemler.
 *
 * [dosyaEki] cikti dosya adinda kullanilir ve bilerek ASCII'dir; boylece
 * uretilen dosya adinin orta parcasi her cihazda ayni gorunur. Kullanicinin
 * kendi dosya adindaki Turkce karakterler ise korunur.
 *
 * Bu sinif saf Kotlin kalir; `cekirdek/` Android'e dokunmaz kuralina uyar.
 * Kullaniciya gorunen etiket burada tutulmaz - eslemesi
 * `ui/ortak/IslemTuruMetni.kt` icinde, `PdfHataMetni.kt`'deki desenle
 * ayni sekilde yapilir: kimlik burada, metne cevirme UI katmaninda.
 */
enum class IslemTuru(val dosyaEki: String) {
    BIRLESTIR("birlestir"),
    BOL("bol"),
    SIRALA("sirala"),
    DONDUR("dondur"),
    SIKISTIR("sikistir"),
    FILIGRAN("filigran"),
    KARART("karart"),
    OCR("ocr"),
    RESIMDEN_PDF("resimden"),
    ;

    companion object {
        fun adindan(ad: String): IslemTuru? = entries.firstOrNull { it.name == ad }
    }
}
