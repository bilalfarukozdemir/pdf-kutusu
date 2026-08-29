package com.yerel.pdfkutusu.ui.ekran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yerel.pdfkutusu.R
import com.yerel.pdfkutusu.pdf.SayfaDuzeni
import com.yerel.pdfkutusu.pdf.SayfaYerlesimi
import com.yerel.pdfkutusu.pdf.SikistirmaKalitesi
import com.yerel.pdfkutusu.ui.model.ResimdenPdfViewModel
import com.yerel.pdfkutusu.ui.ortak.GorselKucukResmi
import com.yerel.pdfkutusu.ui.ortak.SurukleBirakSeridi
import com.yerel.pdfkutusu.ui.ortak.YukleniyorSatiri
import com.yerel.pdfkutusu.ui.ortak.bicimliBoyut

@Composable
fun ResimdenPdfEkrani(gorunum: ResimdenPdfViewModel, geriDon: () -> Unit) {
    val durum by gorunum.durum.collectAsStateWithLifecycle()
    val secenekler by gorunum.secenekler.collectAsStateWithLifecycle()

    AracGovdesi(
        baslik = stringResource(R.string.resimden_baslik),
        bosBaslik = stringResource(R.string.resimden_bos_baslik),
        bosAciklama = stringResource(R.string.resimden_bos_aciklama),
        simge = Icons.Default.PhotoLibrary,
        gorunum = gorunum,
        geriDon = geriDon,
        cokluSecim = true,
        mimeTurleri = arrayOf("image/*"),
        secButonuEtiketi = stringResource(R.string.resimden_gorselleri_sec),
        ekleButonuEtiketi = stringResource(R.string.resimden_gorsel_ekle),
        calistirEtiketi = stringResource(R.string.resimden_calistir),
        calistirEtkin = durum.girdiler.isNotEmpty(),
        calistir = gorunum::olustur,
        secenekler = {
            if (durum.girdiler.isNotEmpty()) {
            // ---------------------------------------------------------- sira
            SecenekKarti(stringResource(R.string.resimden_sira_baslik, durum.girdiler.size)) {
                Text(
                    stringResource(R.string.resimden_sira_ipucu),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SurukleBirakSeridi(
                    ogeler = durum.girdiler,
                    anahtar = { _, oge -> oge.dosya.absolutePath },
                    tasi = gorunum::girdiTasi,
                    cikar = { konum ->
                        durum.girdiler.getOrNull(konum)?.let { gorunum.girdiKaldir(it.dosya) }
                    },
                    altEtiket = { konum, _ -> "${konum + 1}" },
                    enBoyOrani = 1f,
                    seritYuksekligi = 132.dp,
                ) { _, oge ->
                    GorselKucukResmi(
                        dosya = oge.dosya,
                        onizleme = gorunum.gorselOnizleme,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = gorunum::adaGoreSirala) { Text(stringResource(R.string.resimden_ada_gore)) }
                    TextButton(onClick = gorunum::tariheGoreSirala) { Text(stringResource(R.string.resimden_tarihe_gore)) }
                }
            }

            // --------------------------------------------------------- duzen
            SecenekKarti(stringResource(R.string.resimden_duzen_baslik)) {
                SayfaDuzeni.entries.forEach { duzen ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = secenekler.duzen == duzen,
                                onClick = { gorunum.duzenDegistir(duzen) },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = secenekler.duzen == duzen,
                            onClick = { gorunum.duzenDegistir(duzen) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(stringResource(duzen.etiketRes), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(duzen.aciklamaRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                when (secenekler.duzen) {
                    SayfaDuzeni.A4_SIGDIR -> {
                        Text(stringResource(R.string.resimden_kenar_boslugu), style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SayfaYerlesimi.KENAR_BOSLUKLARI_MM.forEach { mm ->
                                FilterChip(
                                    selected = secenekler.kenarBoslguMm == mm,
                                    onClick = { gorunum.kenarBoslguDegistir(mm) },
                                    label = {
                                        Text(
                                            if (mm == 0) {
                                                stringResource(R.string.resimden_kenar_yok)
                                            } else {
                                                stringResource(R.string.resimden_mm, mm)
                                            },
                                        )
                                    },
                                )
                            }
                        }
                    }

                    SayfaDuzeni.GORUNTU_BOYUTU -> {
                        Text(stringResource(R.string.resimden_cozunurluk), style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SayfaYerlesimi.DPI_SECENEKLERI.forEach { dpi ->
                                FilterChip(
                                    selected = secenekler.dpi == dpi,
                                    onClick = { gorunum.dpiDegistir(dpi) },
                                    label = { Text(stringResource(R.string.resimden_dpi, dpi)) },
                                )
                            }
                        }
                        Text(
                            stringResource(R.string.resimden_dpi_aciklama),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Text(
                    stringResource(R.string.resimden_oran_korunur),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // -------------------------------------------------------- kalite
            SecenekKarti(stringResource(R.string.resimden_kalite_baslik)) {
                SikistirmaKalitesi.entries.forEach { kalite ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = secenekler.kalite == kalite,
                                onClick = { gorunum.kaliteDegistir(kalite) },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = secenekler.kalite == kalite,
                            onClick = { gorunum.kaliteDegistir(kalite) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(stringResource(kalite.etiketRes), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(kalite.aciklamaRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (secenekler.tahminHesaplaniyor) {
                    YukleniyorSatiri(stringResource(R.string.sikistir_tahmin_hesaplaniyor))
                } else if (secenekler.tahminiBayt > 0) {
                    Text(
                        stringResource(
                            R.string.resimden_tahmini_cikti,
                            bicimliBoyut(secenekler.tahminiBayt),
                            bicimliBoyut(durum.girdiler.sumOf { it.boyut }),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // ----------------------------------------------------- gizlilik
            SecenekKarti(stringResource(R.string.resimden_gizlilik_baslik)) {
                Text(
                    stringResource(R.string.resimden_gizlilik_govde),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            }
        },
    )
}
