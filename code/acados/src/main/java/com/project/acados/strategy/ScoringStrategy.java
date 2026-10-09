package com.project.acados.strategy;

import com.project.acados.domain.entity.Room;
import com.project.acados.domain.entity.Section;
import com.project.acados.domain.entity.Teacher;

/**
 * Strategy interface for calculating soft factor scores for schedule assignments.
 * Reference: Implement_Plan-AcadOS.md §7 (strategy/), §10.3, §12.4
 */
public interface ScoringStrategy {

    /**
     * Calculates soft factor score for a teacher, room, and section combination.
     *
     * @param teacher candidate teacher
     * @param room candidate room
     * @param section section to be scheduled
     * @return soft factor score (e.g. +30, +20)
     */
    int calculateScore(Teacher teacher, Room room, Section section);
}

