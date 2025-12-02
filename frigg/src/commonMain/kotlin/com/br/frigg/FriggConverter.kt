package com.br.frigg

expect class FriggConverter() {
    suspend fun convertWavToMp3(wavPath: String, bitrate: Int = 128): Result<String>
}
