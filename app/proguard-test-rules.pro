# Yalnizca enstrumante test APK'si icin (testProguardFiles).
# Uygulama APK'sina uygulanmaz.
#
# Test bagimliliklari (Espresso / AndroidX Test) derleme zamani annotation
# isleme siniflarina zayif referans tasir; calisma zamaninda kullanilmazlar.
-dontwarn javax.lang.model.**
-dontwarn javax.annotation.**

# Test siniflarinin adlari JUnit tarafindan yansimayla okunur.
-keep class com.yerel.pdfkutusu.**Testi { *; }
-keep class com.yerel.pdfkutusu.test.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
