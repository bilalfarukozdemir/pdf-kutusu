package com.yerel.pdfkutusu.ui.model

import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.viewModelScope
import com.yerel.pdfkutusu.Bagimliliklar
import com.yerel.pdfkutusu.cekirdek.DosyaAdi
import com.yerel.pdfkutusu.cekirdek.IslemTuru
import com.yerel.pdfkutusu.cekirdek.PdfHatasi
import com.yerel.pdfkutusu.depo.CalismaDosyasi
import com.yerel.pdfkutusu.pdf.BelgeOzeti
import com.yerel.pdfkutusu.pdf.GorselGirdisi
import com.yerel.pdfkutusu.pdf.ResimdenPdf
import com.yerel.pdfkutusu.pdf.ResimdenPdfAyarlari
import com.yerel.pdfkutusu.pdf.SayfaDuzeni
import com.yerel.pdfkutusu.pdf.SikistirmaKalitesi
import com.yerel.pdfkutusu.ui.ortak.bicimliBoyut
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ResimSecenekleri(
    val duzen: SayfaDuzeni = SayfaDuzeni.A4_SIGDIR,
    val kenarBoslguMm: Int = 10,
    val dpi: Int = 150,
    val kalite: SikistirmaKalitesi = SikistirmaKalitesi.ORTA,
    val tahminiBayt: Long = 0,
    val tahminHesaplaniyor: Boolean = false,
)

/**
 * Resimden PDF ekrani.
 *
 * Dosya alma, parola akisi (gorsellerde kullanilmaz ama zarari yok), ilerleme,
 * iptal ve gunluk yazma mantigi [AracViewModel]'den geliyor; burada yalnizca
 * gorsele ozgu kisimlar var.
 */
