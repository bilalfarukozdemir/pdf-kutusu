package com.yerel.pdfkutusu

import com.yerel.pdfkutusu.okuyucu.SayfaYerlesimBilgisi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Okuyucunun kaydirma/yakinlastirma aritmetigi.
 *
 * Bu testlerin varlik sebebi kullanicinin bildirdigi iki somut hata:
 *
 *  1. "Buyutup kucultunce sayfalarda 5-6 sayfa ileri geri gidiyorum."
 *     Sinir hesabi eski olcege gore yapiliyordu; yakinlastirilmis konum
 *     kucuk sinira kistirilinca belgenin ortasina firlatiyordu.
 *  2. "Sayfanin sol kismina yapisik yaklasiyor, saga sola kaydiramiyorum."
 *     Ayni kok: yatay sinir hep 0 hesaplaniyordu.
 *
 * Ikisi de saf aritmetik; cihaza kurup parmakla denemeden yakalanabilirdi.
 * Artik yakalanabiliyor.
 */
class OkuyucuYerlesimTesti {

    private val bosluk = 21f
    private val kenar = 21f
    private val gorunumGenisligi = 1080f
    private val gorunumYuksekligi = 2200f

    /** Karisik yonlu bir belge: sabit oran varsayimlarina yaslanmayalim. */
    private val oranlar = floatArrayOf(
        1.414f, 1.414f, 0.707f, 1.414f, 1.294f,
        1.414f, 1.414f, 1.414f, 0.707f, 1.414f,
        1.414f, 1.414f, 1.414f, 1.414f, 1.414f,
        1.414f, 0.707f, 1.414f, 1.414f, 1.414f,
    )

    private fun duzen(olcek: Float, adet: Int = oranlar.size) =
        SayfaYerlesimBilgisi.hesapla(
            sayfaSayisi = adet,
            sayfaGenisligi = gorunumGenisligi * olcek,
            bosluk = bosluk,
            kenar = kenar,
            oran = { oranlar[it % oranlar.size] },
        )

    /** [olcekle] fonksiyonunun ekrandaki karsiligi. */
    private fun yakinlastir(
        yerlesim: SayfaYerlesimBilgisi,
        kaydirmaY: Float,
        odakY: Float,
        k: Float,
    ): Float = yerlesim.olcekliKonum(kaydirmaY + odakY, k) - odakY

    // ---- Temel yerlesim ----

    @Test
    fun `sayfa ustleri bosluklarla birlikte dizilir`() {
        val y = duzen(1f)
        assertEquals(kenar, y.ustler[0], 0.01f)
        for (i in 1 until y.sayfaSayisi) {
            val beklenen = y.ustler[i - 1] + y.yukseklikler[i - 1] + bosluk
            assertEquals("Sayfa $i yanlis yerde", beklenen, y.ustler[i], 0.01f)
        }
    }

    @Test
    fun `toplam yukseklik degisen ve sabit paylardan olusur`() {
        val y = duzen(1f)
        val sayfalarinToplami = y.yukseklikler.sum()
        assertEquals(sayfalarinToplami, y.icerikYuksekligi, 0.05f)
        assertEquals(bosluk * (y.sayfaSayisi - 1) + kenar * 2f, y.sabitYukseklik, 0.01f)
        assertEquals(y.icerikYuksekligi + y.sabitYukseklik, y.toplamYukseklik, 0.01f)
    }

    @Test
    fun `sadece sayfa yukseklikleri olcekle buyur`() {
        val bir = duzen(1f)
        val uc = duzen(3f)
        assertEquals(bir.icerikYuksekligi * 3f, uc.icerikYuksekligi, 0.5f)
        // Aralar buyumez; bu ayrim odak hesabinin dayanagi.
        assertEquals(bir.sabitYukseklik, uc.sabitYukseklik, 0.01f)
    }

    // ---- Odak noktasinin korunmasi ----

