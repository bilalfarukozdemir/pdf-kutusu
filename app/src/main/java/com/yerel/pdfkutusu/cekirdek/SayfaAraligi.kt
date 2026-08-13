package com.yerel.pdfkutusu.cekirdek

/**
 * `"1-3, 5, 8-"` gibi kullanici ifadelerini sayfa indekslerine cevirir.
 *
 * Kullanici 1-tabanli konusur, kod 0-tabanli calisir; donusum burada tek
 * noktada yapilir.
 *
 * Kabul edilen bicimler:
 *  - `5`     tek sayfa
 *  - `2-5`   kapali aralik
 *  - `3-`    3'ten sona kadar
 *  - `-4`    bastan 4'e kadar
 *  - `tumu`  tum sayfalar
 *
 * Ayiricilar: virgul veya noktali virgul. Tire yerine kisa/uzun tire
 * (`–`, `—`) de kabul edilir; kullanicilar bunlari klavyeden ya da
 * kopyala-yapistir ile kolayca uretiyor.
 */
object SayfaAraligi {

    private val TUMU_ANAHTARLARI = setOf("tumu", "tümü", "hepsi", "all", "*")

    fun tumu(toplamSayfa: Int): List<Int> = (0 until toplamSayfa).toList()

    /**
     * @return artan sirali, tekrarsiz, 0-tabanli sayfa indeksleri
     * @throws PdfHatasi.GecersizAralik ifade bos, bozuk ya da sinir disiysa
     */
    fun ayristir(ifade: String, toplamSayfa: Int): List<Int> {
        if (toplamSayfa <= 0) {
            throw PdfHatasi.GecersizAralik("Belgede sayfa yok.")
        }
        val duzeltilmis = ifade
            .replace('–', '-')
            .replace('—', '-')
            .replace('−', '-')
            .replace(';', ',')
            .trim()

        if (duzeltilmis.isEmpty()) {
            throw PdfHatasi.GecersizAralik("Sayfa aralığı boş olamaz.")
        }
        if (duzeltilmis.lowercase(java.util.Locale.ROOT) in TUMU_ANAHTARLARI) {
            return tumu(toplamSayfa)
        }

        val toplanan = sortedSetOf<Int>()
        for (hamParca in duzeltilmis.split(',')) {
            val parca = hamParca.trim()
            if (parca.isEmpty()) continue // "1,,3" ya da sondaki virgul hosgorulur

            val tire = parca.indexOf('-')
            if (tire < 0) {
                val sayfa = sayiya(parca, parca)
                dogrula(sayfa, toplamSayfa, parca)
                toplanan.add(sayfa - 1)
                continue
            }

            if (parca.indexOf('-', tire + 1) >= 0) {
                throw PdfHatasi.GecersizAralik("Geçersiz aralık: \"$parca\" (birden fazla tire var).")
            }

            val solMetin = parca.substring(0, tire).trim()
            val sagMetin = parca.substring(tire + 1).trim()
            if (solMetin.isEmpty() && sagMetin.isEmpty()) {
                throw PdfHatasi.GecersizAralik("Geçersiz aralık: \"$parca\".")
            }

            val bas = if (solMetin.isEmpty()) 1 else sayiya(solMetin, parca)
            val son = if (sagMetin.isEmpty()) toplamSayfa else sayiya(sagMetin, parca)

            dogrula(bas, toplamSayfa, parca)
            dogrula(son, toplamSayfa, parca)
            if (bas > son) {
                throw PdfHatasi.GecersizAralik(
                    "Geçersiz aralık: \"$parca\" — başlangıç ($bas) bitişten ($son) büyük.",
                )
            }
            for (sayfa in bas..son) toplanan.add(sayfa - 1)
        }

        if (toplanan.isEmpty()) {
            throw PdfHatasi.GecersizAralik("Hiçbir sayfa seçilmedi.")
        }
        return toplanan.toList()
    }

    /** 0-tabanli indeksleri kullaniciya gosterilecek metne cevirir: `[0,1,2,4]` -> `"1-3, 5"`. */
    fun bicimle(indeksler: Collection<Int>): String {
        if (indeksler.isEmpty()) return ""
        val sirali = indeksler.toSortedSet().toList()
        val parcalar = mutableListOf<String>()
        var bas = sirali.first()
        var onceki = bas
        for (i in 1..sirali.size) {
            val simdiki = sirali.getOrNull(i)
            if (simdiki != null && simdiki == onceki + 1) {
                onceki = simdiki
                continue
            }
            parcalar += if (bas == onceki) "${bas + 1}" else "${bas + 1}-${onceki + 1}"
            if (simdiki != null) {
                bas = simdiki
                onceki = simdiki
            }
        }
        return parcalar.joinToString(", ")
    }

    private fun sayiya(metin: String, parca: String): Int {
        if (metin.isEmpty() || metin.any { !it.isDigit() }) {
            throw PdfHatasi.GecersizAralik("Geçersiz aralık: \"$parca\" — yalnızca sayı ve tire kullanın.")
        }
        return metin.toIntOrNull()
            ?: throw PdfHatasi.GecersizAralik("Geçersiz aralık: \"$parca\" — sayı çok büyük.")
    }

    private fun dogrula(sayfa: Int, toplamSayfa: Int, parca: String) {
        if (sayfa < 1 || sayfa > toplamSayfa) {
            throw PdfHatasi.GecersizAralik(
                "Geçersiz aralık: \"$parca\" — belge $toplamSayfa sayfa, $sayfa istendi.",
            )
        }
    }
}
