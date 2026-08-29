package com.yerel.pdfkutusu.cekirdek

/**
 * [PdfHatasi] icin Context'e ihtiyac duymadan tasinabilen, yerellestirilebilir
 * hata kimligi.
 *
 * `cekirdek/` saf Kotlin oldugu ve `android.content.Context`'e dokunamadigi
 * icin bazi hatalar (ozellikle [SayfaAraligi] ile parola/beklenmeyen hata
 * durumlari) hazir Turkce metin yerine bu kimligi tasir. Kimlige karsilik
 * gelen metin UI katmaninda uretilir (bkz. `ui/ortak/PdfHataMetni.kt`),
 * boylece ayni kimlik hem TR hem EN'de doğru cikti verir.
 *
 * [PdfHatasi.kimlik] null ise mesaj zaten cagiran tarafindan (Context'i olan
 * bir katmanda) yerellestirilip [PdfHatasi.kullaniciMesaji] icine konmustur;
 * bu durumda kimlige gerek yoktur.
 */
sealed class PdfHataKimligi {

    object ParolaGerekli : PdfHataKimligi()
    object ParolaYanlis : PdfHataKimligi()
    object Iptal : PdfHataKimligi()

    /** @param teknikNeden alttaki istisnanin mesaji; cevrilmez, oldugu gibi gosterilir. */
    data class Beklenmeyen(val teknikNeden: String) : PdfHataKimligi()

    object GirdiSecilmedi : PdfHataKimligi()

    /** [SayfaAraligi.ayristir] icinde uretilen, Context'siz aralik hatalari. */
    sealed class AralikNedeni : PdfHataKimligi() {
        object BelgedeSayfaYok : AralikNedeni()
        object Bos : AralikNedeni()
        data class BirdenFazlaTire(val parca: String) : AralikNedeni()
        data class EksikSinir(val parca: String) : AralikNedeni()
        data class BasSondanBuyuk(val parca: String, val bas: Int, val son: Int) : AralikNedeni()
        object HicbiriSecilmedi : AralikNedeni()
        data class SayiDegil(val parca: String) : AralikNedeni()
        data class SayiCokBuyuk(val parca: String) : AralikNedeni()
        data class SinirDisi(val parca: String, val toplamSayfa: Int, val istenenSayfa: Int) : AralikNedeni()
    }
}