    @Test
    fun `olcekliKonum sayfa ustunu gercek yerlesimle ayni yere tasir`() {
        val bir = duzen(1f)
        for (k in floatArrayOf(0.5f, 1.5f, 2f, 3f, 5f)) {
            val hedef = duzen(k)
            for (i in 0 until bir.sayfaSayisi) {
                assertEquals(
                    "Olcek $k, sayfa $i",
                    hedef.ustler[i],
                    bir.olcekliKonum(bir.ustler[i], k),
                    0.5f,
                )
            }
        }
    }

    @Test
    fun `yakinlastirinca odaktaki belge noktasi ekranda yerinde kalir`() {
        for (baslangicKaydirma in floatArrayOf(0f, 5_000f, 18_000f, 28_000f)) {
            for (odakY in floatArrayOf(0f, 550f, 1_100f, 2_199f)) {
                for (k in floatArrayOf(1.2f, 2f, 3.5f)) {
                    val bir = duzen(1f)
                    val belgeY = baslangicKaydirma + odakY
                    val sayfa = bir.sayfaBul(belgeY)
                    val icOran = (belgeY - bir.ustler[sayfa]) / bir.yukseklikler[sayfa]

                    val yeniKaydirma = yakinlastir(bir, baslangicKaydirma, odakY, k)
                    val hedef = duzen(k)
                    val yeniBelgeY =
                        hedef.ustler[sayfa] + icOran * hedef.yukseklikler[sayfa]

                    assertEquals(
                        "kaydirma=$baslangicKaydirma odak=$odakY k=$k",
                        odakY,
                        yeniBelgeY - yeniKaydirma,
                        1f,
                    )
                }
            }
        }
    }

    @Test
    fun `uzaklastirinca da odaktaki nokta yerinde kalir`() {
        val ucBuc = duzen(3.5f)
        val kaydirma = 60_000f
        val odakY = 900f
        val belgeY = kaydirma + odakY
        val sayfa = ucBuc.sayfaBul(belgeY)
        val icOran = (belgeY - ucBuc.ustler[sayfa]) / ucBuc.yukseklikler[sayfa]

        val k = 1f / 3.5f
        val yeniKaydirma = yakinlastir(ucBuc, kaydirma, odakY, k)
        val hedef = duzen(1f)
        val yeniBelgeY = hedef.ustler[sayfa] + icOran * hedef.yukseklikler[sayfa]

        assertEquals(odakY, yeniBelgeY - yeniKaydirma, 1f)
    }

    /**
     * Gercek bir parmak hareketi tek bir buyuk adim degil, kucuk adimlarin
     * toplamidir. Adim basina kalan minik sapma birikirse kullanici sayfalar
     * arasinda suruklenir; kullanicinin gordugu tam olarak buydu.
     */
    @Test
    fun `kucuk adimlarin toplami sapma biriktirmez`() {
        var olcek = 1f
        var kaydirma = 18_000f
        val odakY = 940f
        var yerlesim = duzen(olcek)

        val belgeY = kaydirma + odakY
        val sayfa = yerlesim.sayfaBul(belgeY)
        val icOran = (belgeY - yerlesim.ustler[sayfa]) / yerlesim.yukseklikler[sayfa]

        repeat(40) {
            val k = 1.03f
            kaydirma = yakinlastir(yerlesim, kaydirma, odakY, k)
            olcek *= k
            yerlesim = duzen(olcek)
        }
        repeat(40) {
            val k = 1f / 1.03f
            kaydirma = yakinlastir(yerlesim, kaydirma, odakY, k)
            olcek *= k
            yerlesim = duzen(olcek)
        }

        assertEquals("Olcek basa donmeliydi", 1f, olcek, 0.001f)
        val sonBelgeY = yerlesim.ustler[sayfa] + icOran * yerlesim.yukseklikler[sayfa]
        assertEquals("Odak kaydi", odakY, sonBelgeY - kaydirma, 2f)
        assertEquals("Kaydirma konumu basa donmedi", 18_000f, kaydirma, 2f)
    }

    // ---- Sinirlar ----

