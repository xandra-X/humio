package com.example.hrmanagement.ui.attendance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hrmanagement.R
import com.example.hrmanagement.data.AttendanceHistoryItem

class AttendanceRecordAdapter(
    private var items: List<AttendanceHistoryItem> = emptyList()
) : RecyclerView.Adapter<AttendanceRecordAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvRowDate)
        val tvCheckIn: TextView = view.findViewById(R.id.tvRowCheckIn)
        val tvCheckOut: TextView = view.findViewById(R.id.tvRowCheckOut)
        val tvStatus: TextView = view.findViewById(R.id.tvRowStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_record, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val it = items[position]
        holder.tvDate.text = it.date
        holder.tvCheckIn.text = "Check-in: ${it.checkIn ?: "--:--"}"
        holder.tvCheckOut.text = "Check-out: ${it.checkOut ?: "--:--"}"
        holder.tvStatus.text = it.status ?: "UNKNOWN"
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newList: List<AttendanceHistoryItem>) {
        items = newList
        notifyDataSetChanged()
    }
}
