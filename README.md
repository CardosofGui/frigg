# Frigg 🧝🏻‍♀️

[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-blue?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![License](https://img.shields.io/badge/License-GPL--2.0-green.svg)](https://www.gnu.org/licenses/gpl-2.0.html)
[![Version](https://img.shields.io/badge/version-1.1.0-blue.svg)](https://github.com/CardosofGui/frigg)

Uma biblioteca multiplataforma Kotlin para manipulação de áudio e vídeo, disponível para Android e iOS.

## 📖 Sobre Frigg

Frigg é uma figura da mitologia nórdica, sendo a principal deusa, esposa de Odin e deusa do amor, casamento, maternidade e lar. Assim como Frigg cuida e protege o lar, esta biblioteca foi criada para fornecer ferramentas confiáveis e cuidadosas para a manipulação de mídia em suas aplicações multiplataforma.

## 🎯 Descrição

Frigg é uma biblioteca Kotlin Multiplatform que oferece utilitários para manipulação de áudio e vídeo. Desenvolvida com foco em performance, confiabilidade e facilidade de uso, Frigg permite que você trabalhe com mídia de forma consistente em diferentes plataformas.

### Plataformas Suportadas

- ✅ Android (minSdk 24)
- ✅ iOS (arm64, x64, simulator)

### Tecnologias

- **Kotlin Multiplatform**: Código compartilhado entre plataformas
- **LAME**: Biblioteca nativa para codificação MP3
- **Expect/Actual**: Implementações específicas por plataforma
- **Result API**: Uso do padrão `Result<String>` do Kotlin para tratamento de erros

## ✨ Funcionalidades

### Atualmente Disponível

- 🎵 **Conversão WAV para MP3**
  - Suporte a bitrate configurável (padrão: 128 kbps)
  - Validação robusta de arquivos de entrada
  - Sistema de exceções tipadas para tratamento de erros
  - Validação de formato PCM 16-bit
  - Verificação de permissões e espaço em disco
  - API moderna usando `Result<String>` do Kotlin

### Em Desenvolvimento

- 🎬 Funcionalidades de manipulação de vídeo
- 🎚️ Mais formatos de áudio
- 🔧 Ferramentas adicionais de processamento

## 📦 Instalação

### Gradle (Kotlin DSL)

Adicione o repositório Maven Central no seu `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
}
```

Adicione a dependência:

```kotlin
dependencies {
    implementation("io.github.cardosofgui:frigg:1.1.0")
}
```

### Gradle (Groovy)

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'io.github.cardosofgui:frigg:1.1.0'
}
```

## 🚀 Uso Básico

### Android

No Android, você precisa inicializar a biblioteca no `Application` ou `Activity`:

```kotlin
import android.app.Application
import com.br.frigg.FriggConverter

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FriggConverter.initialize(this)
    }
}
```

### Conversão de WAV para MP3

```kotlin
import com.br.frigg.FriggConverter

suspend fun convertAudio() {
    val converter = FriggConverter()
    val wavPath = "/path/to/audio.wav"
    
    val result = converter.convertWavToMp3(wavPath, bitrate = 128)
    
    result.onSuccess { mp3Path ->
        println("Conversão bem-sucedida! MP3 salvo em: $mp3Path")
    }.onFailure { exception ->
        println("Erro na conversão: ${exception.message}")
        exception.printStackTrace()
    }
}
```

### Exemplo Completo com Coroutines

```kotlin
import com.br.frigg.FriggConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun convertAudioFile(wavPath: String) {
    val converter = FriggConverter()
    
    CoroutineScope(Dispatchers.IO).launch {
        val result = converter.convertWavToMp3(
            wavPath = wavPath,
            bitrate = 192 // Qualidade maior
        )
        
        result.onSuccess { mp3Path ->
            // Arquivo MP3 criado com sucesso
            // Faça algo com o arquivo MP3
        }.onFailure { exception ->
            // Trate o erro
            when (exception) {
                is com.br.frigg.FileNotFoundException -> {
                    // Arquivo não encontrado
                }
                is com.br.frigg.InvalidFileException -> {
                    // Arquivo inválido ou corrompido
                }
                is com.br.frigg.StorageException -> {
                    // Espaço insuficiente em disco
                }
                else -> {
                    // Outros erros
                }
            }
        }
    }
}
```

### Usando getOrElse

```kotlin
import com.br.frigg.FriggConverter

suspend fun convertWithFallback(wavPath: String): String {
    val converter = FriggConverter()
    
    return converter.convertWavToMp3(wavPath)
        .getOrElse { exception ->
            // Tratamento de erro personalizado
            throw exception
        }
}
```

### Usando fold

```kotlin
import com.br.frigg.FriggConverter

suspend fun convertWithFold(wavPath: String): String {
    val converter = FriggConverter()
    
    return converter.convertWavToMp3(wavPath).fold(
        onSuccess = { mp3Path -> mp3Path },
        onFailure = { exception -> 
            throw exception
        }
    )
}
```

## 🛡️ Tratamento de Erros

Frigg utiliza um sistema completo de exceções tipadas que permite tratamento específico de diferentes tipos de erros:

### Hierarquia de Exceções

```kotlin
FriggException (classe base)
├── StorageException
├── WritePermissionException
├── InvalidFileException
├── FileNotFoundException
├── ReadPermissionException
├── DirectoryCreationException
├── NativeLibraryException
├── ConversionException
├── EmptyFileException
└── UnknownFriggException
```

### Exemplos de Tratamento

```kotlin
import com.br.frigg.*
import com.br.frigg.FriggConverter

suspend fun convertWithDetailedErrorHandling(wavPath: String) {
    val converter = FriggConverter()
    
    converter.convertWavToMp3(wavPath).onFailure { exception ->
        when (exception) {
            is FileNotFoundException -> {
                println("Arquivo não encontrado: ${exception.filePath}")
            }
            is ReadPermissionException -> {
                println("Sem permissão para ler: ${exception.filePath}")
            }
            is InvalidFileException -> {
                println("Arquivo inválido: ${exception.filePath}")
                println("Motivo: ${exception.reason}")
            }
            is StorageException -> {
                println("Espaço insuficiente")
                println("Disponível: ${exception.availableSpace} bytes")
                println("Necessário: ${exception.requiredSpace} bytes")
            }
            is WritePermissionException -> {
                println("Sem permissão para escrever em: ${exception.path}")
            }
            is DirectoryCreationException -> {
                println("Erro ao criar diretório: ${exception.directoryPath}")
            }
            is NativeLibraryException -> {
                println("Erro ao carregar biblioteca nativa: ${exception.libraryName}")
            }
            is ConversionException -> {
                println("Erro na conversão")
                println("WAV: ${exception.wavPath}")
                println("MP3: ${exception.mp3Path}")
            }
            is EmptyFileException -> {
                println("Arquivo MP3 vazio: ${exception.filePath}")
            }
            is UnknownFriggException -> {
                println("Erro desconhecido: ${exception.message}")
                exception.cause?.printStackTrace()
            }
            else -> {
                println("Erro inesperado: ${exception.message}")
            }
        }
    }
}
```

## 📚 API Reference

### `FriggConverter`

Classe principal para conversão de áudio.

#### Métodos

##### `convertWavToMp3(wavPath: String, bitrate: Int = 128): Result<String>`

Converte um arquivo WAV para MP3.

**Parâmetros:**
- `wavPath`: Caminho completo para o arquivo WAV de entrada
- `bitrate`: Taxa de bits do MP3 de saída (padrão: 128 kbps)

**Retorno:**
- `Result.success(mp3Path: String)`: Conversão bem-sucedida, retorna o caminho do arquivo MP3 criado
- `Result.failure(exception: FriggException)`: Erro na conversão, contém uma exceção tipada

**Requisitos do arquivo WAV:**
- Formato: PCM 16-bit
- Extensão: `.wav`
- Arquivo válido e legível

**Android:**
- Método estático `initialize(context: Context)` deve ser chamado antes do uso

### Sistema de Exceções

#### `FriggException`

Classe base para todas as exceções do módulo Frigg.

```kotlin
open class FriggException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
```

#### `StorageException`

Lançada quando não há espaço suficiente em disco.

```kotlin
open class StorageException(
    message: String,
    val availableSpace: Long,
    val requiredSpace: Long,
    cause: Throwable? = null
) : FriggException(message, cause)
```

#### `WritePermissionException`

Lançada quando não há permissão para escrever no diretório de destino.

```kotlin
open class WritePermissionException(
    message: String,
    val path: String,
    cause: Throwable? = null
) : FriggException(message, cause)
```

#### `InvalidFileException`

Lançada quando o arquivo WAV é inválido, corrompido ou em formato não suportado.

```kotlin
open class InvalidFileException(
    message: String,
    val filePath: String,
    val reason: String,
    cause: Throwable? = null
) : FriggException(message, cause)
```

#### `FileNotFoundException`

Lançada quando o arquivo WAV não foi encontrado.

```kotlin
open class FileNotFoundException(
    message: String,
    val filePath: String,
    cause: Throwable? = null
) : FriggException(message, cause)
```

#### `ReadPermissionException`

Lançada quando não há permissão para ler o arquivo WAV.

```kotlin
open class ReadPermissionException(
    message: String,
    val filePath: String,
    cause: Throwable? = null
) : FriggException(message, cause)
```

#### `DirectoryCreationException`

Lançada quando não é possível criar o diretório de saída.

```kotlin
open class DirectoryCreationException(
    message: String,
    val directoryPath: String,
    cause: Throwable? = null
) : FriggException(message, cause)
```

#### `NativeLibraryException`

Lançada quando há erro ao carregar a biblioteca nativa.

```kotlin
open class NativeLibraryException(
    message: String,
    val libraryName: String? = null,
    cause: Throwable? = null
) : FriggException(message, cause)
```

#### `ConversionException`

Lançada quando a conversão nativa falha.

```kotlin
open class ConversionException(
    message: String,
    val wavPath: String,
    val mp3Path: String,
    cause: Throwable? = null
) : FriggException(message, cause)
```

#### `EmptyFileException`

Lançada quando o arquivo MP3 foi criado mas está vazio ou não foi criado.

```kotlin
open class EmptyFileException(
    message: String,
    val filePath: String,
    cause: Throwable? = null
) : FriggException(message, cause)
```

#### `UnknownFriggException`

Lançada para erros não mapeados.

```kotlin
open class UnknownFriggException(
    message: String,
    override val cause: Throwable
) : FriggException(message, cause)
```

## ⚙️ Requisitos

### Android
- **minSdk**: 24 (Android 7.0)
- **compileSdk**: 36
- **Kotlin**: 2.2.20+

### iOS
- iOS 13.0+
- Suporta dispositivos físicos (arm64) e simuladores (x64, arm64)

### Kotlin
- Versão mínima: 2.2.20

## 🏗️ Estrutura do Projeto

Frigg utiliza a arquitetura **expect/actual** do Kotlin Multiplatform:

```
frigg/
├── src/
│   ├── commonMain/          # Código compartilhado
│   │   └── kotlin/
│   │       └── com/br/frigg/
│   │           ├── FriggConverter.kt      # expect class
│   │           └── FriggException.kt      # Sistema de exceções
│   ├── androidMain/         # Implementação Android
│   │   └── kotlin/
│   │       └── com/br/frigg/
│   │           └── FriggConverter.android.kt  # actual class
│   └── iosMain/             # Implementação iOS
│       └── kotlin/
│           └── com/br/frigg/
│               └── FriggConverter.ios.kt  # actual class
```

## 📝 Licença

Este projeto está licenciado sob a Licença GPL-2.0 - veja o arquivo [LICENSE](LICENSE) para detalhes.

## 🤝 Contribuindo

Contribuições são bem-vindas! Sinta-se à vontade para:

1. Abrir uma [issue](https://github.com/CardosofGui/frigg/issues) para reportar bugs ou sugerir funcionalidades
2. Fazer um [fork](https://github.com/CardosofGui/frigg/fork) do repositório
3. Criar uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
4. Fazer commit das suas mudanças (`git commit -m 'Add some AmazingFeature'`)
5. Fazer push para a branch (`git push origin feature/AmazingFeature`)
6. Abrir um [Pull Request](https://github.com/CardosofGui/frigg/pulls)

## 👤 Autor

**Guilherme Cardoso**

- GitHub: [@CardosofGui](https://github.com/CardosofGui)
- Projeto: [https://github.com/CardosofGui/frigg](https://github.com/CardosofGui/frigg)

## 🔗 Links Úteis

- [Documentação Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [LAME MP3 Encoder](https://lame.sourceforge.io/)
- [Repositório no GitHub](https://github.com/CardosofGui/frigg)

---

Feito com ❤️ usando Kotlin Multiplatform