    /**
     * Isinlanma hatasinin dogrudan testi: parmak hareketinin ortasinda,
     * yerlesim henuz yeniden hesaplanmadan tahmin ettigimiz sinir, gercek
     * yerlesimin siniriyla ayni olmali.
     */
    @Test
    fun `tahmini sinir gercek yerlesimin siniriyla ayni`() {
        val bir = duzen(1f)
        for (k in floatArrayOf(0.5f, 1f, 1.7f, 2f, 3f, 5f)) {
            assertEquals(
                "Olcek $k",
                duzen(k).azamiKaydirma(gorunumYuksekligi),
                bir.olcekliAzamiKaydirma(k, gorunumYuksekligi),
                0.5f,
            )
        }
    }

    @Test
    fun `sinir olcekle birlikte buyur`() {
        val bir = duzen(1f).azamiKaydirma(gorunumYuksekligi)
        val iki = duzen(2f).azamiKaydirma(gorunumYuksekligi)
        assertTrue("Yakinlastirinca sinir buyumeliydi: $bir -> $iki", iki > bir * 1.8f)
    }

    @Test
    fun `belge gorunume sigiyorsa kaydirma yok`() {
        val tek = duzen(0.5f, adet = 1)
        assertEquals(0f, tek.azamiKaydirma(gorunumYuksekligi), 0.01f)
    }

    @Test
    fun `sinir tam olarak son sayfanin altini gosterir`() {
        val y = duzen(2f)
        val sinir = y.azamiKaydirma(gorunumYuksekligi)
        val sonAlt = y.ustler.last() + y.yukseklikler.last() + kenar
        assertEquals(sonAlt, sinir + gorunumYuksekligi, 0.5f)
    }

    // ---- Sayfaya gitme ----

    @Test
    fun `sayfaya gidince o sayfa gorunumun basinda durur`() {
        val y = duzen(1f)
        for (indeks in 0 until y.sayfaSayisi) {
            val kaydirma = y.sayfaBasiKaydirmasi(indeks, gorunumYuksekligi)
            if (kaydirma < y.azamiKaydirma(gorunumYuksekligi)) {
                // Sinira dayanmadiysa sayfa tam olarak kenar boslugu kadar
                // asagida baslamali - belgenin en basiyla ayni cerceve.
                assertEquals("Sayfa $indeks", kenar, y.ustler[indeks] - kaydirma, 0.5f)
            }
        }
    }

    @Test
    fun `ilk sayfaya gitmek belgenin basina goturur`() {
        assertEquals(0f, duzen(1f).sayfaBasiKaydirmasi(0, gorunumYuksekligi), 0.01f)
    }

    @Test
    fun `son sayfaya gitmek siniri asmaz`() {
        val y = duzen(1f)
        val kaydirma = y.sayfaBasiKaydirmasi(y.sayfaSayisi - 1, gorunumYuksekligi)
        assertEquals(y.azamiKaydirma(gorunumYuksekligi), kaydirma, 0.01f)
    }

    @Test
    fun `yakinlastirilmisken de dogru sayfaya gider`() {
        val y = duzen(3f)
        val kaydirma = y.sayfaBasiKaydirmasi(11, gorunumYuksekligi)
        // Sayfanin ustunde kenar boslugu kalir; hemen altindaki icerik
        // gercekten 11. sayfa olmali.
        assertEquals(kenar, y.ustler[11] - kaydirma, 0.5f)
        assertEquals(11, y.sayfaBul(kaydirma + kenar + 1f))
    }

    @Test
    fun `aralik disi numara kistirilir`() {
        val y = duzen(1f)
        assertEquals(
            y.sayfaBasiKaydirmasi(0, gorunumYuksekligi),
            y.sayfaBasiKaydirmasi(-5, gorunumYuksekligi),
            0.01f,
        )
        assertEquals(
            y.sayfaBasiKaydirmasi(y.sayfaSayisi - 1, gorunumYuksekligi),
            y.sayfaBasiKaydirmasi(9_999, gorunumYuksekligi),
            0.01f,
        )
        // Sayfasiz belgede de cokmemeli.
        assertEquals(0f, duzen(1f, adet = 0).sayfaBasiKaydirmasi(3, gorunumYuksekligi), 0.01f)
    }

