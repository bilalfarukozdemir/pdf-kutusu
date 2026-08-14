package com.yerel.pdfkutusu.okuyucu

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.yerel.pdfkutusu.cekirdek.DosyaAdi
import com.yerel.pdfkutusu.pdf.BelgeErisimi
import java.io.File
import java.io.FileNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Disaridan gelen bir PDF'i cizilebilir hale getirir.
 *
 * Varsayilan okuyucu olmak, "kullanicinin bilerek sectigi dosya" varsayimini
 * ortadan kaldirir: belge e-postadan, sohbet uygulamasindan, bulut deposundan
 * ya da indirilenler klasorunden gelebilir. Bu yuzden buradaki her adim
 * savunmaci yazildi.
 *
 * Ele alinan durumlar:
 *  - Saglayici **aranabilir olmayan** bir tanimlayici (pipe) veriyor.
 *    `PdfRenderer` aranabilir dosya ister; bu durumda onbellege kopyalanir.
 *  - Belge **sifreli**. `PdfRenderer` `SecurityException` firlatir; parola
 *    alinip PDFBox ile cozulmus gecici bir kopya uretilir.
 *  - Dosya bozuk, PDF degil ya da bos.
 *  - URI izni geri cekilmis / dosya silinmis.
 */
object BelgeKaynagi {

    sealed interface Sonuc {
        /**
         * @param gecici true ise [dosya] onbellekte uretilmis kopyadir ve
         *   okuyucu kapanirken silinmelidir.
         */
        data class Hazir(
            val dosya: File,
            val gorunenAd: String,
            val gecici: Boolean,
            val sifresiCozuldu: Boolean,
        ) : Sonuc

        data class ParolaGerekli(val gorunenAd: String) : Sonuc

        data class Hata(val mesaj: String, val oneri: String? = null) : Sonuc
    }

    suspend fun coz(
        baglam: Context,
        uri: Uri,
        parola: String?,
        gecicilerDizini: File,
    ): Sonuc = withContext(Dispatchers.IO) {
        val gorunenAd = DosyaAdi.guvenli(adOku(baglam, uri), varsayilan = "belge.pdf")

        // 1) Cizilebilir bir yerel dosya elde et.
        val yerel = try {
            yerelDosyaElde(baglam, uri, gorunenAd, gecicilerDizini)
        } catch (hata: SecurityException) {
            return@withContext Sonuc.Hata(
                "Bu dosyaya erişim izni yok.",
                "Dosyayı gönderen uygulama izni geri çekmiş olabilir. Yeniden açmayı deneyin.",
            )
        } catch (hata: FileNotFoundException) {
            return@withContext Sonuc.Hata(
                "Dosya bulunamadı.",
                "Taşınmış ya da silinmiş olabilir.",
            )
        } catch (hata: Exception) {
            return@withContext Sonuc.Hata(
                "Dosya açılamadı.",
                hata.message?.take(120),
            )
        }

        if (yerel.dosya.length() == 0L) {
            yerel.temizle()
            return@withContext Sonuc.Hata("Dosya boş.", "İçeriği olmayan bir dosya seçilmiş.")
        }

        // 2) PdfRenderer gercekten acabiliyor mu? Asil dogrulama bu.
        when (val deneme = acilabilirMi(yerel.dosya)) {
            AcmaDenemesi.TAMAM ->
                return@withContext Sonuc.Hazir(yerel.dosya, gorunenAd, yerel.gecici, false)

            AcmaDenemesi.SIFRELI -> {
                if (parola.isNullOrEmpty()) {
                    yerel.temizle()
                    return@withContext Sonuc.ParolaGerekli(gorunenAd)
                }
                val cozulmus = try {
                    sifreyiCoz(yerel.dosya, parola, gecicilerDizini)
                } catch (hata: Exception) {
                    yerel.temizle()
                    return@withContext Sonuc.Hata(
                        "Parola doğrulanamadı.",
                        "Büyük/küçük harfe dikkat edip tekrar deneyin.",
                    )
                }
                yerel.temizle()
                return@withContext if (acilabilirMi(cozulmus) == AcmaDenemesi.TAMAM) {
                    Sonuc.Hazir(cozulmus, gorunenAd, gecici = true, sifresiCozuldu = true)
                } else {
                    runCatching { cozulmus.delete() }
                    Sonuc.Hata("Belge çözüldü ama açılamadı.", "Dosya bozuk olabilir.")
                }
            }

            AcmaDenemesi.BOZUK -> {
                yerel.temizle()
                return@withContext Sonuc.Hata(
                    "Bu dosya açılamıyor.",
                    "Geçerli bir PDF olmayabilir ya da bozuk olabilir.",
                )
            }
        }
    }

