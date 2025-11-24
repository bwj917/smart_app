package com.example.demo.repository;

import com.example.demo.domain.UserProblemStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface UserProblemStatsRepository extends JpaRepository<UserProblemStats, Long> {

    // ... (기존 findBy 메서드들 유지) ...

    Optional<UserProblemStats> findByUserIdAndProblem_ProblemId(Long userId, Long problemId);
    List<UserProblemStats> findByUserIdAndNextReviewTimeBefore(Long userId, Date now);
    List<UserProblemStats> findByUserIdAndNextReviewTimeAfter(Long userId, Date now);
    List<UserProblemStats> findByUserId(Long userId);

    @Query("SELECT s FROM UserProblemStats s JOIN s.problem p " +
            "WHERE s.userId = :userId AND p.courseId = :courseId AND s.nextReviewTime < :now " +
            "ORDER BY s.nextReviewTime DESC")
    List<UserProblemStats> findReviewProblems(@Param("userId") Long userId,
                                              @Param("courseId") String courseId,
                                              @Param("now") Date now);

    @Query("SELECT s FROM UserProblemStats s JOIN s.problem p " +
            "WHERE s.userId = :userId AND p.courseId = :courseId AND s.nextReviewTime > :now " +
            "ORDER BY s.nextReviewTime ASC")
    List<UserProblemStats> findFutureProblems(@Param("userId") Long userId,
                                              @Param("courseId") String courseId,
                                              @Param("now") Date now);

    @Query("SELECT s.problem.problemId FROM UserProblemStats s JOIN s.problem p " +
            "WHERE s.userId = :userId AND p.courseId = :courseId")
    List<Long> findSolvedProblemIds(@Param("userId") Long userId, @Param("courseId") String courseId);

}