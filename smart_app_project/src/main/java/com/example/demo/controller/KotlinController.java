package com.example.demo.controller;

import com.example.demo.domain.HintResponse;
import com.example.demo.domain.Problem;
import com.example.demo.domain.SubmissionRequest;
import com.example.demo.domain.SubmissionResponse;
import com.example.demo.dto.ProblemResponseDto;
import com.example.demo.service.KotlinProblemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/problems")
public class KotlinController {

    private final KotlinProblemService kotlinProblemService;

    public KotlinController(KotlinProblemService kotlinProblemService) {
        this.kotlinProblemService = kotlinProblemService;
    }

    @GetMapping("/tenProblem")
    public List<ProblemResponseDto> getTenProblems(
            @RequestParam Long userId,
            // 🔥 [수정] 기본값을 '정보처리기능사'로 변경
            @RequestParam(defaultValue = "정보처리기능사") String courseId
    ){
        return kotlinProblemService.tenProblem(userId, courseId);
    }

    @PostMapping("/submit")
    public SubmissionResponse submit(@RequestBody SubmissionRequest request){

        // 🔥 [수정] 서비스 호출 시 request.getStudyTime()을 맨 뒤에 추가
        SubmissionResponse response = kotlinProblemService.checkAnswer(
                request.getUserAnswer(),
                request.getProblemId(),
                request.getUserId(),
                request.getCheckCount(),
                request.getStudyTime() // 👈 여기 추가!
        );

        return response;
    }

    @GetMapping("/hint/{problemId}/{hintCount}")
    public HintResponse getHint(
            @PathVariable Long problemId,
            @PathVariable int hintCount,
            @RequestParam Long userId) { // 👈 추가됨

        String hint = kotlinProblemService.requestHint(problemId, userId, hintCount); // 👈 서비스로 전달
        return new HintResponse(hint);
    }

    @GetMapping("/frequent-wrong")
    public List<ProblemResponseDto> getFrequentWrongProblems(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "전체") String courseId) { // 🔥 파라미터 추가
        return kotlinProblemService.getFrequentWrongProblems(userId, courseId);
    }

    @PostMapping("/scrap")
    public ResponseEntity<Boolean> scrapProblem(
            @RequestParam Long userId,
            @RequestParam Long problemId) {
        boolean isScrapped = kotlinProblemService.toggleScrap(userId, problemId);
        return ResponseEntity.ok(isScrapped);
    }

    @GetMapping("/scrapped")
    public List<ProblemResponseDto> getScrappedProblems(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "전체") String courseId) {
        return kotlinProblemService.getScrappedProblems(userId, courseId);
    }
}