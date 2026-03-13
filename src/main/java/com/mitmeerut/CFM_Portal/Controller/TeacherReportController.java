package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Model.*;
import com.mitmeerut.CFM_Portal.Repository.*;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teacher")
@CrossOrigin(origins = { "http://localhost:5000", "http://localhost:5173" }, allowCredentials = "true")
public class TeacherReportController {

    private final CourseFileRepository courseFileRepository;
    private final CourseTeacherRepository courseTeacherRepository;
    private final DocumentRepository documentRepository;
    private final HeadingRepository headingRepository;
    private final ActivityLogRepository activityLogRepository;
    private final NotificationRepository notificationRepository;

    public TeacherReportController(CourseFileRepository courseFileRepository,
            CourseTeacherRepository courseTeacherRepository,
            DocumentRepository documentRepository,
            HeadingRepository headingRepository,
            ActivityLogRepository activityLogRepository,
            NotificationRepository notificationRepository) {
        this.courseFileRepository = courseFileRepository;
        this.courseTeacherRepository = courseTeacherRepository;
        this.documentRepository = documentRepository;
        this.headingRepository = headingRepository;
        this.activityLogRepository = activityLogRepository;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Dashboard Summary for Teacher
     */
    @GetMapping("/dashboard/summary")
    public ResponseEntity<Map<String, Object>> getDashboardSummary(@AuthenticationPrincipal CustomUserDetails user) {
        Teacher teacher = user.getTeacher();
        if (teacher == null)
            return ResponseEntity.status(401).build();

        Map<String, Object> summary = new HashMap<>();

        // 1. My Courses (Assigned this year/semester)
        List<CourseTeacher> assignments = courseTeacherRepository.findByTeacherId(teacher.getId());
        summary.put("myCourses", assignments.size());

        // 2. Course Files (Created by teacher)
        List<CourseFile> teacherFiles = courseFileRepository.findByCreatedById(teacher.getId());
        summary.put("courseFiles", teacherFiles.size());

        // 2.5 Rejected Files
        long rejectedCount = teacherFiles.stream()
                .filter(f -> f.getStatus() == ApprovalStatus.REJECTED)
                .count();
        summary.put("rejectedFiles", rejectedCount);

        // 3. Documents (Total uploads by this teacher)
        long totalUploads = documentRepository.countByUploadedBy_Id(teacher.getId());
        summary.put("totalDocuments", totalUploads);

        // 4. Notifications (Unread) - Using user.getUserId() for notifications list
        long unreadNotifications = notificationRepository.countByUser_IdAndIsReadFalse(user.getUserId());
        summary.put("notifications", unreadNotifications);

        return ResponseEntity.ok(summary);
    }

    /**
     * Weekly Upload Activity (Mon-Sun)
     */
    @GetMapping("/dashboard/upload-activity")
    public ResponseEntity<List<Map<String, Object>>> getWeeklyActivity(
            @AuthenticationPrincipal CustomUserDetails user) {
        Teacher teacher = user.getTeacher();
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7).truncatedTo(ChronoUnit.DAYS);

        // HIGH PERFORMANCE: Fetch all daily counts in ONE query
        List<Object[]> stats = documentRepository.getDailyUploadStats(teacher.getId(), sevenDaysAgo);
        Map<String, Long> countMap = new HashMap<>();

        for (Object[] row : stats) {
            // Functional 'DATE' results vary by driver, usually String or Date
            String dateKey = row[0].toString();
            countMap.put(dateKey, (Long) row[1]);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);

        for (int i = 6; i >= 0; i--) {
            LocalDateTime targetDay = now.minusDays(i);
            String dateStr = targetDay.toLocalDate().toString(); // ISO 8601 YYYY-MM-DD

            Map<String, Object> m = new HashMap<>();
            m.put("day", targetDay.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            m.put("count", countMap.getOrDefault(dateStr, 0L));
            result.add(m);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Recent Activity (Last 5)
     */
    @GetMapping("/dashboard/recent-activity")
    public ResponseEntity<List<Map<String, Object>>> getRecentActivity(
            @AuthenticationPrincipal CustomUserDetails user) {
        Teacher teacher = user.getTeacher();
        // Custom query to get top 5 for teacher
        List<Activity_Log> logs = activityLogRepository.findByActor_Id(teacher.getId());

        return ResponseEntity.ok(logs.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .map(log -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("action", log.getAction());
                    m.put("details", log.getDetails());
                    m.put("timestamp", log.getCreatedAt());
                    return m;
                }).collect(Collectors.toList()));
    }

    /**
     * Completion Progress for each Course File
     */
    @GetMapping("/dashboard/course-progress")
    public ResponseEntity<List<Map<String, Object>>> getCourseProgress(
            @AuthenticationPrincipal CustomUserDetails user) {
        Teacher teacher = user.getTeacher();
        List<CourseFile> files = courseFileRepository.findByCreatedById(teacher.getId());

        if (files.isEmpty())
            return ResponseEntity.ok(Collections.emptyList());

        // HIGH PERFORMANCE: Batch count all headings in ONE query
        List<Long> cfIds = files.stream().map(CourseFile::getId).collect(Collectors.toList());
        List<Object[]> stats = headingRepository.findProgressStatsByCourseFileIds(cfIds);

        Map<Long, Long[]> progressStatsMap = new HashMap<>();
        for (Object[] row : stats) {
            progressStatsMap.put((Long) row[0], new Long[] { (Long) row[1], (Long) row[2] });
        }

        List<Map<String, Object>> progressList = new ArrayList<>();
        for (CourseFile cf : files) {
            Long[] counts = progressStatsMap.get(cf.getId());
            if (counts == null || counts[0] == 0)
                continue;

            long totalHeadings = counts[0];
            long completedHeadings = counts[1];
            int percentage = (int) ((completedHeadings * 100) / totalHeadings);

            Map<String, Object> p = new HashMap<>();
            p.put("courseCode", cf.getCourse().getCode());
            p.put("courseTitle", cf.getCourse().getTitle());
            p.put("progress", percentage);
            p.put("id", cf.getId());
            p.put("status", cf.getStatus().name());

            // Determine Current Location
            String location = "Draft";
            if (cf.getStatus() == ApprovalStatus.SUBMITTED) {
                location = "Subject Head";
            } else if (cf.getStatus() == ApprovalStatus.UNDER_REVIEW_HOD) {
                location = "HOD";
            } else if (cf.getStatus() == ApprovalStatus.APPROVED || cf.getStatus() == ApprovalStatus.FINAL_APPROVED) {
                location = "Fully Approved";
            } else if (cf.getStatus() == ApprovalStatus.REJECTED) {
                location = "Returned (Correction Required)";
            }
            p.put("currentLocation", location);

            progressList.add(p);
        }

        return ResponseEntity.ok(progressList);
    }

    /**
     * Get all currently flagged documents that need teacher attention
     */
    @GetMapping("/dashboard/flagged-items")
    public ResponseEntity<List<Map<String, Object>>> getFlaggedItems(@AuthenticationPrincipal CustomUserDetails user) {
        Teacher teacher = user.getTeacher();
        if (teacher == null)
            return ResponseEntity.status(401).build();

        List<DocumentStatus> flaggedStatuses = Arrays.asList(DocumentStatus.REJECTED, DocumentStatus.CHANGES_REQUESTED);
        List<Document> flaggedDocs = documentRepository.findFlaggedDocumentsByTeacher(teacher.getId(), flaggedStatuses);

        return ResponseEntity.ok(flaggedDocs.stream()
                .map(doc -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("docId", doc.getId());
                    m.put("fileName", doc.getFileName());
                    m.put("status", doc.getStatus());
                    m.put("feedback", doc.getReviewerFeedback());
                    m.put("courseFileId", doc.getHeading().getCourseFile().getId());
                    m.put("courseCode", doc.getHeading().getCourseFile().getCourse().getCode());
                    m.put("headingTitle", doc.getHeading().getTitle());
                    m.put("flaggedAt", doc.getReviewedAt());
                    if (doc.getReviewedBy() != null) {
                        m.put("flaggedBy", doc.getReviewedBy().getName());
                    }
                    return m;
                }).collect(Collectors.toList()));
    }

    /**
     * Monthly Uploads for Reports
     */
    /**
     * Monthly Uploads for Reports (Dynamic Last 6 Months)
     */
    @GetMapping("/reports/monthly-uploads")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyUploads(
            @AuthenticationPrincipal CustomUserDetails user) {
        Teacher teacher = user.getTeacher();
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);

        // HIGH PERFORMANCE: Fetch all monthly counts in ONE query
        List<Object[]> stats = documentRepository.getMonthlyUploadStats(teacher.getId(), sixMonthsAgo);
        Map<String, Long> monthMap = new HashMap<>();

        for (Object[] row : stats) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            long count = ((Number) row[2]).longValue();
            monthMap.put(year + "-" + month, count);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 5; i >= 0; i--) {
            LocalDateTime targetMonth = now.minusMonths(i);
            String key = targetMonth.getYear() + "-" + targetMonth.getMonthValue();

            Map<String, Object> m = new HashMap<>();
            m.put("month", targetMonth.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            m.put("count", monthMap.getOrDefault(key, 0L));
            result.add(m);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * File Type Distribution for Reports
     */
    @GetMapping("/reports/file-types")
    public ResponseEntity<List<Map<String, Object>>> getFileTypeDistribution(
            @AuthenticationPrincipal CustomUserDetails user) {
        Teacher teacher = user.getTeacher();
        List<Document> docs = getTeacherDocuments(teacher);

        Map<String, Long> typeCounts = new HashMap<>();
        typeCounts.put("PDF", 0L);
        typeCounts.put("DOCX", 0L);
        typeCounts.put("Images", 0L);
        typeCounts.put("Other", 0L);

        for (Document doc : docs) {
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
            } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")) {
                typeCounts.put("Images", typeCounts.get("Images") + 1);
            } else {
                typeCounts.put("Other", typeCounts.get("Other") + 1);
            }
        }

        return ResponseEntity.ok(typeCounts.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("type", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                }).collect(Collectors.toList()));
    }

    /**
     * Top Courses by File Count
     */
    @GetMapping("/reports/top-courses")
    public ResponseEntity<List<Map<String, Object>>> getTopCourses(@AuthenticationPrincipal CustomUserDetails user) {
        Teacher teacher = user.getTeacher();
        List<CourseFile> files = courseFileRepository.findByCreatedById(teacher.getId());

        List<Map<String, Object>> stats = new ArrayList<>();
        for (CourseFile cf : files) {
            // optimized: get all headings for this file in one go
            List<Heading> headings = headingRepository.findByCourseFileId(cf.getId());
            long docCount = 0;
            if (!headings.isEmpty()) {
                List<Long> headingIds = headings.stream().map(Heading::getId).collect(Collectors.toList());
                docCount = documentRepository.countByHeading_IdIn(headingIds);
            }

            Map<String, Object> s = new HashMap<>();
            s.put("courseCode", cf.getCourse().getCode());
            s.put("courseTitle", cf.getCourse().getTitle());
            s.put("totalFiles", docCount);
            stats.add(s);
        }

        stats.sort((a, b) -> ((Long) b.get("totalFiles")).compareTo((Long) a.get("totalFiles")));
        return ResponseEntity.ok(stats.stream().limit(5).collect(Collectors.toList()));
    }

    /**
     * Export Teacher Report as CSV
     */
    @GetMapping("/reports/export/csv")
    public void exportCSV(@AuthenticationPrincipal CustomUserDetails user,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        Teacher teacher = user.getTeacher();
        response.setContentType("text/csv");
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
        response.setHeader("Content-Disposition", "attachment; filename=teacher_report_" + timestamp + ".csv");

        java.io.PrintWriter writer = response.getWriter();
        writer.println("Teacher: " + (teacher != null ? teacher.getName() : user.getUsername()));
        writer.println("Generated: " + java.time.LocalDateTime.now());
        writer.println();

        // 1. Monthly Uploads
        writer.println("MONTHLY UPLOADS (Jan-Jun)");
        writer.println("Month,Upload Count");
        List<Map<String, Object>> monthly = getMonthlyUploads(user).getBody();
        if (monthly != null) {
            for (Map<String, Object> m : monthly) {
                writer.println(m.get("month") + "," + m.get("count"));
            }
        }
        writer.println();

        // 2. File Types
        writer.println("FILE TYPE DISTRIBUTION");
        writer.println("Type,Count");
        List<Map<String, Object>> types = getFileTypeDistribution(user).getBody();
        if (types != null) {
            for (Map<String, Object> t : types) {
                writer.println(t.get("type") + "," + t.get("count"));
            }
        }
        writer.println();

        // 3. Top Courses
        writer.println("COURSE-WISE FILE COUNT");
        writer.println("Course Code,Course Title,Total Files");
        List<Map<String, Object>> courses = getTopCourses(user).getBody();
        if (courses != null) {
            for (Map<String, Object> c : courses) {
                writer.println(
                        c.get("courseCode") + "," + "\"" + c.get("courseTitle") + "\"" + "," + c.get("totalFiles"));
            }
        }

        writer.flush();
        writer.close();
    }

    /**
     * Export Teacher Report as PDF
     */
    @GetMapping("/reports/export/pdf")
    public void exportPDF(@AuthenticationPrincipal CustomUserDetails user,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        Teacher teacher = user.getTeacher();
        response.setContentType("application/pdf");
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
        response.setHeader("Content-Disposition", "attachment; filename=teacher_report_" + timestamp + ".pdf");

        com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4);
        com.lowagie.text.pdf.PdfWriter.getInstance(document, response.getOutputStream());

        document.open();

        // Title
        com.lowagie.text.Font fontTitle = com.lowagie.text.FontFactory
                .getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 18);
        com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("Teacher Performance & Analytics Report",
                fontTitle);
        title.setAlignment(com.lowagie.text.Paragraph.ALIGN_CENTER);
        document.add(title);

        document.add(new com.lowagie.text.Paragraph(" \n"));

        // Metadata
        com.lowagie.text.Font fontInfo = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA,
                10);
        document.add(new com.lowagie.text.Paragraph(
                "Teacher: " + (teacher != null ? teacher.getName() : user.getUsername()), fontInfo));
        document.add(new com.lowagie.text.Paragraph("Department: "
                + (teacher != null && teacher.getDepartment() != null ? teacher.getDepartment().getName() : "N/A"),
                fontInfo));
        document.add(
                new com.lowagie.text.Paragraph(
                        "Date: " + java.time.LocalDateTime.now()
                                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        fontInfo));

        document.add(new com.lowagie.text.Paragraph(" \n"));

        // Dashboard Summary Table
        com.lowagie.text.pdf.PdfPTable summaryTable = new com.lowagie.text.pdf.PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingBefore(10);

        Map<String, Object> summary = getDashboardSummary(user).getBody();
        if (summary != null) {
            summaryTable.addCell("Metric");
            summaryTable.addCell("Value");
            summaryTable.addCell("My Courses");
            summaryTable.addCell(summary.get("myCourses").toString());
            summaryTable.addCell("Course Files");
            summaryTable.addCell(summary.get("courseFiles").toString());
            summaryTable.addCell("Total Uploads");
            summaryTable.addCell(summary.get("totalDocuments").toString());
        }
        document.add(summaryTable);

        // Sections
        document.add(new com.lowagie.text.Paragraph("\nCourse Statistics (Top 5 Active Courses):",
                com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 12)));
        com.lowagie.text.pdf.PdfPTable courseTable = new com.lowagie.text.pdf.PdfPTable(3);
        courseTable.setWidthPercentage(100);
        courseTable.setSpacingBefore(5);
        courseTable.addCell("Code");
        courseTable.addCell("Title");
        courseTable.addCell("Uploads");

        List<Map<String, Object>> courses = getTopCourses(user).getBody();
        if (courses != null) {
            for (Map<String, Object> c : courses) {
                courseTable.addCell(c.get("courseCode").toString());
                courseTable.addCell(c.get("courseTitle").toString());
                courseTable.addCell(c.get("totalFiles").toString());
            }
        }
        document.add(courseTable);

        document.close();
    }

    // --- Helpers ---

    private List<Document> getTeacherDocuments(Teacher teacher) {
        return documentRepository.findByUploadedBy_Id(teacher.getId());
    }
}
