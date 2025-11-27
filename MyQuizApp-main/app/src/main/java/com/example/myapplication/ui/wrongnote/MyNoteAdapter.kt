package com.example.myapplication.ui.wrongnote

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.model.Problem

class MyNoteAdapter(
    private val items: List<Problem>
) : RecyclerView.Adapter<MyNoteAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvWrongTitle)
        val tvSub: TextView = view.findViewById(R.id.tvWrongUserAnswer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.wrong_note_item, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.question

        Log.d("dfdf","fdd")

        // 🔥 [차별점] 노트에서는 정답만 깔끔하게 표시
        holder.tvSub.text = "정답: ${item.answer}"
        holder.tvSub.setTextColor(Color.parseColor("#333333"))
    }

    override fun getItemCount(): Int = items.size
}