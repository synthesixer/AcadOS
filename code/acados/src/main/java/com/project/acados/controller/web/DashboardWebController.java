package com.project.acados.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web controller serving role-based dashboards, student registration,
 * and admin operational views.
 * Reference: Implement_Plan-AcadOS.md §7, sidebar.html
 */
@Controller
public class DashboardWebController {

    // ========== Dashboards ==========
    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/teacher/dashboard")
    public String teacherDashboard() {
        return "teacher/dashboard";
    }

    @GetMapping("/student/dashboard")
    public String studentDashboard() {
        return "student/dashboard";
    }

    // ========== Teacher Features ==========
    @GetMapping("/teacher/swaps")
    public String teacherSwaps() {
        return "teacher/swaps";
    }

    // ========== Student Features ==========
    @GetMapping("/student/courses")
    public String studentCourses() {
        return "student/courses";
    }

    @GetMapping("/student/registrations")
    public String studentRegistrations() {
        return "student/registrations";
    }

    // ========== Notifications ==========
    @GetMapping("/notifications")
    public String notifications() {
        return "notifications";
    }

    // ========== Admin Management ==========
    @GetMapping("/admin/courses")
    public String adminCourses() {
        return "admin/courses";
    }

    @GetMapping("/admin/rooms")
    public String adminRooms() {
        return "admin/rooms";
    }

    @GetMapping("/admin/sections")
    public String adminSections() {
        return "admin/sections";
    }

    @GetMapping("/admin/users")
    public String adminUsers() {
        return "admin/users";
    }

    @GetMapping("/admin/registrations")
    public String adminRegistrations() {
        return "admin/registrations";
    }

    @GetMapping("/admin/swaps")
    public String adminSwaps() {
        return "admin/swaps";
    }
}
