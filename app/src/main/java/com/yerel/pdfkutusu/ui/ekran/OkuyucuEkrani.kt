package com.yerel.pdfkutusu.ui.ekran

import android.graphics.Bitmap
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yerel.pdfkutusu.okuyucu.OkuyucuMotoru
import com.yerel.pdfkutusu.ui.model.OkuyucuDurumu
import com.yerel.pdfkutusu.ui.model.OkuyucuViewModel
import com.yerel.pdfkutusu.ui.ortak.AracIskeleti
import com.yerel.pdfkutusu.ui.ortak.BosDurum
import com.yerel.pdfkutusu.ui.ortak.ParolaDiyalogu
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private const val ASGARI_YAKINLASTIRMA = 1f
private const val AZAMI_YAKINLASTIRMA = 5f

/**
 * Sayfalarin dikey yerlesimi.
 *
 * Kaydirmayi kendimiz yonettigimiz icin sayfa konumlari mutlak piksel olarak
 * burada tutulur. `LazyColumn`'un "indeks + ofset" modeliyle ugrasmak zorunda
 * kalmiyoruz; yakinlastirma odagini korumak duz bir aritmetik islemine
 * donusuyor.
 */
private class SayfaYerlesimBilgisi(
    val ustler: FloatArray,
    val yukseklikler: FloatArray,
    /** Yakinlastirmayla olceklenen kisim (sayfalarin toplam yuksekligi). */
    val icerikYuksekligi: Float,
    /** Olceklenmeyen kisim (aralar ve kenar boslugu). */
    val sabitYukseklik: Float,
) {
    val toplamYukseklik: Float get() = icerikYuksekligi + sabitYukseklik

    fun gorunurAralik(kaydirmaY: Float, gorunumYuksekligi: Float): IntRange {
        if (ustler.isEmpty()) return IntRange.EMPTY
        val alt = kaydirmaY + gorunumYuksekligi
        var bas = 0
        while (bas < ustler.size - 1 && ustler[bas] + yukseklikler[bas] < kaydirmaY) bas++
        var son = bas
        while (son < ustler.size - 1 && ustler[son + 1] < alt) son++
        // Bir onceki ve sonraki sayfayi da hazir tut.
        return (bas - 1).coerceAtLeast(0)..(son + 1).coerceAtMost(ustler.size - 1)
    }

    fun sayfaBul(icerikY: Float): Int {
        var i = 0
        while (i < ustler.size - 1 && ustler[i + 1] <= icerikY) i++
        return i
    }

    companion object {
        fun hesapla(
            motor: OkuyucuMotoru,
            sayfaGenisligi: Float,
            bosluk: Float,
            kenar: Float,
        ): SayfaYerlesimBilgisi {
            val adet = motor.sayfaSayisi
            val ustler = FloatArray(adet)
            val yukseklikler = FloatArray(adet)
            var y = kenar
            var icerik = 0f
            for (i in 0 until adet) {
                val h = sayfaGenisligi * motor.oranTahmini(i)
                ustler[i] = y
                yukseklikler[i] = h
                icerik += h
                y += h
                if (i < adet - 1) y += bosluk
            }
            val sabit = bosluk * (adet - 1).coerceAtLeast(0) + kenar * 2f
            return SayfaYerlesimBilgisi(ustler, yukseklikler, icerik, sabit)
        }
    }
}

