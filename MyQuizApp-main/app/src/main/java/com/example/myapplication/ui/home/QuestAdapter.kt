package com.example.myapplication.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class QuestAdapter(
    private var items: List<QuestItem> // var로 변경 (데이터 갱신 가능하게)
) : RecyclerView.Adapter<QuestAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tvQuestTitle)
        val ivStatus: ImageView = v.findViewById(R.id.ivQuestStatus) // 🔥 아이디 연결
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quest, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        val statusText = " (${item.current}/${item.goal}${item.unit})"
        if(item.title == "일일 학습 30분"){
            holder.tvTitle.text = item.title + statusText + " : 200포인트"
        }else{
            holder.tvTitle.text = item.title + statusText + " : 100포인트"

        }

        // 🔥 [핵심] 완료 여부에 따라 전구 색상 변경
        if (item.isCompleted) {
            holder.tvTitle.setTextColor(Color.parseColor("#57419d")) // 텍스트: 보라색
            holder.ivStatus.setColorFilter(Color.parseColor("#FFD700")) // 전구: 골드(켜짐)
        } else {
            holder.tvTitle.setTextColor(Color.parseColor("#333333")) // 텍스트: 기본
            holder.ivStatus.setColorFilter(Color.parseColor("#BDBDBD")) // 전구: 회색(꺼짐)
        }
    }

    override fun getItemCount(): Int = items.size

    // 🔥 [추가] 데이터 갱신 함수
    fun updateItems(newItems: List<QuestItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}