package com.yerel.pdfkutusu.satinalma

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [BagisYoneticisi] ile UI arasindaki sozlesme.
 *
 * BillingClient'in kendisi (ve dondurdugu `ProductDetails`/`Purchase`
 * nesneleri) Google Play kutuphanesinin paket-ici constructor'lariyla
 * uretilir; uygulama kodu bunlari ornekleyemez, dolayisiyla dogrudan
 * "sahte BillingClient" yazmak mumkun degil. Bunun yerine sinir bu
 * arayuzde cizilir: gercek yonetici BillingClient'i sarar,
 * `BagisAkisiCihazTesti` ise bu arayuzu sahte (fake) bir siniftaki saf
 * [BagisDurumu] degerleriyle uygular.
 */
interface BagisKaynagi {
    val durum: StateFlow<BagisDurumu>
    fun baglan()
    fun satinAlmayiBaslat(activity: Activity, urunId: String)
    fun mevcutDurumaDon()
}

/**
 * Google Play Billing sarmalayicisi.
 *
 * Uc urun (bkz. [URUN_KIMLIKLERI]) tuketilebilir (consumable) INAPP urunu
 * olarak tanimlanir: satin alma tamamlaninca hemen [ConsumeParams] ile
 * tuketilir, boylece kullanici istedigi kadar tekrar bagis yapabilir.
 *
 * Billing internete kendi cikmaz; cihazdaki Play Store uygulamasiyla
 * IPC/AIDL uzerinden konusur (bkz. PRIVACY.md, CONTRIBUTING.md kural #1).
 */
class BagisYoneticisi(baglam: Context) : BagisKaynagi {

    companion object {
        val URUN_KIMLIKLERI = listOf("destek_kahve", "destek_ogun", "destek_comert")
    }

    private val uygulamaBaglami: Context = baglam.applicationContext

    private val _durum = MutableStateFlow<BagisDurumu>(BagisDurumu.Baglaniyor)
    override val durum: StateFlow<BagisDurumu> = _durum.asStateFlow()

    /** Son sorgulanan urun detaylari - satin alma akisini baslatmak icin gerekli. */
    private var urunDetaylari: Map<String, ProductDetails> = emptyMap()

    private val satinAlmaDinleyicisi = PurchasesUpdatedListener { sonuc, satinAlmalar ->
        when (sonuc.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val liste = satinAlmalar
                if (liste.isNullOrEmpty()) {
                    mevcutDurumaDon()
                } else {
                    liste.forEach { islemiTamamla(it) }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> mevcutDurumaDon()
            else -> _durum.value = BagisDurumu.Hata(sonuc.debugMessage)
        }
    }

    private val istemci: BillingClient = BillingClient.newBuilder(uygulamaBaglami)
        .setListener(satinAlmaDinleyicisi)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .build()

    override fun baglan() {
        _durum.value = BagisDurumu.Baglaniyor
        istemci.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    urunleriSorgula()
                } else {
                    _durum.value = BagisDurumu.MagazaYok
                }
            }

            override fun onBillingServiceDisconnected() {
                // Kutuphane baglantiyi otomatik olarak yeniden dener; burada
                // ek bir eylem gerekmiyor (bkz. BillingClientStateListener dokumani).
            }
        })
    }

    private fun urunleriSorgula() {
        val urunListesi = URUN_KIMLIKLERI.map { kimlik ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(kimlik)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val parametreler = QueryProductDetailsParams.newBuilder()
            .setProductList(urunListesi)
            .build()

        istemci.queryProductDetailsAsync(parametreler) { sonuc, queryProductDetailsResult ->
            if (sonuc.responseCode != BillingClient.BillingResponseCode.OK) {
                _durum.value = BagisDurumu.MagazaYok
                return@queryProductDetailsAsync
            }
            urunDetaylari = queryProductDetailsResult.productDetailsList.associateBy { it.productId }
            _durum.value = secenekListesindenDurum(mevcutSecenekleriTopla())
        }
    }

    override fun satinAlmayiBaslat(activity: Activity, urunId: String) {
        val detay = urunDetaylari[urunId] ?: return
        val teklifJetonu = detay.oneTimePurchaseOfferDetailsList
            ?.firstOrNull()
            ?.offerToken
            ?: run {
                _durum.value = BagisDurumu.Hata(
                    "Urun icin fiyat teklifi bulunamadi: $urunId",
                )
                return
            }

        val parametreListesi = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(detay)
                .setOfferToken(teklifJetonu)
                .build(),
        )
        val akisParametreleri = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(parametreListesi)
            .build()

        _durum.value = BagisDurumu.SatinAliniyor
        istemci.launchBillingFlow(activity, akisParametreleri)
    }

    private fun islemiTamamla(satinAlma: Purchase) {
        if (satinAlma.purchaseState != Purchase.PurchaseState.PURCHASED) {
            // PENDING durumu (ornegin nakit odeme): kullaniciya "beklemede"
            // ayri bir durum eklemek yerine mevcut listeye geri donuyoruz -
            // tuketilebilir bagislarda bekleyen islem cok nadir bir yoldur.
            mevcutDurumaDon()
            return
        }
        val tuketimParametreleri = ConsumeParams.newBuilder()
            .setPurchaseToken(satinAlma.purchaseToken)
            .build()
        istemci.consumeAsync(tuketimParametreleri) { sonuc, _ ->
            _durum.value = if (sonuc.responseCode == BillingClient.BillingResponseCode.OK) {
                BagisDurumu.Tesekkur
            } else {
                BagisDurumu.Hata(sonuc.debugMessage)
            }
        }
    }

    override fun mevcutDurumaDon() {
        _durum.value = secenekListesindenDurum(mevcutSecenekleriTopla())
    }

    private fun mevcutSecenekleriTopla(): List<BagisSecenegi> =
        URUN_KIMLIKLERI.mapNotNull { kimlik ->
            val detay = urunDetaylari[kimlik] ?: return@mapNotNull null
            val teklif = detay.oneTimePurchaseOfferDetails ?: return@mapNotNull null
            BagisSecenegi(urunId = kimlik, fiyatMetni = teklif.formattedPrice)
        }
}
