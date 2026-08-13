package com.yerel.pdfkutusu.pdf

import com.yerel.pdfkutusu.cekirdek.PdfHatasi
import java.io.File

/**
 * Sayfa dondurme.
 *
 * PDF'te dondurme, sayfa sozlugundeki `/Rotate` anahtaridir; icerik akisi
 * degismez. Deger 90'in kati ve `0..270` araliginda **olmak zorundadir**;
 * negatif ya da 360'i asan degerler burada normallestirilir.
 */
object PdfDondurucu {

    /** Ekranda gosterilecek dondurme secenekleri. */
    val SECENEKLER = listOf(90, 180, 270)

    /**
     * @param aci 90'in kati; negatif olabilir (-90 = saat yonunun tersi)
     * @param sayfaIndeksleri null ise tum sayfalar dondurulur
     * @return dondurulen sayfa sayisi
     */
    fun dondur(
        kaynak: File,
        aci: Int,
        cikti: File,
        sayfaIndeksleri: List<Int>? = null,
        parola: String? = null,
        ilerleme: IlerlemeDinleyicisi = IlerlemeYok,
    ): Int {
        if (aci % 90 != 0) {
            throw PdfHatasi.GecersizAralik("Döndürme açısı 90'ın katı olmalı (90, 180, 270).")
        }

        BelgeErisimi.ac(kaynak, parola).use { belge ->
            BelgeErisimi.guvenligiKaldir(belge)
            val toplam = belge.numberOfPages
            val hedefler = sayfaIndeksleri ?: (0 until toplam).toList()
            val hataliIndeks = hedefler.firstOrNull { it < 0 || it >= toplam }
            if (hataliIndeks != null) {
                throw PdfHatasi.GecersizAralik(
                    "Belge $toplam sayfa, ${hataliIndeks + 1}. sayfa istendi.",
                )
            }

            hedefler.forEachIndexed { sira, indeks ->
                val sayfa = belge.getPage(indeks)
                sayfa.rotation = normalize(sayfa.rotation + aci)
                ilerleme(Ilerleme(sira + 1, hedefler.size, "Sayfa ${indeks + 1}"))
            }

            MetaVeriTemizleyici.temizle(belge)
            belge.save(cikti)
            return hedefler.size
        }
    }

    /** Herhangi bir tam sayiyi `0, 90, 180, 270` kumesine indirger. */
    fun normalize(aci: Int): Int {
        val kalan = aci % 360
        return if (kalan < 0) kalan + 360 else kalan
    }
}
