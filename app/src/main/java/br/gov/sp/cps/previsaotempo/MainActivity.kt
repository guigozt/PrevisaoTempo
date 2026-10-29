package br.gov.sp.cps.previsaotempo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var nomeCidadeTexto by remember { mutableStateOf("São Paulo") }
                    var previsao by remember { mutableStateOf<PrevisaoTempo?>(null) }
                    var carregando by remember { mutableStateOf(false) }
                    var mensagem by remember { mutableStateOf("Informe a cidade e toque em buscar.") }

                    val escopo = rememberCoroutineScope()
                    val repository = remember { PrevisaoRepository() }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Previsão do Tempo",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Consulte uma API e obtenha a temperatura atual da cidade."
                        )

                        OutlinedTextField(
                            value = nomeCidadeTexto,
                            onValueChange = { nomeCidadeTexto = it },
                            label = { Text("Nome da cidade") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                val cidade = nomeCidadeTexto.trim()

                                if (cidade.isEmpty()) {
                                    mensagem = "Informe o nome de uma cidade."
                                    previsao = null
                                    return@Button
                                }

                                escopo.launch {
                                    carregando = true
                                    mensagem = "Consultando a API..."
                                    previsao = null

                                    try {
                                        val previsaoAtual = repository.buscarPrevisaoTempo(cidade)
                                        previsao = previsaoAtual
                                        mensagem = "Previsão consultada com sucesso."
                                    } catch (e: Exception) {
                                        mensagem = "Não foi possível consultar a previsão: " +
                                                (e.message ?: "erro inesperado")
                                    } finally {
                                        carregando = false
                                    }
                                }
                            },
                            enabled = !carregando,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Buscar temperatura")
                        }

                        if (carregando) {
                            CircularProgressIndicator()
                        }

                        Text(text = mensagem)

                        previsao?.let {
                            Text(
                                text = "Cidade: ${it.cidade}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Temperatura atual: ${it.temperatura} °C",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "A temperatura é consultada via API REST meteorológica.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
