package com.example.demo.repository;

import com.example.demo.domain.UserProblemStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface UserProblemStatsRepository extends JpaRepository<UserProblemStats, Long> {

    Optional<UserProblemStats> findByUserIdAndProblem_ProblemId(Long userId, Long problemId);

    // 1. 복습해야 할 문제들 (복습 시간이 현재 시간보다 과거인 것)
    @Query("SELECT s FROM UserProblemStats s WHERE s.userId = :userId AND s.problem.courseId = :courseId " +
            "AND s.nextReviewTime IS NOT NULL AND s.nextReviewTime <= :nowTime")
    List<UserProblemStats> findReviewProblems(@Param("userId") Long userId,
                                              @Param("courseId") String courseId,
                                              @Param("nowTime") Date nowTime);

    // 2. 나중에 복습할 문제들 (아직 복습 시간이 안 된 것)
    @Query("SELECT s FROM UserProblemStats s WHERE s.userId = :userId AND s.problem.courseId = :courseId " +
            "AND s.nextReviewTime IS NOT NULL AND s.nextReviewTime > :nowTime")
    List<UserProblemStats> findFutureProblems(@Param("userId") Long userId,
                                              @Param("courseId") String courseId,
                                              @Param("nowTime") Date nowTime);

    // 3. 이미 풀어본 문제들의 ID 목록 (새 문제 출제 시 제외용)
    @Query("SELECT s.problem.problemId FROM UserProblemStats s WHERE s.userId = :userId AND s.problem.courseId = :courseId")
    List<Long> findSolvedProblemIds(@Param("userId") Long userId, @Param("courseId") String courseId);
}