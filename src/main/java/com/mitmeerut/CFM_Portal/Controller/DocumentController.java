package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Model.Document;
import com.mitmeerut.CFM_Portal.Service.DocumentService;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@RestController
@RequestMapping("/api/teacher/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final com.mitmeerut.CFM_Portal.Service.CourseFileService courseFileService;

    @Autowired
    public DocumentController(DocumentService documentService,
            com.mitmeerut.CFM_Portal.Service.CourseFileService courseFileService) {
        this.documentService = documentService;
        this.courseFileService = courseFileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("headingId") Long headingId,
            @RequestParam("courseCode") String courseCode,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails user) {
        Long teacherId = user.getTeacher().getId();
        Document document = documentService.uploadDocument(headingId, file, teacherId, courseCode);

        Map<String, Object> response = new HashMap<>();
        response.put("id", document.getId());
        response.put("fileName", document.getFileName());
        response.put("fileSize", document.getFileSize());
        response.put("uploadedAt", document.getUploadedAt());
        response.put("type", document.getType());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/resubmit")
    public ResponseEntity<Map<String, Object>> resubmitDocument(
            @PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("teacherNote") String teacherNote,
            @AuthenticationPrincipal CustomUserDetails user) {
        Long teacherId = user.getTeacher().getId();
        Document document = documentService.resubmitDocument(id, file, teacherNote, teacherId);

        Map<String, Object> response = new HashMap<>();
        response.put("id", document.getId());
        response.put("fileName", document.getFileName());
        response.put("status", document.getStatus());
        response.put("teacherNote", document.getTeacherNote());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDocument(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        try {
            documentService.deleteDocument(id, user.getTeacher().getId());
            Map<String, String> response = new HashMap<>();
            response.put("message", "Document deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/heading/{headingId}")
    public ResponseEntity<List<Map<String, Object>>> getDocumentsByHeading(
            @PathVariable("headingId") Long headingId,
            @AuthenticationPrincipal CustomUserDetails user) {

        String role = user.getRole() != null ? user.getRole().name() : "TEACHER";

        List<Document> documents = documentService.getDocumentsByHeadingAndRole(headingId, role);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Document doc : documents) {
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
            result.add(docMap);
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<Map<String, String>> submitForReview(@PathVariable("id") Long id) {
        documentService.submitForReview(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Document submitted for review successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, String>> approveDocument(
            @PathVariable("id") Long id,
            @RequestParam(value = "feedback", defaultValue = "Approved") String feedback,
            @AuthenticationPrincipal CustomUserDetails user) {
        Long reviewerId = user.getTeacher().getId();
        documentService.approveDocument(id, reviewerId, feedback);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Document approved successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/request-changes")
    public ResponseEntity<Map<String, String>> requestChanges(
            @PathVariable("id") Long id,
            @RequestParam("feedback") String feedback,
            @AuthenticationPrincipal CustomUserDetails user) {
        Long reviewerId = user.getTeacher().getId();
        documentService.requestChanges(id, reviewerId, feedback);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Changes requested successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, String>> rejectDocument(
            @PathVariable("id") Long id,
            @RequestParam("feedback") String feedback,
            @AuthenticationPrincipal CustomUserDetails user) {
        Long reviewerId = user.getTeacher().getId();
        documentService.rejectDocument(id, reviewerId, feedback);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Document rejected successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails user) throws IOException {
        Document document = documentService.getDocumentById(id);

        // SECURITY CHECK: Validate access to the course file this document belongs to
        if (document.getHeading() != null && document.getHeading().getCourseFile() != null) {
            courseFileService.validateAccess(document.getHeading().getCourseFile(), user);
        }

        Path filePath = documentService.resolvePath(document.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            throw new RuntimeException("File not found on disk at: " + filePath.toAbsolutePath());
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getFileName() + "\"")
                .body(resource);
    }

    @GetMapping("/view/{id}")
    public ResponseEntity<Resource> viewDocument(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails user) throws IOException {
        Document document = documentService.getDocumentById(id);

        // SECURITY CHECK: Validate access to the course file this document belongs to
        if (document.getHeading() != null && document.getHeading().getCourseFile() != null) {
            courseFileService.validateAccess(document.getHeading().getCourseFile(), user);
        }

        Path filePath = documentService.resolvePath(document.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            throw new RuntimeException("File not found on disk at: " + filePath.toAbsolutePath());
        }

        // Determine content type: prioritize stored type, fallback to extension-based
        // detection
        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
        String fileName = document.getFileName().toLowerCase();

        if (document.getType() != null && !document.getType().isEmpty()
                && !document.getType().equals("application/octet-stream")) {
            try {
                contentType = MediaType.parseMediaType(document.getType());
            } catch (Exception e) {
            }
        } else {
            // Fallback for Excel, Word, and common types if type is missing/generic
            if (fileName.endsWith(".pdf"))
                contentType = MediaType.APPLICATION_PDF;
            else if (fileName.endsWith(".xlsx"))
                contentType = MediaType
                        .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            else if (fileName.endsWith(".xls"))
                contentType = MediaType.parseMediaType("application/vnd.ms-excel");
            else if (fileName.endsWith(".docx"))
                contentType = MediaType
                        .parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            else if (fileName.endsWith(".doc"))
                contentType = MediaType.parseMediaType("application/msword");
            else if (fileName.endsWith(".png"))
                contentType = MediaType.IMAGE_PNG;
            else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg"))
                contentType = MediaType.IMAGE_JPEG;
        }

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getFileName() + "\"")
                .body(resource);
    }

}
