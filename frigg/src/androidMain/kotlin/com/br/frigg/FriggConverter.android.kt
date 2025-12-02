package com.br.frigg

import android.content.Context
import com.getkeepsafe.relinker.ReLinker
import java.io.File
import java.io.IOException

actual class FriggConverter actual constructor() {

    external fun convertWavToMp3(wavPath: String, mp3Path: String, bitrate: Int): Boolean

    actual suspend fun convertWavToMp3(wavPath: String, bitrate: Int): Result<String> {
        val wavFile = File(wavPath)
        
        validateWavInput(wavFile).getOrElse { return Result.failure(it) }
        
        val wavFileSize = wavFile.length()
        val mp3Path = wavPath.replace(".wav", ".mp3", ignoreCase = true)
        
        prepareOutputDirectory(mp3Path).getOrElse { return Result.failure(it) }
        
        val mp3Dir = File(mp3Path).parentFile
        if (mp3Dir != null) {
            checkAvailableSpace(mp3Dir, wavFileSize).getOrElse { return Result.failure(it) }
        }
        
        val wavValidation = validateWavFile(wavFile)
        if (wavValidation != null) {
            return Result.failure(InvalidFileException(wavValidation, wavPath, wavValidation))
        }
        
        return executeConversion(wavPath, mp3Path, bitrate, wavFileSize)
    }

    private fun validateWavInput(wavFile: File): Result<Unit> {
        if (!wavFile.exists()) {
            val errorMsg = "Arquivo WAV não encontrado: ${wavFile.absolutePath}"
            return Result.failure(FileNotFoundException(errorMsg, wavFile.absolutePath))
        }
        
        val wavFileSize = wavFile.length()
        
        if (wavFileSize == 0L) {
            val errorMsg = "Arquivo WAV está vazio: ${wavFile.absolutePath}"
            return Result.failure(InvalidFileException(errorMsg, wavFile.absolutePath, "Arquivo está vazio"))
        }
        
        if (wavFileSize < 44) {
            val errorMsg = "Arquivo WAV muito pequeno ($wavFileSize bytes). Um arquivo WAV válido precisa ter pelo menos 44 bytes para o header: ${wavFile.absolutePath}"
            return Result.failure(InvalidFileException(errorMsg, wavFile.absolutePath, "Arquivo muito pequeno ($wavFileSize bytes), mínimo necessário: 44 bytes"))
        }
        
        if (!wavFile.canRead()) {
            val errorMsg = "Sem permissão para ler o arquivo WAV: ${wavFile.absolutePath}"
            return Result.failure(ReadPermissionException(errorMsg, wavFile.absolutePath))
        }
        
        return Result.success(Unit)
    }

    private fun prepareOutputDirectory(mp3Path: String): Result<Unit> {
        val mp3File = File(mp3Path)
        val mp3Dir = mp3File.parentFile
        
        if (mp3Dir != null && !mp3Dir.exists()) {
            try {
                val created = mp3Dir.mkdirs()
                if (!created && !mp3Dir.exists()) {
                    val errorMsg = "Não foi possível criar o diretório de saída: ${mp3Dir.absolutePath}"
                    return Result.failure(DirectoryCreationException(errorMsg, mp3Dir.absolutePath))
                }
            } catch (e: Exception) {
                val errorMsg = "Erro ao criar diretório de saída: ${mp3Dir.absolutePath}. Erro: ${e.message}"
                return Result.failure(DirectoryCreationException(errorMsg, mp3Dir.absolutePath, e))
            }
        }
        
        if (mp3Dir != null && !mp3Dir.canWrite()) {
            val errorMsg = "Sem permissão para escrever no diretório: ${mp3Dir.absolutePath}"
            return Result.failure(WritePermissionException(errorMsg, mp3Dir.absolutePath))
        }
        
        return Result.success(Unit)
    }

    private fun checkAvailableSpace(directory: File, requiredSpace: Long): Result<Unit> {
        val availableSpace = directory.freeSpace
        
        if (availableSpace < requiredSpace) {
            val errorMsg = "Espaço insuficiente no disco. Disponível: $availableSpace bytes, necessário: aproximadamente $requiredSpace bytes"
            return Result.failure(StorageException(errorMsg, availableSpace, requiredSpace))
        }
        
        return Result.success(Unit)
    }

    private fun executeConversion(wavPath: String, mp3Path: String, bitrate: Int, wavFileSize: Long): Result<String> {
        return try {
            val result = convertWavToMp3(wavPath, mp3Path, bitrate)
            
            if (result) {
                val mp3File = File(mp3Path)
                if (mp3File.exists()) {
                    val mp3FileSize = mp3File.length()
                    if (mp3FileSize > 0) {
                        Result.success(mp3Path)
                    } else {
                        val errorMsg = "Conversão retornou sucesso, mas o arquivo MP3 está vazio: $mp3Path"
                        Result.failure(EmptyFileException(errorMsg, mp3Path))
                    }
                } else {
                    val errorMsg = "Conversão retornou sucesso, mas o arquivo MP3 não foi criado: $mp3Path"
                    Result.failure(EmptyFileException(errorMsg, mp3Path))
                }
            } else {
                val errorMsg = "A conversão falhou. Verifique os logs nativos (FriggConverterNative) para detalhes. " +
                        "Possíveis causas: Arquivo WAV inválido ou corrompido, formato de áudio não suportado (apenas PCM 16-bit é suportado), " +
                        "erro ao abrir/criar arquivos, erro na inicialização do encoder LAME, erro durante a codificação. " +
                        "Arquivo WAV: $wavPath ($wavFileSize bytes). Arquivo MP3 esperado: $mp3Path"
                Result.failure(ConversionException(errorMsg, wavPath, mp3Path))
            }
        } catch (e: UnsatisfiedLinkError) {
            val errorMsg = "Erro ao carregar biblioteca nativa: ${e.message}"
            Result.failure(NativeLibraryException(errorMsg, null, e))
        } catch (e: IOException) {
            val errorMsg = "Erro de I/O durante a conversão: ${e.message}"
            Result.failure(UnknownFriggException(errorMsg, e))
        } catch (e: SecurityException) {
            val errorMsg = "Erro de permissão durante a conversão: ${e.message}"
            Result.failure(WritePermissionException(errorMsg, mp3Path, e))
        } catch (e: Exception) {
            val errorMsg = "Erro inesperado durante a conversão: ${e.javaClass.simpleName} - ${e.message}"
            Result.failure(UnknownFriggException(errorMsg, e))
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

                        if (audioFormat != 1.toShort()) {
                            return "Formato de áudio não suportado. Apenas PCM (1) é aceito, encontrado $audioFormat."
                        }

                        if (bitsPerSample != 16.toShort()) {
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

    private fun byteArrayToShortLE(b: ByteArray, offset: Int): Short {
        return ((b[offset].toInt() and 0xFF) or
                ((b[offset + 1].toInt() and 0xFF) shl 8)).toShort()
    }

    companion object {
        fun initialize(context: Context) {
            ReLinker.loadLibrary(context, "c++_shared")
            ReLinker.loadLibrary(context, "wav_to_mp3")
        }
    }
}
