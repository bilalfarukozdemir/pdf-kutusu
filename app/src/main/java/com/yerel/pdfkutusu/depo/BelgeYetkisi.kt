package com.yerel.pdfkutusu.depo

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Belge basina okuma yetkisi.
 *
 * ## Bu bir uygulama izni degil
 *
 * Uygulamanin manifestinde hicbir izin yok ve olmayacak. Buradaki "yetki",
 * kullanicinin dosya seciciden bir belgeyi secmesiyle sisteme verdigi, yalnizca
 * o belgeyi kapsayan bir haktir. Depolamaya genel erisim vermez.
 *
 * ## Kalici olan ve olmayan
 *
 * `ACTION_OPEN_DOCUMENT` ile secilen belgede yetki **kalici** yapilabilir;
 * sistem bunu yeniden baslatmalar arasinda hatirlar. Baska bir uygulamadan
 * (`ACTION_VIEW`, ornegin e-postadan) gelen belgede ise yetki gecicidir ve
 * kalicilastirilamaz: gonderen uygulama o bayragi vermez. Son acilanlar
 * listesinde bu ayrimi sakli tutmuyoruz, kullaniciya soyluyoruz.
 */
object BelgeYetkisi {

    /** @return kalici yetki alinabildiyse `true`. */
    fun kaliciAl(baglam: Context, uri: Uri): Boolean = runCatching {
        baglam.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        true
    }.getOrDefault(false)

    /**
     * Yetkiyi geri birakir.
     *
     * Listeden dusen her belge icin cagrilmali: sistemin paket basina tuttugu
     * kalici yetki sayisi sinirlidir, birakilmayan yetkiler birikir.
     */
    fun birak(baglam: Context, uri: String) {
        runCatching {
            baglam.contentResolver.releasePersistableUriPermission(
                Uri.parse(uri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }
}
