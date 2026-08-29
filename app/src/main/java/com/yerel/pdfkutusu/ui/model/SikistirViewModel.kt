package com.yerel.pdfkutusu.ui.model

import androidx.lifecycle.viewModelScope
import com.yerel.pdfkutusu.Bagimliliklar
import com.yerel.pdfkutusu.R
import com.yerel.pdfkutusu.cekirdek.DosyaAdi
import com.yerel.pdfkutusu.cekirdek.IslemTuru
import com.yerel.pdfkutusu.pdf.PdfSikistirici
import com.yerel.pdfkutusu.pdf.SikistirmaKalitesi
import com.yerel.pdfkutusu.pdf.SikistirmaTahmini
import com.yerel.pdfkutusu.ui.ortak.bicimliBoyut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SikistirmaSecenekleri(
    val kalite: SikistirmaKalitesi = SikistirmaKalitesi.ORTA,
    val tahminler: List<SikistirmaTahmini> = emptyList(),
    val tahminHesaplaniyor: Boolean = false,
    val gorselYok: Boolean = false,
)

class SikistirViewModel(bagimliliklar: Bagimliliklar) :
    AracViewModel(bagimliliklar, IslemTuru.SIKISTIR) {

    private val _secenekler = MutableStateFlow(SikistirmaSecenekleri())
    val secenekler: StateFlow<SikistirmaSecenekleri> = _secenekler.asStateFlow()

    private var tahminIsi: Job? = null

    fun kaliteDegistir(kalite: SikistirmaKalitesi) = _secenekler.update { it.copy(kalite = kalite) }

    override fun girdilerDegisti() {
        tahminIsi?.cancel()
        val girdi = durum.value.ilkGirdi
        if (girdi == null) {
            _secenekler.value = SikistirmaSecenekleri()
            return
        }
        _secenekler.update { it.copy(tahminHesaplaniyor = true, tahminler = emptyList()) }
        tahminIsi = viewModelScope.launch {
            val tahminler = withContext(Dispatchers.IO) {
                runCatching { PdfSikistirici.tahminEt(girdi.dosya) }.getOrDefault(emptyList())
            }
            // Tahmin dosya boyutuna cok yakinsa gomulu gorsel yok demektir.
            val kazancYok = tahminler.isEmpty() ||
                tahminler.all { it.tahminiBayt > girdi.boyut * 0.97 }
            _secenekler.update {
                it.copy(
                    tahminler = tahminler,
                    tahminHesaplaniyor = false,
                    gorselYok = kazancYok,
                )
            }
        }
    }

    fun sikistir() {
        val girdi = durum.value.ilkGirdi ?: return
        val kalite = _secenekler.value.kalite

        calistir { ilerleme ->
            val cikti = calismaAlani.ciktiDosyasi(
                DosyaAdi.cikti(
                    kaynakDosyaAdi = girdi.gorunenAd,
                    islem = IslemTuru.SIKISTIR,
                    ekBilgi = kalite.name.lowercase(java.util.Locale.ROOT),
                ),
            )
            val sonuc = PdfSikistirici.sikistir(
                kaynak = girdi.dosya,
                kalite = kalite,
                cikti = cikti,
                ilerleme = ilerleme,
            )
            val yuzde = (sonuc.kazancOrani * 100).toInt()
            val baglam = bagimliliklar.uygulamaBaglami
            IslemCiktisi(
                dosyalar = listOf(cikti),
                sayfaSayisi = sonuc.sayfaSayisi,
                ozetSatiri = if (yuzde > 0) {
                    baglam.getString(
                        R.string.sikistir_kucculdu,
                        yuzde,
                        bicimliBoyut(sonuc.girdiBoyutu),
                        bicimliBoyut(sonuc.ciktiBoyutu),
                    )
                } else {
                    baglam.getString(R.string.sikistir_degismedi, bicimliBoyut(sonuc.ciktiBoyutu))
                },
                notlar = buildList {
                    add(
                        baglam.getString(
                            R.string.sikistir_not_yeniden_kodlandi,
                            sonuc.yenidenKodlananGorsel,
                            sonuc.toplamGorsel,
                        ),
                    )
                    if (sonuc.toplamGorsel == 0) {
                        add(baglam.getString(R.string.sikistir_not_gomulu_gorsel_yok))
                    }
                    if (yuzde <= 0) {
                        add(baglam.getString(R.string.sikistir_not_zaten_sikistirilmis))
                    }
                },
            )
        }
    }

}
