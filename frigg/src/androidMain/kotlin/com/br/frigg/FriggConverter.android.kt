package com.br.frigg

import android.content.Context
import com.br.frigg.logging.FriggLogging
import com.getkeepsafe.relinker.ReLinker
import java.io.File
import java.io.IOException

private val logger = FriggLogging.logger("FriggConverter")

actual class FriggConverter actual constructor() {

    external fun convertWavToMp3(wavPath: String, mp3Path: String, bitrate: Int): Boolean

    actual suspend fun convertWavToMp3(wavPath: String, bitrate: Int): Result<String> {
        logger.info { "Iniciando conversão WAV para MP3: $wavPath com bitrate $bitrate" }
        
        val wavFile = File(wavPath)
        if (!wavFile.exists()) {
            val errorMsg = "Arquivo WAV não encontrado: $wavPath"
            logger.warn { errorMsg }
            return Result.failure(FileNotFoundException(errorMsg, wavPath))
        }
        
        val wavFileSize = wavFile.length()
        logger.info { "Arquivo WAV encontrado: $wavPath" }
        logger.info { "Tamanho do arquivo WAV: $wavFileSize bytes (${wavFileSize / 1024} KB)" }
        
        if (wavFileSize == 0L) {
            val errorMsg = "Arquivo WAV está vazio: $wavPath"
            logger.error { errorMsg }
            return Result.failure(InvalidFileException(errorMsg, wavPath, "Arquivo está vazio"))
        }
        
        if (wavFileSize < 44) {
            val errorMsg = "Arquivo WAV muito pequeno (${wavFileSize} bytes). Um arquivo WAV válido precisa ter pelo menos 44 bytes para o header: $wavPath"
            logger.error { errorMsg }
            return Result.failure(InvalidFileException(errorMsg, wavPath, "Arquivo muito pequeno (${wavFileSize} bytes), mínimo necessário: 44 bytes"))
        }
        
        if (!wavFile.canRead()) {
            val errorMsg = "Sem permissão para ler o arquivo WAV: $wavPath"
            logger.error { errorMsg }
            return Result.failure(ReadPermissionException(errorMsg, wavPath))
        }
        
        logger.debug { "Permissões do arquivo WAV: readable=true, absolutePath=${wavFile.absolutePath}" }

        val mp3Path = wavPath.replace(".wav", ".mp3", ignoreCase = true)
        logger.debug { "Caminho do arquivo MP3 de saída: $mp3Path" }
        
        val mp3File = File(mp3Path)
        val mp3Dir = mp3File.parentFile
        
        if (mp3Dir != null && !mp3Dir.exists()) {
            try {
                val created = mp3Dir.mkdirs()
                logger.debug { "Tentativa de criar diretório ${mp3Dir.absolutePath}: $created" }
                if (!created && !mp3Dir.exists()) {
                    val errorMsg = "Não foi possível criar o diretório de saída: ${mp3Dir.absolutePath}"
                    logger.error { errorMsg }
                    return Result.failure(DirectoryCreationException(errorMsg, mp3Dir.absolutePath))
                }
            } catch (e: Exception) {
                val errorMsg = "Erro ao criar diretório de saída: ${mp3Dir.absolutePath}. Erro: ${e.message}"
                logger.error(e) { errorMsg }
                return Result.failure(DirectoryCreationException(errorMsg, mp3Dir.absolutePath, e))
            }
        }
        
        if (mp3Dir != null && !mp3Dir.canWrite()) {
            val errorMsg = "Sem permissão para escrever no diretório: ${mp3Dir.absolutePath}"
            logger.warn { errorMsg }
            return Result.failure(WritePermissionException(errorMsg, mp3Dir.absolutePath))
        }
        
        val availableSpace = mp3Dir?.freeSpace ?: 0L
        logger.debug { "Espaço disponível no diretório: $availableSpace bytes" }
        
        if (availableSpace < wavFileSize) {
            val errorMsg = "Espaço insuficiente no disco. Disponível: $availableSpace bytes, necessário: aproximadamente ${wavFileSize} bytes"
            logger.warn { errorMsg }
            return Result.failure(StorageException(errorMsg, availableSpace, wavFileSize))
        }
        
        logger.info { "Iniciando validação do arquivo WAV..." }
        val wavValidation = validateWavFile(wavFile)
        if (wavValidation != null) {
            logger.error { "Validação WAV falhou: $wavValidation" }
            return Result.failure(InvalidFileException(wavValidation, wavPath, wavValidation))
        }
        logger.info { "Validação WAV concluída com sucesso" }
        
        logger.info { "Chamando função nativa convertWavToMp3..." }
        logger.info { "Parâmetros: wavPath=$wavPath, mp3Path=$mp3Path, bitrate=$bitrate" }
        
        return try {
            logger.info { "Executando conversão nativa..." }
            val startTime = System.currentTimeMillis()
            val result = convertWavToMp3(wavPath, mp3Path, bitrate)
            val duration = System.currentTimeMillis() - startTime
            logger.info { "Função nativa retornou: $result (tempo: ${duration}ms)" }
            
            if (result) {
                if (mp3File.exists()) {
                    val mp3FileSize = mp3File.length()
                    logger.info { "Arquivo MP3 criado: $mp3Path" }
                    logger.info { "Tamanho do arquivo MP3: $mp3FileSize bytes (${mp3FileSize / 1024} KB)" }
                    if (mp3FileSize > 0) {
                        val compressionRatio = (wavFileSize.toDouble() / mp3FileSize.toDouble() * 100).toInt()
                        logger.info { "Conversão concluída com sucesso: $mp3Path (${mp3FileSize} bytes, ${compressionRatio}% do tamanho original)" }
                        return Result.success(mp3Path)
                    } else {
                        val errorMsg = "Conversão retornou sucesso, mas o arquivo MP3 está vazio: $mp3Path"
                        logger.error { errorMsg }
                        return Result.failure(EmptyFileException(errorMsg, mp3Path))
                    }
                } else {
                    val errorMsg = "Conversão retornou sucesso, mas o arquivo MP3 não foi criado: $mp3Path"
                    logger.error { errorMsg }
                    logger.error { "Diretório existe: ${mp3File.parentFile?.exists()}, pode escrever: ${mp3File.parentFile?.canWrite()}" }
                    return Result.failure(EmptyFileException(errorMsg, mp3Path))
                }
            } else {
                val errorMsg = buildString {
                    appendLine("A conversão falhou. Verifique os logs nativos (FriggConverterNative) para detalhes.")
                    appendLine("Possíveis causas:")
                    appendLine("1. Arquivo WAV inválido ou corrompido")
                    appendLine("2. Formato de áudio não suportado (apenas PCM 16-bit é suportado)")
                    appendLine("3. Erro ao abrir/criar arquivos")
                    appendLine("4. Erro na inicialização do encoder LAME")
                    appendLine("5. Erro durante a codificação")
                    appendLine("Arquivo WAV: $wavPath (${wavFileSize} bytes)")
                    appendLine("Arquivo MP3 esperado: $mp3Path")
                    appendLine("Tempo de execução: ${duration}ms")
                }
                logger.error { errorMsg }
                return Result.failure(ConversionException(errorMsg, wavPath, mp3Path))
            }
        } catch (e: UnsatisfiedLinkError) {
            val errorMsg = "Erro ao carregar biblioteca nativa: ${e.message}"
            logger.error(e) { errorMsg }
            return Result.failure(NativeLibraryException(errorMsg, null, e))
        } catch (e: IOException) {
            val errorMsg = "Erro de I/O durante a conversão: ${e.message}"
            logger.error(e) { errorMsg }
            return Result.failure(UnknownFriggException(errorMsg, e))
        } catch (e: SecurityException) {
            val errorMsg = "Erro de permissão durante a conversão: ${e.message}"
            logger.error(e) { errorMsg }
            return Result.failure(WritePermissionException(errorMsg, mp3Path, e))
        } catch (e: Exception) {
            val errorMsg = "Erro inesperado durante a conversão: ${e.javaClass.simpleName} - ${e.message}"
            logger.error(e) { errorMsg }
            return Result.failure(UnknownFriggException(errorMsg, e))
        }
    }
    private fun validateWavFile(wavFile: File): String? {
        return try {
            wavFile.inputStream().use { input ->

                val header = ByteArray(12)
                if (input.read(header) < 12) {
                    return "Arquivo WAV muito pequeno ou corrompido. Cabeçalho incompleto."
                }

                val riff = String(header, 0, 4)
                val wave = String(header, 8, 4)

                if (riff != "RIFF") {
                    val detected = when {
                        riff.startsWith("ID3") -> "MP3 (ID3)"
                        riff.startsWith("Ogg") -> "OGG"
                        riff.startsWith("fLaC") -> "FLAC"
                        riff.startsWith("ftyp") -> "MP4/M4A"
                        else -> "desconhecido ('$riff')"
                    }
                    return "Arquivo não é WAV válido. Formato detectado: $detected."
                }

                if (wave != "WAVE") {
                    return "Arquivo inválido. Era esperado 'WAVE', encontrado '$wave'."
                }

                val chunkHeader = ByteArray(8)
                var bytesSearched = 12
                var foundFmt = false

                while (bytesSearched < 1024) {
                    if (input.read(chunkHeader) < 8) {
                        return "Arquivo WAV corrompido. Não foi possível ler próximo chunk."
                    }

                    val chunkId = String(chunkHeader, 0, 4)
                    val chunkSize = byteArrayToIntLE(chunkHeader, 4)

                    if (chunkId == "fmt ") {
                        foundFmt = true

                        val fmt = ByteArray(chunkSize)
                        if (input.read(fmt) < chunkSize) {
                            return "Arquivo WAV corrompido. Chunk 'fmt ' incompleto."
                        }

                        val audioFormat = byteArrayToShortLE(fmt, 0)
                        val bitsPerSample = byteArrayToShortLE(fmt, 14)

                        if (audioFormat != 1) {
                            return "Formato de áudio não suportado. Apenas PCM (1) é aceito, encontrado $audioFormat."
                        }

                        if (bitsPerSample != 16) {
                            return "Bits por sample não suportado. Apenas 16-bit é aceito, encontrado $bitsPerSample-bit."
                        }

                        return null
                    }

                    input.skip(chunkSize.toLong())
                    bytesSearched += 8 + chunkSize
                }

                if (!foundFmt) {
                    return "Arquivo WAV inválido. Chunk 'fmt ' não encontrado."
                }

                null
            }
        } catch (e: Exception) {
            "Erro ao ler arquivo WAV: ${e.message}"
        }
    }
    private fun byteArrayToIntLE(b: ByteArray, offset: Int): Int {
        return (b[offset].toInt() and 0xFF) or
                ((b[offset + 1].toInt() and 0xFF) shl 8) or
                ((b[offset + 2].toInt() and 0xFF) shl 16) or
                ((b[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun byteArrayToShortLE(b: ByteArray, offset: Int): Int {
        return (b[offset].toInt() and 0xFF) or
                ((b[offset + 1].toInt() and 0xFF) shl 8)
    }

    companion object {
        fun initialize(context: Context) {
            ReLinker.loadLibrary(context, "c++_shared")
            ReLinker.loadLibrary(context, "wav_to_mp3")
        }
    }
}
