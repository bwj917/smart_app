package com.example.myapplication.auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.MainActivity // 🔥 추가
import com.example.myapplication.data.remote.RetrofitClient
import com.example.myapplication.databinding.ActivitySignUpBinding
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Response

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private var isIdChecked = false
    private var isEmailVerified = false
    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupIdCheck()
        setupEmailVerification()
        setupSignUpButton()
    }

    private fun setupIdCheck() {
        binding.etId.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val loginId = binding.etId.text.toString().trim()
                if (loginId.isNotEmpty()) {
                    checkId(loginId)
                } else {
                    binding.tvIdCheckMessage.visibility = View.GONE
                }
            }
        }
    }

    private fun checkId(loginId: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.authApiService.checkId(loginId)
                if (response.isSuccessful && response.body() != null) {
                    val isAvailable = response.body()!!.isAvailable
                    binding.tvIdCheckMessage.visibility = View.VISIBLE
                    if (isAvailable) {
                        binding.tvIdCheckMessage.text = "사용 가능한 아이디입니다."
                        binding.tvIdCheckMessage.setTextColor(Color.BLUE) // Success color
                        isIdChecked = true
                    } else {
                        binding.tvIdCheckMessage.text = "이미 사용 중인 아이디입니다."
                        binding.tvIdCheckMessage.setTextColor(Color.RED) // Error color
                        isIdChecked = false
                    }
                } else {
                    showToast("아이디 중복 확인 실패") // <--- 66번째 줄 호출
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("네트워크 오류: ${e.message}")
            }
        }
    }

    private fun setupEmailVerification() {
        // 인증번호 발송
        binding.btnSendCode.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                showToast("이메일을 입력하세요.")
                return@setOnClickListener
            }
            sendEmailCode(email)
        }

        // 인증번호 확인
        binding.btnVerifyCode.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val code = binding.etEmailCode.text.toString().trim()

            if (email.isEmpty() || code.isEmpty()) {
                showToast("이메일과 인증번호를 입력하세요.")
                return@setOnClickListener
            }
            verifyEmailCode(email, code)
        }
    }

    private fun sendEmailCode(email: String) {
        binding.btnSendCode.isEnabled = false
        binding.btnSendCode.text = "발송 중..."
        binding.tvEmailMessage.text = ""
        binding.tvEmailMessage.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val email = binding.etEmail.text.toString().trim()
                val response = RetrofitClient.authApiService.sendEmailCode(email)
                if (response.isSuccessful) {
                    val msg = response.body()?.string() ?: "인증번호가 발송되었습니다."
                    binding.tvEmailMessage.text = msg
                    binding.tvEmailMessage.setTextColor(Color.BLUE)
                    binding.tvEmailMessage.visibility = View.VISIBLE

                    // 타이머 시작 (3분 = 180초)
                    startTimer(180 * 1000L)
                }
                else {
                    val errorMsg = response.errorBody()?.string() ?: "발송 실패"
                    binding.tvEmailMessage.text = errorMsg
                    binding.tvEmailMessage.setTextColor(Color.RED)
                    binding.tvEmailMessage.visibility = View.VISIBLE
                    binding.btnSendCode.isEnabled = true
                    binding.btnSendCode.text = "인증번호 발송"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                binding.tvEmailMessage.text = "오류 발생: ${e.message}"
                binding.tvEmailMessage.setTextColor(Color.RED)
                binding.tvEmailMessage.visibility = View.VISIBLE
                binding.btnSendCode.isEnabled = true
                binding.btnSendCode.text = "인증번호 발송"
            }
        }
    }

    private fun startTimer(millisInFuture: Long) {
        timer?.cancel()
        timer = object : CountDownTimer(millisInFuture, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = millisUntilFinished / 1000 % 60
                val timeString = String.format("%02d:%02d", minutes, seconds)
                binding.btnSendCode.text = "재전송 ($timeString)"
                binding.btnSendCode.isEnabled = false // 타이머 도중엔 비활성화 (JS 로직 참조)
            }

            override fun onFinish() {
                binding.btnSendCode.text = "인증번호 재전송"
                binding.btnSendCode.isEnabled = true
                binding.tvEmailMessage.text = "인증 시간이 만료되었습니다. 다시 시도해주세요."
                binding.tvEmailMessage.setTextColor(Color.RED)
            }
        }.start()
    }

    private fun verifyEmailCode(email: String, code: String) {
        binding.btnVerifyCode.isEnabled = false
        binding.btnVerifyCode.text = "확인 중..."

        lifecycleScope.launch {
            try {

                val response = RetrofitClient.authApiService.verifyEmailCode(email, code)

                if (response.isSuccessful) {
                    val msg = response.body()?.string() ?: "인증 성공"
                    binding.tvVerificationMessage.text = msg
                    binding.tvVerificationMessage.setTextColor(Color.BLUE)
                    binding.tvVerificationMessage.visibility = View.VISIBLE

                    // 성공 처리
                    isEmailVerified = true
                    binding.etEmail.isEnabled = false
                    binding.etEmailCode.isEnabled = false
                    binding.btnSendCode.isEnabled = false
                    binding.btnVerifyCode.isEnabled = false
                    timer?.cancel()
                    binding.btnSendCode.text = "인증 완료"

                    checkSignUpButtonState()

                } else {
                    val errorMsg = response.errorBody()?.string() ?: "인증 실패"
                    binding.tvVerificationMessage.text = errorMsg
                    binding.tvVerificationMessage.setTextColor(Color.RED)
                    binding.tvVerificationMessage.visibility = View.VISIBLE
                    binding.btnVerifyCode.isEnabled = true
                    binding.btnVerifyCode.text = "확인"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                binding.tvVerificationMessage.text = "오류 발생: ${e.message}"
                binding.tvVerificationMessage.setTextColor(Color.RED)
                binding.tvVerificationMessage.visibility = View.VISIBLE
                binding.btnVerifyCode.isEnabled = true
                binding.btnVerifyCode.text = "확인"
            }
        }
    }

    private fun setupSignUpButton() {
        binding.btnSignUp.setOnClickListener {
            // 1. 유효성 검사
            if (!isIdChecked) {
                showToast("아이디 중복 확인이 필요합니다.")
                return@setOnClickListener
            }
            if (!isEmailVerified) {
                showToast("이메일 인증이 필요합니다.")
                return@setOnClickListener
            }

            // 2. 입력값 가져오기
            val userid = binding.etId.text.toString().trim()
            val pw = binding.etPw.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val name = binding.etName.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val pwConfirm = binding.etPwConfirm.text.toString().trim()

            binding.btnSignUp.isEnabled = false // 중복 클릭 방지

            if (userid.isEmpty() || pw.isEmpty() || pwConfirm.isEmpty() || name.isEmpty() || phone.isEmpty()) {
                showToast("모든 정보를 입력해주세요.")
                binding.btnSignUp.isEnabled = true
                return@setOnClickListener
            }

            if (pw != pwConfirm) {
                showToast("비밀번호가 일치하지 않습니다.")
                binding.btnSignUp.isEnabled = true
                return@setOnClickListener
            }

            // 3. 서버 요청
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.authApiService.registerMember(
                        userid, pw, name, email, phone
                    )

                    if (response.isSuccessful) {
                        // 🔥 유저 ID를 AuthManager에 저장하고 바로 로그인 상태로 전환
                        val authResponse = response.body()
                        if (authResponse?.userId != null) {
                            // 유저 ID와 함께 로그인 상태 저장
                            AuthManager.setLoggedIn(this@SignUpActivity, true, authResponse.userId)

                            showToast("회원가입 및 로그인 성공!")
                            // 메인 화면으로 바로 이동
                            val intent = Intent(this@SignUpActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()

                        } else {
                            // 서버 응답에 userId가 없을 경우 (혹시 모를 상황 대비)
                            showToast("회원가입 성공! 이제 로그인 해주세요.")
                            finish()
                        }

                    } else {
                        // 서버 에러 메시지 확인
                        val errorMsg = response.errorBody()?.string() ?: "가입 실패"
                        showToast("오류: $errorMsg")
                        binding.btnSignUp.isEnabled = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    showToast("통신 오류: ${e.message}")
                    binding.btnSignUp.isEnabled = true
                }
            }
        }
    }

    private fun checkSignUpButtonState() {
        // 필요 시 버튼 활성화/비활성화 로직 추가
    }

    // 🔥 showToast 함수는 클래스의 멤버 함수로 정의되어야 합니다. (위치 확인)
    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}