    @Test
    fun `belge gorunume sigiyorsa sayfaya gitmek yerinden oynatmaz`() {
        val tek = duzen(0.5f, adet = 1)
        assertEquals(0f, tek.sayfaBasiKaydirmasi(0, gorunumYuksekligi), 0.01f)
    }

    // ---- Gorunur aralik ve sayfa bulma ----

    @Test
    fun `gorunur aralik komsu sayfalari da icerir`() {
        val y = duzen(1f)
        val ortaSayfa = 6
        val aralik = y.gorunurAralik(y.ustler[ortaSayfa], gorunumYuksekligi)
        assertTrue("Onceki sayfa hazirlanmali", aralik.first <= ortaSayfa - 1)
        assertTrue("Sonraki sayfa hazirlanmali", aralik.last >= ortaSayfa + 1)
    }

    @Test
    fun `gorunur aralik belge sinirlarini asmaz`() {
        val y = duzen(1f)
        val bas = y.gorunurAralik(0f, gorunumYuksekligi)
        assertEquals(0, bas.first)

        val son = y.gorunurAralik(y.azamiKaydirma(gorunumYuksekligi), gorunumYuksekligi)
        assertEquals(y.sayfaSayisi - 1, son.last)

        // Sinirin otesi istense bile indeks tasmamali.
        val tasan = y.gorunurAralik(y.toplamYukseklik * 3f, gorunumYuksekligi)
        assertTrue(tasan.first >= 0 && tasan.last <= y.sayfaSayisi - 1)
    }

    @Test
    fun `sayfaBul sayfa sinirlarinda dogru sonuc verir`() {
        val y = duzen(1f)
        assertEquals(0, y.sayfaBul(-500f))
        assertEquals(0, y.sayfaBul(0f))
        for (i in 0 until y.sayfaSayisi) {
            assertEquals("Sayfa $i ustu", i, y.sayfaBul(y.ustler[i]))
            assertEquals("Sayfa $i ortasi", i, y.sayfaBul(y.ustler[i] + y.yukseklikler[i] / 2f))
        }
        assertEquals(y.sayfaSayisi - 1, y.sayfaBul(y.toplamYukseklik * 5f))
    }

    // ---- Bozuk girdiler ----

    @Test
    fun `sayfasiz belge cokmez`() {
        val y = duzen(1f, adet = 0)
        assertEquals(0, y.sayfaSayisi)
        assertEquals(IntRange.EMPTY, y.gorunurAralik(0f, gorunumYuksekligi))
        assertEquals(0, y.sayfaBul(100f))
        assertEquals(0f, y.azamiKaydirma(gorunumYuksekligi), 0.01f)
        assertEquals(0f, y.icerikYuksekligi, 0.01f)
    }

    @Test
    fun `tek sayfada ara boslugu eklenmez`() {
        val y = duzen(1f, adet = 1)
        assertEquals(kenar * 2f, y.sabitYukseklik, 0.01f)
        assertEquals(kenar, y.ustler[0], 0.01f)
    }

    @Test
    fun `olculmemis genislikte cokmez`() {
        val y = SayfaYerlesimBilgisi.hesapla(5, 0f, bosluk, kenar) { 1.414f }
        assertEquals(0f, y.icerikYuksekligi, 0.01f)
        assertEquals(0f, y.azamiKaydirma(gorunumYuksekligi), 0.01f)
        assertEquals(0, y.sayfaBul(0f))
        // Genislik olculmeden yakinlastirma denenirse de patlamamali.
        assertTrue(y.olcekliKonum(0f, 2f).isFinite())
    }

    @Test
    fun `negatif oran yuksekligi sifirlar`() {
        val y = SayfaYerlesimBilgisi.hesapla(3, 1000f, bosluk, kenar) { -2f }
        assertTrue("Negatif yukseklik kalmamali", y.yukseklikler.all { it >= 0f })
        assertEquals(0f, y.icerikYuksekligi, 0.01f)
    }
}
