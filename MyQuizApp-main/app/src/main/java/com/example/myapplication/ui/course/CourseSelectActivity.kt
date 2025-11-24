package com.example.myapplication.ui.course

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.ui.home.CourseItem

class CourseSelectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_select)

        val rv = findViewById<RecyclerView>(R.id.rvCourseSelect)
        rv.layoutManager = LinearLayoutManager(this)

        val items = listOf(
            CourseItem("정보처리기능사", 0),
            CourseItem("컴활 1급 필기", 0),
            CourseItem("파이썬", 0),
        )

        rv.adapter = CourseSelectAdapter(items) { selected ->
            // 🔥 [중요] 선택한 데이터를 담아서 RESULT_OK 신호를 보냅니다.
            val resultIntent = Intent()
            resultIntent.putExtra("SELECTED_NAME", selected.title)
            setResult(RESULT_OK, resultIntent)

            finish() // 액티비티 종료 (홈으로 돌아감)
        }
    }
}