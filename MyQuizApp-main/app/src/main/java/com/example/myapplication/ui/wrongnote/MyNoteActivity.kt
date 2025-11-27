package com.example.myapplication.ui.wrongnote

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.auth.AuthManager
import com.example.myapplication.data.remote.RetrofitClient
import com.example.myapplication.databinding.ActivityMyNoteBinding // 자동 생성됨
import kotlinx.coroutines.launch

class MyNoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyNoteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupFilter()
        binding.rvMyNoteList.layoutManager = LinearLayoutManager(this)
        loadData("전체")
    }

    private fun setupToolbar() {
        binding.toolbarMyNote.setNavigationOnClickListener { finish() }
    }

    private fun setupFilter() {
        val courses = listOf("전체", "정보처리기능사", "컴활 1급 필기", "파이썬")
        val adapter = ArrayAdapter(this, R.layout.item_filter_dropdown, courses)
        binding.spinnerCourse.setAdapter(adapter)
        binding.spinnerCourse.setText(courses[0], false)

        binding.spinnerCourse.setOnItemClickListener { parent, _, position, _ ->
            val selectedCourse = parent.getItemAtPosition(position).toString()
            loadData(selectedCourse)
        }
    }

    private fun loadData(courseName: String) {
        val userId = AuthManager.getUserId(this) ?: return

        lifecycleScope.launch {
            try {
                // 🔥 [핵심] 스크랩 API 호출
                val response = RetrofitClient.problemApiService.getScrappedProblems(userId, courseName)
                
                if (response.isSuccessful) {
                    val problems = response.body() ?: emptyList()
                    
                    if (problems.isEmpty()) {
                    }
                    // MyNoteAdapter 연결
                    binding.rvMyNoteList.adapter = MyNoteAdapter(problems)
                } else {
                    Toast.makeText(this@MyNoteActivity, "불러오기 실패", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}