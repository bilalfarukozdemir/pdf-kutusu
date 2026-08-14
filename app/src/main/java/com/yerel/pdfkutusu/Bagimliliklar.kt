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
 * Okuyucudan arac ekranina gecerken belgeyi tasiyan kutu.
 *
 * Niyet (Intent) ekleriyle tasimak yerine burada tutuyoruz: dosya zaten
 * calisma alaninda, yalnizca "hangi belge" bilgisinin gezinmeyi asmasi
 * gerekiyor.
 *
 * **Okumak kutuyu bosaltmaz.** Ilk once bosaltiyordu ve sonucu su oluyordu:
 * okuyucudan gelip Sirala'ya giren kullanici belgeyi goruyor, geri cikip
 * Karart'a girdiginde "PDF sec" ile karsilasiyordu. Belge, kullanici acikca
 * birakana ya da uygulama normal yoldan (okuyucudan degil) acilana kadar
 * kullanilabilir kalir.
 */
class BekleyenGirdi {

    @Volatile
    private var kayit: Pair<java.io.File, String>? = null

    fun koy(dosya: java.io.File, gorunenAd: String) {
        kayit = dosya to gorunenAd
    }

    /** Bosaltmadan okur; birden fazla arac ayni belgeyi kullanabilsin. */
    fun oku(): Pair<java.io.File, String>? = kayit?.takeIf { it.first.isFile }

    fun adi(): String? = oku()?.second

    fun varMi(): Boolean = oku() != null

    fun temizle() {
        kayit = null
    }
}
