package com.example.demo.controller;

import com.example.demo.service.KotlinProblemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final KotlinProblemService kotlinProblemService;

    public StatsController(KotlinProblemService kotlinProblemService) {
        this.kotlinProblemService = kotlinProblemService;
    }

    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> getTodayStats(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "정보처리기능사") String courseId) {

        int solvedCount;

        // 🔥 [핵심 수정] courseId가 "all"이면 전체 개수 카운트, 아니면 해당 과목만 카운트
        if ("all".equalsIgnoreCase(courseId)) {
            solvedCount = kotlinProblemService.getTodayTotalSolvedCount(userId);
        } else {
            solvedCount = kotlinProblemService.getTodaySolvedCount(userId, courseId);
        }

        Long studyTime = kotlinProblemService.getTodayStudyTime(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("solvedCount", solvedCount);
        response.put("studyTime", studyTime);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllStats(@RequestParam Long userId) {
        List<Integer> data = kotlinProblemService.getAllStudyData(userId);
        Long totalSeconds = kotlinProblemService.getTotalStudyTime(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("dailyCounts", data);
        response.put("totalTimeSeconds", totalSeconds);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/weekly")
    public ResponseEntity<Map<String, Object>> getWeeklyStats(@RequestParam Long userId) {
        List<Integer> data = kotlinProblemService.getWeeklyStudyData(userId);
        Long time = kotlinProblemService.getWeeklyTotalTime(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("dailyCounts", data);
        response.put("periodTimeSeconds", time);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlyStats(@RequestParam Long userId) {
        List<Integer> data = kotlinProblemService.getMonthlyStudyData(userId);
        Long time = kotlinProblemService.getMonthlyTotalTime(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("dailyCounts", data);
        response.put("periodTimeSeconds", time);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/yearly")
    public ResponseEntity<Map<String, Object>> getYearlyStats(@RequestParam Long userId) {
        List<Integer> data = kotlinProblemService.getYearlyStudyData(userId);
        Long time = kotlinProblemService.getYearlyTotalTime(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("dailyCounts", data);
        response.put("periodTimeSeconds", time);
        return ResponseEntity.ok(response);
    }
}