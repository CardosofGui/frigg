# LAME Utils - Kotlin Multiplatform Module

Este módulo fornece funcionalidades de conversão de áudio WAV para MP3 usando a biblioteca LAME em plataformas Android e iOS.

## Pré-requisitos

- Android NDK (configurado via Android Gradle Plugin)
- CMake 3.18.1 ou superior
- Kotlin Multiplatform configurado
- Para iOS: Xcode e ferramentas de linha de comando

## Configuração Inicial

### 1. Baixar o código-fonte do LAME

Antes de compilar, é necessário baixar o código-fonte do LAME:

```bash
cd lame-utils
./download-lame.sh
```

Isso irá baixar o LAME 3.100 e extrair para `src/native/lame/`.

### 2. Build para Android

O build para Android é automático via Gradle. O CMake irá compilar o LAME e o wrapper para todas as arquiteturas suportadas:
- arm64-v8a
- armeabi-v7a
- x86
- x86_64

```bash
./gradlew :lame-utils:assembleDebug
```

### 3. Build para iOS

Para iOS, é necessário compilar o LAME e o wrapper como bibliotecas estáticas primeiro:

```bash
./build-ios-lame.sh
```

Este script irá:
1. Gerar o `config.h` necessário usando o configure do LAME
2. Compilar o LAME e o wrapper para todas as arquiteturas iOS (arm64, x86_64, arm64-simulator)
3. Gerar as bibliotecas estáticas (.a) em `build/ios-lame/`

Depois, compile o framework Kotlin/Native:

```bash
./gradlew :lame-utils:linkDebugFrameworkIosArm64
./gradlew :lame-utils:linkDebugFrameworkIosSimulatorArm64
```

**Nota:** As bibliotecas estáticas compiladas precisam ser linkadas ao framework Kotlin/Native. Isso pode ser feito adicionando `linkerOpts` no `build.gradle.kts` ou usando um script de build personalizado.

## Uso

```kotlin
import com.br.lame.utils.LameConverter

val success = LameConverter.convertWavToMp3(
    wavPath = "/path/to/audio.wav",
    bitrate = 128
)

if (success) {
    println("Conversão concluída! MP3 gerado em /path/to/audio.mp3")
} else {
    println("Erro na conversão")
}
```

A função `convertWavToMp3` recebe o caminho do arquivo WAV e um bitrate (padrão: 128 kbps). O arquivo MP3 será gerado no mesmo diretório do WAV, com a extensão `.mp3`.

## Estrutura do Projeto

- `src/native/` - Código C/C++ nativo
  - `lame/` - Código-fonte do LAME
  - `wrapper/` - Wrapper C para conversão WAV->MP3
  - `CMakeLists.txt` - Configuração CMake
- `src/androidMain/cpp/` - Bindings JNI para Android
- `src/iosMain/cinterop/` - Definições cinterop para iOS
- `src/commonMain/kotlin/` - API Kotlin multiplataforma

## Notas

- O parsing do arquivo WAV é feito em C para melhor performance
- Apenas arquivos WAV com 16 bits por amostra são suportados
- O arquivo MP3 gerado terá o mesmo nome do WAV, mas com extensão `.mp3`

