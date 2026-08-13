package com.yerel.pdfkutusu.cekirdek

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * Islem gunlugunde girdi/cikti dosyalarini kimliklendirmek icin SHA-256.
 *
 * Ozet degeri gunlukte saklanir; boylece "hangi dosyadan hangi dosya ciktı"
 * sorusu dosya adindan bagimsiz olarak yanitlanabilir.
 */
object Ozet {

    fun sha256(dosya: File): String = dosya.inputStream().use { sha256(it) }

    fun sha256(akis: InputStream): String {
        val ozetleyici = MessageDigest.getInstance("SHA-256")
        val tampon = ByteArray(64 * 1024)
        while (true) {
            val okunan = akis.read(tampon)
            if (okunan <= 0) break
            ozetleyici.update(tampon, 0, okunan)
        }
        return onaltilik(ozetleyici.digest())
    }

    fun sha256(baytlar: ByteArray): String =
        onaltilik(MessageDigest.getInstance("SHA-256").digest(baytlar))

    private fun onaltilik(baytlar: ByteArray): String {
        val basamaklar = "0123456789abcdef"
        val cikti = StringBuilder(baytlar.size * 2)
        for (bayt in baytlar) {
            val deger = bayt.toInt() and 0xFF
            cikti.append(basamaklar[deger ushr 4])
            cikti.append(basamaklar[deger and 0x0F])
        }
        return cikti.toString()
    }
}
