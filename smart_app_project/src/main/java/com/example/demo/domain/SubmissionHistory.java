package com.example.demo.domain;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "SUBMISSION_HISTORY",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_USER_PROBLEM",
                        columnNames = {"USER_ID", "PROBLEM_ID"}
                )
        })
public class SubmissionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long problemId;

    @Column(nullable = false)
    private boolean isCorrect; // 이 필드 때문에 Getter가 필요합니다.

    @Temporal(TemporalType.TIMESTAMP)
    private Date submittedAt; // 언제 풀었는지 기록

    @Column(nullable = false)
    private int studyTime;

    // --- 생성자 ---
    public SubmissionHistory() {}

    public SubmissionHistory(Long userId, Long problemId, boolean isCorrect, Date submittedAt, int studyTime) {
        this.userId = userId;
        this.problemId = problemId;
        this.isCorrect = isCorrect;
        this.submittedAt = submittedAt;
        this.studyTime = studyTime;
    }

    // --- Getter & Setter ---

    // 🔥 [핵심 수정] 이 Getter가 누락되어 에러가 발생했습니다.
    public boolean isCorrect() {
        return isCorrect;
    }

    public Date getSubmittedAt() { return submittedAt; }

    // 이외의 다른 Getter/Setter도 필요하다면 여기에 추가해야 합니다.
    public int getStudyTime() { return studyTime; }
    public Long getUserId() { return userId; }
    public Long getProblemId() { return problemId; }
}