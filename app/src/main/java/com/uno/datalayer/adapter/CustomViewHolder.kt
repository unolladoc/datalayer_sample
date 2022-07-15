package com.uno.datalayer.adapter

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.uno.datalayer.Event
import com.uno.datalayer.databinding.SimpleListItem2Binding

class CustomViewHolder(
    private var binding: SimpleListItem2Binding,
    private val context: Context
) : RecyclerView.ViewHolder(binding.root) {
    private lateinit var event: Event

    fun bindTo(event: Event){
        this.event = event
        binding.itemTitle.text = context.getString(event.title)
        binding.itemText.text = event.text
    }

    fun getEvent(): Event = event
}