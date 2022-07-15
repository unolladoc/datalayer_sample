package com.uno.datalayer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.uno.datalayer.Event
import com.uno.datalayer.databinding.SimpleListItem2Binding

class CustomAdapter : ListAdapter<Event, CustomViewHolder>(DIFF_CALLBACK){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val binding = SimpleListItem2Binding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CustomViewHolder(binding, parent.context)
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val event = getItem(position)
        if (event != null){
            holder.bindTo(event)
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Event>() {
            override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
                return newItem == oldItem
            }

        }
    }
}