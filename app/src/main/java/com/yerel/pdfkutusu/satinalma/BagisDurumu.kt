package com.yerel.pdfkutusu.satinalma

/**
 * Geliştirici desteği satın alma akışının görünür durumları.
 *
 * [BagisYoneticisi] bu tipi bir `StateFlow` olarak disa acar; UI katmani
 * (`BagisBolumu`) yalnizca bu durumlari dinler, BillingClient'i hic gormez.
 */
sealed interface BagisDurumu {

    /** Magazaya baglaniliyor / urun sorgusu suruyor. */
    data object Baglaniyor : BagisDurumu

    /** Play bağlantısı kurulamadı veya kullanılabilir destek ürünü bulunamadı. */
    data object MagazaYok : BagisDurumu

    /** Sorgulanan urunler hazir; kullanici secim yapabilir. */
    data class Hazir(
        val secenekler: List<BagisSecenegi>,
        val bekleyenTalepler: List<DestekTalebi> = emptyList(),
    ) : BagisDurumu

    /** Play'in kendi satin alma ekrani acik / islem suruyor. */
    data object SatinAliniyor : BagisDurumu

    /** Satin alma basarisiz oldu (kullanici iptali disinda bir hata). */
    data class Hata(val mesaj: String) : BagisDurumu
}

/** Play Billing ile alınmış ve henüz e-posta talebi gönderilmemiş hizmet hakkı. */
data class DestekTalebi(
    val urunId: String,
    val referans: String,
)

/** Tek bir destek hizmeti: Play Console'daki INAPP ürün ID'si + yerel fiyat metni. */
data class BagisSecenegi(
    val urunId: String,
    val fiyatMetni: String,
)

/**
 * Sorgulanmis secenek listesinden gosterilecek durumu turetir.
 *
 * Saf fonksiyon: Context'e, BillingClient'a ya da baska bir yan etkiye
 * bagimli degil - hem ilk urun sorgusu sonucunda hem de bir satin alma
 * akisi bittikten sonra "onceki listeye don" icin kullanilir, bu yuzden
 * ayri bir yerde tanimli ve dogrudan birim testle dogrulanir.
 */
fun secenekListesindenDurum(
    secenekler: List<BagisSecenegi>,
    bekleyenTalepler: List<DestekTalebi> = emptyList(),
): BagisDurumu =
    if (secenekler.isEmpty() && bekleyenTalepler.isEmpty()) {
        BagisDurumu.MagazaYok
    } else {
        BagisDurumu.Hazir(secenekler, bekleyenTalepler)
    }
