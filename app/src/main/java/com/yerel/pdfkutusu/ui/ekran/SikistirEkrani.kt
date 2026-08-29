package com.yerel.pdfkutusu.ui.ekran

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yerel.pdfkutusu.R
import com.yerel.pdfkutusu.pdf.SikistirmaKalitesi
import com.yerel.pdfkutusu.ui.model.SikistirViewModel
import com.yerel.pdfkutusu.ui.ortak.YukleniyorSatiri
import com.yerel.pdfkutusu.ui.ortak.bicimliBoyut

@Composable
fun SikistirEkrani(gorunum: SikistirViewModel, geriDon: () -> Unit) {
    val durum by gorunum.durum.collectAsStateWithLifecycle()
    val secenekler by gorunum.secenekler.collectAsStateWithLifecycle()

    AracGovdesi(
        baslik = stringResource(R.string.sikistir_baslik),
        bosBaslik = stringResource(R.string.sikistir_bos_baslik),
        bosAciklama = stringResource(R.string.sikistir_bos_aciklama),
        simge = Icons.Default.Compress,
        gorunum = gorunum,
        geriDon = geriDon,
        calistirEtiketi = stringResource(R.string.sikistir_calistir),
        calistirEtkin = durum.girdiler.isNotEmpty(),
        calistir = gorunum::sikistir,
        secenekler = {
            val girdi = durum.ilkGirdi
            if (girdi != null) {
                SecenekKarti(stringResource(R.string.sikistir_kalite_baslik)) {
                    if (secenekler.tahminHesaplaniyor) {
                        YukleniyorSatiri(stringResource(R.string.sikistir_tahmin_hesaplaniyor))
                    }

                    SikistirmaKalitesi.entries.forEach { kalite ->
                        val tahmin = secenekler.tahminler.firstOrNull { it.kalite == kalite }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = secenekler.kalite == kalite,
                                    onClick = { gorunum.kaliteDegistir(kalite) },
                                )
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = secenekler.kalite == kalite,
                                onClick = { gorunum.kaliteDegistir(kalite) },
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Row {
                                    Text(
                                        stringResource(kalite.etiketRes),
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (tahmin != null) {
                                        Text(
                                            stringResource(R.string.sikistir_tahmin_yaklasik, bicimliBoyut(tahmin.tahminiBayt)),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                                Text(
                                    stringResource(kalite.aciklamaRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Text(
                        stringResource(R.string.sikistir_su_anki_boyut, bicimliBoyut(girdi.boyut), girdi.sayfaSayisi),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        stringResource(R.string.sikistir_tahmin_aciklama),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (secenekler.gorselYok && !secenekler.tahminHesaplaniyor) {
                        Text(
                            stringResource(R.string.sikistir_gorsel_yok),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
    )
}
