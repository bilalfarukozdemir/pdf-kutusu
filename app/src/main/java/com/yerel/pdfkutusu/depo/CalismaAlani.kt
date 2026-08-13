package com.yerel.pdfkutusu.depo

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.yerel.pdfkutusu.cekirdek.DosyaAdi
import com.yerel.pdfkutusu.cekirdek.Ozet
import com.yerel.pdfkutusu.cekirdek.PdfHatasi
import com.yerel.pdfkutusu.pdf.Ilerleme
import com.yerel.pdfkutusu.pdf.IlerlemeDinleyicisi
import com.yerel.pdfkutusu.pdf.IlerlemeYok
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** SAF'tan alinip uygulama alanina kopyalanmis bir girdi. */
data class CalismaDosyasi(
    val dosya: File,
    val gorunenAd: String,
    val kaynakUri: Uri?,
    val boyut: Long,
    val sha256: String,
)

/** Uretilmis bir cikti dosyasi. */
data class CiktiDosyasi(
    val dosya: File,
    val ad: String,
    val boyut: Long,
    val degistirilmeZamani: Long,
)

data class DisaAktarimSonucu(
    val basarili: List<String>,
    val basarisiz: List<Pair<String, String>>,
)

/**
 * Dosya yasam dongusu.
 *
 * ## Degismez kural
 * **Kaynak URI'ye asla yazilmaz.** SAF'tan secilen her dosya once
 * [calismaDizini] icine kopyalanir; tum islemler bu kopya uzerinde yapilir ve
 * sonuc [ciktiDizini] icinde YENI bir dosya olur. Kullanicinin telefonundaki
 * orijinal dosya, uygulama ne yaparsa yapsin bozulmaz.
 *
 * ## Nerede durur
 *  - `filesDir/calisma` : secilen girdilerin kopyalari
 *  - `filesDir/cikti`   : uretilen PDF/TXT dosyalari
 *  - `cacheDir/gecici`  : ara dosyalar (sifre cozulmus kopya vb.)
 *
 * Bunlarin hepsi uygulamaya ozel alandir; baska uygulamalar goremez, izin
 * gerektirmez ve uygulama kaldirilinca silinir.
 */
class CalismaAlani(private val baglam: Context) {

    val calismaDizini: File by lazy { dizinHazirla(File(baglam.filesDir, "calisma")) }
    val ciktiDizini: File by lazy { dizinHazirla(File(baglam.filesDir, "cikti")) }
    val gecicilerDizini: File by lazy { dizinHazirla(File(baglam.cacheDir, "gecici")) }

    // ---------------------------------------------------------------- girdi

    /** SAF URI'sindeki dosyayi calisma alanina kopyalar ve ozetini hesaplar. */
    suspend fun iceriAktar(uri: Uri): CalismaDosyasi = withContext(Dispatchers.IO) {
        val hamAd = gorunenAd(uri) ?: "belge.pdf"
        val guvenliAd = DosyaAdi.guvenli(hamAd)
        val hedef = DosyaAdi.cakismayan(calismaDizini, guvenliAd)

        try {
            baglam.contentResolver.openInputStream(uri).use { girdi ->
                if (girdi == null) throw PdfHatasi.DosyaOkunamadi("Dosya açılamadı: $guvenliAd")
                hedef.outputStream().use { cikti -> girdi.copyTo(cikti, 128 * 1024) }
            }
        } catch (hata: PdfHatasi) {
            throw hata
        } catch (hata: Exception) {
            runCatching { hedef.delete() }
            throw PdfHatasi.DosyaOkunamadi("Dosya kopyalanamadı: $guvenliAd", hata)
        }

        if (hedef.length() == 0L) {
            hedef.delete()
            throw PdfHatasi.DosyaOkunamadi("Dosya boş görünüyor: $guvenliAd")
        }

        CalismaDosyasi(
            dosya = hedef,
            gorunenAd = guvenliAd,
            kaynakUri = uri,
            boyut = hedef.length(),
            sha256 = Ozet.sha256(hedef),
        )
    }

