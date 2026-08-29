package com.yerel.pdfkutusu.ui.ortak

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yerel.pdfkutusu.R
import com.yerel.pdfkutusu.cekirdek.IslemTuru

/**
 * [IslemTuru]'nun kullaniciya gorunen etiketini uretir.
 *
 * `cekirdek/` saf Kotlin oldugu icin kaynak dizesi referansi tasimaz (bkz.
 * `PdfHataMetni.kt`'deki kimlik/metin ayrimiyla ayni desen); eslemesi
 * burada, UI katmaninda yapilir.
 */
@Composable
fun IslemTuru.etiket(): String = stringResource(
    when (this) {
        IslemTuru.BIRLESTIR -> R.string.ana_arac_birlestir_baslik
        IslemTuru.BOL -> R.string.ana_arac_bol_baslik
        IslemTuru.SIRALA -> R.string.ana_arac_sirala_baslik
        IslemTuru.DONDUR -> R.string.ana_arac_dondur_baslik
        IslemTuru.SIKISTIR -> R.string.ana_arac_sikistir_baslik
        IslemTuru.FILIGRAN -> R.string.ana_arac_filigran_baslik
        IslemTuru.KARART -> R.string.ana_arac_karart_baslik
        IslemTuru.OCR -> R.string.ana_arac_ocr_baslik
        IslemTuru.RESIMDEN_PDF -> R.string.ana_arac_resimden_pdf_baslik
    },
)
