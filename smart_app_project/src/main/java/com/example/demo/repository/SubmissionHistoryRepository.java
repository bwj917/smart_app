package com.example.demo.repository;

import com.example.demo.domain.SubmissionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Date;
import java.util.List;

public interface SubmissionHistoryRepository extends JpaRepository<SubmissionHistory, Long> {

    // 1. [홈 화면용] 오늘 전체 문제 수 카운트 (Correct submissions for a period)
    @Query("SELECT COUNT(h) FROM SubmissionHistory h " +
            "WHERE h.userId = :userId AND h.submittedAt BETWEEN :start AND :end AND h.isCorrect = true") // 🔥 정답 필터 추가
    int countCorrectByUserIdAndSubmittedAtBetween(@Param("userId") Long userId,
                                                  @Param("start") Date start,
                                                  @Param("end") Date end);

    // 2. [홈 화면용] 과목별 오늘 문제 수 카운트 (Correct submissions for a course)
    // NOTE: JOIN Problem p ON h.problemId = p.problemId 쿼리 형태로 구현해야 합니다.
    @Query("SELECT COUNT(h) FROM SubmissionHistory h JOIN Problem p ON h.problemId = p.problemId " +
            "WHERE h.userId = :userId AND p.courseId = :courseId AND h.submittedAt BETWEEN :start AND :end " +
            "AND h.isCorrect = true") // 🔥 정답 필터 추가
    int countTodayByCourse(@Param("userId") Long userId,
                           @Param("courseId") String courseId,
                           @Param("start") Date start,
                           @Param("end") Date end);

    // 3. [통계 화면용] 특정 기간(주간, 월간, 연간)의 기록 조회 (List for chart)
    // NOTE: 서비스에서 List를 받아 내부적으로 필터링하므로 이 함수는 그대로 유지
    List<SubmissionHistory> findAllByUserIdAndSubmittedAtBetween(Long userId, Date start, Date end);

    // 4. [통계 화면용] 유저의 '모든' 기록 가져오기 (List for chart)
    List<SubmissionHistory> findByUserId(Long userId);

    // 5. [누적 헤더용] 총 학습 시간 (Correct submissions only)
    @Query("SELECT COALESCE(SUM(h.studyTime), 0) FROM SubmissionHistory h WHERE h.userId = :userId AND h.isCorrect = true") // 🔥 정답 필터 추가
    Long getTotalStudyTime(@Param("userId") Long userId);

    // 6. [기간별 시간용] 총 학습 시간 (Correct submissions only)
    @Query("SELECT COALESCE(SUM(h.studyTime), 0) FROM SubmissionHistory h " +
            "WHERE h.userId = :userId AND h.submittedAt BETWEEN :start AND :end AND h.isCorrect = true") // 🔥 정답 필터 추가
    Long getSumStudyTimeBetween(@Param("userId") Long userId,
                                @Param("start") Date start,
                                @Param("end") Date end);
}