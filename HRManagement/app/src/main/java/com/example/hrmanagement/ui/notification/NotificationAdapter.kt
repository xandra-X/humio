package com.example.hrmanagement.ui.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hrmanagement.R
import com.example.hrmanagement.data.NotificationDto

class NotificationAdapter :
    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    private val items = mutableListOf<NotificationDto>()

    fun submitList(list: List<NotificationDto>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvMessage: TextView =
            itemView.findViewById(R.id.tvMessage)

        private val tvTime: TextView =
            itemView.findViewById(R.id.tvTime)

        fun bind(item: NotificationDto) {
            tvMessage.text = item.message
            tvTime.text = item.createdAt
        }
    }
}
