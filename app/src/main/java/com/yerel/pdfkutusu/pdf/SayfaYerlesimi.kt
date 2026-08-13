package com.yerel.pdfkutusu.pdf

import kotlin.math.max
import kotlin.math.min

/** Resimden PDF sayfa duzeni. */
enum class SayfaDuzeni(val etiket: String, val aciklama: String) {
    A4_SIGDIR(
        "A4'e sığdır",
        "Sayfa A4 olur, görsel ortalanır. Yatay görselde sayfa da yatay olur.",
    ),
    GORUNTU_BOYUTU(
        "Görüntü boyutu",
        "Sayfa görselin kendi boyutunda olur, kenar boşluğu yok. Ekran görüntüleri ve taramalar için.",
    ),
}

/**
 * Bir gorselin sayfadaki yeri; hepsi PDF noktasi (1/72 inc) cinsinden.
 */
data class Yerlesim(
    val sayfaEn: Float,
    val sayfaBoy: Float,
    val cizimX: Float,
    val cizimY: Float,
    val cizimEn: Float,
    val cizimBoy: Float,
) {
    /** Cizim en-boy orani; kaynak oranla karsilastirmak icin. */
    val cizimOrani: Float get() = if (cizimBoy <= 0f) 0f else cizimEn / cizimBoy
}

/**
 * Sayfa yerlesimi ve orneklem hesaplari.
 *
 * Bu nesne bilerek **saf**tir: Android grafik sinifi kullanmaz, dolayisiyla
 * gercek birim testiyle dogrulanabilir. En-boy oraninin korunmasi burada
 * garanti altina alinir.
 */
object SayfaYerlesimi {

    const val A4_EN = 595.276f
    const val A4_BOY = 841.89f

    /** 1 mm = 72/25.4 nokta. */
    const val MM_NOKTA = 72f / 25.4f

    val KENAR_BOSLUKLARI_MM = listOf(0, 10, 20)
    val DPI_SECENEKLERI = listOf(72, 150, 300)

    fun hesapla(
        gorselEn: Int,
        gorselBoy: Int,
        duzen: SayfaDuzeni,
        kenarBoslguMm: Int,
        dpi: Int,
    ): Yerlesim = when (duzen) {
        SayfaDuzeni.A4_SIGDIR -> a4Sigdir(gorselEn, gorselBoy, kenarBoslguMm)
        SayfaDuzeni.GORUNTU_BOYUTU -> goruntuBoyutu(gorselEn, gorselBoy, dpi)
    }

    /**
     * Gorseli A4'e ortalar. Yatay gorselde sayfa da yatay olur.
     * En-boy orani **her zaman** korunur: tek bir olcek carpani kullanilir.
     */
    fun a4Sigdir(gorselEn: Int, gorselBoy: Int, kenarBoslguMm: Int): Yerlesim {
        val en = max(1, gorselEn)
        val boy = max(1, gorselBoy)
        val yatay = en > boy
        val sayfaEn = if (yatay) A4_BOY else A4_EN
        val sayfaBoy = if (yatay) A4_EN else A4_BOY

        val bosluk = (max(0, kenarBoslguMm) * MM_NOKTA)
            .coerceAtMost(min(sayfaEn, sayfaBoy) / 2f - 1f)
        val kullanilabilirEn = (sayfaEn - 2 * bosluk).coerceAtLeast(1f)
        val kullanilabilirBoy = (sayfaBoy - 2 * bosluk).coerceAtLeast(1f)

        val olcek = min(kullanilabilirEn / en, kullanilabilirBoy / boy)
        val cizimEn = en * olcek
        val cizimBoy = boy * olcek

        return Yerlesim(
            sayfaEn = sayfaEn,
            sayfaBoy = sayfaBoy,
            cizimX = (sayfaEn - cizimEn) / 2f,
            cizimY = (sayfaBoy - cizimBoy) / 2f,
            cizimEn = cizimEn,
            cizimBoy = cizimBoy,
        )
    }

    /** Sayfa boyutu = piksel x 72 / DPI. Oran yapisi geregi korunur. */
    fun goruntuBoyutu(gorselEn: Int, gorselBoy: Int, dpi: Int): Yerlesim {
        val en = max(1, gorselEn)
        val boy = max(1, gorselBoy)
        val olcek = 72f / max(1, dpi)
        val sayfaEn = (en * olcek).coerceAtLeast(1f)
        val sayfaBoy = (boy * olcek).coerceAtLeast(1f)
        return Yerlesim(sayfaEn, sayfaBoy, 0f, 0f, sayfaEn, sayfaBoy)
    }

    /**
     * `BitmapFactory.Options.inSampleSize` degeri.
     *
     * Yalnizca 2'nin kuvvetleri kullanilabilir; bu yuzden hedefin **altina
     * dusmeyen** en buyuk kuvveti seceriz. Kalan fark sonradan
     * `createScaledBitmap` ile kapatilir - boylece hicbir asamada gorsel
     * buyutulmez.
     */
    fun ornekBoyutu(gorselEn: Int, gorselBoy: Int, azamiKenar: Int): Int {
        if (gorselEn <= 0 || gorselBoy <= 0 || azamiKenar <= 0) return 1
        val enBuyukKenar = max(gorselEn, gorselBoy)
        var ornek = 1
        while (enBuyukKenar / (ornek * 2) >= azamiKenar && ornek < 1 shl 16) {
            ornek *= 2
        }
        return ornek
    }
}
