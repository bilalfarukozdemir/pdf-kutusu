package com.yerel.pdfkutusu.ui.model

import com.yerel.pdfkutusu.Bagimliliklar
import com.yerel.pdfkutusu.cekirdek.DosyaAdi
import com.yerel.pdfkutusu.cekirdek.IslemTuru
import com.yerel.pdfkutusu.cekirdek.SayfaAraligi
import com.yerel.pdfkutusu.pdf.PdfBolucu
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class BolSecenekleri(
    val aralikIfadesi: String = "",
    /** true: her aralik ayri dosya. false: secilen tum sayfalar tek dosya. */
    val ayriDosyalar: Boolean = false,
)

class BolViewModel(bagimliliklar: Bagimliliklar) :
    AracViewModel(bagimliliklar, IslemTuru.BOL) {

    private val _secenekler = MutableStateFlow(BolSecenekleri())
    val secenekler: StateFlow<BolSecenekleri> = _secenekler.asStateFlow()

    fun aralikDegistir(deger: String) = _secenekler.update { it.copy(aralikIfadesi = deger) }

    fun ayriDosyalarDegistir(deger: Boolean) = _secenekler.update { it.copy(ayriDosyalar = deger) }

    override fun girdilerDegisti() {
        val sayfa = durum.value.ilkGirdi?.sayfaSayisi ?: return
        if (_secenekler.value.aralikIfadesi.isBlank()) {
            _secenekler.update { it.copy(aralikIfadesi = if (sayfa > 1) "1-$sayfa" else "1") }
        }
    }

    fun bol() {
        val girdi = durum.value.ilkGirdi ?: return
        val ayarlar = _secenekler.value

        calistir { ilerleme ->
            if (ayarlar.ayriDosyalar) {
                val parcalar = PdfBolucu.herAraligiAyriDosyaya(
                    kaynak = girdi.dosya,
                    aralikIfadesi = ayarlar.aralikIfadesi,
                    ciktiAdiUret = { sira, indeksler ->
                        calismaAlani.ciktiDosyasi(
                            DosyaAdi.cikti(
                                kaynakDosyaAdi = girdi.gorunenAd,
                                islem = IslemTuru.BOL,
                                ekBilgi = "parca${sira + 1}-${SayfaAraligi.bicimle(indeksler).replace(", ", "_")}",
                            ),
                        )
                    },
                    ilerleme = ilerleme,
                )
                IslemCiktisi(
                    dosyalar = parcalar.map { it.dosya },
                    sayfaSayisi = parcalar.sumOf { it.sayfaSayisi },
                    ozetSatiri = "${parcalar.size} dosya üretildi · " +
                        parcalar.joinToString(" | ") { "${it.aralikMetni} (${it.sayfaSayisi} sayfa)" },
                )
            } else {
                val cikti = calismaAlani.ciktiDosyasi(
                    DosyaAdi.cikti(girdi.gorunenAd, IslemTuru.BOL),
                )
                val parca = PdfBolucu.tekDosyayaCikar(
                    kaynak = girdi.dosya,
                    aralikIfadesi = ayarlar.aralikIfadesi,
                    cikti = cikti,
                    ilerleme = ilerleme,
                )
                IslemCiktisi(
                    dosyalar = listOf(cikti),
                    sayfaSayisi = parca.sayfaSayisi,
                    ozetSatiri = "Sayfa ${parca.aralikMetni} çıkarıldı · ${parca.sayfaSayisi} sayfa",
                )
            }
        }
    }
}
