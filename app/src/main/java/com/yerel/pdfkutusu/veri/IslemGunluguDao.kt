package com.yerel.pdfkutusu.veri

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Islem gunlugu erisimi.
 *
 * ## Tasarim kurali
 * Bu arayuzde **`@Update` ve `@Delete` isaretli hicbir metot yoktur ve
 * olmayacaktir.** Gunlugun degeri degistirilemez olmasindan gelir.
 *
 * Tek istisna [tumunuTemizle]: kullanici gecmisin tamaminin silinmesini
 * isteyebilir (mahremiyet). Bu, tek tek kayit duzenlemekten farklidir -
 * ya hepsi durur ya hicbiri; secmeli budama yapilamaz.
 */
@Dao
interface IslemGunluguDao {

    @Insert
    suspend fun ekle(kayit: IslemKaydi): Long

    @Query("SELECT * FROM islem_gunlugu ORDER BY zaman_damgasi DESC, kimlik DESC")
    fun tumunuIzle(): Flow<List<IslemKaydi>>

    @Query("SELECT * FROM islem_gunlugu ORDER BY zaman_damgasi DESC, kimlik DESC")
    suspend fun tumunuOku(): List<IslemKaydi>

    @Query("SELECT COUNT(*) FROM islem_gunlugu")
    suspend fun kayitSayisi(): Int

    /** Gecmisin tamamini siler. Secmeli silme bilerek yoktur. */
    @Query("DELETE FROM islem_gunlugu")
    suspend fun tumunuTemizle(): Int
}
