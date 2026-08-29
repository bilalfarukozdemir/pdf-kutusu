package com.yerel.pdfkutusu.ui.model

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yerel.pdfkutusu.Bagimliliklar
import com.yerel.pdfkutusu.R
import com.yerel.pdfkutusu.veri.IslemKaydi
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GunlukViewModel(private val bagimliliklar: Bagimliliklar) : ViewModel() {

    val kayitlar: StateFlow<List<IslemKaydi>> = bagimliliklar.gunluk.kayitlar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _bilgi = MutableStateFlow<String?>(null)
    val bilgi: StateFlow<String?> = _bilgi.asStateFlow()

    private val _dokumDosyasi = MutableStateFlow<File?>(null)
    val dokumDosyasi: StateFlow<File?> = _dokumDosyasi.asStateFlow()

    fun tumunuTemizle() {
        viewModelScope.launch {
            val silinen = bagimliliklar.gunluk.tumunuTemizle()
            _bilgi.value = bagimliliklar.uygulamaBaglami.getString(R.string.gunluk_silindi, silinen)
        }
    }

    /** Yedekleme icin gunlugun tamamini metin dosyasina yazar. */
    fun dokumHazirla() {
        viewModelScope.launch {
            val baglam = bagimliliklar.uygulamaBaglami
            val dosya = withContext(Dispatchers.IO) {
                runCatching {
                    val kayitlar = bagimliliklar.gunluk.tumunuOku()
                    val zaman = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT))
                    val hedef = bagimliliklar.calismaAlani.ciktiDosyasi(
                        baglam.getString(R.string.gunluk_dosya_adi, zaman),
                    )
                    hedef.writeText(bagimliliklar.gunluk.metneCevir(kayitlar, baglam), Charsets.UTF_8)
                    hedef
                }.getOrNull()
            }
            _dokumDosyasi.value = dosya
            _bilgi.value = if (dosya != null) {
                baglam.getString(R.string.gunluk_dokum_hazir, dosya.name)
            } else {
                baglam.getString(R.string.gunluk_dokum_olusturulamadi)
            }
        }
    }

    fun disaAktar(kaynak: File, hedef: Uri) {
        viewModelScope.launch {
            val sonuc = runCatching { bagimliliklar.calismaAlani.disaAktar(kaynak, hedef) }
            _bilgi.value = if (sonuc.isSuccess) {
                bagimliliklar.uygulamaBaglami.getString(R.string.gunluk_disa_aktarildi)
            } else {
                bagimliliklar.uygulamaBaglami.getString(R.string.gunluk_disa_aktarma_basarisiz)
            }
        }
    }

    fun bilgiyiKapat() {
        _bilgi.value = null
    }
}
