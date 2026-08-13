package com.yerel.pdfkutusu.pdf

import com.yerel.pdfkutusu.cekirdek.PdfHatasi
import com.yerel.pdfkutusu.cekirdek.SayfaAraligi
import java.io.File

/** Bolme sonucu uretilen tek dosya. */
data class BolmeParcasi(
    val dosya: File,
    val sayfaIndeksleri: List<Int>,
) {
    val sayfaSayisi: Int get() = sayfaIndeksleri.size
    val aralikMetni: String get() = SayfaAraligi.bicimle(sayfaIndeksleri)
}

/**
 * PDF'i sayfa araligi secerek boler.
 *
 * Iki kip:
 *  - [tekDosyayaCikar]: secilen tum sayfalar tek bir PDF olur.
 *  - [herAraligiAyriDosyaya]: `1-3, 7-9` ifadesi iki ayri PDF uretir.
 */
object PdfBolucu {

    fun tekDosyayaCikar(
        kaynak: File,
        aralikIfadesi: String,
        cikti: File,
        parola: String? = null,
        ilerleme: IlerlemeDinleyicisi = IlerlemeYok,
    ): BolmeParcasi {
        BelgeErisimi.ac(kaynak, parola).use { belge ->
            BelgeErisimi.guvenligiKaldir(belge)
            val indeksler = SayfaAraligi.ayristir(aralikIfadesi, belge.numberOfPages)
            SayfaKopyalayici.kopyala(belge, indeksler, ilerleme).use { hedef ->
                MetaVeriTemizleyici.temizle(hedef)
                hedef.save(cikti)
            }
            return BolmeParcasi(cikti, indeksler)
        }
    }

    /**
     * @param ciktiAdiUret aralik sirasi (0-tabanli) ve o araligin sayfa
     *   indeksleri verilerek cagrilir; dosya yolunu dondurur
     */
    fun herAraligiAyriDosyaya(
        kaynak: File,
        aralikIfadesi: String,
        parola: String? = null,
        ciktiAdiUret: (aralikSirasi: Int, indeksler: List<Int>) -> File,
        ilerleme: IlerlemeDinleyicisi = IlerlemeYok,
    ): List<BolmeParcasi> {
        BelgeErisimi.ac(kaynak, parola).use { belge ->
            BelgeErisimi.guvenligiKaldir(belge)
            val toplam = belge.numberOfPages
            val gruplar = aralikIfadesi
                .replace('–', '-').replace('—', '-').replace(';', ',')
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { SayfaAraligi.ayristir(it, toplam) }

            if (gruplar.isEmpty()) throw PdfHatasi.GecersizAralik("Sayfa aralığı boş olamaz.")

            val parcalar = mutableListOf<BolmeParcasi>()
            gruplar.forEachIndexed { sira, indeksler ->
                ilerleme(Ilerleme(sira, gruplar.size, "Parça ${sira + 1}"))
                val hedefDosya = ciktiAdiUret(sira, indeksler)
                SayfaKopyalayici.kopyala(belge, indeksler).use { hedef ->
                    MetaVeriTemizleyici.temizle(hedef)
                    hedef.save(hedefDosya)
                }
                parcalar += BolmeParcasi(hedefDosya, indeksler)
                ilerleme(Ilerleme(sira + 1, gruplar.size, "Parça ${sira + 1}"))
            }
            return parcalar
        }
    }
}
