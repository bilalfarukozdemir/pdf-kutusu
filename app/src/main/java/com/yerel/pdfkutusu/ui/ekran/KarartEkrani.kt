package com.yerel.pdfkutusu.ui.ekran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import com.yerel.pdfkutusu.pdf.PdfKartici
import com.yerel.pdfkutusu.ui.model.KarartViewModel
import com.yerel.pdfkutusu.ui.ortak.KarartmaTuvali
import com.yerel.pdfkutusu.ui.ortak.SayfaSeridi
import com.yerel.pdfkutusu.ui.ortak.bicimliBoyut

@Composable
fun KarartEkrani(gorunum: KarartViewModel, geriDon: () -> Unit) {
    val durum by gorunum.durum.collectAsStateWithLifecycle()
    val karartma by gorunum.karartma.collectAsStateWithLifecycle()
    val sayfaEtiketIsaretliSablon = stringResource(R.string.karart_sayfa_etiket_isaretli)

    AracGovdesi(
        baslik = stringResource(R.string.karart_baslik),
        bosBaslik = stringResource(R.string.karart_bos_baslik),
        bosAciklama = stringResource(R.string.karart_bos_aciklama),
        simge = Icons.Default.Block,
        gorunum = gorunum,
        geriDon = geriDon,
        calistirEtiketi = stringResource(R.string.karart_calistir),
        calistirEtkin = durum.girdiler.isNotEmpty() && karartma.alanlar.isNotEmpty(),
        calistir = gorunum::karart,
        secenekler = {
            val girdi = durum.ilkGirdi
            if (girdi != null) {
                NasilCalisirKarti(karartma.dpi, karartma.karartilanSayfaSayisi, girdi.sayfaSayisi)

                SecenekKarti(stringResource(R.string.karart_sayfa_sec_baslik)) {
                    SayfaSeridi(
                        dosya = girdi.dosya,
                        sayfaIndeksleri = (0 until girdi.sayfaSayisi).toList(),
                        seciliIndeks = karartma.secilenSayfa,
                        onizleme = gorunum.onizlemeDeposu,
                        sec = gorunum::sayfaSec,
                        etiketUret = { indeks ->
                            val sayi = karartma.sayfaninAlanlari(indeks).size
                            if (sayi > 0) {
                                String.format(sayfaEtiketIsaretliSablon, indeks + 1, sayi)
                            } else {
                                "${indeks + 1}"
                            }
                        },
                    )
                }

                SecenekKarti(stringResource(R.string.karart_alan_ciz_baslik, karartma.secilenSayfa + 1)) {
                    Text(
                        stringResource(R.string.karart_alan_ciz_ipucu),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    KarartmaTuvali(
                        dosya = girdi.dosya,
                        sayfaIndeksi = karartma.secilenSayfa,
                        alanlar = karartma.sayfaninAlanlari(karartma.secilenSayfa),
                        onizleme = gorunum.onizlemeDeposu,
                        alanEkle = gorunum::alanEkle,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(onClick = gorunum::sonAlaniGeriAl) {
                            Icon(Icons.Default.Undo, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.karart_geri_al))
                        }
                        TextButton(onClick = gorunum::sayfayiTemizle) {
                            Text(stringResource(R.string.karart_sayfayi_temizle))
                        }
                        TextButton(onClick = gorunum::tumunuTemizleAlanlar) {
                            Icon(Icons.Default.DeleteSweep, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.karart_tumu))
                        }
                    }

                    Text(
                        stringResource(R.string.karart_secilen_alan, karartma.alanlar.size, karartma.karartilanSayfaSayisi),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                SecenekKarti(stringResource(R.string.karart_cozunurluk_baslik)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        gorunum.dpiSecenekleri.forEach { dpi ->
                            FilterChip(
                                selected = karartma.dpi == dpi,
                                onClick = { gorunum.dpiDegistir(dpi) },
                                label = { Text(stringResource(R.string.karart_dpi, dpi)) },
                            )
                        }
                    }
                    Text(
                        stringResource(
                            R.string.karart_cozunurluk_aciklama,
                            PdfKartici.ASGARI_DPI,
                            bicimliBoyut(
                                PdfKartici.tahminiBoyutBayt(
                                    karartma.karartilanSayfaSayisi.coerceAtLeast(1),
                                    karartma.dpi,
                                ),
                            ),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

/**
 * Sartname geregi kullaniciya gosterilmesi zorunlu aciklama.
 * Karartmanin bedeli (gorsele donusme, metin kaybi, boyut artisi) islemden
 * ONCE net bicimde soylenir.
 */
@Composable
private fun NasilCalisirKarti(dpi: Int, karartilanSayfa: Int, toplamSayfa: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Row(Modifier.padding(16.dp)) {
            Icon(Icons.Default.Warning, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            androidx.compose.foundation.layout.Column {
                Text(stringResource(R.string.karart_nasil_calisir_baslik), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.padding(2.dp))
                Text(
                    stringResource(R.string.karart_nasil_calisir_govde),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.padding(4.dp))
                Text(
                    stringResource(R.string.karart_nasil_calisir_detay, dpi, karartilanSayfa, toplamSayfa),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
