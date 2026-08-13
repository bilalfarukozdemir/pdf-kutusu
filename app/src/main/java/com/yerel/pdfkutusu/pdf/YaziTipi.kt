package com.yerel.pdfkutusu.pdf

import android.content.Context
import android.content.res.AssetManager
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.File
import java.io.RandomAccessFile
import java.util.Locale

/**
 * Filigran metni icin yazi tipi secimi.
 *
 * ## Sorun
 * PDF'in "standart 14" yazi tipleri (Helvetica vb.) WinAnsi kodlamasi kullanir.
 * WinAnsi'de **c o u C O U vardir, ancak g G s S i I YOKTUR.** Bu harflerle
 * `showText` cagrilirsa PDFBox `IllegalArgumentException` firlatir.
 *
 * ## Cozum: uc kademeli zincir
 * 1. **Paketli Noto Sans** (`assets/fonts/NotoSans-Regular.ttf`, statik surum).
 *    Cihazdan bagimsiz, determinist sonuc. Alt kume (subset) olarak gomulur,
 *    ciktiyi birkac KB buyutur.
 * 2. Cihazin sistem yazi tipleri. **Statik olanlar degiskenlere tercih edilir**
 *    ([degiskenMi] TTF tablo dizininde `fvar` arar): degisken fontlar hem cok
 *    buyuktur hem de PDFBox tarafinda daha az sinanmistir.
 * 3. Helvetica. Turkce'ye ozgu harfler kodlanamaz, [tamTurkceDestegi] false
 *    doner ve arayuz kullaniciyi uyarir.
 *
 * ## Karakter guvencesi
 * Filigran metni serbest kullanici girdisidir; emoji, Kiril ya da CJK
 * icerebilir. Hicbir yazi tipi her seyi tasimaz. Bu yuzden [hazirla] **hangi
 * kademe secilirse secilsin** her karakteri tek tek sinar ve kodlanamayani
 * degistirir. Boylece filigran islemi bir emoji yuzunden hic cokmez.
 */