/**
 * PDF okuyucu.
 *
 * ## Kaydirma ve yakinlastirma neden elde yazildi
 *
 * Ilk surumde sayfalar bir `LazyColumn` icindeydi ve yakinlastirma
 * `graphicsLayer` ile yapiliyordu. Iki temel sorun cikti:
 *
 *  - **Kucultmede bos alan.** Lazy liste yalnizca o anki yerlesimde gorunen
 *    ogeleri hazirlar; olcegi kucultunce acilan alanda gosterilecek hicbir
 *    sey olmuyordu.
 *  - **Odak noktasi tutmuyordu.** Lazy listenin kaydirma konumu "indeks +
 *    piksel ofseti" olarak tutuluyor; olcek degisince bunu duzeltmek
 *    asenkron yerlesimle yarisa giriyor ve sayfa atlamalarina yol aciyordu.
 *
 * Simdi kaydirma konumunu ([kaydirmaX], [kaydirmaY]) ve olcegi dogrudan biz
 * tutuyoruz. Yakinlastirma odagini korumak tek satirlik bir islem:
 *
 * ```
 * kaydirmaX = (kaydirmaX + odakX) * k - odakX
 * kaydirmaY = (kaydirmaY + odakY) * k - odakY
 * ```
 *
 * Es zamanli, yerlesimden bagimsiz ve tanim geregi dogru: parmaklarin
 * ortasindaki nokta ekranda sabit kalir. Gorunur sayfalar da her karede
 * kaydirma konumundan hesaplandigi icin kucultmede bos alan olusmaz.
 */
@Composable
fun OkuyucuEkrani(
    gorunum: OkuyucuViewModel,
    geriDon: () -> Unit,
    araclardaAc: (File) -> Unit,
    paylas: (File) -> Unit,
) {
    val durum by gorunum.durum.collectAsStateWithLifecycle()
    val mesaj by gorunum.mesaj.collectAsStateWithLifecycle()
    val anlikMesaj = remember { SnackbarHostState() }

    LaunchedEffect(mesaj) {
        val metin = mesaj
        if (metin != null) {
            anlikMesaj.showSnackbar(metin)
            gorunum.mesajiKapat()
        }
    }

    // Okurken ekran sonmesin.
    val gorunumNesnesi = LocalView.current
    DisposableEffect(durum) {
        gorunumNesnesi.keepScreenOn = durum is OkuyucuDurumu.Hazir
        onDispose { gorunumNesnesi.keepScreenOn = false }
    }

    val baslik = (durum as? OkuyucuDurumu.Hazir)?.gorunenAd ?: "Okuyucu"

    AracIskeleti(
        baslik = baslik,
        geriDon = geriDon,
        anlikMesajDurumu = anlikMesaj,
        eylemler = {
            if (durum is OkuyucuDurumu.Hazir) {
                OkuyucuEylemleri(gorunum, araclardaAc, paylas)
            }
        },
    ) { doldurma ->
        Box(Modifier.fillMaxSize().padding(doldurma)) {
            when (val anlik = durum) {
                OkuyucuDurumu.Bos, OkuyucuDurumu.Yukleniyor ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("Belge açılıyor…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                is OkuyucuDurumu.Hata ->
                    BosDurum(
                        simge = Icons.Default.ErrorOutline,
                        baslik = anlik.mesaj,
                        aciklama = anlik.oneri ?: "Başka bir dosyayla deneyin.",
                        eylemEtiketi = "Kapat",
                        eylem = geriDon,
                    )

                is OkuyucuDurumu.ParolaGerekli ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Bu belge parola korumalı.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }

                is OkuyucuDurumu.Hazir -> BelgeGorunumu(anlik, gorunum)
            }
        }
    }

    (durum as? OkuyucuDurumu.ParolaGerekli)?.let { istek ->
        ParolaDiyalogu(
            gorunenAd = istek.gorunenAd,
            hataMesaji = istek.hataMesaji,
            gonder = gorunum::parolaGonder,
            iptal = geriDon,
        )
    }
}

// ---------------------------------------------------------------------------

