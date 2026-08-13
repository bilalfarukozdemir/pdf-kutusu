package com.yerel.pdfkutusu.ui.ortak

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/**
 * Surukle-birak ile yeniden siralanabilen yatay serit.
 *
 * Uzun basma ile oge "kaldirilir", yatay surukleme mesafesi kutucuk
 * genisligine bolunerek hedef konum bulunur. Erisilebilirlik icin her
 * kutucukte ayrica bir "cikar" dugmesi bulunabilir.
 *
 * Hem PDF sayfalarinda hem gorsellerde kullanilir; kutucugun icini cagiran
 * doldurur.
 */
@Composable
fun <T> SurukleBirakSeridi(
    ogeler: List<T>,
    anahtar: (Int, T) -> Any,
    tasi: (kaynak: Int, hedef: Int) -> Unit,
    altEtiket: (Int, T) -> String,
    modifier: Modifier = Modifier,
    kutucukGenisligi: Dp = 92.dp,
    aralik: Dp = 8.dp,
    seritYuksekligi: Dp = 190.dp,
    enBoyOrani: Float = 0.707f,
    cikar: ((Int) -> Unit)? = null,
    icerik: @Composable (Int, T) -> Unit,
) {
    var suruklenenKonum by remember { mutableStateOf<Int?>(null) }
    var kaydirmaX by remember { mutableFloatStateOf(0f) }

    val yogunluk = LocalDensity.current
    val adimPx = with(yogunluk) { (kutucukGenisligi + aralik).toPx() }

    LazyRow(
        modifier = modifier.fillMaxWidth().height(seritYuksekligi),
        horizontalArrangement = Arrangement.spacedBy(aralik),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(ogeler, key = { konum, oge -> anahtar(konum, oge) }) { konum, oge ->
            val suruklenen = suruklenenKonum == konum
            Column(
                modifier = Modifier
                    .width(kutucukGenisligi)
                    .zIndex(if (suruklenen) 1f else 0f)
                    .graphicsLayer {
                        if (suruklenen) {
                            translationX = kaydirmaX
                            scaleX = 1.06f
                            scaleY = 1.06f
                            shadowElevation = 12f
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(enBoyOrani)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = if (suruklenen) 2.dp else 1.dp,
                            color = if (suruklenen) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = RoundedCornerShape(6.dp),
                        )
                        .pointerInput(konum, ogeler.size) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    suruklenenKonum = konum
                                    kaydirmaX = 0f
                                },
                                onDrag = { degisim, sapma ->
                                    degisim.consume()
                                    kaydirmaX += sapma.x
                                },
                                onDragEnd = {
                                    val adim = (kaydirmaX / adimPx).roundToInt()
                                    val hedef = (konum + adim).coerceIn(0, ogeler.lastIndex)
                                    if (hedef != konum) tasi(konum, hedef)
                                    suruklenenKonum = null
                                    kaydirmaX = 0f
                                },
                                onDragCancel = {
                                    suruklenenKonum = null
                                    kaydirmaX = 0f
                                },
                            )
                        },
                ) {
                    Box(Modifier.fillMaxSize()) { icerik(konum, oge) }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(altEtiket(konum, oge), style = MaterialTheme.typography.labelSmall)
                    if (cikar != null) {
                        IconButton(
                            onClick = { cikar(konum) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Çıkar",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
