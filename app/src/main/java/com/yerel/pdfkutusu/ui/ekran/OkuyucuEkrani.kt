package com.yerel.pdfkutusu.ui.ekran

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yerel.pdfkutusu.okuyucu.OkuyucuMotoru
import com.yerel.pdfkutusu.ui.model.OkuyucuDurumu
import com.yerel.pdfkutusu.ui.model.OkuyucuViewModel
import com.yerel.pdfkutusu.ui.ortak.AracIskeleti
import com.yerel.pdfkutusu.ui.ortak.BosDurum
import com.yerel.pdfkutusu.ui.ortak.ParolaDiyalogu
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private const val ASGARI_YAKINLASTIRMA = 1f
private const val AZAMI_YAKINLASTIRMA = 5f

/**
 * PDF okuyucu.
 *
 * ## Akicilik nasil saglaniyor
 *
 * **Yakinlastirma goruntuyu buyutmez, sayfayi yeniden cizer.** `graphicsLayer`
 * ile olceklemek bulanik metin demektir. Bunun yerine sayfanin yerlesim
 * genisligi degisir ve [OkuyucuMotoru] o genislikte yeni bir bitmap uretir;
 * metin her yakinlastirma seviyesinde net kalir.
 *
 * **Cizim hicbir zaman beklemez.** Her sayfa once onbellekteki en iyi surumu
 * gosterir (gerekirse ucuz onizleme buyutulerek), net surum arka planda
 * gelince yerine gecer. Kaydirirken bos kutu ya da donma olmaz.
 *
 * **Parmak hareketi catismasi yok.** Yakinlastirma yalnizca **iki parmak**
 * ekrandayken olaylari tuketir; tek parmakla kaydirma dogrudan `LazyColumn`'a
 * gider ve listenin kendi geri donusum/kaydirma mekanigi bozulmaz.
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
        val acikTut = durum is OkuyucuDurumu.Hazir
        gorunumNesnesi.keepScreenOn = acikTut
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
    val listeDurumu = rememberLazyListState()
    val yatayKaydirma = rememberScrollState()
    val yogunluk = LocalDensity.current
    val kapsam = rememberCoroutineScope()

    // Yerlesime islenmis yakinlastirma: sayfa genisligini ve dolayisiyla
    // cizim cozunurlugunu belirler. Yalnizca parmak kalkinca degisir.
    var yerlesimYakinlastirma by remember(motor) { mutableFloatStateOf(1f) }

    // Parmak hareketi suresince gecerli olan gecici olcek. Bu sirada
    // YENIDEN YERLESIM YAPILMAZ; yalnizca GPU donusumu uygulanir. Her
    // karede LazyColumn'u yeniden yerlestirmek kasmanin sebebiydi.
    var hareketOlcegi by remember(motor) { mutableFloatStateOf(1f) }
    var hareketMerkezi by remember(motor) { mutableStateOf(Offset.Zero) }
    var hareketAktif by remember(motor) { mutableStateOf(false) }

    val sayfalar = remember(motor) { (0 until motor.sayfaSayisi).toList() }
    val gorunenYakinlastirma = yerlesimYakinlastirma * hareketOlcegi

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
                val gorunumGenisligiPx = with(yogunluk) { maxWidth.roundToPx() }
                val gorunumYuksekligiPx = with(yogunluk) { maxHeight.roundToPx() }
                val sayfaGenisligiPx = (gorunumGenisligiPx * yerlesimYakinlastirma).roundToInt()
                val sayfaGenisligiDp = with(yogunluk) { sayfaGenisligiPx.toDp() }

                // Gorunen aralik degisince komsu sayfalari onden ciz.
                // Parmak hareketi surerken cizim istenmez.
                LaunchedEffect(listeDurumu, sayfaGenisligiPx, hareketAktif) {
                    if (hareketAktif) return@LaunchedEffect
                    snapshotFlow {
                        listeDurumu.firstVisibleItemIndex to
                            listeDurumu.layoutInfo.visibleItemsInfo.size
                    }.collect { (ilk, adet) ->
                        if (adet > 0) {
                            gorunum.komsulariHazirla(ilk..(ilk + adet - 1), sayfaGenisligiPx)
                        }
                    }
                }

                /** Parmak kalkinca: olcegi yerlesime isle ve odagi koru. */
                fun hareketiIsle() {
                    val olcek = hareketOlcegi
                    hareketOlcegi = 1f
                    hareketAktif = false
                    if (olcek == 1f) return

                    val eski = yerlesimYakinlastirma
                    val yeni = (eski * olcek)
                        .coerceIn(ASGARI_YAKINLASTIRMA, AZAMI_YAKINLASTIRMA)
                    if (yeni == eski) return
                    val k = yeni / eski
                    yerlesimYakinlastirma = yeni

                    // Parmaklarin ortasindaki nokta ekranda ayni yerde kalsin.
                    val odakX = hareketMerkezi.x
                    val odakY = hareketMerkezi.y
                    val ilkIndeks = listeDurumu.firstVisibleItemIndex
                    val ilkOfset = listeDurumu.firstVisibleItemScrollOffset

                    kapsam.launch {
                        yatayKaydirma.scrollTo(
                            ((yatayKaydirma.value + odakX) * k - odakX)
                                .roundToInt().coerceAtLeast(0),
                        )
                        listeDurumu.scrollToItem(
                            ilkIndeks,
                            (ilkOfset * k + (k - 1f) * odakY).roundToInt().coerceAtLeast(0),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(yatayKaydirma, enabled = gorunenYakinlastirma > 1f)
                        // Yalnizca iki parmak varken olaylari tuketir; tek
                        // parmakla kaydirma LazyColumn'a dokunulmadan gider.
                        .pointerInput(motor) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                var cokParmak = false
                                var toplam = 1f
                                while (true) {
                                    val olay = awaitPointerEvent()
                                    val basili = olay.changes.count { it.pressed }
                                    if (basili == 0) break
                                    if (basili >= 2) {
                                        if (!cokParmak) {
                                            cokParmak = true
                                            hareketMerkezi = olay.calculateCentroid(true)
                                            hareketAktif = true
                                        }
                                        val degisim = olay.calculateZoom()
                                        if (degisim.isFinite() && degisim > 0f) {
                                            val altSinir = ASGARI_YAKINLASTIRMA / yerlesimYakinlastirma
                                            val ustSinir = AZAMI_YAKINLASTIRMA / yerlesimYakinlastirma
                                            toplam = (toplam * degisim).coerceIn(altSinir, ustSinir)
                                            hareketOlcegi = toplam
                                            olay.changes.forEach { it.consume() }
                                        }
                                    }
                                }
                                if (cokParmak) hareketiIsle()
                            }
                        },
                ) {
                    LazyColumn(
                        state = listeDurumu,
                        modifier = Modifier
                            .width(sayfaGenisligiDp)
                            .fillMaxHeight()
                            // Parmak hareketi suresince yalnizca GPU donusumu.
                            .graphicsLayer {
                                if (hareketOlcegi != 1f) {
                                    scaleX = hareketOlcegi
                                    scaleY = hareketOlcegi
                                    transformOrigin = TransformOrigin(
                                        (hareketMerkezi.x / gorunumGenisligiPx.toFloat())
                                            .coerceIn(0f, 1f),
                                        (hareketMerkezi.y / gorunumYuksekligiPx.toFloat())
                                            .coerceIn(0f, 1f),
                                    )
                                }
                            },
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
                    ) {
                        items(sayfalar, key = { it }) { indeks ->
                            SayfaGorunumu(
                                motor = motor,
                                indeks = indeks,
                                genislikPx = sayfaGenisligiPx,
                                cizimEtkin = !hareketAktif,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }

        AltCubuk(
            gecerliSayfa = listeDurumu.firstVisibleItemIndex + 1,
            toplamSayfa = motor.sayfaSayisi,
            yakinlastirma = gorunenYakinlastirma,
            yakinlastirmaDegistir = { istenen ->
                val yeni = istenen.coerceIn(ASGARI_YAKINLASTIRMA, AZAMI_YAKINLASTIRMA)
                val eski = yerlesimYakinlastirma
                if (yeni == eski) return@AltCubuk
                val k = yeni / eski
                val ilkIndeks = listeDurumu.firstVisibleItemIndex
                val ilkOfset = listeDurumu.firstVisibleItemScrollOffset
                yerlesimYakinlastirma = yeni
                kapsam.launch {
                    listeDurumu.scrollToItem(ilkIndeks, (ilkOfset * k).roundToInt().coerceAtLeast(0))
                }
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
    val oran = motor.oranTahmini(indeks)
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
        modifier = modifier
            .aspectRatio(1f / oran.coerceAtLeast(0.1f))
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White),
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
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
            )
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
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
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
    val kapsam = androidx.compose.runtime.rememberCoroutineScope()

    IconButton(
        enabled = !mesgul,
        onClick = {
            mesgul = true
            kapsam.launch {
                val dosya = gorunum.paylasimIcinHazirla()
                mesgul = false
                if (dosya != null) paylas(dosya) else gorunum.mesajGoster("Belge paylaşıma hazırlanamadı.")
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
                    if (dosya != null) araclardaAc(dosya) else gorunum.mesajGoster("Belge araçlara aktarılamadı.")
                }
            },
        )
    }
}
