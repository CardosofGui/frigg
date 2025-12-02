package com.br.frigg

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.br.frigg.logging.FriggLogging
import kotlinx.coroutines.launch

private val logger = FriggLogging.logger("App")

@Composable
fun App(
    converter: FriggConverter = FriggConverter()
) {
    LaunchedEffect(Unit) {
        logger.debug { "Teste" }
    }

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
                            val result = converter.convertWavToMp3(wavPath, bitrate = 128)
                            logger.debug { "Log" }

                            conversionResult = when (result) {
                                is ConversionResult.Success -> {
                                    "Conversão concluída com sucesso!\nArquivo salvo em: ${result.mp3Path}"
                                }
                                is ConversionResult.Error -> {
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