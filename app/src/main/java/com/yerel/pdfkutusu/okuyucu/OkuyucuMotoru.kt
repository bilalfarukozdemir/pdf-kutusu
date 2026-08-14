package com.yerel.pdfkutusu.okuyucu

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import java.io.Closeable
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Sayfanin nokta cinsinden boyutu (dondurme uygulanmis). */
data class SayfaBoyutu(val genislik: Float, val yukseklik: Float) {
    val oran: Float get() = if (genislik <= 0f) 1.414f else yukseklik / genislik
}

/**
 * Okuyucunun cizim motoru. Akiciligin tamami buradaki uc karara bagli.
 *
 * ## 1. Seri erisim
 * [PdfRenderer] is parcacigi guvenli DEGILDIR ve ayni anda yalnizca tek bir
 * sayfa acik olabilir. Tum cizim tek bir [Mutex] arkasinda sirayla yapilir;
 * cagrilar arka planda kosar, ana is parcacigi hicbir zaman beklemez.
 *
 * ## 2. Iki katmanli onbellek
 * Her sayfanin **ucuz** bir surumu ([ONIZLEME_GENISLIGI] piksel) uretilir ve
 * uzun sure onbellekte kalir. Ekranda gorunen sayfalar ayrica tam cozunurlukte
 * cizilir. Kullanici kaydirirken ya da yakinlastirirken **her zaman
 * cizilebilecek bir sey vardir**: net surum hazir degilse ucuz surum buyutulup
 * gosterilir, net surum gelince yerine gecer. Bekleme, dolayisiyla takilma
 * olmaz.
 *
 * ## 3. Genislik kovalama
 * Yakinlastirma surekli bir deger; her piksel degisiminde yeniden cizmek
 * anlamsiz olurdu. Istenen genislik [KOVA_ADIMI] katlarina yuvarlanir, boylece
 * yakinlastirma sirasinda ayni bitmap tekrar kullanilir.
 */
