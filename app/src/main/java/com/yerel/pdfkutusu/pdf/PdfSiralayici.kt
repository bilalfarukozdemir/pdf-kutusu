package com.yerel.pdfkutusu.pdf

import com.yerel.pdfkutusu.cekirdek.PdfHatasi
import java.io.File

/**
 * Sayfalari yeniden siralar (surukle-birak arayuzunun karsiligi).
 *
 * Sayfa silmeye de izin verir: [yeniSira] icinde olmayan sayfalar ciktida
 * yer almaz. Kaynak dosya degistirilmez.
 */
object PdfSiralayici {

    /**
     * @param yeniSira 0-tabanli kaynak sayfa indeksleri, istenen sirayla
     * @return cikti sayfa sayisi
     */
    fun sirala(
        kaynak: File,
        yeniSira: List<Int>,
        cikti: File,
        parola: String? = null,
        ilerleme: IlerlemeDinleyicisi = IlerlemeYok,
    ): Int {
        if (yeniSira.isEmpty()) throw PdfHatasi.GecersizAralik("Çıktıda en az bir sayfa kalmalı.")

        BelgeErisimi.ac(kaynak, parola).use { belge ->
            BelgeErisimi.guvenligiKaldir(belge)
            val toplam = belge.numberOfPages
            val hataliIndeks = yeniSira.firstOrNull { it < 0 || it >= toplam }
            if (hataliIndeks != null) {
                throw PdfHatasi.GecersizAralik(
                    "Sıralama geçersiz: belge $toplam sayfa, ${hataliIndeks + 1}. sayfa istendi.",
                )
            }

            SayfaKopyalayici.kopyala(belge, yeniSira, ilerleme).use { hedef ->
                MetaVeriTemizleyici.temizle(hedef)
                hedef.save(cikti)
                return hedef.numberOfPages
            }
        }
    }
}
