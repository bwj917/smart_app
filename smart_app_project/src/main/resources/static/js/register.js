// --- (기존) 아이디 중복 검사 로직 ---
const loginIdInput = document.getElementById('loginId');
loginIdInput.addEventListener('blur', async function() {
    // ... (이전과 동일한 아이디 중복 검사 코드) ...
    const loginId = this.value;
    const messageDiv = document.getElementById('idCheckMessage');
    if (!loginId) {
        messageDiv.textContent = '';
        return;
    }
    try {
        const response = await fetch(`/api/members/check-id?loginId=${loginId}`);
        const data = await response.json();
        if (data.isAvailable) {
            messageDiv.textContent = '사용 가능한 아이디입니다.';
            messageDiv.className = 'message success';
        } else {
            messageDiv.textContent = '이미 사용 중인 아이디입니다.';
            messageDiv.className = 'message error';
        }
    } catch (error) {
        console.error('ID 중복 확인 중 오류 발생:', error);
        messageDiv.textContent = '오류가 발생했습니다. 다시 시도해주세요.';
        messageDiv.className = 'message error';
    }
});

// --- (기존) 비밀번호 일치 확인 로직 ---
const passwordInput = document.getElementById('password');
const passwordConfirmInput = document.getElementById('passwordConfirm');
const passwordConfirmMessageDiv = document.getElementById('passwordConfirmMessage');

function validatePassword() {
    // ... (이전과 동일한 비밀번호 일치 확인 코드) ...
    const password = passwordInput.value;
    const confirmPassword = passwordConfirmInput.value;
    if (confirmPassword) {
        if (password === confirmPassword) {
            passwordConfirmMessageDiv.textContent = '비밀번호가 일치합니다.';
            passwordConfirmMessageDiv.className = 'message success';
        } else {
            passwordConfirmMessageDiv.textContent = '비밀번호가 일치하지 않습니다.';
            passwordConfirmMessageDiv.className = 'message error';
        }
    } else {
        passwordConfirmMessageDiv.textContent = '';
    }
}
passwordInput.addEventListener('keyup', validatePassword);
passwordConfirmInput.addEventListener('keyup', validatePassword);

// 1. 필요한 HTML 요소 모두 가져오기
const emailInput = document.getElementById('email');
const btnSendCode = document.getElementById('btnSendCode'); // "인증번호 발송" 버튼
const emailMessageDiv = document.getElementById('emailMessage');
const verificationCodeGroup = document.getElementById('verificationCodeGroup'); // 인증번호 입력 그룹

const verificationCodeInput = document.getElementById('verificationCode');
const btnEmailResend = document.getElementById('btnEmailResend'); // "인증번호 재전송" 버튼
const btnVerifyCode = document.getElementById('btnVerifyCode');     // "인증번호 확인" 버튼
const verificationMessageDiv = document.getElementById('verificationMessage');

const btnSignup = document.getElementById('btnSignup'); // "가입하기" 버튼

// 2. [공통 함수] 인증번호 발송/재전송 처리
async function handleSendCode(buttonElement) {
    const email = emailInput.value;

    // 2-1. 이메일 입력 유효성 검사
    if (!email) {
        emailMessageDiv.textContent = '이메일을 먼저 입력해주세요.';
        emailMessageDiv.className = 'message error';
        return;
    }

    // 2-2. 버튼 비활성화 및 메시지 초기화
    buttonElement.disabled = true;
    buttonElement.textContent = '발송 중...';
    emailMessageDiv.textContent = '';
    btnSignup.disabled = true; // (재)인증 시도 시, 회원가입 버튼을 다시 비활성화

    try {
        // 2-3. EmailController의 API 호출 (fetch 사용)
        const response = await fetch('/api/email/send-code', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: new URLSearchParams({
                'email': email
            })
        });

        // 2-4. 응답 처리
        if (response.ok) {
            emailMessageDiv.textContent = await response.text();
            emailMessageDiv.className = 'message success';

            // 인증번호 입력칸을 보여줌
            verificationCodeGroup.style.display = 'block';

            // 3분 타이머 시작 (클릭한 버튼을 기준으로)
            startTimer(180, buttonElement);

        } else {
            // 실패 시
            emailMessageDiv.textContent = await response.text();
            emailMessageDiv.className = 'message error';
            buttonElement.disabled = false; // 버튼 다시 활성화
            // 버튼 원래 텍스트로 복원
            buttonElement.textContent = (buttonElement === btnSendCode) ? '인증번호 발송' : '인증번호 재전송';
        }

    } catch (error) {
        // fetch 자체의 네트워크 오류 등
        console.error('인증번호 발송 요청 오류:', error);
        emailMessageDiv.textContent = '요청 중 오류가 발생했습니다. 네트워크를 확인해주세요.';
        emailMessageDiv.className = 'message error';
        buttonElement.disabled = false; // 버튼 다시 활성화
        buttonElement.textContent = (buttonElement === btnSendCode) ? '인증번호 발송' : '인증번호 재전송';
    }
}

