package com.br.lame.utils

import com.br.lame.utils.native.convert_wav_to_mp3
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toCString
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileManagerDefaultManager

@OptIn(ExperimentalForeignApi::class)
actual object LameConverter {
    actual fun convertWavToMp3(wavPath: String, bitrate: Int): ConversionResult {
        val fileManager = NSFileManagerDefaultManager
        
        if (!fileManager.fileExistsAtPath(wavPath)) {
            return ConversionResult.Error("Arquivo WAV não encontrado: $wavPath")
        }
        
        if (!fileManager.isReadableFileAtPath(wavPath)) {
            return ConversionResult.Error("Sem permissão para ler o arquivo WAV: $wavPath")
        }
        
        val mp3Path = wavPath.replace(".wav", ".mp3", ignoreCase = true)
        val mp3Dir = mp3Path.substringBeforeLast("/")
        
        if (mp3Dir.isNotEmpty() && !fileManager.fileExistsAtPath(mp3Dir)) {
            if (!fileManager.createDirectoryAtPath(mp3Dir, true, null, null)) {
                return ConversionResult.Error("Não foi possível criar o diretório de saída: $mp3Dir")
            }
        }
        
        if (mp3Dir.isNotEmpty() && !fileManager.isWritableFileAtPath(mp3Dir)) {
            return ConversionResult.Error("Sem permissão para escrever no diretório: $mp3Dir")
        }
        
        return try {
            val wavCString = wavPath.toCString()
            val mp3CString = mp3Path.toCString()
            
            val result = convert_wav_to_mp3(wavCString, mp3CString, bitrate)
            
            if (result != 0) {
                if (fileManager.fileExistsAtPath(mp3Path)) {
                    val attributes = fileManager.attributesOfItemAtPath(mp3Path, null)
                    val fileSize = attributes?.get("NSFileSize") as? Long ?: 0L
                    if (fileSize > 0) {
                        ConversionResult.Success(mp3Path)
                    } else {
                        ConversionResult.Error("Conversão retornou sucesso, mas o arquivo MP3 está vazio: $mp3Path")
                    }
                } else {
                    ConversionResult.Error("Conversão retornou sucesso, mas o arquivo MP3 não foi criado: $mp3Path")
                }
            } else {
                ConversionResult.Error("A conversão falhou. Verifique se o arquivo WAV é válido e se há espaço em disco suficiente.")
            }
        } catch (e: Exception) {
            ConversionResult.Error("Erro durante a conversão: ${e.message}", e)
        }
    }
}

