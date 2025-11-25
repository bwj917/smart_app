package com.example.demo.repository;

import com.example.demo.domain.Member;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository {
    void save(Member member);
    Optional<Member> findByUserId(String userid);
    Optional<Member> findById(long id);
    Optional<Member> findByUserName(String name);
    Optional<Member> findByEmail(String email);
    List<Member> findAll();

    void updatePoints(Long id, int newPoints);
    void updateMemberAfterPurchase(Long id, int newPoints, String newOwnedList);

    // 🔥 [신규 추가] 장착 캐릭터 변경 메서드 정의
    void updateEquippedCharacter(Long id, int characterIdx);
}