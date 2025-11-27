package com.example.demo.domain;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "USER_PROBLEM_STATS",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_USER_PROBLEM",
                        columnNames = {"USER_ID", "PROBLEM_ID"}
                )
        })
public class UserProblemStats {

    // 1. 기본 키 (Primary Key)
    // 개별 통계 기록 자체의 고유 ID입니다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "STATS_ID")
    private Long id;

    // 2. 외래 키 (Foreign Key) - Problem
    // 이 통계가 어떤 문제에 대한 것인지 참조합니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROBLEM_ID", nullable = false)
    private Problem problem;

    // 3. 사용자 식별자 (User Identifier)
    // 이 통계가 어떤 사용자의 것인지 식별합니다. (User 엔티티를 직접 참조하지 않고 ID만 저장한다고 가정)
    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    // 4. 복습 시스템 관련 필드
    @Column(name = "PROBLEM_LEVEL")
    private Integer problemLevel = 0; // 문제의 현재 숙련도 레벨 (0부터 시작)

    @Column(name = "NEXT_REVIEW_TIME")
    private Date nextReviewTime; // 다음 복습이 필요한 시각

    // 5. 누적 통계 필드
    @Column(name = "TOTAL_ATTEMPTS")
    private Integer totalAttempts = 0; // 총 시도 횟수

    @Column(name = "CORRECT_ATTEMPTS")
    private Integer correctAttempts = 0; // 정답을 맞힌 횟수

    @Column(name = "HINT_USED_COUNT")
    private Integer hintUsedCount = 0; // 총 힌트 사용 횟수

    @Column(name = "LAST_SOLVED_AT")
    private Date lastSolvedAt; // 마지막으로 정답을 맞힌 시간



    @Column(name = "IS_SCRAPPED")
    private Boolean isScrapped = false;

    // --- 생성자 ---
    public UserProblemStats() {}

    // --- Getter & Setter ---
    // Lombok을 사용하지 않는 경우, 모든 필드에 대한 Getter와 Setter가 필요합니다.
    // 여기서는 가독성을 위해 일부만 작성합니다. (나머지 필드는 필요에 따라 추가하세요)

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Problem getProblem() {
        return problem;
    }

    public void setProblem(Problem problem) {
        this.problem = problem;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    // ... (problemLevel, nextReviewTime, totalAttempts 등 나머지 필드의 Getter/Setter 추가)
    public Integer getProblemLevel() {
        return problemLevel;
    }

    public void setProblemLevel(Integer problemLevel) {
        this.problemLevel = problemLevel;
    }

    public Date getNextReviewTime() {
        return nextReviewTime;
    }

    public void setNextReviewTime(Date nextReviewTime) {
        this.nextReviewTime = nextReviewTime;
    }

    public Integer getTotalAttempts() {
        return totalAttempts;
    }

    public void setTotalAttempts(Integer totalAttempts) {
        this.totalAttempts = totalAttempts;
    }

    public Integer getCorrectAttempts() {
        return correctAttempts;
    }

    public void setCorrectAttempts(Integer correctAttempts) {
        this.correctAttempts = correctAttempts;
    }

    public Integer getHintUsedCount() {
        return hintUsedCount;
    }

    public void setHintUsedCount(Integer hintUsedCount) {
        this.hintUsedCount = hintUsedCount;
    }

    public Date getLastSolvedAt() {
        return lastSolvedAt;
    }

    public void setLastSolvedAt(Date lastSolvedAt) {
        this.lastSolvedAt = lastSolvedAt;
    }

    public boolean isScrapped() {
        return isScrapped != null && isScrapped;
    }

    public void setScrapped(Boolean scrapped) {
        this.isScrapped = scrapped;
    }

}