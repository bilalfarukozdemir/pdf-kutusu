package com.yerel.pdfkutusu

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.yerel.pdfkutusu.pdf.FiligranAyarlari
import com.yerel.pdfkutusu.pdf.PdfFiligranci
import com.yerel.pdfkutusu.pdf.PdfMetinCikarici
import com.yerel.pdfkutusu.test.TestPdfUretici
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Filigranda Turkce karakterlerin bozulmadigini ve gomulu yazi tipinin
 * **alt kume** olarak gomuldugunu cihaz uzerinde olcer.
 *
 * Bu test bir teshis araci olarak yazildi: cihazin `/system/fonts` icerigi
 * ureticiden ureticiye degisiyor ve secilen yazi tipinin hem Turkce'yi
 * tasidigini hem de ciktiyi sismedigini kanitlamak gerekiyor.
 */
@RunWith(AndroidJUnit4::class)
class YaziTipiTesti {

    private lateinit var baglam: Context
    private lateinit var dizin: File

    @Before
    fun hazirla() {
        baglam = ApplicationProvider.getApplicationContext()
        PDFBoxResourceLoader.init(baglam)
        dizin = File(baglam.cacheDir, "font-testi").apply {
            deleteRecursively()
            mkdirs()
        }
    }

    @Test
    fun filigrandaTurkceKarakterlerBozulmazVeAltKumeGomulur() {
        val kaynak = TestPdfUretici.olustur(File(dizin, "kaynak.pdf"), listOf("SAYFA BIR"))
        val filigransizBoyut = kaynak.length()

        val metin = "ĞÜŞİÖÇ ğüşıöç"
        val cikti = File(dizin, "filigranli.pdf")

        val sonuc = PdfFiligranci.uygula(
            kaynak = kaynak,
            ayarlar = FiligranAyarlari(metin = metin),
            cikti = cikti,
        )

        val fark = cikti.length() - filigransizBoyut
        val ciktiMetni = PdfMetinCikarici.cikar(cikti)

        // Filigran 45 derece donduruldugu icin her glif farkli bir taban
        // cizgisine dusuyor ve PDFTextStripper metni satirlara boluyor.
        // Karakterlerin korunup korunmadigini olcerken bosluklari yok sayiyoruz.
        val bosluksuzCikti = ciktiMetni.filterNot { it.isWhitespace() }
        val bosluksuzBeklenen = metin.filterNot { it.isWhitespace() }

        // Teshis: instrumentation ciktisi guvenilir sekilde yakalanmadigi icin
        // tum olculeri hata mesajina gomuyoruz.
        val teshis = buildString {
            append("[TESHIS] font=").append(sonuc.kullanilanYaziTipi)
            append(" | tamTurkce=").append(sonuc.tamTurkceDestegi)
            append(" | filigransiz=").append(filigransizBoyut)
            append(" | filigranli=").append(cikti.length())
            append(" | fark=").append(fark)
            append(" | cikanMetin=[").append(ciktiMetni.replace("\n", "/").trim()).append(']')
            append(" | beklenen=[").append(metin).append(']')
        }

        assertTrue(
            "Yazi tipi tam Turkce desteklemiyor. $teshis",
            sonuc.tamTurkceDestegi,
        )
        assertTrue(
            "Filigran metnindeki Turkce karakterler ciktida korunmamis. $teshis",
            bosluksuzCikti.contains(bosluksuzBeklenen),
        )
        assertTrue(
            "Yazi tipi alt kume olarak gomulmemis gorunuyor. $teshis",
            fark < 100_000,
        )
    }
}
