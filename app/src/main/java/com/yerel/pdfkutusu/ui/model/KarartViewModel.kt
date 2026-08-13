package com.yerel.pdfkutusu.ui.model

import com.yerel.pdfkutusu.Bagimliliklar
import com.yerel.pdfkutusu.cekirdek.DosyaAdi
import com.yerel.pdfkutusu.cekirdek.IslemTuru
import com.yerel.pdfkutusu.cekirdek.PdfHatasi
import com.yerel.pdfkutusu.pdf.KarartmaAlani
import com.yerel.pdfkutusu.pdf.PdfKartici
import com.yerel.pdfkutusu.ui.ortak.bicimliBoyut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class KarartmaEkranDurumu(
    val secilenSayfa: Int = 0,
    val alanlar: List<KarartmaAlani> = emptyList(),
    val dpi: Int = PdfKartici.ASGARI_DPI,
) {
    fun sayfaninAlanlari(sayfa: Int): List<KarartmaAlani> = alanlar.filter { it.sayfaIndeksi == sayfa }
    val karartilanSayfaSayisi: Int get() = alanlar.map { it.sayfaIndeksi }.distinct().size
}

/**
 * Karartma ekrani.
 *
 * Kullanici onizleme uzerinde dikdortgen cizer; koordinatlar 0..1 araliginda
 * normalize saklanir, boylece onizleme cozunurlugu ile ciktinin cozunurlugu
 * birbirinden bagimsizdir.
 */
class KarartViewModel(bagimliliklar: Bagimliliklar) :
    AracViewModel(bagimliliklar, IslemTuru.KARART) {

    private val _karartma = MutableStateFlow(KarartmaEkranDurumu())
    val karartma: StateFlow<KarartmaEkranDurumu> = _karartma.asStateFlow()

    /** DPI secenekleri; 200'un altina inilemez. */
    val dpiSecenekleri = listOf(200, 300, 400)

    override fun girdilerDegisti() {
        _karartma.value = KarartmaEkranDurumu()
    }

    fun sayfaSec(indeks: Int) = _karartma.update { it.copy(secilenSayfa = indeks) }

    fun dpiDegistir(deger: Int) =
        _karartma.update { it.copy(dpi = deger.coerceAtLeast(PdfKartici.ASGARI_DPI)) }

    fun alanEkle(sol: Float, ust: Float, sag: Float, alt: Float) {
        val sayfa = _karartma.value.secilenSayfa
        val alan = KarartmaAlani(sayfa, sol, ust, sag, alt).duzelt()
        if (!alan.gecerliMi) return
        _karartma.update { it.copy(alanlar = it.alanlar + alan) }
    }

    fun sonAlaniGeriAl() {
        _karartma.update { durum ->
            val sonIndeks = durum.alanlar.indexOfLast { it.sayfaIndeksi == durum.secilenSayfa }
            if (sonIndeks < 0) durum
            else durum.copy(alanlar = durum.alanlar.filterIndexed { i, _ -> i != sonIndeks })
        }
    }

    fun sayfayiTemizle() {
        _karartma.update { durum ->
            durum.copy(alanlar = durum.alanlar.filterNot { it.sayfaIndeksi == durum.secilenSayfa })
        }
    }

    fun tumunuTemizleAlanlar() = _karartma.update { it.copy(alanlar = emptyList()) }

    fun karart() {
        val girdi = durum.value.ilkGirdi ?: return
        val ayar = _karartma.value
        if (ayar.alanlar.isEmpty()) {
            guncelle {
                it.copy(hata = PdfHatasi.GirdiYok("Karartmak için önizleme üzerinde en az bir alan çizin."))
            }
            return
        }

        calistir { ilerleme ->
            val cikti = calismaAlani.ciktiDosyasi(
                DosyaAdi.cikti(girdi.gorunenAd, IslemTuru.KARART),
            )
            val sonuc = PdfKartici.karart(
                kaynak = girdi.dosya,
                alanlar = ayar.alanlar,
                cikti = cikti,
                rasterlestirici = rasterlestirici,
                gecicilerDizini = calismaAlani.gecicilerDizini,
                dpi = ayar.dpi,
                ilerleme = ilerleme,
            )
            IslemCiktisi(
                dosyalar = listOf(cikti),
                sayfaSayisi = sonuc.toplamSayfa,
                ozetSatiri = "${sonuc.karartilanSayfalar.size} sayfa karartıldı · " +
                    bicimliBoyut(sonuc.ciktiBoyutuBayt),
                notlar = listOf(
                    "Karartılan sayfalar ${sonuc.kullanilanDpi} DPI görüntüye çevrildi. " +
                        "Bu sayfalarda metin artık seçilemez ve aranamaz.",
                    "Diğer sayfalara dokunulmadı; metinleri seçilebilir kaldı.",
                    "Belge meta verileri (yazar, başlık, üretici) temizlendi.",
                ),
            )
        }
    }
}
