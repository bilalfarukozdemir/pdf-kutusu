package com.yerel.pdfkutusu.ui.model

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yerel.pdfkutusu.Bagimliliklar
import com.yerel.pdfkutusu.cekirdek.IslemTuru
import com.yerel.pdfkutusu.cekirdek.Ozet
import com.yerel.pdfkutusu.cekirdek.PdfHatasi
import com.yerel.pdfkutusu.depo.CalismaDosyasi
import com.yerel.pdfkutusu.pdf.BelgeErisimi
import com.yerel.pdfkutusu.pdf.BelgeIncelemesi
import com.yerel.pdfkutusu.pdf.IlerlemeDinleyicisi
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Tum arac ekranlarinin ortak beyni.
 *
 * Sorumluluklar:
 *  - SAF'tan gelen URI'leri calisma alanina almak ve incelemek
 *  - Sifreli belgelerde parola akisini yurutmek
 *  - Islemi arka planda calistirmak, ilerleme bildirmek, **iptali desteklemek**
 *  - Her islemi (basarili ya da hatali) salt-ekleme gunluge yazmak
 *
 * Alt siniflar yalnizca kendi islemlerini [calistir] icinde tarif eder.
 */
abstract class AracViewModel(
    protected val bagimliliklar: Bagimliliklar,
    val islemTuru: IslemTuru,
    /** true ise ekran tek girdiyle calisir; yeni secim oncekinin yerine gecer. */
    private val tekGirdi: Boolean = true,
) : ViewModel() {

    protected val calismaAlani get() = bagimliliklar.calismaAlani
    protected val gunluk get() = bagimliliklar.gunluk
    protected val rasterlestirici get() = bagimliliklar.rasterlestirici
    protected val onizleme get() = bagimliliklar.onizleme

    /** Ekranlarin kucuk resim cizebilmesi icin. */
    val onizlemeDeposu get() = bagimliliklar.onizleme

    private val _durum = MutableStateFlow(AracEkranDurumu())
    val durum: StateFlow<AracEkranDurumu> = _durum.asStateFlow()

    private val kuyruk = ArrayDeque<Uri>()
    private var yuklemeIsi: Job? = null
    private var islemIsi: Job? = null

    // ------------------------------------------------------------ girdi akisi

    fun dosyalariEkle(uriler: List<Uri>) {
        if (uriler.isEmpty()) return
        if (tekGirdi) {
            kuyruk.clear()
            kuyruk.add(uriler.first())
            guncelle { it.copy(girdiler = emptyList(), sonuc = null, hata = null) }
        } else {
            kuyruk.addAll(uriler)
        }
        kuyruguIsle()
    }

    private fun kuyruguIsle() {
        if (yuklemeIsi?.isActive == true) return
        yuklemeIsi = viewModelScope.launch {
            guncelle { it.copy(dosyaYukleniyor = true, hata = null) }
            while (kuyruk.isNotEmpty()) {
                val uri = kuyruk.removeFirst()
                val calisma = try {
                    calismaAlani.iceriAktar(uri)
                } catch (hata: PdfHatasi) {
                    guncelle { it.copy(hata = hata) }
                    continue
                }

                val inceleme = runCatching { girdiyiIncele(calisma) }
                val sorun = inceleme.exceptionOrNull()
                if (sorun is PdfHatasi.ParolaGerekli) {
                    // Kalan dosyalar kuyrukta bekler; parola verilince devam eder.
                    guncelle {
                        it.copy(
                            dosyaYukleniyor = false,
                            parolaIstegi = ParolaIstegi(
                                calisma.dosya,
                                calisma.gorunenAd,
                                calisma.boyut,
                            ),
                        )
                    }
                    return@launch
                }
                if (sorun != null) {
                    calismaAlani.sil(calisma.dosya)
                    guncelle {
                        it.copy(hata = sorun as? PdfHatasi ?: PdfHatasi.Beklenmeyen(sorun))
                    }
                    continue
                }

                girdiEkle(inceleme.getOrThrow())
            }
            guncelle { it.copy(dosyaYukleniyor = false) }
        }
    }

    /**
     * Alinan dosyayi inceleyip listeye eklenecek ogeye cevirir.
     *
     * Varsayilan davranis PDF icindir. Gorsel gibi baska turlerle calisan
     * ekranlar bunu gecersiz kilar - boylece dosya alma, parola akisi,
     * ilerleme, iptal ve gunluk yazma mantigi tek yerde kalir.
     */
    protected open suspend fun girdiyiIncele(calisma: CalismaDosyasi): GirdiOgesi =
        withContext(Dispatchers.IO) {
            GirdiOgesi(
                dosya = calisma.dosya,
                gorunenAd = calisma.gorunenAd,
                boyut = calisma.boyut,
                sha256 = calisma.sha256,
                ozet = BelgeIncelemesi.incele(calisma.dosya),
            )
        }

    /**
     * Parolayi alir ve calisma kopyasinin **sifresini kalici olarak kaldirir.**
     * Boylece onizleme (PdfRenderer sifreli dosya acamaz) ve tum islemler
     * bundan sonra normal calisir. Orijinal dosya elbette dokunulmaz.
     */
    fun parolaGonder(parola: String) {
        val istek = _durum.value.parolaIstegi ?: return
        viewModelScope.launch {
            guncelle { it.copy(dosyaYukleniyor = true, parolaHatasi = null) }
            val sonuc = withContext(Dispatchers.IO) {
                runCatching {
                    val gecici = File(istek.dosya.parentFile, "cozulmus_${istek.dosya.name}")
                    BelgeErisimi.ac(istek.dosya, parola).use { belge ->
                        belge.isAllSecurityToBeRemoved = true
                        belge.save(gecici)
                    }
                    if (!istek.dosya.delete() || !gecici.renameTo(istek.dosya)) {
                        gecici.delete()
                        throw PdfHatasi.DosyaOkunamadi("Çözülmüş kopya yazılamadı.")
                    }
                    BelgeIncelemesi.incele(istek.dosya)
                }
            }

            sonuc.fold(
                onSuccess = { ozet ->
                    girdiEkle(
                        GirdiOgesi(
                            dosya = istek.dosya,
                            gorunenAd = istek.gorunenAd,
                            boyut = istek.dosya.length(),
                            sha256 = Ozet.sha256(istek.dosya),
                            ozet = ozet,
                            sifresiKaldirildi = true,
                        ),
                    )
                    guncelle {
                        it.copy(
                            parolaIstegi = null,
                            parolaHatasi = null,
                            bilgi = "Parola doğrulandı. Çıktı şifresiz üretilecek.",
                        )
                    }
                    kuyruguIsle()
                },
                onFailure = { hata ->
                    guncelle {
                        it.copy(
                            dosyaYukleniyor = false,
                            parolaHatasi = (hata as? PdfHatasi)?.kullaniciMesaji
                                ?: "Parola doğrulanamadı.",
                        )
                    }
                },
            )
        }
    }

    fun parolayiIptalEt() {
        val istek = _durum.value.parolaIstegi
        if (istek != null) calismaAlani.sil(istek.dosya)
        kuyruk.clear()
        guncelle {
            it.copy(
                parolaIstegi = null,
                parolaHatasi = null,
                dosyaYukleniyor = false,
                bilgi = "Şifreli belge atlandı.",
            )
        }
    }

    private fun girdiEkle(oge: GirdiOgesi) {
        guncelle { durum ->
            val yeni = if (tekGirdi) listOf(oge) else durum.girdiler + oge
            durum.copy(girdiler = yeni, sonuc = null)
        }
        girdilerDegisti()
    }

    fun girdiKaldir(dosya: File) {
        calismaAlani.sil(dosya)
        onizleme.gecersizKil(dosya)
        guncelle { it.copy(girdiler = it.girdiler.filterNot { oge -> oge.dosya == dosya }) }
        girdilerDegisti()
    }

    fun girdiTasi(kaynakIndeks: Int, hedefIndeks: Int) {
        guncelle { durum ->
            val liste = durum.girdiler.toMutableList()
            if (kaynakIndeks !in liste.indices || hedefIndeks !in liste.indices) return@guncelle durum
            liste.add(hedefIndeks, liste.removeAt(kaynakIndeks))
            durum.copy(girdiler = liste)
        }
    }

    /** Girdi listesinin tamamini yeniden duzenler (ada gore sirala vb.). */
    protected fun girdileriYenidenSirala(donusum: (List<GirdiOgesi>) -> List<GirdiOgesi>) {
        guncelle { it.copy(girdiler = donusum(it.girdiler)) }
    }

    fun tumunuTemizle() {
        _durum.value.girdiler.forEach {
            calismaAlani.sil(it.dosya)
            onizleme.gecersizKil(it.dosya)
        }
        kuyruk.clear()
        guncelle { AracEkranDurumu() }
        girdilerDegisti()
    }

    /** Alt siniflar girdi listesi degistiginde varsayilanlarini tazeleyebilir. */
    protected open fun girdilerDegisti() = Unit

    // -------------------------------------------------------------- calistir

    /**
     * Islemi arka planda calistirir.
     *
     * Iptal: [iptalEt] isi iptal eder; ilerleme dinleyicisi bir sonraki
     * bildirimde `ensureActive()` ile istisna firlatir ve islem temiz durur.
     * Yarim kalan cikti dosyasi silinir.
     */
    protected fun calistir(blok: suspend (IlerlemeDinleyicisi) -> IslemCiktisi) {
        if (_durum.value.calisiyor) return
        val girdiler = _durum.value.girdiler
        if (girdiler.isEmpty()) {
            guncelle { it.copy(hata = PdfHatasi.GirdiYok()) }
            return
        }

        val girdiAdi = gunlukGirdiAdi(girdiler)
        val girdiOzeti = gunlukGirdiOzeti(girdiler)

        islemIsi = viewModelScope.launch {
            guncelle { it.copy(calisiyor = true, ilerleme = null, hata = null, sonuc = null) }
            try {
                val cikti = withContext(Dispatchers.IO) {
                    val kapsam = this
                    blok { ilerleme ->
                        kapsam.ensureActive()
                        _durum.update { it.copy(ilerleme = ilerleme) }
                    }
                }
                gunluk.basariliKaydet(
                    islem = islemTuru,
                    girdiDosyaAdi = girdiAdi,
                    girdiSha256 = girdiOzeti,
                    ciktiDosyasi = cikti.dosyalar.firstOrNull(),
                    sayfaSayisi = cikti.sayfaSayisi,
                )
                cikti.dosyalar.forEach { onizleme.gecersizKil(it) }
                guncelle { it.copy(calisiyor = false, ilerleme = null, sonuc = cikti) }
            } catch (iptal: CancellationException) {
                withContext(NonCancellable) {
                    gunluk.hataKaydet(
                        islem = islemTuru,
                        girdiDosyaAdi = girdiAdi,
                        girdiSha256 = girdiOzeti,
                        hataMesaji = "İşlem kullanıcı tarafından iptal edildi.",
                    )
                }
                _durum.update {
                    it.copy(calisiyor = false, ilerleme = null, bilgi = "İşlem iptal edildi.")
                }
                throw iptal
            } catch (hata: Throwable) {
                val pdfHatasi = hata as? PdfHatasi ?: PdfHatasi.Beklenmeyen(hata)
                gunluk.hataKaydet(
                    islem = islemTuru,
                    girdiDosyaAdi = girdiAdi,
                    girdiSha256 = girdiOzeti,
                    hataMesaji = pdfHatasi.kullaniciMesaji,
                )
                guncelle { it.copy(calisiyor = false, ilerleme = null, hata = pdfHatasi) }
            }
        }
    }

    fun iptalEt() {
        islemIsi?.cancel()
        islemIsi = null
    }

    // ----------------------------------------------------------- disa aktarim

    fun disaAktar(kaynak: File, hedef: Uri) {
        viewModelScope.launch {
            try {
                calismaAlani.disaAktar(kaynak, hedef)
                guncelle { it.copy(bilgi = "Dışa aktarıldı: ${kaynak.name}") }
            } catch (hata: PdfHatasi) {
                guncelle { it.copy(hata = hata) }
            }
        }
    }

    fun tumunuDisaAktar(agacUri: Uri) {
        val dosyalar = _durum.value.sonuc?.dosyalar.orEmpty()
        if (dosyalar.isEmpty()) return
        viewModelScope.launch {
            try {
                val sonuc = calismaAlani.tumunuDisaAktar(agacUri, dosyalar)
                val mesaj = buildString {
                    append("${sonuc.basarili.size} dosya dışa aktarıldı")
                    if (sonuc.basarisiz.isNotEmpty()) append(", ${sonuc.basarisiz.size} başarısız")
                    append(".")
                }
                guncelle { it.copy(bilgi = mesaj) }
            } catch (hata: PdfHatasi) {
                guncelle { it.copy(hata = hata) }
            }
        }
    }

    // ------------------------------------------------------------- yardimcilar

    fun hatayiKapat() = guncelle { it.copy(hata = null) }
    fun bilgiyiKapat() = guncelle { it.copy(bilgi = null) }
    fun sonucuKapat() = guncelle { it.copy(sonuc = null) }

    protected fun guncelle(donusum: (AracEkranDurumu) -> AracEkranDurumu) = _durum.update(donusum)

    protected fun bilgiVer(mesaj: String) = guncelle { it.copy(bilgi = mesaj) }

    protected open fun gunlukGirdiAdi(girdiler: List<GirdiOgesi>): String =
        if (girdiler.size == 1) {
            girdiler.first().gorunenAd
        } else {
            "${girdiler.size} dosya: " + girdiler.joinToString(", ") { it.gorunenAd }.take(240)
        }

    /**
     * Birden fazla girdide, girdilerin ozetlerinin ozeti alinir. Boylece
     * "hangi dosyalardan uretildi" sorusu tek bir degerle yanitlanabilir.
     */
    protected open fun gunlukGirdiOzeti(girdiler: List<GirdiOgesi>): String? = when {
        girdiler.isEmpty() -> null
        girdiler.size == 1 -> girdiler.first().sha256
        else -> Ozet.sha256(girdiler.joinToString("") { it.sha256 }.toByteArray())
    }

    override fun onCleared() {
        super.onCleared()
        islemIsi?.cancel()
        yuklemeIsi?.cancel()
    }
}
