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

    // 🔥 [신규] 학습 시간만 저장하는 API
    @PostMapping("/study_time")
    public ResponseEntity<Void> saveStudyTime(@RequestBody Map<String, Object> payload) {
        Long userId = ((Number) payload.get("userId")).longValue();
        int time = ((Number) payload.get("time")).intValue();

        kotlinProblemService.saveOnlyStudyTime(userId, time);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/today_total")
    public ResponseEntity<Map<String, Object>> getTodayTotalStats(@RequestParam Long userId) {
        int count = kotlinProblemService.getTodayTotalSolvedCount(userId);
        Long time = kotlinProblemService.getTodayTotalStudyTime(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("count", count);
        response.put("studyTime", time);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/today")
    public ResponseEntity<Map<String, Integer>> getTodayStats(@RequestParam Long userId, @RequestParam(defaultValue = "정보처리기능사") String courseId) {
        int realCount = kotlinProblemService.getTodaySolvedCount(userId, courseId);
        return ResponseEntity.ok(Map.of("count", realCount));
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