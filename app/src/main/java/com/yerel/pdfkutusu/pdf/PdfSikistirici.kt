package com.yerel.pdfkutusu.pdf

import android.graphics.Bitmap
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.cos.COSStream
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.graphics.form.PDFormXObject
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Sikistirma kalitesi secenekleri.
 *
 * Sikistirma yalnizca **gomulu gorseller** uzerinde calisir: goruntuler
 * yeniden orneklenir ve daha dusuk JPEG kalitesiyle yeniden kodlanir. Metin,
 * vektor cizim ve font verisi hic ellenmez - bu yuzden salt metinden olusan
 * bir PDF'te kazanc yok denecek kadar azdir. Arayuz bunu kullaniciya soyler.
 */
enum class SikistirmaKalitesi(
    val etiket: String,
    val jpegKalitesi: Float,
    val azamiKenarPiksel: Int,
    val aciklama: String,
    /** Tahminde kullanilan piksel basina yaklasik bayt. */
    val baytPiksel: Double,
) {
    YUKSEK("Yüksek", 0.85f, 2200, "Görseller hafifçe küçülür, baskı kalitesi büyük ölçüde korunur.", 0.26),
    ORTA("Orta", 0.65f, 1600, "Ekranda okumak için fazlasıyla yeterli, dosya belirgin küçülür.", 0.15),
    DUSUK("Düşük", 0.45f, 1100, "En küçük dosya; görseller gözle görülür şekilde yumuşar.", 0.085),
}

data class SikistirmaSonucu(
    val girdiBoyutu: Long,
    val ciktiBoyutu: Long,
    val yenidenKodlananGorsel: Int,
    val toplamGorsel: Int,
    val sayfaSayisi: Int,
) {
    /** 0.30 => %30 kucuktu. Negatifse dosya buyudu. */
    val kazancOrani: Float
        get() = if (girdiBoyutu <= 0) 0f else 1f - (ciktiBoyutu.toFloat() / girdiBoyutu)
}

data class SikistirmaTahmini(
    val kalite: SikistirmaKalitesi,
    val tahminiBayt: Long,
)

object PdfSikistirici {

    /** Bu boyutun altindaki gorselleri ellemek zahmete degmez. */
    private const val ASGARI_GORSEL_BAYT = 24_000

    /** Cok kucuk kazanclar icin dosyayi bozma riskini almayalim. */
    private const val ASGARI_KAZANC_ORANI = 0.92

    fun sikistir(
        kaynak: File,
        kalite: SikistirmaKalitesi,
        cikti: File,
        parola: String? = null,
        ilerleme: IlerlemeDinleyicisi = IlerlemeYok,
    ): SikistirmaSonucu {
        val girdiBoyutu = kaynak.length()

        BelgeErisimi.ac(kaynak, parola).use { belge ->
            BelgeErisimi.guvenligiKaldir(belge)
            val sayfaSayisi = belge.numberOfPages
            var toplam = 0
            var yenidenKodlanan = 0
            val gorulen = mutableSetOf<COSStream>()

            for (indeks in 0 until sayfaSayisi) {
                val kaynaklar = belge.getPage(indeks).resources
                if (kaynaklar != null) {
                    val sayim = kaynaklariIsle(belge, kaynaklar, kalite, gorulen, derinlik = 0)
                    toplam += sayim.first
                    yenidenKodlanan += sayim.second
                }
                ilerleme(Ilerleme(indeks + 1, sayfaSayisi, "Sayfa ${indeks + 1}"))
            }

            MetaVeriTemizleyici.temizle(belge)
            belge.save(cikti)

            return SikistirmaSonucu(
                girdiBoyutu = girdiBoyutu,
                ciktiBoyutu = cikti.length(),
                yenidenKodlananGorsel = yenidenKodlanan,
                toplamGorsel = toplam,
                sayfaSayisi = sayfaSayisi,
            )
        }
    }

    /**
     * @return (gorulen gorsel sayisi, yeniden kodlanan gorsel sayisi)
     */
    private fun kaynaklariIsle(
        belge: PDDocument,
        kaynaklar: PDResources,
        kalite: SikistirmaKalitesi,
        gorulen: MutableSet<COSStream>,
        derinlik: Int,
    ): Pair<Int, Int> {
        if (derinlik > 8) return 0 to 0 // Dongusel Form XObject'lere karsi guvenlik

        var toplam = 0
        var degistirilen = 0

        for (ad in kaynaklar.xObjectNames.toList()) {
            val nesne = runCatching { kaynaklar.getXObject(ad) }.getOrNull() ?: continue

            if (nesne is PDFormXObject) {
                val icKaynaklar = nesne.resources ?: continue
                val alt = kaynaklariIsle(belge, icKaynaklar, kalite, gorulen, derinlik + 1)
                toplam += alt.first
                degistirilen += alt.second
                continue
            }
            if (nesne !is PDImageXObject) continue

            toplam++
            val akis = nesne.cosObject
            if (!gorulen.add(akis)) continue // Ayni gorsel birden fazla sayfada olabilir

            if (yenidenKodla(belge, kaynaklar, ad, nesne, kalite)) degistirilen++
        }
        return toplam to degistirilen
    }

