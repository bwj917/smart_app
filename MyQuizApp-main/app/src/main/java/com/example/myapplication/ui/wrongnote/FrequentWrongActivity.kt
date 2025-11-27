package com.example.myapplication.ui.wrongnote

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.auth.AuthManager
import com.example.myapplication.data.remote.RetrofitClient
import com.example.myapplication.databinding.ActivityFrequentWrongBinding // 🔥 바인딩 클래스 변경 주의
import kotlinx.coroutines.launch

class FrequentWrongActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFrequentWrongBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 새로 만든 레이아웃으로 바인딩
        binding = ActivityFrequentWrongBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupFilter() // 드롭다운 설정

        binding.rvFrequentList.layoutManager = LinearLayoutManager(this)

        // 처음에 '전체' 데이터 로드
        loadData("전체")
    }

    private fun setupToolbar() {
        binding.toolbarFrequent.setNavigationOnClickListener { finish() }
    }

    private fun setupFilter() {
        // "전체" 옵션 포함
        val courses = listOf("전체", "정보처리기능사", "컴활 1급 필기", "파이썬")
        val adapter = ArrayAdapter(this, R.layout.item_filter_dropdown, courses)

        binding.spinnerCourse.setAdapter(adapter)
        binding.spinnerCourse.setText(courses[0], false) // 기본값 '전체'

        // 항목 선택 시 이벤트
        binding.spinnerCourse.setOnItemClickListener { parent, _, position, _ ->
            val selectedCourse = parent.getItemAtPosition(position).toString()
            loadData(selectedCourse) // 🔥 선택된 과목으로 재조회
        }
    }

    private fun loadData(courseName: String) {
        val userId = AuthManager.getUserId(this) ?: return

        lifecycleScope.launch {
            try {
                // API 호출 (과목명 함께 전달)
                val response = RetrofitClient.problemApiService.getFrequentWrongProblems(userId, courseName)

                if (response.isSuccessful) {
                    val problems = response.body() ?: emptyList()

                    if (problems.isEmpty()) {
                    }
                    // 어댑터 연결
                    binding.rvFrequentList.adapter = FrequentWrongAdapter(problems)
                } else {
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}