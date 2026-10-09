package com.project.acados.repository;

import com.project.acados.domain.entity.TeacherAvailability;
import com.project.acados.domain.entity.TeacherPreference;
import com.project.acados.domain.entity.TeacherQualification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Performs the teacher-owned cascades specified in database.md before removing a profile. */
public interface UserAccountCleanupRepository extends JpaRepository<TeacherQualification, Long> {

    @Modifying
    @Query("delete from TeacherQualification q where q.teacher.id = :teacherId")
    void deleteQualifications(@Param("teacherId") Long teacherId);

    @Modifying
    @Query("delete from TeacherPreference p where p.teacher.id = :teacherId")
    void deletePreferences(@Param("teacherId") Long teacherId);

    @Modifying
    @Query("delete from TeacherAvailability a where a.teacher.id = :teacherId")
    void deleteAvailabilities(@Param("teacherId") Long teacherId);
}
