package com.example.demo.repository;

import com.example.demo.domain.SubmissionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Date;
import java.util.List;

public interface SubmissionHistoryRepository extends JpaRepository<SubmissionHistory, Long> {

    // 1. [홈 화면용] 과목별 오늘 '정답' 문제 수 카운트
    @Query("SELECT COUNT(h) FROM SubmissionHistory h JOIN Problem p ON h.problemId = p.problemId " +
            "WHERE h.userId = :userId AND p.courseId = :courseId AND h.submittedAt BETWEEN :start AND :end " +
            "AND h.isCorrect = true")
    int countTodayByCourse(@Param("userId") Long userId,
                           @Param("courseId") String courseId,
                           @Param("start") Date start,
                           @Param("end") Date end);

    // 🔥 [신규 추가] 과목 상관없이 오늘 '정답' 전체 개수 카운트
    @Query("SELECT COUNT(h) FROM SubmissionHistory h " +
            "WHERE h.userId = :userId AND h.submittedAt BETWEEN :start AND :end AND h.isCorrect = true")
    int countTodayTotal(@Param("userId") Long userId,
                        @Param("start") Date start,
                        @Param("end") Date end);

    // 2. [통계 화면용] 특정 기간의 기록 조회
    List<SubmissionHistory> findAllByUserIdAndSubmittedAtBetween(Long userId, Date start, Date end);

    // 3. [통계 화면용] 유저의 '모든' 기록 조회
    List<SubmissionHistory> findByUserId(Long userId);

    // 4. [누적 헤더용] 총 학습 시간 (오답 포함)
    @Query("SELECT COALESCE(SUM(h.studyTime), 0) FROM SubmissionHistory h WHERE h.userId = :userId")
    Long getTotalStudyTime(@Param("userId") Long userId);

    // 5. [기간별 시간용] 기간별 학습 시간 (오답 포함)
    @Query("SELECT COALESCE(SUM(h.studyTime), 0) FROM SubmissionHistory h " +
            "WHERE h.userId = :userId AND h.submittedAt BETWEEN :start AND :end")
    Long getSumStudyTimeBetween(@Param("userId") Long userId,
                                @Param("start") Date start,
                                @Param("end") Date end);
}