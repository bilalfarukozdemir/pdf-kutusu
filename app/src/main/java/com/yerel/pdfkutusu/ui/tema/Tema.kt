package com.yerel.pdfkutusu.ui.tema

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val AcikPalet = lightColorScheme(
    primary = Color(0xFF4B5BA9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDEE0FF),
    onPrimaryContainer = Color(0xFF00105C),
    secondary = Color(0xFF5B5D72),
    secondaryContainer = Color(0xFFE0E1F9),
    tertiary = Color(0xFF77536D),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    surface = Color(0xFFFBF8FF),
    surfaceVariant = Color(0xFFE3E1EC),
    onSurfaceVariant = Color(0xFF46464F),
    outline = Color(0xFF767680),
)

private val KoyuPalet = darkColorScheme(
    primary = Color(0xFFBAC3FF),
    onPrimary = Color(0xFF1A2678),
    primaryContainer = Color(0xFF333E8F),
    onPrimaryContainer = Color(0xFFDEE0FF),
    secondary = Color(0xFFC4C5DD),
    secondaryContainer = Color(0xFF434559),
    tertiary = Color(0xFFE6BAD7),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surface = Color(0xFF121318),
    surfaceVariant = Color(0xFF46464F),
    onSurfaceVariant = Color(0xFFC7C5D0),
    outline = Color(0xFF90909A),
)

/**
 * Uygulama temasi.
 *
 * Sistem ayarini izler (acik/koyu). Android 12 ve ustunde kullanicinin duvar
 * kagidindan turetilen dinamik renkleri kullanir; altinda kendi paletimize
 * duseriz.
 */
@Composable
fun PdfKutusuTemasi(
    koyuTema: Boolean = isSystemInDarkTheme(),
    dinamikRenk: Boolean = true,
    icerik: @Composable () -> Unit,
) {
    val baglam = LocalContext.current
    val palet = when {
        dinamikRenk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (koyuTema) dynamicDarkColorScheme(baglam) else dynamicLightColorScheme(baglam)

        koyuTema -> KoyuPalet
        else -> AcikPalet
    }

    MaterialTheme(
        colorScheme = palet,
        typography = MaterialTheme.typography,
        content = icerik,
    )
}