@Composable
private fun BelgeGorunumu(hazir: OkuyucuDurumu.Hazir, gorunum: OkuyucuViewModel) {
    val motor = hazir.motor
    val yogunluk = LocalDensity.current
    val kapsam = rememberCoroutineScope()

    var olcek by remember(motor) { mutableFloatStateOf(1f) }
    var kaydirmaX by remember(motor) { mutableFloatStateOf(0f) }
    var kaydirmaY by remember(motor) { mutableFloatStateOf(0f) }
    var hareketAktif by remember(motor) { mutableStateOf(false) }
    // Motor sayfa oranlarini cizdikce ogrenir; yerlesim bunu izleyip tazelenir.
    var olculenOran by remember(motor) { mutableIntStateOf(0) }

    // Gorunum olculeri yukari tasindi: alt cubuk da ayni geometriyi kullansin.
    var gorunumGenisligi by remember(motor) { mutableFloatStateOf(0f) }
    var gorunumYuksekligi by remember(motor) { mutableFloatStateOf(0f) }

    LaunchedEffect(motor) {
        snapshotFlow { motor.olculenOranSayisi() }.collect { olculenOran = it }
    }

    val bosluk = with(yogunluk) { 8.dp.toPx() }
    val kenar = with(yogunluk) { 8.dp.toPx() }
    val sayfaGenisligi = gorunumGenisligi * olcek

    val yerlesim = remember(motor, sayfaGenisligi, bosluk, kenar, olculenOran) {
        SayfaYerlesimBilgisi.hesapla(motor, sayfaGenisligi, bosluk, kenar)
    }

    fun azamiY() = max(0f, yerlesim.toplamYukseklik - gorunumYuksekligi)
    fun azamiX() = max(0f, sayfaGenisligi - gorunumGenisligi)

    fun sinirla() {
        kaydirmaY = kaydirmaY.coerceIn(0f, azamiY())
        kaydirmaX = kaydirmaX.coerceIn(0f, azamiX())
    }

    /**
     * Odak noktasi ekranda sabit kalacak sekilde olcegi degistirir.
     *
     * Es zamanli ve yerlesimden bagimsiz oldugu icin sapma olusmaz:
     * parmaklarin ortasindaki icerik noktasi tanim geregi yerinde kalir.
     */
    fun olcekle(carpan: Float, odak: Offset) {
        if (gorunumGenisligi <= 0f) return
        val yeni = (olcek * carpan).coerceIn(ASGARI_YAKINLASTIRMA, AZAMI_YAKINLASTIRMA)
        val k = yeni / olcek
        if (k == 1f) return
        kaydirmaX = (kaydirmaX + odak.x) * k - odak.x
        kaydirmaY = (kaydirmaY + odak.y) * k - odak.y
        olcek = yeni

        // Yeni sinirlar: sayfa yukseklikleri olcekle buyur, aralar buyumez.
        val yeniAzamiY = max(
            0f,
            yerlesim.icerikYuksekligi * k + yerlesim.sabitYukseklik - gorunumYuksekligi,
        )
        val yeniAzamiX = max(0f, gorunumGenisligi * yeni - gorunumGenisligi)
        kaydirmaY = kaydirmaY.coerceIn(0f, yeniAzamiY)
        kaydirmaX = kaydirmaX.coerceIn(0f, yeniAzamiX)
    }

    fun kaydir(sapma: Offset) {
        kaydirmaX -= sapma.x
        kaydirmaY -= sapma.y
        sinirla()
    }

    val gorunurler by remember(yerlesim, gorunumYuksekligi) {
        derivedStateOf { yerlesim.gorunurAralik(kaydirmaY, gorunumYuksekligi) }
    }

    val gecerliSayfa by remember(yerlesim, gorunumYuksekligi) {
        derivedStateOf {
            if (gorunumYuksekligi <= 0f) 1
            else yerlesim.sayfaBul(kaydirmaY + gorunumYuksekligi / 2f) + 1
        }
    }

    val sonumleme = remember { exponentialDecay<Float>(frictionMultiplier = 1.1f) }

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .onSizeChanged {
                    gorunumGenisligi = it.width.toFloat()
                    gorunumYuksekligi = it.height.toFloat()
                },
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    // Cocuklar gorunumden buyuk olabilsin (yakinlastirma).
                    .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                    .pointerInput(motor) {
                        awaitEachGesture {
                            val ilk = awaitFirstDown(
                                requireUnconsumed = false,
                                pass = PointerEventPass.Initial,
                            )
                            val hizIzleyici = VelocityTracker()
                            hizIzleyici.addPosition(ilk.uptimeMillis, ilk.position)
                            var yakinlastirdi = false

                            while (true) {
                                val olay = awaitPointerEvent(PointerEventPass.Initial)
                                val basililar = olay.changes.filter { it.pressed }
                                if (basililar.isEmpty()) break

                                if (basililar.size >= 2) {
                                    if (!yakinlastirdi) {
                                        yakinlastirdi = true
                                        hareketAktif = true
                                    }
                                    val carpan = olay.calculateZoom()
                                    val merkez = olay.calculateCentroid(useCurrent = true)
                                    if (carpan.isFinite() && carpan > 0f &&
                                        merkez != Offset.Unspecified
                                    ) {
                                        olcekle(carpan, merkez)
                                    }
                                } else {
                                    val tek = basililar.first()
                                    hizIzleyici.addPosition(tek.uptimeMillis, tek.position)
                                }

                                val sapma = olay.calculatePan()
                                if (sapma != Offset.Zero) kaydir(sapma)

                                olay.changes.forEach { if (it.positionChanged()) it.consume() }
                            }

                            hareketAktif = false

                            // Tek parmakla surukleme sonrasi savurma.
                            if (!yakinlastirdi) {
                                val hiz = hizIzleyici.calculateVelocity()
                                if (abs(hiz.y) > 80f) {
                                    val sinir = azamiY()
                                    kapsam.launch {
                                        AnimationState(
                                            initialValue = kaydirmaY,
                                            initialVelocity = -hiz.y,
                                        ).animateDecay(sonumleme) {
                                            val sinirli = value.coerceIn(0f, sinir)
                                            kaydirmaY = sinirli
                                            if (sinirli != value) cancelAnimation()
                                        }
                                    }
                                }
                            }
                        }
                    },
            ) {
                val sayfaGenisligiDp = with(yogunluk) { sayfaGenisligi.toDp() }
                if (gorunumGenisligi > 0f) for (indeks in gorunurler) {
                    key(indeks) {
                        val sayfaYuksekligiDp = with(yogunluk) {
                            yerlesim.yukseklikler[indeks].toDp()
                        }
                        SayfaGorunumu(
                            motor = motor,
                            indeks = indeks,
                            genislikPx = sayfaGenisligi.roundToInt(),
                            cizimEtkin = !hareketAktif,
                            modifier = Modifier
                                // Konum yerlesim asamasinda okunur: kaydirirken
                                // yeniden kompozisyon gerekmez.
                                .offset {
                                    IntOffset(
                                        (-kaydirmaX).roundToInt(),
                                        (yerlesim.ustler[indeks] - kaydirmaY).roundToInt(),
                                    )
                                }
                                // requiredSize: yakinlastirmada sayfa gorunumden
                                // genis olur. Modifier.size gelen kisitlara
                                // sikistirildigi icin sayfa kirpiliyordu.
                                .requiredSize(sayfaGenisligiDp, sayfaYuksekligiDp),
                        )
                    }
                }
            }

            // Gorunen sayfalarin komsularini onden ciz.
            LaunchedEffect(gorunurler, sayfaGenisligi, hareketAktif) {
                if (!hareketAktif && !gorunurler.isEmpty()) {
                    gorunum.komsulariHazirla(gorunurler, sayfaGenisligi.roundToInt())
                }
            }
        }

        AltCubuk(
            gecerliSayfa = gecerliSayfa,
            toplamSayfa = motor.sayfaSayisi,
            yakinlastirma = olcek,
            yakinlastirmaDegistir = { istenen ->
                val hedef = istenen.coerceIn(ASGARI_YAKINLASTIRMA, AZAMI_YAKINLASTIRMA)
                // Dugmeyle yakinlastirmada odak ekranin ortasi.
                olcekle(hedef / olcek, Offset(gorunumGenisligi / 2f, gorunumYuksekligi / 2f))
            },
        )
    }
}

