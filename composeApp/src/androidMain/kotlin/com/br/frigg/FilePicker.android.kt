package com.br.frigg

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

private const val TAG = "FilePicker"

private var activity: ComponentActivity? = null
private var resultCallback: ((String?) -> Unit)? = null

fun initializeFilePicker(componentActivity: ComponentActivity) {
    activity = componentActivity
}

actual suspend fun pickWavFile(): String? = suspendCancellableCoroutine { continuation ->
    val currentActivity = activity ?: run {
        continuation.resume(null)
        return@suspendCancellableCoroutine
    }
    
    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "audio/*"
        addCategory(Intent.CATEGORY_OPENABLE)
        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/wav", "audio/x-wav", "audio/wave"))
    }
    
    resultCallback = { uri ->
        if (uri != null) {
            Log.d(TAG, "URI recebido do seletor de arquivos: $uri")
            val file = copyUriToTempFile(currentActivity, Uri.parse(uri))
            if (file != null) {
                Log.i(TAG, "Arquivo copiado com sucesso: ${file.absolutePath}")
            } else {
                Log.e(TAG, "Falha ao copiar arquivo do URI: $uri")
            }
            continuation.resume(file?.absolutePath)
        } else {
            Log.w(TAG, "Nenhum URI recebido do seletor de arquivos")
            continuation.resume(null)
        }
        resultCallback = null
    }
    
    val launcher = currentActivity.activityResultRegistry.register(
        "file_picker_${System.currentTimeMillis()}",
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            resultCallback?.invoke(uri?.toString())
        } else {
            resultCallback?.invoke(null)
        }
    }
    
    continuation.invokeOnCancellation {
        resultCallback = null
        launcher.unregister()
    }
    
    try {
        Log.d(TAG, "Lançando seletor de arquivos...")
        launcher.launch(intent)
    } catch (e: Exception) {
        Log.e(TAG, "Erro ao lançar seletor de arquivos", e)
        e.printStackTrace()
        continuation.resume(null)
    }
}

private fun copyUriToTempFile(activity: Activity, uri: Uri): File? {
    return try {
        Log.d(TAG, "Iniciando cópia do arquivo do URI: $uri")
        val inputStream = activity.contentResolver.openInputStream(uri)
        if (inputStream == null) {
            Log.e(TAG, "Falha ao abrir InputStream do URI: $uri")
            return null
        }
        
        val tempFile = File(activity.cacheDir, "temp_audio_${System.currentTimeMillis()}.wav")
        Log.d(TAG, "Criando arquivo temporário: ${tempFile.absolutePath}")
        
        val startTime = System.currentTimeMillis()
        var bytesCopied = 0L
        FileOutputStream(tempFile).use { output ->
            bytesCopied = inputStream.copyTo(output)
        }
        inputStream.close()
        val duration = System.currentTimeMillis() - startTime
        
        val fileSize = tempFile.length()
        Log.i(TAG, "Arquivo copiado: ${tempFile.absolutePath}")
        Log.i(TAG, "Tamanho do arquivo: $fileSize bytes (${fileSize / 1024} KB)")
        Log.d(TAG, "Bytes copiados: $bytesCopied, tempo: ${duration}ms")
        
        if (fileSize != bytesCopied) {
            Log.w(TAG, "Aviso: tamanho do arquivo ($fileSize) diferente dos bytes copiados ($bytesCopied)")
        }
        
        Log.d(TAG, "Validando arquivo WAV...")
        if (!isValidWavFile(tempFile)) {
            Log.e(TAG, "Arquivo WAV inválido, deletando: ${tempFile.absolutePath}")
            tempFile.delete()
            return null
        }
        
        Log.i(TAG, "Arquivo WAV válido e pronto para conversão")
        tempFile
    } catch (e: Exception) {
        Log.e(TAG, "Erro ao copiar arquivo do URI: $uri", e)
        e.printStackTrace()
        null
    }
}

private fun isValidWavFile(file: File): Boolean {
    Log.d(TAG, "Validando arquivo WAV: ${file.absolutePath}")
    
    if (!file.exists()) {
        Log.e(TAG, "Arquivo não existe: ${file.absolutePath}")
        return false
    }
    
    val fileSize = file.length()
    Log.d(TAG, "Tamanho do arquivo: $fileSize bytes")
    
    if (fileSize < 44) {
        Log.e(TAG, "Arquivo muito pequeno: $fileSize bytes (mínimo: 44 bytes)")
        return false
    }
    
    return try {
        val inputStream = file.inputStream()
        val buffer = ByteArray(12)
        val bytesRead = inputStream.read(buffer)
        inputStream.close()
        
        if (bytesRead < 12) {
            Log.e(TAG, "Falha ao ler header WAV: apenas $bytesRead bytes lidos (esperado: 12)")
            return false
        }
        
        val chunkId = String(buffer, 0, 4)
        val format = String(buffer, 8, 4)
        
        Log.d(TAG, "Header WAV lido: chunkId='$chunkId', format='$format'")
        
        if (chunkId != "RIFF") {
            Log.e(TAG, "Chunk ID inválido: esperado 'RIFF', encontrado '$chunkId'")
            return false
        }
        
        if (format != "WAVE") {
            Log.e(TAG, "Formato inválido: esperado 'WAVE', encontrado '$format'")
            return false
        }
        
        Log.i(TAG, "Arquivo WAV válido: RIFF/WAVE detectado")
        true
    } catch (e: Exception) {
        Log.e(TAG, "Erro ao validar arquivo WAV: ${file.absolutePath}", e)
        false
    }
}
