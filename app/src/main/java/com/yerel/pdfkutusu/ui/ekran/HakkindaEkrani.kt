package com.yerel.pdfkutusu.ui.ekran

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yerel.pdfkutusu.R
import com.yerel.pdfkutusu.ui.ortak.AracIskeleti
import com.yerel.pdfkutusu.ui.ortak.BaglantiMetni

@Composable
fun HakkindaEkrani(geriDon: () -> Unit) {
    AracIskeleti(baslik = stringResource(R.string.hakkinda_baslik), geriDon = geriDon) { doldurma ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(doldurma)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            UygulamaKimligi()

            DilSecimBolumu()

            Bolum(
                baslik = stringResource(R.string.hakkinda_risk_baslik),
                govde = stringResource(R.string.hakkinda_risk_govde),
                vurgulu = true,
            )

            Bolum(
                baslik = stringResource(R.string.hakkinda_izinler_baslik),
                govde = stringResource(R.string.hakkinda_izinler_govde),
            )

            Bolum(
                baslik = stringResource(R.string.hakkinda_veri_baslik),
                govde = stringResource(R.string.hakkinda_veri_govde),
            )

            Bolum(
                baslik = stringResource(R.string.hakkinda_orijinal_baslik),
                govde = stringResource(R.string.hakkinda_orijinal_govde),
            )

            Bolum(
                baslik = stringResource(R.string.hakkinda_exif_baslik),
                govde = stringResource(R.string.hakkinda_exif_govde),
            )

            Bolum(
                baslik = stringResource(R.string.hakkinda_kutuphane_baslik),
                govde = stringResource(R.string.hakkinda_kutuphane_govde),
            )

            Bolum(
                baslik = stringResource(R.string.hakkinda_yazitipi_baslik),
                govde = stringResource(R.string.hakkinda_yazitipi_govde),
            )

            Bolum(
                baslik = stringResource(R.string.hakkinda_bilerek_yapilmayan_baslik),
                govde = stringResource(R.string.hakkinda_bilerek_yapilmayan_govde),
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Uygulama kimligi basligi: ad, surum ve yapimci.
 *
 * Metin secilebilir birakildi (kopyalanabilsin), ama tiklanabilir bir baglanti
 * degil: bu uygulama hicbir sekilde disari cikis yapmaz, tarayici da acmaz.
 */
@Composable
private fun UygulamaKimligi() {
    val baglam = LocalContext.current
    val surum = remember(baglam) {
        runCatching {
            baglam.packageManager.getPackageInfo(baglam.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.hakkinda_uygulama_adi), style = MaterialTheme.typography.headlineSmall)
            if (surum.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.hakkinda_surum, surum), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.hakkinda_yapimci_etiket),
                style = MaterialTheme.typography.labelMedium,
            )
            BaglantiMetni(
                metin = "vitrincim.com",
                adres = "https://vitrincim.com",
                stil = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                renk = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.offset(x = (-4).dp),
            )
            Text(
                stringResource(R.string.hakkinda_yapimci_govde),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/**
 * Dil secici: Sistem / Turkce / English.
 *
 * Android 13+ (API 33) `LocaleManager.setApplicationLocales` kullanir - bu,
 * AppCompat gibi buyuk bir bagimlilik eklemeden per-app dil degistirme saglayan
 * yerlesik API'dir. minSdk 26 oldugu icin daha eski cihazlarda secici
 * calismaz; onun yerine bilgilendirici bir not gosterilir. Sistem dili
 * tespiti (values-en/ qualifier) her surumde zaten otomatik calisir, bu
 * yalnizca manuel override'i sinirlar.
 *
 * Secim, sistemin kendi "Uygulama dilleri" mekanizmasi tarafindan kalici
 * olarak saklanir; burada ayrica bir tercih dosyasi tutulmasina gerek yoktur.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DilSecimBolumu() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.hakkinda_dil_baslik), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(10.dp))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val baglam = LocalContext.current
                val yonetici = remember(baglam) { baglam.getSystemService(LocaleManager::class.java) }

                fun gecerliKod(): String {
                    val etiketler = yonetici?.applicationLocales?.toLanguageTags().orEmpty()
                    return when {
                        etiketler.isEmpty() -> ""
                        etiketler.startsWith("tr") -> "tr"
                        etiketler.startsWith("en") -> "en"
                        else -> ""
                    }
                }

                var secili by remember(baglam) { mutableStateOf(gecerliKod()) }

                val secenekler = listOf(
                    "" to stringResource(R.string.hakkinda_dil_sistem),
                    "tr" to stringResource(R.string.hakkinda_dil_turkce),
                    "en" to stringResource(R.string.hakkinda_dil_ingilizce),
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    secenekler.forEachIndexed { indeks, (kod, etiket) ->
                        SegmentedButton(
                            selected = secili == kod,
                            onClick = {
                                secili = kod
                                yonetici?.applicationLocales = if (kod.isEmpty()) {
                                    LocaleList.getEmptyLocaleList()
                                } else {
                                    LocaleList.forLanguageTags(kod)
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = indeks, count = secenekler.size),
                        ) {
                            Text(etiket)
                        }
                    }
                }
            } else {
                Text(
                    stringResource(R.string.hakkinda_dil_eski_cihaz_bilgi),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Bolum(baslik: String, govde: String, vurgulu: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (vurgulu) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(baslik, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                govde,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = if (govde.contains("__")) FontFamily.Monospace else FontFamily.Default,
                ),
            )
        }
    }
}
