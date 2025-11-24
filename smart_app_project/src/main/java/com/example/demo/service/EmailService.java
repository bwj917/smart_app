package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    /**
     * 6자리 숫자 인증번호를 생성합니다.
     */
    public String createVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * 지정된 이메일 주소로 인증번호를 발송합니다.
     */
    public void sendEmail(String toEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setTo(toEmail);
            message.setSubject("큐잇 회원가입 이메일 인증번호");

            // 메일 본문
            String text = "회원가입을 위한 인증번호입니다.\n";
            text += "인증번호: " + code + "\n";
            text += "3분 이내에 입력해주세요.";
            message.setText(text);

            // 발신자 설정이 application.properties에 없다면 여기에 추가할 수 있습니다.
            // message.setFrom("sandrock0429@gmail.com");

            javaMailSender.send(message);

        } catch (Exception e) {
            log.error("메일 발송 실패. To: {}", toEmail, e);
            throw new RuntimeException("메일 발송에 실패했습니다. SMTP 설정을 확인하세요.", e);
        }
    }
}