package com.yerel.pdfkutusu.ui.model

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yerel.pdfkutusu.Bagimliliklar
import com.yerel.pdfkutusu.R
import com.yerel.pdfkutusu.depo.CiktiDosyasi
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Uretilmis dosyalar ekrani: listele, disa aktar, sil.
 *
 * Bu dosyalar uygulamaya ozel alanda durur. "Tümünü dışa aktar", SAF ile
 * secilen bir klasore hepsini kopyalar.
 */
class DosyalarViewModel(private val bagimliliklar: Bagimliliklar) : ViewModel() {

    private val _dosyalar = MutableStateFlow<List<CiktiDosyasi>>(emptyList())
    val dosyalar: StateFlow<List<CiktiDosyasi>> = _dosyalar.asStateFlow()

    private val _yukleniyor = MutableStateFlow(false)
    val yukleniyor: StateFlow<Boolean> = _yukleniyor.asStateFlow()

    private val _bilgi = MutableStateFlow<String?>(null)
    val bilgi: StateFlow<String?> = _bilgi.asStateFlow()

    init {
        yenile()
    }

    fun yenile() {
        viewModelScope.launch {
            _yukleniyor.value = true
            _dosyalar.value = withContext(Dispatchers.IO) { bagimliliklar.calismaAlani.ciktilar() }
            _yukleniyor.value = false
        }
    }

    fun sil(dosya: File) {
        viewModelScope.launch {
            val silindi = withContext(Dispatchers.IO) {
                bagimliliklar.onizleme.gecersizKil(dosya)
                bagimliliklar.calismaAlani.sil(dosya)
            }
            _bilgi.value = if (silindi) {
                bagimliliklar.uygulamaBaglami.getString(R.string.dosyalar_silindi, dosya.name)
            } else {
                bagimliliklar.uygulamaBaglami.getString(R.string.dosyalar_silinemedi, dosya.name)
            }
            yenile()
        }
    }

    fun tumunuSil() {
        viewModelScope.launch {
            val sayi = withContext(Dispatchers.IO) {
                bagimliliklar.onizleme.temizle()
                bagimliliklar.calismaAlani.tumCiktilariSil()
            }
            _bilgi.value = bagimliliklar.uygulamaBaglami.getString(R.string.dosyalar_sayi_silindi, sayi)
            yenile()
        }
    }

    fun disaAktar(kaynak: File, hedef: Uri) {
        viewModelScope.launch {
            val sonuc = runCatching { bagimliliklar.calismaAlani.disaAktar(kaynak, hedef) }
            _bilgi.value = if (sonuc.isSuccess) {
                bagimliliklar.uygulamaBaglami.getString(R.string.dosyalar_disa_aktarildi, kaynak.name)
            } else {
                bagimliliklar.uygulamaBaglami.getString(R.string.dosyalar_disa_aktarma_basarisiz, kaynak.name)
            }
        }
    }

    fun tumunuDisaAktar(agacUri: Uri) {
        val hepsi = _dosyalar.value.map { it.dosya }
        if (hepsi.isEmpty()) return
        viewModelScope.launch {
            _yukleniyor.value = true
            val sonuc = runCatching { bagimliliklar.calismaAlani.tumunuDisaAktar(agacUri, hepsi) }
            _yukleniyor.value = false
            val baglam = bagimliliklar.uygulamaBaglami
            _bilgi.value = sonuc.fold(
                onSuccess = { cikti ->
                    buildString {
                        append(baglam.getString(R.string.dosyalar_toplu_disa_aktarildi, cikti.basarili.size))
                        if (cikti.basarisiz.isNotEmpty()) {
                            append(baglam.getString(R.string.dosyalar_toplu_basarisiz_ek, cikti.basarisiz.size))
                        }
                        append(".")
                    }
                },
                onFailure = { baglam.getString(R.string.dosyalar_toplu_disa_aktarma_hata, it.message ?: "") },
            )
        }
    }

    /** Calisma alanindaki gecici girdi kopyalarini siler. */
    fun calismaAlaniniTemizle() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { bagimliliklar.calismaAlani.calismaGirdileriniTemizle() }
            _bilgi.value = bagimliliklar.uygulamaBaglami.getString(R.string.dosyalar_gecici_silindi)
        }
    }

    fun bilgiyiKapat() {
        _bilgi.value = null
    }
}
