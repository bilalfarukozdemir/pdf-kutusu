package com.yerel.pdfkutusu.ui.model

import com.yerel.pdfkutusu.Bagimliliklar
import com.yerel.pdfkutusu.cekirdek.DosyaAdi
import com.yerel.pdfkutusu.cekirdek.IslemTuru
import com.yerel.pdfkutusu.cekirdek.SayfaAraligi
import com.yerel.pdfkutusu.pdf.FiligranAyarlari
import com.yerel.pdfkutusu.pdf.FiligranRengi
import com.yerel.pdfkutusu.pdf.PdfFiligranci
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FiligranEkranAyarlari(
    val metin: String = "TASLAK",
    val punto: Float = 56f,
    val saydamlik: Float = 0.22f,
    val aci: Float = 45f,
    val renk: FiligranRengi = FiligranRengi.GRI,
    val doseme: Boolean = false,
    val tumSayfalar: Boolean = true,
    val aralikIfadesi: String = "",
)

class FiligranViewModel(bagimliliklar: Bagimliliklar) :
    AracViewModel(bagimliliklar, IslemTuru.FILIGRAN) {

    private val _ayarlar = MutableStateFlow(FiligranEkranAyarlari())
    val ayarlar: StateFlow<FiligranEkranAyarlari> = _ayarlar.asStateFlow()

    fun metinDegistir(deger: String) = _ayarlar.update { it.copy(metin = deger) }
    fun puntoDegistir(deger: Float) = _ayarlar.update { it.copy(punto = deger) }
    fun saydamlikDegistir(deger: Float) = _ayarlar.update { it.copy(saydamlik = deger) }
    fun aciDegistir(deger: Float) = _ayarlar.update { it.copy(aci = deger) }
    fun renkDegistir(deger: FiligranRengi) = _ayarlar.update { it.copy(renk = deger) }
    fun dosemeDegistir(deger: Boolean) = _ayarlar.update { it.copy(doseme = deger) }
    fun tumSayfalarDegistir(deger: Boolean) = _ayarlar.update { it.copy(tumSayfalar = deger) }
    fun aralikDegistir(deger: String) = _ayarlar.update { it.copy(aralikIfadesi = deger) }

    override fun girdilerDegisti() {
        val sayfa = durum.value.ilkGirdi?.sayfaSayisi ?: return
        if (_ayarlar.value.aralikIfadesi.isBlank()) {
            _ayarlar.update { it.copy(aralikIfadesi = if (sayfa > 1) "1-$sayfa" else "1") }
        }
    }

    fun uygula() {
        val girdi = durum.value.ilkGirdi ?: return
        val a = _ayarlar.value

        calistir { ilerleme ->
            val indeksler = if (a.tumSayfalar) {
                null
            } else {
                SayfaAraligi.ayristir(a.aralikIfadesi, girdi.sayfaSayisi)
            }
            val cikti = calismaAlani.ciktiDosyasi(
                DosyaAdi.cikti(girdi.gorunenAd, IslemTuru.FILIGRAN),
            )
            val sonuc = PdfFiligranci.uygula(
                kaynak = girdi.dosya,
                ayarlar = FiligranAyarlari(
                    metin = a.metin,
                    punto = a.punto,
                    saydamlik = a.saydamlik,
                    aci = a.aci,
                    renk = a.renk,
                    doseme = a.doseme,
                ),
                cikti = cikti,
                sayfaIndeksleri = indeksler,
                ilerleme = ilerleme,
            )
            IslemCiktisi(
                dosyalar = listOf(cikti),
                sayfaSayisi = girdi.sayfaSayisi,
                ozetSatiri = "${sonuc.islenenSayfa} sayfaya filigran eklendi",
                notlar = buildList {
                    add("Filigran içeriğin üstüne çizilir; altındaki metin hâlâ seçilebilir. Bilgiyi gerçekten gizlemek için Karart aracını kullanın.")
                    if (!sonuc.tamTurkceDestegi) {
                        add("Cihazda gömülebilir Unicode yazı tipi bulunamadı. ğ/ş/ı harfleri g/s/i olarak yazıldı (${sonuc.kullanilanYaziTipi}).")
                    }
                },
            )
        }
    }
}
