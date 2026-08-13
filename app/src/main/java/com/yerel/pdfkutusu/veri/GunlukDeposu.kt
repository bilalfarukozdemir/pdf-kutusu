package com.yerel.pdfkutusu.veri

import com.yerel.pdfkutusu.cekirdek.IslemTuru
import com.yerel.pdfkutusu.cekirdek.Ozet
import java.io.File
import kotlinx.coroutines.flow.Flow

/**
 * Islem gunlugu deposu.
 *
 * Gunluge yazmak **hicbir zaman islemi bozmaz**: kayit atilamasa bile
 * kullanicinin ciktisi elindedir. Bu yuzden yazma hatalari yutulur.
 */
class GunlukDeposu(private val dao: IslemGunluguDao) {

    val kayitlar: Flow<List<IslemKaydi>> = dao.tumunuIzle()

    suspend fun tumunuOku(): List<IslemKaydi> = dao.tumunuOku()

    suspend fun kayitSayisi(): Int = runCatching { dao.kayitSayisi() }.getOrDefault(0)

    suspend fun basariliKaydet(
        islem: IslemTuru,
        girdiDosyaAdi: String,
        girdiSha256: String?,
        ciktiDosyasi: File?,
        sayfaSayisi: Int?,
        zaman: Long = System.currentTimeMillis(),
    ) {
        runCatching {
            dao.ekle(
                IslemKaydi(
                    zamanDamgasi = zaman,
                    islemTuru = islem.name,
                    girdiDosyaAdi = girdiDosyaAdi,
                    girdiSha256 = girdiSha256,
                    ciktiSha256 = ciktiDosyasi?.let { if (it.exists()) Ozet.sha256(it) else null },
                    sayfaSayisi = sayfaSayisi,
                    sonuc = IslemSonucu.BASARILI,
                    hataMesaji = null,
                    ciktiDosyaAdi = ciktiDosyasi?.name,
                ),
            )
        }
    }

    suspend fun hataKaydet(
        islem: IslemTuru,
        girdiDosyaAdi: String,
        girdiSha256: String?,
        hataMesaji: String,
        zaman: Long = System.currentTimeMillis(),
    ) {
        runCatching {
            dao.ekle(
                IslemKaydi(
                    zamanDamgasi = zaman,
                    islemTuru = islem.name,
                    girdiDosyaAdi = girdiDosyaAdi,
                    girdiSha256 = girdiSha256,
                    ciktiSha256 = null,
                    sayfaSayisi = null,
                    sonuc = IslemSonucu.HATA,
                    hataMesaji = hataMesaji.take(500),
                    ciktiDosyaAdi = null,
                ),
            )
        }
    }

    /** Gecmisin tamamini siler. @return silinen kayit sayisi */
    suspend fun tumunuTemizle(): Int = runCatching { dao.tumunuTemizle() }.getOrDefault(0)

    /** Yedekleme icin dosyaya yazilabilir duz metin dokum. */
    fun metneCevir(kayitlar: List<IslemKaydi>): String = buildString {
        appendLine("PDF Kutusu - işlem günlüğü dökümü")
        appendLine("Kayıt sayısı: ${kayitlar.size}")
        appendLine()
        appendLine(
            listOf(
                "zaman_damgasi", "islem_turu", "girdi_dosya_adi", "girdi_sha256",
                "cikti_dosya_adi", "cikti_sha256", "sayfa_sayisi", "sonuc", "hata_mesaji",
            ).joinToString("\t"),
        )
        for (kayit in kayitlar) {
            appendLine(
                listOf(
                    kayit.zamanDamgasi.toString(),
                    kayit.islemTuru,
                    kayit.girdiDosyaAdi,
                    kayit.girdiSha256.orEmpty(),
                    kayit.ciktiDosyaAdi.orEmpty(),
                    kayit.ciktiSha256.orEmpty(),
                    kayit.sayfaSayisi?.toString().orEmpty(),
                    kayit.sonuc,
                    kayit.hataMesaji.orEmpty().replace('\t', ' ').replace('\n', ' '),
                ).joinToString("\t"),
            )
        }
    }
}
