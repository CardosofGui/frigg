import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
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

    val xcfName = "lame-utilsKit"

    // Função para evitar repetição no iOS
    fun org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget.configureLameInterop() {
        binaries.framework {
            baseName = xcfName
        }
        compilations.getByName("main") {
            cinterops {
                val lame by creating {
                    defFile(project.file("src/iosMain/cinterop/lame.def"))
                    packageName("com.br.lame.utils.native")
                    compilerOpts(
                        "-I${projectDir}/src/native/wrapper",
                        "-I${projectDir}/src/native/lame/libmp3lame",
                        "-I${projectDir}/src/native/lame/include"
                    )
                }
            }
        }
    }

    iosX64 { configureLameInterop() }
    iosArm64 { configureLameInterop() }
    iosSimulatorArm64 { configureLameInterop() }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                implementation("com.getkeepsafe.relinker:relinker:1.4.5")
                implementation(libs.kotlin.logging)
                implementation(libs.slf4j.android)
            }
        }

        iosMain {
            dependencies { }
        }
    }
}

// ---------------------------------------------------------
// ANDROID — CONFIGURAÇÕES DE NDK, CMAKE, ABI ETC
// ESSA PARTE NÃO PODE FICAR DENTRO DO BLOCO "kotlin"
// ---------------------------------------------------------

android {
    namespace = "com.br.lame.utils"
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
                    cppFlags(
                        "-g",
                        "-DDEBUG_BUILD=1"
                    )
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    externalNativeBuild {
        cmake {
            path = file("src/native/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}
