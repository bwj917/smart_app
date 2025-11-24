package com.example.myapplication.ui.quiz

import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText // 🔥 EditText 사용 (ClassCastException 방지)
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.myapplication.R
import com.example.myapplication.data.model.Problem
import com.example.myapplication.ui.viewmodel.ProblemViewModel
import com.example.myapplication.util.toProblemStatusText
import com.example.myapplication.util.toRelativeReviewTime
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator

class QuizActivity : AppCompatActivity() {

    private val problemViewModel: ProblemViewModel by viewModels()
    private var actualProblems: List<Problem> = emptyList()
    private val total get() = actualProblems.size

    // 🔥 수정: Intent에서 받아오기 위해 var로 변경
    private var currentUserId: Long = 0L
    private lateinit var courseId: String

    // 뷰 변수
    private lateinit var progress: LinearProgressIndicator
    private lateinit var tvPercent: TextView
    private lateinit var tvQuestion: TextView
    private lateinit var etAnswerInput: EditText // 🔥 EditText 타입 유지
    private lateinit var btnSubmit: MaterialButton

    private lateinit var feedbackBar: View
    private lateinit var tvFeedback: TextView
    private lateinit var btnContinue: MaterialButton
    private lateinit var ivJudge: ImageView

    private lateinit var btnHint: MaterialButton

    // 레벨 및 상태 표시용 뷰
    private lateinit var tvLevel: TextView
    private lateinit var tvProblemStatus: TextView

    // 로직 변수
    private var skipAutoSave = false
    private var current = 1
    private var answered = false
    private var hintCount = 0
    private var currentHintText: CharSequence? = null
    private var solvedCount = 0

    // 정답 제출 전 레벨을 기억하기 위한 변수
    private var previousLevel = 0

    private var startTime: Long = 0L // 시작 시간

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        // 1. 데이터 수신 (Intent)
        courseId = intent.getStringExtra(CourseIds.EXTRA_COURSE_ID) ?: CourseIds.COMP_BASIC
        currentUserId = intent.getLongExtra(CourseIds.EXTRA_USER_ID, 0L)

