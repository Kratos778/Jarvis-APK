package com.elizierdias.jarvis.ui

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.elizierdias.jarvis.R
import com.elizierdias.jarvis.databinding.ItemMessageBinding

class ChatAdapter(
    private val items: List<ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.VH>() {

    class VH(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val msg = items[position]
        holder.binding.bubble.text = msg.text
        val lp = holder.binding.bubble.layoutParams as FrameLayout.LayoutParams
        if (msg.fromUser) {
            lp.gravity = Gravity.END
            holder.binding.bubble.setBackgroundResource(R.drawable.bg_bubble_user)
        } else {
            lp.gravity = Gravity.START
            holder.binding.bubble.setBackgroundResource(R.drawable.bg_bubble_jarvis)
        }
        holder.binding.bubble.layoutParams = lp
    }
}
