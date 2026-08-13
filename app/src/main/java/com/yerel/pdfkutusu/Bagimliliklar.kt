package com.yerel.pdfkutusu

import android.content.Context
import com.yerel.pdfkutusu.depo.CalismaAlani
import com.yerel.pdfkutusu.depo.Tercihler
import com.yerel.pdfkutusu.onizleme.GorselOnizlemeDeposu
import com.yerel.pdfkutusu.onizleme.OnizlemeDeposu
import com.yerel.pdfkutusu.pdf.PdfRendererRasterlestirici
import com.yerel.pdfkutusu.pdf.SayfaRasterlestirici
import com.yerel.pdfkutusu.veri.GunlukDeposu
import com.yerel.pdfkutusu.veri.PdfVeritabani

/**
 * Elle kurulan bagimlilik kabi.
 *
 * Bu boyutta bir uygulamada Hilt/Dagger kurmak fayda-maliyet acisindan
 * anlamsiz; tek bir sinif hem yeterli hem de okunur.
 */
class Bagimliliklar(baglam: Context) {

    private val uygulamaBaglami = baglam.applicationContext

    val calismaAlani: CalismaAlani by lazy { CalismaAlani(uygulamaBaglami) }
    val tercihler: Tercihler by lazy { Tercihler(uygulamaBaglami) }
    val rasterlestirici: SayfaRasterlestirici by lazy { PdfRendererRasterlestirici() }
    val onizleme: OnizlemeDeposu by lazy { OnizlemeDeposu(rasterlestirici) }
    val gorselOnizleme: GorselOnizlemeDeposu by lazy { GorselOnizlemeDeposu() }
    val gunluk: GunlukDeposu by lazy { GunlukDeposu(PdfVeritabani.al(uygulamaBaglami).gunlukDao()) }
}
