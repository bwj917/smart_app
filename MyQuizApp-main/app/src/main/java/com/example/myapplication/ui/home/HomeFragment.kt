package com.example.myapplication.ui.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.auth.AuthManager
import com.example.myapplication.data.remote.RetrofitClient
import com.example.myapplication.databinding.FragmentHomeBinding
import com.example.myapplication.ui.course.CourseSelectActivity
import com.example.myapplication.ui.quiz.CourseIds
import com.example.myapplication.ui.quiz.QuizActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var courses = mutableListOf(
        CourseItem("정보처리기능사", 0, 0, 60)
    )

    private lateinit var courseAdapter: CourseAdapter

    // 🔥 [추가] 퀘스트 어댑터를 멤버 변수로 선언 (나중에 갱신하기 위해)
    private lateinit var questAdapter: QuestAdapter

    private val startCourseSelect = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedName = result.data?.getStringExtra("SELECTED_NAME")

            if (selectedName != null) {
                val oldItem = courses[0]
                courses[0] = oldItem.copy(title = selectedName)
                courseAdapter.updateItems(courses.toList())
                Toast.makeText(requireContext(), "$selectedName(으)로 변경!", Toast.LENGTH_SHORT).show()
                updateDailyProgress()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // 초기 퀘스트 목록 (0/0 상태)
        val initialQuests = listOf(
            QuestItem("일일 학습 30분", 0, 30, "분", false),
            QuestItem("문제 20개 풀기", 0, 20, "개", false)
        )

        binding.rvCourses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvQuests.layoutManager = LinearLayoutManager(requireContext())

        courseAdapter = CourseAdapter(
            items = courses,
            onStartClick = { item: CourseItem ->
                showQuizPreviewDialog()
            },
            onCardClick = { },
            onReviewClick = { },
            onChangeClick = {
                val intent = Intent(requireContext(), CourseSelectActivity::class.java)
                startCourseSelect.launch(intent)
            }
        )

        // 🔥 [수정] 퀘스트 어댑터 초기화 및 연결
        questAdapter = QuestAdapter(initialQuests)
        binding.rvCourses.adapter = courseAdapter
        binding.rvQuests.adapter = questAdapter

        updateDailyProgress()
    }

    private fun showQuizPreviewDialog() {
        val currentUserId = AuthManager.getUserId(requireContext())

        if (currentUserId == null) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {
            try {
                val currentCourseTitle = courses[0].title
                val response = RetrofitClient.problemApiService.getTenProblems(currentUserId, currentCourseTitle)

                if (response.isSuccessful) {
                    val problemList = response.body() ?: emptyList()

                    if (problemList.isEmpty()) {
                        Toast.makeText(requireContext(), "풀 수 있는 문제가 없습니다.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val now = System.currentTimeMillis()
                    var newCount = 0
                    var retryCount = 0
                    var reviewCount = 0

                    for (p in problemList) {
                        val reviewTimeMillis = p.nextReviewTime?.time ?: 0L
                        if (p.nextReviewTime == null) {
                            newCount++
                        } else {
                            if (reviewTimeMillis > now) {
                                retryCount++
                            } else {
                                reviewCount++
                            }
                        }
                    }

                    val sb = StringBuilder()
                    sb.append("총 ${problemList.size}문제를 학습합니다.\n\n")
                    if (reviewCount > 0) sb.append("🔴 복습 : ${reviewCount}문제\n")
                    if (retryCount > 0) sb.append("🟡 재도전 : ${retryCount}문제\n")
                    if (newCount > 0) sb.append("🔵 새 문제: ${newCount}문제")

                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("오늘의 학습 구성")
                        .setMessage(sb.toString())
                        .setNegativeButton("나중에") { d, _ -> d.dismiss() }
                        .setPositiveButton("학습 시작") { d, _ ->
                            d.dismiss()
                            val intent = Intent(requireContext(), QuizActivity::class.java)
                            intent.putExtra(CourseIds.EXTRA_COURSE_ID, courses[0].title)
                            intent.putExtra(CourseIds.EXTRA_USER_ID, currentUserId)
                            intent.putExtra("RESET_PROGRESS", true)
                            startActivity(intent)
                        }
                        .show()

                } else {
                    Toast.makeText(requireContext(), "정보 로드 실패", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "서버 연결 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateDailyProgress() {
        val currentUserId = AuthManager.getUserId(requireContext()) ?: return

        lifecycleScope.launch {
            try {
                // 1. 코스 진행률 갱신 (기존 로직)
                val newCourses = courses.toMutableList()
                for (i in newCourses.indices) {
                    val course = newCourses[i]
                    val response = RetrofitClient.problemApiService.getTodayStats(currentUserId, course.title)

                    if (response.isSuccessful) {
                        val body = response.body()
                        val count = (body?.get("solvedCount") as? Number)?.toInt() ?: 0

                        val goal = 60
                        val percent = if (goal > 0) (count.toDouble() / goal * 100).toInt() else 0
                        val safePercent = percent.coerceIn(0, 100)

                        newCourses[i] = course.copy(
                            progressPercent = safePercent,
                            solvedCount = count,
                            goal = goal
                        )
                    }
                }
                courses = newCourses
                courseAdapter.updateItems(courses.toList())

                // 2. 🔥 [추가] 일일 퀘스트(전체 학습량) 갱신 로직
                // 과목 상관없이 "오늘 전체" 데이터를 가져옵니다.
                val totalResponse = RetrofitClient.problemApiService.getTodayStats(currentUserId, "all")
                if (totalResponse.isSuccessful) {
                    val body = totalResponse.body()
                    val totalCount = (body?.get("solvedCount") as? Number)?.toInt() ?: 0
                    val totalTimeSec = (body?.get("studyTime") as? Number)?.toLong() ?: 0L
                    val totalTimeMin = (totalTimeSec / 60).toInt()

                    // 퀘스트 목록 새로 생성
                    val newQuests = listOf(
                        QuestItem(
                            title = "일일 학습 30분",
                            current = totalTimeMin,
                            goal = 30,
                            unit = "분",
                            isCompleted = totalTimeMin >= 30 // 30분 이상이면 달성!
                        ),
                        QuestItem(
                            title = "문제 20개 풀기",
                            current = totalCount,
                            goal = 20,
                            unit = "개",
                            isCompleted = totalCount >= 20 // 20개 이상이면 달성!
                        )
                    )
                    // 어댑터에 갱신 알림
                    questAdapter.updateItems(newQuests)
                }

            } catch (e: Exception) {
                Log.e("DEBUG_HOME", "에러 발생", e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let { updateDailyProgress() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}