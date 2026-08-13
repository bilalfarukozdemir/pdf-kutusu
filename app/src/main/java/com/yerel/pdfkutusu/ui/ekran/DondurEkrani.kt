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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        baslik = "Döndür",
        bosBaslik = "Sayfaları döndürün",
        bosAciklama = "Bir PDF seçin, açıyı belirleyin. Tüm sayfaları ya da " +
            "seçtiğiniz aralığı döndürebilirsiniz.",
        simge = Icons.Default.Rotate90DegreesCw,
        gorunum = gorunum,
        geriDon = geriDon,
        calistirEtiketi = "${secenekler.aci}° döndür",
        calistirEtkin = durum.girdiler.isNotEmpty(),
        calistir = gorunum::dondur,
        secenekler = {
            val girdi = durum.ilkGirdi
            if (girdi != null) {
                SecenekKarti("Açı") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PdfDondurucu.SECENEKLER.forEach { aci ->
                            FilterChip(
                                selected = secenekler.aci == aci,
                                onClick = { gorunum.aciDegistir(aci) },
                                label = { Text("$aci°") },
                            )
                        }
                    }
                    Text(
                        "Saat yönünde döndürür. 270° = saat yönünün tersine 90°.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                SecenekKarti("Hangi sayfalar") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Tüm sayfalar", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${girdi.sayfaSayisi} sayfanın tamamı döndürülür.",
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

                SecenekKarti("Önizleme") {
                    SayfaSeridi(
                        dosya = girdi.dosya,
                        sayfaIndeksleri = (0 until girdi.sayfaSayisi).toList(),
                        seciliIndeks = onizlenenSayfa,
                        onizleme = gorunum.onizlemeDeposu,
                        sec = { onizlenenSayfa = it },
                    )
                    Text(
                        "Önizleme kaynak belgeyi gösterir; döndürme çıktıya uygulanır.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}
