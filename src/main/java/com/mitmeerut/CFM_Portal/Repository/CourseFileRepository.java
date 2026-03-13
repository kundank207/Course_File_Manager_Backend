package com.mitmeerut.CFM_Portal.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import com.mitmeerut.CFM_Portal.Model.CourseFile;
import com.mitmeerut.CFM_Portal.Model.ApprovalStatus;

@Repository
public interface CourseFileRepository extends JpaRepository<CourseFile, Long> {

        List<CourseFile> findByCreatedById(Long teacherId);

        List<CourseFile> findByCourse_Id(Long courseId);

        List<CourseFile> findByStatus(ApprovalStatus status);

        long countByStatus(ApprovalStatus status);

        List<CourseFile> findByStatusIn(List<ApprovalStatus> statuses);

        List<CourseFile> findByCreatedByIdAndStatus(Long teacherId, ApprovalStatus status);

        List<CourseFile> findByCreatedByIdAndStatusIn(Long teacherId, List<ApprovalStatus> statuses);

        // Find course files by department (through course -> program -> department)
        @Query("SELECT cf FROM CourseFile cf WHERE cf.course.program.department.id = :departmentId")
        List<CourseFile> findCourseFilesByDepartmentId(@Param("departmentId") Long departmentId);

        // Find course files by department and status
        @Query("SELECT cf FROM CourseFile cf WHERE cf.course.program.department.id = :departmentId AND cf.status = :status")
        List<CourseFile> findCourseFilesByDepartmentIdAndStatus(@Param("departmentId") Long departmentId,
                        @Param("status") ApprovalStatus status);

        @Query("SELECT cf FROM CourseFile cf WHERE cf.course.program.department.id = :departmentId AND cf.status IN :statuses")
        List<CourseFile> findCourseFilesByDepartmentIdAndStatusIn(@Param("departmentId") Long departmentId,
                        @Param("statuses") List<ApprovalStatus> statuses);

        @Query("SELECT COUNT(cf) FROM CourseFile cf WHERE cf.course.program.department.id = :departmentId AND cf.status = :status")
        long countCourseFilesByDepartmentIdAndStatus(@Param("departmentId") Long departmentId,
                        @Param("status") ApprovalStatus status);

        @Query("SELECT COUNT(cf) FROM CourseFile cf WHERE cf.course.program.department.id = :departmentId")
        long countCourseFilesByDepartmentId(@Param("departmentId") Long departmentId);

        // Find course files pending approval for a specific Subject Head
        @Query("SELECT DISTINCT cf FROM CourseFile cf " +
                        "JOIN CourseTeacher ct ON cf.course.id = ct.course.id " +
                        "WHERE ct.teacher.id = :subjectHeadId " +
                        "AND ct.isSubjectHead = true " +
                        "AND ct.academicYear IS NOT NULL AND cf.academicYear IS NOT NULL " +
                        "AND (TRIM(ct.academicYear) = TRIM(cf.academicYear) " +
                        "     OR (SUBSTRING(TRIM(ct.academicYear), 1, 4) = SUBSTRING(TRIM(cf.academicYear), 1, 4) " +
                        "         AND LENGTH(TRIM(ct.academicYear)) >= 4 AND LENGTH(TRIM(cf.academicYear)) >= 4)) " +
                        "AND cf.status = com.mitmeerut.CFM_Portal.Model.ApprovalStatus.SUBMITTED")
        List<CourseFile> findPendingForSubjectHead(@Param("subjectHeadId") Long subjectHeadId);

        @Query("SELECT COUNT(cf) FROM CourseFile cf WHERE cf.course.program.department.id = :deptId AND cf.createdAt >= :start AND cf.createdAt <= :end")
        long countByDepartmentAndInterval(@Param("deptId") Long deptId, @Param("start") java.time.LocalDateTime start,
                        @Param("end") java.time.LocalDateTime end);

        @Query("SELECT DISTINCT cf FROM CourseFile cf " +
                        "JOIN CourseTeacher ct ON cf.course.id = ct.course.id " +
                        "WHERE ct.teacher.id = :subjectHeadId " +
                        "AND ct.isSubjectHead = true " +
                        "AND ct.academicYear IS NOT NULL AND cf.academicYear IS NOT NULL " +
                        "AND (TRIM(ct.academicYear) = TRIM(cf.academicYear) " +
                        "     OR (SUBSTRING(TRIM(ct.academicYear), 1, 4) = SUBSTRING(TRIM(cf.academicYear), 1, 4) " +
                        "         AND LENGTH(TRIM(ct.academicYear)) >= 4 AND LENGTH(TRIM(cf.academicYear)) >= 4)) " +
                        "AND cf.status IN (com.mitmeerut.CFM_Portal.Model.ApprovalStatus.APPROVED, com.mitmeerut.CFM_Portal.Model.ApprovalStatus.FINAL_APPROVED, com.mitmeerut.CFM_Portal.Model.ApprovalStatus.REJECTED, com.mitmeerut.CFM_Portal.Model.ApprovalStatus.ARCHIVED)")
        List<CourseFile> findFinalizedForSubjectHead(@Param("subjectHeadId") Long subjectHeadId);

