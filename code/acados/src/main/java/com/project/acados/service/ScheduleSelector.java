package com.project.acados.service;

import com.project.acados.strategy.ScoringStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Selects the optimal Candidate schedule based on soft constraint scoring.
 * Evaluates ScoringStrategy implementations, ranks candidates, and breaks ties randomly.
 * Reference: class diagram.puml (§6 scheduling), Implement_Plan-AcadOS.md §10.3, §12.1, §12.4
 */
@Component
@RequiredArgsConstructor
public class ScheduleSelector {

    public static final int BASELINE_SCORE = 100;

    private final List<ScoringStrategy> scoringStrategies;
    private final Random random = new Random();

    /**
     * Evaluates soft scores for passed candidates and returns the winning Candidate.
     *
     * @param candidates list of candidates that already passed all Hard Constraints
     * @return highest scoring Candidate (randomized on tie) or null if list is empty
     */
    public Candidate selectBest(List<Candidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        // 1. Calculate soft factor scores
        for (Candidate candidate : candidates) {
            int totalScore = BASELINE_SCORE;
            if (scoringStrategies != null) {
                for (ScoringStrategy strategy : scoringStrategies) {
                    totalScore += strategy.calculateScore(candidate);
                }
            }
            candidate.setScore(totalScore);
        }

        // 2. Find max score
        int maxScore = candidates.stream()
                .mapToInt(Candidate::getScore)
                .max()
                .orElse(BASELINE_SCORE);

        // 3. Collect tied candidates
        List<Candidate> topCandidates = candidates.stream()
                .filter(c -> c.getScore() == maxScore)
                .toList();

        if (topCandidates.size() == 1) {
            return topCandidates.get(0);
        }

        // 4. Random tie-breaking as specified in §12.1 & Sequence 05
        return topCandidates.get(random.nextInt(topCandidates.size()));
    }
}

