package com.example.demo.repository;

import com.example.demo.domain.SubmissionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface SubmissionHistoryRepository extends JpaRepository<SubmissionHistory, Long> {

    // 특정 기간 동안의 기록 조회
    List<SubmissionHistory> findAllByUserIdAndSubmittedAtBetween(Long userId, Date start, Date end);

    // 전체 기록 조회
    List<SubmissionHistory> findByUserId(Long userId);

    // 🔥 [신규] 특정 기간 동안 푼 문제 중 '정답'인 개수 (과목 무관)
    @Query("SELECT COUNT(h) FROM SubmissionHistory h WHERE h.userId = :userId AND h.isCorrect = true AND h.submittedAt BETWEEN :start AND :end")
    int countCorrectByUserIdAndSubmittedAtBetween(@Param("userId") Long userId, @Param("start") Date start, @Param("end") Date end);

    // 🔥 [신규] 특정 기간 동안 학습한 '총 시간(초)' 합계
    @Query("SELECT SUM(h.studyTime) FROM SubmissionHistory h WHERE h.userId = :userId AND h.submittedAt BETWEEN :start AND :end")
    Long getSumStudyTimeBetween(@Param("userId") Long userId, @Param("start") Date start, @Param("end") Date end);

    // 🔥 [신규] 사용자의 '역대 총 학습 시간' 합계
    @Query("SELECT SUM(h.studyTime) FROM SubmissionHistory h WHERE h.userId = :userId")
    Long getTotalStudyTime(@Param("userId") Long userId);

    // 🔥 [신규] 오늘 특정 과목에서 푼(정답인) 문제 수 (조인 쿼리)
    // SubmissionHistory에는 courseId가 없으므로 Problem 테이블과 조인하여 확인
    @Query("SELECT COUNT(h) FROM SubmissionHistory h JOIN Problem p ON h.problemId = p.problemId " +
            "WHERE h.userId = :userId AND p.courseId = :courseId AND h.isCorrect = true " +
            "AND h.submittedAt BETWEEN :start AND :end")
    int countTodayByCourse(@Param("userId") Long userId,
                           @Param("courseId") String courseId,
                           @Param("start") Date start,
                           @Param("end") Date end);
}