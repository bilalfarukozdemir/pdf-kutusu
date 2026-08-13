package com.yerel.pdfkutusu.pdf

import androidx.exifinterface.media.ExifInterface

/**
 * Bir EXIF yon etiketinin gorsel karsiligi.
 *
 * Uygulama sirasi onemlidir: **once dondur, sonra yatay aynala.**
 * (`Matrix.setRotate(aci)` ardindan `postScale(-1f, 1f)`.)
 */
data class GorselDonusumu(
    /** Saat yonunde derece: 0, 90, 180 ya da 270. */
    val donusDerecesi: Int,
    /** Donusten sonra yatay aynalama uygulanacak mi. */
    val yatayAyna: Boolean,
) {
    /** Hicbir sey yapmaya gerek yok mu? */
    val kimlikMi: Boolean get() = donusDerecesi == 0 && !yatayAyna

    /** 90 ve 270'te gorselin eni ile boyu yer degistirir. */
    val enBoyTakas: Boolean get() = donusDerecesi == 90 || donusDerecesi == 270
}

/**
 * EXIF `Orientation` etiketini donusume cevirir.
 *
 * Bu **saf** bir esleme: hicbir Android grafik sinifina dokunmaz, bu yuzden
 * sirf JVM birim testiyle dogrulanabilir. Piksellere uygulama isi
 * [ResimdenPdf] icinde, gercek bir `Matrix` ile yapilir ve cihaz testinde
 * dogrulanir.
 *
 * Telefon fotograflarinin cogu sensorden dik gelmez; yon bilgisi EXIF'te
 * durur. Bu etiket goz ardi edilirse PDF sayfalarinin yarisi yan yatar.
 */
object ExifYonu {

    fun donusum(yonEtiketi: Int): GorselDonusumu = when (yonEtiketi) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> GorselDonusumu(0, yatayAyna = true)
        ExifInterface.ORIENTATION_ROTATE_180 -> GorselDonusumu(180, yatayAyna = false)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> GorselDonusumu(180, yatayAyna = true)
        ExifInterface.ORIENTATION_TRANSPOSE -> GorselDonusumu(90, yatayAyna = true)
        ExifInterface.ORIENTATION_ROTATE_90 -> GorselDonusumu(90, yatayAyna = false)
        ExifInterface.ORIENTATION_TRANSVERSE -> GorselDonusumu(270, yatayAyna = true)
        ExifInterface.ORIENTATION_ROTATE_270 -> GorselDonusumu(270, yatayAyna = false)
        // ORIENTATION_NORMAL, ORIENTATION_UNDEFINED ve tanimsiz her deger
        else -> GorselDonusumu(0, yatayAyna = false)
    }
}
