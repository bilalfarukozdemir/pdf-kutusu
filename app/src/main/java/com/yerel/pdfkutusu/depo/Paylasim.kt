package com.yerel.pdfkutusu.depo

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Uretilen ciktiyi baska bir uygulamaya gonderme.
 *
 * ## Gizlilik notu
 * Bu, uygulamanin dis dunyaya acilan tek kapisidir ve **her zaman kullanicinin
 * kendi eylemidir**. Uygulama hicbir seyi kendiliginden gondermez; INTERNET
 * izni yoktur. Ancak dosyayi teslim ettiginiz uygulama (WhatsApp, e-posta,
 * bulut deposu...) onu istedigi yere yukleyebilir. Arayuz bunu acikca soyler.
 *
 * Paylasima yalnizca `cikti/` klasoru acilir; kullanicinin sectigi kaynak
 * dosyalarin kopyalari (`calisma/`) ve gecici dosyalar disarida kalir.
 */
object Paylasim {

    /** Manifest'teki `android:authorities` degeriyle ayni olmali. */
    private const val SAGLAYICI_EKI = ".dosyalar"

    fun uri(baglam: Context, dosya: File): Uri = FileProvider.getUriForFile(
        baglam,
        baglam.packageName + SAGLAYICI_EKI,
        dosya,
    )

    fun mimeTuru(dosya: File): String =
        if (dosya.extension.equals("txt", ignoreCase = true)) "text/plain" else "application/pdf"

    /**
     * Paylasim niyeti uretir. Tek dosyada `ACTION_SEND`, birden fazlada
     * `ACTION_SEND_MULTIPLE` kullanilir.
     *
     * @return secici (chooser) niyeti; dosya listesi bossa null
     */
    fun niyet(baglam: Context, dosyalar: List<File>, baslik: String = "Paylaş"): Intent? {
        val gecerli = dosyalar.filter { it.exists() && it.length() > 0 }
        if (gecerli.isEmpty()) return null

        val uriler = gecerli.map { uri(baglam, it) }
        val turler = gecerli.map { mimeTuru(it) }.distinct()
        val ortakTur = if (turler.size == 1) turler.first() else "*/*"

        val gonder = if (uriler.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = ortakTur
                putExtra(Intent.EXTRA_STREAM, uriler.first())
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = ortakTur
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uriler))
            }
        }

        gonder.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        gonder.putExtra(Intent.EXTRA_TITLE, gecerli.first().name)

        return Intent.createChooser(gonder, baslik).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