class YaziTipi private constructor(
    val font: PDFont,
    val tamTurkceDestegi: Boolean,
    val kaynakAdi: String,
) {

    /**
     * Metni bu yazi tipiyle guvenle yazilabilir hale getirir.
     *
     * Kodlanamayan karakter once gorsel karsiligiyla (ğ→g), o da olmazsa
     * `?` ile degistirilir. Asla istisna firlatmaz.
     */
    fun hazirla(metin: String): String = buildString(metin.length) {
        for (harf in metin) {
            when {
                yazilabilir(font, harf.toString()) -> append(harf)
                else -> {
                    val aday = INDIRGEME[harf]
                    when {
                        aday != null && yazilabilir(font, aday.toString()) -> append(aday)
                        yazilabilir(font, "?") -> append('?')
                        else -> Unit // Bu karakteri tamamen atla.
                    }
                }
            }
        }
    }

    fun genislik(metin: String, punto: Float): Float =
        font.getStringWidth(metin) / 1000f * punto

    companion object {

        /** Paketli yazi tipinin `assets` icindeki yolu. */
        const val PAKETLI_YOL = "fonts/NotoSans-Regular.ttf"

        /**
         * WinAnsi'de bulunmayan Turkce harflerin gorsel karsiliklari.
         * c/C, o/O, u/U zaten WinAnsi'de oldugu icin listede yok.
         */
        private val INDIRGEME = mapOf(
            'ğ' to 'g', 'Ğ' to 'G',
            'ş' to 's', 'Ş' to 'S',
            'ı' to 'i', 'İ' to 'I',
        )

        /**
         * Sistem yazi tipi adaylari, tercih sirasiyla. Basta bilerek **statik**
         * surumler var: `Roboto-Regular.ttf` bircok cihazda 2,3 MB'lik degisken
         * fonttur, `RobotoStatic-Regular.ttf` ise 300 KB'lik statik surumu.
         */
        private val SISTEM_ADAYLARI = listOf(
            "NotoSans-Regular.ttf",
            "RobotoStatic-Regular.ttf",
            "DroidSans.ttf",
            "NotoSansDisplay-Regular.ttf",
            "Roboto-Regular.ttf",
        )

        /** Turkce'ye ozgu, WinAnsi'de olmayan harfler; yazi tipi sinamasi icin. */
        private const val SINAMA_METNI = "ğĞşŞıİçÇöÖüÜ"

        @Volatile
        private var varliklar: AssetManager? = null

        /**
         * Uygulama acilisinda cagrilir. `PDFBoxResourceLoader.init` ile ayni
         * yerde durur; ikisi de PdfBox'in varliklara erisebilmesi icindir.
         */
        fun baslat(baglam: Context) {
            varliklar = baglam.applicationContext.assets
        }

        fun yukle(belge: PDDocument): YaziTipi {
            paketliDene(belge)?.let { return it }
            sistemdenDene(belge)?.let { return it }
            return YaziTipi(
                font = PDType1Font.HELVETICA_BOLD,
                tamTurkceDestegi = false,
                kaynakAdi = "Helvetica (gömülü değil)",
            )
        }

        // ------------------------------------------------------------ 1. kademe

        private fun paketliDene(belge: PDDocument): YaziTipi? {
            val yonetici = varliklar ?: return null
            return runCatching {
                val gomulu = yonetici.open(PAKETLI_YOL).use { akis ->
                    // Ucuncu parametre embedSubset: yalnizca kullanilan glifler
                    // gomulur, cikti birkac KB buyur.
                    PDType0Font.load(belge, akis, true)
                }
                require(yazilabilir(gomulu, SINAMA_METNI)) { "Türkçe karakterler eksik" }
                YaziTipi(gomulu, tamTurkceDestegi = true, kaynakAdi = "Noto Sans (paketli)")
            }.getOrNull()
        }

        // ------------------------------------------------------------ 2. kademe

        private fun sistemdenDene(belge: PDDocument): YaziTipi? {
            for (dosya in sistemAdaylari()) {
                val sonuc = runCatching {
                    val gomulu = dosya.inputStream().use { PDType0Font.load(belge, it, true) }
                    require(yazilabilir(gomulu, SINAMA_METNI)) { "Türkçe karakterler eksik" }
                    YaziTipi(gomulu, tamTurkceDestegi = true, kaynakAdi = dosya.name)
                }
                if (sonuc.isSuccess) return sonuc.getOrThrow()
            }
            return null
        }

        private fun sistemAdaylari(): List<File> {
            val dizin = File("/system/fonts")
            val mevcut = runCatching {
                dizin.listFiles()?.filter { it.isFile && it.name.endsWith(".ttf", true) }
            }.getOrNull().orEmpty()
            if (mevcut.isEmpty()) return emptyList()

            val oncelikli = SISTEM_ADAYLARI.mapNotNull { ad -> mevcut.firstOrNull { it.name == ad } }
            val yedekler = mevcut.filter { dosya ->
                val ad = dosya.name.lowercase(Locale.ROOT)
                "emoji" !in ad && "symbol" !in ad && "icon" !in ad && "cjk" !in ad &&
                    dosya.length() in 20_000..8_000_000
            }
            // Statik olanlar once: degisken fontlar hem buyuk hem daha az sinanmis.
            return (oncelikli + yedekler)
                .distinctBy { it.absolutePath }
                .sortedBy { if (degiskenMi(it)) 1 else 0 }
                .take(6)
        }

        /**
         * TTF tablo dizininde `fvar` (font variations) tablosu arar.
         *
         * Bicim: 0-3 sfntVersion, 4-5 numTables, ardindan 16 baytlik tablo
         * kayitlari; her kaydin ilk 4 bayti etikettir.
         */
        internal fun degiskenMi(dosya: File): Boolean = runCatching {
            RandomAccessFile(dosya, "r").use { okuyucu ->
                okuyucu.seek(4)
                val tabloSayisi = okuyucu.readUnsignedShort()
                if (tabloSayisi !in 1..512) return@use false
                val etiket = ByteArray(4)
                for (sira in 0 until tabloSayisi) {
                    okuyucu.seek(12L + sira * 16L)
                    okuyucu.readFully(etiket)
                    if (String(etiket, Charsets.US_ASCII) == "fvar") return@use true
                }
                false
            }
        }.getOrDefault(false)

        /**
         * PDFBox'ta kodlama sinamasinin genel yolu: `getStringWidth` kodlanamayan
         * bir karakter gorurse istisna firlatir. `encode` metodu korumalidir.
         */
        private fun yazilabilir(font: PDFont, metin: String): Boolean =
            runCatching { font.getStringWidth(metin) }.isSuccess
    }
}