/**
 * Tek sayfa.
 *
 * Onbellekte ne varsa **hemen** cizer; net surum arka planda gelir. Boylece
 * hizli kaydirmada bos kutu gorunmez.
 */
@Composable
private fun SayfaGorunumu(
    motor: OkuyucuMotoru,
    indeks: Int,
    genislikPx: Int,
    cizimEtkin: Boolean,
    modifier: Modifier = Modifier,
) {
    var bitmap by remember(indeks) {
        mutableStateOf<Bitmap?>(motor.onbellekten(indeks, genislikPx))
    }

    LaunchedEffect(indeks, genislikPx, cizimEtkin) {
        // Elde ne varsa hemen goster - bos kutu gorunmesin.
        motor.onbellekten(indeks, genislikPx)?.let { bitmap = it }
        // Parmak hareketi surerken yeni cizim istenmez: her ara adim icin
        // yeniden cizmek hem bosuna hem de kasmaya yol aciyordu.
        if (!cizimEtkin) return@LaunchedEffect
        motor.ciz(indeks, genislikPx)?.let { bitmap = it }
    }

    Box(
        modifier = modifier.background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        val anlik = bitmap
        if (anlik != null && !anlik.isRecycled) {
            Image(
                bitmap = anlik.asImageBitmap(),
                contentDescription = "Sayfa ${indeks + 1}",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun AltCubuk(
    gecerliSayfa: Int,
    toplamSayfa: Int,
    yakinlastirma: Float,
    yakinlastirmaDegistir: (Float) -> Unit,
) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "$gecerliSayfa / $toplamSayfa",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                "%${(yakinlastirma * 100).roundToInt()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(
                onClick = { yakinlastirmaDegistir(yakinlastirma - 0.5f) },
                enabled = yakinlastirma > ASGARI_YAKINLASTIRMA,
            ) {
                Icon(Icons.Default.ZoomOut, contentDescription = "Uzaklaştır")
            }
            IconButton(
                onClick = { yakinlastirmaDegistir(yakinlastirma + 0.5f) },
                enabled = yakinlastirma < AZAMI_YAKINLASTIRMA,
            ) {
                Icon(Icons.Default.ZoomIn, contentDescription = "Yakınlaştır")
            }
        }
    }
}

