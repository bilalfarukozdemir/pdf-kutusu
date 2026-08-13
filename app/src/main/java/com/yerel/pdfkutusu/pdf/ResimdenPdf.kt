package com.yerel.pdfkutusu.pdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.yerel.pdfkutusu.cekirdek.PdfHatasi
import java.io.File
import java.util.Locale
import java.util.concurrent.CancellationException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class GorselGirdisi(val dosya: File, val gorunenAd: String)

/** Islenemeyen bir gorsel ve nedeni; kullaniciya listelenir. */
data class AtlananGorsel(val ad: String, val neden: String)

data class ResimdenPdfAyarlari(
    val duzen: SayfaDuzeni = SayfaDuzeni.A4_SIGDIR,
    val kenarBoslguMm: Int = 10,
    val dpi: Int = 150,
    val kalite: SikistirmaKalitesi = SikistirmaKalitesi.ORTA,
)

data class ResimdenPdfSonucu(
    val sayfaSayisi: Int,
    val atlananlar: List<AtlananGorsel>,
    val ciktiBoyutuBayt: Long,
)

/**
 * Birden fazla gorseli tek bir PDF'e cevirir.
 *
 * ## Uc kritik davranis
 *
 * **1. EXIF yonu uygulanir.** Telefon fotograflari sensorden dik gelmez; yon
 * bilgisi EXIF etiketindedir. Ham gomulurse sayfalarin yarisi yan yatar.
 * Esleme [ExifYonu] icinde, saf ve test edilebilir.
 *
 * **2. EXIF verisi ciktiya sizmaz.** Fotograflar GPS koordinati, cihaz modeli
 * ve zaman damgasi tasir. Burada gorsel bitmap'e cozulup **yeniden kodlanir**;
 * yeni JPEG akisinda hicbir EXIF bolumu bulunmaz. Belge meta verileri de
 * [MetaVeriTemizleyici] ile temizlenir. Bu, uygulamanin varlik sebebinin
 * geregidir ve cihaz testiyle kanitlanir.
 *
 * **3. Saydam PNG beyaza duzlestirilir.** JPEG alfa kanali tasimaz; saydam
 * bolgeler dogrudan kodlanirsa **siyah** cikar.
 *
 * ## Bellek
 * 12 MP bir fotograf ARGB_8888'de ~48 MB tutar. Bu yuzden gorseller
 * **teker teker** islenir, once `inJustDecodeBounds` ile boyut okunur,
 * `inSampleSize` ile kucultulerek cozulur ve her adimda ara bitmap'ler geri
 * verilir. Belge de ana bellek yerine gecici dosyada tutulur.
 */
object ResimdenPdf {

    fun olustur(
        girdiler: List<GorselGirdisi>,
        ayarlar: ResimdenPdfAyarlari,
        cikti: File,
        gecicilerDizini: File,
        ilerleme: IlerlemeDinleyicisi = IlerlemeYok,
    ): ResimdenPdfSonucu {
        if (girdiler.isEmpty()) {
            throw PdfHatasi.GirdiYok("PDF'e çevirmek için en az bir görsel seçin.")
        }

        val atlananlar = mutableListOf<AtlananGorsel>()
        var sayfaSayisi = 0

        // 100 gorselde belge ana bellege sigmaz. Gecici dizini ACIKCA veriyoruz:
        // PDFBox varsayilan olarak java.io.tmpdir kullanir, Android'de bu yol
        // guvenilir sekilde yazilabilir degildir.
        val bellekAyari = MemoryUsageSetting.setupTempFileOnly().setTempDir(gecicilerDizini)

        PDDocument(bellekAyari).use { hedef ->
            girdiler.forEachIndexed { sira, girdi ->
                // Ilerleme bildirimleri try blogunun DISINDA: iptal istisnasi
                // "bozuk gorsel" sanilip yutulmasin.
                ilerleme(Ilerleme(sira, girdiler.size, girdi.gorunenAd))

                try {
                    val bitmap = bitmapHazirla(girdi.dosya, ayarlar.kalite.azamiKenarPiksel)
                    try {
                        sayfaEkle(hedef, bitmap, ayarlar)
                        sayfaSayisi++
                    } finally {
                        runCatching { bitmap.recycle() }
                    }
                } catch (iptal: CancellationException) {
                    throw iptal
                } catch (bellek: OutOfMemoryError) {
                    atlananlar += AtlananGorsel(girdi.gorunenAd, "Bellek yetmedi, görsel çok büyük.")
                } catch (hata: PdfHatasi) {
                    atlananlar += AtlananGorsel(girdi.gorunenAd, hata.kullaniciMesaji)
                } catch (hata: Exception) {
                    atlananlar += AtlananGorsel(
                        girdi.gorunenAd,
                        hata.message ?: "Görsel okunamadı.",
                    )
                }

                ilerleme(Ilerleme(sira + 1, girdiler.size, girdi.gorunenAd))
            }

            if (sayfaSayisi == 0) {
                val ozet = atlananlar.joinToString("; ") { "${it.ad}: ${it.neden}" }.take(300)
                throw PdfHatasi.BozukBelge("Hiçbir görsel işlenemedi. $ozet")
            }

            MetaVeriTemizleyici.temizle(hedef)
            hedef.save(cikti)
        }

        return ResimdenPdfSonucu(
            sayfaSayisi = sayfaSayisi,
            atlananlar = atlananlar,
            ciktiBoyutuBayt = cikti.length(),
        )
    }

    // ------------------------------------------------------------- gorsel

