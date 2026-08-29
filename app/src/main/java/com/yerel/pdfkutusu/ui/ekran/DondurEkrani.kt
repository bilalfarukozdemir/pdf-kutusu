package com.yerel.pdfkutusu.ui.ekran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yerel.pdfkutusu.R
import com.yerel.pdfkutusu.pdf.PdfDondurucu
import com.yerel.pdfkutusu.ui.model.DondurViewModel
import com.yerel.pdfkutusu.ui.ortak.AralikGirisi
import com.yerel.pdfkutusu.ui.ortak.SayfaSeridi

@Composable
fun DondurEkrani(gorunum: DondurViewModel, geriDon: () -> Unit) {
    val durum by gorunum.durum.collectAsStateWithLifecycle()
    val secenekler by gorunum.secenekler.collectAsStateWithLifecycle()
    var onizlenenSayfa by remember { mutableIntStateOf(0) }

    AracGovdesi(
        baslik = stringResource(R.string.dondur_baslik),
        bosBaslik = stringResource(R.string.dondur_bos_baslik),
        bosAciklama = stringResource(R.string.dondur_bos_aciklama),
        simge = Icons.Default.Rotate90DegreesCw,
        gorunum = gorunum,
        geriDon = geriDon,
        calistirEtiketi = stringResource(R.string.dondur_calistir, secenekler.aci),
        calistirEtkin = durum.girdiler.isNotEmpty(),
        calistir = gorunum::dondur,
        secenekler = {
            val girdi = durum.ilkGirdi
            if (girdi != null) {
                SecenekKarti(stringResource(R.string.dondur_aci_baslik)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PdfDondurucu.SECENEKLER.forEach { aci ->
                            FilterChip(
                                selected = secenekler.aci == aci,
                                onClick = { gorunum.aciDegistir(aci) },
                                label = { Text(stringResource(R.string.dondur_aci_derece, aci)) },
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.dondur_saat_yonu_aciklama),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                SecenekKarti(stringResource(R.string.dondur_hangi_sayfalar_baslik)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.dondur_tum_sayfalar), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                stringResource(R.string.dondur_tum_sayfalar_aciklama, girdi.sayfaSayisi),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = secenekler.tumSayfalar,
                            onCheckedChange = gorunum::tumSayfalarDegistir,
                        )
                    }

                    if (!secenekler.tumSayfalar) {
                        AralikGirisi(
                            deger = secenekler.aralikIfadesi,
                            degisti = gorunum::aralikDegistir,
                            toplamSayfa = girdi.sayfaSayisi,
                        )
                    }
                }

                SecenekKarti(stringResource(R.string.bol_onizleme_baslik)) {
                    SayfaSeridi(
                        dosya = girdi.dosya,
                        sayfaIndeksleri = (0 until girdi.sayfaSayisi).toList(),
                        seciliIndeks = onizlenenSayfa,
                        onizleme = gorunum.onizlemeDeposu,
                        sec = { onizlenenSayfa = it },
                    )
                    Text(
                        stringResource(R.string.dondur_onizleme_aciklama),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}
