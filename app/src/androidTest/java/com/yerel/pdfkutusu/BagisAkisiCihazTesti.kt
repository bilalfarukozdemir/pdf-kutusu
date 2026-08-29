package com.yerel.pdfkutusu

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yerel.pdfkutusu.satinalma.BagisDurumu
import com.yerel.pdfkutusu.satinalma.BagisSecenegi
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
 */
@RunWith(AndroidJUnit4::class)
class BagisAkisiCihazTesti {

    @get:Rule
    val kural = createComposeRule()

    private val baglam get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun metin(anahtar: Int): String = baglam.getString(anahtar)

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
    fun magazaYokDurumuBilgiMesajiniGosterir() {
        kural.setContent {
            PdfKutusuTemasi {
                BagisIcerik(durum = BagisDurumu.MagazaYok, satinAl = {}, mesajiKapat = {})
            }
        }

        kural.onNodeWithText(metin(R.string.hakkinda_bagis_magaza_yok)).assertExists()
    }

    @Test
    fun hazirDurumdaSecenekTiklamasiUrunIdIleGeriCagrilir() {
        var tiklananUrunId: String? = null
        val secenekler = listOf(
            BagisSecenegi(urunId = "destek_kahve", fiyatMetni = "₺39,99"),
            BagisSecenegi(urunId = "destek_ogun", fiyatMetni = "₺99,99"),
            BagisSecenegi(urunId = "destek_comert", fiyatMetni = "₺199,99"),
        )

        kural.setContent {
            PdfKutusuTemasi {
                BagisIcerik(
                    durum = BagisDurumu.Hazir(secenekler),
                    satinAl = { urunId -> tiklananUrunId = urunId },
                    mesajiKapat = {},
                )
            }
        }

        val kahveEtiketi = metin(R.string.hakkinda_bagis_secenek_etiketi)
            .replace("%1\$s", metin(R.string.hakkinda_bagis_kahve))
            .replace("%2\$s", "₺39,99")

        kural.onNodeWithText(kahveEtiketi).assertExists()
        kural.onNodeWithText(kahveEtiketi).performClick()

        assert(tiklananUrunId == "destek_kahve") {
            "Beklenen destek_kahve, gelen: $tiklananUrunId"
        }
    }

    @Test
    fun tesekkurDurumuTamamButonuMesajiKapatiCagirir() {
        var kapatildiMi = false

        kural.setContent {
            PdfKutusuTemasi {
                BagisIcerik(
                    durum = BagisDurumu.Tesekkur,
                    satinAl = {},
                    mesajiKapat = { kapatildiMi = true },
                )
            }
        }

        kural.onNodeWithText(metin(R.string.hakkinda_bagis_tesekkur)).assertExists()
        kural.onNodeWithText(metin(R.string.hakkinda_bagis_tamam)).performClick()

        assert(kapatildiMi) { "mesajiKapat cagrilmadi" }
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

        val beklenenMetin = metin(R.string.hakkinda_bagis_hata).replace("%1\$s", hataMesaji)
        kural.onNodeWithText(beklenenMetin).assertExists()

        kural.onNodeWithText(metin(R.string.hakkinda_bagis_tamam)).performClick()
        assert(kapatildiMi) { "mesajiKapat cagrilmadi" }
    }
}
