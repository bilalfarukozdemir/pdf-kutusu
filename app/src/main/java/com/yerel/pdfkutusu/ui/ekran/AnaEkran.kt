package com.yerel.pdfkutusu.ui.ekran

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import com.yerel.pdfkutusu.R
import com.yerel.pdfkutusu.depo.SonAcilanBelge
import com.yerel.pdfkutusu.ui.ortak.AracIskeleti
import com.yerel.pdfkutusu.ui.ortak.BaglantiMetni
import com.yerel.pdfkutusu.ui.ortak.bicimliZaman

data class AracKarti(
    val rota: String,
    @StringRes val baslikRes: Int,
    @StringRes val aciklamaRes: Int,
    val simge: ImageVector,
)

val ARACLAR = listOf(
    AracKarti(Rotalar.RESIMDEN_PDF, R.string.ana_arac_resimden_pdf_baslik, R.string.ana_arac_resimden_pdf_aciklama, Icons.Default.PhotoLibrary),
    AracKarti(Rotalar.BIRLESTIR, R.string.ana_arac_birlestir_baslik, R.string.ana_arac_birlestir_aciklama, Icons.Default.MergeType),
    AracKarti(Rotalar.BOL, R.string.ana_arac_bol_baslik, R.string.ana_arac_bol_aciklama, Icons.Default.ContentCut),
    AracKarti(Rotalar.SIRALA, R.string.ana_arac_sirala_baslik, R.string.ana_arac_sirala_aciklama, Icons.Default.SwapVert),
    AracKarti(Rotalar.DONDUR, R.string.ana_arac_dondur_baslik, R.string.ana_arac_dondur_aciklama, Icons.Default.Rotate90DegreesCw),
    AracKarti(Rotalar.SIKISTIR, R.string.ana_arac_sikistir_baslik, R.string.ana_arac_sikistir_aciklama, Icons.Default.Compress),
    AracKarti(Rotalar.FILIGRAN, R.string.ana_arac_filigran_baslik, R.string.ana_arac_filigran_aciklama, Icons.Default.BrandingWatermark),
    AracKarti(Rotalar.KARART, R.string.ana_arac_karart_baslik, R.string.ana_arac_karart_aciklama, Icons.Default.Block),
    AracKarti(Rotalar.OCR, R.string.ana_arac_ocr_baslik, R.string.ana_arac_ocr_aciklama, Icons.Default.TextFields),
)

