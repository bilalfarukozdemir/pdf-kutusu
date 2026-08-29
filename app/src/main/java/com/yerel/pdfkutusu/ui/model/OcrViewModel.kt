package com.yerel.pdfkutusu.ui.model

import com.yerel.pdfkutusu.Bagimliliklar
import com.yerel.pdfkutusu.R
import com.yerel.pdfkutusu.cekirdek.DosyaAdi
import com.yerel.pdfkutusu.cekirdek.IslemTuru
import com.yerel.pdfkutusu.cekirdek.SayfaAraligi
import com.yerel.pdfkutusu.ocr.OcrMotoru
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class OcrSecenekleri(
    val aralikIfadesi: String = "",
    val dpi: Int = 300,
    val cikanMetin: String = "",
)

/**
 * Sayfadan metin cikarma (cihaz ustu OCR).
 *
 * Kapsam disi (bilerek): PDF'e aranabilir metin katmani gomme. Bu, dogru
 * konumlandirilmis gorunmez metin cizmeyi gerektirir ve v1'de yapilmayacak.
 */
class OcrViewModel(bagimliliklar: Bagimliliklar) :
    AracViewModel(bagimliliklar, IslemTuru.OCR) {

    private val _secenekler = MutableStateFlow(OcrSecenekleri())
    val secenekler: StateFlow<OcrSecenekleri> = _secenekler.asStateFlow()

    val dpiSecenekleri = listOf(200, 300, 400)

    fun aralikDegistir(deger: String) = _secenekler.update { it.copy(aralikIfadesi = deger) }
    fun dpiDegistir(deger: Int) = _secenekler.update { it.copy(dpi = deger) }

    override fun girdilerDegisti() {
        val sayfa = durum.value.ilkGirdi?.sayfaSayisi ?: return
        _secenekler.update {
            it.copy(
                aralikIfadesi = if (it.aralikIfadesi.isBlank()) {
                    if (sayfa > 1) "1-${minOf(sayfa, 5)}" else "1"
                } else {
                    it.aralikIfadesi
                },
                cikanMetin = "",
            )
        }
    }

    fun tani() {
        val girdi = durum.value.ilkGirdi ?: return
        val ayarlar = _secenekler.value

        calistir { ilerleme ->
            val indeksler = SayfaAraligi.ayristir(ayarlar.aralikIfadesi, girdi.sayfaSayisi)
            val sonuc = OcrMotoru().use { motor ->
                motor.sayfalariTani(
                    kaynak = girdi.dosya,
                    sayfaIndeksleri = indeksler,
                    rasterlestirici = rasterlestirici,
                    dpi = ayarlar.dpi,
                    ilerleme = ilerleme,
                )
            }

            _secenekler.update { it.copy(cikanMetin = sonuc.tumMetin) }

            val cikti = calismaAlani.ciktiDosyasi(
                DosyaAdi.cikti(girdi.gorunenAd, IslemTuru.OCR, uzanti = "txt"),
            )
            cikti.writeText(sonuc.tumMetin, Charsets.UTF_8)

            val baglam = bagimliliklar.uygulamaBaglami
            IslemCiktisi(
                dosyalar = listOf(cikti),
                sayfaSayisi = indeksler.size,
                ozetSatiri = if (sonuc.bosMu) {
                    baglam.getString(R.string.ocr_metin_bulunamadi)
                } else {
                    baglam.getString(R.string.ocr_ozet_satiri, indeksler.size, sonuc.karakterSayisi)
                },
                notlar = buildList {
                    add(baglam.getString(R.string.ocr_not_txt))
                    add(baglam.getString(R.string.ocr_not_ml_kit))
                    if (sonuc.bosMu) {
                        add(baglam.getString(R.string.ocr_not_bos_sayfa))
                    }
                    if (girdi.ozet.metinKatmaniVar) {
                        add(baglam.getString(R.string.ocr_not_zaten_metin_var))
                    }
                },
            )
        }
    }

    fun metniTemizle() = _secenekler.update { it.copy(cikanMetin = "") }
}
