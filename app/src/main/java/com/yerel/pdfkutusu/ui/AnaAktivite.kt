package com.yerel.pdfkutusu.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yerel.pdfkutusu.Bagimliliklar
import com.yerel.pdfkutusu.PdfKutusuUygulamasi
import com.yerel.pdfkutusu.depo.BelgeYetkisi
import com.yerel.pdfkutusu.ui.ekran.AnaEkran
import com.yerel.pdfkutusu.ui.ekran.BirlestirEkrani
import com.yerel.pdfkutusu.ui.ekran.BolEkrani
import com.yerel.pdfkutusu.ui.ekran.DondurEkrani
import com.yerel.pdfkutusu.ui.ekran.DosyalarEkrani
import com.yerel.pdfkutusu.ui.ekran.FiligranEkrani
import com.yerel.pdfkutusu.ui.ekran.GunlukEkrani
import com.yerel.pdfkutusu.ui.ekran.HakkindaEkrani
import com.yerel.pdfkutusu.ui.ekran.IlkAcilisUyarisi
import com.yerel.pdfkutusu.ui.ekran.KarartEkrani
import com.yerel.pdfkutusu.ui.ekran.OcrEkrani
import com.yerel.pdfkutusu.ui.ekran.ResimdenPdfEkrani
import com.yerel.pdfkutusu.ui.ekran.Rotalar
import com.yerel.pdfkutusu.ui.ekran.SikistirEkrani
import com.yerel.pdfkutusu.ui.ekran.SiralaEkrani
import com.yerel.pdfkutusu.ui.model.BirlestirViewModel
import com.yerel.pdfkutusu.ui.model.BolViewModel
import com.yerel.pdfkutusu.ui.model.DondurViewModel
import com.yerel.pdfkutusu.ui.model.DosyalarViewModel
import com.yerel.pdfkutusu.ui.model.FiligranViewModel
import com.yerel.pdfkutusu.ui.model.GunlukViewModel
import com.yerel.pdfkutusu.ui.model.KarartViewModel
import com.yerel.pdfkutusu.ui.model.OcrViewModel
import com.yerel.pdfkutusu.ui.model.ResimdenPdfViewModel
import com.yerel.pdfkutusu.ui.model.SikistirViewModel
import com.yerel.pdfkutusu.ui.model.SiralaViewModel
import com.yerel.pdfkutusu.ui.tema.PdfKutusuTemasi
import java.io.File

class AnaAktivite : ComponentActivity() {

