package com.yerel.pdfkutusu.ui.model

import com.yerel.pdfkutusu.Bagimliliklar
import com.yerel.pdfkutusu.R
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
                it.copy(
                    hata = PdfHatasi.GirdiYok(
                        bagimliliklar.uygulamaBaglami.getString(R.string.birlestir_hata_en_az_iki),
                    ),
                )
            }
            return
        }

        calistir { ilerleme ->
            val ad = DosyaAdi.cikti(girdiler.first().gorunenAd, IslemTuru.BIRLESTIR)
            val cikti = calismaAlani.ciktiDosyasi(ad)
            val baglam = bagimliliklar.uygulamaBaglami
            val sayfaSayisi = PdfBirlestirici.birlestir(
                girdiler = girdiler.map { BirlestirmeGirdisi(it.dosya, it.gorunenAd) },
                cikti = cikti,
                ilerleme = ilerleme,
                baglam = baglam,
            )
            IslemCiktisi(
                dosyalar = listOf(cikti),
                sayfaSayisi = sayfaSayisi,
                ozetSatiri = baglam.getString(R.string.birlestir_ozet_satiri, girdiler.size, sayfaSayisi),
                notlar = buildList {
                    add(baglam.getString(R.string.birlestir_not_sira))
                    if (girdiler.any { it.ozet.uyarilar.isNotEmpty() }) {
                        add(baglam.getString(R.string.birlestir_not_yer_imi))
                    }
                },
            )
        }
    }
}
