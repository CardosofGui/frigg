import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.JavadocJar

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    signing
    id("com.vanniktech.maven.publish") version "0.34.0"
}

kotlin {

    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                }
            }
        }
    }

    val xcfName = "friggKit"

    val iosX64 = iosX64()
    val iosArm64 = iosArm64()
    val iosSimulatorArm64 = iosSimulatorArm64()

    listOf(iosX64, iosArm64, iosSimulatorArm64).forEach { target ->
        target.binaries.framework {
            baseName = xcfName
            isStatic = true
        }
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        compilations.getByName("main") {
            cinterops.create("lame") {
                defFile(project.file("src/iosMain/cinterop/lame.def"))
                packageName = "com.br.frigg.native"
                includeDirs {
                    allHeaders("src/native/wrapper")
                    allHeaders("src/native/lame/libmp3lame")
                    allHeaders("src/native/lame/include")
                }
                extraOpts("-verbose")
            }
        }

        compilations.getByName("main") {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xskip-prerelease-check")
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(projects.friggLogging)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.relinker)
            }
        }

        iosMain {}
    }
}

android {
    namespace = "com.br.frigg"
    compileSdk = 36
    ndkVersion = "26.1.10909125"

    defaultConfig {
        minSdk = 24

        externalNativeBuild {
            cmake {
                arguments(
                    "-DANDROID_ARM_NEON=ON",
                    "-D__ARM_NEON__=1",
                    "-DANDROID_TOOLCHAIN=clang",
                    "-DANDROID_ARM_MODE=arm",
                    "-DCMAKE_ANDROID_ARM_NEON=TRUE",
                    "-DANDROID_STL=c++_shared"
                )

                cppFlags(
                    "-O3",
                    "-ffast-math",
                    "-ftree-vectorize",
                    "-funroll-loops",
                    "-fomit-frame-pointer",
                    "-finline-functions",
                    "-DNDEBUG=1"
                )

                cFlags(
                    "-O3",
                    "-ffast-math",
                    "-ftree-vectorize"
                )

                abiFilters("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            externalNativeBuild {
                cmake {
                    arguments(
                        "-DCMAKE_BUILD_TYPE=Release",
                        "-DCMAKE_C_FLAGS_RELEASE=-O3 -flto -ffast-math",
                        "-DCMAKE_CXX_FLAGS_RELEASE=-O3 -flto -ffast-math"
                    )
                    abiFilters("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
                }
            }
        }

        debug {
            externalNativeBuild {
                cmake {
                    arguments(
                        "-DCMAKE_BUILD_TYPE=RelWithDebInfo",
                        "-DCMAKE_C_FLAGS_RELWITHDEBINFO=-O2 -g -ffast-math",
                        "-DCMAKE_CXX_FLAGS_RELWITHDEBINFO=-O2 -g -ffast-math"
                    )
                    cppFlags("-g", "-DDEBUG_BUILD=1")
                }
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/native/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

tasks.matching { it.name.contains("compileIosMainKotlinMetadata") }.configureEach {
    enabled = false
}

// ---------------------------------------------------------
//     PUBLICAÇÃO USANDO VANNIKTECH
// ---------------------------------------------------------
group = "io.github.cardosofgui"
version = "1.0.0"

mavenPublishing {

    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Empty(),
            sourcesJar = true,
            androidVariantsToPublish = listOf("release")
        )
    )

    coordinates(
        groupId = "io.github.cardosofgui",
        artifactId = "frigg",
        version = "1.0.0"
    )

    pom {
        name.set("frigg")
        description.set("Kotlin Multiplatform library for WAV → MP3 conversion using LAME")
        url.set("https://github.com/CardosofGui/frigg")

        licenses {
            license {
                name.set("GPL-2.0")
                url.set("https://www.gnu.org/licenses/gpl-2.0.html")
            }
        }

        scm {
            url.set("https://github.com/CardosofGui/frigg")
            connection.set("scm:git:https://github.com/CardosofGui/frigg.git")
            developerConnection.set("scm:git:ssh://github.com/CardosofGui/frigg.git")
        }

        developers {
            developer {
                id.set("cardosofgui")
                name.set("Guilherme Cardoso")
                email.set("")
            }
        }
    }
}