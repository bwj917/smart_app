package com.example.myapplication.ui.quiz

import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
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

    private var currentUserId: Long = 0L
    private lateinit var courseId: String

    // 뷰 변수
    private lateinit var progress: LinearProgressIndicator
    private lateinit var tvPercent: TextView
    private lateinit var tvQuestion: TextView
    private lateinit var etAnswerInput: EditText
    private lateinit var btnSubmit: MaterialButton

    private lateinit var feedbackBar: View
    private lateinit var tvFeedback: TextView
    private lateinit var btnContinue: MaterialButton
    private lateinit var ivJudge: ImageView

    private lateinit var btnHint: MaterialButton

    private lateinit var tvLevel: TextView
    private lateinit var tvProblemStatus: TextView

    // 로직 변수
    private var skipAutoSave = false
    private var current = 1
    private var answered = false
    private var hintCount = 0
    private var currentHintText: CharSequence? = null
    private var solvedCount = 0

    private var previousLevel = 0
    private var startTime: Long = 0L // 시작 시간

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        courseId = intent.getStringExtra(CourseIds.EXTRA_COURSE_ID) ?: CourseIds.COMP_BASIC
        currentUserId = intent.getLongExtra(CourseIds.EXTRA_USER_ID, 0L)

        if (currentUserId == 0L) {
            Toast.makeText(this, "유저 정보가 유효하지 않습니다.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        bindViews()
        observeViewModel()

        val shouldReset = intent.getBooleanExtra("RESET_PROGRESS", false)

        if (shouldReset) {
            current = 1
            solvedCount = 0
            ProgressStore.save(this, courseId, currentIndex = 1, solvedCount = 0)
        } else {
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

        problemViewModel.fetchProblems(currentUserId, courseId)

        onBackPressedDispatcher.addCallback(this) { showExitConfirmDialog() }
    }

    private fun observeViewModel() {
        problemViewModel.allProblemsLiveData.observe(this) { problems ->
            if (problems.isNotEmpty()) {
                actualProblems = problems
                problemViewModel.setCurrentIndex(current - 1)
                setupProgress()
                renderQuestion()
                updateProgress()
            }
        }

        problemViewModel.submissionResult.observe(this) { result ->
            if (result != null) {
                renderSubmitResult(result.isCorrect, result.updatedProblem)
                if (result.isCorrect) solvedCount++
            }
        }

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

                if (hintCount >= 3) {
                    btnHint.isEnabled = false
                    btnHint.text = "힌트 소진"
                } else {
                    updateHintButtonState(hintCount)
                }
            }
        }

        problemViewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrEmpty()) Log.e(TAG, "Error: $message")
        }
    }

    private fun bindViews() {
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
            problemViewModel.nextProblem()
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

        problemViewModel.submitAnswer(currentProblem.problemId, currentUserId, userAnswer, hintCount, durationSeconds)
    }

    private fun renderQuestion() {
        val item = actualProblems.getOrNull(current - 1) ?: return

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

        startTime = System.currentTimeMillis() // 시간 측정 시작

        btnHint.isEnabled = true
        currentHintText = null
        btnHint.setIconResource(R.drawable.ic_lightbulb)
        btnHint.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        btnHint.iconPadding = (8 * resources.displayMetrics.density).toInt()

        updateHintButtonState(0)

        tvQuestion.text = item.question
        findViewById<TextView>(R.id.tvQuestionTitle).text = "${current} / ${total} 문제"

        etAnswerInput.setText("")
        etAnswerInput.hint = "여기에 정답을 입력하세요"
        etAnswerInput.isEnabled = true
        answered = false

        hideFeedbacks()
        problemViewModel.clearHintData()

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

    // 🔥 [핵심 수정] 나가기 버튼 클릭 시 학습 시간 저장 후 종료
    private fun showExitConfirmDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("퀴즈 나가기")
            .setMessage("학습 내용은 모두 저장 됩니다.")
            .setNegativeButton("취소") { d, _ -> d.dismiss() }
            .setPositiveButton("나가기") { d, _ ->
                d.dismiss()

                // 현재 풀고 있던 문제의 시간 계산
                val endTime = System.currentTimeMillis()
                val durationSeconds = ((endTime - startTime) / 1000).toInt()
                val currentProblem = actualProblems.getOrNull(current - 1)

                // 아직 제출 안 했고, 1초 이상 공부했고, 문제가 유효하다면
                if (!answered && durationSeconds > 0 && currentProblem != null) {
                    // 빈 문자열("")을 정답으로 제출 -> 서버에서 오답 처리 + 시간 기록됨
                    // onComplete 콜백으로 finish() 실행
                    Toast.makeText(this, "학습 기록 저장 중...", Toast.LENGTH_SHORT).show()
                    problemViewModel.submitAnswer(
                        problemId = currentProblem.problemId,
                        userId = currentUserId,
                        userAnswer = "",
                        checkCount = hintCount,
                        studyTime = durationSeconds,
                        onComplete = {
                            ProgressStore.saveSync(this, courseId, currentIndex = current, solvedCount = solvedCount)
                            finish()
                        }
                    )
                } else {
                    // 이미 제출했거나 문제 로딩 전이면 바로 종료
                    ProgressStore.saveSync(this, courseId, currentIndex = current, solvedCount = solvedCount)
                    finish()
                }
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

    private fun updateHintButtonState(count: Int) {
        val mainText = "힌트 보기"
        val subText = if (count < 2) "  (Lv -1)" else "  (Lv 초기화)"
        val subColor = if (count < 2) "#E0E0E0" else "#FF5252"

        val builder = SpannableStringBuilder()
        builder.append(mainText)
        val start = builder.length
        builder.append(subText)
        val end = builder.length

        builder.setSpan(RelativeSizeSpan(0.9f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(ForegroundColorSpan(Color.parseColor(subColor)), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        btnHint.text = builder

        class QuizActivity : AppCompatActivity() {

            private val problemViewModel: ProblemViewModel by viewModels()
            private var actualProblems: List<Problem> = emptyList()
            private val total get() = actualProblems.size

            private var currentUserId: Long = 0L
            private lateinit var courseId: String

            // 뷰 변수
            private lateinit var progress: LinearProgressIndicator
            private lateinit var tvPercent: TextView
            private lateinit var tvQuestion: TextView
            private lateinit var etAnswerInput: EditText
            private lateinit var btnSubmit: MaterialButton

            private lateinit var feedbackBar: View
            private lateinit var tvFeedback: TextView
            private lateinit var btnContinue: MaterialButton
            private lateinit var ivJudge: ImageView

            private lateinit var btnHint: MaterialButton

            private lateinit var tvLevel: TextView
            private lateinit var tvProblemStatus: TextView

            // 로직 변수
            private var skipAutoSave = false
            private var current = 1
            private var answered = false
            private var hintCount = 0
            private var currentHintText: CharSequence? = null
            private var solvedCount = 0

            private var previousLevel = 0
            private var startTime: Long = 0L // 시작 시간

            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.activity_quiz)

                courseId = intent.getStringExtra(CourseIds.EXTRA_COURSE_ID) ?: CourseIds.COMP_BASIC
                currentUserId = intent.getLongExtra(CourseIds.EXTRA_USER_ID, 0L)

                if (currentUserId == 0L) {
                    Toast.makeText(this, "유저 정보가 유효하지 않습니다.", Toast.LENGTH_LONG).show()
                    finish()
                    return
                }

                bindViews()
                observeViewModel()

                val shouldReset = intent.getBooleanExtra("RESET_PROGRESS", false)

                if (shouldReset) {
                    current = 1
                    solvedCount = 0
                    ProgressStore.save(this, courseId, currentIndex = 1, solvedCount = 0)
                } else {
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

                problemViewModel.fetchProblems(currentUserId, courseId)

                onBackPressedDispatcher.addCallback(this) { showExitConfirmDialog() }
            }

            private fun observeViewModel() {
                problemViewModel.allProblemsLiveData.observe(this) { problems ->
                    if (problems.isNotEmpty()) {
                        actualProblems = problems
                        problemViewModel.setCurrentIndex(current - 1)
                        setupProgress()
                        renderQuestion()
                        updateProgress()
                    }
                }

                problemViewModel.submissionResult.observe(this) { result ->
                    if (result != null) {
                        renderSubmitResult(result.isCorrect, result.updatedProblem)
                        if (result.isCorrect) solvedCount++
                    }
                }

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

                        if (hintCount >= 3) {
                            btnHint.isEnabled = false
                            btnHint.text = "힌트 소진"
                        } else {
                            updateHintButtonState(hintCount)
                        }
                    }
                }

                problemViewModel.errorMessage.observe(this) { message ->
                    if (!message.isNullOrEmpty()) Log.e(TAG, "Error: $message")
                }
            }

            private fun bindViews() {
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
                    problemViewModel.nextProblem()
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

                problemViewModel.submitAnswer(currentProblem.problemId, currentUserId, userAnswer, hintCount, durationSeconds)
            }

            private fun renderQuestion() {
                val item = actualProblems.getOrNull(current - 1) ?: return

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

                startTime = System.currentTimeMillis() // 시간 측정 시작

                btnHint.isEnabled = true
                currentHintText = null
                btnHint.setIconResource(R.drawable.ic_lightbulb)
                btnHint.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
                btnHint.iconPadding = (8 * resources.displayMetrics.density).toInt()

                updateHintButtonState(0)

                tvQuestion.text = item.question
                findViewById<TextView>(R.id.tvQuestionTitle).text = "${current} / ${total} 문제"

                etAnswerInput.setText("")
                etAnswerInput.hint = "여기에 정답을 입력하세요"
                etAnswerInput.isEnabled = true
                answered = false

                hideFeedbacks()
                problemViewModel.clearHintData()

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

            // 🔥 [핵심 수정] 나가기 버튼 클릭 시 학습 시간 저장 후 종료
            private fun showExitConfirmDialog() {
                MaterialAlertDialogBuilder(this)
                    .setTitle("퀴즈 나가기")
                    .setMessage("나가면 현재 문제의 공부 시간은 기록됩니다. 나갈까요?")
                    .setNegativeButton("취소") { d, _ -> d.dismiss() }
                    .setPositiveButton("나가기") { d, _ ->
                        d.dismiss()

                        // 현재 풀고 있던 문제의 시간 계산
                        val endTime = System.currentTimeMillis()
                        val durationSeconds = ((endTime - startTime) / 1000).toInt()
                        val currentProblem = actualProblems.getOrNull(current - 1)

                        // 아직 제출 안 했고, 1초 이상 공부했고, 문제가 유효하다면
                        if (!answered && durationSeconds > 0 && currentProblem != null) {
                            // 빈 문자열("")을 정답으로 제출 -> 서버에서 오답 처리 + 시간 기록됨
                            // onComplete 콜백으로 finish() 실행
                            Toast.makeText(this, "학습 기록 저장 중...", Toast.LENGTH_SHORT).show()
                            problemViewModel.submitAnswer(
                                problemId = currentProblem.problemId,
                                userId = currentUserId,
                                userAnswer = "",
                                checkCount = hintCount,
                                studyTime = durationSeconds,
                                onComplete = {
                                    ProgressStore.saveSync(this, courseId, currentIndex = current, solvedCount = solvedCount)
                                    finish()
                                }
                            )
                        } else {
                            // 이미 제출했거나 문제 로딩 전이면 바로 종료
                            ProgressStore.saveSync(this, courseId, currentIndex = current, solvedCount = solvedCount)
                            finish()
                        }
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

            private fun updateHintButtonState(count: Int) {
                val mainText = "힌트 보기"
                val subText = if (count < 2) "  (Lv -1)" else "  (Lv 초기화)"
                val subColor = if (count < 2) "#E0E0E0" else "#FF5252"

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
        }}

    override fun onPause() {
        super.onPause()
        if (!skipAutoSave) {
            ProgressStore.save(this, courseId, currentIndex = current, solvedCount = solvedCount)
        }
    }
}