import com.android.build.api.artifact.SingleArtifact
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.yerel.pdfkutusu"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yerel.pdfkutusu"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ML Kit'in paketli OCR modeli her mimari icin ~10 MB'lik bir yerel
        // kutuphane getirir; dort mimari birden paketlenince APK ~67 MB olur.
        // Telefonda bunlardan yalnizca biri kullanilir. Tek mimarilik (dolayisiyla
        // ~38 MB) bir APK icin:
        //     ./gradlew assembleDebug -PtekAbi=arm64-v8a
        // Varsayilan davranis bilerek "hepsi": uretilen APK her cihazda ve
        // emulatorde calisir.
        providers.gradleProperty("tekAbi").orNull?.let { istenen ->
            ndk {
                abiFilters += istenen.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            }
        }

        // Uygulama yedeklemeye kapali: gecmis gunlugu ve calisma dosyalari
        // cihazdan disari (bulut yedegine) cikmasin.
        resourceConfigurations += listOf("tr", "en")
    }

    // -----------------------------------------------------------------------
    // Yayin imzasi
    //
    // keystore.properties varsa (dosya .gitignore'da) oradan okunur. Yoksa
    // release derlemesi DEBUG anahtariyla imzalanir: yandan yukleme (sideload)
    // icin calisir, uygulama debuggable DEGILDIR, ama imza herkesin elindeki
    // ortak anahtardir - baskasi uzerine guncelleme imzalayabilir ve magazaya
    // gonderilemez. Kendi anahtarinizi uretmek icin: README > Surum derlemesi.
    // -----------------------------------------------------------------------
    val imzaDosyasi = rootProject.file("keystore.properties")
    val kendiAnahtarVar = imzaDosyasi.exists()

    signingConfigs {
        if (kendiAnahtarVar) {
            create("yayin") {
                val ozellikler = Properties().apply {
                    imzaDosyasi.inputStream().use { load(it) }
                }
                storeFile = rootProject.file(ozellikler.getProperty("storeFile"))
                storePassword = ozellikler.getProperty("storePassword")
                keyAlias = ozellikler.getProperty("keyAlias")
                keyPassword = ozellikler.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            // R8 kucultmesi ACIK: 31,9 MB -> 21,0 MB.
            //
            // Kucultulmus APK cihazda dogrulandi - 10 enstrumante testin tamami
            // bu derlemeye karsi gecti (PdfBox, ML Kit, Room, EXIF dahil).
            // Tekrarlamak icin:
            //     ./gradlew connectedReleaseAndroidTest -PtestBuildType=release
            //
            // Sorun ararken kapatmak icin: -PkucultR8=false
            val r8Acik = providers.gradleProperty("kucultR8")
                .map { !it.equals("false", ignoreCase = true) }
                .getOrElse(true)

            isMinifyEnabled = r8Acik
            isShrinkResources = r8Acik
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = if (kendiAnahtarVar) {
                signingConfigs.getByName("yayin")
            } else {
                signingConfigs.getByName("debug")
            }
            // Yalnizca -PtestBuildType=release ile kosulan enstrumante test
            // APK'sina uygulanir; uygulama APK'sini etkilemez.
            testProguardFiles("proguard-test-rules.pro")
        }
    }

    // Enstrumante testleri release derlemesine karsi da kosturabilmek icin:
    //     ./gradlew connectedReleaseAndroidTest -PtestBuildType=release
    // R8'in PdfBox/ML Kit yansimasini bozup bozmadigi ancak boyle anlasilir.
    testBuildType = providers.gradleProperty("testBuildType").getOrElse("debug")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    androidResources {
        // ML Kit paketli modeli sikistirilmadan paketlenmeli.
        noCompress += listOf("tflite", "lite")
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module",
                "META-INF/INDEX.LIST",
            )
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    lint {
        abortOnError = false
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.documentfile)
    // EXIF yonu okumak icin. Platformun android.media.ExifInterface'i HEIF ve
    // WebP'de eksik; androidx surumu hepsini okur. Hicbir izin tanimlamaz.
    implementation(libs.androidx.exifinterface)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // PDF motoru (Apache 2.0). AGPL lisansli hicbir kutuphane kullanilmiyor.
    implementation(libs.pdfbox.android)

    // ML Kit metin tanima - PAKETLI model. Model APK icinde gelir,
    // calisma zamaninda indirme yapilmaz, internet gerekmez.
    implementation(libs.mlkit.text.recognition)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// ---------------------------------------------------------------------------
// YAPISAL GARANTI: birlesmis (merged) AndroidManifest.xml icinde ag izinleri
// bulunamaz. Kaynak manifestte `tools:node="remove"` ile kaldirilmis olsalar
// bile, herhangi bir kutuphane yeni bir ag izni eklemeye kalkarsa bu gorev
// derlemeyi durdurur. Bu bir soz degil, derleme zamani kontroludur.
// ---------------------------------------------------------------------------
abstract class AgIzniDenetimi : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val birlesmisManifest: RegularFileProperty

    @get:OutputFile
    abstract val rapor: RegularFileProperty

    companion object {
        /**
         * Tek istisna: androidx.core'un ekledigi, uygulamanin KENDI paket adiyla
         * isimlendirilmis imza (signature) seviyesindeki iznidir. API 33+'ta
         * `ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED)` cagrilari
         * dinamik alicilari korumak icin bunu kullanir.
         *
         * Bu bir yetenek talebi DEGILDIR: yalnizca ayni imzayla imzalanmis kod
         * (yani uygulamanin kendisi) tarafindan kullanilabilir, kullaniciya
         * gosterilmez ve hicbir sisteme/aga erisim saglamaz. Kaldirmak
         * androidx bilesenlerini bozar.
         */
        val IZIN_VERILEN_DESENLER = listOf(
            Regex("""^[\w.]+\.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION$"""),
        )

        val YASAKLI_IZINLER = listOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.CHANGE_NETWORK_STATE",
            "android.permission.CHANGE_WIFI_STATE",
            "android.permission.MANAGE_EXTERNAL_STORAGE",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
        )
    }

    @TaskAction
    fun denetle() {
        val manifest = birlesmisManifest.get().asFile
        val metin = manifest.readText()

        // Sadece gercekten talep edilen izinleri yakala; yorum satirlarini degil.
        val talepEdilen = Regex(
            """<uses-permission[^>]*android:name\s*=\s*"([^"]+)"""",
            RegexOption.IGNORE_CASE,
        ).findAll(metin).map { it.groupValues[1] }.toSortedSet()

        val beklenen = talepEdilen.filter { izin -> IZIN_VERILEN_DESENLER.any { it.matches(izin) } }
        val kalanlar = talepEdilen - beklenen.toSet()
        val agirIhlaller = kalanlar.filter { it in YASAKLI_IZINLER }

        val raporMetni = buildString {
            appendLine("Birlesmis manifest: ${manifest.absolutePath}")
            appendLine()
            appendLine("Kullaniciya gorunen / yetenek veren izin: ${kalanlar.size}")
            if (kalanlar.isEmpty()) {
                appendLine("  YOK — hedeflenen durum.")
            } else {
                kalanlar.forEach {
                    appendLine("  - $it" + if (it in YASAKLI_IZINLER) "   <== YASAKLI" else "")
                }
            }
            appendLine()
            appendLine("Beklenen uygulama-ici imza izinleri: ${beklenen.size}")
            beklenen.forEach { appendLine("  - $it (signature, kullaniciya gorunmez)") }
            appendLine()
            appendLine("INTERNET var mi: " + talepEdilen.contains("android.permission.INTERNET"))
        }
        val raporDosyasi = rapor.get().asFile
        raporDosyasi.parentFile.mkdirs()
        raporDosyasi.writeText(raporMetni)

        // Hedef SIFIR izin. Yeni bir bagimlilik zararsiz gorunen bir izin
        // eklese bile burada duruyoruz: karar bilincli verilmeli.
        if (kalanlar.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("IZIN IHLALI: birlesmis manifest izin talep ediyor.")
                    if (agirIhlaller.isNotEmpty()) {
                        appendLine(
                            "  Ag/depolama izni (kesinlikle olmamali): " +
                                agirIhlaller.joinToString(", "),
                        )
                    }
                    appendLine("  Beklenmeyen izinler: ${kalanlar.joinToString(", ")}")
                    appendLine("  Manifest: ${manifest.absolutePath}")
                    appendLine(
                        "  Muhtemelen bir bagimlilik ekledi. Kaldirmak icin " +
                            "app/src/main/AndroidManifest.xml icine ekleyin:",
                    )
                    kalanlar.forEach {
                        appendLine("    <uses-permission android:name=\"$it\" tools:node=\"remove\" />")
                    }
                },
            )
        }
        logger.lifecycle(
            "[izin-denetimi] Temiz: INTERNET ve diger yetenek izinleri yok. " +
                "Rapor: ${raporDosyasi.absolutePath}",
        )
    }
}

androidComponents {
    onVariants { varyant ->
        val buyukAd = varyant.name.replaceFirstChar { it.uppercase() }
        val denetim = tasks.register<AgIzniDenetimi>("verify${buyukAd}NoNetworkPermission") {
            group = "verification"
            description = "Birlesmis manifestte INTERNET ve benzeri ag izinlerinin bulunmadigini dogrular."
            birlesmisManifest.set(varyant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
            rapor.set(layout.buildDirectory.file("reports/izin-denetimi/${varyant.name}.txt"))
        }

        // Hem paketleme hem de dogrulama akislarina bagla.
        tasks.matching { it.name == "assemble$buyukAd" }.configureEach { dependsOn(denetim) }
        tasks.matching { it.name == "package$buyukAd" }.configureEach { dependsOn(denetim) }
    }
}

tasks.matching { it.name == "check" }.configureEach {
    dependsOn("verifyDebugNoNetworkPermission")
}
