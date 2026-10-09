package com.project.acados.service;

import com.project.acados.domain.entity.Room;
import com.project.acados.domain.entity.Section;
import com.project.acados.domain.entity.Teacher;
import com.project.acados.domain.entity.TimeSlot;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Candidate timetable assignment for a section.
 * 1 Candidate = all periods of 1 Section (Sequence 05).
 * Combination of Teacher x Room x List of TimeSlots.
 * Reference: class diagram.puml (§6 scheduling), Implement_Plan-AcadOS.md §10.3, §12.2
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidate {

    private Section section;
    private Teacher teacher;
    private Room room;

    @Builder.Default
    private List<TimeSlot> timeSlots = new ArrayList<>();

    private int score;

    public void addScore(int points) {
        this.score += points;
    }
}

