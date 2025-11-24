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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var courses = mutableListOf(
        CourseItem("정보처리기능사", 0)
    )

    // 초기 퀘스트 데이터 (0/0 상태)
    private var quests = mutableListOf(
        QuestItem("일일 학습 30분", 0, 30, "분"),
        QuestItem("문제 20개 풀기", 0, 20, "개")
    )

    private lateinit var courseAdapter: CourseAdapter
    private lateinit var questAdapter: QuestAdapter // 🔥 퀘스트 어댑터 변수 추가

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
        super.onViewCreated(view, savedInstanceState)

        binding.rvCourses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvQuests.layoutManager = LinearLayoutManager(requireContext())

        // 코스 어댑터 설정
        courseAdapter = CourseAdapter(
            items = courses,
            onStartClick = { item -> showQuizPreviewDialog() },
            onCardClick = { },
            onReviewClick = { },
            onChangeClick = {
                val intent = Intent(requireContext(), CourseSelectActivity::class.java)
                startCourseSelect.launch(intent)
            }
        )

        // 퀘스트 어댑터 설정
        questAdapter = QuestAdapter(quests)

        binding.rvCourses.adapter = courseAdapter
        binding.rvQuests.adapter = questAdapter
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            updateDailyProgress()
        }
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
                delay(500)

                // 1. 코스 진행률 갱신 (기존 로직)
                val newCourses = courses.toMutableList()
                for (i in newCourses.indices) {
                    val course = newCourses[i]
                    val response = RetrofitClient.problemApiService.getTodaySolvedCount(currentUserId, course.title)

                    if (response.isSuccessful) {
                        val count = response.body()?.get("count") ?: 0
                        val goal = 60
                        val percent = if (goal > 0) (count.toDouble() / goal * 100).toInt() else 0
                        val safePercent = percent.coerceIn(0, 100)

                        newCourses[i] = course.copy(progressPercent = safePercent, solvedCount = count)
                    }
                }
                courses = newCourses

                // 2. 🔥 [추가] 일일 퀘스트(전구) 데이터 갱신
                // 서버에서 '오늘 전체 통계'를 가져옵니다.
                val statsResponse = RetrofitClient.problemApiService.getTodayTotalStats(currentUserId)

                if (statsResponse.isSuccessful) {
                    val body = statsResponse.body()
                    // 문제 수
                    val totalCount = (body?.get("count") as? Number)?.toInt() ?: 0
                    // 공부 시간 (초 단위) -> 분 단위로 변환
                    val totalTimeSec = (body?.get("studyTime") as? Number)?.toLong() ?: 0L
                    val totalTimeMin = (totalTimeSec / 60).toInt()

                    // 퀘스트 리스트 업데이트
                    val newQuests = mutableListOf(
                        QuestItem("일일 학습 30분", totalTimeMin, 30, "분"),
                        QuestItem("문제 20개 풀기", totalCount, 20, "개")
                    )
                    quests = newQuests
                }

                // 3. UI 반영
                view?.post {
                    if (_binding != null) {
                        courseAdapter.updateItems(courses.toList())
                        questAdapter.updateItems(quests.toList()) // 퀘스트 어댑터 갱신
                        Log.d("DEBUG_HOME", "UI 강제 업데이트 실행됨")
                    }
                }

            } catch (e: Exception) {
                Log.e("DEBUG_HOME", "에러 발생", e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let {
            updateDailyProgress()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}