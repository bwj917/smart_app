package com.example.demo.domain;

import jakarta.persistence.*; // Spring Boot 3.x 이상 버전

@Entity // 1. 이 클래스가 DB 테이블과 매핑됨을 명시
@Table(name = "PROBLEM") // 2. 실제 테이블 이름 지정 (오라클은 대문자 권장)
public class Problem {

    @Id
    @Column(name = "PROBLEMID")
    private Long problemId;

    // question, answer는 DB에 QUESTION, ANSWER로 저장되어 있다고 가정
    private String question;

    private String answer;

    @Column(name = "COURSE_ID")
    private String courseId;



    // 기본 생성자 (필수)
    public Problem() {}

    public Long getProblemId() {
        return problemId;
    }

    public void setProblemId(Long problemId) {
        this.problemId = problemId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

//    public Integer getProblemLevel() {
//        return problemLevel;
//    }
//
//    public void setProblemLevel(Integer problemLevel) {
//        this.problemLevel = problemLevel;
//    }
//
//    public Date getNextReviewTime() {
//        return nextReviewTime;
//    }
//
//    public void setNextReviewTime(Date nextReviewTime) {
//        this.nextReviewTime = nextReviewTime;
//    }

    // Getter 및 Setter (데이터 접근용)
    // ... (생략)
    
    @Override
    public String toString() {
        return "문제 [ID=" + problemId + ", 질문='" + question + "', 답변='" + answer + "]";
    }
}