package com.example.demo.service;

import com.example.demo.domain.Problem;
import com.example.demo.domain.SubmissionHistory;
import com.example.demo.domain.SubmissionResponse;
import com.example.demo.domain.UserProblemStats;
import com.example.demo.dto.ProblemResponseDto;
import com.example.demo.repository.ProblemRepository;
import com.example.demo.repository.SubmissionHistoryRepository;
import com.example.demo.repository.UserProblemStatsRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class KotlinProblemService {

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private UserProblemStatsRepository statsRepository;

    @Autowired
    private SubmissionHistoryRepository historyRepository;

    // 문제 제출
    @Transactional
    public SubmissionResponse checkAnswer(String userAnswer, Long problemId, Long userId, int checkCount, int studyTime){
        Problem problem = findProblem(problemId);

        String cleanDbAnswer = problem.getAnswer().replaceAll("\\s+", "");
        String cleanUserAnswer = userAnswer.replaceAll("\\s+", "");
        boolean isCorrect = cleanDbAnswer.equalsIgnoreCase(cleanUserAnswer);

        UserProblemStats stats = findOrCreateStats(userId, problem);
        updateStats(stats, isCorrect, checkCount);

        SubmissionHistory history = new SubmissionHistory(
                userId,
                problemId,
                isCorrect,
                new Date(),
                studyTime
        );
        historyRepository.save(history);

        return new SubmissionResponse(isCorrect, new ProblemResponseDto(problem, stats));
    }

    // 문제 리스트 가져오기
    @Transactional
    public List<ProblemResponseDto> tenProblem(Long userId, String courseId) {
        List<ProblemResponseDto> tenProblem = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        Date nowTime = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());

        List<UserProblemStats> beforeStats = statsRepository.findReviewProblems(userId, courseId, nowTime);
        List<UserProblemStats> futureStats = statsRepository.findFutureProblems(userId, courseId, nowTime);
        List<Long> solvedIds = statsRepository.findSolvedProblemIds(userId, courseId);
        List<Problem> newProblem;

        if (solvedIds.isEmpty()) {
            newProblem = problemRepository.findByCourseId(courseId);
        } else {
            newProblem = problemRepository.findNewProblems(courseId, solvedIds);
        }
        Collections.shuffle(newProblem);

        if (beforeStats.size() + newProblem.size() < 10) {
            int fp = Math.min(10 - (beforeStats.size() + newProblem.size()), futureStats.size());
            for (UserProblemStats stat : beforeStats) tenProblem.add(new ProblemResponseDto(stat.getProblem(), stat));
            for (int i = 0; i < fp; i++) tenProblem.add(new ProblemResponseDto(futureStats.get(i).getProblem(), futureStats.get(i)));
            for (Problem p : newProblem) tenProblem.add(new ProblemResponseDto(p, null));
        } else {
            if (beforeStats.isEmpty()) {
                int count = Math.min(10, newProblem.size());
                for (int i = 0; i < count; i++) tenProblem.add(new ProblemResponseDto(newProblem.get(i), null));
            } else {
                if (beforeStats.size() >= 7 && newProblem.size() < 3) {
                    int num = 10 - newProblem.size();
                    for (int i = 0; i < num; i++) tenProblem.add(new ProblemResponseDto(beforeStats.get(i).getProblem(), beforeStats.get(i)));
                    for (Problem p : newProblem) tenProblem.add(new ProblemResponseDto(p, null));
                } else if (beforeStats.size() >= 7 && newProblem.size() >= 3) {
                    for (int i = 0; i < 7; i++) tenProblem.add(new ProblemResponseDto(beforeStats.get(i).getProblem(), beforeStats.get(i)));
                    for (int i = 0; i < 3; i++) tenProblem.add(new ProblemResponseDto(newProblem.get(i), null));
                } else {
                    int requiredNew = 10 - beforeStats.size();
                    int countFromNew = Math.min(requiredNew, newProblem.size());
                    for (UserProblemStats stat : beforeStats) tenProblem.add(new ProblemResponseDto(stat.getProblem(), stat));
                    for (int i = 0; i < countFromNew; i++) tenProblem.add(new ProblemResponseDto(newProblem.get(i), null));
                }
            }
        }
        return tenProblem;
    }

    public Problem findProblem(Long problemId) {
        return problemRepository.findById(problemId)
                .orElseThrow(() -> new NoSuchElementException("ID " + problemId + " 문제 없음"));
    }

    private UserProblemStats findOrCreateStats(Long userId, Problem problem) {
        Optional<UserProblemStats> optionalStats = statsRepository.findByUserIdAndProblem_ProblemId(userId, problem.getProblemId());
        if (optionalStats.isPresent()) return optionalStats.get();
        else {
            UserProblemStats newStats = new UserProblemStats();
            newStats.setUserId(userId);
            newStats.setProblem(problem);
            return statsRepository.save(newStats);
        }
    }

    @Transactional
    public void updateStats(UserProblemStats stats, Boolean isCorrect, int checkCount){
        stats.setTotalAttempts(stats.getTotalAttempts() + 1);
        if (isCorrect) {
            stats.setCorrectAttempts(stats.getCorrectAttempts() + 1);
            stats.setLastSolvedAt(new Date());
            if(checkCount == 0) {
                int currentLevel = stats.getProblemLevel();
                int nextLevel = (currentLevel == 0) ? 5 : (currentLevel < 5 ? currentLevel + 1 : 5);
                stats.setProblemLevel(nextLevel);
            }
            setReviewTime(stats);
        }
    }

    @Transactional
    public void setReviewTime(UserProblemStats stats){
        LocalDateTime now = null;
        switch(stats.getProblemLevel()){
            case 1: now = LocalDateTime.now().plusMinutes(5); break;
            case 2: now = LocalDateTime.now().plusDays(1); break;
            case 3: now = LocalDateTime.now().plusDays(3); break;
            case 4: now = LocalDateTime.now().plusDays(7); break;
            case 5: now = LocalDateTime.now().plusDays(15); break;
            default: now = LocalDateTime.now().plusMinutes(5); break;
        }
        if (now != null) stats.setNextReviewTime(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()));
    }

    @Transactional
    public String requestHint(Long currentProblemId, Long userId, int checkCount){
        Problem currentProblem = findProblem(currentProblemId);
        UserProblemStats stats = findOrCreateStats(userId, currentProblem);
        int currentLevel = stats.getProblemLevel() == 0 ? 1 : stats.getProblemLevel();

        if (checkCount == 1 || checkCount == 2) {
            if (currentLevel > 1) stats.setProblemLevel(currentLevel - 1);
        } else if (checkCount >= 3) {
            stats.setProblemLevel(1);
        }
        statsRepository.save(stats);

        String cleanAnswer = currentProblem.getAnswer().replaceAll("\\s+", "");
        if(checkCount == 1) return String.valueOf(cleanAnswer.length());
        else if(checkCount == 2) return cleanAnswer.substring(0, 1);
        else if(checkCount >= 3) return currentProblem.getAnswer();
        return "";
    }

    // --- 통계 메서드 ---

    public int getTodaySolvedCount(Long userId, String courseId) {
        // 🔥 [추가] "all" 요청이 들어오면 전체 합계를 반환하도록 분기 처리
        if ("all".equalsIgnoreCase(courseId)) {
            return getTodayTotalSolvedCount(userId);
        }

        // 기존 로직 (특정 과목만 카운트)
        LocalDateTime startLdt = LocalDateTime.now().with(java.time.LocalTime.MIN);
        LocalDateTime endLdt = LocalDateTime.now().with(java.time.LocalTime.MAX);
        return historyRepository.countTodayByCourse(
                userId, courseId,
                Date.from(startLdt.atZone(ZoneId.systemDefault()).toInstant()),
                Date.from(endLdt.atZone(ZoneId.systemDefault()).toInstant())
        );
    }

    // 🔥 [신규 추가] 오늘 전체 푼 문제 수 (과목 무관)
    public int getTodayTotalSolvedCount(Long userId) {
        LocalDateTime startLdt = LocalDateTime.now().with(java.time.LocalTime.MIN);
        LocalDateTime endLdt = LocalDateTime.now().with(java.time.LocalTime.MAX);
        return historyRepository.countTodayTotal(
                userId,
                Date.from(startLdt.atZone(ZoneId.systemDefault()).toInstant()),
                Date.from(endLdt.atZone(ZoneId.systemDefault()).toInstant())
        );
    }

    public Long getTodayStudyTime(Long userId) {
        LocalDateTime startLdt = LocalDateTime.now().with(java.time.LocalTime.MIN);
        LocalDateTime endLdt = LocalDateTime.now().with(java.time.LocalTime.MAX);
        Date start = Date.from(startLdt.atZone(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(endLdt.atZone(ZoneId.systemDefault()).toInstant());

        Long time = historyRepository.getSumStudyTimeBetween(userId, start, end);
        return time != null ? time : 0L;
    }

    public List<Integer> getWeeklyStudyData(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate startDay = today.minusDays(6);
        LocalDateTime startLdt = startDay.atStartOfDay();
        LocalDateTime endLdt = today.atTime(java.time.LocalTime.MAX);

        List<SubmissionHistory> historyList = historyRepository.findAllByUserIdAndSubmittedAtBetween(
                userId,
                Date.from(startLdt.atZone(ZoneId.systemDefault()).toInstant()),
                Date.from(endLdt.atZone(ZoneId.systemDefault()).toInstant())
        );

        int[] weeklyCounts = new int[7];
        for (SubmissionHistory h : historyList) {
            if (!h.isCorrect()) continue;
            LocalDate solvedDate = h.getSubmittedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            int index = (int) java.time.temporal.ChronoUnit.DAYS.between(startDay, solvedDate);
            if (index >= 0 && index < 7) weeklyCounts[index]++;
        }
        List<Integer> result = new ArrayList<>();
        for (int c : weeklyCounts) result.add(c);
        return result;
    }

    public List<Integer> getMonthlyStudyData(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startLdt = now.withDayOfMonth(1).with(java.time.LocalTime.MIN);
        LocalDateTime endLdt = now.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth()).with(java.time.LocalTime.MAX);

        List<SubmissionHistory> historyList = historyRepository.findAllByUserIdAndSubmittedAtBetween(
                userId,
                Date.from(startLdt.atZone(ZoneId.systemDefault()).toInstant()),
                Date.from(endLdt.atZone(ZoneId.systemDefault()).toInstant())
        );

        int daysInMonth = now.toLocalDate().lengthOfMonth();
        int[] dailyCounts = new int[daysInMonth];
        for (SubmissionHistory h : historyList) {
            if (!h.isCorrect()) continue;
            int day = h.getSubmittedAt().toInstant().atZone(ZoneId.systemDefault()).getDayOfMonth();
            if (day - 1 < dailyCounts.length) dailyCounts[day - 1]++;
        }
        List<Integer> result = new ArrayList<>();
        for (int c : dailyCounts) result.add(c);
        return result;
    }

    public List<Integer> getYearlyStudyData(Long userId) {
        int year = LocalDate.now().getYear();
        int[] monthlyCounts = new int[12];
        LocalDateTime start = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(year, 12, 31, 23, 59, 59);

        List<SubmissionHistory> list = historyRepository.findAllByUserIdAndSubmittedAtBetween(
                userId,
                Date.from(start.atZone(ZoneId.systemDefault()).toInstant()),
                Date.from(end.atZone(ZoneId.systemDefault()).toInstant())
        );

        for (SubmissionHistory h : list) {
            if (!h.isCorrect()) continue;
            int month = h.getSubmittedAt().toInstant().atZone(ZoneId.systemDefault()).getMonthValue();
            monthlyCounts[month - 1]++;
        }
        List<Integer> result = new ArrayList<>();
        for(int c : monthlyCounts) result.add(c);
        return result;
    }

    public List<Integer> getAllStudyData(Long userId) {
        int currentYear = LocalDate.now().getYear();
        int startYear = currentYear - 4;
        int[] yearlyCounts = new int[5];
        List<SubmissionHistory> allHistory = historyRepository.findByUserId(userId);
        for (SubmissionHistory h : allHistory) {
            if (!h.isCorrect()) continue;
            int solvedYear = h.getSubmittedAt().toInstant().atZone(ZoneId.systemDefault()).getYear();
            int index = solvedYear - startYear;
            if (index >= 0 && index < 5) yearlyCounts[index]++;
        }
        List<Integer> result = new ArrayList<>();
        for(int c : yearlyCounts) result.add(c);
        return result;
    }

    public Long getTotalStudyTime(Long userId) {
        Long total = historyRepository.getTotalStudyTime(userId);
        return total != null ? total : 0L;
    }

    public Long getWeeklyTotalTime(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate startDay = today.minusDays(6);
        Date start = Date.from(startDay.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(today.atTime(java.time.LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());
        Long time = historyRepository.getSumStudyTimeBetween(userId, start, end);
        return time != null ? time : 0L;
    }

    public Long getMonthlyTotalTime(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startLdt = now.withDayOfMonth(1).with(java.time.LocalTime.MIN);
        LocalDateTime endLdt = now.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth()).with(java.time.LocalTime.MAX);
        Date start = Date.from(startLdt.atZone(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(endLdt.atZone(ZoneId.systemDefault()).toInstant());
        Long time = historyRepository.getSumStudyTimeBetween(userId, start, end);
        return time != null ? time : 0L;
    }

    public Long getYearlyTotalTime(Long userId) {
        int year = LocalDate.now().getYear();
        LocalDateTime start = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(year, 12, 31, 23, 59, 59);
        Long time = historyRepository.getSumStudyTimeBetween(userId,
                Date.from(start.atZone(ZoneId.systemDefault()).toInstant()),
                Date.from(end.atZone(ZoneId.systemDefault()).toInstant()));
        return time != null ? time : 0L;
    }
}