package com.br.frigg

import com.br.frigg.native.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager

@OptIn(ExperimentalForeignApi::class)
actual class FriggConverter actual constructor() {
    actual suspend fun convertWavToMp3(wavPath: String, bitrate: Int): Result<String> {
        val fileManager = NSFileManager.defaultManager
        
        if (!fileManager.fileExistsAtPath(wavPath)) {
            return Result.failure(FileNotFoundException("Arquivo WAV não encontrado: $wavPath", wavPath))
        }
        
        if (!fileManager.isReadableFileAtPath(wavPath)) {
            return Result.failure(ReadPermissionException("Sem permissão para ler o arquivo WAV: $wavPath", wavPath))
        }
        
        val mp3Path = wavPath.replace(".wav", ".mp3", ignoreCase = true)
        val mp3Dir = mp3Path.substringBeforeLast("/")
        
        if (mp3Dir.isNotEmpty() && !fileManager.fileExistsAtPath(mp3Dir)) {
            if (!fileManager.createDirectoryAtPath(mp3Dir, true, null, null)) {
                return Result.failure(DirectoryCreationException("Não foi possível criar o diretório de saída: $mp3Dir", mp3Dir))
            }
        }
        
        if (mp3Dir.isNotEmpty() && !fileManager.isWritableFileAtPath(mp3Dir)) {
            return Result.failure(WritePermissionException("Sem permissão para escrever no diretório: $mp3Dir", mp3Dir))
        }
        
        return try {
            val result = convert_wav_to_mp3(wavPath, mp3Path, bitrate)
            
            if (result != 0) {
                if (fileManager.fileExistsAtPath(mp3Path)) {
                    val attributes = fileManager.attributesOfItemAtPath(mp3Path, null)
                    val fileSize = attributes?.get("NSFileSize") as? Long ?: 0L
                    if (fileSize > 0) {
                        return Result.success(mp3Path)
                    } else {
                        return Result.failure(EmptyFileException("Conversão retornou sucesso, mas o arquivo MP3 está vazio: $mp3Path", mp3Path))
                    }
                } else {
                    return Result.failure(EmptyFileException("Conversão retornou sucesso, mas o arquivo MP3 não foi criado: $mp3Path", mp3Path))
                }
            } else {
                return Result.failure(ConversionException("A conversão falhou. Verifique se o arquivo WAV é válido e se há espaço em disco suficiente.", wavPath, mp3Path))
            }
        } catch (e: Exception) {
            return Result.failure(UnknownFriggException("Erro durante a conversão: ${e.message}", e))
        }
    }
}
