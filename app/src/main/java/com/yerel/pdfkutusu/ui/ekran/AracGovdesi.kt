package com.yerel.pdfkutusu.ui.ekran

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yerel.pdfkutusu.depo.Paylasim
import com.yerel.pdfkutusu.ui.model.AracViewModel
import com.yerel.pdfkutusu.ui.ortak.AracIskeleti
import com.yerel.pdfkutusu.ui.ortak.BosDurum
import com.yerel.pdfkutusu.ui.ortak.GirdiKarti
import com.yerel.pdfkutusu.ui.ortak.HataKarti
import com.yerel.pdfkutusu.ui.ortak.IlerlemeKarti
import com.yerel.pdfkutusu.ui.ortak.ParolaDiyalogu
import com.yerel.pdfkutusu.ui.ortak.SonucKarti
import com.yerel.pdfkutusu.ui.ortak.UyariKarti
import com.yerel.pdfkutusu.ui.ortak.YukleniyorSatiri
import java.io.File

/**
 * Tum arac ekranlarinin ortak govdesi.
 *
 * Sartnamedeki dort durumu tek yerde ele alir - boylece her arac ekraninda
 * ayni davranisi elde ediyoruz:
 *  - **bos**        : dosya secilmemis, ne yapilacagini anlatan bos durum
 *  - **yukleniyor** : dosya aliniyor ya da islem suruyor (+ iptal)
 *  - **basarili**   : cikti karti, kaydet / tumunu kaydet
 *  - **hata**       : kapatilabilir hata karti, ekran acik kalir
 */
@Composable
fun AracGovdesi(
    baslik: String,
    bosBaslik: String,
    bosAciklama: String,
    simge: ImageVector,
    gorunum: AracViewModel,
    geriDon: () -> Unit,
    calistirEtiketi: String,
    calistir: () -> Unit,
    modifier: Modifier = Modifier,
    cokluSecim: Boolean = false,
    calistirEtkin: Boolean = true,
    /** SAF secicisine verilecek MIME turleri. */
    mimeTurleri: Array<String> = arrayOf("application/pdf"),
    secButonuEtiketi: String = if (cokluSecim) "PDF'leri seç" else "PDF seç",
    ekleButonuEtiketi: String = if (cokluSecim) "PDF ekle" else "Başka PDF seç",
    secenekler: @Composable ColumnScope.() -> Unit = {},
) {
    val durum by gorunum.durum.collectAsStateWithLifecycle()
    val anlikMesaj = remember { SnackbarHostState() }
    var kaydedilecek by remember { mutableStateOf<File?>(null) }

    val pdfSecici = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uriler -> gorunum.dosyalariEkle(uriler) }

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

    LaunchedEffect(durum.bilgi) {
        val mesaj = durum.bilgi
        if (mesaj != null) {
            anlikMesaj.showSnackbar(mesaj)
            gorunum.bilgiyiKapat()
        }
    }

    val baglam = LocalContext.current
    val paylas: (List<File>) -> Unit = { dosyalar ->
        val niyet = Paylasim.niyet(baglam, dosyalar)
        if (niyet == null) {
            gorunum.mesajGoster("Paylaşılacak dosya bulunamadı.")
        } else {
            runCatching { baglam.startActivity(niyet) }.onFailure {
                gorunum.mesajGoster("Paylaşabilecek bir uygulama bulunamadı.")
            }
        }
    }

    val dosyaSec = { pdfSecici.launch(mimeTurleri) }
    val dosyaKaydet: (File) -> Unit = { dosya ->
        kaydedilecek = dosya
        if (dosya.extension.equals("txt", ignoreCase = true)) {
            metinKaydedici.launch(dosya.name)
        } else {
            pdfKaydedici.launch(dosya.name)
        }
    }

    AracIskeleti(
        baslik = baslik,
        geriDon = geriDon,
        anlikMesajDurumu = anlikMesaj,
        eylemler = {
            if (durum.girdiler.isNotEmpty()) {
                IconButton(onClick = { gorunum.tumunuTemizle() }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Seçimi temizle")
                }
            }
        },
    ) { doldurma ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(doldurma)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            if (durum.bosMu) {
                BosDurum(
                    simge = simge,
                    baslik = bosBaslik,
                    aciklama = bosAciklama,
                    eylemEtiketi = secButonuEtiketi,
                    eylem = dosyaSec,
                )
            } else {
                // -------------------------------------------------- girdiler
                durum.girdiler.forEachIndexed { indeks, oge ->
                    GirdiKarti(
                        oge = oge,
                        kaldir = { gorunum.girdiKaldir(oge.dosya) },
                        yukari = if (cokluSecim && indeks > 0) {
                            { gorunum.girdiTasi(indeks, indeks - 1) }
                        } else {
                            null
                        },
                        asagi = if (cokluSecim && indeks < durum.girdiler.lastIndex) {
                            { gorunum.girdiTasi(indeks, indeks + 1) }
                        } else {
                            null
                        },
                    )
                }

                OutlinedButton(onClick = dosyaSec, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(ekleButonuEtiketi)
                }

                if (durum.dosyaYukleniyor) {
                    YukleniyorSatiri("Dosya alınıyor…")
                }

                UyariKarti(durum.uyarilar)

                // -------------------------------------------------- secenekler
                secenekler()

                // -------------------------------------------------- calistir
                if (durum.calisiyor) {
                    IlerlemeKarti(durum.ilerleme, gorunum::iptalEt)
                } else {
                    Button(
                        onClick = calistir,
                        enabled = calistirEtkin && !durum.mesgulMu,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(calistirEtiketi)
                    }
                }
            }

            // ------------------------------------------------------ sonuclar
            durum.hata?.let { hata ->
                HataKarti(hata = hata, kapat = gorunum::hatayiKapat)
            }

            durum.sonuc?.let { sonuc ->
                SonucKarti(
                    sonuc = sonuc,
                    kaydet = dosyaKaydet,
                    paylas = paylas,
                    kapat = gorunum::sonucuKapat,
                    tumunuKaydet = { klasorSecici.launch(null) },
                    yenidenAdlandir = gorunum::ciktiyiYenidenAdlandir,
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    durum.parolaIstegi?.let { istek ->
        ParolaDiyalogu(
            istek = istek,
            hataMesaji = durum.parolaHatasi,
            gonder = gorunum::parolaGonder,
            iptal = gorunum::parolayiIptalEt,
        )
    }
}

/** Secenek bolumlerini gruplayan kart. */
@Composable
fun SecenekKarti(
    baslik: String,
    modifier: Modifier = Modifier,
    icerik: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(baslik, style = MaterialTheme.typography.titleSmall)
            icerik()
        }
    }
}
