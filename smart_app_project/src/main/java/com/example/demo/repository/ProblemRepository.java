package com.example.demo.repository;

import com.example.demo.domain.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProblemRepository extends JpaRepository<Problem, Long> {

    // 🔥 [수정] 특정 과목(courseId)이면서 + 이미 푼 문제(solvedIds)가 아닌 것 조회
    @Query("SELECT p FROM Problem p WHERE p.courseId = :courseId AND p.problemId NOT IN :solvedIds")
    List<Problem> findNewProblems(@Param("courseId") String courseId, @Param("solvedIds") List<Long> solvedIds);

    // (푼 문제가 하나도 없을 때) 특정 과목의 전체 문제 조회
    List<Problem> findByCourseId(String courseId);
}