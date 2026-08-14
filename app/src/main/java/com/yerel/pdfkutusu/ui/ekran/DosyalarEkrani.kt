package com.yerel.pdfkutusu.ui.ekran

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yerel.pdfkutusu.depo.Paylasim
import com.yerel.pdfkutusu.ui.model.DosyalarViewModel
import com.yerel.pdfkutusu.ui.ortak.AracIskeleti
import com.yerel.pdfkutusu.ui.ortak.BosDurum
import com.yerel.pdfkutusu.ui.ortak.YukleniyorSatiri
import com.yerel.pdfkutusu.ui.ortak.bicimliBoyut
import com.yerel.pdfkutusu.ui.ortak.bicimliZaman
import java.io.File

@Composable
fun DosyalarEkrani(gorunum: DosyalarViewModel, geriDon: () -> Unit) {
    val dosyalar by gorunum.dosyalar.collectAsStateWithLifecycle()
    val yukleniyor by gorunum.yukleniyor.collectAsStateWithLifecycle()
    val bilgi by gorunum.bilgi.collectAsStateWithLifecycle()
    val anlikMesaj = remember { SnackbarHostState() }
    val baglam = LocalContext.current
    var kaydedilecek by remember { mutableStateOf<File?>(null) }
    var silmeSorusu by remember { mutableStateOf(false) }

    val pdfKaydedici = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { hedef ->
        val kaynak = kaydedilecek
        if (hedef != null && kaynak != null) gorunum.disaAktar(kaynak, hedef)
        kaydedilecek = null
    }
    val metinKaydedici = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { hedef ->
        val kaynak = kaydedilecek
        if (hedef != null && kaynak != null) gorunum.disaAktar(kaynak, hedef)
        kaydedilecek = null
    }
    val klasorSecici = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { agac -> if (agac != null) gorunum.tumunuDisaAktar(agac) }

    LaunchedEffect(bilgi) {
        val mesaj = bilgi
        if (mesaj != null) {
            anlikMesaj.showSnackbar(mesaj)
            gorunum.bilgiyiKapat()
        }
    }

    val kaydet: (File) -> Unit = { dosya ->
        kaydedilecek = dosya
        if (dosya.extension.equals("txt", true)) {
            metinKaydedici.launch(dosya.name)
        } else {
            pdfKaydedici.launch(dosya.name)
        }
    }

    AracIskeleti(
        baslik = "Üretilen dosyalar",
        geriDon = geriDon,
        anlikMesajDurumu = anlikMesaj,
        eylemler = {
            IconButton(onClick = gorunum::yenile) {
                Icon(Icons.Default.Refresh, contentDescription = "Yenile")
            }
            if (dosyalar.isNotEmpty()) {
                IconButton(onClick = { silmeSorusu = true }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Tümünü sil")
                }
            }
        },
    ) { doldurma ->
        Column(Modifier.fillMaxSize().padding(doldurma)) {
            if (yukleniyor) YukleniyorSatiri("Yükleniyor…")

            if (dosyalar.isEmpty() && !yukleniyor) {
                BosDurum(
                    simge = Icons.Default.Folder,
                    baslik = "Henüz dosya yok",
                    aciklama = "Bir araç çalıştırdığınızda çıktılar burada birikir. " +
                        "Dosyalar uygulamaya özel alanda durur; dışa aktarmadıkça " +
                        "başka uygulamalar göremez.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        OutlinedButton(
                            onClick = { klasorSecici.launch(null) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.FolderOpen, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Tümünü bir klasöre aktar (${dosyalar.size})")
                        }
                    }
                    items(dosyalar, key = { it.dosya.absolutePath }) { oge ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    top = 8.dp,
                                    bottom = 8.dp,
                                    end = 4.dp,
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        oge.ad,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        "${bicimliBoyut(oge.boyut)} · " +
                                            bicimliZaman(oge.degistirilmeZamani),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = {
                                    val niyet = Paylasim.niyet(baglam, listOf(oge.dosya))
                                    if (niyet != null) {
                                        runCatching { baglam.startActivity(niyet) }
                                    }
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "Paylaş")
                                }
                                IconButton(onClick = { kaydet(oge.dosya) }) {
                                    Icon(Icons.Default.Save, contentDescription = "Dışa aktar")
                                }
                                IconButton(onClick = { gorunum.sil(oge.dosya) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Sil")
                                }
                            }
                        }
                    }
                    item {
                        TextButton(
                            onClick = gorunum::calismaAlaniniTemizle,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Geçici çalışma dosyalarını temizle")
                        }
                    }
                }
            }
        }
    }

    if (silmeSorusu) {
        AlertDialog(
            onDismissRequest = { silmeSorusu = false },
            title = { Text("Tüm çıktılar silinsin mi?") },
            text = {
                Text(
                    "${dosyalar.size} dosya kalıcı olarak silinecek. " +
                        "Daha önce dışa aktardıklarınız etkilenmez.",
                )
            },
            confirmButton = {
                Button(onClick = {
                    gorunum.tumunuSil()
                    silmeSorusu = false
                }) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { silmeSorusu = false }) { Text("Vazgeç") }
            },
        )
    }
}
