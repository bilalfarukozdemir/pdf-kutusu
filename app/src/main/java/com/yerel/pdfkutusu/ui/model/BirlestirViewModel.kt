package com.yerel.pdfkutusu.ui.model

import com.yerel.pdfkutusu.Bagimliliklar
import com.yerel.pdfkutusu.cekirdek.DosyaAdi
import com.yerel.pdfkutusu.cekirdek.IslemTuru
import com.yerel.pdfkutusu.cekirdek.PdfHatasi
import com.yerel.pdfkutusu.pdf.BirlestirmeGirdisi
import com.yerel.pdfkutusu.pdf.PdfBirlestirici

class BirlestirViewModel(bagimliliklar: Bagimliliklar) :
    AracViewModel(bagimliliklar, IslemTuru.BIRLESTIR, tekGirdi = false) {

    fun birlestir() {
        val girdiler = durum.value.girdiler
        if (girdiler.size < 2) {
            guncelle {
                it.copy(hata = PdfHatasi.GirdiYok("Birleştirmek için en az iki PDF seçin."))
            }
            return
        }

        calistir { ilerleme ->
            val ad = DosyaAdi.cikti(girdiler.first().gorunenAd, IslemTuru.BIRLESTIR)
            val cikti = calismaAlani.ciktiDosyasi(ad)
            val sayfaSayisi = PdfBirlestirici.birlestir(
                girdiler = girdiler.map { BirlestirmeGirdisi(it.dosya, it.gorunenAd) },
                cikti = cikti,
                ilerleme = ilerleme,
            )
            IslemCiktisi(
                dosyalar = listOf(cikti),
                sayfaSayisi = sayfaSayisi,
                ozetSatiri = "${girdiler.size} dosya birleştirildi · $sayfaSayisi sayfa",
                notlar = buildList {
                    add("Sıra, listedeki sıradır. Değiştirmek için okları kullanın.")
                    if (girdiler.any { it.ozet.uyarilar.isNotEmpty() }) {
                        add("Yer imleri ve form alanları birleştirmede korunmayabilir.")
                    }
                },
            )
        }
    }
}
