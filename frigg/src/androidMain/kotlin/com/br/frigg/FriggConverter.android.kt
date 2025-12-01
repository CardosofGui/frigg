package com.br.frigg

import android.content.Context
import com.getkeepsafe.relinker.ReLinker
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.IOException

private val logger = KotlinLogging.logger("FriggConverter")

actual class FriggConverter actual constructor() {

    external fun convertWavToMp3(wavPath: String, mp3Path: String, bitrate: Int): Boolean

    actual suspend fun convertWavToMp3(wavPath: String, bitrate: Int): ConversionResult {
        logger.info { "Iniciando conversão WAV para MP3: $wavPath com bitrate $bitrate" }
        
        val wavFile = File(wavPath)
        if (!wavFile.exists()) {
            val errorMsg = "Arquivo WAV não encontrado: $wavPath"
            logger.warn { errorMsg }
            return ConversionResult.Error(errorMsg)
        }
        
        val wavFileSize = wavFile.length()
        logger.info { "Arquivo WAV encontrado: $wavPath" }
        logger.info { "Tamanho do arquivo WAV: $wavFileSize bytes (${wavFileSize / 1024} KB)" }
        
        if (wavFileSize == 0L) {
            val errorMsg = "Arquivo WAV está vazio: $wavPath"
            logger.error { errorMsg }
            return ConversionResult.Error(errorMsg)
        }
        
        if (wavFileSize < 44) {
            val errorMsg = "Arquivo WAV muito pequeno (${wavFileSize} bytes). Um arquivo WAV válido precisa ter pelo menos 44 bytes para o header: $wavPath"
            logger.error { errorMsg }
            return ConversionResult.Error(errorMsg)
        }
        
        if (!wavFile.canRead()) {
            val errorMsg = "Sem permissão para ler o arquivo WAV: $wavPath"
            logger.error { errorMsg }
            return ConversionResult.Error(errorMsg)
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
                    return ConversionResult.Error(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "Erro ao criar diretório de saída: ${mp3Dir.absolutePath}. Erro: ${e.message}"
                logger.error(e) { errorMsg }
                return ConversionResult.Error(errorMsg, e)
            }
        }
        
        if (mp3Dir != null && !mp3Dir.canWrite()) {
            val errorMsg = "Sem permissão para escrever no diretório: ${mp3Dir.absolutePath}"
            logger.warn { errorMsg }
            return ConversionResult.Error(errorMsg)
        }
        
        val availableSpace = mp3Dir?.freeSpace ?: 0L
        logger.debug { "Espaço disponível no diretório: $availableSpace bytes" }
        
        if (availableSpace < wavFileSize) {
            val errorMsg = "Espaço insuficiente no disco. Disponível: $availableSpace bytes, necessário: aproximadamente ${wavFileSize} bytes"
            logger.warn { errorMsg }
            return ConversionResult.Error(errorMsg)
        }
        
        logger.info { "Iniciando validação do arquivo WAV..." }
        val wavValidation = validateWavFile(wavFile)
        if (wavValidation != null) {
            logger.error { "Validação WAV falhou: $wavValidation" }
            return ConversionResult.Error(wavValidation)
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
                        ConversionResult.Success(mp3Path)
                    } else {
                        val errorMsg = "Conversão retornou sucesso, mas o arquivo MP3 está vazio: $mp3Path"
                        logger.error { errorMsg }
                        ConversionResult.Error(errorMsg)
                    }
                } else {
                    val errorMsg = "Conversão retornou sucesso, mas o arquivo MP3 não foi criado: $mp3Path"
                    logger.error { errorMsg }
                    logger.error { "Diretório existe: ${mp3File.parentFile?.exists()}, pode escrever: ${mp3File.parentFile?.canWrite()}" }
                    ConversionResult.Error(errorMsg)
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
                ConversionResult.Error(errorMsg)
            }
        } catch (e: UnsatisfiedLinkError) {
            val errorMsg = "Erro ao carregar biblioteca nativa: ${e.message}"
            logger.error(e) { errorMsg }
            ConversionResult.Error(errorMsg, e)
        } catch (e: IOException) {
            val errorMsg = "Erro de I/O durante a conversão: ${e.message}"
            logger.error(e) { errorMsg }
            ConversionResult.Error(errorMsg, e)
        } catch (e: SecurityException) {
            val errorMsg = "Erro de permissão durante a conversão: ${e.message}"
            logger.error(e) { errorMsg }
            ConversionResult.Error(errorMsg, e)
        } catch (e: Exception) {
            val errorMsg = "Erro inesperado durante a conversão: ${e.javaClass.simpleName} - ${e.message}"
            logger.error(e) { errorMsg }
            ConversionResult.Error(errorMsg, e)
        }
    }
    private fun validateWavFile(wavFile: File): String? {
        return try {
            val inputStream = wavFile.inputStream()
            val headerBuffer = ByteArray(12)
            val bytesRead = inputStream.read(headerBuffer)
            
            if (bytesRead < 12) {
                inputStream.close()
                return "Arquivo WAV muito pequeno ou corrompido. Header incompleto (${bytesRead} bytes lidos, esperado pelo menos 12 bytes)"
            }
            
            val chunkId = String(headerBuffer, 0, 4)
            if (chunkId != "RIFF") {
                inputStream.close()
                val detectedFormat = when {
                    chunkId.startsWith("ID3") -> "MP3 (detectado header ID3)"
                    chunkId.startsWith("Ogg") -> "OGG"
                    chunkId.startsWith("fLaC") -> "FLAC"
                    chunkId.startsWith("ftyp") -> "MP4/M4A"
                    else -> "desconhecido (header: '$chunkId')"
                }
                return "Arquivo selecionado não é um WAV válido. Formato detectado: $detectedFormat. Por favor, selecione um arquivo WAV (PCM 16-bit)."
            }
            
            val format = String(headerBuffer, 8, 4)
            if (format != "WAVE") {
                inputStream.close()
                return "Arquivo não é um WAV válido. Format esperado: 'WAVE', encontrado: '$format'"
            }
            
            var position = 12
            var foundFmt = false
            val chunkBuffer = ByteArray(8)
            
            while (position < 1024) {
                val read = inputStream.read(chunkBuffer)
                if (read < 8) {
                    inputStream.close()
                    return "Arquivo WAV corrompido. Não foi possível encontrar o chunk 'fmt '"
                }
                
                val chunkId = String(chunkBuffer, 0, 4)
                val chunkSize = (chunkBuffer[4].toInt() and 0xFF) or
                               ((chunkBuffer[5].toInt() and 0xFF) shl 8) or
                               ((chunkBuffer[6].toInt() and 0xFF) shl 16) or
                               ((chunkBuffer[7].toInt() and 0xFF) shl 24)
                
                if (chunkId == "fmt ") {
                    foundFmt = true
                    val fmtBuffer = ByteArray(16)
                    val fmtRead = inputStream.read(fmtBuffer)
                    if (fmtRead < 16) {
                        inputStream.close()
                        return "Arquivo WAV corrompido. Chunk 'fmt ' incompleto"
                    }
                    
                    val audioFormat = (fmtBuffer[0].toInt() and 0xFF) or ((fmtBuffer[1].toInt() and 0xFF) shl 8)
                    if (audioFormat != 1) {
                        inputStream.close()
                        return "Formato de áudio não suportado. Apenas PCM (formato 1) é suportado, encontrado: formato $audioFormat"
                    }
                    
                    val numChannels = (fmtBuffer[2].toInt() and 0xFF) or ((fmtBuffer[3].toInt() and 0xFF) shl 8)
                    val sampleRate = (fmtBuffer[4].toInt() and 0xFF) or 
                                    ((fmtBuffer[5].toInt() and 0xFF) shl 8) or
                                    ((fmtBuffer[6].toInt() and 0xFF) shl 16) or
                                    ((fmtBuffer[7].toInt() and 0xFF) shl 24)
                    val bitsPerSample = (fmtBuffer[14].toInt() and 0xFF) or ((fmtBuffer[15].toInt() and 0xFF) shl 8)
                    
                    logger.info { "Detalhes do WAV: $numChannels canal(is), ${sampleRate}Hz, ${bitsPerSample}-bit, formato PCM" }
                    
                    if (bitsPerSample != 16) {
                        inputStream.close()
                        val errorMsg = "Bits por sample não suportado. Apenas 16-bit é suportado, encontrado: $bitsPerSample-bit"
                        logger.error { errorMsg }
                        return errorMsg
                    }
                    
                    logger.info { "WAV válido e compatível: $numChannels canais, ${sampleRate}Hz, ${bitsPerSample}-bit PCM" }
                    inputStream.close()
                    return null
                } else {
                    inputStream.skip(chunkSize.toLong())
                    position += 8 + chunkSize
                }
            }
            
            inputStream.close()
            if (!foundFmt) {
                return "Arquivo WAV inválido. Chunk 'fmt ' não encontrado"
            }
            
            null
        } catch (e: Exception) {
            logger.error(e) { "Erro ao validar arquivo WAV" }
            "Erro ao ler header do arquivo WAV: ${e.message}"
        }
    }

    companion object {
        fun initialize(context: Context) {
            ReLinker.loadLibrary(context, "c++_shared")
            ReLinker.loadLibrary(context, "wav_to_mp3")
        }
    }
}
