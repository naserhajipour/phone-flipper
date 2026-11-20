package com.phoneflipper.app

import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val items: MutableList<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.MessageHolder>() {
    class MessageHolder(val text: TextView) : RecyclerView.ViewHolder(text)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHolder {
        val view = TextView(parent.context).apply {
            textSize = 16f
            setPadding(20, 12, 20, 12)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        return MessageHolder(view)
    }

    override fun onBindViewHolder(holder: MessageHolder, position: Int) {
        val item = items[position]
        holder.text.text = if (item.isUser) "شما: ${item.text}" else "دستیار: ${item.text}"
        holder.text.gravity = if (item.isUser) Gravity.END else Gravity.START
    }

    override fun getItemCount(): Int = items.size

    fun add(message: ChatMessage) {
        items.add(message)
        notifyItemInserted(items.lastIndex)
    }

    fun lastAiMessage(): String? = items.lastOrNull { !it.isUser }?.text
}
