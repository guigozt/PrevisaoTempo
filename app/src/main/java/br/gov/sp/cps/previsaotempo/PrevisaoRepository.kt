package br.gov.sp.cps.previsaotempo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class PrevisaoRepository {

    suspend fun buscarPrevisaoTempo(cidade: String): PrevisaoTempo = withContext(Dispatchers.IO) {
        val cidadeEncoded = URLEncoder.encode(cidade.trim(), "UTF-8")
        val urlGeo = URL("https://geocoding-api.open-meteo.com/v1/search?name=$cidadeEncoded&count=1&language=pt")
        val conexaoGeo = urlGeo.openConnection() as HttpURLConnection

        val jsonGeo = try {
            conexaoGeo.requestMethod = "GET"
            conexaoGeo.connectTimeout = 10000
            conexaoGeo.readTimeout = 10000

            val codigoResposta = conexaoGeo.responseCode
            val fluxo = if (codigoResposta in 200..299) {
                conexaoGeo.inputStream
            } else {
                conexaoGeo.errorStream
            }

            val resposta = fluxo?.bufferedReader()?.use { it.readText() }
                ?: throw IOException("A API de busca não retornou conteúdo.")

            if (codigoResposta !in 200..299) {
                throw IOException("A API de busca retornou HTTP $codigoResposta.")
            }

            JSONObject(resposta)
        } finally {
            conexaoGeo.disconnect()
        }

        val resultados = jsonGeo.optJSONArray("results")
        if (resultados == null || resultados.length() == 0) {
            throw IOException("Cidade não encontrada.")
        }

        val itemCidade = resultados.getJSONObject(0)
        val nomeCidade = itemCidade.getString("name")
        val pais = itemCidade.optString("country", "")
        val latitude = itemCidade.getDouble("latitude")
        val longitude = itemCidade.getDouble("longitude")

        val urlWeather = URL("https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current_weather=true")
        val conexaoWeather = urlWeather.openConnection() as HttpURLConnection

        val jsonWeather = try {
            conexaoWeather.requestMethod = "GET"
            conexaoWeather.connectTimeout = 10000
            conexaoWeather.readTimeout = 10000

            val codigoResposta = conexaoWeather.responseCode
            val fluxo = if (codigoResposta in 200..299) {
                conexaoWeather.inputStream
            } else {
                conexaoWeather.errorStream
            }

            val resposta = fluxo?.bufferedReader()?.use { it.readText() }
                ?: throw IOException("A API meteorológica não retornou conteúdo.")

            if (codigoResposta !in 200..299) {
                throw IOException("A API meteorológica retornou HTTP $codigoResposta.")
            }

            JSONObject(resposta)
        } finally {
            conexaoWeather.disconnect()
        }

        val currentWeather = jsonWeather.optJSONObject("current_weather")
            ?: throw IOException("Dados de temperatura não disponíveis.")

        val temperatura = currentWeather.getDouble("temperature")
        val cidadeExibicao = if (pais.isNotEmpty()) "$nomeCidade, $pais" else nomeCidade

        PrevisaoTempo(
            cidade = cidadeExibicao,
            temperatura = temperatura
        )
    }
}
