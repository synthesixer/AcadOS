package com.project.acados.service;

import com.project.acados.dto.request.TeacherAvailabilityToggleRequest;
import com.project.acados.dto.request.TeacherPreferenceRequest;
import com.project.acados.dto.response.CourseResponse;
import com.project.acados.dto.response.TeacherAvailabilityResponse;
import com.project.acados.dto.response.TeacherPreferenceResponse;

import java.util.List;

/**
 * Service interface for managing teacher course preferences and slot availabilities.
 * Follows Layered Architecture and Dependency Inversion Principle (DIP).
 */
public interface TeacherPreferenceService {

    List<TeacherPreferenceResponse> getPreferences(String userIdentifier);

    List<CourseResponse> getQualifiedCourses(String userIdentifier);

    TeacherPreferenceResponse savePreference(TeacherPreferenceRequest request, String userIdentifier);

    void deletePreference(Long id, String userIdentifier);

    List<TeacherAvailabilityResponse> getAvailabilities(String userIdentifier);

    TeacherAvailabilityResponse updateAvailability(TeacherAvailabilityToggleRequest request, String userIdentifier);
}

