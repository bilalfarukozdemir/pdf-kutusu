package com.yerel.pdfkutusu

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.yerel.pdfkutusu.okuyucu.BelgeKaynagi
import com.yerel.pdfkutusu.okuyucu.OkuyucuMotoru
import com.yerel.pdfkutusu.pdf.BelgeErisimi
import com.yerel.pdfkutusu.test.TestPdfUretici
import java.io.File
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Okuyucunun dayanikliligi ve cizim davranisi.
 *
 * Varsayilan PDF okuyucu olmak, "kullanicinin bilerek sectigi dosya"
 * varsayimini ortadan kaldirir: belge e-postadan, sohbetten, buluttan
 * gelebilir; bozuk, bos, sifreli ya da hic PDF olmayabilir. Bu testler
 * bu durumlarin **hicbirinde cokmedigini** ve kullaniciya anlamli bir mesaj
 * dondugunu dogrular.
 */
@RunWith(AndroidJUnit4::class)
class OkuyucuCihazTesti {

    private lateinit var baglam: Context
    private lateinit var dizin: File
    private lateinit var geciciler: File
    private val acilanMotorlar = mutableListOf<OkuyucuMotoru>()

    @Before
    fun hazirla() {
        baglam = ApplicationProvider.getApplicationContext()
        PDFBoxResourceLoader.init(baglam)
        dizin = File(baglam.cacheDir, "okuyucu-testi").apply {
            deleteRecursively()
            mkdirs()
        }
        geciciler = File(dizin, "gecici").apply { mkdirs() }
    }

    @After
    fun topla() {
        acilanMotorlar.forEach { runCatching { it.close() } }
        acilanMotorlar.clear()
    }

    private fun motorAc(dosya: File): OkuyucuMotoru =
        OkuyucuMotoru.ac(dosya).also { acilanMotorlar.add(it) }

    private fun pdf(ad: String, sayfaSayisi: Int): File = TestPdfUretici.olustur(
        File(dizin, ad),
        (1..sayfaSayisi).map { "SAYFA $it" },
    )

    // =====================================================================
    // BelgeKaynagi — hata dayanikliligi
    // =====================================================================

    @Test
    fun normalPdfAcilir() = runBlocking {
        val kaynak = pdf("normal.pdf", 3)
        val sonuc = BelgeKaynagi.coz(baglam, Uri.fromFile(kaynak), null, geciciler)

        assertTrue("Beklenen Hazir, gelen: $sonuc", sonuc is BelgeKaynagi.Sonuc.Hazir)
        val hazir = sonuc as BelgeKaynagi.Sonuc.Hazir
        assertEquals("normal.pdf", hazir.gorunenAd)
        assertEquals(3, motorAc(hazir.dosya).sayfaSayisi)
    }

    @Test
    fun bozukDosyaCokmezAnlamliHataDoner() = runBlocking {
        val bozuk = File(dizin, "bozuk.pdf").apply {
            writeBytes(ByteArray(8192) { (it % 251).toByte() })
        }
        val sonuc = BelgeKaynagi.coz(baglam, Uri.fromFile(bozuk), null, geciciler)

        assertTrue("Beklenen Hata, gelen: $sonuc", sonuc is BelgeKaynagi.Sonuc.Hata)
        assertTrue((sonuc as BelgeKaynagi.Sonuc.Hata).mesaj.isNotBlank())
    }

    @Test
    fun bosDosyaAnlamliHataDoner() = runBlocking {
        val bos = File(dizin, "bos.pdf").apply { writeBytes(ByteArray(0)) }
        val sonuc = BelgeKaynagi.coz(baglam, Uri.fromFile(bos), null, geciciler)

        assertTrue(sonuc is BelgeKaynagi.Sonuc.Hata)
        assertTrue((sonuc as BelgeKaynagi.Sonuc.Hata).mesaj.contains("boş", ignoreCase = true))
    }

    @Test
    fun pdfOlmayanDosyaHataDoner() = runBlocking {
        val metin = File(dizin, "aslinda-metin.pdf").apply {
            writeText("Bu bir PDF değil, düz metin.")
        }
        val sonuc = BelgeKaynagi.coz(baglam, Uri.fromFile(metin), null, geciciler)
        assertTrue(sonuc is BelgeKaynagi.Sonuc.Hata)
    }

    @Test
    fun olmayanDosyaCokmez() = runBlocking {
        val yok = File(dizin, "hic-yok.pdf")
        val sonuc = BelgeKaynagi.coz(baglam, Uri.fromFile(yok), null, geciciler)
        assertTrue("Beklenen Hata, gelen: $sonuc", sonuc is BelgeKaynagi.Sonuc.Hata)
    }

