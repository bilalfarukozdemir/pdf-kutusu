package com.yerel.pdfkutusu

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.yerel.pdfkutusu.pdf.YaziTipi

/**
 * Uygulama giris noktasi.
 *
 * PdfBox-Android, standart-14 font metriklerini (Helvetica, Times vb.) ve
 * renk uzayi tanimlarini AAR'in `assets` klasorunden okur. Bu yukleyici
 * baslatilmazsa ilk font erisiminde "Could not find referenced font"
 * benzeri bir hata alinir. Bu yuzden uygulama acilir acilmaz baslatiyoruz.
 */
class PdfKutusuUygulamasi : Application() {

    lateinit var bagimliliklar: Bagimliliklar
        private set

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
        // Filigranin paketli Noto Sans'a ulasabilmesi icin; PdfBox'in kendi
        // varlik yukleyicisiyle ayni yerde durmasi bilincli.
        YaziTipi.baslat(applicationContext)
        bagimliliklar = Bagimliliklar(this)
    }
}
