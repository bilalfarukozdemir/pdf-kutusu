package com.yerel.pdfkutusu.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.yerel.pdfkutusu.cekirdek.PdfHatasi
import java.io.File
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Karartilacak tek bir dikdortgen.
 *
 * Koordinatlar `0..1` araliginda normalize edilmistir ve **sol-ust kokenlidir**
 * (ekranda gordugunuz duzen). Boylece secim, onizlemenin cozunurlugunden
 * bagimsiz olarak saklanabilir.
 */
data class KarartmaAlani(
    val sayfaIndeksi: Int,
    val sol: Float,
    val ust: Float,
    val sag: Float,
    val alt: Float,
) {
    fun duzelt(): KarartmaAlani = KarartmaAlani(
        sayfaIndeksi = sayfaIndeksi,
        sol = min(sol, sag).coerceIn(0f, 1f),
        ust = min(ust, alt).coerceIn(0f, 1f),
        sag = max(sol, sag).coerceIn(0f, 1f),
        alt = max(ust, alt).coerceIn(0f, 1f),
    )

    val gecerliMi: Boolean get() = (sag - sol) > 0.001f && (alt - ust) > 0.001f
}

data class KarartmaSonucu(
    val karartilanSayfalar: List<Int>,
    val toplamSayfa: Int,
    val ciktiBoyutuBayt: Long,
    val kullanilanDpi: Int,
)

/**
 * GERCEK karartma.
 *
 * ## Neden dikdortgen cizmek yeterli degil
 * Mevcut sayfanin ustune siyah dikdortgen cizmek **sahte karartmadir**.
 * PDF'in icerik akisinda metin oldugu gibi durur; herhangi bir okuyucuda
 * dikdortgenin altini secip kopyalayabilir, `pdftotext` benzeri bir araca
 * verip okuyabilirsiniz. Bu uygulama bu yontemi bilerek desteklemez.
 *
 * ## Uygulanan yontem
 * 1. Sayfa [PdfKartici.ASGARI_DPI] (200) ya da ustunde bir cozunurlukte
 *    bitmap'e render edilir.
 * 2. Secilen dikdortgenler bitmap'in **piksellerine** opak siyah boyanir.
 *    Alttaki metin, vektor cizim, gomulu gorsel - hepsi piksel duzeyinde yok
 *    olur, cunku artik ortada yalnizca piksel vardir.
 * 3. Sayfa bu bitmap'ten yeniden olusturulur; ciktida o sayfanin metin akisi
 *    hic bulunmaz.
 * 4. Belge meta verileri temizlenir.
 *
 * Karartilmayan sayfalar dokunulmadan kopyalanir; metinleri secilebilir kalir.
 */
object PdfKartici {

    /** Sartname geregi taban cozunurluk. Daha dusugune izin verilmez. */
    const val ASGARI_DPI = 200

    /**
     * Siyah alanin JPEG blok sinirlarina hizalanmasi. JPEG 8x8 DCT bloklariyla
     * calisir; dikdortgeni blok sinirina tasirarak kenarlarda bulanik bir
     * gecis bandi olusmasini engelleriz.
     */
    private const val BLOK = 8

    /** Secim kenarinda kil payi kalmasini onlemek icin piksel payi. */
    private const val PAY_PIKSEL = 3

    fun karart(
        kaynak: File,
        alanlar: List<KarartmaAlani>,
        cikti: File,
        rasterlestirici: SayfaRasterlestirici,
        gecicilerDizini: File,
        dpi: Int = ASGARI_DPI,
        jpegKalitesi: Float = 0.92f,
        parola: String? = null,
        ilerleme: IlerlemeDinleyicisi = IlerlemeYok,
    ): KarartmaSonucu {
        val gecerliAlanlar = alanlar.map { it.duzelt() }.filter { it.gecerliMi }
        if (gecerliAlanlar.isEmpty()) {
            throw PdfHatasi.GirdiYok("Karartmak için en az bir alan seçin.")
        }
        val kullanilanDpi = max(ASGARI_DPI, dpi)
        val sayfayaGore = gecerliAlanlar.groupBy { it.sayfaIndeksi }

        val (rasterKaynak, geciciMi) = RasterHazirligi.hazirla(kaynak, parola, gecicilerDizini)
        try {
            BelgeErisimi.ac(kaynak, parola).use { kaynakBelge ->
                BelgeErisimi.guvenligiKaldir(kaynakBelge)
                val toplamSayfa = kaynakBelge.numberOfPages

                val sinirDisi = sayfayaGore.keys.firstOrNull { it < 0 || it >= toplamSayfa }
                if (sinirDisi != null) {
                    throw PdfHatasi.GecersizAralik(
                        "Belge $toplamSayfa sayfa, ${sinirDisi + 1}. sayfa karartılmak istendi.",
                    )
                }

                rasterlestirici.ac(rasterKaynak).use { oturum ->
                    PDDocument().use { hedef ->
                        for (indeks in 0 until toplamSayfa) {
                            val sayfaAlanlari = sayfayaGore[indeks]
                            if (sayfaAlanlari.isNullOrEmpty()) {
                                // Dokunulmayan sayfa: metni secilebilir kalir.
                                // Dogrudan hedefe aktariyoruz; ara bir belge acilirsa
                                // o belge kapandiginda COS nesneleri gecersiz olur.
                                SayfaKopyalayici.sayfaAktar(hedef, kaynakBelge.getPage(indeks))
                            } else {
                                rasterSayfaEkle(
                                    hedef = hedef,
                                    oturum = oturum,
                                    indeks = indeks,
                                    alanlar = sayfaAlanlari,
                                    dpi = kullanilanDpi,
                                    jpegKalitesi = jpegKalitesi,
                                )
                            }
                            ilerleme(Ilerleme(indeks + 1, toplamSayfa, "Sayfa ${indeks + 1}"))
                        }

                        // Karartmada meta veri temizligi opsiyonel degil.
                        MetaVeriTemizleyici.temizle(hedef)
                        hedef.save(cikti)
                    }
                }

                return KarartmaSonucu(
                    karartilanSayfalar = sayfayaGore.keys.sorted(),
                    toplamSayfa = toplamSayfa,
                    ciktiBoyutuBayt = cikti.length(),
                    kullanilanDpi = kullanilanDpi,
                )
            }
        } finally {
            if (geciciMi) runCatching { rasterKaynak.delete() }
        }
    }

