package tn.dyndns.android

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import tn.dyndns.android.databinding.ItemEntryBinding
import tn.dyndns.android.models.DyndnsEntry
import java.text.SimpleDateFormat
import java.util.*

class EntryAdapter(
    private val onItemClick: (DyndnsEntry) -> Unit,
    private val onItemLongClick: (DyndnsEntry) -> Unit,
    private val onToggleEnabled: (DyndnsEntry) -> Unit
) : ListAdapter<DyndnsEntry, EntryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                onItemClick(getItem(adapterPosition))
            }
            binding.root.setOnLongClickListener {
                onItemLongClick(getItem(adapterPosition))
                true
            }

            binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                val entry = getItem(adapterPosition)
                if (entry.enabled != isChecked) {
                    onToggleEnabled(entry)
                }
                updateSwitchColors(isChecked)
            }
        }

        private fun updateSwitchColors(isChecked: Boolean) {
            val context = binding.root.context
            if (isChecked) {
                // Active state - Green
                binding.switchEnabled.setThumbTintList(ContextCompat.getColorStateList(context, R.color.switch_thumb_active))
                binding.switchEnabled.setTrackTintList(ContextCompat.getColorStateList(context, R.color.switch_track_active))
            } else {
                // Inactive state - Red
                binding.switchEnabled.setThumbTintList(ContextCompat.getColorStateList(context, R.color.switch_thumb_inactive))
                binding.switchEnabled.setTrackTintList(ContextCompat.getColorStateList(context, R.color.switch_track_inactive))
            }
        }

        fun bind(entry: DyndnsEntry) {
            binding.textName.text = entry.name
            binding.textHostname.text = "${entry.fqdn ?: entry.hostname} (${entry.resolvedIp})"
            binding.textProvider.text = entry.providerType.name
            binding.textStatus.text = "${entry.lastStatus}"
            binding.textLastUpdate.text = if (entry.lastUpdateTime > 0) {
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date(entry.lastUpdateTime))
                "Last: $date"
            } else {
                "Never updated"
            }

            binding.switchEnabled.isChecked = entry.enabled
            updateSwitchColors(entry.enabled)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DyndnsEntry>() {
        override fun areItemsTheSame(old: DyndnsEntry, new: DyndnsEntry) = old.id == new.id
        override fun areContentsTheSame(old: DyndnsEntry, new: DyndnsEntry) = old == new
    }
}