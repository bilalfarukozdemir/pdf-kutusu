package com.yerel.pdfkutusu.veri

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [IslemKaydi::class],
    version = 1,
    exportSchema = true,
)
abstract class PdfVeritabani : RoomDatabase() {

    abstract fun gunlukDao(): IslemGunluguDao

    companion object {
        private const val DOSYA_ADI = "pdf_kutusu.db"

        @Volatile
        private var ornek: PdfVeritabani? = null

        fun al(baglam: Context): PdfVeritabani =
            ornek ?: synchronized(this) {
                ornek ?: Room.databaseBuilder(
                    baglam.applicationContext,
                    PdfVeritabani::class.java,
                    DOSYA_ADI,
                ).build().also { ornek = it }
            }
    }
}
