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

        // 🔥 [수정] CourseItem 생성자 변경 반영 (나머지 값은 0, 60으로 채움)
        val items = listOf(
            CourseItem("정보처리기능사", 0, 0, 60),
            CourseItem("컴활 1급 필기", 0, 0, 60),
            CourseItem("파이썬", 0, 0, 60),
        )

        rv.adapter = CourseSelectAdapter(items) { selected ->
            val resultIntent = Intent()
            resultIntent.putExtra("SELECTED_NAME", selected.title)
            setResult(RESULT_OK, resultIntent)

            finish()
        }
    }
}