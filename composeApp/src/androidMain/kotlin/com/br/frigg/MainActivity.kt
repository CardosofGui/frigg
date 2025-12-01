package com.br.frigg

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            initializeFilePicker(this)
            com.br.frigg.LameConverter.initialize(this)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        if (hasStoragePermissions()) {
            initializeFilePicker(this)
            com.br.frigg.LameConverter.initialize(this)
        } else {
            requestStoragePermissions()
        }

        setContent {
            App()
        }
    }
    
    private fun hasStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestStoragePermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        requestPermissionLauncher.launch(permissions)
    }
    
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}