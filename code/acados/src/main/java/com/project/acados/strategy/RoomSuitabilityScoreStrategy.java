package com.project.acados.strategy;

import com.project.acados.domain.entity.Room;
import com.project.acados.domain.entity.Section;
import com.project.acados.domain.entity.Teacher;
import org.springframework.stereotype.Component;

/**
 * Concrete scoring strategy: Room Suitability (+20 points).
 * Awards 20 points if room capacity is well-matched to section size without excessive waste.
 * Reference: Implement_Plan-AcadOS.md §7 (strategy/), §10.3, §12.4
 */
@Component
public class RoomSuitabilityScoreStrategy implements ScoringStrategy {

    public static final int ROOM_SUITABILITY_BONUS = 20;

    @Override
    public int calculateScore(Teacher teacher, Room room, Section section) {
        if (room == null || section == null) {
            return 0;
        }

        if (section.getCapacity() == null || room.getCapacity() == null || section.getCapacity() <= 0) {
            return 0;
        }

        if (room.getCapacity() < section.getCapacity()) {
            return 0;
        }

        int excessSeats = room.getCapacity() - section.getCapacity();
        if (excessSeats <= 15 || room.getCapacity() <= section.getCapacity() * 1.5) {
            return ROOM_SUITABILITY_BONUS;
        }

        return 0;
    }
}

