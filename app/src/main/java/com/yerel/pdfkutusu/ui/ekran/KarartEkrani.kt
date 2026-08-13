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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yerel.pdfkutusu.pdf.PdfKartici
import com.yerel.pdfkutusu.ui.model.KarartViewModel
import com.yerel.pdfkutusu.ui.ortak.KarartmaTuvali
import com.yerel.pdfkutusu.ui.ortak.SayfaSeridi
import com.yerel.pdfkutusu.ui.ortak.bicimliBoyut

@Composable
fun KarartEkrani(gorunum: KarartViewModel, geriDon: () -> Unit) {
    val durum by gorunum.durum.collectAsStateWithLifecycle()
    val karartma by gorunum.karartma.collectAsStateWithLifecycle()

    AracGovdesi(
        baslik = "Karart",
        bosBaslik = "Bilgiyi gerçekten kaldırın",
        bosAciklama = "Bir PDF seçin, sayfayı açın ve gizlemek istediğiniz alanı " +
            "parmağınızla çizin. Seçilen alanlar sayfa görüntüsünden silinir.",
        simge = Icons.Default.Block,
        gorunum = gorunum,
        geriDon = geriDon,
        calistirEtiketi = "Karart ve kaydet",
        calistirEtkin = durum.girdiler.isNotEmpty() && karartma.alanlar.isNotEmpty(),
        calistir = gorunum::karart,
        secenekler = {
            val girdi = durum.ilkGirdi
            if (girdi != null) {
                NasilCalisirKarti(karartma.dpi, karartma.karartilanSayfaSayisi, girdi.sayfaSayisi)

                SecenekKarti("Sayfa seç") {
                    SayfaSeridi(
                        dosya = girdi.dosya,
                        sayfaIndeksleri = (0 until girdi.sayfaSayisi).toList(),
                        seciliIndeks = karartma.secilenSayfa,
                        onizleme = gorunum.onizlemeDeposu,
                        sec = gorunum::sayfaSec,
                        etiketUret = { indeks ->
                            val sayi = karartma.sayfaninAlanlari(indeks).size
                            if (sayi > 0) "${indeks + 1} ●$sayi" else "${indeks + 1}"
                        },
                    )
                }

                SecenekKarti("Alan çiz — sayfa ${karartma.secilenSayfa + 1}") {
                    Text(
                        "Gizlemek istediğiniz yerin üzerinde parmağınızı sürükleyin.",
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
                            Text("Geri al")
                        }
                        TextButton(onClick = gorunum::sayfayiTemizle) {
                            Text("Bu sayfayı temizle")
                        }
                        TextButton(onClick = gorunum::tumunuTemizleAlanlar) {
                            Icon(Icons.Default.DeleteSweep, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Tümü")
                        }
                    }

                    Text(
                        "Seçilen alan: ${karartma.alanlar.size} · " +
                            "${karartma.karartilanSayfaSayisi} sayfada",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                SecenekKarti("Çözünürlük") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        gorunum.dpiSecenekleri.forEach { dpi ->
                            FilterChip(
                                selected = karartma.dpi == dpi,
                                onClick = { gorunum.dpiDegistir(dpi) },
                                label = { Text("$dpi DPI") },
                            )
                        }
                    }
                    Text(
                        "En düşük ${PdfKartici.ASGARI_DPI} DPI. Yüksek çözünürlük daha okunaklı " +
                            "sayfa, daha büyük dosya demektir. Tahmini artış: " +
                            bicimliBoyut(
                                PdfKartici.tahminiBoyutBayt(
                                    karartma.karartilanSayfaSayisi.coerceAtLeast(1),
                                    karartma.dpi,
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
                Text("Karartma nasıl çalışır", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.padding(2.dp))
                Text(
                    "Karartılan sayfa görüntüye çevrilir, metni artık seçilemez ve " +
                        "dosya boyutu artar.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.padding(4.dp))
                Text(
                    "• Sayfa $dpi DPI'da görüntüye dönüştürülür, seçtiğiniz alanlar " +
                        "piksellerin üzerine opak siyah boyanır.\n" +
                        "• Böylece metin PDF'in içeriğinden gerçekten kalkar — " +
                        "üstüne dikdörtgen çizmekten farkı budur.\n" +
                        "• Dokunmadığınız sayfalar aynen kalır; metinleri seçilebilir olmayı sürdürür " +
                        "($karartilanSayfa / $toplamSayfa sayfa karartılacak).\n" +
                        "• Belge meta verileri (yazar, başlık, üretici) temizlenir.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
