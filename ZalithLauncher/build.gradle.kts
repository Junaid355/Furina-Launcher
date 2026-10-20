plugins {
    id("com.android.application")
}

val getCFApiKey = {
    System.getenv("CURSEFORGE_API_KEY") ?: run {
        val curseforgeKeyFile = File(rootDir, "curseforge_key.txt")
        if (curseforgeKeyFile.canRead() && curseforgeKeyFile.isFile) {
            curseforgeKeyFile.readText()
        } else {
            logger.warn("BUILD: You have no CurseForge key, the curseforge api will get disabled !")
            "DUMMY"
        }
    }
}

val getBuildType = {
    val buildType = System.getenv("ZL_BUILD_TYPE") ?: "DEBUG"
    logger.warn("BUILD: Build Type --> $buildType")
    buildType
}

val nameId = "com.movtery.zalithlauncher"
val generatedZalithDir = file("${layout.buildDirectory.get().asFile}/generated/source/zalith/java")
val launcherAPPName = project.findProperty("launcher_app_name") as? String ?: error("The \"launcher_app_name\" property is not set in gradle.properties.")
val launcherName = project.findProperty("launcher_name") as? String ?: error("The \"launcher_name\" property is not set in gradle.properties.")
val launcherVersionCode = (project.findProperty("launcher_version_code") as? String)?.toIntOrNull() ?: error("The \"launcher_version_code\" property is not set as an integer in gradle.properties.")
val launcherVersionName = project.findProperty("launcher_version_name") as? String ?: error("The \"launcher_version_name\" property is not set in gradle.properties.")

configurations {
    create("instrumentedClasspath") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }
}

android {
    namespace = nameId
    compileSdk = 36

    signingConfigs {
        create("releaseBuild") {
            val pwd = System.getenv("MOVTERY_KEYSTORE_PASSWORD")
            storeFile = file("movtery-key.jks")
            storePassword = pwd
            keyAlias = "mtp"
            keyPassword = pwd
        }
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    defaultConfig {
        applicationId = nameId
        minSdk = 26
        targetSdk = 36
        versionCode = launcherVersionCode
        versionName = launcherVersionName
        multiDexEnabled = true //important
        manifestPlaceholders["launcher_name"] = launcherAPPName
    }

    buildTypes {
        val storageProviderId = "$nameId.storage_provider"

        getByName("debug") {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debugConfig")
            resValue("string", "storageProviderAuthorities", "$storageProviderId.debug")
        }
        create("proguard") {
            initWith(getByName("debug"))
            isMinifyEnabled = true
            isShrinkResources = true
        }
        create("proguardNoDebug") {
            initWith(getByName("proguard"))
            isDebuggable = false
        }
        getByName("release") {
            // Don't set to true or java.awt will be a.a or something similar.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            resValue("string", "storageProviderAuthorities", storageProviderId)
            signingConfig = signingConfigs.getByName("releaseBuild")
        }
    }

    sourceSets["main"].java.srcDirs(generatedZalithDir)

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            pickFirsts += listOf("**/libbytehook.so")
        }
    }

    buildFeatures {
        prefab = true
        buildConfig = true
        viewBinding = true
        resValues = true
    }

    buildToolsVersion = "36.0.0"
}

fun generateJavaClass(sourceOutputDir: File, packageName: String, className: String, constantMap: Map<String, String>) {
    val outputDir = File(sourceOutputDir, packageName.replace(".", "/"))
    outputDir.mkdirs()
    val javaFile = File(outputDir, "$className.java")
    val constants = constantMap.entries.joinToString("\n") { (key, value) ->
        "\tpublic static final String $key = \"$value\";"
    }
    javaFile.writeText(
        """
        |/**
        | * Automatically generated file. DO NOT MODIFY
        | */
        |package $packageName;
        |
        |public class $className {
        |$constants
        |}
        """.trimMargin()
    )
    println("Generated Java file: ${javaFile.absolutePath}")
}

tasks.register("generateInfoDistributor") {
    doLast {
        val constantMap = mapOf(
            "CURSEFORGE_API_KEY" to getCFApiKey(),
            "LAUNCHER_NAME" to project.property("launcher_name").toString(),
            "APP_NAME" to project.property("launcher_app_name").toString(),
            "BUILD_TYPE" to getBuildType()
        )
        generateJavaClass(generatedZalithDir, "com.movtery.zalithlauncher", "InfoDistributor", constantMap)
    }
}

tasks.named("preBuild") {
    dependsOn("generateInfoDistributor")
}

dependencies {
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("commons-codec:commons-codec:1.17.1")
    // implementation("com.wu-man:android-bsf-api:3.1.3")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0-beta01")
    implementation("androidx.annotation:annotation:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.core:core-ktx:1.13.0")
    implementation("androidx.palette:palette-ktx:1.0.0")

    implementation("com.github.duanhong169:checkerboarddrawable:1.0.2")
    implementation("com.github.PojavLauncherTeam:portrait-sdp:ed33e89cbc")
    implementation("com.github.PojavLauncherTeam:portrait-ssp:6c02fd739b")
    implementation("com.github.Mathias-Boulay:ExtendedView:1.0.0")
    implementation("com.github.Mathias-Boulay:android_gamepad_remapper:2.0.3")
    implementation("com.github.Mathias-Boulay:virtual-joystick-android:1.14")
    implementation("com.github.skydoves:powerspinner:1.2.7")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.github.angcyo.DslTablayout:TabLayout:3.6.5")

    implementation("com.github.megatronking.stringfog:xor:5.0.0")

    implementation("top.fifthlight.touchcontroller:proxy-client-android:0.0.2")

    // implementation("com.intuit.sdp:sdp-android:1.0.5")
    // implementation("com.intuit.ssp:ssp-android:1.0.5")

    implementation("org.tukaani:xz:1.9")
    // Our version of exp4j can be built from source at
    // https://github.com/PojavLauncherTeam/exp4j
    implementation("net.sourceforge.htmlcleaner:htmlcleaner:2.6.1")
    implementation("com.bytedance:bytehook:1.0.10")

    // implementation("net.sourceforge.streamsupport:streamsupport-cfuture:1.7.0")

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.commonmark:commonmark:0.19.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    implementation("com.getkeepsafe.taptargetview:taptargetview:1.14.0")
    implementation("io.github.petterpx:floatingx:2.3.3")
    implementation("org.greenrobot:eventbus:3.3.1")
    implementation("com.moandjiezana.toml:toml4j:0.7.2") {
        exclude(group = "com.google.code.gson", module = "gson")
    }
}
