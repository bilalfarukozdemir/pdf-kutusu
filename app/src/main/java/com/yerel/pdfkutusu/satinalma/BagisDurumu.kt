package com.yerel.pdfkutusu.satinalma

/**
 * Bagis (destek) akisinin gorunur durumlari.
 *
 * [BagisYoneticisi] bu tipi bir `StateFlow` olarak disa acar; UI katmani
 * (`BagisBolumu`) yalnizca bu durumlari dinler, BillingClient'i hic gormez.
 */
sealed interface BagisDurumu {

    /** Magazaya baglaniliyor / urun sorgusu suruyor. */
    data object Baglaniyor : BagisDurumu

    /** Cihazda Play Store yok ya da magaza kullanilamaz durumda. */
    data object MagazaYok : BagisDurumu

    /** Sorgulanan urunler hazir; kullanici secim yapabilir. */
    data class Hazir(val secenekler: List<BagisSecenegi>) : BagisDurumu

    /** Play'in kendi satin alma ekrani acik / islem suruyor. */
    data object SatinAliniyor : BagisDurumu

    /** Satin alma basariyla tamamlandi ve tuketildi (tekrar bagis yapilabilir). */
    data object Tesekkur : BagisDurumu

    /** Satin alma basarisiz oldu (kullanici iptali disinda bir hata). */
    data class Hata(val mesaj: String) : BagisDurumu
}

/** Tek bir bagis secenegi: Play Console'daki INAPP urun ID'si + yerel fiyat metni. */
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
fun secenekListesindenDurum(secenekler: List<BagisSecenegi>): BagisDurumu =
    if (secenekler.isEmpty()) BagisDurumu.MagazaYok else BagisDurumu.Hazir(secenekler)
