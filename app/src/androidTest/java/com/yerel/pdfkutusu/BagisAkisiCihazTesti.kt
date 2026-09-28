package com.yerel.pdfkutusu

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yerel.pdfkutusu.satinalma.BagisDurumu
import com.yerel.pdfkutusu.satinalma.BagisSecenegi
import com.yerel.pdfkutusu.satinalma.BagisYoneticisi
import com.yerel.pdfkutusu.satinalma.DestekTalebi
import com.yerel.pdfkutusu.ui.ekran.BagisIcerik
import com.yerel.pdfkutusu.ui.tema.PdfKutusuTemasi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `BagisIcerik` composable'inin durum bazli UI testi.
 *
 * Gercek `BillingClient`, `ProductDetails` ve `Purchase` Google Play
 * kutuphanesinin paket-ici constructor'lariyla uretilir; uygulama kodu
 * bunlari sahte (fake) olarak ornekleyemez. Bu yuzden sahtelik siniri
 * [BagisIcerik]'in aldigi [BagisDurumu] degerinde cizilir: bu test hicbir
 * zaman gercek magazaya baglanmaz, yalnizca elle uretilmis durumlari
 * composable'a verip dogru metin/butonlarin gorundugunu ve tiklamalarin
 * dogru geri cagirmayi tetikledigini dogrular.
 *
 * Model "bagis" (karsilikli kahve) degil "oncelikli inceleme hizmeti":
 * urun kimlikleri [BagisYoneticisi.URUN_KIMLIKLERI]'nden gelir, satin alma
 * sonrasi `Hata` durumu degil **bekleyen talep** uretilir; kullanici e-postayi
 * kendisi gonderir, talep `DestekTalebi` olarak cihazda saklanir. Onceki
 * `Tesekkur` durumu bu modelde yok; onun "kullanici mesaji gorup kapatsin"
 * isi artik `Hata` durumundaki `Tamam` butonuyla ve bekleyen talep blogundaki
 * `E-postayi gonderdim` butonuyla yurutulur - ikisi de asagida test ediliyor.
 */
@RunWith(AndroidJUnit4::class)
class BagisAkisiCihazTesti {

    @get:Rule
    val kural = createComposeRule()

    private val baglam get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun metin(anahtar: Int, vararg argumanlar: Any): String =
        if (argumanlar.isEmpty()) {
            baglam.getString(anahtar)
        } else {
            baglam.getString(anahtar, *argumanlar)
        }

    /**
     * Urun kimligi -> yerel ad etiketi.
     *
     * Bu harita [BagisYoneticisi.URUN_KIMLIKLERI] ile **birebir ayni olmak
     * zorunda**. `HakkindaEkrani.bagisEtiketi` bilinmeyen kimliklerde
     * `else -> urunId` dalina dustugu icin, burada eksik bir gecer, ekranda
     * kiranmadan yalnizca "priority_xyz" gostermesi demektir: sessiz bir
     * hatadir ve ancak bu test yakalar.
     */
    private val yerelAdlar = mapOf(
        "priority_request_review" to R.string.hakkinda_bagis_istek_incelemesi,
        "priority_pr_review" to R.string.hakkinda_bagis_pr_incelemesi,
        "priority_review_bundle" to R.string.hakkinda_bagis_inceleme_paketi,
    )

    private fun hizmetSecenekleri(): List<BagisSecenegi> = listOf(
        BagisSecenegi(urunId = "priority_request_review", fiyatMetni = "₺49,99"),
        BagisSecenegi(urunId = "priority_pr_review", fiyatMetni = "₺149,99"),
        BagisSecenegi(urunId = "priority_review_bundle", fiyatMetni = "₺249,99"),
    )

    /** Buton etiketini uretimdeki `bagisEtiketi` ile ayni bicimde kurar. */
    private fun etiket(urunId: String, fiyat: String): String =
        metin(R.string.hakkinda_bagis_secenek_etiketi, metin(yerelAdlar.getValue(urunId)), fiyat)

    // ------------------------------------------------------------ durumlar

    @Test
    fun baglaniyorDurumuBekleFaseArac() {
        kural.setContent {
            PdfKutusuTemasi {
                BagisIcerik(durum = BagisDurumu.Baglaniyor, satinAl = {}, mesajiKapat = {})
            }
        }

        kural.onNodeWithText(metin(R.string.hakkinda_bagis_baglaniyor)).assertExists()
    }

