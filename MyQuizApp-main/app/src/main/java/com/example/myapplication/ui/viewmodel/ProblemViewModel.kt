package com.example.myapplication.ui.viewmodel

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Problem
import com.example.myapplication.data.model.SubmissionRequest
import com.example.myapplication.data.model.SubmissionResponse
import com.example.myapplication.data.remote.RetrofitClient
import kotlinx.coroutines.launch

class ProblemViewModel : ViewModel() {

    private var allProblems: List<Problem> = emptyList()
    private var currentProblemIndex : Int = 0

    private val _currentProblem = MutableLiveData<Problem?>()
    val currentProblem : LiveData<Problem?> = _currentProblem

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _allProblemsLiveData = MutableLiveData<List<Problem>>()
    val allProblemsLiveData: LiveData<List<Problem>> = _allProblemsLiveData

    private val _submissionResult = MutableLiveData<SubmissionResponse?>()
    val submissionResult: LiveData<SubmissionResponse?> = _submissionResult

    private val _hintContent = MutableLiveData<String>()
    val hintContent: LiveData<String> = _hintContent

    fun fetchProblems(userId: Long, courseId: String = "정보처리기능사"){
        viewModelScope.launch{
            Log.d("QUIZ_APP", "네트워크 통신 시작 시도... 코스ID: $courseId")
            try{
                val response = RetrofitClient.problemApiService.getTenProblems(userId, courseId)

                if(response.isSuccessful){
                    val receivedProblems = response.body() ?: emptyList()
                    allProblems = receivedProblems
                    _allProblemsLiveData.value = receivedProblems
                    Log.d("QUIZ_APP", "통신 성공, 문제 개수: ${receivedProblems.size}개")
                } else {
                    _errorMessage.value = "서버 응답 실패: ${response.code()}"
                }
            } catch(e: Exception){
                _errorMessage.value = "네트워크 오류: ${e.localizedMessage}"
            }
        }
    }

    // 🔥 [수정] onComplete 콜백 추가 (기본값 null)
    fun submitAnswer(
        problemId: Long,
        userId: Long,
        userAnswer: String,
        checkCount: Int,
        studyTime: Int,
        onComplete: (() -> Unit)? = null // 👈 추가됨: 작업 완료 후 실행할 함수
    ) {
        viewModelScope.launch {
            try {
                val request = SubmissionRequest(problemId, userId, userAnswer, checkCount, studyTime)
                val response = RetrofitClient.problemApiService.submitAnswer(request)

                if (response.isSuccessful) {
                    val result = response.body()
                    if (result != null) {
                        _submissionResult.value = result
                    } else {
                        _submissionResult.value = null
                    }
                } else {
                    _errorMessage.value = "답변 제출 실패: ${response.code()}"
                    _submissionResult.value = null
                }
            } catch (e: Exception) {
                Log.e("QUIZ_APP", "답변 제출 네트워크 오류: ${e.localizedMessage}")
                _errorMessage.value = "답변 제출 네트워크 오류: ${e.localizedMessage}"
                _submissionResult.value = null
            } finally {
                // 🔥 [추가] 통신이 성공하든 실패하든 작업이 끝나면 호출 (화면 종료 등을 위해)
                onComplete?.invoke()
            }
        }
    }

    fun nextProblem(){
        if(currentProblemIndex < allProblems.size - 1){
            currentProblemIndex++
            updateCurrentProblem()
        } else {
            _currentProblem.value = null
            _errorMessage.value = "모든 퀴즈를 완료했습니다!"
        }
    }

    private fun updateCurrentProblem(){
        if(allProblems.isNotEmpty() && currentProblemIndex < allProblems.size){
            _currentProblem.value = allProblems[currentProblemIndex]
        } else {
            _currentProblem.value = null
        }
    }

    fun getTotalProblemCoount(): Int{
        return allProblems.size
    }

    fun setCurrentIndex(index: Int){
        if(index >= 0 && index < allProblems.size){
            currentProblemIndex = index
            updateCurrentProblem()
        }
    }

    fun clearHintData() {
        _hintContent.value = ""
    }

    fun requestHint(problemId: Long, userId: Long, hintCount: Int) {
        viewModelScope.launch {
            try {
                val hintResponse = RetrofitClient.problemApiService.getHint(problemId, hintCount, userId)
                if (hintResponse.isSuccessful) {
                    _hintContent.value = hintResponse.body()?.hintText ?: "오류"
                } else {
                    _hintContent.value = "서버 오류"
                }
            } catch (e: Exception) {
                _hintContent.value = "네트워크 오류"
            }
        }
    }
}