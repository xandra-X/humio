package com.example.hrmanagement.ui.leave

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hrmanagement.R
import com.example.hrmanagement.data.LeaveHistoryItem

class LeaveHistoryAdapter(
    private var items: List<LeaveHistoryItem> = emptyList()
) : RecyclerView.Adapter<LeaveHistoryAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivActivityIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvHistoryTitle)
        val tvSubtitle: TextView = view.findViewById(R.id.tvHistorySubtitle)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leave_history, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.tvTitle.text = item.type
        holder.tvSubtitle.text = "${item.daysText} - ${item.note}"
        holder.tvStatus.text = item.status

        Glide.with(holder.itemView.context)
            .load(R.drawable.cal)
            .into(holder.ivIcon)

        val bgRes =
            if (item.status.equals("REJECTED", true))
                R.drawable.bg_status_rejected
            else
                R.drawable.bg_status_approved

        holder.tvStatus.setBackgroundResource(bgRes)
    }

    override fun getItemCount(): Int = items.size

    fun submitList(list: List<LeaveHistoryItem>) {
        items = list
        notifyDataSetChanged()
    }
}
