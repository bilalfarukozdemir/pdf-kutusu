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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yerel.pdfkutusu.okuyucu.OkuyucuMotoru
import com.yerel.pdfkutusu.okuyucu.SayfaYerlesimBilgisi
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
        SayfaYerlesimBilgisi.hesapla(
            sayfaSayisi = motor.sayfaSayisi,
            sayfaGenisligi = sayfaGenisligi,
            bosluk = bosluk,
            kenar = kenar,
            oran = motor::oranTahmini,
        )
    }

    // ONEMLI: pointerInput yalnizca `motor` degisince yeniden kurulur, yani
    // icindeki lambda ILK kompozisyondaki degerleri yakalar. Yerlesimi
    // dogrudan yakalarsak parmak hareketi sonsuza dek olcek=1 zamanindaki
    // geometriyle hesap yapar; azamiX() hep 0 cikar ve yatay kaydirma hic
    // calismaz. Guncel referansi State uzerinden okuyoruz.
    val guncelYerlesim by rememberUpdatedState(yerlesim)

    /** Canli olcekten turetilir; yakalanan eski deger kullanilmaz. */
    fun sayfaGenisligiCanli() = gorunumGenisligi * olcek

    fun azamiY() = guncelYerlesim.azamiKaydirma(gorunumYuksekligi)
    fun azamiX() = max(0f, sayfaGenisligiCanli() - gorunumGenisligi)

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

        // Yatayda sabit bosluk yok; sayfa gorunum genisligini tamamen doldurur.
        kaydirmaX = (kaydirmaX + odak.x) * k - odak.x
        // Dikeyde var: aralar ve kenar boslugu yakinlastirmayla buyumez.
        kaydirmaY = guncelYerlesim.olcekliKonum(kaydirmaY + odak.y, k) - odak.y
        olcek = yeni

        val yeniAzamiY = guncelYerlesim.olcekliAzamiKaydirma(k, gorunumYuksekligi)
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

    // Yerlesim islevi indeksle erisim istiyor; araligi bir kez listeye ceviriyoruz.
    val gorunenler = remember(gorunurler) { gorunurler.toList() }

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
            // Sayfalari hazir bir kapsayiciya birakmayip elle yerlestiriyoruz.
            //
            // Yakinlastirilan sayfa gorunumden genis olur ve bu, hazir
            // kapsayicilarla iki ayri tuzaga yol acti. Ikisi de olculdu:
            //
            //  - `Box` + `requiredSize`: cocuk gelen kisittan buyuk oldugunda
            //    `Placeable` onu "kirpilmis" sayar ve tasan kismi ortalar.
            //    Olculen sapma tam olarak (gorunum - sayfa) / 2 idi; sayfa
            //    sonuna kadar saga kaydirildiginda sag kenari ekranin
            //    ortasinda kaliyor, geri kalani gri zemin olarak goruluyordu.
            //  - `wrapContentSize(unbounded = true)`: yukaridakini duzeltiyor
            //    ama kapsayicinin boyutunu cocuklara bagliyor. Belge yeni
            //    acildiginda ilk karede cocuk yok (gorunum genisligi henuz
            //    olculmedi) ve kutuya hicbir dokunma olayi ulasmiyordu.
            //
            // Kendi `Layout`umuzda sayfayi tam istedigimiz olcude olcup tam
            // istedigimiz noktaya koyuyoruz; kapsayici ise her zaman gorunum
            // boyutunda kaliyor, yani dokunma alani da hep dogru.
            Layout(
                modifier = Modifier
                    .fillMaxSize()
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
                content = {
                    if (gorunumGenisligi > 0f) {
                        for (indeks in gorunenler) {
                            key(indeks) {
                                SayfaGorunumu(
                                    motor = motor,
                                    indeks = indeks,
                                    genislikPx = sayfaGenisligi.roundToInt(),
                                    cizimEtkin = !hareketAktif,
                                )
                            }
                        }
                    }
                },
            ) { olculebilirler, kisitlar ->
                val en = sayfaGenisligi.roundToInt().coerceAtLeast(0)
                val sayfalar = olculebilirler.mapIndexed { sira, olculebilir ->
                    val boy = yerlesim.yukseklikler[gorunenler[sira]]
                        .roundToInt().coerceAtLeast(0)
                    olculebilir.measure(Constraints.fixed(en, boy))
                }
                layout(kisitlar.maxWidth, kisitlar.maxHeight) {
                    // Kaydirma konumu burada okunuyor. Yerlesim asamasindaki
                    // okuma yeniden kompozisyon tetiklemedigi icin parmak
                    // hareketi tek bir yerlestirme adimina iniyor.
                    sayfalar.forEachIndexed { sira, sayfa ->
                        sayfa.place(
                            x = (-kaydirmaX).roundToInt(),
                            y = (yerlesim.ustler[gorunenler[sira]] - kaydirmaY)
                                .roundToInt(),
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