    /**
     * Tek bir sayfayi rasterlestirir, karartir ve hedef belgeye gorsel sayfa
     * olarak ekler.
     *
     * Not: [SayfaKopyalayici] burada bilerek kullanilmaz. Amac, kaynak sayfanin
     * icerik akisindan **hicbir seyin** hedefe gecmemesi.
     */
    private fun rasterSayfaEkle(
        hedef: PDDocument,
        oturum: SayfaRasterlestirici.Oturum,
        indeks: Int,
        alanlar: List<KarartmaAlani>,
        dpi: Int,
        jpegKalitesi: Float,
    ) {
        val boyut = oturum.noktaBoyutu(indeks)
        val bitmap = oturum.rasterlestir(indeks, dpi)
        try {
            karaBoya(bitmap, alanlar)

            val gorsel = JPEGFactory.createFromImage(hedef, bitmap, jpegKalitesi)
            // Sayfa boyutu nokta cinsinden korunur; dondurme acisi 0'dir cunku
            // render edilmis bitmap zaten gorsel yonundedir.
            val sayfa = PDPage(PDRectangle(boyut.genislik, boyut.yukseklik))
            sayfa.rotation = 0
            hedef.addPage(sayfa)

            PDPageContentStream(hedef, sayfa).use { akis ->
                akis.drawImage(gorsel, 0f, 0f, boyut.genislik, boyut.yukseklik)
            }
        } finally {
            runCatching { bitmap.recycle() }
        }
    }

    /** Dikdortgenleri bitmap piksellerine opak siyah boyar. */
    internal fun karaBoya(bitmap: Bitmap, alanlar: List<KarartmaAlani>) {
        val tuval = Canvas(bitmap)
        val firca = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = false // Kenarda yari saydam piksel istemiyoruz.
            alpha = 255
        }
        val en = bitmap.width
        val boy = bitmap.height

        for (alan in alanlar) {
            val duzgun = alan.duzelt()
            var sol = floor(duzgun.sol * en).toInt() - PAY_PIKSEL
            var ust = floor(duzgun.ust * boy).toInt() - PAY_PIKSEL
            var sag = ceil(duzgun.sag * en).toInt() + PAY_PIKSEL
            var alt = ceil(duzgun.alt * boy).toInt() + PAY_PIKSEL

            // JPEG blok sinirlarina disari dogru hizala.
            sol = (floor(sol.toFloat() / BLOK).toInt() * BLOK).coerceIn(0, en)
            ust = (floor(ust.toFloat() / BLOK).toInt() * BLOK).coerceIn(0, boy)
            sag = (ceil(sag.toFloat() / BLOK).toInt() * BLOK).coerceIn(0, en)
            alt = (ceil(alt.toFloat() / BLOK).toInt() * BLOK).coerceIn(0, boy)

            if (sag > sol && alt > ust) {
                tuval.drawRect(sol.toFloat(), ust.toFloat(), sag.toFloat(), alt.toFloat(), firca)
            }
        }
    }

    /** Arayuzde gosterilecek tahmini cikti buyumesi (kaba). */
    fun tahminiBoyutBayt(sayfaSayisi: Int, dpi: Int): Long {
        // A4 @200 DPI, kalite 0.92 -> ~350 KB/sayfa gozlemlenen ortalama.
        // roundToLong: roundToInt Int.MAX_VALUE'da doyuyor ve ~6.100 sayfadan
        // sonra tahmin 2,1 GB'da donuyordu.
        val olcek = (dpi.toDouble() / ASGARI_DPI).let { it * it }
        return (sayfaSayisi * 350_000L * olcek).roundToLong()
    }
}
