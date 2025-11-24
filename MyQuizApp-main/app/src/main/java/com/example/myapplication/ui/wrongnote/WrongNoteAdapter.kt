package com.example.myapplication.ui.wrongnote

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.WrongProblem

class WrongNoteAdapter(
    private val items: List<WrongProblem>,
    private val onItemClick: (WrongProblem) -> Unit
) : RecyclerView.Adapter<WrongNoteAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvWrongTitle)
        val tvUserAnswer: TextView = view.findViewById(R.id.tvWrongUserAnswer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.wrong_note_item, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        holder.tvUserAnswer.text = "내가 쓴 답: ${item.userAnswer}"

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size
}
