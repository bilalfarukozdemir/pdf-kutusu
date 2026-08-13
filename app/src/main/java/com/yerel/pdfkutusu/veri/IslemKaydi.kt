package com.yerel.pdfkutusu.veri

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Islem sonucu. Metin olarak saklanir; ileride yeni deger eklemek kolay olsun. */
object IslemSonucu {
    const val BASARILI = "BASARILI"
    const val HATA = "HATA"
}

/**
 * Salt-ekleme islem gunlugu.
 *
 * Bu tablodaki satirlar **hicbir zaman guncellenmez**. Bir islem yapildi mi,
 * kaydi oldugu gibi kalir. Kullanici gunlugun tamamini temizleyebilir ama
 * tek bir satiri duzenleyemez ya da silemez - gunlugu "duzeltilebilir" hale
 * getirmek onu ise yaramaz kilardi.
 *
 * `girdi_sha256` ve `cikti_sha256` dosya adindan bagimsiz kimliktir: dosyayi
 * sonradan yeniden adlandirsaniz bile hangi girdiden hangi ciktinin
 * uretildigini eslestirebilirsiniz.
 */
@Entity(tableName = "islem_gunlugu")
data class IslemKaydi(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "kimlik")
    val kimlik: Long = 0,

    /** Unix epoch milisaniye. */
    @ColumnInfo(name = "zaman_damgasi")
    val zamanDamgasi: Long,

    /** [com.yerel.pdfkutusu.cekirdek.IslemTuru] enum adi. */
    @ColumnInfo(name = "islem_turu")
    val islemTuru: String,

    @ColumnInfo(name = "girdi_dosya_adi")
    val girdiDosyaAdi: String,

    @ColumnInfo(name = "girdi_sha256")
    val girdiSha256: String?,

    @ColumnInfo(name = "cikti_sha256")
    val ciktiSha256: String?,

    /** Ciktinin sayfa sayisi; hata durumunda null. */
    @ColumnInfo(name = "sayfa_sayisi")
    val sayfaSayisi: Int?,

    /** [IslemSonucu.BASARILI] ya da [IslemSonucu.HATA]. */
    @ColumnInfo(name = "sonuc")
    val sonuc: String,

    @ColumnInfo(name = "hata_mesaji")
    val hataMesaji: String?,

    /**
     * Sartnamedeki alan listesine ek. Gunlukten dosyayi bulabilmek pratikte
     * sart oldugu icin ekledik; salt-ekleme kuralini degistirmez.
     */
    @ColumnInfo(name = "cikti_dosya_adi")
    val ciktiDosyaAdi: String?,
) {
    val basariliMi: Boolean get() = sonuc == IslemSonucu.BASARILI
}