    @Test
    fun sifreliBelgeParolaIster() = runBlocking {
        val sifreli = sifreliPdfUret("sifreli.pdf", "gizli123")
        val sonuc = BelgeKaynagi.coz(baglam, Uri.fromFile(sifreli), null, geciciler)

        assertTrue("Beklenen ParolaGerekli, gelen: $sonuc", sonuc is BelgeKaynagi.Sonuc.ParolaGerekli)
    }

    @Test
    fun dogruParolaBelgeyiAcar() = runBlocking {
        val sifreli = sifreliPdfUret("sifreli2.pdf", "gizli123")
        val sonuc = BelgeKaynagi.coz(baglam, Uri.fromFile(sifreli), "gizli123", geciciler)

        assertTrue("Beklenen Hazir, gelen: $sonuc", sonuc is BelgeKaynagi.Sonuc.Hazir)
        val hazir = sonuc as BelgeKaynagi.Sonuc.Hazir
        assertTrue("Cozulmus kopya gecici olmali", hazir.gecici)
        assertTrue(hazir.sifresiCozuldu)
        assertEquals(2, motorAc(hazir.dosya).sayfaSayisi)
    }

    @Test
    fun yanlisParolaAnlamliHataDoner() = runBlocking {
        val sifreli = sifreliPdfUret("sifreli3.pdf", "gizli123")
        val sonuc = BelgeKaynagi.coz(baglam, Uri.fromFile(sifreli), "yanlis", geciciler)

        assertTrue("Beklenen Hata, gelen: $sonuc", sonuc is BelgeKaynagi.Sonuc.Hata)
        assertTrue((sonuc as BelgeKaynagi.Sonuc.Hata).mesaj.contains("Parola", ignoreCase = true))
    }

    // =====================================================================
    // OkuyucuMotoru — cizim ve onbellek
    // =====================================================================

    @Test
    fun sayfalarCizilir() = runBlocking {
        val motor = motorAc(pdf("ciz.pdf", 5))
        assertEquals(5, motor.sayfaSayisi)

        val bitmap = motor.ciz(0, 720)
        assertNotNull("Sayfa cizilemedi", bitmap)
        assertTrue(bitmap!!.width >= 720)
        assertTrue(bitmap.height > bitmap.width) // A4 dikey
    }

    @Test
    fun onbellekDoluOlmadanNullDonerSonraDolar() = runBlocking {
        val motor = motorAc(pdf("onbellek.pdf", 2))

        assertNull("Cizim oncesi onbellek bos olmali", motor.onbellekten(0, 720))
        motor.ciz(0, 720)
        assertNotNull("Cizim sonrasi onbellekte olmali", motor.onbellekten(0, 720))
    }

    @Test
    fun yakinKalitedeIstekAyniBitmapiKullanir() = runBlocking {
        val motor = motorAc(pdf("kova.pdf", 1))

        // Ayni kovaya dusen iki istek ayni nesneyi dondurmeli; yakinlastirma
        // sirasinda her piksel degisiminde yeniden cizim olmamasinin sebebi bu.
        val ilk = motor.ciz(0, 600)
        val ikinci = motor.ciz(0, 640)
        assertNotNull(ilk)
        assertTrue("Kovalama calismiyor, yeniden cizim yapildi", ilk === ikinci)
    }

    @Test
    fun dusukCozunurlukIstenirseUcuzSurumGelir() = runBlocking {
        val motor = motorAc(pdf("ucuz.pdf", 1))
        val ucuz = motor.ciz(0, OkuyucuMotoru.ONIZLEME_GENISLIGI)
        assertNotNull(ucuz)
        assertTrue(ucuz!!.width <= OkuyucuMotoru.ONIZLEME_GENISLIGI + OkuyucuMotoru.KOVA_ADIMI)
    }

    @Test
    fun ucuzSurumVarkenNetSurumIstenirseHemenBirSeyDoner() = runBlocking {
        val motor = motorAc(pdf("katman.pdf", 1))
        motor.ciz(0, OkuyucuMotoru.ONIZLEME_GENISLIGI)

        // Net surum henuz yokken bile gosterilecek bir sey bulunmali:
        // kaydirirken bos kutu gorunmemesinin sebebi bu.
        val eldeki = motor.onbellekten(0, 2048)
        assertNotNull("Dusuk cozunurluklu yedek bulunamadi", eldeki)
    }

