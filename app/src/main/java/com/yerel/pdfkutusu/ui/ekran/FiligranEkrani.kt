package com.yerel.pdfkutusu.ui.ekran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yerel.pdfkutusu.R
import com.yerel.pdfkutusu.pdf.FiligranRengi
import com.yerel.pdfkutusu.ui.model.FiligranViewModel
import com.yerel.pdfkutusu.ui.ortak.AralikGirisi
import kotlin.math.roundToInt

@Composable
fun FiligranEkrani(gorunum: FiligranViewModel, geriDon: () -> Unit) {
    val durum by gorunum.durum.collectAsStateWithLifecycle()
    val ayarlar by gorunum.ayarlar.collectAsStateWithLifecycle()

    AracGovdesi(
        baslik = stringResource(R.string.filigran_baslik),
        bosBaslik = stringResource(R.string.filigran_bos_baslik),
        bosAciklama = stringResource(R.string.filigran_bos_aciklama),
        simge = Icons.Default.BrandingWatermark,
        gorunum = gorunum,
        geriDon = geriDon,
        calistirEtiketi = stringResource(R.string.filigran_calistir),
        calistirEtkin = durum.girdiler.isNotEmpty() && ayarlar.metin.isNotBlank(),
        calistir = gorunum::uygula,
        secenekler = {
            val girdi = durum.ilkGirdi
            if (girdi != null) {
                SecenekKarti(stringResource(R.string.filigran_metin_baslik)) {
                    OutlinedTextField(
                        value = ayarlar.metin,
                        onValueChange = gorunum::metinDegistir,
                        label = { Text(stringResource(R.string.filigran_metin_etiket)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text(stringResource(R.string.filigran_metin_aciklama))
                        },
                    )
                }

                SecenekKarti(stringResource(R.string.filigran_gorunum_baslik)) {
                    Text(stringResource(R.string.filigran_punto, ayarlar.punto.roundToInt()))
                    Slider(
                        value = ayarlar.punto,
                        onValueChange = gorunum::puntoDegistir,
                        valueRange = 12f..140f,
                    )

                    Text(stringResource(R.string.filigran_saydamlik, (ayarlar.saydamlik * 100).roundToInt()))
                    Slider(
                        value = ayarlar.saydamlik,
                        onValueChange = gorunum::saydamlikDegistir,
                        valueRange = 0.05f..1f,
                    )

                    Text(stringResource(R.string.filigran_aci, ayarlar.aci.roundToInt()))
                    Slider(
                        value = ayarlar.aci,
                        onValueChange = gorunum::aciDegistir,
                        valueRange = 0f..90f,
                    )

                    Text(stringResource(R.string.filigran_renk), style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FiligranRengi.entries.forEach { renk ->
                            FilterChip(
                                selected = ayarlar.renk == renk,
                                onClick = { gorunum.renkDegistir(renk) },
                                label = { Text(stringResource(renk.etiketRes)) },
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.filigran_dose_baslik), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                stringResource(R.string.filigran_dose_aciklama),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(checked = ayarlar.doseme, onCheckedChange = gorunum::dosemeDegistir)
                    }
                }

                SecenekKarti(stringResource(R.string.dondur_hangi_sayfalar_baslik)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.filigran_hangi_sayfalar), modifier = Modifier.weight(1f))
                        Switch(
                            checked = ayarlar.tumSayfalar,
                            onCheckedChange = gorunum::tumSayfalarDegistir,
                        )
                    }
                    if (!ayarlar.tumSayfalar) {
                        AralikGirisi(
                            deger = ayarlar.aralikIfadesi,
                            degisti = gorunum::aralikDegistir,
                            toplamSayfa = girdi.sayfaSayisi,
                        )
                    }
                }

                SecenekKarti(stringResource(R.string.filigran_bilmeniz_gereken_baslik)) {
                    Text(
                        stringResource(R.string.filigran_bilmeniz_gereken_govde),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
    )
}
