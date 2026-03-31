package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Model.Approval;
import com.mitmeerut.CFM_Portal.Repository.DocumentRepository;
import com.mitmeerut.CFM_Portal.Model.Teacher;
import com.mitmeerut.CFM_Portal.Model.CourseFile;
import com.mitmeerut.CFM_Portal.Model.ApprovalStatus;
import com.mitmeerut.CFM_Portal.Repository.ApprovalRepository;
import com.mitmeerut.CFM_Portal.Repository.CourseFileRepository;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")

public class ApprovalController {

    private final CourseFileRepository courseFileRepository;
    private final ApprovalRepository approvalRepository;
    private final DocumentRepository documentRepository;
    private final com.mitmeerut.CFM_Portal.Service.CourseFileService courseFileService;

    public ApprovalController(CourseFileRepository courseFileRepository,
            ApprovalRepository approvalRepository,
            DocumentRepository documentRepository,
            com.mitmeerut.CFM_Portal.Service.CourseFileService courseFileService) {
        this.courseFileRepository = courseFileRepository;
        this.approvalRepository = approvalRepository;
        this.documentRepository = documentRepository;
        this.courseFileService = courseFileService;
    }

    // ==================== TEACHER ENDPOINTS ====================

    @PostMapping("/teacher/course-files/{id}/submit")
    public ResponseEntity<Map<String, Object>> submitCourseFile(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        CourseFile courseFile = courseFileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course file not found"));

        if (!courseFile.getCreatedBy().getId().equals(user.getTeacher().getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
        }

        CourseFile updated = courseFileService.submitFile(id);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Course file submitted successfully");
        response.put("status", updated.getStatus());
        response.put("submittedAt", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/teacher/course-files/{id}/status")
    public ResponseEntity<Map<String, Object>> getCourseFileStatus(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        CourseFile courseFile = courseFileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course file not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("id", courseFile.getId());
        response.put("status", courseFile.getStatus());
        response.put("courseCode", courseFile.getCourse().getCode());
        response.put("courseTitle", courseFile.getCourse().getTitle());
        response.put("teacherName", courseFile.getCreatedBy().getName());
        response.put("academicYear", courseFile.getAcademicYear());
        response.put("section", courseFile.getSection());
        response.put("submittedDate", courseFile.getCreatedAt());

        Optional<Approval> latestApproval = approvalRepository.findTopByCourseFile_IdOrderByActedAtDesc(id);
        if (latestApproval.isPresent()) {
            Approval approval = latestApproval.get();
            response.put("lastAction", Map.of(
                    "stage", approval.getStage(),
                    "status", approval.getStatus(),
                    "comment", approval.getComment() != null ? approval.getComment() : "",
                    "actedAt", approval.getActedAt()));
        }

        return ResponseEntity.ok(response);
    }

    // ==================== SUBJECT HEAD ENDPOINTS ====================

    @GetMapping("/subject-head/pending-approvals")
    public ResponseEntity<List<Map<String, Object>>> getSubjectHeadPendingApprovals(
            @AuthenticationPrincipal CustomUserDetails user) {

        Teacher teacher = user.getTeacher();
        if (teacher == null)
            return ResponseEntity.ok(Collections.emptyList());

        List<CourseFile> pendingFiles = courseFileRepository.findPendingForSubjectHead(teacher.getId());

        // Pre-fetch IDs of course files with inactive documents to avoid N+1
        List<Long> fileIds = pendingFiles.stream().map(CourseFile::getId).collect(Collectors.toList());
        List<Long> filesWithIncorrect = fileIds.isEmpty() ? List.of()
                : documentRepository.findCourseFileIdsWithInactiveDocuments(fileIds);

        List<Map<String, Object>> result = pendingFiles.stream().map(cf -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", cf.getId());
            item.put("courseFileId", cf.getId());
            item.put("academicYear", cf.getAcademicYear());
            item.put("section", cf.getSection());
            item.put("submittedDate", cf.getCreatedAt());
            item.put("createdAt", cf.getCreatedAt());

            String statusStr = cf.getStatus().name();
            if (cf.getStatus() == ApprovalStatus.SUBMITTED)
                statusStr = "SUBMITTED";
            item.put("status", statusStr);

            if (cf.getCourse() != null) {
                item.put("courseCode", cf.getCourse().getCode());
                item.put("courseTitle", cf.getCourse().getTitle());
                Map<String, Object> courseMap = new HashMap<>();
                courseMap.put("id", cf.getCourse().getId());
                courseMap.put("code", cf.getCourse().getCode());
                courseMap.put("title", cf.getCourse().getTitle());
                item.put("course", courseMap);
            }

            if (cf.getCreatedBy() != null) {
                item.put("teacherName", cf.getCreatedBy().getName());
                Map<String, Object> teacherMap = new HashMap<>();
                teacherMap.put("id", cf.getCreatedBy().getId());
                teacherMap.put("name", cf.getCreatedBy().getName());
                item.put("createdBy", teacherMap);
            }

            // Batched check for incorrect files
            item.put("hasIncorrectFiles", filesWithIncorrect.contains(cf.getId()));

            // Add version count for comparison logic
            long vCount = courseFileService.getVersionCount(cf.getCourse().getId(), cf.getAcademicYear(),
                    cf.getSection());
            item.put("versionCount", vCount);
            item.put("revisionNumber", cf.getRevisionNumber());

            return item;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);

    }

    @GetMapping("/subject-head/finalized-approvals")
    public ResponseEntity<List<Map<String, Object>>> getSubjectHeadFinalizedApprovals(
            @AuthenticationPrincipal CustomUserDetails user) {

        Teacher teacher = user.getTeacher();
        if (teacher == null)
            return ResponseEntity.ok(Collections.emptyList());

        List<CourseFile> finalizedFiles = courseFileRepository.findFinalizedForSubjectHead(teacher.getId());

        List<Map<String, Object>> result = new ArrayList<>();
        for (CourseFile cf : finalizedFiles) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", cf.getId());
            item.put("courseFileId", cf.getId());
            item.put("academicYear", cf.getAcademicYear());
            item.put("section", cf.getSection());
            item.put("status", cf.getStatus().name());
            item.put("submittedDate", cf.getCreatedAt());

            if (cf.getCourse() != null) {
                item.put("courseCode", cf.getCourse().getCode());
                item.put("courseTitle", cf.getCourse().getTitle());
            }

            if (cf.getCreatedBy() != null) {
                item.put("teacherName", cf.getCreatedBy().getName());
            }

            // Get latest approval for "Reviewed By" and "Review Date"
            java.util.Optional<Approval> latestApp = approvalRepository
                    .findTopByCourseFile_IdOrderByActedAtDesc(cf.getId());
            if (latestApp.isPresent()) {
                item.put("reviewedByName",
                        latestApp.get().getApprover() != null ? latestApp.get().getApprover().getName() : "System");
                item.put("reviewDate", latestApp.get().getActedAt());
            } else {
                item.put("reviewedByName", "N/A");
                item.put("reviewDate", null);
            }

            result.add(item);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/subject-head/assigned-courses")
    public ResponseEntity<List<Map<String, Object>>> getSubjectHeadAssignedCourses(
            @AuthenticationPrincipal CustomUserDetails user) {
        Teacher sh = user.getTeacher();
        if (sh == null)
            return ResponseEntity.ok(Collections.emptyList());

        // Find all assignments for courses where this person is Subject Head
        List<com.mitmeerut.CFM_Portal.Model.CourseTeacher> allAssignments = courseFileService
                .getAllAssignmentsUnderSubjectHead(sh.getId());

        List<Map<String, Object>> result = new ArrayList<>();
        for (com.mitmeerut.CFM_Portal.Model.CourseTeacher ct : allAssignments) {
            Map<String, Object> item = new HashMap<>();
            // Use course ID for navigation (or CT id for specific section view)
            item.put("id", ct.getCourse().getId());
            item.put("ctId", ct.getId());
            item.put("courseCode", ct.getCourse().getCode());
            item.put("courseTitle", ct.getCourse().getTitle());
            item.put("academicYear", ct.getAcademicYear());
            item.put("section", ct.getSection());
            item.put("teacherName", ct.getTeacher() != null ? ct.getTeacher().getName() : "N/A");

            String acadYear = ct.getAcademicYear() != null ? ct.getAcademicYear().trim() : "";
            String sect = ct.getSection() != null ? ct.getSection().trim() : "";

            // Look for actual file status - handle trimmed matches and multiple revisions
            List<CourseFile> revisions = courseFileRepository.findRevisions(
                    ct.getTeacher().getId(),
                    ct.getCourse().getId(),
                    acadYear,
                    sect);

            if (!revisions.isEmpty()) {
                // Smart Selection:
                // 1. Favor Submitted/Under Review (Action Required)
                // 2. Otherwise favor Approved/Finalized (Completion Status)
                // 3. Fallback to the latest revision (Work in Progress)
                CourseFile selected = revisions.get(0);

                Optional<CourseFile> actionNeeded = revisions.stream()
                        .filter(f -> f.getStatus() == com.mitmeerut.CFM_Portal.Model.ApprovalStatus.SUBMITTED ||
                                f.getStatus() == com.mitmeerut.CFM_Portal.Model.ApprovalStatus.UNDER_REVIEW_HOD)
                        .findFirst();

                if (actionNeeded.isPresent()) {
                    selected = actionNeeded.get();
                } else {
                    Optional<CourseFile> completed = revisions.stream()
                            .filter(f -> f.getStatus() == com.mitmeerut.CFM_Portal.Model.ApprovalStatus.FINAL_APPROVED
                                    ||
                                    f.getStatus() == com.mitmeerut.CFM_Portal.Model.ApprovalStatus.APPROVED)
                            .findFirst();
                    if (completed.isPresent()) {
                        selected = completed.get();
                    }
                }

                item.put("courseFileId", selected.getId());

                String statusStr = selected.getStatus().name();
                // Map to standardized status string for frontend
                if (selected.getStatus() == com.mitmeerut.CFM_Portal.Model.ApprovalStatus.SUBMITTED)
                    statusStr = "SUBMITTED";
                else if (selected.getStatus() == com.mitmeerut.CFM_Portal.Model.ApprovalStatus.FINAL_APPROVED)
                    statusStr = "FINAL_APPROVED";
                else if (selected.getStatus() == com.mitmeerut.CFM_Portal.Model.ApprovalStatus.APPROVED)
                    statusStr = "APPROVED";

                item.put("status", statusStr);
                item.put("revisionNumber", selected.getRevisionNumber());
            } else {
                item.put("status", "NOT_CREATED");
            }

            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/subject-head/overview")
    public ResponseEntity<Map<String, Object>> getSubjectHeadOverview(
            @AuthenticationPrincipal CustomUserDetails user) {
        Teacher sh = user.getTeacher();
        if (sh == null)
            return ResponseEntity.badRequest().build();

        List<CourseFile> supervisedFiles = courseFileRepository.findAllForSubjectHead(sh.getId());

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFiles", supervisedFiles.size());
        stats.put("pendingReviews", supervisedFiles.stream()
                .filter(f -> f.getStatus() == com.mitmeerut.CFM_Portal.Model.ApprovalStatus.SUBMITTED).count());
        stats.put("approvedFiles", supervisedFiles.stream()
                .filter(f -> f.getStatus() == com.mitmeerut.CFM_Portal.Model.ApprovalStatus.APPROVED ||
                        f.getStatus() == com.mitmeerut.CFM_Portal.Model.ApprovalStatus.FINAL_APPROVED)
                .count());
        stats.put("withHod", supervisedFiles.stream()
                .filter(f -> f.getStatus() == com.mitmeerut.CFM_Portal.Model.ApprovalStatus.UNDER_REVIEW_HOD).count());

        return ResponseEntity.ok(stats);
    }

    @PostMapping("/subject-head/approvals/{courseFileId}/approve")
    public ResponseEntity<Map<String, Object>> subjectHeadApprove(
            @PathVariable("courseFileId") Long courseFileId,
            @AuthenticationPrincipal CustomUserDetails user) {

        courseFileService.approveFile(courseFileId, user.getTeacher().getId());

        return ResponseEntity.ok(Map.of("message", "Approved by Subject Head"));
    }

    @PostMapping("/subject-head/approvals/{courseFileId}/return")
    public ResponseEntity<Map<String, Object>> subjectHeadReturn(
            @PathVariable("courseFileId") Long courseFileId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails user) {

        String comment = body.get("comment");
        courseFileService.rejectFile(courseFileId, comment, "SUBJECT_HEAD", user.getTeacher().getId());

        return ResponseEntity.ok(Map.of("message", "Returned to teacher"));
    }

    // ==================== HOD ENDPOINTS ====================

    // --- Approval History ---

    @GetMapping("/approvals/{courseFileId}/history")
    public ResponseEntity<List<Map<String, Object>>> getApprovalHistory(
            @PathVariable("courseFileId") Long courseFileId) {

        List<Approval> approvals = approvalRepository.findByCourseFile_Id(courseFileId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Approval approval : approvals) {
            Map<String, Object> item = new HashMap<>();
            item.put("stage", approval.getStage());
            item.put("status", approval.getStatus());
            item.put("comment", approval.getComment());

            String approverName = "Unknown";
            Teacher approver = approval.getApprover();
            if (approver != null) {
                approverName = approver.getName();
            }
            item.put("approverName", approverName);
            item.put("actedAt", approval.getActedAt());

            result.add(item);
        }

        return ResponseEntity.ok(result);
    }
}
