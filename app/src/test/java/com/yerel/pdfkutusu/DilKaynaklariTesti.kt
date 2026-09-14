package com.yerel.pdfkutusu

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.w3c.dom.Element

/**
 * Uygulamanin hangi telefon dilinde hangi dilde acildigi ve iki dil dosyasinin
 * birbirinden kopmamasi.
 *
 * Android telefonun dili icin ceviri bulamazsa etiketsiz values/ klasorunu
 * kullanir. O klasor Ingilizce olmali: Turkce oldugunda Hintce, Portekizce gibi
 * dillerdeki telefonlarda uygulama Turkce aciliyordu.
 */
@RunWith(RobolectricTestRunner::class)
class DilKaynaklariTesti {

    private fun metin(anahtar: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(anahtar)

    private fun ingilizceAcildi() {
        assertEquals("PDF Box", metin(R.string.uygulama_adi))
        assertEquals("Save", metin(R.string.ortak_kaydet))
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun `ingilizce telefonda ingilizce acilir`() {
        ingilizceAcildi()
    }

    @Test
    @Config(qualifiers = "tr-rTR")
    fun `turkce telefonda turkce acilir`() {
        assertEquals("PDF Kutusu", metin(R.string.uygulama_adi))
        assertEquals("Kaydet", metin(R.string.ortak_kaydet))
    }

    @Test
    @Config(qualifiers = "hi-rIN")
    fun `cevirisi olmayan hintce telefonda ingilizce acilir`() {
        ingilizceAcildi()
    }

    @Test
    @Config(qualifiers = "pt-rBR")
    fun `cevirisi olmayan portekizce telefonda ingilizce acilir`() {
        ingilizceAcildi()
    }

    @Test
    @Config(qualifiers = "de-rDE")
    fun `cevirisi olmayan almanca telefonda ingilizce acilir`() {
        ingilizceAcildi()
    }

    // Tek dosyada kalan anahtar, o dosyanin dili disindaki telefonlarda ya
    // Resources.NotFoundException ile coker (varsayilanda yoksa) ya da sessizce
    // Ingilizce kalir (Turkcede yoksa).
    @Test
    fun `her anahtar iki dil dosyasinda da var`() {
        val ingilizce = kaynaklar(VARSAYILAN)
        val turkce = kaynaklar(TURKCE)
        val yalnizIngilizce = ingilizce.keys - turkce.keys
        val yalnizTurkce = turkce.keys - ingilizce.keys
        assertTrue(
            "Yalniz values/: $yalnizIngilizce, yalniz values-tr/: $yalnizTurkce",
            yalnizIngilizce.isEmpty() && yalnizTurkce.isEmpty(),
        )
    }

    // Yer tutucu farki yalnizca o dilde calisma zamaninda cokme demektir
    // (MissingFormatArgumentException); derleme bunu yakalamaz.
    @Test
    fun `yer tutucular iki dilde ayni`() {
        val ingilizce = kaynaklar(VARSAYILAN)
        val turkce = kaynaklar(TURKCE)
        // Desen hic eslesmezse iki taraf da bos liste olur ve test bos yere gecer.
        assertTrue(
            "Hicbir metinde yer tutucu bulunamadi; desen bozuk olabilir",
            ingilizce.values.count { YER_TUTUCU.containsMatchIn(it) } > 10,
        )
        val farkli = ingilizce.keys.intersect(turkce.keys).filter {
            yerTutucular(ingilizce.getValue(it)) != yerTutucular(turkce.getValue(it))
        }
        assertTrue("Yer tutuculari farkli anahtarlar: $farkli", farkli.isEmpty())
    }

    private fun kaynaklar(dosya: File): Map<String, String> {
        assertTrue("Bulunamadi: ${dosya.absolutePath}", dosya.isFile)
        val kok = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(dosya).documentElement
        val dugumler = kok.childNodes
        val sonuc = buildMap<String, String> {
            for (i in 0 until dugumler.length) {
                val eleman = dugumler.item(i) as? Element ?: continue
                put("${eleman.tagName}:${eleman.getAttribute("name")}", eleman.textContent)
            }
        }
        // Ayristirma bozulup dosyayi bos okursa karsilastirmalar bos yere gecmesin.
        assertTrue("${dosya.path} neredeyse bos okundu: ${sonuc.size} kayit", sonuc.size > 100)
        return sonuc
    }

    private fun yerTutucular(metin: String): List<String> =
        YER_TUTUCU.findAll(metin).map { it.value }.sorted().toList()

    private companion object {
        // Gradle birim testleri modul klasorunde (app/) calisir.
        val VARSAYILAN = File("src/main/res/values/strings.xml")
        val TURKCE = File("src/main/res/values-tr/strings.xml")
        val YER_TUTUCU = Regex("""%(\d+\$)?[sdf]""")
    }
}
