package com.phoneflipper.app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class ChatActivity : AppCompatActivity() {
    private lateinit var adapter: ChatAdapter
    private lateinit var aiManager: AIManager
    private val btService = BluetoothService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        aiManager = AIManager(this)

        val chatRecycler = findViewById<RecyclerView>(R.id.chatRecycler)
        val inputText = findViewById<EditText>(R.id.inputText)
        val sendBtn = findViewById<Button>(R.id.sendBtn)

        // Extra UI for hardware sending
        val btAddrInput = EditText(this).apply { hint = "BT device address (e.g. 00:11:22:33:44:55)" }
        val connectBtn = Button(this).apply { text = "Connect BT" }
        val sendToHwBtn = Button(this).apply { text = "Send last AI to hardware" }

        // Attach to layout programmatically (keeps layout XML unchanged)
        val root = findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0) as android.view.ViewGroup
        (root as android.widget.LinearLayout).addView(btAddrInput)
        root.addView(connectBtn)
        root.addView(sendToHwBtn)

        adapter = ChatAdapter(mutableListOf())
        chatRecycler.adapter = adapter
        chatRecycler.layoutManager = LinearLayoutManager(this)

        // Load saved BT address
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPrefs = EncryptedSharedPreferences.create(
            "hw_prefs",
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        btAddrInput.setText(sharedPrefs.getString("bt_address", ""))

        connectBtn.setOnClickListener {
            val addr = btAddrInput.text.toString().trim()
            if (addr.isEmpty()) {
                Toast.makeText(this, "Enter Bluetooth address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Save address
            sharedPrefs.edit().putString("bt_address", addr).apply()

            CoroutineScope(Dispatchers.Main).launch {
                val ok = btService.connect(addr)
                if (ok) Toast.makeText(this@ChatActivity, "Connected to $addr", Toast.LENGTH_SHORT).show()
                else Toast.makeText(this@ChatActivity, "Failed to connect", Toast.LENGTH_SHORT).show()
            }
        }

        sendToHwBtn.setOnClickListener {
            // get last AI message
            val last = adapterItemsLastAI()
            if (last == null) {
                Toast.makeText(this, "No AI reply to send", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.Main).launch {
                val success = btService.send(last)
                if (success) Toast.makeText(this@ChatActivity, "Sent to hardware", Toast.LENGTH_SHORT).show()
                else Toast.makeText(this@ChatActivity, "Send failed", Toast.LENGTH_SHORT).show()
            }
        }

        sendBtn.setOnClickListener {
            val text = inputText.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            adapter.add(ChatMessage(text, true))
            inputText.text.clear()

            // read API key from secure prefs
            val masterKeyAlias2 = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPrefs2 = EncryptedSharedPreferences.create(
                "ai_prefs",
                masterKeyAlias2,
                this,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            val apiKey = sharedPrefs2.getString("openai_api_key", "") ?: ""

            CoroutineScope(Dispatchers.Main).launch {
                val reply = aiManager.chat(text, apiKey)
                adapter.add(ChatMessage(reply, false))
                chatRecycler.scrollToPosition(adapter.itemCount - 1)
            }
        }
    }

    private fun adapterItemsLastAI(): String? {
        // reflectively access adapter items (ChatAdapter uses private list)
        // For simplicity, we extend ChatAdapter to expose last AI or track externally.
        // Here we use a workaround by accessing the adapter via reflection (best-effort)
        try {
            val field = adapter.javaClass.getDeclaredField("items")
            field.isAccessible = true
            val items = field.get(adapter) as MutableList<ChatMessage>
            for (i in items.size - 1 downTo 0) {
                val it = items[i]
                if (!it.fromUser) return it.text
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        btService.disconnect()
    }
}
