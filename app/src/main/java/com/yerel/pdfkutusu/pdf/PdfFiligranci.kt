package com.yerel.pdfkutusu.pdf

import android.content.Context
import androidx.annotation.StringRes
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.util.Matrix
import com.yerel.pdfkutusu.R
import com.yerel.pdfkutusu.cekirdek.PdfHatasi
import java.io.File
import kotlin.math.abs
import kotlin.math.max

enum class FiligranRengi(@StringRes val etiketRes: Int, val r: Float, val g: Float, val b: Float) {
    GRI(R.string.renk_gri, 0.45f, 0.45f, 0.45f),
    KIRMIZI(R.string.renk_kirmizi, 0.75f, 0.12f, 0.12f),
    MAVI(R.string.renk_mavi, 0.15f, 0.30f, 0.65f),
    SIYAH(R.string.renk_siyah, 0f, 0f, 0f),
}

data class FiligranAyarlari(
    val metin: String,
    val punto: Float = 48f,
    /** 0 = tamamen saydam, 1 = tamamen opak. */
    val saydamlik: Float = 0.22f,
    /** Gorsel acidan derece; 45 = sol alttan sag uste capraz. */
    val aci: Float = 45f,
    val renk: FiligranRengi = FiligranRengi.GRI,
    /** true ise sayfa bastan basa doseli filigranla kaplanir. */
    val doseme: Boolean = false,
)

data class FiligranSonucu(
    val islenenSayfa: Int,
    /** false ise metindeki ğ/ş/ı gibi harfler sadelestirildi. */
    val tamTurkceDestegi: Boolean,
    val kullanilanYaziTipi: String,
)

/**
 * Metin filigrani ekler.
 *
 * Filigran, var olan icerigin **ustune** eklenir (APPEND kipi) ve saydam bir
 * grafik durumu kullanir. Bu bir gizleme araci DEGILDIR - altindaki metin
 * hala secilebilir. Bilgiyi gercekten kaldirmak icin [PdfKartici] kullanin.
 */
object PdfFiligranci {

    fun uygula(
        kaynak: File,
        ayarlar: FiligranAyarlari,
        cikti: File,
        sayfaIndeksleri: List<Int>? = null,
        parola: String? = null,
        ilerleme: IlerlemeDinleyicisi = IlerlemeYok,
        baglam: Context? = null,
    ): FiligranSonucu {
        if (ayarlar.metin.isBlank()) {
            throw PdfHatasi.GirdiYok(
                baglam?.getString(R.string.filigran_metin_bos_olamaz) ?: "Filigran metni boş olamaz.",
            )
        }

        BelgeErisimi.ac(kaynak, parola).use { belge ->
            BelgeErisimi.guvenligiKaldir(belge)
            val toplam = belge.numberOfPages
            val hedefler = sayfaIndeksleri ?: (0 until toplam).toList()
            val hatali = hedefler.firstOrNull { it < 0 || it >= toplam }
            if (hatali != null) {
                throw PdfHatasi.GecersizAralik(
                    baglam?.getString(R.string.filigran_gecersiz_sayfa, toplam, hatali + 1)
                        ?: "Belge $toplam sayfa, ${hatali + 1}. sayfa istendi.",
                )
            }

            val yaziTipi = YaziTipi.yukle(belge, baglam)
            val metin = yaziTipi.hazirla(ayarlar.metin)

            val grafikDurumu = PDExtendedGraphicsState().apply {
                nonStrokingAlphaConstant = ayarlar.saydamlik.coerceIn(0.02f, 1f)
                strokingAlphaConstant = ayarlar.saydamlik.coerceIn(0.02f, 1f)
            }

            hedefler.forEachIndexed { sira, indeks ->
                sayfayaYaz(belge, indeks, metin, ayarlar, yaziTipi, grafikDurumu)
                ilerleme(Ilerleme(sira + 1, hedefler.size, "Sayfa ${indeks + 1}"))
            }

            MetaVeriTemizleyici.temizle(belge)
            belge.save(cikti)
            return FiligranSonucu(hedefler.size, yaziTipi.tamTurkceDestegi, yaziTipi.kaynakAdi)
        }
    }

    private fun sayfayaYaz(
        belge: com.tom_roush.pdfbox.pdmodel.PDDocument,
        indeks: Int,
        metin: String,
        ayarlar: FiligranAyarlari,
        yaziTipi: YaziTipi,
        grafikDurumu: PDExtendedGraphicsState,
    ) {
        val sayfa = belge.getPage(indeks)
        val kutu = sayfa.cropBox ?: sayfa.mediaBox
        val genislik = kutu.width
        val yukseklik = kutu.height

        // Sayfa /Rotate ile dondurulmusse icerik koordinat sistemi donmez;
        // goruntuleyici tum sayfayi cevirir. Filigranin EKRANDA istenen acida
        // gorunmesi icin icerik acisina sayfa dondurmesini ekliyoruz.
        val gorselAci = ayarlar.aci + sayfa.rotation
        val radyan = Math.toRadians(gorselAci.toDouble())

        val metinGenisligi = max(1f, yaziTipi.genislik(metin, ayarlar.punto))
        val metinYuksekligi = ayarlar.punto * 0.72f

        PDPageContentStream(
            belge,
            sayfa,
            PDPageContentStream.AppendMode.APPEND,
            /* compress = */ true,
            /* resetContext = */ true,
        ).use { akis ->
            akis.saveGraphicsState()
            akis.setGraphicsStateParameters(grafikDurumu)
            akis.setNonStrokingColor(ayarlar.renk.r, ayarlar.renk.g, ayarlar.renk.b)
            akis.beginText()
            akis.setFont(yaziTipi.font, ayarlar.punto)

            if (ayarlar.doseme) {
                val adimX = metinGenisligi * 1.35f
                val adimY = max(metinYuksekligi * 4f, ayarlar.punto * 3f)
                // Dondurulmus metin kosegen tasar; kenarlardan tasmayi gozeterek
                // izgarayi sayfanin disina dogru genisletiyoruz.
                val tasma = (abs(metinGenisligi) + abs(metinYuksekligi))
                var y = kutu.lowerLeftY - tasma
                while (y < kutu.lowerLeftY + yukseklik + tasma) {
                    var x = kutu.lowerLeftX - tasma
                    while (x < kutu.lowerLeftX + genislik + tasma) {
                        akis.setTextMatrix(matris(x, y, radyan, 0f, 0f))
                        akis.showText(metin)
                        x += adimX
                    }
                    y += adimY
                }
            } else {
                val merkezX = kutu.lowerLeftX + genislik / 2f
                val merkezY = kutu.lowerLeftY + yukseklik / 2f
                akis.setTextMatrix(
                    matris(merkezX, merkezY, radyan, -metinGenisligi / 2f, -metinYuksekligi / 2f),
                )
                akis.showText(metin)
            }

            akis.endText()
            akis.restoreGraphicsState()
        }
    }

    /** Once merkeze tasi, sonra dondur, sonra metni kendi icinde ortala. */
    private fun matris(x: Float, y: Float, radyan: Double, kaydirX: Float, kaydirY: Float): Matrix =
        Matrix().apply {
            translate(x, y)
            rotate(radyan)
            translate(kaydirX, kaydirY)
        }
}
