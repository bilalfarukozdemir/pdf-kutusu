package com.yerel.pdfkutusu.ui.ortak

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TARIH_SAAT = DateTimeFormatter
    .ofPattern("dd.MM.yyyy HH:mm:ss", Locale("tr", "TR"))
    .withZone(ZoneId.systemDefault())

fun bicimliBoyut(bayt: Long): String = when {
    bayt >= 1_048_576 -> String.format(Locale.ROOT, "%.1f MB", bayt / 1_048_576.0)
    bayt >= 1024 -> String.format(Locale.ROOT, "%.0f KB", bayt / 1024.0)
    else -> "$bayt B"
}

fun bicimliZaman(epochMilis: Long): String =
    runCatching { TARIH_SAAT.format(Instant.ofEpochMilli(epochMilis)) }.getOrDefault("-")

fun kisaOzet(sha256: String?): String =
    if (sha256.isNullOrBlank()) "-" else sha256.take(12) + "…"
