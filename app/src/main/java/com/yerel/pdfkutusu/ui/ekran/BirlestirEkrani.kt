package com.yerel.pdfkutusu.ui.ekran

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yerel.pdfkutusu.R
import com.yerel.pdfkutusu.ui.model.BirlestirViewModel
import com.yerel.pdfkutusu.ui.ortak.bicimliBoyut

@Composable
fun BirlestirEkrani(gorunum: BirlestirViewModel, geriDon: () -> Unit) {
    val durum by gorunum.durum.collectAsStateWithLifecycle()

    AracGovdesi(
        baslik = stringResource(R.string.birlestir_baslik),
        bosBaslik = stringResource(R.string.birlestir_bos_baslik),
        bosAciklama = stringResource(R.string.birlestir_bos_aciklama),
        simge = Icons.Default.MergeType,
        gorunum = gorunum,
        geriDon = geriDon,
        cokluSecim = true,
        calistirEtiketi = stringResource(R.string.birlestir_calistir),
        calistirEtkin = durum.girdiler.size >= 2,
        calistir = gorunum::birlestir,
        secenekler = {
            if (durum.girdiler.isNotEmpty()) {
                SecenekKarti(stringResource(R.string.birlestir_ozet_baslik)) {
                    Text(
                        stringResource(
                            R.string.birlestir_ozet_metni,
                            durum.girdiler.size,
                            durum.toplamSayfa,
                            bicimliBoyut(durum.girdiler.sumOf { it.boyut }),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (durum.girdiler.size < 2) {
                        Text(
                            stringResource(R.string.birlestir_daha_ekle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    )
}