    @Test
    fun satinAliniyorDurumuBekleFaseAraciGosterir() {
        kural.setContent {
            PdfKutusuTemasi {
                BagisIcerik(durum = BagisDurumu.SatinAliniyor, satinAl = {}, mesajiKapat = {})
            }
        }

        kural.onNodeWithText(metin(R.string.hakkinda_bagis_satin_aliniyor)).assertExists()
    }

    @Test
    fun magazaYokDurumuBilgiMesajiniVeTekrarDeneButonunuGosterir() {
        var tekrarSayisi = 0

        kural.setContent {
            PdfKutusuTemasi {
                BagisIcerik(
                    durum = BagisDurumu.MagazaYok,
                    satinAl = {},
                    mesajiKapat = {},
                    tekrarDene = { tekrarSayisi++ },
                )
            }
        }

        kural.onNodeWithText(metin(R.string.hakkinda_bagis_magaza_yok)).assertExists()
        kural.onNodeWithText(metin(R.string.hakkinda_bagis_tekrar_dene)).performClick()

        assert(tekrarSayisi == 1) { "tekrarDene $tekrarSayisi kez cagrildi, 1 bekleniyordu" }
    }

    @Test
    fun hataDurumuMesajiGosterirVeTamamButonuCalisir() {
        var kapatildiMi = false
        val hataMesaji = "Test hatasi"

        kural.setContent {
            PdfKutusuTemasi {
                BagisIcerik(
                    durum = BagisDurumu.Hata(hataMesaji),
                    satinAl = {},
                    mesajiKapat = { kapatildiMi = true },
                )
            }
        }

        val beklenenMetin = metin(R.string.hakkinda_bagis_hata, hataMesaji)
        kural.onNodeWithText(beklenenMetin).assertExists()

        kural.onNodeWithText(metin(R.string.hakkinda_bagis_tamam)).performClick()
        assert(kapatildiMi) { "mesajiKapat cagrilmadi" }
    }

    // ---------------------------------------------------------- hazir/urunler

    @Test
    fun hazirDurumdaSecenekTiklamasiUrunIdIleGeriCagrilir() {
        var tiklananUrunId: String? = null
        val secenekler = hizmetSecenekleri()

        kural.setContent {
            PdfKutusuTemasi {
                BagisIcerik(
                    durum = BagisDurumu.Hazir(secenekler),
                    satinAl = { urunId -> tiklananUrunId = urunId },
                    mesajiKapat = {},
                )
            }
        }

        kural.onNodeWithText(etiket("priority_request_review", "₺49,99"))
            .assertExists()
            .performClick()

        assert(tiklananUrunId == "priority_request_review") {
            "Beklenen priority_request_review, gelen: $tiklananUrunId"
        }
    }

    /**
     * Her hizmet kimligi icin yerel ad etiketi tanimli mi?
     *
     * Satis ekrani `URUN_KIMLIKLERI`'ni koda gore degil, ucu da elle yazilmis
     * bir `when` ile etiketlendirir. Ikisi ayrilirsa yeni bir hizmet eklenir
     * ekken kullanici ham urun kimligini gorur ve **hicbir sey kirmaz**.
     * Asagidaki esitlik kontrolu, ekleme unutuldugu anda kirmizi verir.
     */
    @Test
    fun herHizmetKimligininYerelAdiTanimli() {
        assert(BagisYoneticisi.URUN_KIMLIKLERI.toSet() == yerelAdlar.keys) {
            "Kimlikler ayrismis. BagisYoneticisi.URUN_KIMLIKLERI = " +
                "${BagisYoneticisi.URUN_KIMLIKLERI}, bu testin etiket haritasi = " +
                "${yerelAdlar.keys}. Yeni bir hizmet eklerken HakkindaEkrani " +
                "icindeki bagisEtiketi() when'ini de guncelle."
        }
    }

    @Test
    fun tumHizmetSecenekleriYerelAdVeFiyatGosterir() {
        val secenekler = hizmetSecenekleri()

        kural.setContent {
            PdfKutusuTemasi {
                BagisIcerik(durum = BagisDurumu.Hazir(secenekler), satinAl = {}, mesajiKapat = {})
            }
        }

        secenekler.forEach { secenek ->
            kural.onNodeWithText(etiket(secenek.urunId, secenek.fiyatMetni)).assertExists()
        }
    }

