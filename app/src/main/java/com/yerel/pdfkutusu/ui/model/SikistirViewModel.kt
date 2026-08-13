package com.yerel.pdfkutusu.ui.model

import androidx.lifecycle.viewModelScope
import com.yerel.pdfkutusu.Bagimliliklar
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
            IslemCiktisi(
                dosyalar = listOf(cikti),
                sayfaSayisi = sonuc.sayfaSayisi,
                ozetSatiri = if (yuzde > 0) {
                    "%$yuzde küçüldü · ${bicimliBoyut(sonuc.girdiBoyutu)} → ${bicimliBoyut(sonuc.ciktiBoyutu)}"
                } else {
                    "Boyut değişmedi · ${bicimliBoyut(sonuc.ciktiBoyutu)}"
                },
                notlar = buildList {
                    add("${sonuc.yenidenKodlananGorsel}/${sonuc.toplamGorsel} görsel yeniden kodlandı.")
                    if (sonuc.toplamGorsel == 0) {
                        add("Belgede gömülü görsel yok. Sıkıştırma yalnızca görseller üzerinde çalışır; metin ve yazı tipi verisi ellenmez.")
                    }
                    if (yuzde <= 0) {
                        add("Görseller zaten iyi sıkıştırılmış olabilir. Orijinali kullanmanız daha iyi olur.")
                    }
                },
            )
        }
    }

}
