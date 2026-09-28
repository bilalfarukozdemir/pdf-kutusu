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
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

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
 * Ürünler tüketilebilir INAPP satın alımlarıdır. Satın alma Play tarafından
 * onaylanınca önce talep hakkı cihazda saklanır, sonra satın alma tüketilir.
 * Böylece kullanıcı e-posta talebini daha sonra gönderebilir ve aynı hizmeti
 * yeniden satın alabilir.
 *
 * Billing internete kendi cikmaz; cihazdaki Play Store uygulamasiyla
 * IPC/AIDL uzerinden konusur (bkz. PRIVACY.md, CONTRIBUTING.md kural #1).
 */
class BagisYoneticisi(baglam: Context) : BagisKaynagi {

    companion object {
        val URUN_KIMLIKLERI = listOf(
            "priority_request_review",
            "priority_pr_review",
            "priority_review_bundle",
        )

        private const val TALEP_KAYITLARI = "destek_talep_kayitlari"
        private const val TALEP_ANAHTARI = "bekleyen_talepler"
    }

    private val uygulamaBaglami: Context = baglam.applicationContext
    private val tercihler = uygulamaBaglami.getSharedPreferences(TALEP_KAYITLARI, Context.MODE_PRIVATE)

    private val _durum = MutableStateFlow<BagisDurumu>(BagisDurumu.Baglaniyor)
    override val durum: StateFlow<BagisDurumu> = _durum.asStateFlow()

    /** Son sorgulanan urun detaylari - satin alma akisini baslatmak icin gerekli. */
    private var urunDetaylari: Map<String, ProductDetails> = emptyMap()
    private var baglantiSuruyor = false

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
        if (istemci.isReady) {
            urunleriSorgula()
            return
        }
        if (baglantiSuruyor) return
        baglantiSuruyor = true
        istemci.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                baglantiSuruyor = false
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    urunleriSorgula()
                } else {
                    _durum.value = mevcutDurum()
                }
            }

            override fun onBillingServiceDisconnected() {
                baglantiSuruyor = false
                // Bekleyen istekleri sakla; BillingClient baglantiyi yeniden dener.
                _durum.value = mevcutDurum()
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
                mevcutDurumaDon()
                return@queryProductDetailsAsync
            }
            urunDetaylari = queryProductDetailsResult.productDetailsList.associateBy { it.productId }
            sahiplikleriSorgula()
        }
    }

    /** Önceki sürümden kalan, tüketilmemiş satın alımları da güvenle talebe çevirir. */
    private fun sahiplikleriSorgula() {
        val parametreler = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        istemci.queryPurchasesAsync(parametreler) { sonuc, satinAlmalar ->
            if (sonuc.responseCode != BillingClient.BillingResponseCode.OK) {
                mevcutDurumaDon()
                return@queryPurchasesAsync
            }

            val hizmetSatinAlmalari = satinAlmalar.filter { satinAlma ->
                satinAlma.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    satinAlma.products.any { it in URUN_KIMLIKLERI }
            }
            if (hizmetSatinAlmalari.isEmpty()) {
                mevcutDurumaDon()
            } else {
                hizmetSatinAlmalari.forEach(::talebiKaydetVeSatinAlmayiTuket)
            }
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
            // Nakit gibi yöntemlerde PENDING satın alma, ürün hakkı vermez.
            mevcutDurumaDon()
            return
        }

        talebiKaydetVeSatinAlmayiTuket(satinAlma)
    }

    private fun talebiKaydetVeSatinAlmayiTuket(satinAlma: Purchase) {
        val urunId = satinAlma.products.firstOrNull { it in URUN_KIMLIKLERI }
        if (urunId == null) {
            mevcutDurumaDon()
            return
        }

        val talep = DestekTalebi(urunId, referansOlustur(satinAlma.purchaseToken))
        if (!talebiKaliciKaydet(talep)) {
            _durum.value = BagisDurumu.Hata("Talep bilgisi cihaza kaydedilemedi.")
            return
        }

        val tuketimParametreleri = ConsumeParams.newBuilder()
            .setPurchaseToken(satinAlma.purchaseToken)
            .build()
        istemci.consumeAsync(tuketimParametreleri) { sonuc, _ ->
            _durum.value = if (sonuc.responseCode == BillingClient.BillingResponseCode.OK) {
                mevcutDurum()
            } else {
                BagisDurumu.Hata(sonuc.debugMessage)
            }
        }
    }

    override fun mevcutDurumaDon() {
        _durum.value = mevcutDurum()
    }

    fun talebiTamamla(referans: String) {
        val kalanlar = talepleriOku()
            .filterNot { it.referans == referans }
            .map(::talepKaydi)
            .toMutableSet()
        if (tercihler.edit().putStringSet(TALEP_ANAHTARI, kalanlar).commit()) {
            _durum.value = mevcutDurum()
        } else {
            _durum.value = BagisDurumu.Hata("Talep kaydı cihazdan silinemedi. Tekrar deneyin.")
        }
    }

    private fun mevcutDurum(): BagisDurumu =
        secenekListesindenDurum(mevcutSecenekleriTopla(), talepleriOku())

    private fun talepleriOku(): List<DestekTalebi> =
        tercihler.getStringSet(TALEP_ANAHTARI, emptySet()).orEmpty().mapNotNull { kayit ->
            val parcalar = kayit.split(':', limit = 2)
            if (parcalar.size != 2 || parcalar[0] !in URUN_KIMLIKLERI) return@mapNotNull null
            DestekTalebi(parcalar[0], parcalar[1])
        }

    private fun talebiKaliciKaydet(talep: DestekTalebi): Boolean {
        val kayitlar = tercihler.getStringSet(TALEP_ANAHTARI, emptySet()).orEmpty().toMutableSet()
        kayitlar += talepKaydi(talep)
        return tercihler.edit().putStringSet(TALEP_ANAHTARI, kayitlar).commit()
    }

    private fun talepKaydi(talep: DestekTalebi): String = "${talep.urunId}:${talep.referans}"

    /** Satın alma jetonunu e-postaya koymadan, aynı satın alma için kararlı bir kısa kod üretir. */
    private fun referansOlustur(satinAlmaJetonu: String): String {
        val ozet = MessageDigest.getInstance("SHA-256").digest(satinAlmaJetonu.toByteArray())
        return ozet.take(6).joinToString("") { "%02X".format(it.toInt() and 0xFF) }
    }

    private fun mevcutSecenekleriTopla(): List<BagisSecenegi> =
        URUN_KIMLIKLERI.mapNotNull { kimlik ->
            val detay = urunDetaylari[kimlik] ?: return@mapNotNull null
            val teklif = detay.oneTimePurchaseOfferDetails ?: return@mapNotNull null
            BagisSecenegi(urunId = kimlik, fiyatMetni = teklif.formattedPrice)
        }
}