        // Find all course files under supervision of a Subject Head
        @Query("SELECT DISTINCT cf FROM CourseFile cf " +
                        "JOIN CourseTeacher ct ON cf.course.id = ct.course.id " +
                        "WHERE ct.teacher.id = :subjectHeadId " +
                        "AND ct.isSubjectHead = true " +
                        "AND ct.academicYear IS NOT NULL AND cf.academicYear IS NOT NULL " +
                        "AND (TRIM(ct.academicYear) = TRIM(cf.academicYear) " +
                        "     OR (SUBSTRING(TRIM(ct.academicYear), 1, 4) = SUBSTRING(TRIM(cf.academicYear), 1, 4) " +
                        "         AND LENGTH(TRIM(ct.academicYear)) >= 4 AND LENGTH(TRIM(cf.academicYear)) >= 4))")
        List<CourseFile> findAllForSubjectHead(@Param("subjectHeadId") Long subjectHeadId);

        // Find the latest revision for a course file
        Optional<CourseFile> findTopByCourseIdAndAcademicYearAndSectionAndCreatedByIdOrderByRevisionNumberDesc(
                        @Param("courseId") Long courseId,
                        @Param("academicYear") String academicYear, @Param("section") String section,
                        @Param("teacherId") Long teacherId);

        Optional<CourseFile> findTopByCourseIdAndAcademicYearAndSectionAndCreatedByIdAndTemplateIdOrderByRevisionNumberDesc(
                        @Param("courseId") Long courseId,
                        @Param("academicYear") String academicYear, @Param("section") String section,
                        @Param("teacherId") Long teacherId, @Param("templateId") Long templateId);

        Optional<CourseFile> findTopByCourseIdAndAcademicYearAndSectionOrderByRevisionNumberDesc(
                        @Param("courseId") Long courseId,
                        @Param("academicYear") String academicYear, @Param("section") String section);

        @jakarta.transaction.Transactional
        @org.springframework.data.jpa.repository.Modifying
        @Query("UPDATE CourseFile c SET c.status = :status WHERE c.id = :id")
        void updateStatus(@Param("id") Long id, @Param("status") com.mitmeerut.CFM_Portal.Model.ApprovalStatus status);

        // Find previous years course files
        List<CourseFile> findByCourse_CodeAndAcademicYearNot(String code, String academicYear);

        List<CourseFile> findByCourse_IdAndAcademicYearNot(Long courseId, String academicYear);

        List<CourseFile> findByCourseIdAndAcademicYearAndSectionOrderByRevisionNumberDesc(Long courseId,
                        String academicYear, String section);

        long countByCourseIdAndAcademicYearAndSection(Long courseId, String academicYear, String section);

        @Query("SELECT cf FROM CourseFile cf WHERE cf.createdBy.id = :teacherId AND cf.course.id = :courseId " +
                        "AND TRIM(cf.academicYear) = :year AND (:section = '' OR TRIM(cf.section) = :section) " +
                        "ORDER BY cf.revisionNumber DESC, cf.id DESC")
        List<CourseFile> findRevisions(@Param("teacherId") Long teacherId,
                        @Param("courseId") Long courseId, @Param("year") String year, @Param("section") String section);

        @Query("SELECT COUNT(DISTINCT cf.course.id) FROM CourseFile cf WHERE cf.course.program.department.id = :deptId")
        long countActiveCoursesInDepartment(@Param("deptId") Long deptId);

        @Query("SELECT cf.course.program.department.id as deptId, cf.course.program.department.name as deptName, " +
                        "cf.status as status, COUNT(cf) as count " +
                        "FROM CourseFile cf " +
                        "GROUP BY cf.course.program.department.id, cf.course.program.department.name, cf.status")
        List<Object[]> getCombinedComplianceStats();

        @Query("SELECT cf.course.id, cf.academicYear, cf.section, COUNT(cf) " +
                        "FROM CourseFile cf WHERE cf.createdBy.id = :teacherId " +
                        "GROUP BY cf.course.id, cf.academicYear, cf.section")
        List<Object[]> findVersionCountsByTeacher(@Param("teacherId") Long teacherId);
}
