package com.project.acados.dto.response;

import com.project.acados.domain.entity.Schedule;
import com.project.acados.domain.enums.ScheduleStatus;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Response DTO for Schedule entity.
 * Reference: class diagram.puml, Implement_Plan-AcadOS.md §16
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleResponse {

    private Long id;
    private Long sectionId;
    private Integer sectionNumber;
    private String courseCode;
    private String courseTitle;

    private Long teacherId;
    private String teacherName;

    private Long roomId;
    private String roomName;

    private Long timeSlotId;
    private DayOfWeek dayOfWeek;

    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime startTime;

    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime endTime;

    private ScheduleStatus status;

    public static ScheduleResponse fromEntity(Schedule schedule) {
        if (schedule == null) {
            return null;
        }

        ScheduleResponseBuilder builder = ScheduleResponse.builder()
                .id(schedule.getId())
                .status(schedule.getStatus());

        if (schedule.getSection() != null) {
            builder.sectionId(schedule.getSection().getId())
                    .sectionNumber(schedule.getSection().getSectionNumber());
            if (schedule.getSection().getCourse() != null) {
                builder.courseCode(schedule.getSection().getCourse().getCourseCode())
                        .courseTitle(schedule.getSection().getCourse().getTitle());
            }
        }

        if (schedule.getTeacher() != null) {
            builder.teacherId(schedule.getTeacher().getId())
                    .teacherName(schedule.getTeacher().getFullName());
        }

        if (schedule.getRoom() != null) {
            String bldg = schedule.getRoom().getBuilding() != null ? schedule.getRoom().getBuilding() : "";
            String rNum = schedule.getRoom().getRoomNumber() != null ? schedule.getRoom().getRoomNumber() : "";
            String roomName = (bldg + " " + rNum).trim();
            builder.roomId(schedule.getRoom().getId())
                    .roomName(roomName.isEmpty() ? "TBA" : roomName);
        }

        if (schedule.getTimeSlot() != null) {
            builder.timeSlotId(schedule.getTimeSlot().getId())
                    .dayOfWeek(schedule.getTimeSlot().getDayOfWeek())
                    .startTime(schedule.getTimeSlot().getStartTime())
                    .endTime(schedule.getTimeSlot().getEndTime());
        }

        return builder.build();
    }
}

