package com.example.myapplication.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class QuestAdapter(private var items: List<QuestItem>) : RecyclerView.Adapter<QuestAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tvQuestTitle)
        val tvStatus: TextView = v.findViewById(R.id.tvQuestStatus)
        val ivIcon: ImageView = v.findViewById(R.id.ivQuestIcon) // 전구 아이콘
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quest, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.tvTitle.text = item.title

        if (item.isAchieved) {
            // 🔥 [달성 시]
            holder.tvStatus.text = "달성 완료!"
            holder.tvStatus.setTextColor(Color.parseColor("#57419D")) // 보라색 텍스트

            // 전구에 불 켜기 (노란색)
            holder.ivIcon.setColorFilter(Color.parseColor("#FFD700"))
        } else {
            // [미달성 시]
            holder.tvStatus.text = "${item.current} / ${item.goal} ${item.unit}"
            holder.tvStatus.setTextColor(Color.parseColor("#888888")) // 회색 텍스트

            // 전구 끄기 (연한 회색)
            holder.ivIcon.setColorFilter(Color.parseColor("#E0E0E0"))
        }
    }

    override fun getItemCount(): Int = items.size

    // 데이터 갱신 함수
    fun updateItems(newItems: List<QuestItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}