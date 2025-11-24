package com.example.myapplication.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class QuestAdapter(
    private val items: List<QuestItem>
) : RecyclerView.Adapter<QuestAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tvQuestTitle) // xml ID 확인 필요
        // 퀘스트 아이템 레이아웃에 맞는 뷰들 바인딩...
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        // item_quest.xml 레이아웃이 필요합니다.
        // 만약 없으면 간단한 텍스트뷰만 있는 레이아웃을 만드셔야 합니다.
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quest, parent, false) 
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        // 기타 바인딩 로직
    }

    override fun getItemCount(): Int = items.size
}