package com.yerel.pdfkutusu.ui.model

import com.yerel.pdfkutusu.Bagimliliklar
import com.yerel.pdfkutusu.cekirdek.DosyaAdi
import com.yerel.pdfkutusu.cekirdek.IslemTuru
import com.yerel.pdfkutusu.cekirdek.PdfHatasi
import com.yerel.pdfkutusu.pdf.PdfSiralayici
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sayfa siralama.
 *
 * [sira] kaynak sayfa indekslerini istenen sirayla tutar. Surukle-birak
 * arayuzu bu listeyi degistirir; sayfa cikarmak da mumkundur.
 */
class SiralaViewModel(bagimliliklar: Bagimliliklar) :
    AracViewModel(bagimliliklar, IslemTuru.SIRALA) {

    private val _sira = MutableStateFlow<List<Int>>(emptyList())
    val sira: StateFlow<List<Int>> = _sira.asStateFlow()

    override fun girdilerDegisti() {
        val girdi = durum.value.ilkGirdi
        _sira.value = if (girdi == null) emptyList() else (0 until girdi.sayfaSayisi).toList()
    }

    fun tasi(kaynakIndeks: Int, hedefIndeks: Int) {
        val liste = _sira.value.toMutableList()
        if (kaynakIndeks !in liste.indices || hedefIndeks !in liste.indices) return
        liste.add(hedefIndeks, liste.removeAt(kaynakIndeks))
        _sira.value = liste
    }

    fun sayfaCikar(konum: Int) {
        val liste = _sira.value.toMutableList()
        if (konum !in liste.indices) return
        if (liste.size == 1) {
            guncelle { it.copy(hata = PdfHatasi.GecersizAralik("Çıktıda en az bir sayfa kalmalı.")) }
            return
        }
        liste.removeAt(konum)
        _sira.value = liste
    }

    fun tersCevir() {
        _sira.value = _sira.value.reversed()
    }

    fun sifirla() = girdilerDegisti()

    fun uygula() {
        val girdi = durum.value.ilkGirdi ?: return
        val yeniSira = _sira.value
        if (yeniSira.isEmpty()) {
            guncelle { it.copy(hata = PdfHatasi.GecersizAralik("Çıktıda en az bir sayfa kalmalı.")) }
            return
        }

        calistir { ilerleme ->
            val cikti = calismaAlani.ciktiDosyasi(
                DosyaAdi.cikti(girdi.gorunenAd, IslemTuru.SIRALA),
            )
            val sayfaSayisi = PdfSiralayici.sirala(
                kaynak = girdi.dosya,
                yeniSira = yeniSira,
                cikti = cikti,
                ilerleme = ilerleme,
            )
            val cikarilan = girdi.sayfaSayisi - yeniSira.distinct().size
            IslemCiktisi(
                dosyalar = listOf(cikti),
                sayfaSayisi = sayfaSayisi,
                ozetSatiri = "$sayfaSayisi sayfa yeniden sıralandı" +
                    if (cikarilan > 0) " · $cikarilan sayfa çıkarıldı" else "",
            )
        }
    }
}
