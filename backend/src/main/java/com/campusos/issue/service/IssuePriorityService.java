package com.campusos.issue.service;

import com.campusos.issue.enums.IssueEnums;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class IssuePriorityService {

    public record Recommendation(IssueEnums.Priority priority, int score, String explanation) {}

    public Recommendation recommend(IssueEnums.Category category, IssueEnums.Priority requested,
                                    Integer affectedPeople, Boolean safetyRisk, Instant createdAt) {
        int score = 10;
        List<String> reasons = new ArrayList<>();

        if (Boolean.TRUE.equals(safetyRisk) || category == IssueEnums.Category.SAFETY) {
            score += 30;
            reasons.add("safety risk (+30)");
        }
        if (category == IssueEnums.Category.ELECTRICAL || category == IssueEnums.Category.LAB_EQUIPMENT) {
            score += 10;
            reasons.add("hazard-prone category (+10)");
        }
        if (requested != null) {
            int base = switch (requested) {
                case LOW -> 0;
                case MEDIUM -> 10;
                case HIGH -> 20;
                case CRITICAL -> 30;
            };
            score += base;
            if (base > 0) reasons.add("reporter priority " + requested + " (+" + base + ")");
        }
        int people = affectedPeople == null ? 1 : Math.max(1, affectedPeople);
        if (people >= 50) { score += 20; reasons.add("50+ people affected (+20)"); }
        else if (people >= 10) { score += 12; reasons.add("10+ people affected (+12)"); }
        else if (people >= 3) { score += 5; reasons.add("several people affected (+5)"); }

        if (createdAt != null) {
            long days = Duration.between(createdAt, Instant.now()).toDays();
            int ageing = (int) Math.min(20, days * 2);
            if (ageing > 0) { score += ageing; reasons.add("waiting " + days + "d (+" + ageing + ")"); }
        }

        IssueEnums.Priority band;
        if (score >= 70) band = IssueEnums.Priority.CRITICAL;
        else if (score >= 50) band = IssueEnums.Priority.HIGH;
        else if (score >= 30) band = IssueEnums.Priority.MEDIUM;
        else band = IssueEnums.Priority.LOW;

        String explanation = reasons.isEmpty()
                ? "Score " + score + "/100 → " + band + " (baseline, no risk factors)."
                : "Score " + score + "/100 → " + band + ": " + String.join(", ", reasons) + ".";
        return new Recommendation(band, score, explanation);
    }
}
