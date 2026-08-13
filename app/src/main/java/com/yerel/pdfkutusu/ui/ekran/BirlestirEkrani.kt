package com.yerel.pdfkutusu.ui.ekran

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yerel.pdfkutusu.ui.model.BirlestirViewModel
import com.yerel.pdfkutusu.ui.ortak.bicimliBoyut

@Composable
fun BirlestirEkrani(gorunum: BirlestirViewModel, geriDon: () -> Unit) {
    val durum by gorunum.durum.collectAsStateWithLifecycle()

    AracGovdesi(
        baslik = "Birleştir",
        bosBaslik = "PDF'leri tek dosyada birleştirin",
        bosAciklama = "En az iki PDF seçin. Çıktıdaki sıra, listedeki sıradır; " +
            "okları kullanarak değiştirebilirsiniz.",
        simge = Icons.Default.MergeType,
        gorunum = gorunum,
        geriDon = geriDon,
        cokluSecim = true,
        calistirEtiketi = "Birleştir",
        calistirEtkin = durum.girdiler.size >= 2,
        calistir = gorunum::birlestir,
        secenekler = {
            if (durum.girdiler.isNotEmpty()) {
                SecenekKarti("Özet") {
                    Text(
                        "${durum.girdiler.size} dosya · toplam ${durum.toplamSayfa} sayfa · " +
                            bicimliBoyut(durum.girdiler.sumOf { it.boyut }),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (durum.girdiler.size < 2) {
                        Text(
                            "Birleştirmek için en az bir dosya daha ekleyin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    )
}
