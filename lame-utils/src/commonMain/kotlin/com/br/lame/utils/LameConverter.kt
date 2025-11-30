package com.br.lame.utils

expect object LameConverter {
    fun convertWavToMp3(wavPath: String, bitrate: Int = 128): Boolean
}

