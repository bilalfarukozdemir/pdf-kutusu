package com.yerel.pdfkutusu.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yerel.pdfkutusu.Bagimliliklar
import com.yerel.pdfkutusu.PdfKutusuUygulamasi
import com.yerel.pdfkutusu.depo.Paylasim
import com.yerel.pdfkutusu.ui.ekran.OkuyucuEkrani
import com.yerel.pdfkutusu.ui.model.OkuyucuViewModel
import com.yerel.pdfkutusu.ui.tema.PdfKutusuTemasi

/**
 * PDF okuyucu penceresi.
 *
 * Baska bir uygulamadan (`ACTION_VIEW`) gelen PDF'ler burada acilir. Ana
 * arac ekrani ile ayri bir aktivite olmasinin sebebi geri tusu davranisi:
 * disaridan gelen belgede geri, cagiran uygulamaya donmeli - arac
 * izgarasina degil.
 */
class OkuyucuAktivite : ComponentActivity() {

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
                    androidx.compose.runtime.LaunchedEffect(uri) {
                        if (gorunum.durum.value is com.yerel.pdfkutusu.ui.model.OkuyucuDurumu.Bos) {
                            gorunum.ac(uri)
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

    private fun gelenUri(niyet: Intent?): Uri? {
        if (niyet == null) return null
        return when (niyet.action) {
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
