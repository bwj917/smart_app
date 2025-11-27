package com.example.demo.repository;

import com.example.demo.domain.UserProblemStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
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
            "WHERE s.userId = :userId AND p.courseId = :courseId " +
            "AND (s.nextReviewTime < :now OR s.nextReviewTime IS NULL) " +
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

    // 조건: 최소 2번 이상 시도한 문제 중, 많이 푼 순서대로 정렬
    @Query("SELECT s FROM UserProblemStats s JOIN FETCH s.problem p " +
            "WHERE s.userId = :userId AND s.totalAttempts >= 2 " +
            "ORDER BY s.totalAttempts DESC")
    List<UserProblemStats> findFrequentWrongProblems(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT s FROM UserProblemStats s JOIN FETCH s.problem p " +
            "WHERE s.userId = :userId AND p.courseId = :courseId AND s.totalAttempts >= 2 " +
            "ORDER BY s.totalAttempts DESC")
    List<UserProblemStats> findFrequentWrongProblemsByCourse(@Param("userId") Long userId,
                                                             @Param("courseId") String courseId,
                                                             Pageable pageable);

    @Query("SELECT s FROM UserProblemStats s JOIN FETCH s.problem p " +
            "WHERE s.userId = :userId AND s.isScrapped = true " +
            "ORDER BY s.id DESC")
    List<UserProblemStats> findScrappedProblems(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT s FROM UserProblemStats s JOIN FETCH s.problem p " +
            "WHERE s.userId = :userId AND p.courseId = :courseId AND s.isScrapped = true " +
            "ORDER BY s.id DESC")
    List<UserProblemStats> findScrappedProblemsByCourse(@Param("userId") Long userId,
                                                        @Param("courseId") String courseId,
                                                        Pageable pageable);
}