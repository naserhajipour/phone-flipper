package com.phoneflipper.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AIManager(private val context: Context) {
    suspend fun chat(prompt: String, apiKey: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext "برای پاسخ هوش مصنوعی، ابتدا کلید API را در تنظیمات برنامه وارد کنید. کلید را در کد یا مخزن عمومی قرار ندهید."
        var connection: HttpURLConnection? = null
        try {
            connection = (URL("https://api.openai.com/v1/chat/completions").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 30000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
            }
            val body = JSONObject()
                .put("model", "gpt-4o-mini")
                .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
                .put("temperature", 0.3)
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) return@withContext "خطای API ($code): " + runCatching { JSONObject(response).optJSONObject("error")?.optString("message") }.getOrNull().orEmpty()
            JSONObject(response).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
                ?.takeIf { it.isNotBlank() } ?: "پاسخ خالی از سرویس دریافت شد."
        } catch (e: Exception) {
            "ارتباط با سرویس هوش مصنوعی ناموفق بود: ${e.message ?: "خطای شبکه"}"
        } finally {
            connection?.disconnect()
        }
    }
}