    companion object {
        const val EYLEM_ARACLARDA_AC = "com.yerel.pdfkutusu.ARACLARDA_AC"
        const val EK_DOSYA_YOLU = "dosya_yolu"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        okuyucudanGeleni_al(intent)
        setContent {
            PdfKutusuTemasi {
                Surface(modifier = Modifier) {
                    Uygulama()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        okuyucudanGeleni_al(intent)
    }

    /**
     * Okuyucudan devredilen belgeyi bekleyen kutuya koyar. Kullanici bir arac
     * sectiginde o arac belgeyi kendiliginden yukler.
     */
    private fun okuyucudanGeleni_al(niyet: Intent?) {
        val kutu = (applicationContext as PdfKutusuUygulamasi).bagimliliklar.bekleyenGirdi

        if (niyet?.action != EYLEM_ARACLARDA_AC) {
            // Normal acilis: onceki oturumdan kalan belge kendiliginden
            // yuklenmesin.
            kutu.temizle()
            return
        }
        val yol = niyet.getStringExtra(EK_DOSYA_YOLU) ?: return
        val dosya = File(yol)
        if (!dosya.isFile) return
        kutu.koy(dosya, dosya.name)
    }
}

@Composable
private fun Uygulama() {
    val baglam = LocalContext.current
    val bagimliliklar = remember {
        (baglam.applicationContext as PdfKutusuUygulamasi).bagimliliklar
    }
    val gezinme = rememberNavController()
    var uyariGoster by remember { mutableStateOf(!bagimliliklar.tercihler.uyariOnaylandi) }

    UygulamaGezinmesi(gezinme, bagimliliklar)

    if (uyariGoster) {
        IlkAcilisUyarisi(
            onayla = {
                bagimliliklar.tercihler.uyariOnaylandi = true
                uyariGoster = false
            },
        )
    }
}

@Composable
private fun UygulamaGezinmesi(gezinme: NavHostController, bagimliliklar: Bagimliliklar) {
    val geriDon: () -> Unit = { gezinme.popBackStack() }

    NavHost(navController = gezinme, startDestination = Rotalar.ANA) {
        composable(Rotalar.ANA) {
            var bekleyenAd by remember {
                mutableStateOf(bagimliliklar.bekleyenGirdi.adi())
            }
            val baglam = LocalContext.current
            var sonAcilanlar by remember {
                mutableStateOf(bagimliliklar.sonAcilanlar.listele())
            }

            // Okuyucudan donunce liste tazelensin: yeni acilan belge basta
            // gorunmeli.
            val yasamDongusu = LocalLifecycleOwner.current
            DisposableEffect(yasamDongusu) {
                val gozlemci = LifecycleEventObserver { _, olay ->
                    if (olay == Lifecycle.Event.ON_START) {
                        sonAcilanlar = bagimliliklar.sonAcilanlar.listele()
                    }
                }
                yasamDongusu.lifecycle.addObserver(gozlemci)
                onDispose { yasamDongusu.lifecycle.removeObserver(gozlemci) }
            }

            val secici = rememberLauncherForActivityResult(
                // GetContent degil OpenDocument: yalnizca bu sozlesme kalici
                // yetki verebiliyor, son acilanlar listesi buna dayaniyor.
                ActivityResultContracts.OpenDocument(),
            ) { secilen ->
                if (secilen != null) {
                    baglam.startActivity(
                        OkuyucuAktivite.acmaNiyeti(baglam, secilen.toString()),
                    )
                }
            }

            AnaEkran(
                gecis = { rota -> gezinme.navigate(rota) },
                bekleyenBelge = bekleyenAd,
                bekleyeniBirak = {
                    bagimliliklar.bekleyenGirdi.temizle()
                    bekleyenAd = null
                },
                sonAcilanlar = sonAcilanlar,
                pdfAc = { secici.launch(arrayOf("application/pdf")) },
                sonAcilaniAc = { belge ->
                    baglam.startActivity(OkuyucuAktivite.acmaNiyeti(baglam, belge.uri))
                },
                sonAcilaniSil = { belge ->
                    bagimliliklar.sonAcilanlar.sil(belge.uri)
                    if (belge.kalici && belge.uri.startsWith("content:")) {
                        BelgeYetkisi.birak(baglam, belge.uri)
                    }
                    sonAcilanlar = bagimliliklar.sonAcilanlar.listele()
                },
                sonAcilanlariTemizle = {
                    bagimliliklar.sonAcilanlar.temizle()
                        .filter { it.kalici && it.uri.startsWith("content:") }
                        .forEach { BelgeYetkisi.birak(baglam, it.uri) }
                    sonAcilanlar = bagimliliklar.sonAcilanlar.listele()
                },
            )
        }
        composable(Rotalar.BIRLESTIR) {
            BirlestirEkrani(araci(bagimliliklar) { BirlestirViewModel(it) }, geriDon)
        }
        composable(Rotalar.BOL) {
            BolEkrani(araci(bagimliliklar) { BolViewModel(it) }, geriDon)
        }
        composable(Rotalar.SIRALA) {
            SiralaEkrani(araci(bagimliliklar) { SiralaViewModel(it) }, geriDon)
        }
        composable(Rotalar.DONDUR) {
            DondurEkrani(araci(bagimliliklar) { DondurViewModel(it) }, geriDon)
        }
        composable(Rotalar.SIKISTIR) {
            SikistirEkrani(araci(bagimliliklar) { SikistirViewModel(it) }, geriDon)
        }
        composable(Rotalar.FILIGRAN) {
            FiligranEkrani(araci(bagimliliklar) { FiligranViewModel(it) }, geriDon)
        }
        composable(Rotalar.KARART) {
            KarartEkrani(araci(bagimliliklar) { KarartViewModel(it) }, geriDon)
        }
        composable(Rotalar.OCR) {
            OcrEkrani(araci(bagimliliklar) { OcrViewModel(it) }, geriDon)
        }
        composable(Rotalar.RESIMDEN_PDF) {
            ResimdenPdfEkrani(araci(bagimliliklar) { ResimdenPdfViewModel(it) }, geriDon)
        }
        composable(Rotalar.GUNLUK) {
            GunlukEkrani(araci(bagimliliklar) { GunlukViewModel(it) }, geriDon)
        }
        composable(Rotalar.DOSYALAR) {
            DosyalarEkrani(araci(bagimliliklar) { DosyalarViewModel(it) }, geriDon)
        }
        composable(Rotalar.HAKKINDA) {
            HakkindaEkrani(geriDon)
        }
    }
}

/**
 * Elle kurulan bagimlilik kabindan ViewModel uretir.
 * Hilt/Dagger eklemeden ayni isi goren en kisa yol.
 */
@Composable
private inline fun <reified VM : ViewModel> araci(
    bagimliliklar: Bagimliliklar,
    crossinline olustur: (Bagimliliklar) -> VM,
): VM = viewModel(
    factory = viewModelFactory {
        initializer { olustur(bagimliliklar) }
    },
)
