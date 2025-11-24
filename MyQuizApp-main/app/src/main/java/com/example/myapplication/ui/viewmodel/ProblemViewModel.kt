package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Problem
import com.example.myapplication.data.model.SubmissionRequest
import com.example.myapplication.data.remote.RetrofitClient
import kotlinx.coroutines.launch

class ProblemViewModel : ViewModel() {

    private val _allProblemsLiveData = MutableLiveData<List<Problem>>()
    val allProblemsLiveData: LiveData<List<Problem>> get() = _allProblemsLiveData

    private val _submissionResult = MutableLiveData<SubmissionResult?>()
    val submissionResult: LiveData<SubmissionResult?> get() = _submissionResult

    private val _hintContent = MutableLiveData<String?>()
    val hintContent: LiveData<String?> get() = _hintContent

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private val _currentIndex = MutableLiveData<Int>(0)
    val currentIndex: LiveData<Int> get() = _currentIndex

    fun setCurrentIndex(index: Int) {
        _currentIndex.value = index
    }

    fun nextProblem() {
        _currentIndex.value = (_currentIndex.value ?: 0) + 1
        _submissionResult.value = null
    }

    fun clearHintData() {
        _hintContent.value = null
    }

    fun fetchProblems(userId: Long, courseId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.problemApiService.getTenProblems(userId, courseId)
                if (response.isSuccessful) {
                    _allProblemsLiveData.postValue(response.body() ?: emptyList())
                } else {
                    _errorMessage.postValue("문제 로드 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                _errorMessage.postValue("네트워크 오류: ${e.message}")
            }
        }
    }

    // 🔥 [수정] studyTime 파라미터 추가
    fun submitAnswer(problemId: Long, userId: Long, userAnswer: String, hintCount: Int, studyTime: Int) {
        viewModelScope.launch {
            try {
                val request = SubmissionRequest(
                    userId = userId,
                    problemId = problemId,
                    userAnswer = userAnswer,
                    checkCount = hintCount,
                    studyTime = studyTime // 🔥 추가
                )
                val response = RetrofitClient.problemApiService.submitAnswer(request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    val updatedProblem = body.problemResponse?.let { dto ->
                        Problem(
                            problemId = dto.problemId,
                            question = dto.question,
                            answer = dto.answer,
                            problemLevel = dto.stats?.problemLevel ?: 0,
                            nextReviewTime = null
                        )
                    }

                    _submissionResult.postValue(
                        SubmissionResult(
                            isCorrect = body.correct,
                            updatedProblem = updatedProblem
                        )
                    )
                } else {
                    _errorMessage.postValue("제출 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                _errorMessage.postValue("제출 오류: ${e.message}")
            }
        }
    }

    fun requestHint(problemId: Long, userId: Long, hintCount: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.problemApiService.getHint(problemId, hintCount, userId)
                if (response.isSuccessful) {
                    // 🔥 [수정] 서버 모델에 맞춰 hintText 사용
                    _hintContent.postValue(response.body()?.hintText)
                } else {
                    _errorMessage.postValue("힌트 요청 실패")
                }
            } catch (e: Exception) {
                _errorMessage.postValue("힌트 통신 오류: ${e.message}")
            }
        }
    }
}

data class SubmissionResult(
    val isCorrect: Boolean,
    val updatedProblem: Problem?
)