    @Test
    fun sinirDisiSayfaNullDoner() = runBlocking {
        val motor = motorAc(pdf("sinir.pdf", 2))
        assertNull(motor.ciz(-1, 720))
        assertNull(motor.ciz(99, 720))
        assertNull(motor.onbellekten(99, 720))
    }

    @Test
    fun kapatmaTekrarliCagrildaGuvenli() = runBlocking {
        val motor = OkuyucuMotoru.ac(pdf("kapat.pdf", 1))
        motor.ciz(0, 480)
        motor.close()
        motor.close() // ikinci kez cokmemeli
        assertNull("Kapandiktan sonra cizim yapilmamali", motor.ciz(0, 480))
    }

    @Test
    fun hedefGenislikSinirlariAsmaz() {
        assertEquals(
            OkuyucuMotoru.AZAMI_GENISLIK,
            OkuyucuMotoru.hedefGenislik(2000, 10f),
        )
        assertTrue(OkuyucuMotoru.hedefGenislik(100, 0.1f) >= OkuyucuMotoru.ONIZLEME_GENISLIGI)
    }

    // =====================================================================
    // Akicilik olcumu
    // =====================================================================

    @Test
    fun onbellektenOkumaAnlikOlmali() = runBlocking {
        val motor = motorAc(pdf("hiz.pdf", 10))
        val genislik = 1080

        val ucuzCizim = measureTimeMillis { motor.ciz(1, OkuyucuMotoru.ONIZLEME_GENISLIGI) }
        val ilkCizim = measureTimeMillis { motor.ciz(0, genislik) }
        val ikinciCizim = measureTimeMillis { motor.ciz(0, genislik) } // onbellekten
        val onbellekten = measureTimeMillis {
            repeat(120) { motor.onbellekten(0, genislik) }
        }
        val ardisik = measureTimeMillis {
            for (indeks in 2 until 10) motor.ciz(indeks, genislik)
        }

        olcumYaz(
            buildString {
                appendLine("--- okuyucu cizim olcumleri (A4, cihaz uzerinde) ---")
                appendLine("ucuz surum (256 px)          : $ucuzCizim ms")
                appendLine("ilk tam cizim (1080 px)      : $ilkCizim ms")
                appendLine("ayni sayfa yeniden (onbellek): $ikinciCizim ms")
                appendLine("onbellekten 120 okuma        : $onbellekten ms")
                appendLine("8 sayfa ardisik (1080 px)    : $ardisik ms")
                appendLine("  -> sayfa basina ortalama   : ${ardisik / 8} ms")
            },
        )

        // Onbellekten okuma Compose'un her karede cagirabilecegi kadar ucuz
        // olmali: 120 cagri tek bir karenin butcesinin (16 ms) altinda.
        assertTrue(
            "Onbellekten 120 okuma $onbellekten ms surdu, cok yavas",
            onbellekten < 16,
        )
        assertTrue("Ilk cizim $ilkCizim ms surdu", ilkCizim < 5_000)
        assertTrue("Onbellekten cizim $ikinciCizim ms surdu", ikinciCizim < 50)
    }

    /** Olcumleri dosyaya yazar; `adb pull` ile alinip raporlanabilsin. */
    private fun olcumYaz(metin: String) {
        runCatching {
            val hedef = File(
                baglam.getExternalFilesDir(null) ?: baglam.filesDir,
                "okuyucu-olcumleri.txt",
            )
            hedef.writeText(metin)
        }
    }

    @Test
    fun cokSayfaliBelgeCokmedenCizilir() = runBlocking {
        val motor = motorAc(pdf("uzun.pdf", 60))
        assertEquals(60, motor.sayfaSayisi)

        // Ucuz katman: tum sayfalar. Bellek patlamamali.
        for (indeks in 0 until motor.sayfaSayisi) {
            assertNotNull("Sayfa $indeks cizilemedi", motor.ciz(indeks, OkuyucuMotoru.ONIZLEME_GENISLIGI))
        }
        // Ardindan birkac sayfa tam cozunurlukte.
        for (indeks in 0 until 5) {
            assertNotNull(motor.ciz(indeks, 1440))
        }
    }

    // =====================================================================

    private fun sifreliPdfUret(ad: String, parola: String): File {
        val duz = TestPdfUretici.olustur(File(dizin, "duz_$ad"), listOf("BIR", "IKI"))
        val hedef = File(dizin, ad)
        BelgeErisimi.ac(duz).use { belge ->
            belge.protect(
                StandardProtectionPolicy(parola, parola, AccessPermission()),
            )
            belge.save(hedef)
        }
        return hedef
    }
}
