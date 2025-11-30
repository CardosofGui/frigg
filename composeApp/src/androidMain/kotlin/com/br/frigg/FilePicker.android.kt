package com.br.frigg

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

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
        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/wav", "audio/x-wav", "audio/*"))
    }
    
    resultCallback = { uri ->
        if (uri != null) {
            val file = copyUriToTempFile(currentActivity, Uri.parse(uri))
            continuation.resume(file?.absolutePath)
        } else {
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
        launcher.launch(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        continuation.resume(null)
    }
}

private fun copyUriToTempFile(activity: Activity, uri: Uri): File? {
    return try {
        val inputStream = activity.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File(activity.cacheDir, "temp_audio_${System.currentTimeMillis()}.wav")
        
        FileOutputStream(tempFile).use { output ->
            inputStream.copyTo(output)
        }
        inputStream.close()
        
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