        if (currentUserId == 0L) {
            Toast.makeText(this, "유저 정보가 유효하지 않습니다.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // 2. 초기화
        bindViews()

        // 3. ViewModel 관찰
        observeViewModel()

        // 4. 진행 상황 로드
        val shouldReset = intent.getBooleanExtra("RESET_PROGRESS", false)

        if (shouldReset) {
            // 초기화 요청이 오면 무조건 1번부터 시작
            current = 1
            solvedCount = 0
            // 저장소도 즉시 초기화 (덮어쓰기)
            ProgressStore.save(this, courseId, currentIndex = 1, solvedCount = 0)
        } else {
            // 기존 로직 (이어하기)
            val (savedIndex, savedSolved) = ProgressStore.load(this, courseId)
            if (savedSolved >= 10 || savedIndex > 10) {
                current = 1
                solvedCount = 0
                ProgressStore.save(this, courseId, currentIndex = 1, solvedCount = 0)
            } else {
                current = savedIndex.coerceAtLeast(1)
                solvedCount = savedSolved
            }
        }

        // 5. 서버에 문제 요청
        Log.d(TAG, "문제 요청 시작: UserID=$currentUserId, Course=$courseId")
        problemViewModel.fetchProblems(currentUserId, courseId)

        // 6. 뒤로가기 콜백 (showExitConfirmDialog 함수가 아래에 정의되어 있어야 함)
        onBackPressedDispatcher.addCallback(this) { showExitConfirmDialog() }
    }

    private fun observeViewModel() {
        // 문제 목록 관찰
        problemViewModel.allProblemsLiveData.observe(this) { problems ->
            if (problems.isNotEmpty()) {
                actualProblems = problems
                Log.i(TAG, "서버에서 ${problems.size}개의 문제 수신 완료")

                // ViewModel 인덱스 동기화
                problemViewModel.setCurrentIndex(current - 1)

                // UI 초기화 및 화면 그리기 (원래 코드 함수명 사용)
                setupProgress()
                renderQuestion()
                updateProgress()
            } else {
                Log.w(TAG, "수신된 문제 목록이 비어 있습니다.")
            }
        }

        // 제출 결과 관찰
        problemViewModel.submissionResult.observe(this) { result ->
            if (result != null) {
                renderSubmitResult(result.isCorrect, result.updatedProblem)
                if (result.isCorrect) solvedCount++
            } else if (answered) {
                Log.e(TAG, "문제 제출 결과 수신 실패 (NULL)")
            }
        }

        // 힌트 관찰
        problemViewModel.hintContent.observe(this) { hint ->
            if (!hint.isNullOrEmpty()) {
                var fullHint: String? = null
                if (hintCount == 1) {
                    fullHint = "정답은 ${hint}글자입니다."
                } else if (hintCount > 1) {
                    fullHint = "$hint"
                }

                currentHintText = fullHint

                if (fullHint != null) {
                    etAnswerInput.hint = fullHint
                    Toast.makeText(this, "힌트: $fullHint", Toast.LENGTH_SHORT).show()
                }
                // 🔥 [추가] 힌트를 받았으므로 버튼 상태 업데이트
                // hintCount는 이미 증가된 상태입니다. (1, 2, 3...)

                if (hintCount >= 3) {
                    btnHint.isEnabled = false
                    btnHint.text = "힌트 소진"
                } else {
                    // 다음 힌트를 위한 텍스트 업데이트
                    updateHintButtonState(hintCount)
                }
            }
        }

        // 에러 메시지 관찰
        problemViewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrEmpty()) Log.e(TAG, "Error: $message")
        }
    }

    private fun bindViews() {
        // 🔥 XML의 TextInputLayout 내부 EditText를 가져옵니다. (캐스팅 없이 EditText로 받음)
        etAnswerInput = findViewById(R.id.etAnswerInput)

        btnSubmit = findViewById(R.id.btnSubmit)
        progress = findViewById(R.id.progressQuiz)
        tvPercent = findViewById(R.id.tvProgressPercent)
        tvQuestion = findViewById(R.id.tvQuestion)
        ivJudge = findViewById(R.id.ivJudge)

        feedbackBar = findViewById(R.id.feedbackBar)
        tvFeedback = findViewById(R.id.tvFeedback)
        btnContinue = findViewById(R.id.btnContinue)
        btnHint = findViewById(R.id.btnHint)

        tvProblemStatus = findViewById(R.id.tvProblemStatus)
        tvLevel = findViewById(R.id.tvLevel)

        bindHintClick()
        bindSubmitClick()

        btnContinue.setOnClickListener { goToNextProblem() }

        etAnswerInput.setOnEditorActionListener { _, actionId, event ->
            val isEnterAction = actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.action == android.view.KeyEvent.ACTION_DOWN)

            if (isEnterAction) {
                val imm = ContextCompat.getSystemService(this, InputMethodManager::class.java)
                imm?.hideSoftInputFromWindow(etAnswerInput.windowToken, 0)
                if (answered) goToNextProblem() else submitCurrentAnswer()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

    private fun goToNextProblem() {
        if (current < total) {
            current += 1
            problemViewModel.nextProblem() // ViewModel 인덱스 증가
            answered = false
            hintCount = 0

            hideFeedbacks()
            renderQuestion()
            updateProgress()
            btnContinue.text = if (current == total) "완료" else "다음 문제"
            ProgressStore.save(this, courseId, currentIndex = current, solvedCount = solvedCount)
        } else {
            progress.setProgressCompat(total, true)
            tvPercent.text = "100%"
            showCompletion()
        }
    }

    private fun setupProgress() {
        progress.max = total
        updateProgress()
        btnContinue.text = if (current == total) "완료" else "다음 문제"
    }

    private fun updateProgress() {
        val currentProgress = (current - 1).coerceAtLeast(0)
        progress.setProgressCompat(currentProgress, true)
        val pct = if (total == 0) 0 else (currentProgress.toFloat() / total * 100).toInt()
        tvPercent.text = "$pct%"
    }

    private fun bindSubmitClick() {
        btnSubmit.setOnClickListener {
            submitCurrentAnswer()
        }
    }

    private fun submitCurrentAnswer() {
        if (answered) return

        val userAnswer = etAnswerInput.text.toString().trim()
        val currentProblem = actualProblems.getOrNull(current - 1)

        val endTime = System.currentTimeMillis()
        val durationSeconds = ((endTime - startTime) / 1000).toInt()

        if (currentProblem == null) return

        if (userAnswer.isBlank()) {
            tvFeedback.text = "답변을 입력해주세요."
            feedbackBar.visibility = View.VISIBLE
            return
        }

        btnSubmit.isEnabled = false
        etAnswerInput.isEnabled = false

        problemViewModel.submitAnswer(currentProblem.problemId, currentUserId, userAnswer, hintCount,durationSeconds)
    }

    private fun renderQuestion() {
        val item = actualProblems.getOrNull(current - 1) ?: return

        // 1. 문제 상태 및 레벨 텍스트 표시
        tvProblemStatus.text = item.toProblemStatusText()
        previousLevel = item.problemLevel ?: 0

        if (previousLevel == 0) {
            tvLevel.visibility = View.GONE
        } else {
            tvLevel.visibility = View.VISIBLE
            when (previousLevel) {
                1 -> {
                    tvLevel.text = "복습 1단계 "
                    tvLevel.setTextColor(Color.parseColor("#FF5252"))
                }
                2 -> {
                    tvLevel.text = "복습 2단계 "
                    tvLevel.setTextColor(Color.parseColor("#FF9800"))
                }
                3 -> {
                    tvLevel.text = "복습 3단계 "
                    tvLevel.setTextColor(Color.parseColor("#FBC02D"))
                }
                4 -> {
                    tvLevel.text = "복습 4단계 "
                    tvLevel.setTextColor(Color.parseColor("#4CAF50"))
                }
                5 -> {
                    tvLevel.text = "복습 5단계"
                    tvLevel.setTextColor(Color.parseColor("#2196F3"))
                }
                else -> {
                    tvLevel.text = "복습 ${previousLevel}단계"
                    tvLevel.setTextColor(Color.parseColor("#555555"))
                }
            }
        }

        startTime = System.currentTimeMillis()

        // 힌트 버튼 초기화
        btnHint.isEnabled = true
        currentHintText = null
        btnHint.setIconResource(R.drawable.ic_lightbulb)
        btnHint.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        btnHint.iconPadding = (8 * resources.displayMetrics.density).toInt()

        // 🔥 [수정] 별도 함수로 분리하여 초기 상태(0회 사용) 적용
        updateHintButtonState(0)

        // 텍스트 구성: "힌트 보기  (Lv -1)"
        val mainText = "힌트 보기"
        val subText = "  (Lv -1)" // 패널티 문구

        val builder = SpannableStringBuilder()
        builder.append(mainText)

        val start = builder.length
        builder.append(subText)
        val end = builder.length

        // " (Lv -1)" 부분 스타일 적용 (작게, 빨간색, 굵게)
        builder.setSpan(RelativeSizeSpan(0.9f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(ForegroundColorSpan(Color.parseColor("#E0E0E0")), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        btnHint.text = builder


        // 3. 문제 텍스트 및 입력창 초기화
        tvQuestion.text = item.question
        findViewById<TextView>(R.id.tvQuestionTitle).text = "${current} / ${total} 문제"

        etAnswerInput.setText("")
        etAnswerInput.hint = "여기에 정답을 입력하세요"
        etAnswerInput.isEnabled = true
        answered = false

        hideFeedbacks()
        problemViewModel.clearHintData()

        // 4. 캐릭터 및 버튼 상태 초기화
        ivJudge.setImageResource(R.drawable.quit2)
        btnSubmit.visibility = View.VISIBLE
        btnContinue.visibility = View.GONE
    }

    private fun hideFeedbacks() {
        feedbackBar.visibility = View.GONE
        tvFeedback.text = ""
    }

    private fun renderSubmitResult(isCorrect: Boolean, updatedProblem: Problem?) {
        feedbackBar.visibility = View.VISIBLE
        answered = true
        etAnswerInput.isEnabled = false
        btnSubmit.visibility = View.GONE
        btnContinue.visibility = View.VISIBLE

        if (isCorrect) {
            val newLevel = updatedProblem?.problemLevel ?: 0
            val levelDiff = newLevel - previousLevel
            // val statusText = if (levelDiff > 0) "단계 상승 (+${levelDiff})" else "단계 유지"
            val statusColor = ContextCompat.getColor(this, R.color.brand_primary)

            val reviewTime = updatedProblem?.nextReviewTime
            val timeText = reviewTime.toRelativeReviewTime()

            val firstLine = "정답입니다! ($timeText)\n"
            var secondLine = ""
            var secondLineColor = Color.parseColor("#555555")

            when {
                newLevel < previousLevel -> {
                    secondLine = "📉 힌트 사용: ${previousLevel}단계 ➔ ${newLevel}단계 하락"
                    secondLineColor = Color.parseColor("#FF5252")
                }
                newLevel > previousLevel -> {
                    val prevText = if (previousLevel == 0) "새 문제" else "${previousLevel}단계"
                    secondLine = "✨ 실력 상승: $prevText ➔ ${newLevel}단계 Up!"
                    secondLineColor = ContextCompat.getColor(this, R.color.brand_primary)
                }
                else -> {
                    secondLine = "현재 단계 유지 (${newLevel}단계)"
                }
            }

            val builder = SpannableStringBuilder()
            val start1 = builder.length
            builder.append(firstLine)
            val end1 = builder.length
            builder.setSpan(RelativeSizeSpan(1.3f), start1, end1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.brand_primary)), start1, end1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            val start2 = builder.length
            builder.append(secondLine)
            val end2 = builder.length
            builder.setSpan(RelativeSizeSpan(1.0f), start2, end2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(ForegroundColorSpan(secondLineColor), start2, end2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            tvFeedback.text = builder
            ivJudge.setImageResource(R.drawable.quit3)

        } else {
            tvFeedback.text = "아쉽다! 오답이에요."
            tvFeedback.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            ivJudge.setImageResource(R.drawable.quit4)

            // 오답 시 다시 풀기 기능을 원하면 아래 주석 해제
            etAnswerInput.isEnabled = true
            btnSubmit.visibility = View.VISIBLE
            btnContinue.visibility = View.GONE
            answered = false
        }
        ivJudge.visibility = View.VISIBLE
    }

    private fun bindHintClick() {
        btnHint.setOnClickListener {
            if (answered) return@setOnClickListener
            hintCount += 1

            val currentProblem = actualProblems.getOrNull(current - 1) ?: return@setOnClickListener
            problemViewModel.requestHint(currentProblem.problemId, currentUserId, hintCount)
        }
    }

    // 🔥 여기에 누락되었던 함수를 정의합니다.
    private fun showExitConfirmDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("퀴즈 나가기")
            .setMessage("나가면 진행 상황이 저장돼요. 나갈까요?")
            .setNegativeButton("취소") { d, _ -> d.dismiss() }
            .setPositiveButton("나가기") { d, _ ->
                d.dismiss()
                ProgressStore.saveSync(this, courseId, currentIndex = current, solvedCount = solvedCount)
                finish()
            }
            .show()
    }

    private fun showCompletion() {
        MaterialAlertDialogBuilder(this)
            .setTitle("완료")
            .setMessage("모든 문제를 풀었습니다!\n총 ${solvedCount}문제 정답!")
            .setPositiveButton("확인") { d, _ ->
                d.dismiss()
                skipAutoSave = true
                ProgressStore.saveSync(this, courseId, currentIndex = total, solvedCount = solvedCount)
                finish()
            }
            .show()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    private fun updateHintButtonState(count: Int) {
        val mainText = "힌트 보기"
        // 0번, 1번 사용 후 -> 다음은 -1 감소
        // 2번 사용 후 -> 다음(3번째)은 초기화
        val subText = if (count < 2) "  (Lv -1)" else "  (Lv 초기화)"
        val subColor = if (count < 2) "#E0E0E0" else "#FF5252" // 초기화는 빨간색 경고

        val builder = SpannableStringBuilder()
        builder.append(mainText)
        val start = builder.length
        builder.append(subText)
        val end = builder.length

        builder.setSpan(RelativeSizeSpan(0.9f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(ForegroundColorSpan(Color.parseColor(subColor)), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        btnHint.text = builder
    }

    override fun onPause() {
        super.onPause()
        if (!skipAutoSave) {
            ProgressStore.save(this, courseId, currentIndex = current, solvedCount = solvedCount)
        }
    }
}