// 3. "인증번호 발송" 버튼 클릭 이벤트
btnSendCode.addEventListener('click', function() {
    handleSendCode(btnSendCode);
});

// 4. "인증번호 재전송" 버튼 클릭 이벤트
btnEmailResend.addEventListener('click', function() {
    handleSendCode(btnEmailResend);
});


// 5. 3분 타이머 함수
function startTimer(duration, displayElement) {
    let timer = duration, minutes, seconds;
    displayElement.disabled = true; // 타이머 동안 버튼 비활성화

    // 버튼의 원래 텍스트 저장
    const originalText = (displayElement === btnSendCode) ? '인증번호 발송' : '인증번호 재전송';

    const interval = setInterval(function () {
        minutes = parseInt(timer / 60, 10);
        seconds = parseInt(timer % 60, 10);

        minutes = minutes < 10 ? "0" + minutes : minutes;
        seconds = seconds < 10 ? "0" + seconds : seconds;

        displayElement.textContent = `재전송 (${minutes}:${seconds})`;

        if (--timer < 0) {
            clearInterval(interval);
            displayElement.textContent = originalText; // 원래 텍스트로 복구
            displayElement.disabled = false;
            emailMessageDiv.textContent = '인증 시간이 만료되었습니다. 다시 시도해주세요.';
            emailMessageDiv.className = 'message error';
        }
    }, 1000);
}

// 6. "인증번호 확인" 버튼 클릭 이벤트
btnVerifyCode.addEventListener('click', async function() {
    const email = emailInput.value;
    const code = verificationCodeInput.value;

    // 6-1. 유효성 검사
    if (!email || !code) {
        verificationMessageDiv.textContent = '이메일과 인증번호를 모두 입력해주세요.';
        verificationMessageDiv.className = 'message error';
        return;
    }

    // 6-2. 버튼 비활성화
    btnVerifyCode.disabled = true;
    btnVerifyCode.textContent = '확인 중...';

    try {
        // 6-3. [수정됨] EmailController의 검증 API 호출
        const response = await fetch('/api/email/verify-code', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: new URLSearchParams({
                'email': email,
                // ⭐️ 오류 지점 ⭐️
                'verificationCode': code            })
        });

        // 6-4. 응답 처리
        if (response.ok) {
            // ★★★ 인증 성공 ★★★
            verificationMessageDiv.textContent = await response.text();
            verificationMessageDiv.className = 'message success';

            // 인증 성공 시, 더 이상 수정 못하게 막기
            emailInput.readOnly = true;
            verificationCodeInput.readOnly = true;
            btnSendCode.disabled = true;
            btnVerifyCode.disabled = true;
            btnEmailResend.disabled = true; // 재전송 버튼도 비활성화

            // ★★★ 인증 성공 시, 회원가입 버튼 활성화 ★★★
            btnSignup.disabled = false;

        } else {
            // ★★★ 인증 실패 ★★★
            verificationMessageDiv.textContent = await response.text();
            verificationMessageDiv.className = 'message error';
            btnVerifyCode.disabled = false; // 확인 버튼 다시 활성화
            btnVerifyCode.textContent = '인증번호 확인';
            // 실패 시, 회원가입 버튼은 계속 비활성화 상태 유지
            btnSignup.disabled = true;
        }

    } catch (error) {
        // fetch 자체의 네트워크 오류 등
        console.error('인증번호 확인 요청 오류:', error);
        verificationMessageDiv.textContent = '요청 중 오류가 발생했습니다. 네트워크를 확인해주세요.';
        verificationMessageDiv.className = 'message error';
        btnVerifyCode.disabled = false; // 확인 버튼 다시 활성화
        btnVerifyCode.textContent = '인증번호 확인';
        // 오류 시, 회원가입 버튼은 계속 비활성화 상태 유지
        btnSignup.disabled = true;
    }
});