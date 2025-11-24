package com.example.myapplication.ui.wrongnote

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class WrongNoteDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wrong_note_detail)
        // 🔹 Toolbar 뒤로가기 버튼 활성화
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarWrongDetail)
        toolbar.setNavigationOnClickListener { finish() }

        val title = intent.getStringExtra("title") ?: ""
        val question = intent.getStringExtra("question") ?: ""
        val correct = intent.getStringExtra("correctAnswer") ?: ""

        findViewById<TextView>(R.id.tvDetailTitle).text = title
        findViewById<TextView>(R.id.tvDetailQuestion).text = question

        val etAnswer = findViewById<TextInputEditText>(R.id.etUserInput)
        val tvCorrect = findViewById<TextView>(R.id.tvCorrectAnswer)
        val btnCheck = findViewById<MaterialButton>(R.id.btnCheckAnswer)

        btnCheck.setOnClickListener {
            tvCorrect.text = "정답: $correct"
            tvCorrect.visibility = View.VISIBLE
        }
    }
}