    /**
     * SAF `content://` URI'lerinde DISPLAY_NAME sutunu, `file://` URI'lerinde
     * (testler ve bazi dosya yoneticileri) son yol parcasi kullanilir.
     */
    fun gorunenAd(uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment

        val sorgudan = runCatching {
            baglam.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { imlec ->
                    if (imlec.moveToFirst() && !imlec.isNull(0)) imlec.getString(0) else null
                }
        }.getOrNull()
        if (!sorgudan.isNullOrBlank()) return sorgudan

        return runCatching { DocumentFile.fromSingleUri(baglam, uri)?.name }.getOrNull()
            ?: uri.lastPathSegment
    }

    // ---------------------------------------------------------------- cikti

    fun ciktiDosyasi(ad: String): File = DosyaAdi.cakismayan(ciktiDizini, DosyaAdi.guvenli(ad))

    fun ciktilar(): List<CiktiDosyasi> =
        ciktiDizini.listFiles().orEmpty()
            .filter { it.isFile && it.length() > 0 }
            .sortedByDescending { it.lastModified() }
            .map { CiktiDosyasi(it, it.name, it.length(), it.lastModified()) }

    fun sil(dosya: File): Boolean = runCatching { dosya.delete() }.getOrDefault(false)

    fun calismaGirdileriniTemizle() {
        calismaDizini.listFiles()?.forEach { runCatching { it.delete() } }
        gecicilerDizini.listFiles()?.forEach { runCatching { it.delete() } }
    }

    fun tumCiktilariSil(): Int {
        var sayac = 0
        ciktiDizini.listFiles()?.forEach { if (runCatching { it.delete() }.getOrDefault(false)) sayac++ }
        return sayac
    }

    // ----------------------------------------------------------- disa aktar

    /** Tek dosyayi SAF ile kullanicinin sectigi konuma yazar. */
    suspend fun disaAktar(kaynak: File, hedefUri: Uri): Long = withContext(Dispatchers.IO) {
        try {
            baglam.contentResolver.openOutputStream(hedefUri, "wt").use { cikti ->
                if (cikti == null) throw PdfHatasi.DosyaOkunamadi("Hedef konuma yazılamadı.")
                kaynak.inputStream().use { girdi -> girdi.copyTo(cikti, 128 * 1024) }
            }
            kaynak.length()
        } catch (hata: PdfHatasi) {
            throw hata
        } catch (hata: Exception) {
            throw PdfHatasi.DosyaOkunamadi("Dışa aktarma başarısız: ${kaynak.name}", hata)
        }
    }

    /** Secilen klasore birden fazla dosyayi yazar ("tümünü dışa aktar"). */
    suspend fun tumunuDisaAktar(
        agacUri: Uri,
        dosyalar: List<File>,
        ilerleme: IlerlemeDinleyicisi = IlerlemeYok,
    ): DisaAktarimSonucu = withContext(Dispatchers.IO) {
        val klasor = DocumentFile.fromTreeUri(baglam, agacUri)
            ?: throw PdfHatasi.DosyaOkunamadi("Klasör açılamadı.")
        if (!klasor.canWrite()) {
            throw PdfHatasi.DosyaOkunamadi("Seçilen klasöre yazma izni yok.")
        }

        val basarili = mutableListOf<String>()
        val basarisiz = mutableListOf<Pair<String, String>>()

        dosyalar.forEachIndexed { sira, dosya ->
            ilerleme(Ilerleme(sira, dosyalar.size, dosya.name))
            val sonuc = runCatching {
                val tur = if (dosya.extension.equals("txt", true)) "text/plain" else "application/pdf"
                val hedef = klasor.createFile(tur, dosya.name)
                    ?: error("Hedef dosya oluşturulamadı")
                baglam.contentResolver.openOutputStream(hedef.uri, "wt").use { cikti ->
                    if (cikti == null) error("Hedef akış açılamadı")
                    dosya.inputStream().use { girdi -> girdi.copyTo(cikti, 128 * 1024) }
                }
            }
            if (sonuc.isSuccess) {
                basarili += dosya.name
            } else {
                basarisiz += dosya.name to (sonuc.exceptionOrNull()?.message ?: "bilinmeyen hata")
            }
            ilerleme(Ilerleme(sira + 1, dosyalar.size, dosya.name))
        }
        DisaAktarimSonucu(basarili, basarisiz)
    }

    private fun dizinHazirla(dizin: File): File {
        if (!dizin.exists()) dizin.mkdirs()
        return dizin
    }
}
