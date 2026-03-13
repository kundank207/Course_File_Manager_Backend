package com.mitmeerut.CFM_Portal.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import com.mitmeerut.CFM_Portal.Model.Document;
import com.mitmeerut.CFM_Portal.Model.DocumentStatus;
import com.mitmeerut.CFM_Portal.Model.Heading;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
        // List<Document> findByHeading(Heading heading);

        List<Document> findByHeading_Id(Long headingId);

        @Query("SELECT d FROM Document d WHERE d.heading.id = :headingId AND (d.isActive IS TRUE OR d.isActive IS NULL)")
        List<Document> findByHeading_IdAndIsActiveTrue(@Param("headingId") Long headingId);

        // For SUBJECT HEAD
        @Query("SELECT d FROM Document d WHERE d.heading.id = :headingId AND d.status = :status AND (d.isActive IS TRUE OR d.isActive IS NULL)")
        List<Document> findByHeading_IdAndStatusAndIsActiveTrue(@Param("headingId") Long headingId,
                        @Param("status") DocumentStatus status);

        @Query("SELECT d FROM Document d WHERE d.heading.id = :headingId AND d.status IN :statuses AND (d.isActive IS TRUE OR d.isActive IS NULL)")
        List<Document> findByHeading_IdAndStatusInAndIsActiveTrue(@Param("headingId") Long headingId,
                        @Param("statuses") List<DocumentStatus> statuses);

        // For Auto-Approval Cron
        List<Document> findByStatusAndReviewDeadlineBefore(DocumentStatus status, LocalDateTime now);

        long countByHeading_IdIn(List<Long> headingIds);

        List<Document> findByHeading_CourseFile_Id(Long courseFileId);

        boolean existsByHeading(Heading heading);

        @Query("SELECT COALESCE(SUM(d.fileSize), 0) FROM Document d")
        Long getTotalStorageUsed();

        @Query("SELECT COUNT(d) FROM Document d WHERE d.uploadedBy.id = :teacherId")
        long countByUploadedBy_Id(@Param("teacherId") Long teacherId);

        List<Document> findByUploadedBy_Id(Long teacherId);

        @Query("SELECT COUNT(d) FROM Document d WHERE d.uploadedBy.department.id = :deptId AND d.uploadedAt >= :start AND d.uploadedAt <= :end")
        long countByDepartmentAndInterval(@Param("deptId") Long deptId, @Param("start") java.time.LocalDateTime start,
                        @Param("end") java.time.LocalDateTime end);

        @Query("SELECT DISTINCT d.heading.courseFile.id FROM Document d WHERE d.heading.courseFile.id IN :ids AND d.isActive = false")
        List<Long> findCourseFileIdsWithInactiveDocuments(@Param("ids") List<Long> ids);

        @Query("SELECT d FROM Document d WHERE d.heading.courseFile.createdBy.id = :teacherId AND d.isActive = true AND d.status IN :statuses")
        List<Document> findFlaggedDocumentsByTeacher(@Param("teacherId") Long teacherId,
                        @Param("statuses") List<DocumentStatus> statuses);

        @Query("SELECT COUNT(d) FROM Document d WHERE d.uploadedBy.id = :teacherId AND d.uploadedAt >= :start AND d.uploadedAt <= :end")
        long countByUploadedByAndInterval(@Param("teacherId") Long teacherId, @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("SELECT FUNCTION('DATE', d.uploadedAt) as date, COUNT(d) " +
                        "FROM Document d WHERE d.uploadedBy.id = :teacherId AND d.uploadedAt >= :start " +
                        "GROUP BY FUNCTION('DATE', d.uploadedAt)")
        List<Object[]> getDailyUploadStats(@Param("teacherId") Long teacherId, @Param("start") LocalDateTime start);

        @Query("SELECT FUNCTION('YEAR', d.uploadedAt) as year, FUNCTION('MONTH', d.uploadedAt) as month, COUNT(d) " +
                        "FROM Document d WHERE d.uploadedBy.id = :teacherId AND d.uploadedAt >= :start " +
                        "GROUP BY FUNCTION('YEAR', d.uploadedAt), FUNCTION('MONTH', d.uploadedAt)")
        List<Object[]> getMonthlyUploadStats(@Param("teacherId") Long teacherId, @Param("start") LocalDateTime start);

        @Query("SELECT COUNT(d) FROM Document d WHERE d.heading.courseFile.id IN :courseFileIds")
        long countByCourseFileIdIn(@Param("courseFileIds") List<Long> courseFileIds);

        @Query("SELECT d FROM Document d WHERE d.heading.courseFile.id IN :courseFileIds")
        List<Document> findByCourseFileIdIn(@Param("courseFileIds") List<Long> courseFileIds);
}