@Composable
fun AnaEkran(
    gecis: (String) -> Unit,
    bekleyenBelge: String? = null,
    bekleyeniBirak: () -> Unit = {},
    sonAcilanlar: List<SonAcilanBelge> = emptyList(),
    pdfAc: () -> Unit = {},
    sonAcilaniAc: (SonAcilanBelge) -> Unit = {},
    sonAcilaniSil: (SonAcilanBelge) -> Unit = {},
    sonAcilanlariTemizle: () -> Unit = {},
) {
    AracIskeleti(
        baslik = stringResource(R.string.ana_baslik),
        eylemler = {
            IconButton(onClick = { gecis(Rotalar.HAKKINDA) }) {
                Icon(Icons.Default.Info, contentDescription = stringResource(R.string.ana_hakkinda_cd))
            }
        },
    ) { doldurma ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(doldurma),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { CevrimdisiSeridi() }

            if (bekleyenBelge != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    BekleyenBelgeSeridi(bekleyenBelge, bekleyeniBirak)
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SonAcilanlarSeridi(
                    belgeler = sonAcilanlar,
                    pdfAc = pdfAc,
                    ac = sonAcilaniAc,
                    sil = sonAcilaniSil,
                    temizle = sonAcilanlariTemizle,
                )
            }

            items(ARACLAR, key = { it.rota }) { arac ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .clickable { gecis(arac.rota) },
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Icon(
                            arac.simge,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(arac.baslikRes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(arac.aciklamaRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    YanKart(
                        baslik = stringResource(R.string.ana_dosyalar_baslik),
                        aciklama = stringResource(R.string.ana_dosyalar_aciklama),
                        simge = Icons.Default.Folder,
                        modifier = Modifier.weight(1f),
                    ) { gecis(Rotalar.DOSYALAR) }
                    YanKart(
                        baslik = stringResource(R.string.ana_gunluk_baslik),
                        aciklama = stringResource(R.string.ana_gunluk_aciklama),
                        simge = Icons.Default.History,
                        modifier = Modifier.weight(1f),
                    ) { gecis(Rotalar.GUNLUK) }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.ana_yapimci_etiket),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    BaglantiMetni(
                        metin = "vitrincim.com",
                        adres = "https://vitrincim.com",
                        stil = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun YanKart(
    baslik: String,
    aciklama: String,
    simge: ImageVector,
    modifier: Modifier = Modifier,
    tikla: () -> Unit,
) {
    Card(
        modifier = modifier.height(92.dp).clickable(onClick = tikla),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(simge, contentDescription = null)
            Spacer(Modifier.height(6.dp))
            Text(baslik, style = MaterialTheme.typography.titleSmall)
            Text(aciklama, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * Son acilan belgeler.
 *
 * Yatay kaydirilir: liste 20 belgeye kadar cikabiliyor, dikeyde bu kadar yer
 * ayirmak arac izgarasini asagi itip ana ekrani bozardi.
 *
 * Liste bos olsa bile serit gorunur - "PDF aç" dugmesi burada ve baska yerde
 * yok. Uygulamanin icinden bir belge acmanin tek yolu bu.
 */
@Composable
private fun SonAcilanlarSeridi(
    belgeler: List<SonAcilanBelge>,
    pdfAc: () -> Unit,
    ac: (SonAcilanBelge) -> Unit,
    sil: (SonAcilanBelge) -> Unit,
    temizle: () -> Unit,
) {
    var temizlemeSorusu by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(top = 6.dp, bottom = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.ana_son_acilanlar_baslik),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = pdfAc) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.ana_pdf_ac))
                }
                if (belgeler.isNotEmpty()) {
                    IconButton(onClick = { temizlemeSorusu = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.ana_listeyi_temizle_cd))
                    }
                }
            }

            if (belgeler.isEmpty()) {
                Text(
                    stringResource(R.string.ana_son_acilanlar_bos),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
            } else {
                LazyRow(
                    modifier = Modifier.padding(top = 8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 14.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(belgeler, key = { it.uri }) { belge ->
                        SonAcilanKart(belge, ac = { ac(belge) }, sil = { sil(belge) })
                    }
                }
            }
        }
    }

    if (temizlemeSorusu) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { temizlemeSorusu = false },
            title = { Text(stringResource(R.string.ana_listeyi_temizle_dialog_baslik)) },
            text = {
                Text(stringResource(R.string.ana_listeyi_temizle_dialog_govde))
            },
            confirmButton = {
                TextButton(onClick = {
                    temizle()
                    temizlemeSorusu = false
                }) { Text(stringResource(R.string.ana_temizle)) }
            },
            dismissButton = {
                TextButton(onClick = { temizlemeSorusu = false }) { Text(stringResource(R.string.ortak_vazgec)) }
            },
        )
    }
}

@Composable
private fun SonAcilanKart(belge: SonAcilanBelge, ac: () -> Unit, sil: () -> Unit) {
    Card(
        modifier = Modifier.width(164.dp).clickable(onClick = ac),
        // Kapsayici kartla ayni tonda kalirsa cipin sinirlari secilmiyor;
        // cihazda bakinca tek bir blok gibi gorunuyordu.
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(Modifier.padding(start = 12.dp, top = 4.dp, end = 4.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = sil, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.ortak_listeden_cikar_cd),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(
                belge.ad,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            if (belge.kalici) {
                Text(
                    bicimliZaman(belge.zaman),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            } else {
                // Baska bir uygulamadan gelen belgede yetki gecicidir; bunu
                // pesinen soyluyoruz ki dokunup hata almasin.
                Text(
                    stringResource(R.string.ana_gecici_erisim),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Okuyucudan devredilen belge. Kullanici birden fazla araca sokabilsin diye
 * bir arac secildiginde kaybolmaz; ancak burada acikca birakilir.
 */
@Composable
private fun BekleyenBelgeSeridi(ad: String, birak: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Description, contentDescription = null, Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ana_araclara_hazir), style = MaterialTheme.typography.titleSmall)
                Text(
                    ad,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
                Text(
                    stringResource(R.string.ana_bir_arac_secin),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            IconButton(onClick = birak) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.ana_belgeyi_birak_cd))
            }
        }
    }
}

@Composable
private fun CevrimdisiSeridi() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.CloudOff, contentDescription = null, Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.ana_tamamen_cevrimdisi), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.ana_cevrimdisi_aciklama),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Ilk acilisda gosterilen uyari.
 *
 * Kullanicinin bu araci ne icin kullanip ne icin kullanmamasi gerektigini
 * pesinen soyluyoruz.
 */
@Composable
fun IlkAcilisUyarisi(onayla: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { /* Okunmadan gecilemez. */ },
        icon = { Icon(Icons.Default.Info, contentDescription = null) },
        title = { Text(stringResource(R.string.ana_ilk_acilis_baslik)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.ana_ilk_acilis_govde1),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.ana_ilk_acilis_govde2),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = { Button(onClick = onayla) { Text(stringResource(R.string.ana_ilk_acilis_anladim)) } },
    )
}
