package com.br.frigg

sealed class ConversionResult {
    data class Success(val mp3Path: String) : ConversionResult()
    data class Error(val message: String, val cause: Throwable? = null) : ConversionResult()
}

expect class FriggConverter() {
    suspend fun convertWavToMp3(wavPath: String, bitrate: Int = 128): ConversionResult
}
