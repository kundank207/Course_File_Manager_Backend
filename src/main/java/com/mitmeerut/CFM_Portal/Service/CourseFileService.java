package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.CourseFile;
import com.mitmeerut.CFM_Portal.Model.ApprovalStatus;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface CourseFileService {

    CourseFile createCourseFile(com.mitmeerut.CFM_Portal.dto.CreateCourseFileRequest request, CustomUserDetails user);

    List<CourseFile> getCourseFilesByTeacher(Long teacherId);

    void deleteCourseFile(Long id, Long teacherId);

    // Approval Workflow
    CourseFile submitFile(Long fileId);

    CourseFile approveFile(Long fileId, Long approverId);

    CourseFile rejectFile(Long fileId, String reason, String role, Long reviewerId);

    List<CourseFile> getCourseFilesByStatus(Long teacherId, ApprovalStatus status);

    List<CourseFile> getRejectedCourseFilesByTeacher(Long teacherId);

    List<com.mitmeerut.CFM_Portal.Model.CourseTeacher> getSubjectHeadAssignments(Long teacherId);

    List<com.mitmeerut.CFM_Portal.Model.CourseTeacher> getAllAssignmentsUnderSubjectHead(Long teacherId);

    CourseFile createRevision(Long fileId, Long teacherId);

    // Course File Reuse
    List<CourseFile> findPreviousCourseFiles(Long courseId, String academicYear);

    CourseFile reuseCourseFile(Long previousFileId, Long newCourseId, String academicYear, String section,
            Long teacherId);

    // Course File Sharing
    void shareCourseFile(Long fileId, Long sharedById, Long sharedWithId, String message);

    List<com.mitmeerut.CFM_Portal.Model.CourseFileShare> getSharedWithMe(Long teacherId);

    CourseFile copySharedFile(Long shareId, String academicYear, String section, Long teacherId);

    long getVersionCount(Long courseId, String academicYear, String section);

    void validateAccess(CourseFile cf, CustomUserDetails user);

    void validateAccess(Long courseFileId, CustomUserDetails user);
}
