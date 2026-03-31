package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Model.*;
import com.mitmeerut.CFM_Portal.Repository.CourseFileRepository;
import com.mitmeerut.CFM_Portal.Service.HeadingService;
import com.mitmeerut.CFM_Portal.Service.DocumentService;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller for review operations accessible by Subject Head and HOD
 */
@RestController
@RequestMapping("/api/review")

@PreAuthorize("hasAnyRole('HOD', 'SUBJECTHEAD', 'TEACHER')")
public class ReviewController {

    private final CourseFileRepository courseFileRepository;
    private final HeadingService headingService;
    private final DocumentService documentService;
    private final com.mitmeerut.CFM_Portal.Service.CourseFileService courseFileService;

    public ReviewController(CourseFileRepository courseFileRepository,
            HeadingService headingService,
            DocumentService documentService,
            com.mitmeerut.CFM_Portal.Service.CourseFileService courseFileService) {
        this.courseFileRepository = courseFileRepository;
        this.headingService = headingService;
        this.documentService = documentService;
        this.courseFileService = courseFileService;
    }

    /**
     * Get course file details for review
     */
    @GetMapping("/course-file/{id}")
    public ResponseEntity<Map<String, Object>> getCourseFileDetails(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        CourseFile courseFile = courseFileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course file not found"));

        // SECURITY: Validate that the reviewer has access to this specific course file
        courseFileService.validateAccess(courseFile, user);

        Map<String, Object> response = new HashMap<>();
        response.put("id", courseFile.getId());

        // Map status for frontend (keeping it simple and aligned with enum)
        String statusStr = courseFile.getStatus().name();
        if (courseFile.getStatus() == ApprovalStatus.SUBMITTED) {
            statusStr = "SUBMITTED";
        } else if (courseFile.getStatus() == ApprovalStatus.UNDER_REVIEW_HOD) {
            statusStr = "UNDER_REVIEW_HOD";
        } else if (courseFile.getStatus() == ApprovalStatus.REJECTED) {
            statusStr = "REJECTED";
        } else if (courseFile.getStatus() == ApprovalStatus.APPROVED) {
            statusStr = "APPROVED";
        } else if (courseFile.getStatus() == ApprovalStatus.FINAL_APPROVED) {
            statusStr = "FINAL_APPROVED";
        }

        response.put("status", statusStr);

        if (courseFile.getCourse() != null) {
            response.put("courseCode", courseFile.getCourse().getCode());
            response.put("courseTitle", courseFile.getCourse().getTitle());
            Map<String, Object> courseMap = new HashMap<>();
            courseMap.put("id", courseFile.getCourse().getId());
            courseMap.put("code", courseFile.getCourse().getCode());
            courseMap.put("title", courseFile.getCourse().getTitle());
            response.put("course", courseMap);
        }

        if (courseFile.getCreatedBy() != null) {
            response.put("teacherName", courseFile.getCreatedBy().getName());
            Map<String, Object> teacherMap = new HashMap<>();
            teacherMap.put("id", courseFile.getCreatedBy().getId());
            teacherMap.put("name", courseFile.getCreatedBy().getName());
            response.put("createdBy", teacherMap);
        }

        response.put("academicYear", courseFile.getAcademicYear());
        response.put("section", courseFile.getSection());
        response.put("revisionNumber", courseFile.getRevisionNumber());
        response.put("submittedDate", courseFile.getCreatedAt());
        response.put("createdAt", courseFile.getCreatedAt());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/course-file/{courseFileId}/tree")
    public ResponseEntity<List<Map<String, Object>>> getTreeStructure(
            @PathVariable("courseFileId") Long courseFileId,
            @AuthenticationPrincipal CustomUserDetails user) {

        // SECURITY: Validate access before fetching tree
        courseFileService.validateAccess(courseFileId, user);

        // HIGH PERFORMANCE: Fetch everything in 2 queries instead of recursive N+1
        List<Heading> allHeadings = headingService.getAllHeadingsForFile(courseFileId);
        List<Document> allDocs = documentService.getDocumentsByCourseFile(courseFileId);

        String role = user.getRole() != null ? user.getRole().name() : "TEACHER";

        // 1. Group documents by heading ID and filter by role
        Map<Long, List<Map<String, Object>>> docsByHeading = new HashMap<>();
        for (Document doc : allDocs) {
            if (doc.getHeading() == null)
                continue;

            boolean visible = false;
            if (!Boolean.FALSE.equals(doc.getIsActive())) {
                if (role.equals("TEACHER")) {
                    visible = true;
                } else if (role.equals("SUBJECTHEAD") || role.equals("HOD")) {
                    visible = doc.getStatus() == DocumentStatus.PENDING_REVIEW
                            || doc.getStatus() == DocumentStatus.APPROVED
                            || doc.getStatus() == DocumentStatus.REJECTED
                            || doc.getStatus() == DocumentStatus.CHANGES_REQUESTED;
                } else {
                    visible = true;
                }
            }

            if (visible) {
                Map<String, Object> docMap = new HashMap<>();
                docMap.put("id", doc.getId());
                docMap.put("fileName", doc.getFileName());
                docMap.put("fileSize", doc.getFileSize());
                docMap.put("uploadedAt", doc.getUploadedAt());
                docMap.put("type", doc.getType());
                docMap.put("status", doc.getStatus());
                docMap.put("versionNo", doc.getVersionNo());
                docMap.put("teacherNote", doc.getTeacherNote());
                docMap.put("reviewerFeedback", doc.getReviewerFeedback());
                docMap.put("isActive", doc.getIsActive());
                docMap.put("isResubmitted", doc.getIsResubmitted());
                if (doc.getReviewedBy() != null) {
                    docMap.put("reviewedByName", doc.getReviewedBy().getName());
                }

                docsByHeading.computeIfAbsent(doc.getHeading().getId(), k -> new ArrayList<>()).add(docMap);
            }
        }

        // 2. Index headings by ID and organize by parent ID
        Map<Long, Map<String, Object>> nodesById = new HashMap<>();
        Map<Long, List<Map<String, Object>>> childrenByParent = new HashMap<>();
        List<Map<String, Object>> rootNodes = new ArrayList<>();

        for (Heading h : allHeadings) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", h.getId());
            node.put("title", h.getTitle());
            node.put("orderIndex", h.getOrderIndex());
            node.put("children", new ArrayList<>());
            node.put("documents", docsByHeading.getOrDefault(h.getId(), new ArrayList<>()));

            nodesById.put(h.getId(), node);

            Long pid = (h.getParentHeading() != null) ? h.getParentHeading().getId() : null;
            if (pid == null) {
                rootNodes.add(node);
            } else {
                childrenByParent.computeIfAbsent(pid, k -> new ArrayList<>()).add(node);
            }
        }

        // 3. Assemble the tree in memory
        for (Heading h : allHeadings) {
            Map<String, Object> parentNode = nodesById.get(h.getId());
            List<Map<String, Object>> children = childrenByParent.get(h.getId());
            if (children != null) {
                children.sort(Comparator.comparingInt(o -> (Integer) o.get("orderIndex")));
                parentNode.put("children", children);
            }
        }

        rootNodes.sort(Comparator.comparingInt(o -> (Integer) o.get("orderIndex")));
        return ResponseEntity.ok(rootNodes);
    }

    @PostMapping("/course-file/{id}/submit")
    public ResponseEntity<CourseFile> submitCourseFile(@PathVariable("id") Long id) {
        return ResponseEntity.ok(courseFileService.submitFile(id));
    }

    @PostMapping("/course-file/{id}/approve/subject-head")
    public ResponseEntity<CourseFile> approveBySubjectHead(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(courseFileService.approveFile(id, user.getTeacher().getId()));
    }

    @PostMapping("/course-file/{id}/approve/hod")
    public ResponseEntity<CourseFile> approveByHod(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(courseFileService.approveFile(id, user.getTeacher().getId()));
    }

    @PostMapping("/course-file/{id}/reject")
    public ResponseEntity<CourseFile> rejectCourseFile(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails user) {
        String reason = body.get("reason");
        String role = user.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .filter(r -> r.equals("HOD") || r.equals("SUBJECTHEAD") || r.equals("SUBJECT_HEAD"))
                .findFirst()
                .orElse("TEACHER");
        return ResponseEntity.ok(courseFileService.rejectFile(id, reason, role, user.getTeacher().getId()));
    }
}
