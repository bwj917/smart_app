package com.example.demo.controller;

import com.example.demo.domain.Member;
import com.example.demo.dto.AuthSuccessResponse;
import com.example.demo.dto.LoginForm;
import com.example.demo.dto.RegisterForm;
import com.example.demo.dto.VerifyCodeRequest;
import com.example.demo.service.EmailAuthService;
import com.example.demo.service.EmailService;
import com.example.demo.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final EmailService emailService;
    private final EmailAuthService emailAuthService;

    // ---------------------- 1. Home / View ----------------------

    @GetMapping("/")
    public String home(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("loginMember") == null) {
            return "Login";
        }
        Member loginMember = (Member) session.getAttribute("loginMember");
        model.addAttribute("userid", loginMember.getUserid());
        return "/members/LoginSuccess";
    }

    @GetMapping(value = "/register")
    public String createForm() {
        return "/members/register";
    }

    // [ID/PW 찾기 페이지]
    @GetMapping(value = "/find-id")
    public String findIdForm() {
        return "FindId";
    }

    @GetMapping(value = "/find-password")
    public String findPasswordForm() {
        return "FindPassword";
    }


    // ---------------------- 2. Member API ----------------------

    // [1] ID 중복 확인 API (GET 요청, @RequestParam)
    @GetMapping("/api/members/check-id")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkLoginIdDuplicate(@RequestParam("loginId") String loginId) {
        boolean isAvailable = memberService.findOne(loginId).isEmpty();
        Map<String, Boolean> response = Collections.singletonMap("isAvailable", isAvailable);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/api/login")
    @ResponseBody
    // 🔥 수정: 성공 시 AuthSuccessResponse DTO를 반환하도록 변경
    public ResponseEntity<?> loginForApp(@ModelAttribute LoginForm loginform, HttpServletRequest request) {
        Optional<Member> loginMemberOptional = memberService.Login(loginform.getUserid(), loginform.getPw());

        if (loginMemberOptional.isEmpty()) {
            // 실패 시 400 Bad Request와 함께 텍스트 에러 메시지를 반환
            return ResponseEntity.badRequest().body("아이디 또는 비밀번호를 확인하세요.");
        }

        Member loginMember = loginMemberOptional.get();
        HttpSession session = request.getSession();
        session.setAttribute("loginMember", loginMember);

        // 🔥 수정: 성공 시 유저 ID를 포함한 JSON 응답을 반환
        AuthSuccessResponse response = new AuthSuccessResponse(loginMember.getId(), "로그인 성공");
        return ResponseEntity.ok(response);
    }

    // [3] 회원가입 처리 API (POST 요청, @ModelAttribute)
    // 🔥 수정: @Controller에서 @ResponseBody를 추가하여 JSON API로 변경
    @PostMapping(value = "/register-process")
    @ResponseBody
    public ResponseEntity<?> create(@ModelAttribute RegisterForm registerform) {
        Member member = new Member();
        member.setUserid(registerform.getUserid());
        member.setPw(registerform.getPw());
        member.setName(registerform.getName());
        member.setEmail(registerform.getEmail());
        member.setPhone(registerform.getPhone());

        try {
            Long memberId = memberService.join(member);

            // 🔥 수정: 성공 시 유저 ID를 포함한 JSON 응답을 반환
            AuthSuccessResponse response = new AuthSuccessResponse(memberId, "회원가입 및 로그인 성공");
            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            // 🔥 중복 예외 발생 시 409 Conflict 상태 코드와 메시지를 텍스트로 반환
            // 클라이언트가 JSON 파싱 오류(MalformedJsonException)를 일으키지 않도록 텍스트로 반환합니다.
            log.error("회원가입 실패", e);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(e.getMessage());
        }
    }

    // ---------------------- 3. Email/Find API ----------------------

    // [4] 인증 코드 발송 API (POST 요청, @RequestParam)
    @PostMapping("/api/email/send-code")
    @ResponseBody
    public ResponseEntity<String> sendVerificationCode(@RequestParam("email") String email) {
        try {
            String code = emailService.createVerificationCode();
            emailAuthService.saveCode(email, code);
            emailService.sendEmail(email, code);

            return ResponseEntity.ok("인증번호가 발송되었습니다. 3분 이내에 입력해주세요.");

        } catch (Exception e) {
            log.error("인증 코드 발송 실패. Email: {}", email, e);
            return ResponseEntity.status(500).body("이메일 발송 중 오류가 발생했습니다.");
        }
    }

    // [5] 인증 코드 검증 API (POST 요청, @ModelAttribute)
    @PostMapping("/api/email/verify-code")
    @ResponseBody
    public ResponseEntity<String> verifyCode(@ModelAttribute VerifyCodeRequest request) {
        String email = request.getEmail();
        String code = request.getVerificationCode();
        try {
            if(emailAuthService.verifyCode(email, code)) {
                emailAuthService.deleteCode(email);
                return ResponseEntity.ok("인증완료.");
            }
            else
                return ResponseEntity.badRequest().body("인증코드가 맞지 않습니다.");
        } catch (Exception e) {
            log.error("인증 실패. Email: {}", email, e);
            return ResponseEntity.status(500).body("인증 중 오류가 발생했습니다.");
        }
    }

    // [6] ID 찾기 API (POST 요청, @RequestParam)
    @PostMapping(value = "/api/find-id")
    @ResponseBody
    public ResponseEntity<?> findId(@RequestParam("email") String email) {
        Optional<Member> memberOptional = memberService.findByEmail(email);

        if (memberOptional.isPresent()) {
            String userId = memberOptional.get().getUserid();
            return ResponseEntity.ok(userId);
        } else {
            return ResponseEntity.badRequest().body("일치하는 회원 정보를 찾을 수 없습니다.");
        }
    }
}