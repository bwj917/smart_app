package com.example.myapplication.ui.wrongnote

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.StudyRepository
import com.google.android.material.appbar.MaterialToolbar

class WrongNoteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wrong_note)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarWrongNote)
        toolbar.setNavigationOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvWrongNotes)
        rv.layoutManager = LinearLayoutManager(this)

        val wrongList = StudyRepository.getWrongProblems(this)

        val adapter = WrongNoteAdapter(wrongList) { item ->
            // 클릭 → 상세 페이지로 이동
            val intent = Intent(this, WrongNoteDetailActivity::class.java)
            intent.putExtra("title", item.title)
            intent.putExtra("question", item.question)
            intent.putExtra("userAnswer", item.userAnswer)
            intent.putExtra("correctAnswer", item.correctAnswer)
            startActivity(intent)
        }
        rv.adapter = adapter
    }
}
