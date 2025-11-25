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
        memberRepository.findByUserId(member.getUserid())
                .ifPresent(m -> {
                    throw new IllegalStateException("이미 사용 중인 아이디입니다.");
                });

        memberRepository.findByEmail(member.getEmail())
                .ifPresent(m -> {
                    throw new IllegalStateException("이미 사용 중인 이메일입니다.");
                });
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public Optional<Member> findOne(String userId) {
        return memberRepository.findByUserId(userId);
    }

    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    public Optional<Member> Login(String userId, String password) {
        Optional<Member> memberOptional = memberRepository.findByUserId(userId);
        return memberOptional.filter(member ->
                passwordEncoder.matches(password, member.getPw())
        );
    }

    public Optional<Member> findOneById(Long userId) {
        return memberRepository.findById(userId);
    }

    public int addPoints(Long userId, int amount) {
        Optional<Member> memberOptional = memberRepository.findById(userId);
        if (memberOptional.isPresent()) {
            Member member = memberOptional.get();
            int updatedPoints = member.getPoints() + amount;
            memberRepository.updatePoints(userId, updatedPoints);
            return updatedPoints;
        }
        return -1;
    }

    public void updatePurchase(Long userId, int newPoints, String newOwnedList) {
        memberRepository.updateMemberAfterPurchase(userId, newPoints, newOwnedList);
    }

    // 🔥 [신규 추가] 장착 캐릭터 업데이트 연결
    public void updateEquippedCharacter(Long userId, int characterIdx) {
        memberRepository.updateEquippedCharacter(userId, characterIdx);
    }
}