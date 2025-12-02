# Frigg 🧝🏻‍♀️

[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-blue?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![License](https://img.shields.io/badge/License-GPL--2.0-green.svg)](https://www.gnu.org/licenses/gpl-2.0.html)
[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://github.com/CardosofGui/frigg)

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

## ✨ Funcionalidades

### Atualmente Disponível

- 🎵 **Conversão WAV para MP3**
  - Suporte a bitrate configurável (padrão: 128 kbps)
  - Validação robusta de arquivos de entrada
  - Tratamento de erros detalhado
  - Validação de formato PCM 16-bit
  - Verificação de permissões e espaço em disco

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
    implementation("io.github.cardosofgui:frigg:1.0.0")
}
```

### Gradle (Groovy)

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'io.github.cardosofgui:frigg:1.0.0'
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
import com.br.frigg.ConversionResult

suspend fun convertAudio() {
    val converter = FriggConverter()
    val wavPath = "/path/to/audio.wav"
    
    when (val result = converter.convertWavToMp3(wavPath, bitrate = 128)) {
        is ConversionResult.Success -> {
            println("Conversão bem-sucedida! MP3 salvo em: ${result.mp3Path}")
        }
        is ConversionResult.Error -> {
            println("Erro na conversão: ${result.message}")
            result.cause?.printStackTrace()
        }
    }
}
```

### Exemplo Completo com Coroutines

```kotlin
import com.br.frigg.FriggConverter
import com.br.frigg.ConversionResult
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
        
        when (result) {
            is ConversionResult.Success -> {
                // Arquivo MP3 criado com sucesso
                val mp3Path = result.mp3Path
                // Faça algo com o arquivo MP3
            }
            is ConversionResult.Error -> {
                // Trate o erro
                val errorMessage = result.message
                val cause = result.cause
                // Exiba mensagem de erro ao usuário
            }
        }
    }
}
```

## 📚 API Reference

### `FriggConverter`

Classe principal para conversão de áudio.

#### Métodos

##### `convertWavToMp3(wavPath: String, bitrate: Int = 128): ConversionResult`

Converte um arquivo WAV para MP3.

**Parâmetros:**
- `wavPath`: Caminho completo para o arquivo WAV de entrada
- `bitrate`: Taxa de bits do MP3 de saída (padrão: 128 kbps)

**Retorno:**
- `ConversionResult.Success(mp3Path: String)`: Conversão bem-sucedida
- `ConversionResult.Error(message: String, cause: Throwable?)`: Erro na conversão

**Requisitos do arquivo WAV:**
- Formato: PCM 16-bit
- Extensão: `.wav`
- Arquivo válido e legível

**Android:**
- Método estático `initialize(context: Context)` deve ser chamado antes do uso

### `ConversionResult`

Sealed class que representa o resultado da conversão.

```kotlin
sealed class ConversionResult {
    data class Success(val mp3Path: String) : ConversionResult()
    data class Error(val message: String, val cause: Throwable? = null) : ConversionResult()
}
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
│   │           └── FriggConverter.kt  # expect class
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
