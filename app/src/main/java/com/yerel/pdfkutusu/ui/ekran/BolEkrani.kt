package com.yerel.pdfkutusu.ui.ekran

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
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
import com.yerel.pdfkutusu.ui.model.BolViewModel
import com.yerel.pdfkutusu.ui.ortak.AralikGirisi
import com.yerel.pdfkutusu.ui.ortak.SayfaSeridi

@Composable
fun BolEkrani(gorunum: BolViewModel, geriDon: () -> Unit) {
    val durum by gorunum.durum.collectAsStateWithLifecycle()
    val secenekler by gorunum.secenekler.collectAsStateWithLifecycle()
    var onizlenenSayfa by remember { mutableIntStateOf(0) }

    AracGovdesi(
        baslik = stringResource(R.string.bol_baslik),
        bosBaslik = stringResource(R.string.bol_bos_baslik),
        bosAciklama = stringResource(R.string.bol_bos_aciklama),
        simge = Icons.Default.ContentCut,
        gorunum = gorunum,
        geriDon = geriDon,
        calistirEtiketi = if (secenekler.ayriDosyalar) {
            stringResource(R.string.bol_calistir_ayri)
        } else {
            stringResource(R.string.bol_calistir_cikar)
        },
        calistirEtkin = durum.girdiler.isNotEmpty() && secenekler.aralikIfadesi.isNotBlank(),
        calistir = gorunum::bol,
        secenekler = {
            val girdi = durum.ilkGirdi
            if (girdi != null) {
                SecenekKarti(stringResource(R.string.bol_sayfa_secimi_baslik)) {
                    AralikGirisi(
                        deger = secenekler.aralikIfadesi,
                        degisti = gorunum::aralikDegistir,
                        toplamSayfa = girdi.sayfaSayisi,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.bol_her_aralik_ayri), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                if (secenekler.ayriDosyalar) {
                                    stringResource(R.string.bol_ayri_aciklama)
                                } else {
                                    stringResource(R.string.bol_tek_aciklama)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = secenekler.ayriDosyalar,
                            onCheckedChange = gorunum::ayriDosyalarDegistir,
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
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.bol_onizleme_ipucu),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}
