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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yerel.pdfkutusu.pdf.FiligranRengi
import com.yerel.pdfkutusu.ui.model.FiligranViewModel
import com.yerel.pdfkutusu.ui.ortak.AralikGirisi
import kotlin.math.roundToInt

@Composable
fun FiligranEkrani(gorunum: FiligranViewModel, geriDon: () -> Unit) {
    val durum by gorunum.durum.collectAsStateWithLifecycle()
    val ayarlar by gorunum.ayarlar.collectAsStateWithLifecycle()

    AracGovdesi(
        baslik = "Filigran",
        bosBaslik = "Metin filigranı ekleyin",
        bosAciklama = "Bir PDF seçin ve sayfalara çapraz bir metin ekleyin. " +
            "Filigran içeriğin üstüne çizilir, altındaki metni gizlemez.",
        simge = Icons.Default.BrandingWatermark,
        gorunum = gorunum,
        geriDon = geriDon,
        calistirEtiketi = "Filigranı uygula",
        calistirEtkin = durum.girdiler.isNotEmpty() && ayarlar.metin.isNotBlank(),
        calistir = gorunum::uygula,
        secenekler = {
            val girdi = durum.ilkGirdi
            if (girdi != null) {
                SecenekKarti("Metin") {
                    OutlinedTextField(
                        value = ayarlar.metin,
                        onValueChange = gorunum::metinDegistir,
                        label = { Text("Filigran metni") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text("Türkçe karakterler desteklenir; cihazın yazı tipi gömülür.")
                        },
                    )
                }

                SecenekKarti("Görünüm") {
                    Text("Punto: ${ayarlar.punto.roundToInt()}")
                    Slider(
                        value = ayarlar.punto,
                        onValueChange = gorunum::puntoDegistir,
                        valueRange = 12f..140f,
                    )

                    Text("Saydamlık: %${(ayarlar.saydamlik * 100).roundToInt()}")
                    Slider(
                        value = ayarlar.saydamlik,
                        onValueChange = gorunum::saydamlikDegistir,
                        valueRange = 0.05f..1f,
                    )

                    Text("Açı: ${ayarlar.aci.roundToInt()}°")
                    Slider(
                        value = ayarlar.aci,
                        onValueChange = gorunum::aciDegistir,
                        valueRange = 0f..90f,
                    )

                    Text("Renk", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FiligranRengi.entries.forEach { renk ->
                            FilterChip(
                                selected = ayarlar.renk == renk,
                                onClick = { gorunum.renkDegistir(renk) },
                                label = { Text(renk.etiket) },
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Döşe", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Sayfayı baştan başa tekrarlayan filigranla kapla.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(checked = ayarlar.doseme, onCheckedChange = gorunum::dosemeDegistir)
                    }
                }

                SecenekKarti("Hangi sayfalar") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Tüm sayfalar", modifier = Modifier.weight(1f))
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

                SecenekKarti("Bilmeniz gereken") {
                    Text(
                        "Filigran bir gizleme aracı değildir. Altındaki metin PDF'in içerik " +
                            "akışında kalır ve kopyalanabilir. Bilgiyi gerçekten kaldırmak için " +
                            "Karart aracını kullanın.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
    )
}
