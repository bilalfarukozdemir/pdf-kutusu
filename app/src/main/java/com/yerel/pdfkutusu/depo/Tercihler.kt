package com.yerel.pdfkutusu.depo

import android.content.Context

/**
 * Kucuk yerel ayarlar. SharedPreferences yeterli; bir bagimlilik daha
 * eklemeye deger bir ihtiyac yok.
 */
class Tercihler(baglam: Context) {

    private val depo = baglam.applicationContext
        .getSharedPreferences("pdf_kutusu_tercihler", Context.MODE_PRIVATE)

    /** Ilk acilistaki "dusuk riskli kullanim" uyarisi onaylandi mi? */
    var uyariOnaylandi: Boolean
        get() = depo.getBoolean(ANAHTAR_UYARI, false)
        set(deger) = depo.edit().putBoolean(ANAHTAR_UYARI, deger).apply()

    fun sifirla() = depo.edit().clear().apply()

    private companion object {
        const val ANAHTAR_UYARI = "uyari_onaylandi_v1"
    }
}
