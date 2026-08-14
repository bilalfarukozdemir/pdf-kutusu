package com.yerel.pdfkutusu.ui.ortak

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.yerel.pdfkutusu.cekirdek.PdfHatasi
import com.yerel.pdfkutusu.pdf.Ilerleme
import com.yerel.pdfkutusu.ui.model.GirdiOgesi
import com.yerel.pdfkutusu.ui.model.IslemCiktisi
import com.yerel.pdfkutusu.ui.model.ParolaIstegi
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AracIskeleti(
    baslik: String,
    geriDon: (() -> Unit)? = null,
    anlikMesajDurumu: SnackbarHostState = remember { SnackbarHostState() },
    eylemler: @Composable () -> Unit = {},
    icerik: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(baslik, maxLines = 1) },
                navigationIcon = {
                    if (geriDon != null) {
                        IconButton(onClick = geriDon) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    }
                },
                actions = { eylemler() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        snackbarHost = { SnackbarHost(anlikMesajDurumu) },
        content = icerik,
    )
}

/** Bos durum: ekranda henuz veri yok, kullaniciya ne yapacagini soyle. */
@Composable
fun BosDurum(
    simge: ImageVector,
    baslik: String,
    aciklama: String,
    eylemEtiketi: String? = null,
    eylem: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            simge,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(baslik, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            aciklama,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (eylemEtiketi != null && eylem != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = eylem) { Text(eylemEtiketi) }
        }
    }
}

