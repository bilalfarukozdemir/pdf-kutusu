# PdfBox-Android yansima (reflection) ile font/renk uzayi siniflarini yukler.
-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
-dontwarn com.tom_roush.**
-dontwarn org.apache.**
-dontwarn javax.**
-dontwarn java.awt.**

# ML Kit paketli metin tanima
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Room
-keep class com.yerel.pdfkutusu.veri.** { *; }