class OkuyucuMotoru private constructor(
    private val tanimlayici: ParcelFileDescriptor,
    private val motor: PdfRenderer,
) : Closeable {

    private val kilit = Mutex()

    @Volatile
    private var kapandi = false

    val sayfaSayisi: Int = motor.pageCount

    /** Sayfa boyutlari istendikce doldurulur; 1000 sayfalik belgede onden taranmaz. */
    private val boyutlar = arrayOfNulls<SayfaBoyutu>(sayfaSayisi)

    /** Ilk ogrenilen oran, henuz olculmemis sayfalar icin yer tutucu. */
    @Volatile
    private var varsayilanOran: Float = 1.414f

    /**
     * Ucuz onizleme katmani. **Ayri bir onbellek olmasi kritik:** tek bir
     * yakinlastirilmis sayfa 20 MB'i asabiliyor ve ortak onbellekte butun
     * onizlemeleri sipurup goturuyordu. Sonuc, kullanicinin gordugu sey:
     * biraz gezindikten sonra o an bakilan sayfa disinda her sey "yukleniyor"
     * durumuna dusuyordu.
     */
    private val onizlemeOnbellegi: LruCache<Int, Bitmap>

    /** Tam cozunurluklu sayfalar. */
    private val netOnbellek: LruCache<String, Bitmap>

    /** Sayfa -> o sayfa icin net onbellekte bulunan genislik kovalari. */
    private val mevcutKovalar = ConcurrentHashMap<Int, MutableSet<Int>>()

    init {
        val azamiBellek = Runtime.getRuntime().maxMemory()
        val onizlemeButcesi = (azamiBellek / 12)
            .coerceIn(8L * 1024 * 1024, 40L * 1024 * 1024).toInt()
        val netButce = (azamiBellek / 6)
            .coerceIn(16L * 1024 * 1024, 96L * 1024 * 1024).toInt()

        onizlemeOnbellegi = object : LruCache<Int, Bitmap>(onizlemeButcesi) {
            override fun sizeOf(anahtar: Int, deger: Bitmap): Int = deger.byteCount
        }

        netOnbellek = object : LruCache<String, Bitmap>(netButce) {
            override fun sizeOf(anahtar: String, deger: Bitmap): Int = deger.byteCount

            override fun entryRemoved(
                degerlendirildi: Boolean,
                anahtar: String,
                eski: Bitmap,
                yeni: Bitmap?,
            ) {
                if (yeni == null) {
                    val (sayfa, kova) = anahtarCoz(anahtar) ?: return
                    mevcutKovalar[sayfa]?.remove(kova)
                }
            }
        }
    }

    // ------------------------------------------------------------- boyutlar

    /**
     * Sayfanin en/boy orani. Olculmemisse son bilinen orani dondurur; boylece
     * liste yerlesimi beklemeden kurulabilir, olcum gelince duzelir.
     */
    fun oranTahmini(indeks: Int): Float = boyutlar.getOrNull(indeks)?.oran ?: varsayilanOran

    /** Kac sayfanin gercek orani olculdu. Yerlesim bunu izleyip tazelenir. */
    fun olculenOranSayisi(): Int = boyutlar.count { it != null }

    suspend fun boyut(indeks: Int): SayfaBoyutu? {
        boyutlar.getOrNull(indeks)?.let { return it }
        if (indeks !in 0 until sayfaSayisi) return null
        return kilit.withLock {
            boyutlar[indeks] ?: withContext(Dispatchers.IO) { boyutIcsel(indeks) }
        }
    }

    private fun boyutIcsel(indeks: Int): SayfaBoyutu? {
        if (kapandi) return null
        return runCatching {
            val sayfa = motor.openPage(indeks)
            try {
                SayfaBoyutu(sayfa.width.toFloat(), sayfa.height.toFloat()).also {
                    boyutlar[indeks] = it
                    varsayilanOran = it.oran
                }
            } finally {
                runCatching { sayfa.close() }
            }
        }.getOrNull()
    }

    // --------------------------------------------------------------- cizim

    /**
     * Onbellekten **beklemeden** en iyi mevcut bitmap'i dondurur.
     * Compose bunu her karede cagirabilir; disk ya da cizim islemi yapmaz.
     */
    fun onbellekten(indeks: Int, tercihEdilenGenislik: Int): Bitmap? {
        val hedefKova = kova(tercihEdilenGenislik)
        val kovalar = mevcutKovalar[indeks]

        if (kovalar != null && kovalar.isNotEmpty()) {
            // Once tam ya da daha buyuk olan en kucugu; yoksa en buyuk kucuk olan.
            val uygun = kovalar.filter { it >= hedefKova }.minOrNull() ?: kovalar.maxOrNull()
            if (uygun != null) {
                netOnbellek.get(anahtar(indeks, uygun))?.let { return it }
            }
        }
        // Net surum yoksa ucuz katman: gosterilecek bir sey her zaman bulunsun.
        return onizlemeOnbellegi.get(indeks)
    }

    /**
     * Sayfayi istenen genislikte cizer ve onbellege koyar.
     * Hata durumunda null doner - okuyucu tek bir bozuk sayfa yuzunden
     * kapanmamali.
     */
    suspend fun ciz(indeks: Int, hedefGenislikPx: Int): Bitmap? {
        if (indeks !in 0 until sayfaSayisi) return null
        val kova = kova(hedefGenislikPx)
        hazirdanAl(indeks, kova)?.let { return it }

        return kilit.withLock {
            if (kapandi) return@withLock null
            hazirdanAl(indeks, kova)?.let { return@withLock it }
            withContext(Dispatchers.IO) { cizIcsel(indeks, kova) }
        }
    }

    private fun hazirdanAl(indeks: Int, kova: Int): Bitmap? =
        if (kova == ONIZLEME_GENISLIGI) {
            onizlemeOnbellegi.get(indeks)
        } else {
            netOnbellek.get(anahtar(indeks, kova))
        }

    private fun cizIcsel(indeks: Int, kova: Int): Bitmap? {
        val boyut = boyutlar[indeks] ?: boyutIcsel(indeks) ?: return null
        var genislik = kova

        // Bellek yetmezse cozunurlugu yariya indirip yeniden dene. Okuyucu
        // buyuk bir tarama yuzunden cokmemeli.
        repeat(4) {
            if (kapandi) return null
            val yukseklik = max(1, (genislik * boyut.oran).roundToInt())
            try {
                val bitmap = Bitmap.createBitmap(genislik, yukseklik, Bitmap.Config.ARGB_8888)
                Canvas(bitmap).drawColor(Color.WHITE)
                val sayfa = motor.openPage(indeks)
                try {
                    sayfa.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                } finally {
                    runCatching { sayfa.close() }
                }
                bitmap.setHasAlpha(false)
                if (genislik == ONIZLEME_GENISLIGI) {
                    onizlemeOnbellegi.put(indeks, bitmap)
                } else {
                    netOnbellek.put(anahtar(indeks, genislik), bitmap)
                    mevcutKovalar
                        .getOrPut(indeks) { java.util.Collections.synchronizedSet(mutableSetOf()) }
                        .add(genislik)
                }
                return bitmap
            } catch (yetersiz: OutOfMemoryError) {
                // Yalnizca net onbellegi bosalt: onizlemeler gitmezse
                // ekranda hala gosterilecek bir sey kalir.
                netOnbellek.evictAll()
                mevcutKovalar.clear()
                genislik = max(KOVA_ADIMI, genislik / 2)
            } catch (hata: Exception) {
                // Bozuk sayfa: bu sayfayi atla, belge acik kalsin.
                return null
            }
        }
        return null
    }

    override fun close() {
        kapandi = true
        runCatching { netOnbellek.evictAll() }
        runCatching { onizlemeOnbellegi.evictAll() }
        mevcutKovalar.clear()
        runCatching { motor.close() }
        runCatching { tanimlayici.close() }
    }

    // ---------------------------------------------------------- yardimcilar

    private fun kova(genislik: Int): Int {
        val sinirli = genislik.coerceIn(ONIZLEME_GENISLIGI, AZAMI_GENISLIK)
        return (((sinirli + KOVA_ADIMI - 1) / KOVA_ADIMI) * KOVA_ADIMI)
            .coerceAtMost(AZAMI_GENISLIK)
    }

    private fun anahtar(indeks: Int, kova: Int) = "$indeks@$kova"

    private fun anahtarCoz(anahtar: String): Pair<Int, Int>? {
        val parcalar = anahtar.split('@')
        if (parcalar.size != 2) return null
        val sayfa = parcalar[0].toIntOrNull() ?: return null
        val kova = parcalar[1].toIntOrNull() ?: return null
        return sayfa to kova
    }

    companion object {
        /** Her sayfanin ucuz surumu; kaydirmada anlik gosterilir. */
        const val ONIZLEME_GENISLIGI = 256

        /**
         * Yakinlastirmada tek bir sayfanin cizilecegi azami genislik.
         *
         * 1600 px'lik bir A4 bitmap'i ~14 MB tutar; onbellege birkac sayfa
         * sigar. Daha yukarisi (3072 px -> 53 MB) tek bir sayfayla onbellegi
         * doldurup surekli yeniden cizime yol aciyordu. Daha derin
         * yakinlastirmada netligi artirmanin dogru yolu tum sayfayi buyuk
         * cizmek degil, yalnizca gorunen bolgeyi karo karo cizmektir.
         */
        const val AZAMI_GENISLIK = 1600

        const val KOVA_ADIMI = 256

        /**
         * @throws Exception dosya acilamazsa. Cagiran [BelgeKaynagi] ile
         *   dogruladigi icin burada normal kosulda hata beklenmez.
         */
        fun ac(dosya: File): OkuyucuMotoru {
            val tanimlayici = ParcelFileDescriptor.open(
                dosya,
                ParcelFileDescriptor.MODE_READ_ONLY,
            )
            return try {
                OkuyucuMotoru(tanimlayici, PdfRenderer(tanimlayici))
            } catch (hata: Throwable) {
                runCatching { tanimlayici.close() }
                throw hata
            }
        }

        /** Yakinlastirma oranina gore istenecek cizim genisligi. */
        fun hedefGenislik(gorunumGenisligiPx: Int, yakinlastirma: Float): Int =
            min(AZAMI_GENISLIK, max(ONIZLEME_GENISLIGI, (gorunumGenisligiPx * yakinlastirma).roundToInt()))
    }
}
