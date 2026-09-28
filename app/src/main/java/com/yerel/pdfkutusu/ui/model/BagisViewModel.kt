package com.yerel.pdfkutusu.ui.model

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yerel.pdfkutusu.Bagimliliklar
import com.yerel.pdfkutusu.satinalma.BagisDurumu
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * `HakkindaEkrani`'ndaki bagis bolumunu besler.
 *
 * Ekran her acildiginda [Bagimliliklar.bagisYoneticisi] ile magazaya
 * baglanir. Yonetici uygulama omru boyunca yasayan (lazy) tek bir
 * BillingClient tutar; bu ViewModel yalnizca durumu dinler ve kullanici
 * eylemlerini iletir.
 */
class BagisViewModel(private val bagimliliklar: Bagimliliklar) : ViewModel() {

    val durum: StateFlow<BagisDurumu> = bagimliliklar.bagisYoneticisi.durum
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BagisDurumu.Baglaniyor)

    init {
        bagimliliklar.bagisYoneticisi.baglan()
    }

    fun satinAl(activity: Activity, urunId: String) {
        bagimliliklar.bagisYoneticisi.satinAlmayiBaslat(activity, urunId)
    }

    fun talebiTamamla(referans: String) {
        bagimliliklar.bagisYoneticisi.talebiTamamla(referans)
    }

    /** Magaza baglantisi veya bagis katalogu gecici olarak bos geldiyse tekrar sorgular. */
    fun tekrarDene() {
        bagimliliklar.bagisYoneticisi.baglan()
    }

    /** Tesekkur/hata mesaji kapatildiginda secenek listesine geri doner. */
    fun mesajiKapat() {
        bagimliliklar.bagisYoneticisi.mevcutDurumaDon()
    }
}
