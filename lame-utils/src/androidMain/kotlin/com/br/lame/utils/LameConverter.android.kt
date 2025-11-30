package com.br.lame.utils

import android.content.Context
import android.os.Build
import com.getkeepsafe.relinker.ReLinker
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

actual object LameConverter {
    private var isLibraryLoaded = false
    private var appContext: Context? = null
    
    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (!isLibraryLoaded) {
            try {
                extractAndLoadLibraries()
                isLibraryLoaded = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JvmStatic
    external fun convertWavToMp3(wavPath: String, mp3Path: String, bitrate: Int): Boolean

    actual fun convertWavToMp3(wavPath: String, bitrate: Int): Boolean {
        if (!isLibraryLoaded && appContext != null) {
            try {
                extractAndLoadLibraries()
                isLibraryLoaded = true
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }
        
        val wavFile = File(wavPath)
        if (!wavFile.exists()) {
            return false
        }

        val mp3Path = wavPath.replace(".wav", ".mp3", ignoreCase = true)
        return convertWavToMp3(wavPath, mp3Path, bitrate)
    }
    
    private fun extractAndLoadLibraries() {
        val context = appContext ?: return
        val abi = getAbi()
        val libDir = File(context.applicationInfo.nativeLibraryDir)
        
        if (!libDir.exists()) {
            libDir.mkdirs()
        }
        
        val cppSharedLib = File(libDir, "libc++_shared.so")
        val wavToMp3Lib = File(libDir, "libwav_to_mp3.so")
        
        if (!cppSharedLib.exists()) {
            extractAsset("lib/$abi/libc++_shared.so", cppSharedLib)
        }
        
        if (!wavToMp3Lib.exists()) {
            extractAsset("lib/$abi/libwav_to_mp3.so", wavToMp3Lib)
        }
        
        ReLinker.loadLibrary(context, "c++_shared")
        ReLinker.loadLibrary(context, "wav_to_mp3")
    }
    
    private fun extractAsset(assetPath: String, destination: File) {
        val context = appContext ?: return
        
        try {
            val inputStream: InputStream = context.assets.open(assetPath)
            val outputStream = FileOutputStream(destination)
            
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            destination.setExecutable(true, false)
            destination.setReadable(true, false)
        } catch (e: Exception) {
            throw RuntimeException("Failed to extract native library: $assetPath", e)
        }
    }
    
    private fun getAbi(): String {
        return when {
            Build.SUPPORTED_64_BIT_ABIS.isNotEmpty() -> {
                when (Build.SUPPORTED_64_BIT_ABIS[0]) {
                    "arm64-v8a" -> "arm64-v8a"
                    "x86_64" -> "x86_64"
                    else -> "arm64-v8a"
                }
            }
            Build.SUPPORTED_32_BIT_ABIS.isNotEmpty() -> {
                when (Build.SUPPORTED_32_BIT_ABIS[0]) {
                    "armeabi-v7a" -> "armeabi-v7a"
                    "x86" -> "x86"
                    else -> "armeabi-v7a"
                }
            }
            else -> "arm64-v8a"
        }
    }
}
