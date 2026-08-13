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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        baslik = "Resimden PDF",
        bosBaslik = "Görselleri tek PDF'te toplayın",
        bosAciklama = "Birden fazla fotoğraf ya da ekran görüntüsü seçin. " +
            "Konum ve cihaz bilgisi (EXIF) çıktıya aktarılmaz.",
        simge = Icons.Default.PhotoLibrary,
        gorunum = gorunum,
        geriDon = geriDon,
        cokluSecim = true,
        mimeTurleri = arrayOf("image/*"),
        secButonuEtiketi = "Görselleri seç",
        ekleButonuEtiketi = "Görsel ekle",
        calistirEtiketi = "PDF oluştur",
        calistirEtkin = durum.girdiler.isNotEmpty(),
        calistir = gorunum::olustur,
        secenekler = {
            if (durum.girdiler.isNotEmpty()) {
            // ---------------------------------------------------------- sira
            SecenekKarti("Sıra — ${durum.girdiler.size} görsel") {
                Text(
                    "Bir görseli basılı tutup yana sürükleyerek taşıyın.",
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
                    TextButton(onClick = gorunum::adaGoreSirala) { Text("Ada göre") }
                    TextButton(onClick = gorunum::tariheGoreSirala) { Text("Çekilme tarihine göre") }
                }
            }

            // --------------------------------------------------------- duzen
            SecenekKarti("Sayfa düzeni") {
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
                            Text(duzen.etiket, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                duzen.aciklama,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                when (secenekler.duzen) {
                    SayfaDuzeni.A4_SIGDIR -> {
                        Text("Kenar boşluğu", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SayfaYerlesimi.KENAR_BOSLUKLARI_MM.forEach { mm ->
                                FilterChip(
                                    selected = secenekler.kenarBoslguMm == mm,
                                    onClick = { gorunum.kenarBoslguDegistir(mm) },
                                    label = { Text(if (mm == 0) "Yok" else "$mm mm") },
                                )
                            }
                        }
                    }

                    SayfaDuzeni.GORUNTU_BOYUTU -> {
                        Text("Çözünürlük", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SayfaYerlesimi.DPI_SECENEKLERI.forEach { dpi ->
                                FilterChip(
                                    selected = secenekler.dpi == dpi,
                                    onClick = { gorunum.dpiDegistir(dpi) },
                                    label = { Text("$dpi DPI") },
                                )
                            }
                        }
                        Text(
                            "Sayfa boyutu = piksel × 72 / DPI. Yüksek DPI, aynı görseli daha " +
                                "küçük bir sayfaya sığdırır.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Text(
                    "En-boy oranı hiçbir düzende bozulmaz.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // -------------------------------------------------------- kalite
            SecenekKarti("Kalite") {
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
                            Text(kalite.etiket, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                kalite.aciklama,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (secenekler.tahminHesaplaniyor) {
                    YukleniyorSatiri("Tahmini boyut hesaplanıyor…")
                } else if (secenekler.tahminiBayt > 0) {
                    Text(
                        "Tahmini çıktı: ≈ ${bicimliBoyut(secenekler.tahminiBayt)} · " +
                            "kaynak toplamı ${bicimliBoyut(durum.girdiler.sumOf { it.boyut })}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // ----------------------------------------------------- gizlilik
            SecenekKarti("Gizlilik") {
                Text(
                    "Görseller bitmap'e çözülüp yeniden kodlanır. Fotoğraflarınızın EXIF " +
                        "verisi — GPS konumu, cihaz modeli, çekim tarihi — çıktıya geçmez. " +
                        "Belge meta verileri de temizlenir.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            }
        },
    )
}
