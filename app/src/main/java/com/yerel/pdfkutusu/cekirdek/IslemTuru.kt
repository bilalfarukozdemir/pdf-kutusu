package com.yerel.pdfkutusu.cekirdek

import androidx.annotation.StringRes
import com.yerel.pdfkutusu.R

/**
 * Uygulamanin destekledigi islemler.
 *
 * [dosyaEki] cikti dosya adinda kullanilir ve bilerek ASCII'dir; boylece
 * uretilen dosya adinin orta parcasi her cihazda ayni gorunur. Kullanicinin
 * kendi dosya adindaki Turkce karakterler ise korunur.
 *
 * [etiketRes] kaynak dizesi referansidir (davranissal olarak Android'e
 * baglilik getirmez, sadece derleme zamaninda sabit bir int) - metnin
 * kendisi UI katmaninda `stringResource`/`getString` ile cozulur; boylece
 * bu sinif tek bir yerden hem Turkce hem Ingilizce etiket saglar.
 */
enum class IslemTuru(@StringRes val etiketRes: Int, val dosyaEki: String) {
    BIRLESTIR(R.string.ana_arac_birlestir_baslik, "birlestir"),
    BOL(R.string.ana_arac_bol_baslik, "bol"),
    SIRALA(R.string.ana_arac_sirala_baslik, "sirala"),
    DONDUR(R.string.ana_arac_dondur_baslik, "dondur"),
    SIKISTIR(R.string.ana_arac_sikistir_baslik, "sikistir"),
    FILIGRAN(R.string.ana_arac_filigran_baslik, "filigran"),
    KARART(R.string.ana_arac_karart_baslik, "karart"),
    OCR(R.string.ana_arac_ocr_baslik, "ocr"),
    RESIMDEN_PDF(R.string.ana_arac_resimden_pdf_baslik, "resimden"),
    ;

    companion object {
        fun adindan(ad: String): IslemTuru? = entries.firstOrNull { it.name == ad }
    }
}
