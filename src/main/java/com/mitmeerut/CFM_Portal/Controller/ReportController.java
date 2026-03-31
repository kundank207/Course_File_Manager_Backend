package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Model.ApprovalStatus;
import com.mitmeerut.CFM_Portal.Model.*;
import com.mitmeerut.CFM_Portal.Repository.*;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")

public class ReportController {

    private final CourseFileRepository courseFileRepository;
    private final DocumentRepository documentRepository;

    public ReportController(CourseFileRepository courseFileRepository,
            DocumentRepository documentRepository) {
        this.courseFileRepository = courseFileRepository;
        this.documentRepository = documentRepository;
    }

    /**
     * Get summary metrics for Subject Head
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getReportSummary(@AuthenticationPrincipal CustomUserDetails user) {
        Teacher teacher = user.getTeacher();
        if (teacher == null)
            return ResponseEntity.status(401).build();

        List<CourseFile> supervisedFiles = courseFileRepository.findAllForSubjectHead(teacher.getId());

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalFiles", supervisedFiles.size());
        summary.put("pendingFiles", supervisedFiles.stream()
                .filter(f -> f.getStatus() == ApprovalStatus.SUBMITTED)
                .count());
        summary.put("approvedFiles", supervisedFiles.stream()
                .filter(f -> f.getStatus() == ApprovalStatus.APPROVED || f.getStatus() == ApprovalStatus.FINAL_APPROVED)
                .count());

        // Count documents across all supervised files in one go
        if (supervisedFiles.isEmpty()) {
            summary.put("totalUploads", 0);
        } else {
            List<Long> cfIds = supervisedFiles.stream().map(CourseFile::getId).collect(Collectors.toList());
            summary.put("totalUploads", documentRepository.countByCourseFileIdIn(cfIds));
        }

        return ResponseEntity.ok(summary);
    }

    /**
     * Get monthly uploads (last 6 months)
     */
    /**
     * Get monthly uploads (Dynamic Last 6 Months)
     */
    @GetMapping("/monthly-uploads")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyUploads(
            @AuthenticationPrincipal CustomUserDetails user) {
        Teacher teacher = user.getTeacher();
        List<CourseFile> supervisedFiles = courseFileRepository.findAllForSubjectHead(teacher.getId());

        if (supervisedFiles.isEmpty())
            return ResponseEntity.ok(Collections.emptyList());

        // Get all documents for these files in ONE query
        List<Long> cfIds = supervisedFiles.stream().map(CourseFile::getId).collect(Collectors.toList());
        List<Document> documents = documentRepository.findByCourseFileIdIn(cfIds);

        // Dynamic last 6 months
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        Map<String, Long> monthlyCounts = new LinkedHashMap<>();

        for (int i = 5; i >= 0; i--) {
            String monthName = now.minusMonths(i).getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            monthlyCounts.put(monthName, 0L);
        }

        java.time.LocalDateTime sixMonthsAgo = now.minusMonths(6);
        for (Document doc : documents) {
            if (doc.getUploadedAt().isAfter(sixMonthsAgo)) {
                String monthName = doc.getUploadedAt().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                if (monthlyCounts.containsKey(monthName)) {
                    monthlyCounts.put(monthName, monthlyCounts.get(monthName) + 1);
                }
            }
        }

        List<Map<String, Object>> result = monthlyCounts.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("month", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Get distribution of file types
     */
    @GetMapping("/file-types")
    public ResponseEntity<List<Map<String, Object>>> getFileTypeDistribution(
            @AuthenticationPrincipal CustomUserDetails user) {
        Teacher teacher = user.getTeacher();
        List<CourseFile> supervisedFiles = courseFileRepository.findAllForSubjectHead(teacher.getId());

        if (supervisedFiles.isEmpty())
            return ResponseEntity.ok(Collections.emptyList());

        List<Long> cfIds = supervisedFiles.stream().map(CourseFile::getId).collect(Collectors.toList());
        List<Document> documents = documentRepository.findByCourseFileIdIn(cfIds);

        Map<String, Long> typeCounts = new HashMap<>();
        typeCounts.put("PDF", 0L);
        typeCounts.put("DOCX", 0L);
        typeCounts.put("Images", 0L);
        typeCounts.put("Other", 0L);

        for (Document doc : documents) {
            String fileName = doc.getFileName();
            if (fileName == null) {
                typeCounts.put("Other", typeCounts.get("Other") + 1);
                continue;
            }
            fileName = fileName.toLowerCase();
            if (fileName.endsWith(".pdf")) {
                typeCounts.put("PDF", typeCounts.get("PDF") + 1);
            } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
                typeCounts.put("DOCX", typeCounts.get("DOCX") + 1);
            } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")
                    || fileName.endsWith(".gif")) {
                typeCounts.put("Images", typeCounts.get("Images") + 1);
            } else {
                typeCounts.put("Other", typeCounts.get("Other") + 1);
            }
        }

        List<Map<String, Object>> result = typeCounts.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("type", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Get top courses by content volume
     */
    @GetMapping("/top-courses")
    public ResponseEntity<List<Map<String, Object>>> getTopCourses(@AuthenticationPrincipal CustomUserDetails user) {
        Teacher teacher = user.getTeacher();
        List<CourseFile> supervisedFiles = courseFileRepository.findAllForSubjectHead(teacher.getId());

        if (supervisedFiles.isEmpty())
            return ResponseEntity.ok(Collections.emptyList());

        List<Long> cfIds = supervisedFiles.stream().map(CourseFile::getId).collect(Collectors.toList());
        List<Document> allDocs = documentRepository.findByCourseFileIdIn(cfIds);

        // Group documents by course file ID in memory
        Map<Long, Long> docCountsByCf = allDocs.stream()
                .collect(Collectors.groupingBy(d -> d.getHeading().getCourseFile().getId(), Collectors.counting()));

        List<Map<String, Object>> courseStats = new ArrayList<>();
        for (CourseFile cf : supervisedFiles) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("courseCode", cf.getCourse().getCode());
            stat.put("courseTitle", cf.getCourse().getTitle());
            stat.put("uploads", docCountsByCf.getOrDefault(cf.getId(), 0L));
            courseStats.add(stat);
        }

        // Sort by uploads descending
        courseStats.sort((a, b) -> ((Long) b.get("uploads")).compareTo((Long) a.get("uploads")));

        return ResponseEntity.ok(courseStats.stream().limit(5).collect(Collectors.toList()));
    }

    // --- Helper Methods (Deprecated/Cleaned up) ---
}
