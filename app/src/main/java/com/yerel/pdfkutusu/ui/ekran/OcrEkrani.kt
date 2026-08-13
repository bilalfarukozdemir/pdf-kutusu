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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yerel.pdfkutusu.ui.model.OcrViewModel
import com.yerel.pdfkutusu.ui.ortak.AralikGirisi

@Composable
fun OcrEkrani(gorunum: OcrViewModel, geriDon: () -> Unit) {
    val durum by gorunum.durum.collectAsStateWithLifecycle()
    val secenekler by gorunum.secenekler.collectAsStateWithLifecycle()
    val baglam = LocalContext.current

    AracGovdesi(
        baslik = "OCR — metin çıkar",
        bosBaslik = "Sayfadaki metni okuyun",
        bosAciklama = "Bir PDF seçin. Sayfalar cihaz üstünde taranır; " +
            "hiçbir görüntü dışarı gönderilmez.",
        simge = Icons.Default.TextFields,
        gorunum = gorunum,
        geriDon = geriDon,
        calistirEtiketi = "Metni çıkar",
        calistirEtkin = durum.girdiler.isNotEmpty() && secenekler.aralikIfadesi.isNotBlank(),
        calistir = gorunum::tani,
        secenekler = {
            val girdi = durum.ilkGirdi
            if (girdi != null) {
                SecenekKarti("Sayfalar") {
                    AralikGirisi(
                        deger = secenekler.aralikIfadesi,
                        degisti = gorunum::aralikDegistir,
                        toplamSayfa = girdi.sayfaSayisi,
                    )
                    Text(
                        "Tarama sayfa başına birkaç saniye sürer; geniş aralıklarda sabırlı olun.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                SecenekKarti("Tarama çözünürlüğü") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        gorunum.dpiSecenekleri.forEach { dpi ->
                            FilterChip(
                                selected = secenekler.dpi == dpi,
                                onClick = { gorunum.dpiDegistir(dpi) },
                                label = { Text("$dpi DPI") },
                            )
                        }
                    }
                    Text(
                        "Küçük punto ya da soluk taramalarda 400 DPI belirgin fark yaratır.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (girdi.ozet.metinKatmaniVar) {
                    SecenekKarti("Not") {
                        Text(
                            "Bu belgede zaten seçilebilir metin var. OCR bir tahmindir; " +
                                "metni doğrudan bir PDF okuyucudan kopyalamak daha doğru sonuç verir.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                if (secenekler.cikanMetin.isNotBlank()) {
                    SecenekKarti("Çıkan metin") {
                        OutlinedTextField(
                            value = secenekler.cikanMetin,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 320.dp),
                            textStyle = MaterialTheme.typography.bodySmall,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { panoyaKopyala(baglam, secenekler.cikanMetin) }) {
                                Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Panoya kopyala")
                            }
                            TextButton(onClick = gorunum::metniTemizle) { Text("Temizle") }
                        }
                        Text(
                            ".txt olarak kaydetmek için aşağıdaki sonuç kartındaki " +
                                "\"Kaydet\" düğmesini kullanın.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                SecenekKarti("Kapsam dışı") {
                    Text(
                        "PDF'e aranabilir metin katmanı gömme bu sürümde yok. " +
                            "Çıkan metin ayrı bir .txt dosyası olarak verilir.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
    )
}

private fun panoyaKopyala(baglam: Context, metin: String) {
    val pano = baglam.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    pano.setPrimaryClip(ClipData.newPlainText("PDF Kutusu OCR", metin))
}
