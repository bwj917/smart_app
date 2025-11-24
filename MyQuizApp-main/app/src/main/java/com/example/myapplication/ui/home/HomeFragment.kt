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
                // 1. 코스 진행률 갱신 (기존 로직 유지)
                val newCourses = courses.toMutableList()
                for (i in newCourses.indices) {
                    val course = newCourses[i]
                    val response = RetrofitClient.problemApiService.getTodayStats(currentUserId, course.title)
                    if (response.isSuccessful) {
                        val body = response.body()
                        val count = (body?.get("solvedCount") as? Number)?.toInt() ?: 0

                        // 목표값 계산 등...
                        val goal = 60
                        val percent = if (goal > 0) (count.toDouble() / goal * 100).toInt() else 0
                        newCourses[i] = course.copy(progressPercent = percent.coerceIn(0, 100), solvedCount = count)
                    }
                }
                courses = newCourses
                courseAdapter.updateItems(courses.toList())


                // 2. 🔥 일일 퀘스트 & 포인트 갱신 로직
                val totalResponse = RetrofitClient.problemApiService.getTodayStats(currentUserId, "all")

                if (totalResponse.isSuccessful) {
                    val body = totalResponse.body()
                    val totalCount = (body?.get("solvedCount") as? Number)?.toInt() ?: 0
                    val totalTimeSec = (body?.get("studyTime") as? Number)?.toLong() ?: 0L
                    val totalTimeMin = (totalTimeSec / 60).toInt()

                    // (A) 서버에서 현재 포인트 가져와서 화면에 표시!
                    // StatsController에서 "currentPoints"를 보내준다고 가정
                    val serverPoints = (body?.get("currentPoints") as? Number)?.toInt() ?: 0
                    binding.tvUserPoints.text = "포인트 $serverPoints" // 👈 화면 갱신!

                    // 퀘스트 목표 설정
                    val goalTime = 30
                    val goalCount = 20

                    val isTimeDone = totalTimeMin >= goalTime
                    val isCountDone = totalCount >= goalCount

                    // (B) 달성 시 포인트 지급 요청 (중복 지급 방지 포함)
                    // 이미 받은 포인트는 로컬(SharedPref)에서 체크해서 서버 요청 안함
                    checkAndReward(currentUserId, "QUEST_TIME", isTimeDone, 100, "일일 학습 완료! 100P")
                    checkAndReward(currentUserId, "QUEST_COUNT", isCountDone, 100, "문제 풀이 완료! 100P")

                    // (C) 리스트 갱신
                    val newQuests = listOf(
                        QuestItem("일일 학습 30분", totalTimeMin, goalTime, "분", isTimeDone),
                        QuestItem("문제 20개 풀기", totalCount, goalCount, "개", isCountDone)
                    )
                    questAdapter.updateItems(newQuests)
                }

            } catch (e: Exception) {
                Log.e("DEBUG_HOME", "데이터 로드 실패", e)
            }
        }
    }

    private suspend fun checkAndReward(userId: Long, questKey: String, isDone: Boolean, amount: Int, msg: String) {
        if (!isDone) return // 달성 안 했으면 종료

        // ❌ [삭제] 이 코드가 에러 원인입니다 (API 26+ 필요)
        // val today = java.time.LocalDate.now().toString()

        // ✅ [수정] API 24에서도 잘 작동하는 방식으로 변경 (SimpleDateFormat)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val today = sdf.format(java.util.Date())

        // ----------------------------------------------------
        // 아래 코드는 그대로 유지
        // ----------------------------------------------------
        val prefKey = "${questKey}_$today"

        val prefs = requireContext().getSharedPreferences("QuestPrefs", android.content.Context.MODE_PRIVATE)
        val alreadyReceived = prefs.getBoolean(prefKey, false)

        if (!alreadyReceived) {
            // 서버에 포인트 지급 요청
            val response = RetrofitClient.problemApiService.rewardPoints(userId, amount)
            if (response.isSuccessful) {
                // 1. 내부 저장소에 '받았음' 기록
                prefs.edit().putBoolean(prefKey, true).apply()

                // 2. 토스트 메시지
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

                // 3. 화면의 포인트 숫자 즉시 업데이트 (숫자만 추출해서 더하기)
                val currentText = binding.tvUserPoints.text.toString().replace(Regex("[^0-9]"), "")
                val currentVal = currentText.toIntOrNull() ?: 0
                binding.tvUserPoints.text = "포인트 ${currentVal + amount}"
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