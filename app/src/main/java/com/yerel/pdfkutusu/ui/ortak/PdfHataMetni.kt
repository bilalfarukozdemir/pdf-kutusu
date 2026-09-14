package com.yerel.pdfkutusu.ui.ortak

import android.content.Context
import com.yerel.pdfkutusu.R
import com.yerel.pdfkutusu.cekirdek.PdfHataKimligi
import com.yerel.pdfkutusu.cekirdek.PdfHatasi

/**
 * [PdfHatasi]'nin ekranda gosterilecek asil mesajini uretir.
 *
 * `cekirdek/` saf Kotlin oldugu icin Context'e erisemeyen bazi hatalar
 * (parola, iptal, beklenmeyen istisna, [SayfaAraligi] hatalari) hazir metin
 * yerine [PdfHatasi.kimlik] tasir. Bu fonksiyon kimlik doluysa onu R.string
 * karsiligina cevirir; bos ise [PdfHatasi.kullaniciMesaji] zaten cagiran
 * tarafindan (Context'i olan bir katmanda) yerellestirilmis demektir ve
 * oldugu gibi kullanilir.
 */
fun PdfHatasi.kullaniciMesajiUret(baglam: Context): String =
    kimlik?.let { baglam.pdfHataKimligiMetni(it) } ?: kullaniciMesaji

/**
 * [PdfHatasi.oneri]'nin ekranda gosterilecek karsiligi.
 *
 * Mesajin aksine oneri hata TURUNE baglidir: her alt sinif sabit bir oneri
 * tasir ve bu metin `cekirdek/` icinde Turkce yazilidir. Bu yuzden oneri
 * [PdfHatasi.kimlik]'e bakilmadan, her zaman turun R.string karsiligindan
 * uretilir; ham [PdfHatasi.oneri] ekrana cikarsa Ingilizce arayuzde Turkce kalir.
 */
fun PdfHatasi.oneriUret(baglam: Context): String? = when (this) {
    is PdfHatasi.ParolaGerekli -> baglam.getString(R.string.cekirdek_hata_parola_gerekli_oneri)
    is PdfHatasi.ParolaYanlis -> baglam.getString(R.string.cekirdek_hata_parola_yanlis_oneri)
    is PdfHatasi.GecersizAralik -> baglam.getString(R.string.cekirdek_hata_aralik_oneri)
    is PdfHatasi.BozukBelge -> baglam.getString(R.string.cekirdek_hata_bozuk_belge_oneri)
    is PdfHatasi.DosyaOkunamadi -> baglam.getString(R.string.cekirdek_hata_dosya_okunamadi_oneri)
    is PdfHatasi.Beklenmeyen -> baglam.getString(R.string.cekirdek_hata_beklenmeyen_oneri)
    // Bu iki hatanin onerisi yok.
    is PdfHatasi.GirdiYok, is PdfHatasi.Iptal -> null
}

private fun Context.pdfHataKimligiMetni(kimlik: PdfHataKimligi): String = when (kimlik) {
    is PdfHataKimligi.ParolaGerekli -> getString(R.string.cekirdek_hata_parola_gerekli_mesaj)
    is PdfHataKimligi.ParolaYanlis -> getString(R.string.cekirdek_hata_parola_yanlis_mesaj)
    is PdfHataKimligi.Iptal -> getString(R.string.cekirdek_hata_iptal_mesaj)
    is PdfHataKimligi.Beklenmeyen -> getString(R.string.cekirdek_hata_beklenmeyen_mesaj, kimlik.teknikNeden)
    is PdfHataKimligi.GirdiSecilmedi -> getString(R.string.cekirdek_hata_girdi_yok_mesaj)
    is PdfHataKimligi.AralikNedeni -> aralikNedeniMetni(kimlik)
}

private fun Context.aralikNedeniMetni(kimlik: PdfHataKimligi.AralikNedeni): String = when (kimlik) {
    is PdfHataKimligi.AralikNedeni.BelgedeSayfaYok ->
        getString(R.string.cekirdek_hata_aralik_belgede_sayfa_yok)
    is PdfHataKimligi.AralikNedeni.Bos ->
        getString(R.string.cekirdek_hata_aralik_bos)
    is PdfHataKimligi.AralikNedeni.BirdenFazlaTire ->
        getString(R.string.cekirdek_hata_aralik_birden_fazla_tire, kimlik.parca)
    is PdfHataKimligi.AralikNedeni.EksikSinir ->
        getString(R.string.cekirdek_hata_aralik_eksik_sinir, kimlik.parca)
    is PdfHataKimligi.AralikNedeni.BasSondanBuyuk ->
        getString(R.string.cekirdek_hata_aralik_bas_sondan_buyuk, kimlik.parca, kimlik.bas, kimlik.son)
    is PdfHataKimligi.AralikNedeni.HicbiriSecilmedi ->
        getString(R.string.cekirdek_hata_aralik_hicbiri_secilmedi)
    is PdfHataKimligi.AralikNedeni.SayiDegil ->
        getString(R.string.cekirdek_hata_aralik_sayi_degil, kimlik.parca)
    is PdfHataKimligi.AralikNedeni.SayiCokBuyuk ->
        getString(R.string.cekirdek_hata_aralik_sayi_cok_buyuk, kimlik.parca)
    is PdfHataKimligi.AralikNedeni.SinirDisi ->
        getString(
            R.string.cekirdek_hata_aralik_sinir_disi,
            kimlik.parca,
            kimlik.toplamSayfa,
            kimlik.istenenSayfa,
        )
}
