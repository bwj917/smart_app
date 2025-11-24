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
    Optional<Member> findByEmail(String email); // ⭐️ 이메일로 회원을 찾는 메서드 추가
    List<Member> findAll();

    void updatePoints(Long id, int newPoints);
}