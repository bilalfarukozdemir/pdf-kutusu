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

    val uygulamaBaglami: Context = baglam.applicationContext

    val calismaAlani: CalismaAlani by lazy { CalismaAlani(uygulamaBaglami) }
    val tercihler: Tercihler by lazy { Tercihler(uygulamaBaglami) }
    val rasterlestirici: SayfaRasterlestirici by lazy { PdfRendererRasterlestirici() }
    val onizleme: OnizlemeDeposu by lazy { OnizlemeDeposu(rasterlestirici) }
    val gorselOnizleme: GorselOnizlemeDeposu by lazy { GorselOnizlemeDeposu() }
    val gunluk: GunlukDeposu by lazy { GunlukDeposu(PdfVeritabani.al(uygulamaBaglami).gunlukDao()) }

    /** Okuyucudan araclara devredilen belge. */
    val bekleyenGirdi = BekleyenGirdi()
}

/**
 * Okuyucudan arac ekranina gecerken belgeyi tasiyan tek seferlik kutu.
 *
 * Niyet (Intent) ekleriyle tasimak yerine burada tutuyoruz: dosya zaten
 * calisma alaninda, yalnizca "hangi belge" bilgisinin gezinmeyi asmasi
 * gerekiyor. Okundugunda temizlenir, boylece uygulamayi sonra normal
 * acildiginda eski bir belge kendiliginden yuklenmez.
 */
class BekleyenGirdi {

    @Volatile
    private var kayit: Pair<java.io.File, String>? = null

    fun koy(dosya: java.io.File, gorunenAd: String) {
        kayit = dosya to gorunenAd
    }

    /** Varsa dondurur ve kutuyu bosaltir. */
    fun al(): Pair<java.io.File, String>? {
        val mevcut = kayit
        kayit = null
        return mevcut
    }

    fun varMi(): Boolean = kayit != null

    fun temizle() {
        kayit = null
    }
}
