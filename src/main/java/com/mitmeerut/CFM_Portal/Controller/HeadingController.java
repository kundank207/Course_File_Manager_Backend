package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Model.Heading;
import com.mitmeerut.CFM_Portal.Model.Document;
import com.mitmeerut.CFM_Portal.Model.DocumentStatus;
import com.mitmeerut.CFM_Portal.Service.HeadingService;
import com.mitmeerut.CFM_Portal.Service.DocumentService;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/teacher/headings")

public class HeadingController {

    private final HeadingService headingService;
    private final DocumentService documentService;

    @Autowired
    public HeadingController(HeadingService headingService, DocumentService documentService) {
        this.headingService = headingService;
        this.documentService = documentService;
    }

    // DTO for creating heading
    public static class CreateHeadingRequest {
        public Long courseFileId;
        public Long parentHeadingId;
        public String title;
        public Integer orderIndex;
    }

    // DTO for updating heading
    public static class UpdateHeadingRequest {
        public String title;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createHeading(
            @RequestBody CreateHeadingRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        Heading heading = headingService.createHeading(
                request.courseFileId,
                request.parentHeadingId,
                request.title,
                request.orderIndex,
                user.getTeacher().getId());
        return ResponseEntity.ok(headingToMap(heading));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateHeading(
            @PathVariable("id") Long id,
            @RequestBody UpdateHeadingRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        Heading heading = headingService.updateHeading(id, request.title, user.getTeacher().getId());
        return ResponseEntity.ok(headingToMap(heading));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteHeading(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        try {
            headingService.deleteHeading(id, user.getTeacher().getId());
            Map<String, String> response = new HashMap<>();
            response.put("message", "Heading deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Get full tree structure for a course file
    @GetMapping("/course-file/{courseFileId}/tree")
    public ResponseEntity<List<Map<String, Object>>> getTreeStructure(
            @PathVariable("courseFileId") Long courseFileId,
            @AuthenticationPrincipal CustomUserDetails user) {

        // HIGH PERFORMANCE: Fetch everything in 2 queries instead of recursive N+1
        List<Heading> allHeadings = headingService.getAllHeadingsForFile(courseFileId);
        List<Document> allDocs = documentService.getDocumentsByCourseFile(courseFileId);

        String role = user.getRole() != null ? user.getRole().name() : "TEACHER";

        // 1. Group documents by heading ID and filter by role
        Map<Long, List<Map<String, Object>>> docsByHeading = new HashMap<>();
        Set<Long> headingsWithIssues = new HashSet<>();
        for (Document doc : allDocs) {
            if (doc.getHeading() == null)
                continue;

            boolean visible = false;
            // SMART RECOVERY LOGIC:
            // Track headings that have issues (Rejected, Requested Changes, or has
            // Feedback)
            // Even if the file is currently inactive (deleted), we track the history.
            boolean hasIssue = doc.getStatus() == DocumentStatus.REJECTED ||
                    doc.getStatus() == DocumentStatus.CHANGES_REQUESTED ||
                    (doc.getReviewerFeedback() != null && !doc.getReviewerFeedback().trim().isEmpty());

            if (hasIssue && doc.getHeading() != null) {
                Heading current = doc.getHeading();
                while (current != null) {
                    headingsWithIssues.add(current.getId());
                    current = current.getParentHeading();
                }
            }

            // Only show active documents for all roles in the professional tree view
            if (!Boolean.FALSE.equals(doc.getIsActive())) {
                if (role.equals("TEACHER")) {
                    visible = true; // Teacher sees all active versions
                } else if (role.equals("SUBJECTHEAD")) {
                    visible = doc.getStatus() == DocumentStatus.PENDING_REVIEW
                            || doc.getStatus() == DocumentStatus.APPROVED;
                } else if (role.equals("HOD")) {
                    visible = doc.getStatus() == DocumentStatus.PENDING_REVIEW
                            || doc.getStatus() == DocumentStatus.APPROVED;
                } else {
                    visible = true;
                }
            }

            if (visible) {
                docsByHeading.computeIfAbsent(doc.getHeading().getId(), k -> new ArrayList<>())
                        .add(documentToSimpleMap(doc));
            }
        }

        // 2. Index headings by ID and organize by parent ID
        Map<Long, Map<String, Object>> nodesById = new HashMap<>();
        Map<Long, List<Map<String, Object>>> childrenByParent = new HashMap<>();
        List<Map<String, Object>> rootNodes = new ArrayList<>();

        for (Heading h : allHeadings) {
            Map<String, Object> node = headingToMap(h);
            node.put("children", new ArrayList<>());
            node.put("files", docsByHeading.getOrDefault(h.getId(), new ArrayList<>()));
            node.put("needsFix", headingsWithIssues.contains(h.getId())); // Signal to UI to keep upload active

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
                // Sort children by order index
                children.sort(Comparator.comparingInt(o -> (Integer) o.get("orderIndex")));
                parentNode.put("children", children);
            }
        }

        // 4. Sort root nodes
        rootNodes.sort(Comparator.comparingInt(o -> (Integer) o.get("orderIndex")));

        return ResponseEntity.ok(rootNodes);
    }

    private Map<String, Object> documentToSimpleMap(Document doc) {
        Map<String, Object> docMap = new HashMap<>();
        docMap.put("id", doc.getId());
        docMap.put("fileName", doc.getFileName());
        docMap.put("fileSize", doc.getFileSize());
        docMap.put("uploadedAt", doc.getUploadedAt());
        docMap.put("type", doc.getType());
        docMap.put("status", doc.getStatus());
        docMap.put("versionNo", doc.getVersionNo());
        docMap.put("submittedAt", doc.getSubmittedAt());
        docMap.put("reviewDeadline", doc.getReviewDeadline());
        docMap.put("reviewedAt", doc.getReviewedAt());
        docMap.put("isActive", doc.getIsActive());
        docMap.put("teacherNote", doc.getTeacherNote());
        docMap.put("isResubmitted", doc.getIsResubmitted());
        docMap.put("reviewerFeedback", doc.getReviewerFeedback());
        if (doc.getReviewedBy() != null) {
            docMap.put("reviewedByName", doc.getReviewedBy().getName());
        }
        return docMap;
    }

    private Map<String, Object> headingToMap(Heading heading) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", heading.getId());
        map.put("title", heading.getTitle());
        map.put("orderIndex", heading.getOrderIndex());
        map.put("courseFileId", heading.getCourseFile() != null ? heading.getCourseFile().getId() : null);
        map.put("parentHeadingId", heading.getParentHeading() != null ? heading.getParentHeading().getId() : null);
        map.put("createdAt", heading.getCreatedAt());
        return map;
    }
}
