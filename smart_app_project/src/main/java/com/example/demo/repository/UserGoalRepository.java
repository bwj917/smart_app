package com.example.demo.repository;

import com.example.demo.domain.UserGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserGoalRepository extends JpaRepository<UserGoal, Long> {
    // 특정 유저의 특정 과목 목표 찾기
    Optional<UserGoal> findByUserIdAndCourseName(Long userId, String courseName);

    List<UserGoal> findByUserId(Long userId);
}