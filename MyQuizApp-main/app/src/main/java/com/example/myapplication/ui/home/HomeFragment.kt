package com.example.myapplication.ui.home

import android.app.Activity
import android.content.Context
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
import com.example.myapplication.R
import com.example.myapplication.auth.AuthManager
import com.example.myapplication.data.remote.RetrofitClient
import com.example.myapplication.databinding.FragmentHomeBinding
import com.example.myapplication.ui.course.CourseSelectActivity
import com.example.myapplication.ui.quiz.CourseIds
import com.example.myapplication.ui.quiz.QuizActivity
import com.example.myapplication.util.CharacterManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // 🔥 [추가 1] 캐릭터 이미지 리스트 (ShopDialog와 순서가 같아야 함)
    private val characterList = listOf(
        R.drawable.quit,
        R.drawable.quit_rabbit,
        R.drawable.quit_panda
    )
    private var currentCharacterIndex = 0

    private var courses = mutableListOf(
        CourseItem("정보처리기능사", 0, 0, 60)
    )

    private lateinit var courseAdapter: CourseAdapter
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
        super.onViewCreated(view, savedInstanceState)

        // 🔥 [추가 2] 상점 및 캐릭터 로직 초기화
        setupCharacterLogic()

        // 초기 퀘스트 목록
        val initialQuests = listOf(
            QuestItem("일일 학습 30분", 0, 30, "분", false),
            QuestItem("문제 20개 풀기", 0, 20, "개", false)
        )

        binding.rvCourses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvQuests.layoutManager = LinearLayoutManager(requireContext())

        courseAdapter = CourseAdapter(
            items = courses,
            onStartClick = { item: CourseItem -> showQuizPreviewDialog() },
            onCardClick = { },
            onReviewClick = { },
            onChangeClick = {
                val intent = Intent(requireContext(), CourseSelectActivity::class.java)
                startCourseSelect.launch(intent)
            }
        )

        questAdapter = QuestAdapter(initialQuests)
        binding.rvCourses.adapter = courseAdapter
        binding.rvQuests.adapter = questAdapter

        updateDailyProgress()
    }

    // 🔥 [추가 3] 캐릭터 클릭 시 상점 열기 로직
    private fun setupCharacterLogic() {
        val userId = AuthManager.getUserId(requireContext())

        // 1. [초기화] 일단 기본값(0)으로 이미지 설정
        var serverEquippedIdx = 0
        if (characterList.isNotEmpty()) {
            binding.imageView.setImageResource(characterList[0])
        }

        if (userId != null) {
            // 2. [자동 동기화] 화면 켜지자마자 서버에서 "내 장착 정보" 가져오기
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.problemApiService.getTodayStats(userId, "all")
                    if (response.isSuccessful) {
                        val body = response.body()
                        // 🔥 서버가 알려준 장착 번호 가져오기
                        serverEquippedIdx = (body?.get("equippedCharacterIdx") as? Number)?.toInt() ?: 0
                        currentCharacterIndex = serverEquippedIdx // 전역 변수 업데이트

                        // 이미지 즉시 변경
                        if (currentCharacterIndex in characterList.indices) {
                            binding.imageView.setImageResource(characterList[currentCharacterIndex])
                        }

                        // 로컬에도 저장 (퀴즈 화면 등에서 쓰기 위해)
                        requireContext().getSharedPreferences("UserSettings", Context.MODE_PRIVATE)
                            .edit().putInt("SELECTED_CHARACTER_IDX", currentCharacterIndex).apply()
                    }
                } catch (e: Exception) {
                    Log.e("Home", "장착 정보 로드 실패", e)
                }
            }

            // 3. [클릭 이벤트] 캐릭터 클릭 -> 최신 정보 조회 -> 상점 오픈
            binding.imageView.setOnClickListener {
                // 현재 화면의 포인트 (백업용)
                val currentPointStr = binding.tvUserPoints.text.toString().replace(Regex("[^0-9]"), "")
                val currentPoints = currentPointStr.toIntOrNull() ?: 0

                lifecycleScope.launch {
                    try {
                        // 🔥 상점 열기 전, 최신 데이터(포인트, 소유목록) 서버에서 다시 가져오기
                        val response = RetrofitClient.problemApiService.getTodayStats(userId, "all")

                        if (response.isSuccessful) {
                            val body = response.body()

                            // ------------------------------------------------------
                            // 🛠️ [파싱] ownedList와 serverPoints를 여기서 정의해야 함!
                            // ------------------------------------------------------
                            val rawOwned = body?.get("ownedCharacters")
                            val ownedList = mutableListOf<Int>()

                            when (rawOwned) {
                                is String -> rawOwned.split(",").forEach { s -> s.trim().toIntOrNull()?.let { ownedList.add(it) } }
                                is Number -> ownedList.add(rawOwned.toInt())
                                else -> ownedList.add(0)
                            }
                            if (!ownedList.contains(0)) ownedList.add(0)

                            // 서버 포인트 가져오기
                            val serverPoints = (body?.get("currentPoints") as? Number)?.toInt() ?: currentPoints

                            // ------------------------------------------------------
                            // 🛒 상점 다이얼로그 띄우기
                            // ------------------------------------------------------
                            ShopDialog(requireContext(), userId, serverPoints, ownedList, currentCharacterIndex) { newIdx, leftPoints ->

                                // [콜백] 장착 변경 시 -> 서버에 "나 이거 꼈어!"라고 저장 요청
                                updateEquippedCharacterOnServer(userId, newIdx)

                                // UI 변경
                                currentCharacterIndex = newIdx
                                if (newIdx < characterList.size) {
                                    binding.imageView.setImageResource(characterList[newIdx])
                                }
                                binding.tvUserPoints.text = "포인트 $leftPoints"

                                // 로컬 저장
                                requireContext().getSharedPreferences("UserSettings", Context.MODE_PRIVATE)
                                    .edit().putInt("SELECTED_CHARACTER_IDX", newIdx).apply()

                            }.show()

                        } else {
                            Toast.makeText(requireContext(), "서버 통신 실패", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("Home", "상점 로드 실패", e)
                    }
                }
            }
        }
    }

    private fun updateEquippedCharacterOnServer(userId: Long, idx: Int) {
        lifecycleScope.launch {
            try {
                RetrofitClient.problemApiService.equipCharacter(userId, idx)
            } catch (e: Exception) {
                Log.e("Home", "장착 저장 실패", e)
            }
        }
    }

    private fun validateEquippedCharacter() {
        val userId = AuthManager.getUserId(requireContext()) ?: return

        lifecycleScope.launch {
            try {
                // 서버에서 내 정보(소유 목록) 가져오기
                val response = RetrofitClient.problemApiService.getTodayStats(userId, "all")

                if (response.isSuccessful) {
                    val body = response.body()
                    val rawOwned = body?.get("ownedCharacters")
                    val ownedList = mutableListOf<Int>()

                    // 소유 목록 파싱 (기존 로직과 동일)
                    when (rawOwned) {
                        is String -> rawOwned.split(",").forEach { s -> s.trim().toIntOrNull()?.let { ownedList.add(it) } }
                        is Number -> ownedList.add(rawOwned.toInt())
                        else -> ownedList.add(0)
                    }
                    if (!ownedList.contains(0)) ownedList.add(0)

                    // 🚨 검증 시작: 현재 장착된 번호(currentCharacterIndex)가 소유 목록(ownedList)에 있는가?
                    if (!ownedList.contains(currentCharacterIndex)) {
                        // ❌ 내꺼 아님! (이전 사용자가 쓰던 것) -> 기본 캐릭터로 강제 초기화
                        Log.w("CharacterCheck", "미보유 캐릭터 장착 감지! 초기화 진행.")

                        currentCharacterIndex = 0 // 0번(펭귄)으로 변경

                        // 화면 갱신
                        binding.imageView.setImageResource(characterList[0])

                        // 로컬 저장소도 0번으로 덮어쓰기
                        val prefs = requireContext().getSharedPreferences("UserSettings", Context.MODE_PRIVATE)
                        prefs.edit().putInt("SELECTED_CHARACTER_IDX", 0).apply()

                        // (선택) 사용자에게 알림
                        // Toast.makeText(requireContext(), "장착 정보가 초기화되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("CharacterCheck", "검증 실패", e)
            }
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
                            if (reviewTimeMillis > now) retryCount++ else reviewCount++
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
                // 1. 코스 진행률 갱신
                val newCourses = courses.toMutableList()
                for (i in newCourses.indices) {
                    val course = newCourses[i]
                    val response = RetrofitClient.problemApiService.getTodayStats(currentUserId, course.title)
                    if (response.isSuccessful) {
                        val body = response.body()
                        val count = (body?.get("solvedCount") as? Number)?.toInt() ?: 0
                        val goal = 60
                        val percent = if (goal > 0) (count.toDouble() / goal * 100).toInt() else 0
                        newCourses[i] = course.copy(progressPercent = percent.coerceIn(0, 100), solvedCount = count)
                    }
                }
                courses = newCourses
                courseAdapter.updateItems(courses.toList())

                // 2. 일일 퀘스트 & 포인트 갱신
                val totalResponse = RetrofitClient.problemApiService.getTodayStats(currentUserId, "all")
                if (totalResponse.isSuccessful) {
                    val body = totalResponse.body()
                    val totalCount = (body?.get("solvedCount") as? Number)?.toInt() ?: 0
                    Log.d("d", "totalCount$totalCount")

                    val totalTimeSec = (body?.get("studyTime") as? Number)?.toLong() ?: 0L
                    val totalTimeMin = (totalTimeSec / 60).toInt()

                    Log.d("d", "totalTimeSec$totalTimeSec")

                    val serverPoints = (body?.get("currentPoints") as? Number)?.toInt() ?: 0
                    binding.tvUserPoints.text = "포인트 $serverPoints"

                    val goalTime = 30
                    val goalCount = 20
                    val isTimeDone = totalTimeMin >= goalTime
                    val isCountDone = totalCount >= goalCount

                    checkAndReward(currentUserId, "QUEST_TIME", isTimeDone, 100, "일일 학습 완료! 100P")
                    checkAndReward(currentUserId, "QUEST_COUNT", isCountDone, 100, "문제 풀이 완료! 100P")

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
        if (!isDone) return

        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val today = sdf.format(java.util.Date())
        val prefKey = "${questKey}_$today"

        val prefs = requireContext().getSharedPreferences("QuestPrefs", android.content.Context.MODE_PRIVATE)
        val alreadyReceived = prefs.getBoolean(prefKey, false)

        if (!alreadyReceived) {
            val response = RetrofitClient.problemApiService.rewardPoints(userId, amount)
            if (response.isSuccessful) {
                prefs.edit().putBoolean(prefKey, true).apply()
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
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