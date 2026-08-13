package com.yerel.pdfkutusu.ui.model

import com.yerel.pdfkutusu.Bagimliliklar
import com.yerel.pdfkutusu.cekirdek.DosyaAdi
import com.yerel.pdfkutusu.cekirdek.IslemTuru
import com.yerel.pdfkutusu.cekirdek.SayfaAraligi
import com.yerel.pdfkutusu.pdf.PdfDondurucu
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DondurmeSecenekleri(
    val aci: Int = 90,
    val tumSayfalar: Boolean = true,
    val aralikIfadesi: String = "",
)

class DondurViewModel(bagimliliklar: Bagimliliklar) :
    AracViewModel(bagimliliklar, IslemTuru.DONDUR) {

    private val _secenekler = MutableStateFlow(DondurmeSecenekleri())
    val secenekler: StateFlow<DondurmeSecenekleri> = _secenekler.asStateFlow()

    fun aciDegistir(aci: Int) = _secenekler.update { it.copy(aci = aci) }
    fun tumSayfalarDegistir(deger: Boolean) = _secenekler.update { it.copy(tumSayfalar = deger) }
    fun aralikDegistir(deger: String) = _secenekler.update { it.copy(aralikIfadesi = deger) }

    override fun girdilerDegisti() {
        val sayfa = durum.value.ilkGirdi?.sayfaSayisi ?: return
        if (_secenekler.value.aralikIfadesi.isBlank()) {
            _secenekler.update { it.copy(aralikIfadesi = if (sayfa > 1) "1-$sayfa" else "1") }
        }
    }

    fun dondur() {
        val girdi = durum.value.ilkGirdi ?: return
        val ayarlar = _secenekler.value

        calistir { ilerleme ->
            val indeksler = if (ayarlar.tumSayfalar) {
                null
            } else {
                SayfaAraligi.ayristir(ayarlar.aralikIfadesi, girdi.sayfaSayisi)
            }
            val cikti = calismaAlani.ciktiDosyasi(
                DosyaAdi.cikti(girdi.gorunenAd, IslemTuru.DONDUR, ekBilgi = "${ayarlar.aci}derece"),
            )
            val dondurulen = PdfDondurucu.dondur(
                kaynak = girdi.dosya,
                aci = ayarlar.aci,
                cikti = cikti,
                sayfaIndeksleri = indeksler,
                ilerleme = ilerleme,
            )
            IslemCiktisi(
                dosyalar = listOf(cikti),
                sayfaSayisi = girdi.sayfaSayisi,
                ozetSatiri = "$dondurulen sayfa ${ayarlar.aci}° döndürüldü",
                notlar = listOf(
                    "Döndürme sayfanın /Rotate değerini değiştirir; metin ve görseller yeniden çizilmez.",
                ),
            )
        }
    }
}
