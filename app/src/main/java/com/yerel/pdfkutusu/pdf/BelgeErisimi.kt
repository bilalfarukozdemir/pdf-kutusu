package com.yerel.pdfkutusu.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.yerel.pdfkutusu.cekirdek.PdfHatasi
import java.io.File
import java.io.IOException

/**
 * PDF acma ve kaydetme; parola korumali belgeler dahil.
 *
 * Kural: kaynak URI'ye asla yazilmaz. Buradaki tum fonksiyonlar uygulamaya
 * ozel calisma dizinindeki kopyalar uzerinde calisir.
 */
object BelgeErisimi {

    /**
     * @param parola null ise once parolasiz denenir. Belge sifreliyse
     *   [PdfHatasi.ParolaGerekli] firlatilir ve arayuz parola sorar.
     */
    fun ac(dosya: File, parola: String? = null): PDDocument {
        if (!dosya.exists() || dosya.length() == 0L) {
            throw PdfHatasi.DosyaOkunamadi("Dosya bulunamadı ya da boş: ${dosya.name}")
        }
        return try {
            if (parola.isNullOrEmpty()) PDDocument.load(dosya) else PDDocument.load(dosya, parola)
        } catch (hata: InvalidPasswordException) {
            if (parola.isNullOrEmpty()) throw PdfHatasi.ParolaGerekli() else throw PdfHatasi.ParolaYanlis()
        } catch (hata: IOException) {
            throw PdfHatasi.BozukBelge("PDF açılamadı: ${dosya.name}", hata)
        } catch (hata: OutOfMemoryError) {
            throw PdfHatasi.BozukBelge("Belge bellege sığmadı: ${dosya.name}")
        }
    }

    /** Belge sifreli acildiysa turetilen ciktiyi kaydedebilmek icin sifreleme kaldirilir. */
    fun guvenligiKaldir(belge: PDDocument) {
        if (belge.isEncrypted) {
            belge.isAllSecurityToBeRemoved = true
        }
    }

    /** Belgenin sifreli olup olmadigini acmadan hizlica anlamak icin. */
    fun sifreliMi(dosya: File): Boolean = try {
        PDDocument.load(dosya).use { false }
    } catch (hata: InvalidPasswordException) {
        true
    } catch (hata: Exception) {
        false
    }

    fun sayfaSayisi(dosya: File, parola: String? = null): Int =
        ac(dosya, parola).use { it.numberOfPages }
}