@Composable
private fun OkuyucuEylemleri(
    gorunum: OkuyucuViewModel,
    araclardaAc: (File) -> Unit,
    paylas: (File) -> Unit,
) {
    var menuAcik by remember { mutableStateOf(false) }
    var mesgul by remember { mutableStateOf(false) }
    val kapsam = rememberCoroutineScope()

    IconButton(
        enabled = !mesgul,
        onClick = {
            mesgul = true
            kapsam.launch {
                val dosya = gorunum.paylasimIcinHazirla()
                mesgul = false
                if (dosya != null) {
                    paylas(dosya)
                } else {
                    gorunum.mesajGoster("Belge paylaşıma hazırlanamadı.")
                }
            }
        },
    ) {
        Icon(Icons.Default.Share, contentDescription = "Paylaş")
    }

    IconButton(enabled = !mesgul, onClick = { menuAcik = true }) {
        Icon(Icons.Default.Build, contentDescription = "Araçlar")
    }

    DropdownMenu(expanded = menuAcik, onDismissRequest = { menuAcik = false }) {
        DropdownMenuItem(
            text = { Text("Araçlarda aç") },
            onClick = {
                menuAcik = false
                mesgul = true
                kapsam.launch {
                    val dosya = gorunum.araclaraKopyala()
                    mesgul = false
                    if (dosya != null) {
                        araclardaAc(dosya)
                    } else {
                        gorunum.mesajGoster("Belge araçlara aktarılamadı.")
                    }
                }
            },
        )
    }
}
