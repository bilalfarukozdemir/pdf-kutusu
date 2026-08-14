package com.yerel.pdfkutusu.okuyucu

/**
 * Okuyucudaki sayfalarin dikey yerlesimi.
 *
 * Kaydirmayi ve yakinlastirmayi elde yonettigimiz icin sayfa konumlari mutlak
 * piksel olarak burada tutulur. `LazyColumn`'un "indeks + piksel ofseti"
 * modeliyle ugrasmak zorunda kalmiyoruz.
 *
 * ## Neden ayri bir dosyada
 *
 * Bu sinif bir sure `OkuyucuEkrani.kt` icinde `private` durdu ve test
 * edilemedi. Kullanicinin bildirdigi iki hata da (yakinlastirirken baska bir
 * sayfaya isinlanma, yatayda kaydiramama) tam olarak burada hesaplanan
 * sinirlardan cikti. Saf aritmetik oldugu icin JVM testiyle dogrulanabilir;
 * cihaza kurup parmakla denemek zorunda kalmamak icin disari alindi.
 *
 * ## Iki parcali yukseklik
 *
 * Yakinlastirinca **sayfa yukseklikleri** olcekle carpilir; **aralar ve kenar
 * bosluklari** carpilmaz. Bu yuzden toplam yukseklik tek bir sayi olarak degil
 * [icerikYuksekligi] + [sabitYukseklik] olarak tutulur. Ikisini ayirmadan
 * yapilan her hesap, ustteki her sayfa arasi kadar sapma birakir; asagidaki
 * sayfalarda bu birikip gorunur bir kaymaya donusur.
 */
internal class SayfaYerlesimBilgisi(
    val ustler: FloatArray,
    val yukseklikler: FloatArray,
    /** Yakinlastirmayla olceklenen kisim (sayfalarin toplam yuksekligi). */
    val icerikYuksekligi: Float,
    /** Olceklenmeyen kisim (aralar ve kenar boslugu). */
    val sabitYukseklik: Float,
    /** Iki sayfa arasi bosluk. */
    val bosluk: Float,
    /** Ust ve alt kenar boslugu. */
    val kenar: Float,
) {
    val sayfaSayisi: Int get() = ustler.size

    val toplamYukseklik: Float get() = icerikYuksekligi + sabitYukseklik

    /** Belge bu gorunume sigmiyorsa kaydirilabilecek azami mesafe. */
    fun azamiKaydirma(gorunumYuksekligi: Float): Float =
        (toplamYukseklik - gorunumYuksekligi).coerceAtLeast(0f)

    /**
     * Ekranda gorunen sayfalar; bir onceki ve bir sonraki de dahil.
     *
     * Komsulari da dondurmemizin sebebi kullaniciyi bos beyaz alanla
     * karsilastirmamak: parmak kaydirmaya baslamadan once komsu sayfanin
     * ucuz katmani hazir olsun.
     */
    fun gorunurAralik(kaydirmaY: Float, gorunumYuksekligi: Float): IntRange {
        if (ustler.isEmpty()) return IntRange.EMPTY
        val alt = kaydirmaY + gorunumYuksekligi
        var bas = 0
        while (bas < ustler.size - 1 && ustler[bas] + yukseklikler[bas] < kaydirmaY) bas++
        var son = bas
        while (son < ustler.size - 1 && ustler[son + 1] < alt) son++
        return (bas - 1).coerceAtLeast(0)..(son + 1).coerceAtMost(ustler.size - 1)
    }

    /** [icerikY] belge koordinatindaki sayfanin indeksi. */
    fun sayfaBul(icerikY: Float): Int {
        var i = 0
        while (i < ustler.size - 1 && ustler[i + 1] <= icerikY) i++
        return i
    }

    /**
     * [icerikY] belge koordinatinin, olcek [k] katina cikinca gelecegi yer.
     *
     * Konumun tamamini [k] ile carpmak yanlistir: o konumun icinde ustteki
     * sayfa aralari ve kenar boslugu da vardir, bunlar yakinlastirmayla
     * buyumez. Once o sabit payi cikariyoruz, olcekliyoruz, geri ekliyoruz.
     *
     * Yakinlastirma odaginin ekranda sabit kalmasi bu fonksiyonun dogrulugana
     * baglidir.
     */
    fun olcekliKonum(icerikY: Float, k: Float): Float {
        if (ustler.isEmpty()) return icerikY * k
        val sabitPay = kenar + bosluk * sayfaBul(icerikY)
        return (icerikY - sabitPay) * k + sabitPay
    }

    /**
     * Olcek [k] katina cikinca olusacak azami kaydirma mesafesi.
     *
     * Yerlesim yeniden hesaplanmadan once siniri bilmemiz gerekiyor; parmak
     * hareketinin ortasinda bir kompozisyon beklemek istemiyoruz.
     */
    fun olcekliAzamiKaydirma(k: Float, gorunumYuksekligi: Float): Float =
        (icerikYuksekligi * k + sabitYukseklik - gorunumYuksekligi).coerceAtLeast(0f)

    companion object {
        /**
         * @param oran sayfa indeksinden yukseklik/genislik oranini veren islev.
         *   Olculmemis sayfalar icin tahmin donebilir.
         */
        fun hesapla(
            sayfaSayisi: Int,
            sayfaGenisligi: Float,
            bosluk: Float,
            kenar: Float,
            oran: (Int) -> Float,
        ): SayfaYerlesimBilgisi {
            val adet = sayfaSayisi.coerceAtLeast(0)
            val ustler = FloatArray(adet)
            val yukseklikler = FloatArray(adet)
            var y = kenar
            var icerik = 0f
            for (i in 0 until adet) {
                val h = (sayfaGenisligi * oran(i)).coerceAtLeast(0f)
                ustler[i] = y
                yukseklikler[i] = h
                icerik += h
                y += h
                if (i < adet - 1) y += bosluk
            }
            val sabit = bosluk * (adet - 1).coerceAtLeast(0) + kenar * 2f
            return SayfaYerlesimBilgisi(ustler, yukseklikler, icerik, sabit, bosluk, kenar)
        }
    }
}