    /**
     * Bilinmeyen bir kimlik ham kimligi gosterilir - bu, bir ustteki sessiz
     * hatanin kaynagidir. Test burada o dali kasitli olarak belgeler: duzeltme
     * "bu davranis kaldirilsin" degil, "bu davranis bilinmeyen kimlikte de
     * olur, o yuzden kimlik-etiket haritasi senkron tutulmalidir" olmali.
     */
    @Test
    fun bilinmeyenUrunKimligiHamKimligiGosterir() {
        val bilinmeyen = "gelecekteki_hizmet"

        kural.setContent {
            PdfKutusuTemasi {
                BagisIcerik(
                    durum = BagisDurumu.Hazir(
                        listOf(BagisSecenegi(bilinmeyen, "₺1,00")),
                    ),
                    satinAl = {},
                    mesajiKapat = {},
                )
            }
        }

        kural.onNodeWithText(
            metin(R.string.hakkinda_bagis_secenek_etiketi, bilinmeyen, "₺1,00"),
        ).assertExists()
    }

    // ------------------------------------------------- bekleyen destek talebi

    /**
     * Satin alinan hizmet `Tesekkur` ekrani degil, **bekleyen talep** olarak
     * gelir: kullanici e-postayi kendisi gonderir, "gonderdim" dediginde talep
     * `talebiTamamla(referans)` ile listeden dusulur.
     *
     * Eski `Tesekkur` testinin baktigi sey ("kullanici mesaji gorup kapatsin")
     * budur; `Hata` durumu testi zaten `mesajiKapat` yolunu kapsiyor.
     */
    @Test
    fun bekleyenTalepKoduGosterilirVeIkiButonuDogrulBaglar() {
        val talep = DestekTalebi(urunId = "priority_pr_review", referans = "A1B2C3D4E5F6")
        var acilanTalep: DestekTalebi? = null
        var tamamlananReferans: String? = null

        kural.setContent {
            PdfKutusuTemasi {
                BagisIcerik(
                    durum = BagisDurumu.Hazir(hizmetSecenekleri(), listOf(talep)),
                    satinAl = {},
                    mesajiKapat = {},
                    talepEpostasiAc = { acilanTalep = it },
                    talebiTamamla = { tamamlananReferans = it },
                )
            }
        }

        kural.onNodeWithText(metin(R.string.hakkinda_bagis_talep_baslik)).assertExists()
        kural.onNodeWithText(metin(R.string.hakkinda_bagis_talep_referans, talep.referans))
            .assertExists()

        kural.onNodeWithText(metin(R.string.hakkinda_bagis_eposta_ac)).performClick()
        kural.onNodeWithText(metin(R.string.hakkinda_bagis_talep_gonderildi)).performClick()

        assert(acilanTalep == talep) { "Beklenen $talep, gelen: $acilanTalep" }
        assert(tamamlananReferans == talep.referans) {
            "Beklenen referans ${talep.referans}, gelen: $tamamlananReferans"
        }
    }

    /**
     * Sorgu bos donse ama bekleyen talep varsa ekran cokmez: kullanici elinde
     * olan hizmeti tamamlayabilir, ayrica "tekrar dene" ile katalog yeniden
     * istenir. Bu, "magaza yok" ile "katalog bos" arasindaki ayrimdir.
     */
    @Test
    fun secenekYokkenBekleyenTalepGosterilirVeTekrarDenenir() {
        val talep = DestekTalebi(urunId = "priority_request_review", referans = "0A0B0C0D0E0F")
        var tekrarSayisi = 0

        kural.setContent {
            PdfKutusuTemasi {
                BagisIcerik(
                    durum = BagisDurumu.Hazir(emptyList(), listOf(talep)),
                    satinAl = {},
                    mesajiKapat = {},
                    tekrarDene = { tekrarSayisi++ },
                )
            }
        }

        kural.onNodeWithText(metin(R.string.hakkinda_bagis_talep_referans, talep.referans))
            .assertExists()
        kural.onNodeWithText(metin(R.string.hakkinda_bagis_urunler_yuklenemedi)).assertExists()

        kural.onNodeWithText(metin(R.string.hakkinda_bagis_tekrar_dene)).performClick()
        assert(tekrarSayisi == 1) { "tekrarDene $tekrarSayisi kez cagrildi, 1 bekleniyordu" }
    }
}
