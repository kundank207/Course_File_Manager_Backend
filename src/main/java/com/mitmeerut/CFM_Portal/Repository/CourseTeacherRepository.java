package com.mitmeerut.CFM_Portal.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import com.mitmeerut.CFM_Portal.Model.CourseTeacher;

@Repository
public interface CourseTeacherRepository extends JpaRepository<CourseTeacher, Long> {
    boolean existsByCourseIdAndTeacherIdAndAcademicYearAndSection(Long courseId, Long teacherId,
            String academicYear,
            String section);

    @Query("""
                SELECT ct
                FROM CourseTeacher ct
                WHERE ct.teacher.id = :teacherId
            """)
    List<CourseTeacher> findByTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT COUNT(ct) FROM CourseTeacher ct WHERE ct.teacher.id = :teacherId")
    long countByTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT COUNT(ct) > 0 FROM CourseTeacher ct WHERE ct.teacher.id = :teacherId AND ct.isSubjectHead = true")
    boolean existsByTeacherIdAndIsSubjectHeadTrue(@Param("teacherId") Long teacherId);

    @Query("SELECT ct FROM CourseTeacher ct WHERE ct.course.id = :courseId AND ct.isSubjectHead = true AND ct.academicYear = :academicYear AND ct.section = :section")
    List<CourseTeacher> findSubjectHeadByCourseAndSection(@Param("courseId") Long courseId,
            @Param("academicYear") String academicYear, @Param("section") String section);

    @Query("SELECT ct FROM CourseTeacher ct WHERE ct.course.id = :courseId AND ct.isSubjectHead = true AND ct.academicYear = :academicYear")
    List<CourseTeacher> findSubjectHeadByCourse(@Param("courseId") Long courseId,
            @Param("academicYear") String academicYear);

    @Query("""
                SELECT ct
                FROM CourseTeacher ct
                WHERE ct.teacher.id = :teacherId AND ct.isSubjectHead = true
            """)
    List<CourseTeacher> findSubjectHeadAssignmentsByTeacherId(@Param("teacherId") Long teacherId);

    List<CourseTeacher> findByCourseIdAndAcademicYearAndIsSubjectHeadTrue(Long courseId, String academicYear);

    List<CourseTeacher> findByCourseIdAndIsSubjectHeadTrue(Long courseId);

    List<CourseTeacher> findByCourseId(Long courseId);

    @Query("SELECT DISTINCT ct FROM CourseTeacher ct " +
            "WHERE EXISTS (SELECT 1 FROM CourseTeacher ct2 " +
            "WHERE ct2.course.id = ct.course.id " +
            "AND ct2.academicYear = ct.academicYear " +
            "AND ct2.teacher.id = :shId " +
            "AND ct2.isSubjectHead = true)")
    List<CourseTeacher> findAllAssignmentsUnderSubjectHead(@Param("shId") Long shId);
}