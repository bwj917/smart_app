package com.example.demo.repository;

import com.example.demo.domain.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProblemRepository extends JpaRepository<Problem, Long> {

    // 해당 과목의 모든 문제 조회
    List<Problem> findByCourseId(String courseId);

    // 🔥 [필수 추가] 이미 푼 문제(solvedIds)를 제외한 나머지 문제들을 조회하는 쿼리
    @Query("SELECT p FROM Problem p WHERE p.courseId = :courseId AND p.problemId NOT IN :solvedIds")
    List<Problem> findNewProblems(@Param("courseId") String courseId, @Param("solvedIds") List<Long> solvedIds);
}