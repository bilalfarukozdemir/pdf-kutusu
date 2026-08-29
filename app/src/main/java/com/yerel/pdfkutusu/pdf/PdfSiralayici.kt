package com.yerel.pdfkutusu.pdf

import android.content.Context
import com.yerel.pdfkutusu.R
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
        baglam: Context? = null,
    ): Int {
        if (yeniSira.isEmpty()) {
            throw PdfHatasi.GecersizAralik(
                baglam?.getString(R.string.sirala_hata_en_az_bir_sayfa_v2) ?: "Çıktıda en az bir sayfa kalmalı.",
            )
        }

        BelgeErisimi.ac(kaynak, parola).use { belge ->
            BelgeErisimi.guvenligiKaldir(belge)
            val toplam = belge.numberOfPages
            val hataliIndeks = yeniSira.firstOrNull { it < 0 || it >= toplam }
            if (hataliIndeks != null) {
                throw PdfHatasi.GecersizAralik(
                    baglam?.getString(R.string.sirala_hata_gecersiz_sayfa, toplam, hataliIndeks + 1)
                        ?: "Sıralama geçersiz: belge $toplam sayfa, ${hataliIndeks + 1}. sayfa istendi.",
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
