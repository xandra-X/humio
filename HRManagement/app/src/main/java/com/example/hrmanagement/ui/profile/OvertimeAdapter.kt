package com.example.hrmanagement.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hrmanagement.R
import com.example.hrmanagement.data.profile.OvertimeItem

class OvertimeAdapter(
    private val items: List<OvertimeItem>
) : RecyclerView.Adapter<OvertimeAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvHours: TextView = view.findViewById(R.id.tvHours)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_overtime, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.tvDate.text = item.date
        holder.tvHours.text =
            "${item.hours} hours × ${"%.2f".format(item.amount / item.hours)}"
        holder.tvAmount.text =
            "${"%.2f".format(item.amount)}"
    }

    override fun getItemCount(): Int = items.size
}
