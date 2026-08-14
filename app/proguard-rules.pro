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

# ---------------------------------------------------------------------------
# Kucultulmus derlemenin cihazda test edilebilmesi icin
#
# Uygulama ve enstrumante test APK'si AYRI AYRI kucultulur. Test APK'si,
# uygulamanin icindeki kotlin-stdlib ve androidx altyapisini kullanir; ama
# uygulamanin R8 kosumu test APK'sini gormedigi icin "kullanilmiyor" diye
# silinen siniflar calisma zamaninda test kosucusunu cokertir:
#
#   NoSuchMethodError: ... in class Lj3/j;        (kotlin.jvm.internal.Intrinsics)
#   NoClassDefFoundError: androidx/tracing/Trace  (AndroidJUnitRunner.onCreate)
#   NoClassDefFoundError: kotlin/LazyKt           (TestDirCalculator)
#
# Bunlar UYGULAMANIN hatasi degildir - uretimde o siniflarin silinmesi
# dogrudur. Ama dagittigimiz APK'nin ta kendisini test edebilmek, birkac yuz
# KB'lik bu maliyete deger: aksi halde kullaniciya dogrulanmamis bir derleme
# gondermis oluruz.
# ---------------------------------------------------------------------------
-keep class kotlin.** { *; }
-keep class kotlin.jvm.internal.Intrinsics { *; }
-keep class androidx.tracing.** { *; }
-dontwarn kotlin.**

# Test `runBlocking` kullaniyor; uygulama kullanmadigi icin R8 atiyor.
-keep class kotlinx.coroutines.** { *; }

# Uygulama ExifInterface'i InputStream ile kurar, test ise dosya yolu ile
# (EXIF yazabilmek icin `saveAttributes()` gerekiyor). R8 kullanilmayan
# yapiciyi atinca test soyle patliyor:
#   NoSuchMethodError: No direct method <init>(Ljava/lang/String;)V in Lt1/g;
-keep class androidx.exifinterface.** { *; }

# Uygulamanin kendi siniflari da oldugu gibi tutulur.
#
# Iki sebep:
#
# 1. Test APK'si bu siniflari dogrudan cagirir. R8 yalnizca yeniden
#    adlandirmakla kalmayip nesneleri (object) satir ici hale getirir ve
#    yapici imzalarini degistirir; -applymapping ise sadece ISIMLERI
#    tasir, imza degisikliklerini degil. Sonuc:
#      NoSuchMethodError: No direct method <init>(Ljava/lang/String;)V in Lt1/g;
#      NoSuchFieldError:  No field INSTANCE of type LN2/s; in class LN2/s;
#
# 2. Bu proje MIT lisansli, kaynagi herkese acik. Kendi sinif adlarimizi
#    gizlemek hicbir sey korumaz. R8'in bu projedeki asil degeri olu kod ve
#    kullanilmayan kaynaklarin atilmasi; o kazanc aynen duruyor.
#
# Olculen maliyet: 20,2 MB -> 20,8 MB. Kucultmesiz surum 31,9 MB.
-keep class com.yerel.pdfkutusu.** { *; }
