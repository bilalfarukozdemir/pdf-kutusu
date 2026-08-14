package com.yerel.pdfkutusu.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yerel.pdfkutusu.Bagimliliklar
import com.yerel.pdfkutusu.PdfKutusuUygulamasi
import com.yerel.pdfkutusu.depo.BelgeYetkisi
import com.yerel.pdfkutusu.depo.Paylasim
import com.yerel.pdfkutusu.depo.SonAcilanBelge
import com.yerel.pdfkutusu.ui.ekran.OkuyucuEkrani
import com.yerel.pdfkutusu.ui.model.OkuyucuDurumu
import com.yerel.pdfkutusu.ui.model.OkuyucuViewModel
import com.yerel.pdfkutusu.ui.tema.PdfKutusuTemasi
import java.io.File

/**
 * PDF okuyucu penceresi.
 *
 * Baska bir uygulamadan (`ACTION_VIEW`) gelen PDF'ler burada acilir. Ana
 * arac ekrani ile ayri bir aktivite olmasinin sebebi geri tusu davranisi:
 * disaridan gelen belgede geri, cagiran uygulamaya donmeli - arac
 * izgarasina degil.
 */
class OkuyucuAktivite : ComponentActivity() {

    companion object {
        /** Uygulamanin kendi calisma alanindaki bir dosyayi acmak icin. */
        const val EYLEM_YEREL_DOSYA = "com.yerel.pdfkutusu.OKUYUCUDA_AC"
        const val EK_DOSYA_YOLU = "dosya_yolu"

        /**
         * Kayitli bir belgeyi acacak niyeti kurar.
         *
         * Kendi dosyalarimizi `file://` URI'siyle *niyet icinde* gecmiyoruz:
         * Android, uygulamadan cikan her `file://` URI'sinde
         * `FileUriExposedException` firlatir. Bunun yerine yolu duz bir ek
         * olarak tasiyip URI'yi karsi tarafta kuruyoruz.
         */
        fun acmaNiyeti(baglam: Context, uri: String): Intent {
            val ayrisik = Uri.parse(uri)
            return Intent(baglam, OkuyucuAktivite::class.java).apply {
                if (ayrisik.scheme == "file") {
                    action = EYLEM_YEREL_DOSYA
                    putExtra(EK_DOSYA_YOLU, ayrisik.path)
                } else {
                    action = Intent.ACTION_VIEW
                    data = ayrisik
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = gelenUri(intent)
        if (uri == null) {
            finish()
            return
        }

        setContent {
            PdfKutusuTemasi {
                Surface {
                    val baglam = LocalContext.current
                    val bagimliliklar = remember {
                        (baglam.applicationContext as PdfKutusuUygulamasi).bagimliliklar
                    }
                    val gorunum: OkuyucuViewModel = viewModel(
                        factory = viewModelFactory {
                            initializer { OkuyucuViewModel(bagimliliklar) }
                        },
                    )

                    // Ekran dondugunde ViewModel yasamaya devam eder; belgeyi
                    // yeniden acmak bosuna is olur.
                    LaunchedEffect(uri) {
                        if (gorunum.durum.value is OkuyucuDurumu.Bos) {
                            gorunum.ac(uri)
                        }
                    }

                    // Son acilanlara ancak belge gercekten acildiginda
                    // yaziyoruz: acilamayan dosya listeye girmemeli, ayrica
                    // dogru gorunen ad da bu asamada belli oluyor.
                    val durum by gorunum.durum.collectAsStateWithLifecycle()
                    LaunchedEffect(durum) {
                        (durum as? OkuyucuDurumu.Hazir)?.let { hazir ->
                            sonAcilanlaraYaz(bagimliliklar, uri, hazir.gorunenAd)
                        }
                    }

                    OkuyucuEkrani(
                        gorunum = gorunum,
                        geriDon = { finish() },
                        araclardaAc = { dosya ->
                            startActivity(
                                Intent(this@OkuyucuAktivite, AnaAktivite::class.java).apply {
                                    action = AnaAktivite.EYLEM_ARACLARDA_AC
                                    putExtra(AnaAktivite.EK_DOSYA_YOLU, dosya.absolutePath)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                },
                            )
                        },
                        paylas = { dosya ->
                            val niyet = Paylasim.niyet(this@OkuyucuAktivite, listOf(dosya))
                            if (niyet != null) {
                                runCatching { startActivity(niyet) }
                                    .onFailure { gorunum.mesajGoster("Paylaşabilecek bir uygulama bulunamadı.") }
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Ayni pencereye yeni bir belge gelirse bastan baslat; okuyucu
        // durumu tek bir belgeye baglidir.
        setIntent(intent)
        recreate()
    }

    /**
     * Acilan belgeyi son acilanlara yazar; mumkunse kalici yetkisini alir.
     *
     * Listeden dusen kayitlarin yetkisi geri birakilir - sistemin paket basina
     * tuttugu kalici yetki sayisi sinirli.
     */
    private fun sonAcilanlaraYaz(bagimliliklar: Bagimliliklar, uri: Uri, ad: String) {
        val kalici = if (uri.scheme == "file") {
            // Kendi calisma alanimizdaki dosya; yetki diye bir mesele yok.
            true
        } else {
            BelgeYetkisi.kaliciAl(this, uri)
        }
        val dusenler = bagimliliklar.sonAcilanlar.ekle(
            SonAcilanBelge(uri.toString(), ad, System.currentTimeMillis(), kalici),
        )
        dusenler.filter { it.kalici && it.uri.startsWith("content:") }
            .forEach { BelgeYetkisi.birak(this, it.uri) }
    }

    private fun gelenUri(niyet: Intent?): Uri? {
        if (niyet == null) return null
        return when (niyet.action) {
            EYLEM_YEREL_DOSYA -> {
                val yol = niyet.getStringExtra(EK_DOSYA_YOLU) ?: return null
                File(yol).takeIf { it.isFile }?.let(Uri::fromFile)
            }
            Intent.ACTION_VIEW -> niyet.data
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                niyet.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            }
            else -> niyet.data
        }
    }
}

/** ViewModel fabrikasi icin ortak yardimci. */
internal inline fun <reified VM : ViewModel> okuyucuFabrikasi(
    bagimliliklar: Bagimliliklar,
    crossinline olustur: (Bagimliliklar) -> VM,
) = viewModelFactory { initializer { olustur(bagimliliklar) } }
