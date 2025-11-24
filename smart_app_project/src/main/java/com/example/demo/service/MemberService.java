package com.example.demo.service;

import com.example.demo.domain.Member;
import com.example.demo.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Long join(Member member) {
        String encodedPassword = passwordEncoder.encode(member.getPw());
        member.setPw(encodedPassword);
        validateDuplicateMember(member);
        memberRepository.save(member);
        return member.getId();
    }

    private void validateDuplicateMember(Member member) {
// 1. ID 중복 확인 (기존 로직)
        memberRepository.findByUserId(member.getUserid())
                .ifPresent(m -> {
                    throw new IllegalStateException("이미 사용 중인 아이디입니다.");
                });

        // 2. ⭐️ 이메일 중복 확인 추가 ⭐️
        memberRepository.findByEmail(member.getEmail())
                .ifPresent(m -> {
                    // 이메일 중복은 회원가입 시 반드시 확인해야 합니다.
                    throw new IllegalStateException("이미 사용 중인 이메일입니다.");
                });
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public Optional<Member> findOne(String userId) {
        return memberRepository.findByUserId(userId);

    }

    // ID 찾기 기능을 위해 추가
    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    public Optional<Member> Login(String userId, String password) {
        Optional<Member> memberOptional = memberRepository.findByUserId(userId);

        return memberOptional.filter(member ->
                passwordEncoder.matches(password, member.getPw())
        );
    }
}