package com.example.myapplication.ui.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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

    // 1. 멤버 변수 선언 (화면 갱신용)
    private var courses = mutableListOf(
        CourseItem("정보처리기능사", 0))

    private lateinit var courseAdapter: CourseAdapter

    // 2. 과목 선택 화면에서 돌아왔을 때 실행되는 콜백
    private val startCourseSelect = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedName = result.data?.getStringExtra("SELECTED_NAME")

            if (selectedName != null) {
                // 데이터 갱신
                val oldItem = courses[0]
                courses[0] = oldItem.copy(title = selectedName)

                // 🔥 [핵심 수정] 리스트를 새로 복사해서(.toList()) 넣어야 어댑터가 변경을 확실히 감지합니다.
                courseAdapter.updateItems(courses.toList())

                Toast.makeText(requireContext(), "$selectedName(으)로 변경!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // ⚠️ 여기에 val courses = ... 코드가 있으면 절대 안 됩니다. (삭제됨 확인)

        val quests = mutableListOf(
            QuestItem("일일 학습 30분", 30, false),
            QuestItem("문제 20개 풀기", 20, false)
        )

        binding.rvCourses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvQuests.layoutManager = LinearLayoutManager(requireContext())

        // 3. 어댑터 초기화
        courseAdapter = CourseAdapter(
            items = courses, // 멤버 변수 사용
            onStartClick = { item: CourseItem ->
                // 학습하기 버튼
                showQuizPreviewDialog()
            },
            onCardClick = { },
            onReviewClick = { },
            onChangeClick = {
                // 과목 변경하기 버튼
                val intent = Intent(requireContext(), CourseSelectActivity::class.java)
                startCourseSelect.launch(intent)
            }
        )

        // 🔥 [필수] 어댑터 연결
        binding.rvCourses.adapter = courseAdapter
        binding.rvQuests.adapter = QuestAdapter(quests)

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

                    // 문제 통계 계산
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

                            // 🔥 변경된 과목명으로 퀴즈 시작
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
                // courses 리스트를 복사해서 수정할 준비 (동시성 문제 방지)
                val newCourses = courses.toMutableList()

                // 🔥 [수정 2] 모든 과목을 하나씩 돌면서 서버에 물어봄
                for (i in newCourses.indices) {
                    val course = newCourses[i]

                    // "정보처리기능사 푼 개수 줘", "정보보안기사 푼 개수 줘" ...
                    val response = RetrofitClient.problemApiService.getTodaySolvedCount(currentUserId, course.title)

                    if (response.isSuccessful) {
                        val count = response.body()?.get("count") ?: 0
                        val goal = 60

                        // 퍼센트 계산
                        val percent = if (goal > 0) (count.toDouble() / goal * 100).toInt() else 0
                        val safePercent = percent.coerceIn(0, 100)

                        // 리스트 데이터 업데이트
                        newCourses[i] = course.copy(progressPercent = safePercent)
                    }
                }

                // 🔥 [수정 3] 다 고친 리스트를 원본에 덮어쓰고 어댑터에 알림
                courses = newCourses
                courseAdapter.updateItems(courses.toList())

            } catch (e: Exception) {
                Log.e("DEBUG_HOME", "에러 발생", e)
            }
        }
    }


    // 🔥 [추가] 화면이 보일 때마다 서버에서 데이터를 가져와 갱신합니다.
    override fun onResume() {
        super.onResume()

        // 뷰가 유효할 때만 학습량 갱신 함수 호출
        view?.let {
            updateDailyProgress()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}