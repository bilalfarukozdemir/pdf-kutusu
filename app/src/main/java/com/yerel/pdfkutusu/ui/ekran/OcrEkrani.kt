package com.yerel.pdfkutusu.ui.ekran

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yerel.pdfkutusu.R
import com.yerel.pdfkutusu.ui.model.OcrViewModel
import com.yerel.pdfkutusu.ui.ortak.AralikGirisi

@Composable
fun OcrEkrani(gorunum: OcrViewModel, geriDon: () -> Unit) {
    val durum by gorunum.durum.collectAsStateWithLifecycle()
    val secenekler by gorunum.secenekler.collectAsStateWithLifecycle()
    val baglam = LocalContext.current
    val panoEtiketi = stringResource(R.string.ocr_pano_etiketi)

    AracGovdesi(
        baslik = stringResource(R.string.ocr_baslik),
        bosBaslik = stringResource(R.string.ocr_bos_baslik),
        bosAciklama = stringResource(R.string.ocr_bos_aciklama),
        simge = Icons.Default.TextFields,
        gorunum = gorunum,
        geriDon = geriDon,
        calistirEtiketi = stringResource(R.string.ocr_calistir),
        calistirEtkin = durum.girdiler.isNotEmpty() && secenekler.aralikIfadesi.isNotBlank(),
        calistir = gorunum::tani,
        secenekler = {
            val girdi = durum.ilkGirdi
            if (girdi != null) {
                SecenekKarti(stringResource(R.string.ocr_sayfalar_baslik)) {
                    AralikGirisi(
                        deger = secenekler.aralikIfadesi,
                        degisti = gorunum::aralikDegistir,
                        toplamSayfa = girdi.sayfaSayisi,
                    )
                    Text(
                        stringResource(R.string.ocr_sure_ipucu),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                SecenekKarti(stringResource(R.string.ocr_cozunurluk_baslik)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        gorunum.dpiSecenekleri.forEach { dpi ->
                            FilterChip(
                                selected = secenekler.dpi == dpi,
                                onClick = { gorunum.dpiDegistir(dpi) },
                                label = { Text(stringResource(R.string.ocr_dpi, dpi)) },
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.ocr_dpi_ipucu),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (girdi.ozet.metinKatmaniVar) {
                    SecenekKarti(stringResource(R.string.ocr_not_baslik)) {
                        Text(
                            stringResource(R.string.ocr_not_metin_katmani_var),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                if (secenekler.cikanMetin.isNotBlank()) {
                    SecenekKarti(stringResource(R.string.ocr_cikan_metin_baslik)) {
                        OutlinedTextField(
                            value = secenekler.cikanMetin,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 320.dp),
                            textStyle = MaterialTheme.typography.bodySmall,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { panoyaKopyala(baglam, panoEtiketi, secenekler.cikanMetin) }) {
                                Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.ocr_panoya_kopyala))
                            }
                            TextButton(onClick = gorunum::metniTemizle) { Text(stringResource(R.string.ocr_temizle)) }
                        }
                        Text(
                            stringResource(R.string.ocr_kaydetme_ipucu),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                SecenekKarti(stringResource(R.string.ocr_kapsam_disi_baslik)) {
                    Text(
                        stringResource(R.string.ocr_kapsam_disi_govde),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
    )
}

private fun panoyaKopyala(baglam: Context, etiket: String, metin: String) {
    val pano = baglam.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    pano.setPrimaryClip(ClipData.newPlainText(etiket, metin))
}
