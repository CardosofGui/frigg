package com.br.frigg

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.br.frigg.LameConverter
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        var isConverting by remember { mutableStateOf(false) }
        var conversionResult by remember { mutableStateOf<String?>(null) }
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isConverting = true
                        conversionResult = null
                        
                        val wavPath = pickWavFile()
                        
                        if (wavPath != null) {
                            val result = LameConverter.convertWavToMp3(wavPath, bitrate = 128)
                            conversionResult = when (result) {
                                is com.br.frigg.ConversionResult.Success -> {
                                    "Conversão concluída com sucesso!\nArquivo salvo em: ${result.mp3Path}"
                                }
                                is com.br.frigg.ConversionResult.Error -> {
                                    "Erro ao converter o arquivo:\n${result.message}"
                                }
                            }
                        } else {
                            conversionResult = "Nenhum arquivo selecionado."
                        }
                        
                        isConverting = false
                    }
                },
                enabled = !isConverting
            ) {
                Text("Selecionar arquivo WAV")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isConverting) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Convertendo...")
            }
            
            conversionResult?.let { result ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = result,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}