    private fun yenidenKodla(
        belge: PDDocument,
        kaynaklar: PDResources,
        ad: COSName,
        gorsel: PDImageXObject,
        kalite: SikistirmaKalitesi,
    ): Boolean {
        // Maske ve saydamlik iceren gorseller JPEG'e cevrilirse gorsel bozulur:
        // JPEG alfa kanali tasimaz. Bunlari oldugu gibi birakiyoruz.
        if (gorsel.isStencil) return false
        if (runCatching { gorsel.softMask }.getOrNull() != null) return false
        if (runCatching { gorsel.mask }.getOrNull() != null) return false

        val eskiBoyut = runCatching { gorsel.cosObject.getInt(COSName.LENGTH, 0) }.getOrDefault(0)
        if (eskiBoyut in 1 until ASGARI_GORSEL_BAYT) return false

        val kaynakBitmap = runCatching { gorsel.image }.getOrNull() ?: return false
        var olceklenmis: Bitmap? = null
        try {
            olceklenmis = olcekle(kaynakBitmap, kalite.azamiKenarPiksel)
            val yeniBaytlar = ByteArrayOutputStream().use { akis ->
                val basarili = olceklenmis.compress(
                    Bitmap.CompressFormat.JPEG,
                    (kalite.jpegKalitesi * 100).roundToInt().coerceIn(1, 100),
                    akis,
                )
                if (!basarili) return false
                akis.toByteArray()
            }

            val karsilastirmaTabani = if (eskiBoyut > 0) eskiBoyut.toLong() else Long.MAX_VALUE
            if (yeniBaytlar.size >= karsilastirmaTabani * ASGARI_KAZANC_ORANI) return false

            val yeniGorsel = ByteArrayInputStream(yeniBaytlar).use {
                JPEGFactory.createFromStream(belge, it)
            }
            kaynaklar.put(ad, yeniGorsel)
            return true
        } catch (hata: Exception) {
            // Tek bir gorsel yuzunden tum sikistirmayi kaybetmeyelim.
            return false
        } catch (hata: OutOfMemoryError) {
            return false
        } finally {
            if (olceklenmis != null && olceklenmis !== kaynakBitmap) {
                runCatching { olceklenmis.recycle() }
            }
            runCatching { kaynakBitmap.recycle() }
        }
    }

    private fun olcekle(kaynak: Bitmap, azamiKenar: Int): Bitmap {
        val enBuyukKenar = max(kaynak.width, kaynak.height)
        if (enBuyukKenar <= azamiKenar) return kaynak
        val oran = azamiKenar.toFloat() / enBuyukKenar
        val yeniEn = max(1, (kaynak.width * oran).roundToInt())
        val yeniBoy = max(1, (kaynak.height * oran).roundToInt())
        return Bitmap.createScaledBitmap(kaynak, yeniEn, yeniBoy, true)
    }

    /**
     * Sikistirmadan once kullaniciya gosterilecek tahmin.
     *
     * Gorselleri **cozmeden** yalnizca boyut ve piksel bilgisini okur; bu
     * yuzden hizlidir. Sonuc yaklasiktir ve arayuzde "~" ile gosterilir.
     */
    fun tahminEt(kaynak: File, parola: String? = null): List<SikistirmaTahmini> {
        val dosyaBoyutu = kaynak.length()
        var gorselBaytlari = 0L
        val gorselPikselleri = mutableListOf<Triple<Int, Int, Int>>() // en, boy, bayt

        runCatching {
            BelgeErisimi.ac(kaynak, parola).use { belge ->
                val gorulen = mutableSetOf<COSStream>()
                for (indeks in 0 until belge.numberOfPages) {
                    val kaynaklar = belge.getPage(indeks).resources ?: continue
                    envanterCikar(kaynaklar, gorulen, gorselPikselleri, 0)
                }
            }
        }
        gorselBaytlari = gorselPikselleri.sumOf { it.third.toLong() }
        val gorselDisiBaytlar = max(0L, dosyaBoyutu - gorselBaytlari)

        return SikistirmaKalitesi.entries.map { kalite ->
            val yeniGorselBaytlari = gorselPikselleri.sumOf { (en, boy, bayt) ->
                if (bayt < ASGARI_GORSEL_BAYT) {
                    bayt.toLong()
                } else {
                    val enBuyukKenar = max(en, boy)
                    val oran = min(1.0, kalite.azamiKenarPiksel.toDouble() / max(1, enBuyukKenar))
                    val yeniPiksel = en.toDouble() * boy.toDouble() * oran * oran
                    min(bayt.toLong(), (yeniPiksel * kalite.baytPiksel).toLong())
                }
            }
            SikistirmaTahmini(kalite, gorselDisiBaytlar + yeniGorselBaytlari)
        }
    }

    private fun envanterCikar(
        kaynaklar: PDResources,
        gorulen: MutableSet<COSStream>,
        toplayici: MutableList<Triple<Int, Int, Int>>,
        derinlik: Int,
    ) {
        if (derinlik > 8) return
        for (ad in kaynaklar.xObjectNames.toList()) {
            val nesne = runCatching { kaynaklar.getXObject(ad) }.getOrNull() ?: continue
            if (nesne is PDFormXObject) {
                nesne.resources?.let { envanterCikar(it, gorulen, toplayici, derinlik + 1) }
                continue
            }
            if (nesne !is PDImageXObject) continue
            if (!gorulen.add(nesne.cosObject)) continue
            val bayt = runCatching { nesne.cosObject.getInt(COSName.LENGTH, 0) }.getOrDefault(0)
            val en = runCatching { nesne.width }.getOrDefault(0)
            val boy = runCatching { nesne.height }.getOrDefault(0)
            if (en > 0 && boy > 0) toplayici += Triple(en, boy, bayt)
        }
    }
}
