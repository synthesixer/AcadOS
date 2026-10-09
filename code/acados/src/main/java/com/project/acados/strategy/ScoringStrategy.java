package com.project.acados.strategy;

import com.project.acados.service.Candidate;

/**
 * Strategy interface for calculating soft factor scores for candidate schedules.
 * Reference: class diagram.puml (§6 strategy), Implement_Plan-AcadOS.md §7 (strategy/), §10.3, §12.4
 */
public interface ScoringStrategy {

    /**
     * Calculates soft factor score for a candidate schedule.
     *
     * @param candidate candidate containing section, teacher, room, and timeSlots
     * @return soft factor score (e.g. +30, +20)
     */
    int calculateScore(Candidate candidate);
}
