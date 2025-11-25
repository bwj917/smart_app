package com.example.demo.controller;

import com.example.demo.domain.Member;
import com.example.demo.service.KotlinProblemService;
import com.example.demo.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final KotlinProblemService kotlinProblemService;
    private final MemberService memberService;

    public StatsController(KotlinProblemService kotlinProblemService, MemberService memberService) {
        this.kotlinProblemService = kotlinProblemService;
        this.memberService = memberService;
    }

    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> getTodayStats(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "정보처리기능사") String courseId) {

        // ... (기존 통계 로직: solvedCount, studyTime 계산) ...
        int solvedCount = 0;
        // (위 로직은 기존 유지)

        solvedCount = kotlinProblemService.getTodaySolvedCount(userId, courseId);

        Long studyTime = kotlinProblemService.getTodayStudyTime(userId);

        // 유저 정보 조회
        int currentPoints = 0;
        String ownedCharacters = "0";
        int equippedCharacterIdx = 0; // ⭐️ 기본값 0

        Optional<Member> member = memberService.findOneById(userId);
        if (member.isPresent()) {
            Member m = member.get();
            currentPoints = m.getPoints();

            if (m.getOwnedCharacters() != null) {
                ownedCharacters = m.getOwnedCharacters();
            }

            // ⭐️ [핵심 추가] DB에서 가져온 장착 번호를 변수에 담기
            equippedCharacterIdx = m.getEquippedCharacterIdx();
        }
        Map<String, Object> response = new HashMap<>();
        response.put("solvedCount", solvedCount);
        response.put("studyTime", studyTime);
        response.put("currentPoints", currentPoints);
        response.put("ownedCharacters", ownedCharacters);

        // ⭐️ [핵심 추가] 이 값을 앱으로 보내줘야 앱이 캐릭터를 바꿉니다!
        response.put("equippedCharacterIdx", equippedCharacterIdx);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/equip-character")
    public ResponseEntity<String> equipCharacter(
            @RequestParam Long userId,
            @RequestParam int characterIdx) {

        // (여기서 소유 여부 검증 로직을 넣을 수도 있지만, 편의상 생략하고 바로 업데이트)
        // Service에 updateEquippedCharacter 메서드 추가 필요 (Repository 연결)
        memberService.updateEquippedCharacter(userId, characterIdx);

        return ResponseEntity.ok("장착 완료");
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

    @PostMapping("/reward")
    public ResponseEntity<Map<String, Object>> rewardPoints(
            @RequestParam Long userId,
            @RequestParam int amount) {

        int newTotal = memberService.addPoints(userId, amount);

        Map<String, Object> response = new HashMap<>();
        response.put("success", newTotal != -1);
        response.put("newTotalPoints", newTotal);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/buy-character")
    public ResponseEntity<Map<String, Object>> buyCharacter(
            @RequestParam Long userId,
            @RequestParam int characterIdx,
            @RequestParam int price) {

        Map<String, Object> response = new HashMap<>();

        Optional<Member> memberOpt = memberService.findOneById(userId);
        if (memberOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "유저 정보 없음");
            return ResponseEntity.badRequest().body(response);
        }

        Member member = memberOpt.get();
        int currentPoints = member.getPoints();

        // 1. 이미 가지고 있는지 확인
        String owned = member.getOwnedCharacters();
        if (owned == null) owned = "0"; // null 방지

        List<String> ownedList = new ArrayList<>(Arrays.asList(owned.split(",")));

        if (ownedList.contains(String.valueOf(characterIdx))) {
            response.put("success", false);
            response.put("message", "이미 보유한 캐릭터입니다.");
            return ResponseEntity.ok(response);
        }

        // 2. 포인트 확인
        if (currentPoints < price) {
            response.put("success", false);
            response.put("message", "포인트가 부족합니다.");
            return ResponseEntity.ok(response);
        }

        // 3. 구매 처리 (포인트 차감 + 목록 추가)
        int newPoints = currentPoints - price;
        ownedList.add(String.valueOf(characterIdx));
        String newOwnedStr = String.join(",", ownedList);

        memberService.updatePurchase(userId, newPoints, newOwnedStr);

        response.put("success", true);
        response.put("newPoints", newPoints);
        response.put("ownedCharacters", newOwnedStr);

        return ResponseEntity.ok(response);
    }


}