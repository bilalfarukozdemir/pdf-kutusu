package com.yerel.pdfkutusu.ui.ekran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yerel.pdfkutusu.R
import com.yerel.pdfkutusu.ui.model.SiralaViewModel
import com.yerel.pdfkutusu.ui.ortak.SayfaKucukResmi
import com.yerel.pdfkutusu.ui.ortak.SurukleBirakSeridi

@Composable
fun SiralaEkrani(gorunum: SiralaViewModel, geriDon: () -> Unit) {
    val durum by gorunum.durum.collectAsStateWithLifecycle()
    val sira by gorunum.sira.collectAsStateWithLifecycle()
    val konumEtiketSablon = stringResource(R.string.sirala_konum_etiket)

    AracGovdesi(
        baslik = stringResource(R.string.sirala_baslik),
        bosBaslik = stringResource(R.string.sirala_bos_baslik),
        bosAciklama = stringResource(R.string.sirala_bos_aciklama),
        simge = Icons.Default.SwapVert,
        gorunum = gorunum,
        geriDon = geriDon,
        calistirEtiketi = stringResource(R.string.sirala_calistir),
        calistirEtkin = durum.girdiler.isNotEmpty() && sira.isNotEmpty(),
        calistir = gorunum::uygula,
        secenekler = {
            val girdi = durum.ilkGirdi
            if (girdi != null) {
                SecenekKarti(stringResource(R.string.sirala_sayfa_sirasi_baslik)) {
                    Text(
                        stringResource(R.string.sirala_ipucu),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    SurukleBirakSeridi(
                        ogeler = sira,
                        anahtar = { konum, sayfa -> "$konum:$sayfa" },
                        tasi = gorunum::tasi,
                        cikar = gorunum::sayfaCikar,
                        altEtiket = { konum, sayfa ->
                            String.format(konumEtiketSablon, konum + 1, sayfa + 1)
                        },
                    ) { _, sayfaIndeksi ->
                        SayfaKucukResmi(
                            dosya = girdi.dosya,
                            sayfaIndeksi = sayfaIndeksi,
                            onizleme = gorunum.onizlemeDeposu,
                            modifier = Modifier.fillMaxSize(),
                            hedefGenislikPx = 200,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(onClick = gorunum::tersCevir) {
                            Icon(Icons.Default.SwapHoriz, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.sirala_ters_cevir))
                        }
                        TextButton(onClick = gorunum::sifirla) {
                            Icon(Icons.Default.Restore, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.sirala_sifirla))
                        }
                    }

                    Text(
                        stringResource(R.string.sirala_cikti_ozet, sira.size, girdi.sayfaSayisi),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
    )
}