    /** Coz, yonlendir, kucult, beyaza duzlestir. Cagiran geri vermekle yukumlu. */
    internal fun bitmapHazirla(dosya: File, azamiKenar: Int): Bitmap {
        val olcum = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching { dosya.inputStream().use { BitmapFactory.decodeStream(it, null, olcum) } }
        if (olcum.outWidth <= 0 || olcum.outHeight <= 0) {
            throw PdfHatasi.BozukBelge(desteklenmeyenNeden(dosya))
        }

        val cozumAyari = BitmapFactory.Options().apply {
            inSampleSize = SayfaYerlesimi.ornekBoyutu(olcum.outWidth, olcum.outHeight, azamiKenar)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val ham = dosya.inputStream().use { BitmapFactory.decodeStream(it, null, cozumAyari) }
            ?: throw PdfHatasi.BozukBelge(desteklenmeyenNeden(dosya))

        val yonlu = yonuUygula(dosya, ham)
        val kucuk = kucult(yonlu, azamiKenar)
        return beyazaDuzlestir(kucuk)
    }

    private fun yonuUygula(dosya: File, kaynak: Bitmap): Bitmap {
        val etiket = runCatching {
            dosya.inputStream().use { akis ->
                ExifInterface(akis).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val donusum = ExifYonu.donusum(etiket)
        if (donusum.kimlikMi) return kaynak

        // Once dondur, sonra yatay aynala - ExifYonu'nun tanimladigi sira.
        val matris = Matrix().apply {
            setRotate(donusum.donusDerecesi.toFloat())
            if (donusum.yatayAyna) postScale(-1f, 1f)
        }
        val donmus = Bitmap.createBitmap(
            kaynak, 0, 0, kaynak.width, kaynak.height, matris, true,
        )
        if (donmus !== kaynak) runCatching { kaynak.recycle() }
        return donmus
    }

    private fun kucult(kaynak: Bitmap, azamiKenar: Int): Bitmap {
        val enBuyukKenar = max(kaynak.width, kaynak.height)
        if (enBuyukKenar <= azamiKenar) return kaynak
        val oran = azamiKenar.toFloat() / enBuyukKenar
        val yeni = Bitmap.createScaledBitmap(
            kaynak,
            max(1, (kaynak.width * oran).roundToInt()),
            max(1, (kaynak.height * oran).roundToInt()),
            true,
        )
        if (yeni !== kaynak) runCatching { kaynak.recycle() }
        return yeni
    }

    /**
     * Saydamligi beyaz zemine dumduzler.
     *
     * JPEG alfa tasimaz: saydam PNG dogrudan kodlanirsa o bolgeler siyah cikar.
     * Alfasi olmayan gorsellerde (JPEG'lerin cogu) kopya cikarmadan geciyoruz,
     * bu 12 MP'lik bir fotografta ~48 MB tasarruf demek.
     */
    private fun beyazaDuzlestir(kaynak: Bitmap): Bitmap {
        if (!kaynak.hasAlpha()) return kaynak

        val hedef = Bitmap.createBitmap(kaynak.width, kaynak.height, Bitmap.Config.ARGB_8888)
        Canvas(hedef).apply {
            drawColor(Color.WHITE)
            drawBitmap(kaynak, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
        }
        runCatching { kaynak.recycle() }
        hedef.setHasAlpha(false)
        return hedef
    }

    private fun sayfaEkle(hedef: PDDocument, bitmap: Bitmap, ayarlar: ResimdenPdfAyarlari) {
        val yerlesim = SayfaYerlesimi.hesapla(
            gorselEn = bitmap.width,
            gorselBoy = bitmap.height,
            duzen = ayarlar.duzen,
            kenarBoslguMm = ayarlar.kenarBoslguMm,
            dpi = ayarlar.dpi,
        )
        val gorsel = JPEGFactory.createFromImage(hedef, bitmap, ayarlar.kalite.jpegKalitesi)
        val sayfa = PDPage(PDRectangle(yerlesim.sayfaEn, yerlesim.sayfaBoy))
        hedef.addPage(sayfa)
        PDPageContentStream(hedef, sayfa).use { akis ->
            akis.drawImage(
                gorsel,
                yerlesim.cizimX,
                yerlesim.cizimY,
                yerlesim.cizimEn,
                yerlesim.cizimBoy,
            )
        }
    }

    private fun desteklenmeyenNeden(dosya: File): String {
        val uzanti = dosya.extension.lowercase(Locale.ROOT)
        if (uzanti in setOf("heic", "heif") && Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return "HEIC/HEIF görselleri Android 9 (API 28) ve üzeri gerektirir; " +
                "bu cihaz Android ${Build.VERSION.RELEASE}. Görseli JPEG'e çevirip tekrar deneyin."
        }
        return "Görsel biçimi tanınmadı ya da dosya bozuk."
    }

    // ------------------------------------------------------------- tahmin

    /**
     * Islemeden once kaba cikti boyutu tahmini.
     *
     * Gorselleri **cozmeden**, yalnizca basliktaki boyut bilgisini okur.
     * Kalite katsayilari [SikistirmaKalitesi] icinden gelir - sikistirma
     * mantigi burada yeniden yazilmaz.
     */
    fun tahminEt(dosyalar: List<File>, kalite: SikistirmaKalitesi): Long {
        var toplam = 0L
        for (dosya in dosyalar) {
            val olcum = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            runCatching { dosya.inputStream().use { BitmapFactory.decodeStream(it, null, olcum) } }
            val en = olcum.outWidth
            val boy = olcum.outHeight
            if (en <= 0 || boy <= 0) continue
            val oran = min(1.0, kalite.azamiKenarPiksel.toDouble() / max(en, boy))
            val piksel = en.toDouble() * boy.toDouble() * oran * oran
            toplam += (piksel * kalite.baytPiksel).toLong()
        }
        // Sayfa basina PDF yapisal yuku.
        return toplam + 2_000L * dosyalar.size
    }
}
