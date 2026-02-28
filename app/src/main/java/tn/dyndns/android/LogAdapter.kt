package tn.dyndns.android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import tn.dyndns.android.models.LogEntry

class LogAdapter : ListAdapter<LogEntry, LogAdapter.ViewHolder>(DiffCallback()) {

    private var currentTextSize = 12f // Default text size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1)

        init {
            textView.setTextIsSelectable(true)
            textView.setPadding(16, 8, 16, 8)
        }

        fun bind(log: LogEntry, textSize: Float) {
            textView.text = "[${log.getFullTime()}] ${log.message}"
            textView.textSize = textSize

            // Only color errors in red, everything else uses theme default
            if (log.level == "ERROR") {
                textView.setTextColor(ContextCompat.getColor(textView.context, android.R.color.holo_red_dark))
            }
        }

        fun setTextSize(size: Float) {
            textView.textSize = size
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), currentTextSize)
    }

    fun setTextSize(size: Float) {
        currentTextSize = size
        notifyDataSetChanged() // Refresh all items with new text size
    }

    class DiffCallback : DiffUtil.ItemCallback<LogEntry>() {
        override fun areItemsTheSame(oldItem: LogEntry, newItem: LogEntry) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: LogEntry, newItem: LogEntry) = oldItem == newItem
    }
}