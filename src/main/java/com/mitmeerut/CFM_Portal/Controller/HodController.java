package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Model.ApprovalStatus;
import com.mitmeerut.CFM_Portal.Model.*;
import com.mitmeerut.CFM_Portal.Repository.*;
import com.mitmeerut.CFM_Portal.Service.EmailService;
import com.mitmeerut.CFM_Portal.Service.CourseFileService;

import com.mitmeerut.CFM_Portal.dto.HODOverviewDTO;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hod")
@CrossOrigin(origins = { "http://localhost:5000", "http://localhost:5173" }, allowCredentials = "true")
@PreAuthorize("hasRole('HOD')")
public class HodController {

    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;
    private final CourseFileRepository courseFileRepository;
    private final CourseTeacherRepository courseTeacherRepository;
    private final DocumentRepository documentRepository;
    private final HeadingRepository headingRepository;
    private final ActivityLogRepository activityLogRepository;
    private final AppreciationRepository appreciationRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final TemplateRepository templateRepository;
    private final CourseFileService courseFileService;

    public HodController(TeacherRepository teacherRepository,
            CourseRepository courseRepository,
            CourseFileRepository courseFileRepository,
            CourseTeacherRepository courseTeacherRepository,
            DocumentRepository documentRepository,
            HeadingRepository headingRepository,
            ActivityLogRepository activityLogRepository,
            AppreciationRepository appreciationRepository,
            EmailService emailService,
            UserRepository userRepository,
            NotificationRepository notificationRepository,
            TemplateRepository templateRepository,
            CourseFileService courseFileService) {
        this.teacherRepository = teacherRepository;
        this.courseRepository = courseRepository;
        this.courseFileRepository = courseFileRepository;
        this.courseTeacherRepository = courseTeacherRepository;
        this.documentRepository = documentRepository;
        this.headingRepository = headingRepository;
        this.activityLogRepository = activityLogRepository;
        this.appreciationRepository = appreciationRepository;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.templateRepository = templateRepository;
        this.courseFileService = courseFileService;
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    @GetMapping("/debug/counts")
    public ResponseEntity<?> getDebugCounts(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long deptId = userDetails.getDepartmentId();
        Map<String, Object> debug = new HashMap<>();
        debug.put("your_dept_id", deptId);
        debug.put("dept_teachers", teacherRepository.findByDepartmentId(deptId).size());
        debug.put("dept_courses", courseRepository.findCoursesByDepartmentId(deptId).size());
        debug.put("global_teachers", teacherRepository.count());
        debug.put("global_courses", courseRepository.count());
        debug.put("global_users", userRepository.count());

        // Check if HOD's own teacher record has the correct dept
        if (userDetails.getTeacher() != null) {
            debug.put("hod_teacher_id", userDetails.getTeacher().getId());
            debug.put("hod_teacher_dept_id",
                    userDetails.getTeacher().getDepartment() != null ? userDetails.getTeacher().getDepartment().getId()
                            : "NULL");
        }

        return ResponseEntity.ok(debug);
    }

    @GetMapping("/pending-approvals")
    public ResponseEntity<List<Map<String, Object>>> getPendingApprovals(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long deptId = userDetails.getDepartmentId();
        if (deptId == null)
            return ResponseEntity.badRequest().build();

        List<CourseFile> pendingFiles = courseFileRepository.findCourseFilesByDepartmentIdAndStatus(deptId,
                ApprovalStatus.UNDER_REVIEW_HOD);

        // Pre-fetch IDs of course files with inactive documents to avoid N+1
        List<Long> fileIds = pendingFiles.stream().map(CourseFile::getId).collect(Collectors.toList());
        List<Long> filesWithIncorrect = fileIds.isEmpty() ? List.of()
                : documentRepository.findCourseFileIdsWithInactiveDocuments(fileIds);

        List<Map<String, Object>> result = pendingFiles.stream().map(cf -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", cf.getId());
            map.put("courseFileId", cf.getId());
            map.put("academicYear", cf.getAcademicYear());
            map.put("section", cf.getSection());
            map.put("courseCode", cf.getCourse() != null ? cf.getCourse().getCode() : "N/A");
            map.put("courseTitle", cf.getCourse() != null ? cf.getCourse().getTitle() : "N/A");
            map.put("teacherName", cf.getCreatedBy() != null ? cf.getCreatedBy().getName() : "N/A");
            map.put("submittedDate", cf.getCreatedAt());
            map.put("status", cf.getStatus().name());

            // Identify Stage and Forwarder
            if (cf.getStatus() == ApprovalStatus.SUBMITTED) {
                map.put("stage", "Wait for Subject Head");
                map.put("forwardedBy", "Awaiting SH");
            } else if (cf.getStatus() == ApprovalStatus.UNDER_REVIEW_HOD) {
                map.put("stage", "Final Approval (HOD)");
                String systemComment = cf.getSystemComment() != null ? cf.getSystemComment() : "";

                if (systemComment.startsWith("FORWARDED_BY_SH: ")) {
                    map.put("forwardedBy", systemComment.substring(17));
                } else if (systemComment.contains("SYSTEM_DIRECT")) {
                    map.put("forwardedBy", "System (No SH)");
                } else if (systemComment.contains("Reviewed and forwarded by Subject Head")) {
                    map.put("forwardedBy", "Subject Head");
                } else {
                    map.put("forwardedBy", "System");
                }
            }

            if (cf.getCourse() != null) {
                map.put("course", Map.of(
                        "id", cf.getCourse().getId(),
                        "code", cf.getCourse().getCode(),
                        "title", cf.getCourse().getTitle()));
            }
            if (cf.getCreatedBy() != null) {
                map.put("createdBy", Map.of(
                        "id", cf.getCreatedBy().getId(),
                        "name", cf.getCreatedBy().getName()));
            }

            // Batched check for incorrect files
            map.put("hasIncorrectFiles", filesWithIncorrect.contains(cf.getId()));

            // Add version count for comparison logic
            long vCount = courseFileService.getVersionCount(cf.getCourse().getId(), cf.getAcademicYear(),
                    cf.getSection());
            map.put("versionCount", vCount);
            map.put("revisionNumber", cf.getRevisionNumber());

            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/approved-records")
    public ResponseEntity<List<Map<String, Object>>> getApprovedRecords(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long deptId = userDetails.getDepartmentId();
        if (deptId == null)
            return ResponseEntity.badRequest().build();

        List<ApprovalStatus> approvedStatuses = List.of(ApprovalStatus.APPROVED, ApprovalStatus.FINAL_APPROVED,
                ApprovalStatus.ARCHIVED);
        List<CourseFile> approvedFiles = courseFileRepository.findCourseFilesByDepartmentIdAndStatusIn(deptId,
                approvedStatuses);

        // Pre-fetch IDs of course files with inactive documents to avoid N+1
        List<Long> fileIds = approvedFiles.stream().map(CourseFile::getId).collect(Collectors.toList());
        List<Long> filesWithIncorrect = fileIds.isEmpty() ? List.of()
                : documentRepository.findCourseFileIdsWithInactiveDocuments(fileIds);

        List<Map<String, Object>> result = approvedFiles.stream().map(cf -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", cf.getId());
            map.put("courseFileId", cf.getId());
            map.put("academicYear", cf.getAcademicYear());
            map.put("section", cf.getSection());
            map.put("courseCode", cf.getCourse() != null ? cf.getCourse().getCode() : "N/A");
            map.put("courseTitle", cf.getCourse() != null ? cf.getCourse().getTitle() : "N/A");
            map.put("teacherName", cf.getCreatedBy() != null ? cf.getCreatedBy().getName() : "N/A");
            map.put("approvedDate", cf.getUpdatedAt()); // Using updatedAt as simplified approved date
            map.put("status", cf.getStatus().name());
            map.put("revisionNumber", cf.getRevisionNumber());

            // Batched check for incorrect files
            map.put("hasIncorrectFiles", filesWithIncorrect.contains(cf.getId()));

            // Add version count for comparison logic
            long vCount = courseFileService.getVersionCount(cf.getCourse().getId(), cf.getAcademicYear(),
                    cf.getSection());
            map.put("versionCount", vCount);

            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/approvals/{courseFileId}/approve")
    public ResponseEntity<Map<String, Object>> hodApprove(
            @PathVariable("courseFileId") Long courseFileId,
            @AuthenticationPrincipal CustomUserDetails user) {
        courseFileService.approveFile(courseFileId, user.getTeacher().getId());
        return ResponseEntity.ok(Map.of("message", "Final approval granted"));
    }

    @PostMapping("/approvals/{courseFileId}/return")
    public ResponseEntity<Map<String, Object>> hodReturn(
            @PathVariable("courseFileId") Long courseFileId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails user) {
        String comment = body.get("comment");
        courseFileService.rejectFile(courseFileId, comment, "HOD", user.getTeacher().getId());
        return ResponseEntity.ok(Map.of("message", "Returned to teacher by HOD"));
    }

    /**
     * Dashboard Overview (Counts, Charts, etc.)
     */
    @GetMapping("/overview")
    public ResponseEntity<?> getOverview(@AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Long deptId = userDetails.getDepartmentId();

            // Fallback: If principal is stale, fetch from DB
            if (deptId == null) {
                User u = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
                if (u != null && u.getTeacher() != null && u.getTeacher().getDepartment() != null) {
                    deptId = u.getTeacher().getDepartment().getId();
                }
            }

            if (deptId == null) {
                System.err.println("HOD Dashboard Error: deptId is null for user " + userDetails.getUsername());
                return ResponseEntity.badRequest().body(
                        "No department found for this HOD profile. Please check if your Teacher record is linked to a Department.");
            }

            System.out.println("Loading Optimized HOD Overview for Dept ID: " + deptId);

            long teacherCount = teacherRepository.countByDepartmentId(deptId);
            long courseCount = courseRepository.countCoursesByDepartmentId(deptId);
            long pending = courseFileRepository.countCourseFilesByDepartmentIdAndStatus(deptId,
                    ApprovalStatus.UNDER_REVIEW_HOD);

            System.out.println("DEBUG HOD: Optimized counts - Teachers: " + teacherCount + ", Courses: " + courseCount
                    + ", Pending: " + pending);

            // Course Distribution (Uses a more efficient repository query if possible, but
            // keep simple for now)
            List<Course> courses = courseRepository.findCoursesByDepartmentId(deptId);
            Map<String, Long> distribution = courses.stream()
                    .collect(Collectors.groupingBy(c -> c.getProgram() != null ? c.getProgram().getName() : "General",
                            Collectors.counting()));

            List<Map<String, Object>> courseDistData = distribution.entrySet().stream().map(e -> {
                Map<String, Object> m = new HashMap<>();
                m.put("name", e.getKey());
                m.put("value", e.getValue());
                return m;
            }).collect(Collectors.toList());

            // Weekly Engagement
            List<Map<String, Object>> weeklyData = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (int i = 3; i >= 0; i--) {
                LocalDateTime start = now.minusWeeks(i + 1);
                LocalDateTime end = now.minusWeeks(i);

                long activityCount = 0;
                try {
                    activityCount += activityLogRepository.countByDepartmentAndInterval(deptId, start, end);
                    activityCount += documentRepository.countByDepartmentAndInterval(deptId, start, end);
                    activityCount += courseFileRepository.countByDepartmentAndInterval(deptId, start, end);
                } catch (Exception e) {
                }

                Map<String, Object> week = new HashMap<>();
                week.put("name", "Week " + (4 - i));
                week.put("activity", activityCount);
                weeklyData.add(week);
            }

            // Activity Percent
            long activeTeachersCount = activityLogRepository.countActiveTeachersInDepartment(deptId, now.minusDays(7));
            double activityPercent = teacherCount == 0 ? 0 : (double) activeTeachersCount / teacherCount * 100;

            HODOverviewDTO overview = new HODOverviewDTO(
                    teacherCount,
                    courseCount,
                    pending,
                    weeklyData,
                    courseDistData,
                    Math.round(activityPercent * 10.0) / 10.0);

            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR IN HOD OVERVIEW: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to load dashboard: " + e.getMessage());
        }
    }

    /**
     * Faculty Performance Table Data
     */
    @GetMapping("/faculty/performance")
    public ResponseEntity<List<Map<String, Object>>> getFacultyPerformance(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Long deptId = userDetails.getDepartmentId();
            if (deptId == null) {
                User u = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
                if (u != null && u.getTeacher() != null && u.getTeacher().getDepartment() != null) {
                    deptId = u.getTeacher().getDepartment().getId();
                }
            }
            if (deptId == null)
                return ResponseEntity.badRequest().build();

            List<Teacher> teachers = teacherRepository.findByDepartmentId(deptId);
            List<Map<String, Object>> performanceList = new ArrayList<>();

            for (Teacher t : teachers) {
                Map<String, Object> stat = new HashMap<>();
                long activeCourses = courseTeacherRepository.countByTeacherId(t.getId());

                long totalUploads = documentRepository.countByUploadedBy_Id(t.getId());

                String status = "Average";
                if (totalUploads > 20 && activeCourses >= 1)
                    status = "Excellent";
                else if (totalUploads > 5)
                    status = "Good";

                stat.put("id", t.getId());
                stat.put("name", t.getName());
                stat.put("email", t.getEmailOfficial());
                stat.put("designation", t.getDesignation());
                stat.put("role", "Teacher");
                stat.put("activeCourses", activeCourses);
                stat.put("totalUploads", totalUploads);
                stat.put("status", status);

                performanceList.add(stat);
            }
            return ResponseEntity.ok(performanceList);
        } catch (Exception e) {
            System.err.println("HOD Performance load error: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/faculty-performance") // maintain compatibility
    public ResponseEntity<?> getFacultyPerformanceCompat(@AuthenticationPrincipal CustomUserDetails user) {
        return getFacultyPerformance(user);
    }

    /**
     * Top Performer Card
     */
    @GetMapping("/faculty/top-performer")
    public ResponseEntity<Map<String, Object>> getTopPerformer(@AuthenticationPrincipal CustomUserDetails user) {
        Long deptId = user.getDepartmentId();
        if (deptId == null)
            return ResponseEntity.badRequest().build();

        List<Teacher> teachers = teacherRepository.findByDepartmentId(deptId);

        Teacher top = null;
        long maxUploads = -1;

        for (Teacher t : teachers) {
            long uploads = getTeacherUploadCount(t);
            if (uploads > maxUploads) {
                maxUploads = uploads;
                top = t;
            }
        }

        if (top == null)
            return ResponseEntity.noContent().build();

        Map<String, Object> result = new HashMap<>();
        result.put("name", top.getName());
        result.put("reason", "Highest uploads this month (" + maxUploads + " files)");
        result.put("uploads", maxUploads);
        result.put("designation", top.getDesignation());

        return ResponseEntity.ok(result);
    }

    /**
     * Most Active Faculty Card
     */
    @GetMapping("/faculty/most-active")
    public ResponseEntity<Map<String, Object>> getMostActive(@AuthenticationPrincipal CustomUserDetails user) {
        Long deptId = user.getDepartmentId();
        if (deptId == null)
            return ResponseEntity.badRequest().build();

        List<Teacher> teachers = teacherRepository.findByDepartmentId(deptId);

        Teacher mostActive = null;
        LocalDateTime latest = null;

        for (Teacher t : teachers) {
            // Check Activity Logs
            try {
                List<Activity_Log> logs = activityLogRepository.findByActor_Id(t.getId());
                if (logs != null && !logs.isEmpty()) {
                    LocalDateTime logTime = logs.stream()
                            .filter(l -> l.getCreatedAt() != null)
                            .map(Activity_Log::getCreatedAt)
                            .max(LocalDateTime::compareTo)
                            .orElse(null);

                    if (logTime != null && (latest == null || logTime.isAfter(latest))) {
                        latest = logTime;
                        mostActive = t;
                    }
                }

                // Also check latest direct document upload
                List<CourseFile> files = courseFileRepository.findByCreatedById(t.getId());
                for (CourseFile cf : files) {
                    List<Heading> hs = headingRepository.findByCourseFileId(cf.getId());
                    for (Heading h : hs) {
                        List<Document> docs = documentRepository.findByHeading_Id(h.getId());
                        for (Document d : docs) {
                            if (d.getUploadedAt() != null && (latest == null || d.getUploadedAt().isAfter(latest))) {
                                latest = d.getUploadedAt();
                                mostActive = t;
                            }
                        }
                    }
                }
            } catch (Exception e) {
            }
        }

        if (mostActive == null) {
            // If no activity at all, just pick one teacher to avoid N/A if possible
            if (!teachers.isEmpty()) {
                mostActive = teachers.get(0);
                latest = LocalDateTime.now().minusDays(30); // Placeholder
            } else {
                return ResponseEntity.noContent().build();
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("name", mostActive.getName());
        result.put("id", mostActive.getId());
        result.put("lastActivity", latest != null ? latest : LocalDateTime.now());
        result.put("uploads", getTeacherUploadCount(mostActive));

        return ResponseEntity.ok(result);
    }

    /**
     * Department Course Coverage Card
     */
    @GetMapping("/department/course-coverage")
    public ResponseEntity<Map<String, Object>> getCourseCoverage(@AuthenticationPrincipal CustomUserDetails user) {
        Long deptId = user.getDepartmentId();
        if (deptId == null)
            return ResponseEntity.badRequest().build();

        // Method 1: Get courses formally linked to the department via Program/Structure
        Set<Course> coursesSet = new HashSet<>(courseRepository.findCoursesByDepartmentId(deptId));

        // Method 2: Get courses assigned to teachers in this department (often more
        // accurate in real use)
        List<Teacher> teachers = teacherRepository.findByDepartmentId(deptId);
        for (Teacher t : teachers) {
            List<CourseTeacher> assignments = courseTeacherRepository.findByTeacherId(t.getId());
            for (CourseTeacher ct : assignments) {
                if (ct.getCourse() != null) {
                    coursesSet.add(ct.getCourse());
                }
            }
        }

        if (coursesSet.isEmpty()) {
            return ResponseEntity.ok(Map.of("coverage", 0, "total", 0, "active", 0));
        }

        long activeCourses = 0;
        for (Course c : coursesSet) {
            boolean hasContent = false;
            try {
                // Check if any heading in any course file for this course has documents
                List<CourseFile> cFiles = courseFileRepository.findByCourse_Id(c.getId());
                for (CourseFile cf : cFiles) {
                    List<Heading> headings = headingRepository.findByCourseFileId(cf.getId());
                    for (Heading h : headings) {
                        if (documentRepository.existsByHeading(h)) {
                            hasContent = true;
                            break;
                        }
                    }
                    if (hasContent)
                        break;
                }
            } catch (Exception e) {
            }

            if (hasContent)
                activeCourses++;
        }

        double percent = (double) activeCourses / coursesSet.size() * 100;

        Map<String, Object> result = new HashMap<>();
        result.put("coverage", Math.round(percent));
        result.put("total", coursesSet.size());
        result.put("active", activeCourses);

        return ResponseEntity.ok(result);
    }

    /**
     * Weekly Engagement Chart Data
     */
    @GetMapping("/department/weekly-engagement")
    public ResponseEntity<Map<String, Object>> getWeeklyEngagement(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Long deptId = userDetails.getDepartmentId();
            if (deptId == null) {
                User u = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
                if (u != null && u.getTeacher() != null && u.getTeacher().getDepartment() != null) {
                    deptId = u.getTeacher().getDepartment().getId();
                }
            }
            if (deptId == null)
                return ResponseEntity.badRequest().build();

            LocalDateTime now = LocalDateTime.now();
            List<Map<String, Object>> weeklyData = new ArrayList<>();

            long[] counts = new long[4];
            for (int i = 3; i >= 0; i--) {
                LocalDateTime start = now.minusWeeks(i + 1);
                LocalDateTime end = now.minusWeeks(i);

                long count = 0;
                try {
                    count += activityLogRepository.countByDepartmentAndInterval(deptId, start, end);
                    count += documentRepository.countByDepartmentAndInterval(deptId, start, end);
                    count += courseFileRepository.countByDepartmentAndInterval(deptId, start, end);
                    count += templateRepository.countByDepartmentAndInterval(deptId, start, end);
                } catch (Exception e) {
                }

                counts[3 - i] = count;

                Map<String, Object> week = new HashMap<>();
                week.put("name", "Week " + (4 - i));
                week.put("activity", count);
                weeklyData.add(week);
            }

            double percentChange = 0;
            if (counts[2] > 0)
                percentChange = ((double) (counts[3] - counts[2]) / counts[2]) * 100;
            else if (counts[3] > 0)
                percentChange = 100;

            Map<String, Object> response = new HashMap<>();
            response.put("labels", List.of("Week 1", "Week 2", "Week 3", "Week 4"));
            response.put("data", List.of(counts[0], counts[1], counts[2], counts[3]));
            response.put("weeklyEngagementData", weeklyData);
            response.put("percentChange", Math.round(percentChange));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("HOD Weekly load error: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get courses having zero uploaded documentation/materials
     */
    @GetMapping("/courses/missing-materials")
    public ResponseEntity<?> getMissingMaterials(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long deptId = userDetails.getDepartmentId();
        if (deptId == null)
            return ResponseEntity.badRequest().build();

        // 1. Get all courses for this department
        Set<Course> allDeptCourses = new HashSet<>(courseRepository.findCoursesByDepartmentId(deptId));

        // Add courses from faculty assignments if missing from formal structure
        List<Teacher> deptTeachers = teacherRepository.findByDepartmentId(deptId);
        for (Teacher t : deptTeachers) {
            List<CourseTeacher> assignments = courseTeacherRepository.findByTeacherId(t.getId());
            for (CourseTeacher ct : assignments) {
                if (ct.getCourse() != null)
                    allDeptCourses.add(ct.getCourse());
            }
        }

        List<Map<String, Object>> missingList = new ArrayList<>();

        for (Course c : allDeptCourses) {
            boolean hasContent = false;
            try {
                List<CourseFile> cFiles = courseFileRepository.findByCourse_Id(c.getId());
                for (CourseFile cf : cFiles) {
                    List<Heading> headings = headingRepository.findByCourseFileId(cf.getId());
                    for (Heading h : headings) {
                        if (documentRepository.existsByHeading(h)) {
                            hasContent = true;
                            break;
                        }
                    }
                    if (hasContent)
                        break;
                }
            } catch (Exception e) {
            }

            if (!hasContent) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", c.getId());
                item.put("code", c.getCode());
                item.put("title", c.getTitle());

                // Find assigned teachers
                List<CourseTeacher> cts = courseTeacherRepository.findByCourseId(c.getId());
                List<String> teacherNames = cts.stream()
                        .filter(ct -> ct.getTeacher() != null)
                        .map(ct -> ct.getTeacher().getName())
                        .distinct()
                        .collect(Collectors.toList());

                item.put("assignedTeachers", teacherNames);
                missingList.add(item);
            }
        }

        return ResponseEntity.ok(missingList);
    }

    /**
     * Get detailed activity for a specific teacher (HOD View)
     */
    @GetMapping("/teacher/{id}/activity")
    public ResponseEntity<?> getTeacherActivity(@AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long teacherId) {

        Teacher teacher = teacherRepository.findById(teacherId).orElse(null);
        if (teacher == null)
            return ResponseEntity.notFound().build();

        // Check if teacher belongs to HOD's department
        if (teacher.getDepartment() != null && !teacher.getDepartment().getId().equals(userDetails.getDepartmentId())) {
            return ResponseEntity.status(403).body("Unauthorized access to this teacher's data");
        }

        Map<String, Object> response = new HashMap<>();

        // 1. Stats
        Map<String, Object> stats = new HashMap<>();
        stats.put("name", teacher.getName());
        stats.put("designation", teacher.getDesignation());

        long totalUploads = getTeacherUploadCount(teacher);
        stats.put("uploads", totalUploads);

        List<CourseFile> teacherFiles = courseFileRepository.findByCreatedById(teacher.getId());
        stats.put("courseFiles", teacherFiles.size());

        long approvedCount = teacherFiles.stream()
                .filter(f -> f.getStatus() == ApprovalStatus.APPROVED)
                .count();
        stats.put("approvals", approvedCount);

        long templateCount = templateRepository.findAll().stream()
                .filter(t -> t.getDepartmentId() != null
                        && teacher.getDepartment() != null
                        && t.getDepartmentId().equals(teacher.getDepartment().getId()))
                .count(); // This is a bit loose, ideally track who created template
        stats.put("templates", templateCount);

        response.put("stats", stats);

        // 2. Logs (Last 20)
        List<Activity_Log> logs = activityLogRepository.findByActor_Id(teacher.getId());
        if (logs != null) {
            logs = logs.stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .limit(20)
                    .collect(Collectors.toList());
        }
        response.put("logs", logs != null ? logs : new ArrayList<>());

        // 3. Course Files
        response.put("files", teacherFiles);

        return ResponseEntity.ok(response);
    }

    /**
     * Send Appreciation to Teacher
     */
    @PostMapping("/faculty/appreciation/send")
    public ResponseEntity<?> sendAppreciation(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, Object> payload) {
        Long teacherId = Long.valueOf(payload.get("teacherId").toString());
        String message = payload.get("message").toString();

        Teacher teacher = teacherRepository.findById(teacherId).orElse(null);
        if (teacher == null)
            return ResponseEntity.notFound().build();

        if (teacher.getDepartment() != null && !teacher.getDepartment().getId().equals(userDetails.getDepartmentId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Unauthorized department access"));
        }

        User sender = userRepository.findById(userDetails.getUserId()).orElse(null);
        if (sender == null)
            return ResponseEntity.status(401).build();

        Appreciation appreciation = new Appreciation(sender, teacher, message);
        appreciationRepository.save(appreciation);

        User receiverUser = userRepository.findAll().stream()
                .filter(u -> u.getTeacher() != null && u.getTeacher().getId().equals(teacherId))
                .findFirst().orElse(null);

        if (receiverUser != null) {
            Notification note = new Notification();
            note.setUser(receiverUser);
            note.setSender(sender);
            note.setTitle("You received an Appreciation!");
            note.setMessage("HOD " + sender.getUsername() + " says: " + message);
            note.setType("APPRECIATION");
            note.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(note);
        }

        if (teacher.getEmailOfficial() != null) {
            try {
                emailService.sendEmail(teacher.getEmailOfficial(), "New Appreciation from HOD", message);
            } catch (Exception e) {
            }
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "Appreciation sent successfully"));
    }

    @GetMapping("/faculty")
    public ResponseEntity<?> getFaculty(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long deptId = userDetails.getDepartmentId();
        if (deptId == null)
            return ResponseEntity.badRequest().body("No department found");

        List<User> users = userRepository.findByTeacher_DepartmentId(deptId);
        List<Map<String, Object>> response = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getTeacher() != null ? u.getTeacher().getId() : null);
            map.put("userId", u.getId());
            map.put("name", u.getTeacher() != null ? u.getTeacher().getName() : u.getUsername());
            map.put("email", u.getEmail());
            map.put("role", u.getRole());
            map.put("isActive", u.getIsActive());
            map.put("designation", u.getTeacher() != null ? u.getTeacher().getDesignation() : "N/A");
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/submission-heatmap")
    public ResponseEntity<List<Map<String, Object>>> getSubmissionHeatmap(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long deptId = userDetails.getDepartmentId();
        if (deptId == null)
            return ResponseEntity.badRequest().build();

        // Target: Last 12 Weeks (84 days)
        LocalDateTime since = LocalDateTime.now().minusDays(84).truncatedTo(java.time.temporal.ChronoUnit.DAYS);
        List<Object[]> queryResults = activityLogRepository.findDailyActivityCounts(deptId, since);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : queryResults) {
            Map<String, Object> m = new HashMap<>();
            m.put("date", row[0].toString());
            m.put("count", row[1]);
            result.add(m);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Broadcast email to all department teachers
     */
    @PostMapping("/email/department")
    public ResponseEntity<?> emailDepartment(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, String> payload) {

        String subject = payload.get("subject");
        String body = payload.get("body");
        Long deptId = userDetails.getDepartmentId();

        if (deptId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Department not assigned"));
        }

        List<Teacher> teachers = teacherRepository.findByDepartmentId(deptId);
        int sentCount = 0;

        User sender = userRepository.findById(userDetails.getUserId()).orElse(null);

        for (Teacher teacher : teachers) {
            // Also send system notification
            User receiverUser = userRepository.findAll().stream()
                    .filter(u -> u.getTeacher() != null && u.getTeacher().getId().equals(teacher.getId()))
                    .findFirst().orElse(null);

            if (receiverUser != null && sender != null) {
                Notification note = new Notification();
                note.setUser(receiverUser);
                note.setSender(sender);
                note.setTitle("Message from HOD: " + subject);
                note.setMessage(body);
                note.setType("DEPARTMENT_ALERT");
                note.setCreatedAt(LocalDateTime.now());
                notificationRepository.save(note);
            }

            // Send Email
            String email = teacher.getEmailOfficial();
            if (email != null && !email.isEmpty()) {
                try {
                    emailService.sendEmail(email, subject, body);
                    sentCount++;
                } catch (Exception e) {
                    System.err.println("Broadcast error to: " + email + " - " + e.getMessage());
                }
            }
        }

        return ResponseEntity
                .ok(Map.of("success", true, "sentTo", sentCount, "message", "Broadcast sent to all faculty members"));
    }

    // --- Helpers ---
    private long getTeacherUploadCount(Teacher teacher) {
        return documentRepository.countByUploadedBy_Id(teacher.getId());
    }

}
