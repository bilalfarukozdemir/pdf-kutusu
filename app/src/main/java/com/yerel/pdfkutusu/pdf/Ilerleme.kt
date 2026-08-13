package com.yerel.pdfkutusu.pdf

/**
 * Uzun islemlerin ilerleme bildirimi.
 *
 * [PdfIslemi] fonksiyonlari coroutine bilmez; iptal, dinleyicinin istisna
 * firlatmasiyla gerceklesir. ViewModel katmani dinleyicinin icinde
 * `ensureActive()` cagirir, boylece coroutine iptal edildiginde islem
 * bir sonraki ilerleme noktasinda temiz sekilde durur.
 */
data class Ilerleme(
    val tamamlanan: Int,
    val toplam: Int,
    val etiket: String = "",
) {
    val oran: Float get() = if (toplam <= 0) 0f else (tamamlanan.toFloat() / toplam).coerceIn(0f, 1f)
}

typealias IlerlemeDinleyicisi = (Ilerleme) -> Unit

/** Ilerleme bildirmek istemeyen cagrilar icin. */
val IlerlemeYok: IlerlemeDinleyicisi = {}
