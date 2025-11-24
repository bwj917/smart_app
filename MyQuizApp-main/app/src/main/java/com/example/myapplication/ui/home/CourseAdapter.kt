package com.example.myapplication.ui.home

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.google.android.material.button.MaterialButton

class CourseAdapter(
    var items: List<CourseItem>,
    private val onStartClick: (CourseItem) -> Unit,
    private val onCardClick: (CourseItem) -> Unit,
    private val onReviewClick: (CourseItem) -> Unit,
    private val onChangeClick: () -> Unit
) : RecyclerView.Adapter<CourseAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tvTitle)
        val tvSub: TextView = v.findViewById(R.id.tvSub)
        val tvPercent: TextView = v.findViewById(R.id.tvPercent)
        val btnStart: MaterialButton = v.findViewById(R.id.btnStart)
        val btnChangeCourse: MaterialButton = v.findViewById(R.id.btnChangeCourse)
        val circleProgress: com.google.android.material.progressindicator.CircularProgressIndicator = v.findViewById(R.id.circleProgress)
        val root: View = v
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.tvTitle.text = item.title

        holder.tvPercent.text = "${item.progressPercent}%"
        holder.circleProgress.setProgressCompat(item.progressPercent, true)

        // 🔥 [핵심 수정] 퍼센트 역계산 대신 실제 개수(solvedCount)를 표시
        holder.tvSub.text = "오늘: ${item.solvedCount} / ${item.goal} 개"

        holder.btnStart.setOnClickListener {
            Log.d("DEBUG_APP", "어댑터: 학습하기 버튼 클릭됨! (${item.title})")
            onStartClick(item)
        }

        holder.btnChangeCourse.setOnClickListener {
            onChangeClick()
        }

        holder.root.setOnClickListener { onCardClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<CourseItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}