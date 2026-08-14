package com.yerel.pdfkutusu.ui.model

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yerel.pdfkutusu.Bagimliliklar
import com.yerel.pdfkutusu.cekirdek.DosyaAdi
import com.yerel.pdfkutusu.okuyucu.BelgeKaynagi
import com.yerel.pdfkutusu.okuyucu.OkuyucuMotoru
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface OkuyucuDurumu {
    data object Bos : OkuyucuDurumu
    data object Yukleniyor : OkuyucuDurumu

    data class ParolaGerekli(
        val gorunenAd: String,
        val hataMesaji: String? = null,
    ) : OkuyucuDurumu

    data class Hazir(
        val motor: OkuyucuMotoru,
        val gorunenAd: String,
        val sifresiCozuldu: Boolean,
    ) : OkuyucuDurumu

    data class Hata(val mesaj: String, val oneri: String? = null) : OkuyucuDurumu
}

/**
 * Okuyucu ekraninin durum makinesi.
 *
 * Belgeyi acar, parola akisini yurutur, cizim motorunun omrunu yonetir ve
 * gecici dosyalari temizler. Cizimin kendisi [OkuyucuMotoru] icinde.
 */
class OkuyucuViewModel(private val bagimliliklar: Bagimliliklar) : ViewModel() {

    private val _durum = MutableStateFlow<OkuyucuDurumu>(OkuyucuDurumu.Bos)
    val durum: StateFlow<OkuyucuDurumu> = _durum.asStateFlow()

    private val _mesaj = MutableStateFlow<String?>(null)
    val mesaj: StateFlow<String?> = _mesaj.asStateFlow()

    private var acmaIsi: Job? = null
    private var motor: OkuyucuMotoru? = null
    private var acikDosya: File? = null
    private var acikDosyaGecici = false
    private var kaynakUri: Uri? = null

    /** Onbellege alma islerini takip edip belge degisince iptal etmek icin. */
    private var onYuklemeIsi: Job? = null

    fun ac(uri: Uri, parola: String? = null) {
        acmaIsi?.cancel()
        kaynakUri = uri
        acmaIsi = viewModelScope.launch {
            _durum.value = OkuyucuDurumu.Yukleniyor
            kaynaklariBirak()

            val sonuc = BelgeKaynagi.coz(
                baglam = bagimliliklar.uygulamaBaglami,
                uri = uri,
                parola = parola,
                gecicilerDizini = bagimliliklar.calismaAlani.gecicilerDizini,
            )

            when (sonuc) {
                is BelgeKaynagi.Sonuc.ParolaGerekli ->
                    _durum.value = OkuyucuDurumu.ParolaGerekli(
                        gorunenAd = sonuc.gorunenAd,
                        hataMesaji = if (parola.isNullOrEmpty()) null else "Parola doğrulanamadı.",
                    )

                is BelgeKaynagi.Sonuc.Hata ->
                    _durum.value = OkuyucuDurumu.Hata(sonuc.mesaj, sonuc.oneri)

                is BelgeKaynagi.Sonuc.Hazir -> {
                    val yeniMotor = withContext(Dispatchers.IO) {
                        runCatching { OkuyucuMotoru.ac(sonuc.dosya) }.getOrNull()
                    }
                    if (yeniMotor == null) {
                        if (sonuc.gecici) runCatching { sonuc.dosya.delete() }
                        _durum.value = OkuyucuDurumu.Hata(
                            "Belge açıldı ama çizilemedi.",
                            "Dosya bozuk olabilir.",
                        )
                    } else {
                        motor = yeniMotor
                        acikDosya = sonuc.dosya
                        acikDosyaGecici = sonuc.gecici
                        _durum.value = OkuyucuDurumu.Hazir(
                            motor = yeniMotor,
                            gorunenAd = sonuc.gorunenAd,
                            sifresiCozuldu = sonuc.sifresiCozuldu,
                        )
                        ucuzKatmaniHazirla(yeniMotor)
                    }
                }
            }
        }
    }

    fun parolaGonder(parola: String) {
        val uri = kaynakUri ?: return
        ac(uri, parola)
    }

    /**
     * Her sayfanin ucuz surumunu arka planda uretir.
     *
     * Kaydirma sirasinda "bos kutu" gorunmemesinin sebebi bu: net surum hazir
     * olmasa da gosterilecek bir sey her zaman bulunur. Dusuk oncelikli ve
     * iptal edilebilir; belge kapaninca durur.
     */
    private fun ucuzKatmaniHazirla(hedefMotor: OkuyucuMotoru) {
        onYuklemeIsi?.cancel()
        onYuklemeIsi = viewModelScope.launch(Dispatchers.IO) {
            for (indeks in 0 until hedefMotor.sayfaSayisi) {
                if (!isActive) return@launch
                hedefMotor.ciz(indeks, OkuyucuMotoru.ONIZLEME_GENISLIGI)
            }
        }
    }

    /** Gorunen sayfalarin komsularini onden cizer. */
    fun komsulariHazirla(gorunenAralik: IntRange, hedefGenislik: Int) {
        val aktifMotor = motor ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val bas = (gorunenAralik.first - 1).coerceAtLeast(0)
            val son = (gorunenAralik.last + 1).coerceAtMost(aktifMotor.sayfaSayisi - 1)
            for (indeks in bas..son) {
                aktifMotor.ciz(indeks, hedefGenislik)
            }
        }
    }

    /**
     * Acik belgeyi araclarda kullanilmak uzere calisma alanina kopyalar.
     * @return kopyalanan dosya, ya da kopyalanamadiysa null
     */
    suspend fun araclaraKopyala(): File? {
        val kaynak = acikDosya ?: return null
        val ad = (_durum.value as? OkuyucuDurumu.Hazir)?.gorunenAd ?: kaynak.name
        return withContext(Dispatchers.IO) {
            runCatching {
                val hedef = DosyaAdi.cakismayan(
                    bagimliliklar.calismaAlani.calismaDizini,
                    DosyaAdi.guvenli(ad),
                )
                kaynak.copyTo(hedef, overwrite = true)
                hedef
            }.getOrNull()
        }
    }

    /** Paylasim icin acik belgenin kopyasini cikti klasorunde uretir. */
    suspend fun paylasimIcinHazirla(): File? {
        val kaynak = acikDosya ?: return null
        val ad = (_durum.value as? OkuyucuDurumu.Hazir)?.gorunenAd ?: kaynak.name
        return withContext(Dispatchers.IO) {
            runCatching {
                val hedef = bagimliliklar.calismaAlani.ciktiDosyasi(DosyaAdi.guvenli(ad))
                kaynak.copyTo(hedef, overwrite = true)
                hedef
            }.getOrNull()
        }
    }

    fun mesajGoster(metin: String) {
        _mesaj.value = metin
    }

    fun mesajiKapat() {
        _mesaj.value = null
    }

    private fun kaynaklariBirak() {
        onYuklemeIsi?.cancel()
        onYuklemeIsi = null
        runCatching { motor?.close() }
        motor = null
        val eski = acikDosya
        if (acikDosyaGecici && eski != null) runCatching { eski.delete() }
        acikDosya = null
        acikDosyaGecici = false
    }

    override fun onCleared() {
        super.onCleared()
        acmaIsi?.cancel()
        kaynaklariBirak()
    }
}
