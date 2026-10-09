package com.project.acados.strategy;

import com.project.acados.domain.entity.Room;
import com.project.acados.domain.entity.Section;
import com.project.acados.service.Candidate;
import org.springframework.stereotype.Component;

/**
 * Concrete scoring strategy: Room Suitability (+20 points).
 * Awards 20 points if room capacity is well-matched to section size without excessive waste.
 * Reference: class diagram.puml (§6 strategy), Implement_Plan-AcadOS.md §7 (strategy/), §10.3, §12.4, §28 ข้อ 6
 *
 * NOTE [CONFIRMED TBA / INFERRED]:
 * เกณฑ์ความเหมาะสมของห้องเรียน (+20 คะแนน) ใน Implement_Plan-AcadOS.md §12.4 และ §28 ข้อ 6 ระบุสถานะเป็น (TBA):
 * "6. Room Suitability Scoring Algorithm (TBA): สูตรการคำนวณความเหมาะสมของห้องเรียน (+20 คะแนน) จะถูกกำหนดเกณฑ์มาตรฐานใน RoomSuitabilityScoreStrategy"
 * ปัจจุบันอนุมานเกณฑ์: ความจุห้องต้องเพียงพอและไม่เกิน 150% ของขนาด Section (หรือที่นั่งเหลือว่าง <= 15)
 * ทำการ noted ไว้ในโค้ด เผื่ออัปเดตเกณฑ์มาตรฐานในอนาคตเมื่อได้รับข้อกำหนดสูตรที่แน่นอน
 */
@Component
public class RoomSuitabilityScoreStrategy implements ScoringStrategy {

    public static final int ROOM_SUITABILITY_BONUS = 20;

    @Override
    public int calculateScore(Candidate candidate) {
        if (candidate == null || candidate.getRoom() == null || candidate.getSection() == null) {
            return 0;
        }

        Room room = candidate.getRoom();
        Section section = candidate.getSection();

        if (section.getCapacity() == null || room.getCapacity() == null || section.getCapacity() <= 0) {
            return 0;
        }

        if (room.getCapacity() < section.getCapacity()) {
            return 0;
        }

        // NOTE [CONFIRMED TBA / INFERRED]: เกณฑ์ความจุห้องพอดี section.capacity <= room.capacity <= section.capacity * 1.5 หรือ excess <= 15
        int excessSeats = room.getCapacity() - section.getCapacity();
        if (excessSeats <= 15 || room.getCapacity() <= section.getCapacity() * 1.5) {
            return ROOM_SUITABILITY_BONUS;
        }

        return 0;
    }
}
