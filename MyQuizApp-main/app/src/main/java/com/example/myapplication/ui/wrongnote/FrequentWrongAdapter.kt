package com.example.myapplication.ui.wrongnote

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.model.Problem

class FrequentWrongAdapter(
    private val items: List<Problem>
) : RecyclerView.Adapter<FrequentWrongAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvWrongTitle)
        val tvSub: TextView = view.findViewById(R.id.tvWrongUserAnswer)
        // 🔥 [추가] 시도 횟수 표시용 (재활용하거나 새로 추가 가능, 여기선 tvWrongUserAnswer에 합쳐서 표시)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.wrong_note_item, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]

        // 🔥 [수정 1] 글자 수 자르는 로직(substring) 제거 -> 전체 텍스트 표시
        holder.tvTitle.text = item.question

        // 🔥 [수정 2] 정답과 함께 재도전 횟수 표시
        val attempts = item.totalAttempts ?: 0
        holder.tvSub.text = "정답: ${item.answer}  |  총 ${attempts}회 도전"
        holder.tvSub.setTextColor(Color.parseColor("#555555")) // 색상 조정
    }

    override fun getItemCount(): Int = items.size
}