package com.yerel.pdfkutusu.ui.ekran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yerel.pdfkutusu.ui.ortak.AracIskeleti
import com.yerel.pdfkutusu.ui.ortak.BaglantiMetni

@Composable
fun HakkindaEkrani(geriDon: () -> Unit) {
    AracIskeleti(baslik = "Hakkında", geriDon = geriDon) { doldurma ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(doldurma)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            UygulamaKimligi()

            Bolum(
                baslik = "Düşük riskli kullanım uyarısı",
                govde = "Bu araç kişisel ve düşük riskli kullanım içindir. Resmî, hukuki " +
                    "veya regüle belgeler için tek başına güvenmeyin. Çıktıyı her zaman " +
                    "açıp gözle kontrol edin.",
                vurgulu = true,
            )

            Bolum(
                baslik = "İzinler",
                govde = "Bu uygulama HİÇBİR izin istemez. AndroidManifest.xml içinde " +
                    "INTERNET izni bulunmaz; bağımlılıkların eklemeye çalıştığı ağ izinleri " +
                    "manifest birleştirmede silinir ve derleme sırasında bir doğrulama " +
                    "görevi bunu kontrol eder.\n\n" +
                    "Dosya seçme ve dışa aktarma Storage Access Framework (SAF) ile yapılır; " +
                    "SAF izin gerektirmez. MANAGE_EXTERNAL_STORAGE veya geniş depolama izni " +
                    "hiçbir koşulda istenmez.",
            )

            Bolum(
                baslik = "Verileriniz nerede duruyor",
                govde = "• Seçtiğiniz dosyaların kopyaları: uygulama alanı / calisma\n" +
                    "• Üretilen çıktılar: uygulama alanı / cikti\n" +
                    "• Geçici dosyalar: önbellek / gecici\n" +
                    "• İşlem günlüğü: uygulama alanı / pdf_kutusu.db (SQLite)\n\n" +
                    "Hepsi uygulamaya özeldir; başka uygulamalar okuyamaz. Bulut yedeği ve " +
                    "cihaz transferi kapalıdır. Uygulamayı kaldırdığınızda hepsi silinir.",
            )

            Bolum(
                baslik = "Orijinal dosyalarınız",
                govde = "Kaynak dosyaya asla yazılmaz. Her işlem yeni bir çıktı dosyası " +
                    "üretir:\n\n<orijinal-ad>__<islem>__<yyyyMMdd-HHmmss>.pdf",
            )

            Bolum(
                baslik = "Görselleriniz ve EXIF",
                govde = "Resimden PDF aracı, görselleri çözüp yeniden kodlar. Fotoğrafların " +
                    "taşıdığı EXIF verisi — GPS konumu, cihaz markası ve modeli, çekim " +
                    "tarihi — çıktıya aktarılmaz. Yön etiketi ise okunup uygulanır, " +
                    "böylece sayfalar yan yatmaz.",
            )

            Bolum(
                baslik = "Kullanılan kütüphaneler",
                govde = "• PdfBox-Android (Apache 2.0) — PDF okuma/yazma\n" +
                    "• Android PdfRenderer (yerleşik) — sayfa görüntüleme ve rasterize\n" +
                    "• ML Kit Text Recognition v2, paketli model (Apache 2.0) — cihaz üstü OCR\n" +
                    "• AndroidX ExifInterface (Apache 2.0) — görsel yön etiketi\n" +
                    "• Jetpack Compose, Material 3, Room (Apache 2.0)\n\n" +
                    "AGPL lisanslı hiçbir bileşen kullanılmadı (MuPDF, iText vb. yok).",
            )

            Bolum(
                baslik = "Paketli yazı tipi",
                govde = "Filigran metni, uygulamayla birlikte gelen Noto Sans (statik sürüm) " +
                    "ile yazılır ve PDF'e yalnızca kullanılan harfler gömülür. Böylece " +
                    "ğ, ş, ı gibi harfler her cihazda doğru çıkar.\n\n" +
                    "Noto Sans — Copyright The Noto Project Authors.\n" +
                    "SIL Open Font License 1.1 ile lisanslıdır.\n" +
                    "Lisans metni: assets/fonts/OFL.txt",
            )

            Bolum(
                baslik = "Bilerek yapılmayanlar",
                govde = "• Office → PDF dönüşümü: mobil cihazda güvenilir biçimde yapılamaz. " +
                    "Bunu Microsoft Office uygulamasından yapın.\n" +
                    "• İmza akışı (imzacı davet etme, onay kaydı): sunucu ve kimlik doğrulama " +
                    "gerektirir; çevrimdışı bir telefon uygulamasında anlamsızdır.\n" +
                    "• Hesap, abonelik, telemetri, analitik, bulut senkronu, reklam: yok.\n" +
                    "• PDF'e aranabilir OCR metin katmanı gömme: bu sürümün kapsamı dışında.",
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Uygulama kimligi basligi: ad, surum ve yapimci.
 *
 * Metin secilebilir birakildi (kopyalanabilsin), ama tiklanabilir bir baglanti
 * degil: bu uygulama hicbir sekilde disari cikis yapmaz, tarayici da acmaz.
 */
@Composable
private fun UygulamaKimligi() {
    val baglam = LocalContext.current
    val surum = remember(baglam) {
        runCatching {
            baglam.packageManager.getPackageInfo(baglam.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("PDF Kutusu", style = MaterialTheme.typography.headlineSmall)
            if (surum.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text("Sürüm $surum", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Yapımcı",
                style = MaterialTheme.typography.labelMedium,
            )
            BaglantiMetni(
                metin = "vitrincim.com",
                adres = "https://vitrincim.com",
                stil = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                renk = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.offset(x = (-4).dp),
            )
            Text(
                "Dokunmak tarayıcınızı açar. Uygulama hiçbir veri göndermez.",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun Bolum(baslik: String, govde: String, vurgulu: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (vurgulu) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(baslik, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                govde,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = if (govde.contains("__")) FontFamily.Monospace else FontFamily.Default,
                ),
            )
        }
    }
}
