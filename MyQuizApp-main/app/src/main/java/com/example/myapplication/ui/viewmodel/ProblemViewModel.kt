package com.example.myapplication.ui.viewmodel

import android.content.ContentValues.TAG
import android.nfc.Tag
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
import kotlin.math.log

class ProblemViewModel:ViewModel() { // ViewModel 상속

    private var allProblems: List<Problem> = emptyList()

    private var currentProblemIndex : Int = 0

    private val _currentProblem = MutableLiveData<Problem?>()
    // ui가 관찰할 문제
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
                // 🔥 [수정] 여기에 courseId를 넣어주세요!
                val response = RetrofitClient.problemApiService.getTenProblems(userId, courseId)

                if(response.isSuccessful){
                    val receivedProblems = response.body() ?: emptyList()
                    allProblems = receivedProblems
                    _allProblemsLiveData.value = receivedProblems
                    Log.d("QUIZ_APP", "통신 성공, 문제 개수: ${receivedProblems.size}개")
                }else{
                    _errorMessage.value = "서버 응답 실패: ${response.code()}"
                }
            }catch(e: Exception){
                _errorMessage.value = "네트워크 오류: ${e.localizedMessage}"
            }
        }
    }

    fun submitAnswer(problemId: Long, userId: Long, userAnswer: String, checkCount: Int, studyTime: Int) {
        viewModelScope.launch {
            try {
                val request = SubmissionRequest(problemId, userId, userAnswer, checkCount, studyTime)

                val response = RetrofitClient.problemApiService.submitAnswer(request)

                Log.d(TAG, "체크체크${response.isSuccessful}")

                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d(TAG, "체크체크2${result}")

                    if (result != null) {
                        _submissionResult.value = result
                    } else {
                        Log.w("QUIZ_APP", "답변 제출 성공 (본문 없음). 서버가 응답을 보내도록 확인 필요.")
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
        Log.i("QUIZ_APP", "updateCurrentProblem 호출됨. 인덱스: $currentProblemIndex, 전체 개수: ${allProblems.size}")
        if(allProblems.isNotEmpty() && currentProblemIndex < allProblems.size){
            _currentProblem.value = allProblems[currentProblemIndex]
            Log.i("QUIZ_APP", "문제 할당 성공: ${allProblems[currentProblemIndex].question}")
        } else {
            _currentProblem. value = null
            Log.w("QUIZ_APP", "할당할 문제가 없거나 인덱스 오류.")
        }
    }

    fun getTotalProblemCoount(): Int{
        return allProblems.size
    }

    fun setCurrentIndex(index: Int){
        if(index >= 0 && index < allProblems.size){
            if(index >= 0 && index < allProblems.size){
                currentProblemIndex = index
                updateCurrentProblem()
            }
        }
    }

    fun clearHintData() {
        _hintContent.value = ""
    }

    fun requestHint(problemId: Long, userId: Long, hintCount: Int) { // 👈 userId 받기
        viewModelScope.launch {
            try {
                // API 호출 시 userId 전달
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