/** Yukleniyor durumu: belirsiz sureli kisa isler icin. */
@Composable
fun YukleniyorSatiri(mesaj: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(12.dp))
        Text(mesaj, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Uzun islem: yuzde, asama adi ve **iptal**. */
@Composable
fun IlerlemeKarti(
    ilerleme: Ilerleme?,
    iptalEt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = ilerleme?.etiket?.takeIf { it.isNotBlank() } ?: "İşleniyor…",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                if (ilerleme != null && ilerleme.toplam > 0) {
                    Text(
                        "${ilerleme.tamamlanan}/${ilerleme.toplam}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            if (ilerleme != null && ilerleme.toplam > 0) {
                LinearProgressIndicator(
                    progress = { ilerleme.oran },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = iptalEt) {
                Icon(Icons.Default.Close, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("İptal et")
            }
        }
    }
}

/** Kurtarilabilir hata: ekran acik kalir, kullanici duzeltip tekrar dener. */
@Composable
fun HataKarti(
    hata: PdfHatasi,
    kapat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(Modifier.padding(16.dp)) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(hata.kullaniciMesaji, style = MaterialTheme.typography.titleSmall)
                if (hata.oneri != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(hata.oneri, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = kapat) {
                Icon(Icons.Default.Close, contentDescription = "Kapat")
            }
        }
    }
}

/**
 * Basarili durum: cikti dosyalari, yeniden adlandirma, paylasma, disa aktarma.
 *
 * Tek dosyalik ciktilarda ad dogrudan burada duzenlenebilir: kullanici
 * kaydetmeden ya da paylasmadan **once** adi duzeltebilsin. Ad diskte de
 * degisir, boylece Dosyalar ekraninda ayni adla gorunur.
 */
@Composable
fun SonucKarti(
    sonuc: IslemCiktisi,
    kaydet: (File) -> Unit,
    paylas: (List<File>) -> Unit,
    kapat: () -> Unit,
    modifier: Modifier = Modifier,
    tumunuKaydet: (() -> Unit)? = null,
    yenidenAdlandir: ((File, String) -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("İşlem tamamlandı", style = MaterialTheme.typography.titleSmall)
                    if (sonuc.ozetSatiri.isNotBlank()) {
                        Text(sonuc.ozetSatiri, style = MaterialTheme.typography.bodySmall)
                    }
                }
                IconButton(onClick = kapat) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat")
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            val tekDosya = sonuc.dosyalar.singleOrNull()

            if (tekDosya != null) {
                if (yenidenAdlandir != null) {
                    DosyaAdiDuzenleyici(
                        dosya = tekDosya,
                        yenidenAdlandir = { yeniAd -> yenidenAdlandir(tekDosya, yeniAd) },
                    )
                } else {
                    Text(
                        tekDosya.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Text(
                    bicimliBoyut(tekDosya.length()),
                    style = MaterialTheme.typography.labelSmall,
                )
            } else {
                sonuc.dosyalar.forEach { dosya ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                dosya.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                bicimliBoyut(dosya.length()),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        TextButton(onClick = { kaydet(dosya) }) {
                            Icon(Icons.Default.Save, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Kaydet")
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { paylas(sonuc.dosyalar) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (tekDosya != null) "Paylaş" else "Hepsini paylaş")
                }
                if (tekDosya != null) {
                    OutlinedButton(
                        onClick = { kaydet(tekDosya) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Kaydet")
                    }
                }
            }

            if (sonuc.dosyalar.size > 1 && tumunuKaydet != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = tumunuKaydet, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Folder, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Tümünü bir klasöre aktar")
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "\"Kaydet\" dosyayı seçtiğiniz konuma yazar ve cihazda kalır. " +
                    "\"Paylaş\" ise dosyayı başka bir uygulamaya teslim eder — o uygulama " +
                    "onu istediği yere gönderebilir.",
                style = MaterialTheme.typography.labelSmall,
            )

            if (sonuc.notlar.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                sonuc.notlar.forEach { not ->
                    Text(
                        "• $not",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
        }
    }
}

/**
 * Cikti adini yerinde duzenleme.
 *
 * Uzanti ayri gosterilir ve degistirilemez; kullanici yanlislikla ".pdf"yi
 * silip dosyayi acilamaz hale getirmesin. Ad ancak onaylandiginda (✓)
 * diskte degisir.
 */
@Composable
private fun DosyaAdiDuzenleyici(
    dosya: File,
    yenidenAdlandir: (String) -> Unit,
) {
    val uzanti = dosya.extension
    var ad by remember(dosya.absolutePath) { mutableStateOf(dosya.nameWithoutExtension) }
    val degisti = ad.trim().isNotBlank() && ad.trim() != dosya.nameWithoutExtension

    OutlinedTextField(
        value = ad,
        onValueChange = { ad = it },
        label = { Text("Dosya adı") },
        singleLine = true,
        suffix = if (uzanti.isNotEmpty()) {
            { Text(".$uzanti") }
        } else {
            null
        },
        trailingIcon = {
            if (degisti) {
                IconButton(onClick = { yenidenAdlandir(ad.trim()) }) {
                    Icon(Icons.Default.Check, contentDescription = "Yeni adı onayla")
                }
            }
        },
        supportingText = {
            Text(
                if (degisti) {
                    "Onaylamak için ✓ dokunun"
                } else {
                    "Kaydetmeden ya da paylaşmadan önce adı değiştirebilirsiniz"
                },
            )
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Islem oncesi risk uyarilari. */
@Composable
fun UyariKarti(uyarilar: List<String>, modifier: Modifier = Modifier) {
    if (uyarilar.isEmpty()) return
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Row(Modifier.padding(16.dp)) {
            Icon(Icons.Default.Warning, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("İşlem öncesi dikkat", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                uyarilar.forEach {
                    Text("• $it", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun GirdiKarti(
    oge: GirdiOgesi,
    kaldir: () -> Unit,
    yukari: (() -> Unit)? = null,
    asagi: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    oge.gorunenAd,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${oge.sayfaSayisi} sayfa · ${bicimliBoyut(oge.boyut)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (oge.sifresiKaldirildi) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Şifresi çözüldü",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (yukari != null) {
                IconButton(onClick = yukari) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Yukarı taşı")
                }
            }
            if (asagi != null) {
                IconButton(onClick = asagi) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Aşağı taşı")
                }
            }
            IconButton(onClick = kaldir) {
                Icon(Icons.Default.Delete, contentDescription = "Listeden çıkar")
            }
        }
    }
}

@Composable
fun ParolaDiyalogu(
    istek: ParolaIstegi,
    hataMesaji: String?,
    gonder: (String) -> Unit,
    iptal: () -> Unit,
) {
    var parola by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = iptal,
        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
        title = { Text("Parola gerekli") },
        text = {
            Column {
                Text(
                    "\"${istek.gorunenAd}\" şifreli. Açmak için belgenin parolasını girin.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = parola,
                    onValueChange = { parola = it },
                    label = { Text("Parola") },
                    singleLine = true,
                    isError = hataMesaji != null,
                    supportingText = hataMesaji?.let { { Text(it) } },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Parola yalnızca bu belgeyi açmak için kullanılır, hiçbir yere kaydedilmez.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = { gonder(parola) }, enabled = parola.isNotEmpty()) { Text("Aç") }
        },
        dismissButton = { TextButton(onClick = iptal) { Text("Vazgeç") } },
    )
}

@Composable
fun AralikGirisi(
    deger: String,
    degisti: (String) -> Unit,
    toplamSayfa: Int,
    etiket: String = "Sayfa aralığı",
    etkin: Boolean = true,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = deger,
        onValueChange = degisti,
        label = { Text(etiket) },
        enabled = etkin,
        singleLine = true,
        placeholder = { Text("örn. 1-3, 5, 8-") },
        supportingText = {
            Text("Belge $toplamSayfa sayfa. Virgülle ayırın; \"3-\" son sayfaya kadar demektir.")
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Tiklanabilir baglanti metni.
 *
 * Uygulamanin disariya acilan tek noktasi. `INTERNET` izni gerektirmez: agi
 * kullanicinin tarayicisi kullanir, bu uygulama hicbir sey indirmez ve
 * gondermez. Dokunma kullanicinin kendi eylemidir; belge verisi tasinmaz.
 *
 * Tarayici yoksa (kiosk cihazlar) sessizce hicbir sey yapmaz, cokmez.
 */
@Composable
fun BaglantiMetni(
    metin: String,
    adres: String,
    modifier: Modifier = Modifier,
    stil: TextStyle = MaterialTheme.typography.bodyMedium,
    renk: Color = MaterialTheme.colorScheme.primary,
) {
    val acici = LocalUriHandler.current
    Text(
        text = metin,
        style = stil.copy(textDecoration = TextDecoration.Underline),
        color = renk,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(
                role = Role.Button,
                onClickLabel = "$metin adresini tarayıcıda aç",
            ) { runCatching { acici.openUri(adres) } }
            .padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

@Composable
fun BolumBasligi(metin: String, modifier: Modifier = Modifier) {
    Text(
        metin,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
fun DurumRozeti(metin: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(metin, style = MaterialTheme.typography.labelSmall)
    }
}
