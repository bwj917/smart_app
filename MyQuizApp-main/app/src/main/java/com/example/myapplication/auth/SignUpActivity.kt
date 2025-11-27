package com.example.myapplication.auth

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.MainActivity
import com.example.myapplication.data.remote.RetrofitClient
import com.example.myapplication.databinding.ActivitySignUpBinding
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private var isIdChecked = false
    private var isEmailVerified = false
    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupStep1IdCheck()
        setupStep2PwEnter()
        setupStep3PwConfirm()
        setupStep4Email()
        setupStep6SignUp()
    }

    // [STEP 1] 아이디 입력 및 중복 확인
    private fun setupStep1IdCheck() {
        binding.etId.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val loginId = binding.etId.text.toString().trim()
                if (loginId.isNotEmpty()) {
                    checkId(loginId)
                }
            }
        }

        binding.etId.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                val loginId = binding.etId.text.toString().trim()
                if (loginId.isNotEmpty()) {
                    checkId(loginId)
                }
                return@setOnEditorActionListener true
            }
            false
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
                        binding.tvIdCheckMessage.setTextColor(Color.BLUE)
                        isIdChecked = true

                        // 🔥 다음 단계 노출 (애니메이션 자동 적용)
                        if (binding.layoutStepPw.visibility == View.GONE) {
                            binding.layoutStepPw.visibility = View.VISIBLE
                            binding.etPw.requestFocus()
                        } else {
                            binding.etPw.requestFocus()
                        }
                    } else {
                        binding.tvIdCheckMessage.text = "이미 사용 중인 아이디입니다."
                        binding.tvIdCheckMessage.setTextColor(Color.RED)
                        isIdChecked = false
                    }
                } else {
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // [STEP 2] 비밀번호 입력 후 엔터
    private fun setupStep2PwEnter() {
        binding.etPw.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                val pw = binding.etPw.text.toString()
                if (pw.isNotEmpty()) {
                    binding.layoutStepInfo.visibility = View.VISIBLE
                    binding.etPwConfirm.requestFocus()
                }
                return@setOnEditorActionListener true
            }
            false
        }
    }

    // [STEP 3] 비밀번호 확인
    private fun setupStep3PwConfirm() {
        val pwWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validatePasswordMatch()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        binding.etPw.addTextChangedListener(pwWatcher)
        binding.etPwConfirm.addTextChangedListener(pwWatcher)
    }

    private fun validatePasswordMatch() {
        val pw = binding.etPw.text.toString()
        val confirm = binding.etPwConfirm.text.toString()

        if (pw.isNotEmpty() && confirm.isNotEmpty()) {
            binding.tvPwConfirmMessage.visibility = View.VISIBLE
            if (pw == confirm) {
                binding.tvPwConfirmMessage.text = "비밀번호가 일치합니다."
                binding.tvPwConfirmMessage.setTextColor(Color.BLUE)

                if (binding.layoutStepEmail.visibility == View.GONE) {
                    binding.layoutStepEmail.visibility = View.VISIBLE
                    binding.etEmail.requestFocus()
                }
            } else {
                binding.tvPwConfirmMessage.text = "비밀번호가 일치하지 않습니다."
                binding.tvPwConfirmMessage.setTextColor(Color.RED)
            }
        } else {
            binding.tvPwConfirmMessage.visibility = View.GONE
        }
    }

    // [STEP 4 & 5] 이메일 인증
    private fun setupStep4Email() {
        binding.btnSendCode.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                return@setOnClickListener
            }
            sendEmailCode(email)
        }

        binding.btnVerifyCode.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val code = binding.etEmailCode.text.toString().trim()
            if (email.isEmpty() || code.isEmpty()) {
                return@setOnClickListener
            }
            verifyEmailCode(email, code)
        }
    }

    private fun sendEmailCode(email: String) {
        binding.btnSendCode.isEnabled = false
        binding.btnSendCode.text = "전송중"

        hideKeyboard()

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.authApiService.sendEmailCode(email)
                if (response.isSuccessful) {
                    binding.tvEmailMessage.text = "인증번호가 발송되었습니다."
                    binding.tvEmailMessage.setTextColor(Color.BLUE)
                    binding.tvEmailMessage.visibility = View.VISIBLE

                    binding.layoutStepVerify.visibility = View.VISIBLE
                    binding.etEmailCode.requestFocus()

                    startTimer(180 * 1000L)
                } else {
                    binding.tvEmailMessage.text = "발송 실패"
                    binding.tvEmailMessage.setTextColor(Color.RED)
                    binding.tvEmailMessage.visibility = View.VISIBLE
                    binding.btnSendCode.isEnabled = true
                    binding.btnSendCode.text = "발송"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                binding.btnSendCode.isEnabled = true
                binding.btnSendCode.text = "발송"
            }
        }
    }

    private fun verifyEmailCode(email: String, code: String) {
        binding.btnVerifyCode.isEnabled = false
        binding.btnVerifyCode.text = "확인중"

        hideKeyboard()

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.authApiService.verifyEmailCode(email, code)
                if (response.isSuccessful) {
                    binding.tvVerificationMessage.text = "인증 성공"
                    binding.tvVerificationMessage.setTextColor(Color.BLUE)
                    binding.tvVerificationMessage.visibility = View.VISIBLE

                    isEmailVerified = true
                    timer?.cancel()
                    binding.btnSendCode.text = "완료"

                    binding.btnSignUp.visibility = View.VISIBLE

                } else {
                    binding.tvVerificationMessage.text = "인증 실패"
                    binding.tvVerificationMessage.setTextColor(Color.RED)
                    binding.tvVerificationMessage.visibility = View.VISIBLE
                    binding.btnVerifyCode.isEnabled = true
                    binding.btnVerifyCode.text = "확인"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                binding.btnVerifyCode.isEnabled = true
                binding.btnVerifyCode.text = "확인"
            }
        }
    }

    // [STEP 6] 회원가입 요청 (이름/전화번호 제외)
    private fun setupStep6SignUp() {
        binding.btnSignUp.setOnClickListener {
            if (!isIdChecked) { return@setOnClickListener }
            if (!isEmailVerified) {return@setOnClickListener }

            val userid = binding.etId.text.toString().trim()
            val pw = binding.etPw.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()

            // 🔥 [수정] 이름과 전화번호는 더 이상 받지 않으므로 빈 값으로 전송
            val name = ""
            val phone = ""

            binding.btnSignUp.isEnabled = false

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.authApiService.registerMember(userid, pw, name, email, phone)
                    if (response.isSuccessful) {
                        val authResponse = response.body()
                        if (authResponse?.userId != null) {
                            AuthManager.setLoggedIn(this@SignUpActivity, authResponse.userId, true)
                            val intent = Intent(this@SignUpActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        showToast("가입 실패: ${response.errorBody()?.string()}")
                        binding.btnSignUp.isEnabled = true
                    }
                } catch (e: Exception) {
                    showToast("오류: ${e.message}")
                    binding.btnSignUp.isEnabled = true
                }
            }
        }
    }

    private fun startTimer(millisInFuture: Long) {
        timer?.cancel()
        timer = object : CountDownTimer(millisInFuture, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val min = millisUntilFinished / 1000 / 60
                val sec = millisUntilFinished / 1000 % 60
                binding.btnSendCode.text = String.format("%02d:%02d", min, sec)
                binding.btnSendCode.isEnabled = false
            }
            override fun onFinish() {
                binding.btnSendCode.text = "재전송"
                binding.btnSendCode.isEnabled = true
            }
        }.start()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}