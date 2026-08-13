package com.yerel.pdfkutusu.ui.ortak

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.yerel.pdfkutusu.onizleme.OnizlemeDeposu
import com.yerel.pdfkutusu.pdf.KarartmaAlani
import java.io.File
import kotlin.math.max
import kotlin.math.min

private const val A4_ORANI = 0.707f

/**
 * Tek sayfa kucuk resmi.
 *
 * Ayri bir goruntuleme kutuphanesi yok; Android'in `PdfRenderer` motoru
 * [OnizlemeDeposu] uzerinden kullaniliyor.
 */
@Composable
fun SayfaKucukResmi(
    dosya: File,
    sayfaIndeksi: Int,
    onizleme: OnizlemeDeposu,
    modifier: Modifier = Modifier,
    hedefGenislikPx: Int = 360,
) {
    val bitmap by produceState<Bitmap?>(null, dosya.absolutePath, sayfaIndeksi, hedefGenislikPx) {
        value = onizleme.kucukResim(dosya, sayfaIndeksi, hedefGenislikPx)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val anlik = bitmap
        if (anlik == null) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Image(
                bitmap = anlik.asImageBitmap(),
                contentDescription = "Sayfa ${sayfaIndeksi + 1} önizlemesi",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Gorsel kucuk resmi.
 *
 * EXIF yonu uygulanmis halini gosterir - onizleme ile cikti ayni kod
 * yolundan gectigi icin kullanicinin gordugu yon, PDF'e giren yondur.
 */
@Composable
fun GorselKucukResmi(
    dosya: File,
    onizleme: com.yerel.pdfkutusu.onizleme.GorselOnizlemeDeposu,
    modifier: Modifier = Modifier,
    hedefKenarPx: Int = 240,
) {
    val bitmap by produceState<Bitmap?>(null, dosya.absolutePath, hedefKenarPx) {
        value = onizleme.kucukResim(dosya, hedefKenarPx)
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val anlik = bitmap
        if (anlik == null) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Image(
                bitmap = anlik.asImageBitmap(),
                contentDescription = dosya.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Sayfa secmek icin yatay kucuk resim seridi. */
@Composable
fun SayfaSeridi(
    dosya: File,
    sayfaIndeksleri: List<Int>,
    seciliIndeks: Int,
    onizleme: OnizlemeDeposu,
    sec: (Int) -> Unit,
    modifier: Modifier = Modifier,
    etiketUret: (Int) -> String = { "${it + 1}" },
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
    ) {
        itemsIndexed(sayfaIndeksleri, key = { konum, indeks -> "$konum-$indeks" }) { _, indeks ->
            val secili = indeks == seciliIndeks
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .aspectRatio(A4_ORANI)
                        .border(
                            width = if (secili) 2.dp else 1.dp,
                            color = if (secili) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = RoundedCornerShape(6.dp),
                        )
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { sec(indeks) },
                ) {
                    SayfaKucukResmi(dosya, indeks, onizleme, Modifier.fillMaxSize(), 200)
                }
                Text(
                    etiketUret(indeks),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (secili) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

/**
 * Karartma tuvali: sayfa onizlemesi uzerinde parmakla dikdortgen cizme.
 *
 * Koordinatlar `0..1` araliginda normalize edilerek disari verilir; boylece
 * onizleme cozunurlugu ile ciktinin render cozunurlugu birbirinden bagimsiz
 * kalir.
 *
 * Buradaki siyah dikdortgenler yalnizca **secim gostergesidir**. Gercek
 * karartma [com.yerel.pdfkutusu.pdf.PdfKartici] icinde, sayfa piksellerine
 * uygulanir.
 */
@Composable
fun KarartmaTuvali(
    dosya: File,
    sayfaIndeksi: Int,
    alanlar: List<KarartmaAlani>,
    onizleme: OnizlemeDeposu,
    alanEkle: (sol: Float, ust: Float, sag: Float, alt: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bitmap by produceState<Bitmap?>(null, dosya.absolutePath, sayfaIndeksi) {
        value = onizleme.kucukResim(dosya, sayfaIndeksi, 1200)
    }

    var baslangic by remember(sayfaIndeksi) { mutableStateOf<Offset?>(null) }
    var suanki by remember(sayfaIndeksi) { mutableStateOf<Offset?>(null) }

    val anlik = bitmap
    val oran = if (anlik != null && anlik.height > 0) {
        anlik.width.toFloat() / anlik.height
    } else {
        A4_ORANI
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(oran)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (anlik == null) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
            return@Box
        }

        Image(
            bitmap = anlik.asImageBitmap(),
            contentDescription = "Sayfa ${sayfaIndeksi + 1}",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )

        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(sayfaIndeksi, dosya.absolutePath) {
                    detectDragGestures(
                        onDragStart = { nokta ->
                            baslangic = nokta
                            suanki = nokta
                        },
                        onDrag = { degisim, _ ->
                            degisim.consume()
                            suanki = degisim.position
                        },
                        onDragEnd = {
                            val bas = baslangic
                            val son = suanki
                            if (bas != null && son != null && size.width > 0 && size.height > 0) {
                                val en = size.width.toFloat()
                                val boy = size.height.toFloat()
                                alanEkle(
                                    (min(bas.x, son.x) / en).coerceIn(0f, 1f),
                                    (min(bas.y, son.y) / boy).coerceIn(0f, 1f),
                                    (max(bas.x, son.x) / en).coerceIn(0f, 1f),
                                    (max(bas.y, son.y) / boy).coerceIn(0f, 1f),
                                )
                            }
                            baslangic = null
                            suanki = null
                        },
                        onDragCancel = {
                            baslangic = null
                            suanki = null
                        },
                    )
                },
        ) {
            // Onaylanmis alanlar: opak siyah, ciktida da boyle gorunecek.
            alanlar.forEach { alan ->
                val duzgun = alan.duzelt()
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(duzgun.sol * size.width, duzgun.ust * size.height),
                    size = Size(
                        (duzgun.sag - duzgun.sol) * size.width,
                        (duzgun.alt - duzgun.ust) * size.height,
                    ),
                )
            }

            // Cizilmekte olan alan: kesikli degil, yari saydam onizleme.
            val bas = baslangic
            val son = suanki
            if (bas != null && son != null) {
                val solUst = Offset(min(bas.x, son.x), min(bas.y, son.y))
                val boyut = Size(
                    kotlin.math.abs(son.x - bas.x),
                    kotlin.math.abs(son.y - bas.y),
                )
                drawRect(color = Color.Black.copy(alpha = 0.55f), topLeft = solUst, size = boyut)
                drawRect(
                    color = Color.White,
                    topLeft = solUst,
                    size = boyut,
                    style = Stroke(width = 2f),
                )
            }
        }
    }
}

/** Sayfa izgarasi (siralama ekrani icin). */
@Composable
fun SayfaKutucugu(
    dosya: File,
    sayfaIndeksi: Int,
    etiket: String,
    onizleme: OnizlemeDeposu,
    modifier: Modifier = Modifier,
    altBilgi: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(A4_ORANI)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(6.dp),
                ),
        ) {
            SayfaKucukResmi(dosya, sayfaIndeksi, onizleme, Modifier.fillMaxSize(), 240)
        }
        Text(etiket, style = MaterialTheme.typography.labelSmall)
        altBilgi()
    }
}
