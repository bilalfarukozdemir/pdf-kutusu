package com.yerel.pdfkutusu.ui.ekran

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yerel.pdfkutusu.pdf.SikistirmaKalitesi
import com.yerel.pdfkutusu.ui.model.SikistirViewModel
import com.yerel.pdfkutusu.ui.ortak.YukleniyorSatiri
import com.yerel.pdfkutusu.ui.ortak.bicimliBoyut

@Composable
fun SikistirEkrani(gorunum: SikistirViewModel, geriDon: () -> Unit) {
    val durum by gorunum.durum.collectAsStateWithLifecycle()
    val secenekler by gorunum.secenekler.collectAsStateWithLifecycle()

    AracGovdesi(
        baslik = "Sıkıştır",
        bosBaslik = "Dosya boyutunu küçültün",
        bosAciklama = "Bir PDF seçin. Gömülü görseller yeniden örneklenip daha " +
            "düşük kalitede kodlanır; metin ve yazı tipleri değişmez.",
        simge = Icons.Default.Compress,
        gorunum = gorunum,
        geriDon = geriDon,
        calistirEtiketi = "Sıkıştır",
        calistirEtkin = durum.girdiler.isNotEmpty(),
        calistir = gorunum::sikistir,
        secenekler = {
            val girdi = durum.ilkGirdi
            if (girdi != null) {
                SecenekKarti("Kalite") {
                    if (secenekler.tahminHesaplaniyor) {
                        YukleniyorSatiri("Tahmini boyut hesaplanıyor…")
                    }

                    SikistirmaKalitesi.entries.forEach { kalite ->
                        val tahmin = secenekler.tahminler.firstOrNull { it.kalite == kalite }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = secenekler.kalite == kalite,
                                    onClick = { gorunum.kaliteDegistir(kalite) },
                                )
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = secenekler.kalite == kalite,
                                onClick = { gorunum.kaliteDegistir(kalite) },
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Row {
                                    Text(
                                        kalite.etiket,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (tahmin != null) {
                                        Text(
                                            "≈ ${bicimliBoyut(tahmin.tahminiBayt)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                                Text(
                                    kalite.aciklama,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Text(
                        "Şu anki boyut: ${bicimliBoyut(girdi.boyut)} · ${girdi.sayfaSayisi} sayfa",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Tahminler yaklaşıktır (≈). Gerçek sonuç işlemden sonra gösterilir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (secenekler.gorselYok && !secenekler.tahminHesaplaniyor) {
                        Text(
                            "Bu belgede sıkıştırılabilir görsel görünmüyor. Kazanç çok az olacak; " +
                                "orijinali kullanmak daha iyi olabilir.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
    )
}
