package com.project.acados.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web controller serving timetable grid, academic calendar, and holiday views.
 * Reference: Implement_Plan-AcadOS.md §7, §21, sidebar.html
 */
@Controller
public class TimetableWebController {

    @GetMapping("/admin/schedules")
    public String adminSchedules() {
        return "timetable/grid";
    }

    @GetMapping("/teacher/timetable")
    public String teacherTimetable() {
        return "timetable/grid";
    }

    @GetMapping("/student/timetable")
    public String studentTimetable() {
        return "timetable/grid";
    }

    @GetMapping({"/timetable", "/timetable/grid"})
    public String timetableGrid() {
        return "timetable/grid";
    }

    @GetMapping({"/calendar", "/admin/calendar"})
    public String calendar() {
        return "timetable/calendar";
    }

    @GetMapping("/admin/holidays")
    public String holidays() {
        return "timetable/holidays";
    }
}

