package com.phoneflipper.app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PromptActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(20,20,20,20)
        }

        val title = TextView(this).apply {
            text = "AI Hardware Assistant"
            textSize = 22f
        }

        val input = EditText(this).apply {
            hint = "Ask for commands, wiring, or hardware guidance..."
            setText(HardwarePromptLibrary.RF_EXAMPLE)
        }

        val output = TextView(this).apply {
            text = "Assistant output will appear here"
            setPadding(0, 16, 0, 0)
        }

        val button = Button(this).apply {
            text = "Generate hardware guidance"
        }

        root.addView(title)
        root.addView(input)
        root.addView(button)
        root.addView(output)
        setContentView(root)

        button.setOnClickListener {
            val prompt = input.text.toString().trim()
            if (prompt.isEmpty()) return@setOnClickListener

            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val prefs = EncryptedSharedPreferences.create(
                "ai_prefs",
                masterKeyAlias,
                this,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            val apiKey = prefs.getString("openai_api_key", "") ?: ""
            val assistant = HardwareAssistant(this)
            val systemPrompt = HardwarePromptLibrary.buildSystemPrompt("fa")
            val finalPrompt = "${systemPrompt}\n\n${CommandTemplates.aiPromptForHardware(prompt)}"

            CoroutineScope(Dispatchers.Main).launch {
                val manager = AIManager(this@PromptActivity)
                val result = manager.chat(finalPrompt, apiKey)
                output.text = result
            }
        }
    }
}
