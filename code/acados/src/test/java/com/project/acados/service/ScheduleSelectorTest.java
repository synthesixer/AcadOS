package com.project.acados.service;

import com.project.acados.strategy.ScoringStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ScheduleSelector Unit Tests")
class ScheduleSelectorTest {

    @Test
    @DisplayName("Should add baseline 100 and select candidate with highest strategy score")
    void shouldSelectCandidateWithHighestScore() {
        ScoringStrategy mockStrategy = Candidate::getScore;

        ScheduleSelector selector = new ScheduleSelector(List.of(mockStrategy));

        Candidate c1 = Candidate.builder().score(10).build();
        Candidate c2 = Candidate.builder().score(30).build();
        Candidate c3 = Candidate.builder().score(20).build();

        Candidate winner = selector.selectBest(List.of(c1, c2, c3));

        assertNotNull(winner);
        assertSame(c2, winner);
        assertEquals(130, c2.getScore());
        assertEquals(110, c1.getScore());
        assertEquals(120, c3.getScore());
    }

    @Test
    @DisplayName("Should handle tie-breaking by returning one of the tied winners")
    void shouldHandleTieBreaking() {
        ScoringStrategy fixedStrategy = candidate -> 20;

        ScheduleSelector selector = new ScheduleSelector(List.of(fixedStrategy));

        Candidate c1 = Candidate.builder().score(0).build();
        Candidate c2 = Candidate.builder().score(0).build();

        Candidate winner = selector.selectBest(List.of(c1, c2));

        assertNotNull(winner);
        assertTrue(winner == c1 || winner == c2);
        assertEquals(120, winner.getScore());
    }

    @Test
    @DisplayName("Should return null when candidate list is empty or null")
    void shouldReturnNullWhenEmpty() {
        ScheduleSelector selector = new ScheduleSelector(List.of());
        assertNull(selector.selectBest(List.of()));
        assertNull(selector.selectBest(null));
    }
}

