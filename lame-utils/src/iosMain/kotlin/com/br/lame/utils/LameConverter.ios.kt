package com.br.lame.utils

import com.br.lame.utils.native.convert_wav_to_mp3
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toCString

@OptIn(ExperimentalForeignApi::class)
actual object LameConverter {
    actual fun convertWavToMp3(wavPath: String, bitrate: Int): Boolean {
        val mp3Path = wavPath.replace(".wav", ".mp3", ignoreCase = true)
        
        val wavCString = wavPath.toCString()
        val mp3CString = mp3Path.toCString()
        
        val result = convert_wav_to_mp3(wavCString, mp3CString, bitrate)
        
        return result != 0
    }
}

