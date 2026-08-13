package com.yerel.pdfkutusu.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File

data class BelgeOzeti(
    val sayfaSayisi: Int,
    val sifreliydi: Boolean,
    val metinKatmaniVar: Boolean,
    /** Islem oncesi kullaniciya gosterilecek riskler. */
    val uyarilar: List<String>,
)

/**
 * Islem oncesi risk taramasi.
 *
 * Sartname geregi kullaniciya "font, form alani, imza veya sifreleme iceren
 * PDF'lerde cikti bozulabilir" uyarisi gosteriliyor. Genel bir uyari yerine
 * belgede **gercekten** ne oldugunu soylemeyi tercih ettik; boylece uyari
 * her seferinde ayni oldugu icin gorunmez hale gelmiyor.
 */
object BelgeIncelemesi {

    /** Font taramasi icin bakilacak azami sayfa; buyuk belgelerde beklemeyelim. */
    private const val TARANACAK_SAYFA = 8

    fun incele(dosya: File, parola: String? = null): BelgeOzeti =
        BelgeErisimi.ac(dosya, parola).use { belge -> incele(belge) }

    fun incele(belge: PDDocument): BelgeOzeti {
        val uyarilar = mutableListOf<String>()
        val sifreli = runCatching { belge.isEncrypted }.getOrDefault(false)

        if (sifreli) {
            uyarilar += "Belge şifreli. Çıktı şifresiz üretilir; parolayı yeniden koymanız gerekir."
        }

        runCatching {
            val form = belge.documentCatalog?.acroForm
            if (form != null && form.fields.isNotEmpty()) {
                uyarilar += "Belgede ${form.fields.size} form alanı var. " +
                    "Sayfa kopyalayan işlemlerde (böl, sırala, birleştir) form doldurulabilirliği kaybolabilir."
            }
            if (form?.isSignaturesExist == true) {
                uyarilar += "Belgede imza alanı tanımlı."
            }
        }

        runCatching {
            if (belge.signatureDictionaries.isNotEmpty()) {
                uyarilar += "Belge dijital olarak imzalanmış. Herhangi bir değişiklik imzayı geçersiz kılar."
            }
        }

        val gomulmeyenFontlar = gomulmeyenFontlar(belge)
        if (gomulmeyenFontlar.isNotEmpty()) {
            val ornek = gomulmeyenFontlar.take(3).joinToString(", ")
            uyarilar += "Gömülü olmayan yazı tipi var ($ornek). " +
                "Başka bir cihazda açıldığında harfler kayabilir."
        }

        val metinVar = runCatching {
            (0 until minOf(belge.numberOfPages, 3)).any {
                PdfMetinCikarici.metinKatmaniVarMi(belge, it)
            }
        }.getOrDefault(false)

        return BelgeOzeti(
            sayfaSayisi = belge.numberOfPages,
            sifreliydi = sifreli,
            metinKatmaniVar = metinVar,
            uyarilar = uyarilar,
        )
    }

    private fun gomulmeyenFontlar(belge: PDDocument): List<String> {
        val adlar = linkedSetOf<String>()
        runCatching {
            val sinir = minOf(belge.numberOfPages, TARANACAK_SAYFA)
            for (indeks in 0 until sinir) {
                val kaynaklar = belge.getPage(indeks).resources ?: continue
                for (ad in kaynaklar.fontNames.toList()) {
                    val font = runCatching { kaynaklar.getFont(ad) }.getOrNull() ?: continue
                    if (!font.isEmbedded) {
                        adlar += font.name ?: ad.name
                    }
                    if (adlar.size >= 6) return adlar.toList()
                }
            }
        }
        return adlar.toList()
    }
}
