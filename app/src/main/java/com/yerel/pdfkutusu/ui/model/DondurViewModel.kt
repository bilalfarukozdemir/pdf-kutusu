package com.yerel.pdfkutusu.ui.model

import com.yerel.pdfkutusu.Bagimliliklar
import com.yerel.pdfkutusu.R
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

        val baglam = bagimliliklar.uygulamaBaglami
        calistir { ilerleme ->
            val indeksler = if (ayarlar.tumSayfalar) {
                null
            } else {
                SayfaAraligi.ayristir(ayarlar.aralikIfadesi, girdi.sayfaSayisi)
            }
            val cikti = calismaAlani.ciktiDosyasi(
                DosyaAdi.cikti(
                    girdi.gorunenAd,
                    IslemTuru.DONDUR,
                    ekBilgi = baglam.getString(R.string.dondur_ek_bilgi, ayarlar.aci),
                ),
            )
            val dondurulen = PdfDondurucu.dondur(
                kaynak = girdi.dosya,
                aci = ayarlar.aci,
                cikti = cikti,
                sayfaIndeksleri = indeksler,
                ilerleme = ilerleme,
                baglam = baglam,
            )
            IslemCiktisi(
                dosyalar = listOf(cikti),
                sayfaSayisi = girdi.sayfaSayisi,
                ozetSatiri = baglam.getString(R.string.dondur_ozet_satiri, dondurulen, ayarlar.aci),
                notlar = listOf(baglam.getString(R.string.dondur_not)),
            )
        }
    }
}