    // ------------------------------------------------------------------

    private class YerelDosya(val dosya: File, val gecici: Boolean) {
        fun temizle() {
            if (gecici) runCatching { dosya.delete() }
        }
    }

    /**
     * Once dogrudan saglayicinin tanimlayicisini kullanmayi dener; bu, buyuk
     * belgelerde kopyalama maliyetini tamamen ortadan kaldirir. Tanimlayici
     * aranabilir degilse (boru/pipe) onbellege kopyalar.
     */
    private fun yerelDosyaElde(
        baglam: Context,
        uri: Uri,
        gorunenAd: String,
        gecicilerDizini: File,
    ): YerelDosya {
        if (uri.scheme == "file") {
            val yol = uri.path
            if (yol != null) {
                val dosya = File(yol)
                if (dosya.isFile && dosya.canRead()) return YerelDosya(dosya, gecici = false)
            }
        }

        baglam.contentResolver.openFileDescriptor(uri, "r")?.use { tanimlayici ->
            if (aranabilirMi(tanimlayici)) {
                // Aranabilir; yine de PdfRenderer'a kendi actigimiz bir
                // tanimlayici vermemiz gerekiyor. Dosyayi onbellege almadan
                // kullanabilmek icin /proc yolu guvenilir degil, bu yuzden
                // kopyaliyoruz - ama yalnizca gercekten gerektiginde.
                return kopyala(baglam, uri, gorunenAd, gecicilerDizini)
            }
        }
        return kopyala(baglam, uri, gorunenAd, gecicilerDizini)
    }

    private fun aranabilirMi(tanimlayici: ParcelFileDescriptor): Boolean =
        runCatching { tanimlayici.statSize >= 0L }.getOrDefault(false)

    private fun kopyala(
        baglam: Context,
        uri: Uri,
        gorunenAd: String,
        gecicilerDizini: File,
    ): YerelDosya {
        if (!gecicilerDizini.exists()) gecicilerDizini.mkdirs()
        val hedef = File(gecicilerDizini, "okunan_${System.nanoTime()}_$gorunenAd")
        baglam.contentResolver.openInputStream(uri).use { girdi ->
            requireNotNull(girdi) { "Akış açılamadı" }
            hedef.outputStream().use { cikti -> girdi.copyTo(cikti, 256 * 1024) }
        }
        return YerelDosya(hedef, gecici = true)
    }

    private enum class AcmaDenemesi { TAMAM, SIFRELI, BOZUK }

    private fun acilabilirMi(dosya: File): AcmaDenemesi = try {
        ParcelFileDescriptor.open(dosya, ParcelFileDescriptor.MODE_READ_ONLY).use { tanimlayici ->
            PdfRenderer(tanimlayici).use { motor ->
                if (motor.pageCount > 0) AcmaDenemesi.TAMAM else AcmaDenemesi.BOZUK
            }
        }
    } catch (hata: SecurityException) {
        // PdfRenderer sifreli belgede bunu firlatir.
        AcmaDenemesi.SIFRELI
    } catch (hata: Exception) {
        AcmaDenemesi.BOZUK
    }

    private fun sifreyiCoz(kaynak: File, parola: String, gecicilerDizini: File): File {
        val hedef = File(gecicilerDizini, "cozulmus_${System.nanoTime()}.pdf")
        BelgeErisimi.ac(kaynak, parola).use { belge ->
            belge.isAllSecurityToBeRemoved = true
            belge.save(hedef)
        }
        return hedef
    }

    private fun adOku(baglam: Context, uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        val sorgudan = runCatching {
            baglam.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { imlec ->
                    if (imlec.moveToFirst() && !imlec.isNull(0)) imlec.getString(0) else null
                }
        }.getOrNull()
        return sorgudan ?: uri.lastPathSegment
    }
}
