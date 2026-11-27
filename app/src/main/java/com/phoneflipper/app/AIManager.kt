package com.phoneflipper.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Optional OpenAI-compatible chat client. API keys are supplied per request and never persisted.
 * Network calls must be executed off the UI thread by the caller.
 */
class AIManager {
    fun chat(prompt: String, apiKey: String): String {
        require(apiKey.isNotBlank()) { "کلید API وارد نشده است." }
        val body = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "system")
                put("content", HardwarePromptLibrary.buildSystemPrompt("fa"))
            }).put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            }))
            put("temperature", 0.3)
        }
        val connection = (URL("https://api.openai.com/v1/chat/completions").openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 45000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (code !in 200..299) {
                val message = try { JSONObject(response).optJSONObject("error")?.optString("message") } catch (_: Exception) { null }
                throw IllegalStateException(message?.takeIf { it.isNotBlank() } ?: "HTTP $code")
            }
            val choices = JSONObject(response).optJSONArray("choices")
                ?: throw IllegalStateException("پاسخ API فاقد choices است.")
            return choices.optJSONObject(0)?.optJSONObject("message")?.optString("content")
                ?.takeIf { it.isNotBlank() } ?: "پاسخ خالی از سرویس دریافت شد."
        } finally {
            connection.disconnect()
        }
    }
}