class ResimdenPdfViewModel(bagimliliklar: Bagimliliklar) :
    AracViewModel(bagimliliklar, IslemTuru.RESIMDEN_PDF, tekGirdi = false) {

    val gorselOnizleme get() = bagimliliklar.gorselOnizleme

    private val _secenekler = MutableStateFlow(ResimSecenekleri())
    val secenekler: StateFlow<ResimSecenekleri> = _secenekler.asStateFlow()

    private var tahminIsi: Job? = null

    // ------------------------------------------------------------ girdi

    /**
     * Gorsel girdisi PDF degil; [com.yerel.pdfkutusu.pdf.BelgeIncelemesi]
     * yerine yalnizca cozulebilirligini dogruluyoruz. Her gorsel bir sayfa
     * oldugu icin ozette sayfa sayisi 1.
     */
    override suspend fun girdiyiIncele(calisma: CalismaDosyasi): GirdiOgesi =
        withContext(Dispatchers.IO) {
            val olcum = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            runCatching {
                calisma.dosya.inputStream().use { BitmapFactory.decodeStream(it, null, olcum) }
            }
            if (olcum.outWidth <= 0 || olcum.outHeight <= 0) {
                throw PdfHatasi.BozukBelge(
                    "Görsel açılamadı: ${calisma.gorunenAd}. Biçim tanınmadı ya da dosya bozuk.",
                )
            }
            GirdiOgesi(
                dosya = calisma.dosya,
                gorunenAd = calisma.gorunenAd,
                boyut = calisma.boyut,
                sha256 = calisma.sha256,
                ozet = BelgeOzeti(
                    sayfaSayisi = 1,
                    sifreliydi = false,
                    metinKatmaniVar = false,
                    uyarilar = emptyList(),
                ),
            )
        }

    // ------------------------------------------------------------ secenekler

    fun duzenDegistir(deger: SayfaDuzeni) {
        _secenekler.update { it.copy(duzen = deger) }
        tahminHesapla()
    }

    fun kenarBoslguDegistir(deger: Int) = _secenekler.update { it.copy(kenarBoslguMm = deger) }

    fun dpiDegistir(deger: Int) = _secenekler.update { it.copy(dpi = deger) }

    fun kaliteDegistir(deger: SikistirmaKalitesi) {
        _secenekler.update { it.copy(kalite = deger) }
        tahminHesapla()
    }

    // ------------------------------------------------------------ siralama

    fun adaGoreSirala() = girdileriYenidenSirala { liste ->
        liste.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.gorunenAd })
    }

    /**
     * Cekilme tarihine gore siralar. Dosyanin `lastModified` degeri ise
     * yaramaz - calisma alanina kopyalandigi an damgalanir. Bu yuzden EXIF
     * `DateTimeOriginal` etiketi okunur; yoksa oge sona alinir.
     */
    fun tariheGoreSirala() {
        viewModelScope.launch {
            val damgalar = withContext(Dispatchers.IO) {
                durum.value.girdiler.associate { oge -> oge.dosya.absolutePath to cekilmeZamani(oge.dosya) }
            }
            girdileriYenidenSirala { liste ->
                liste.sortedBy { damgalar[it.dosya.absolutePath] ?: "9999" }
            }
            bilgiVer("Çekilme tarihine göre sıralandı.")
        }
    }

    private fun cekilmeZamani(dosya: File): String? = runCatching {
        dosya.inputStream().use { akis ->
            val exif = ExifInterface(akis)
            exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
        }
    }.getOrNull()

    // ------------------------------------------------------------ tahmin

    override fun girdilerDegisti() {
        tahminHesapla()
    }

    private fun tahminHesapla() {
        tahminIsi?.cancel()
        val dosyalar = durum.value.girdiler.map { it.dosya }
        if (dosyalar.isEmpty()) {
            _secenekler.update { it.copy(tahminiBayt = 0, tahminHesaplaniyor = false) }
            return
        }
        _secenekler.update { it.copy(tahminHesaplaniyor = true) }
        tahminIsi = viewModelScope.launch {
            val kalite = _secenekler.value.kalite
            val tahmin = withContext(Dispatchers.IO) {
                runCatching { ResimdenPdf.tahminEt(dosyalar, kalite) }.getOrDefault(0L)
            }
            _secenekler.update { it.copy(tahminiBayt = tahmin, tahminHesaplaniyor = false) }
        }
    }

    // ------------------------------------------------------------ calistir

    fun olustur() {
        val girdiler = durum.value.girdiler
        if (girdiler.isEmpty()) {
            guncelle { it.copy(hata = PdfHatasi.GirdiYok("En az bir görsel seçin.")) }
            return
        }
        val ayarlar = _secenekler.value

        calistir { ilerleme ->
            val cikti = calismaAlani.ciktiDosyasi(ciktiAdiUret(girdiler.first().gorunenAd))
            val sonuc = ResimdenPdf.olustur(
                girdiler = girdiler.map { GorselGirdisi(it.dosya, it.gorunenAd) },
                ayarlar = ResimdenPdfAyarlari(
                    duzen = ayarlar.duzen,
                    kenarBoslguMm = ayarlar.kenarBoslguMm,
                    dpi = ayarlar.dpi,
                    kalite = ayarlar.kalite,
                ),
                cikti = cikti,
                gecicilerDizini = calismaAlani.gecicilerDizini,
                ilerleme = ilerleme,
            )

            IslemCiktisi(
                dosyalar = listOf(cikti),
                sayfaSayisi = sonuc.sayfaSayisi,
                ozetSatiri = "${sonuc.sayfaSayisi} sayfa · " + bicimliBoyut(sonuc.ciktiBoyutuBayt),
                notlar = buildList {
                    add("Görsellerin EXIF verisi (GPS konumu, cihaz modeli, çekim tarihi) çıktıya aktarılmadı.")
                    if (sonuc.atlananlar.isNotEmpty()) {
                        add("${sonuc.atlananlar.size} görsel atlandı:")
                        sonuc.atlananlar.take(10).forEach { add("   ${it.ad} — ${it.neden}") }
                        if (sonuc.atlananlar.size > 10) {
                            add("   … ve ${sonuc.atlananlar.size - 10} tane daha")
                        }
                    }
                },
            )
        }
    }

    /** Ilk gorselin adi kullanilamazsa `resimler__<zaman>.pdf`. */
    private fun ciktiAdiUret(ilkAd: String): String {
        val taban = DosyaAdi.tabani(DosyaAdi.guvenli(ilkAd)).trim()
        val kullanilabilir = taban.isNotEmpty() &&
            !taban.equals("belge", ignoreCase = true) &&
            taban.any { it.isLetterOrDigit() }
        return DosyaAdi.cikti(
            kaynakDosyaAdi = if (kullanilabilir) ilkAd else "resimler",
            islem = IslemTuru.RESIMDEN_PDF,
        )
    }

    override fun gunlukGirdiAdi(girdiler: List<GirdiOgesi>): String =
        "${girdiler.size} görsel: " +
            girdiler.joinToString(", ") { it.gorunenAd }.take(240)

    private companion object {
        val TURKCE = Locale("tr", "TR")
    }
}
