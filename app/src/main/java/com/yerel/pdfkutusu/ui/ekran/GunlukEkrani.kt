package com.yerel.pdfkutusu.ui.ekran

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yerel.pdfkutusu.R
import com.yerel.pdfkutusu.cekirdek.IslemTuru
import com.yerel.pdfkutusu.ui.model.GunlukViewModel
import com.yerel.pdfkutusu.ui.ortak.AracIskeleti
import com.yerel.pdfkutusu.ui.ortak.BosDurum
import com.yerel.pdfkutusu.ui.ortak.bicimliZaman
import com.yerel.pdfkutusu.ui.ortak.kisaOzet
import com.yerel.pdfkutusu.veri.IslemKaydi

@Composable
fun GunlukEkrani(gorunum: GunlukViewModel, geriDon: () -> Unit) {
    val kayitlar by gorunum.kayitlar.collectAsStateWithLifecycle()
    val bilgi by gorunum.bilgi.collectAsStateWithLifecycle()
    val dokum by gorunum.dokumDosyasi.collectAsStateWithLifecycle()
    val anlikMesaj = remember { SnackbarHostState() }
    var temizlemeSorusu by remember { mutableStateOf(false) }

    val kaydedici = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { hedef ->
        val kaynak = dokum
        if (hedef != null && kaynak != null) gorunum.disaAktar(kaynak, hedef)
    }

    LaunchedEffect(bilgi) {
        val mesaj = bilgi
        if (mesaj != null) {
            anlikMesaj.showSnackbar(mesaj)
            gorunum.bilgiyiKapat()
        }
    }
    LaunchedEffect(dokum) {
        val dosya = dokum
        if (dosya != null) kaydedici.launch(dosya.name)
    }

    AracIskeleti(
        baslik = stringResource(R.string.gunluk_baslik),
        geriDon = geriDon,
        anlikMesajDurumu = anlikMesaj,
        eylemler = {
            if (kayitlar.isNotEmpty()) {
                IconButton(onClick = gorunum::dokumHazirla) {
                    Icon(Icons.Default.Save, contentDescription = stringResource(R.string.gunluk_dokum_disa_aktar_cd))
                }
                IconButton(onClick = { temizlemeSorusu = true }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.gunluk_tumunu_temizle_cd))
                }
            }
        },
    ) { doldurma ->
        if (kayitlar.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(doldurma)) {
                BosDurum(
                    simge = Icons.Default.History,
                    baslik = stringResource(R.string.gunluk_bos_baslik),
                    aciklama = stringResource(R.string.gunluk_bos_aciklama),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(doldurma),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        stringResource(R.string.gunluk_kayit_sayisi_govde, kayitlar.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(kayitlar, key = { it.kimlik }) { kayit -> KayitKarti(kayit) }
            }
        }
    }

    if (temizlemeSorusu) {
        AlertDialog(
            onDismissRequest = { temizlemeSorusu = false },
            title = { Text(stringResource(R.string.gunluk_temizle_dialog_baslik)) },
            text = {
                Text(stringResource(R.string.gunluk_temizle_dialog_govde, kayitlar.size))
            },
            confirmButton = {
                Button(onClick = {
                    gorunum.tumunuTemizle()
                    temizlemeSorusu = false
                }) { Text(stringResource(R.string.gunluk_tumunu_sil)) }
            },
            dismissButton = {
                TextButton(onClick = { temizlemeSorusu = false }) { Text(stringResource(R.string.ortak_vazgec)) }
            },
        )
    }
}

@Composable
private fun KayitKarti(kayit: IslemKaydi) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (kayit.basariliMi) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.ErrorOutline
                    },
                    contentDescription = null,
                    tint = if (kayit.basariliMi) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    IslemTuru.adindan(kayit.islemTuru)?.let { stringResource(it.etiketRes) } ?: kayit.islemTuru,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    bicimliZaman(kayit.zamanDamgasi),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.padding(4.dp))
            AlanSatiri(stringResource(R.string.gunluk_alan_girdi), kayit.girdiDosyaAdi)
            AlanSatiri(stringResource(R.string.gunluk_alan_girdi_sha), kisaOzet(kayit.girdiSha256))
            if (kayit.ciktiDosyaAdi != null) AlanSatiri(stringResource(R.string.gunluk_alan_cikti), kayit.ciktiDosyaAdi)
            AlanSatiri(stringResource(R.string.gunluk_alan_cikti_sha), kisaOzet(kayit.ciktiSha256))
            AlanSatiri(stringResource(R.string.gunluk_alan_sayfa_sayisi), kayit.sayfaSayisi?.toString() ?: "-")
            if (!kayit.basariliMi && kayit.hataMesaji != null) {
                Spacer(Modifier.padding(2.dp))
                Text(
                    kayit.hataMesaji,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun AlanSatiri(etiket: String, deger: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            etiket,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp),
        )
        Text(
            deger,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.weight(1f),
        )
    }
}
