package com.yerel.pdfkutusu.satinalma

/**
 * Play'in `Purchase` nesnesinden testte kurulabilir ozet.
 * [durum] Billing sabitidir: 1 = PURCHASED, 2 = PENDING.
 */
data class SatinAlmaOzeti(
    val urunler: List<String>,
    val durum: Int,
    val jeton: String,
)

const val SATIN_ALMA_DURUMU_PURCHASED = 1

/** Ekran disi tarama en cok bu sikliktadir (onResume her donuste tetiklenir). */
const val TARAMA_ARALIGI_MS = 60_000L

/**
 * Play'e onaylanmasi/tuketilmesi gereken alimlar: yalnizca PURCHASED olan ve
 * bilinen bir hizmet urunu iceren, ayni jeton bir kez. PENDING (2) urun
 * vermez, onaylanmaz; sonradan PURCHASED olunca sonraki taramada gelir.
 */
fun islenecekSatinAlmalar(
    satinAlmalar: List<SatinAlmaOzeti>,
    bilinenUrunler: Collection<String>,
): List<SatinAlmaOzeti> =
    satinAlmalar
        .filter { it.durum == SATIN_ALMA_DURUMU_PURCHASED && it.urunler.any { u -> u in bilinenUrunler } }
        .distinctBy { it.jeton }

/** Talep kaydini kume olarak ekler; ayni kayit ikinci kez yazilmaz. */
fun talepKaydiEkle(kayitlar: Set<String>, yeniKayit: String): Set<String> = kayitlar + yeniKayit

/** Onceki tarama yoksa (ya da basarisiz olduysa) hemen, aksi halde aralik dolunca true. */
fun taramaZamaniGeldiMi(sonTaramaMs: Long?, simdiMs: Long, aralikMs: Long = TARAMA_ARALIGI_MS): Boolean =
    sonTaramaMs == null || simdiMs - sonTaramaMs >= aralikMs
