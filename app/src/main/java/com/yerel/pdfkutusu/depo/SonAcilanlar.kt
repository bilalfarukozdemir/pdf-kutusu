package com.yerel.pdfkutusu.depo

import android.content.Context

/** Okuyucuda acilmis bir belgenin kaydi. */
data class SonAcilanBelge(
    /** `content://…` ya da uygulamanin kendi ciktilari icin `file://…`. */
    val uri: String,
    val ad: String,
    val zaman: Long,
    /**
     * Belgeye sonradan da erisilebilir mi?
     *
     * Uygulama icinden secilen belgelerde kalici yetki alinabildigi icin
     * `true`. Baska bir uygulamadan (`ACTION_VIEW`) gelen belgede yetki
     * gecicidir - kullanici listeye ertesi gun dokunursa acilmaz, o yuzden
     * bunu bilerek isaretliyoruz ve arayuzde soyluyoruz.
     */
    val kalici: Boolean,
)

/**
 * Kayitlarin saklandigi yer.
 *
 * Ayri bir arayuz olmasinin tek sebebi test: siralama, kapasite ve
 * ayristirma mantigi Android'e hic dokunmadan sinanabiliyor.
 */
interface KayitDeposu {
    fun oku(): Set<String>
    fun yaz(kayitlar: Set<String>)
}

/**
 * Son acilan belgeler listesi.
 *
 * ## Islem gunlugunden neden ayri
 *
 * Islem gunlugu bilerek degistirilemez (DAO'sunda UPDATE/DELETE yok): ne
 * yaptigini sonradan dogrulayabilmek icin. Burasi tam tersi: kullanicinin
 * hangi belgeleri actigi bilgisi mahremiyet alanina girer, tek tek ve topluca
 * silinebilmeli. Bu yuzden ayri bir depo, ayri kurallar.
 *
 * Liste cihazdan disari cikmaz - uygulamanin internet izni zaten yok - ama
 * telefonu birine uzattiginda son okuduklarinin adlari gorunur. Silme bu
 * yuzden var.
 */
class SonAcilanlar(private val depo: KayitDeposu) {

    fun listele(): List<SonAcilanBelge> =
        depo.oku().mapNotNull(::coz).sortedByDescending { it.zaman }

    /**
     * Belgeyi listenin basina koyar.
     *
     * @return kapasite asildigi icin listeden dusen kayitlar. Cagiran taraf
     *   bunlarin kalici URI yetkisini birakmali; yoksa sistemin paket basina
     *   tuttugu yetki kotasi zamanla dolar.
     */
    fun ekle(belge: SonAcilanBelge): List<SonAcilanBelge> {
        val temiz = belge.copy(ad = belge.ad.replace(AYIRAC, ' '))
        // Ayni belge yeniden acildiysa kopya birakmiyoruz, basa aliyoruz.
        val digerleri = listele().filterNot { it.uri == temiz.uri }
        val tumu = (listOf(temiz) + digerleri).sortedByDescending { it.zaman }
        val kalanlar = tumu.take(AZAMI)
        depo.yaz(kalanlar.map(::yaz).toSet())
        return tumu.drop(AZAMI)
    }

    fun sil(uri: String) {
        depo.yaz(listele().filterNot { it.uri == uri }.map(::yaz).toSet())
    }

    /** @return silinen kayitlar; yetkileri birakilsin diye dondurulur. */
    fun temizle(): List<SonAcilanBelge> {
        val vardi = listele()
        depo.yaz(emptySet())
        return vardi
    }

    private fun yaz(belge: SonAcilanBelge): String =
        listOf(
            belge.zaman.toString(),
            if (belge.kalici) "1" else "0",
            belge.uri,
            belge.ad,
        ).joinToString(AYIRAC.toString())

    private fun coz(kayit: String): SonAcilanBelge? {
        val parcalar = kayit.split(AYIRAC, limit = 4)
        if (parcalar.size != 4) return null
        val zaman = parcalar[0].toLongOrNull() ?: return null
        if (parcalar[2].isBlank()) return null
        return SonAcilanBelge(
            uri = parcalar[2],
            ad = parcalar[3],
            zaman = zaman,
            kalici = parcalar[1] == "1",
        )
    }

    companion object {
        /**
         * Listede tutulan azami belge sayisi.
         *
         * Sistemin paket basina tuttugu kalici URI yetkisi sinirlidir; 20
         * kayitla o sinira yaklasmiyoruz bile. Ayrica 20'den uzun bir "son
         * acilanlar" listesi zaten okunmuyor.
         */
        const val AZAMI = 20

        /**
         * Alan ayraci: birim ayirici (U+001F).
         *
         * Yuzde kodlanmis bir URI'de ya da makul bir dosya adinda bulunmaz;
         * yine de gelen adlarda temizliyoruz.
         *
         * Kaynak dosyaya gercek kontrol karakteri yazmiyoruz - dosyayi metin
         * araclariyla aranamaz hale getiriyor - bu yuzden koddan uretiliyor.
         */
        private val AYIRAC: Char = 31.toChar()
    }
}

/** Kayitlari uygulamanin kendi tercih dosyasinda tutar. */
class TercihKayitDeposu(baglam: Context) : KayitDeposu {

    private val depo = baglam.applicationContext
        .getSharedPreferences("pdf_kutusu_son_acilanlar", Context.MODE_PRIVATE)

    override fun oku(): Set<String> = depo.getStringSet(ANAHTAR, null)?.toSet() ?: emptySet()

    override fun yaz(kayitlar: Set<String>) {
        // Her seferinde yeni bir kume yaziyoruz: SharedPreferences'in
        // dondurdugu kumeyi degistirip geri vermek sessizce kaybolur.
        depo.edit().putStringSet(ANAHTAR, HashSet(kayitlar)).apply()
    }

    private companion object {
        const val ANAHTAR = "son_acilanlar_v1"
    }
}
