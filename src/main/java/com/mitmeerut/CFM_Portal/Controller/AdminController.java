package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Model.ApprovalStatus;
import com.mitmeerut.CFM_Portal.Model.User;
import com.mitmeerut.CFM_Portal.Service.EmailService;
import com.mitmeerut.CFM_Portal.Service.UserService;
import com.mitmeerut.CFM_Portal.Model.Department;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.mitmeerut.CFM_Portal.dto.ChatTeacherDTO;
import java.util.Arrays;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = { "http://localhost:5000" }, allowCredentials = "true")
@org.springframework.security.access.prepost.PreAuthorize("hasAuthority('MANAGE_SYSTEM')")
public class AdminController {

        @Autowired
        private UserService userService;

        @Autowired
        private EmailService emailService;

        @Autowired
        private com.mitmeerut.CFM_Portal.Repository.UserRepository userRepository;

        @Autowired
        private com.mitmeerut.CFM_Portal.Repository.CourseRepository courseRepository;

        @Autowired
        private com.mitmeerut.CFM_Portal.Repository.CourseFileRepository courseFileRepository;

        @Autowired
        private com.mitmeerut.CFM_Portal.Repository.DocumentRepository documentRepository;

        @Autowired
        private com.mitmeerut.CFM_Portal.Repository.DepartmentRepository departmentRepository;

        @Autowired
        private com.mitmeerut.CFM_Portal.Repository.ActivityLogRepository activityLogRepository;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private com.mitmeerut.CFM_Portal.Service.NotificationService notificationService;

        @GetMapping("/pending-teachers")
        public ResponseEntity<List<User>> getPendingTeachers() {
                List<User> pending = userService.getPendingTeachers();
                return ResponseEntity.ok(pending);
        }

        @GetMapping("/teachers")
        public ResponseEntity<List<User>> getAllTeachers() {
                return ResponseEntity.ok(userService.getAllTeachers());
        }

        @PostMapping("/approve/{userId}")
        public ResponseEntity<?> approveTeacher(@PathVariable("userId") Long userId) {

                User approvedUser = userService.approveUser(userId);

                // Log activity
                try {
                        com.mitmeerut.CFM_Portal.Model.Activity_Log log = new com.mitmeerut.CFM_Portal.Model.Activity_Log();
                        log.setActor(null); // System or add current user if sessions are available
                        log.setAction("Teacher Approved");
                        log.setTargetType("User");
                        log.setTargetId(approvedUser.getId());

                        Map<String, String> logDetails = new HashMap<>();
                        logDetails.put("event", "Teacher Account Approved");
                        logDetails.put("username", approvedUser.getUsername());
                        logDetails.put("approvedBy", "Admin");
                        log.setDetails(objectMapper.writeValueAsString(logDetails));

                        activityLogRepository.save(log);
                } catch (Exception e) {
                        e.printStackTrace();
                }

                emailService.sendEmail(
                                approvedUser.getEmail(),
                                "Your Teacher Account Has Been Approved",
                                "Dear " + approvedUser.getUsername() + ",\n\n" +
                                                "Your teacher account has been successfully APPROVED by the Administrator.\n"
                                                +
                                                "You can now log in to your dashboard.\n\n" +
                                                "Login Link: http://localhost:5000/login\n\n" +
                                                "Regards,\nCourse File Management Team");

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Teacher approved successfully");
                response.put("email", approvedUser.getEmail());

                // Return safe user DTO
                Map<String, Object> safeUser = new HashMap<>();
                safeUser.put("id", approvedUser.getId());
                safeUser.put("username", approvedUser.getUsername());
                safeUser.put("isActive", approvedUser.getIsActive());
                response.put("user", safeUser);

                return ResponseEntity.ok(response);
        }

        @GetMapping("/hods")
        public ResponseEntity<List<Map<String, Object>>> getHodList() {
                // Task: Only fetch users with role = HOD
                List<User> users = userRepository.findByRole(User.userRole.HOD);

                List<Map<String, Object>> result = users.stream().map(u -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", u.getId());
                        map.put("email", u.getEmail());
                        // Resolve name from Teacher entity if available, else username
                        String name = u.getUsername();
                        if (u.getTeacher() != null && u.getTeacher().getName() != null) {
                                name = u.getTeacher().getName();
                        }
                        map.put("name", name);
                        return map;
                }).collect(Collectors.toList());

                return ResponseEntity.ok(result);
        }

        @GetMapping("/dashboard/stats")
        public ResponseEntity<Map<String, Object>> getDashboardStats() {
                // TASK: Count ALL users from DB (exactly matching SELECT COUNT(*) FROM users)
                long totalUsers = userRepository.count();
                long totalCourses = courseRepository.count();
                long totalDocuments = documentRepository.count();
                Long storageUsedBytes = documentRepository.getTotalStorageUsed();

                if (storageUsedBytes == null)
                        storageUsedBytes = 0L;

                // Active users (seen in last 15 minutes)
                long onlineUsers = userRepository.countByLastLoginAfter(LocalDateTime.now().minusMinutes(15));

                double storageUsedMB = storageUsedBytes / (1024.0 * 1024.0);

                Map<String, Object> statsResponse = new HashMap<>();
                statsResponse.put("totalUsers", totalUsers);
                statsResponse.put("activeUsers", onlineUsers); // "onlineUsers" for internal clarity
                statsResponse.put("totalCourses", totalCourses);
                statsResponse.put("totalDocuments", totalDocuments);
                statsResponse.put("totalStorageUsed", storageUsedBytes);
                statsResponse.put("storageUsedMB", Math.round(storageUsedMB * 100.0) / 100.0);

                return ResponseEntity.ok(statsResponse);
        }

        @GetMapping("/dashboard/growth")
        public ResponseEntity<List<Map<String, Object>>> getSystemGrowth() {
                // Get real stats from DB
                List<Object[]> stats = userRepository.getGrowthStats();
                Map<String, Long> countMap = new HashMap<>();
                for (Object[] row : stats) {
                        int year = (int) row[0];
                        int month = (int) row[1];
                        long count = (long) row[2];
                        countMap.put(year + "-" + month, count);
                }

                List<Map<String, Object>> growth = new ArrayList<>();
                java.time.LocalDate now = java.time.LocalDate.now();

                // Always return last 6 months to make the chart look full/professional
                for (int i = 5; i >= 0; i--) {
                        java.time.LocalDate date = now.minusMonths(i);
                        String key = date.getYear() + "-" + date.getMonthValue();
                        String label = date.getMonth().getDisplayName(java.time.format.TextStyle.SHORT,
                                        java.util.Locale.ENGLISH) + " " + date.getYear();

                        Map<String, Object> m = new HashMap<>();
                        m.put("name", label);
                        m.put("uploads", countMap.getOrDefault(key, 0L));
                        growth.add(m);
                }

                return ResponseEntity.ok(growth);
        }

        @GetMapping("/dashboard/activity")
        public ResponseEntity<List<Map<String, Object>>> getRecentActivity() {
                List<com.mitmeerut.CFM_Portal.Model.Activity_Log> logs = activityLogRepository
                                .findTop50ByOrderByCreatedAtDesc();

                List<Map<String, Object>> response = logs.stream().map(log -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", log.getId());
                        map.put("action", log.getAction());
                        map.put("details", log.getDetails());
                        map.put("timestamp",
                                        log.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

                        // Flatten actor for frontend
                        if (log.getActor() != null) {
                                map.put("actor", log.getActor().getName());
                        } else {
                                map.put("actor", "System");
                        }

                        return map;
                }).collect(Collectors.toList());

                return ResponseEntity.ok(response);
        }

        @GetMapping("/dashboard/distribution")
        public ResponseEntity<List<Map<String, Object>>> getContentDistribution() {
                List<Map<String, Object>> distribution = new java.util.ArrayList<>();

                // Actual counts based on CourseFile Status - HIGH PERFORMANCE COUNT
                long approved = courseFileRepository.countCourseFilesByDepartmentIdAndStatus(null,
                                ApprovalStatus.APPROVED);
                long submitted = courseFileRepository.countCourseFilesByDepartmentIdAndStatus(null,
                                ApprovalStatus.SUBMITTED);
                long returned = courseFileRepository.countCourseFilesByDepartmentIdAndStatus(null,
                                ApprovalStatus.REJECTED);
                long drafts = courseFileRepository.countCourseFilesByDepartmentIdAndStatus(null, ApprovalStatus.DRAFT);

                Map<String, Object> m1 = new HashMap<>();
                m1.put("name", "Approved Files");
                m1.put("value", approved);
                distribution.add(m1);

                Map<String, Object> m2 = new HashMap<>();
                m2.put("name", "Pending Review");
                m2.put("value", submitted);
                distribution.add(m2);

                Map<String, Object> m3 = new HashMap<>();
                m3.put("name", "Returned Files");
                m3.put("value", returned);
                distribution.add(m3);

                Map<String, Object> m4 = new HashMap<>();
                m4.put("name", "Drafts");
                m4.put("value", drafts);
                distribution.add(m4);

                return ResponseEntity.ok(distribution);
        }

        @GetMapping("/dashboard/health")
        public ResponseEntity<Map<String, Object>> getSystemHealth() {
                Map<String, Object> health = new HashMap<>();

                // Database Health via real DataSource check
                boolean dbUp = false;
                try {
                        jdbcTemplate.execute("SELECT 1");
                        dbUp = true;
                } catch (Exception e) {
                        dbUp = false;
                }
                health.put("database", dbUp ? "UP" : "DOWN");

                // Real User Stats (no filters) - HIGH PERFORMANCE COUNT
                long totalUsers = userRepository.count();
                long activeUsersCount = userRepository.countByIsActiveTrue();

                health.put("totalUsers", totalUsers);
                health.put("activeUsers", activeUsersCount);

                // Real Storage Health
                Long storageUsedBytes = documentRepository.getTotalStorageUsed();
                if (storageUsedBytes == null)
                        storageUsedBytes = 0L;

                double storageUsedMB = storageUsedBytes / (1024.0 * 1024.0);
                double storageLimitMB = 1024.0; // 1GB default limit
                double usagePercent = (storageUsedMB / storageLimitMB) * 100.0;

                health.put("storageUsedMB", Math.round(storageUsedMB * 100.0) / 100.0);
                health.put("storageLimitMB", storageLimitMB);
                health.put("storageUsagePercent", Math.min(100.0, Math.round(usagePercent * 100.0) / 100.0));

                return ResponseEntity.ok(health);
        }

        @GetMapping("/dashboard/health/details")
        public ResponseEntity<Map<String, Object>> getDetailedSystemReport() {
                Map<String, Object> details = new HashMap<>();

                // Growth Data
                List<User> users = userRepository.findAll();
                Map<String, Long> growthMap = users.stream()
                                .filter(u -> u.getCreatedAt() != null)
                                .collect(Collectors.groupingBy(
                                                u -> u.getCreatedAt().getMonth().name().substring(0, 3) + " "
                                                                + u.getCreatedAt().getYear(),
                                                Collectors.counting()));
                details.put("userGrowthData", growthMap);

                // File type distribution (mocking logic but real count)
                List<Map<String, Object>> distribution = new ArrayList<>();
                // Real implementation would group documents by extension, but for now we use
                // counts of entities
                Map<String, Object> docs = new HashMap<>();
                docs.put("name", "Documents");
                docs.put("value", documentRepository.count());
                distribution.add(docs);

                Map<String, Object> courses = new HashMap<>();
                courses.put("name", "Courses");
                courses.put("value", courseRepository.count());
                distribution.add(courses);

                details.put("fileTypeDistribution", distribution);

                // Recent Activities
                details.put("recentSystemActivities", activityLogRepository.findTop50ByOrderByCreatedAtDesc());

                return ResponseEntity.ok(details);
        }

        @GetMapping("/dashboard/health/export/csv")
        public void exportToCSV(HttpServletResponse response) throws IOException {
                response.setContentType("text/csv");
                // Sanitized filename to prevent export failure
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
                response.setHeader("Content-Disposition", "attachment; filename=system_report_" + timestamp + ".csv");

                java.io.PrintWriter writer = response.getWriter();
                writer.println("Category,Metric,Value");

                // System stats
                writer.println("System,Total Users," + userRepository.count());
                writer.println("System,Total Courses," + courseRepository.count());
                writer.println("System,Total Documents," + documentRepository.count());

                Long storage = documentRepository.getTotalStorageUsed();
                writer.println("Storage,Used Bytes," + (storage != null ? storage : 0));

                writer.flush();
                writer.close();
        }

        @GetMapping("/dashboard/health/export/pdf")
        public void exportToPDF(HttpServletResponse response) throws IOException {
                response.setContentType("application/pdf");
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
                response.setHeader("Content-Disposition", "attachment; filename=system_report_" + timestamp + ".pdf");

                Document document = new Document(PageSize.A4);
                PdfWriter.getInstance(document, response.getOutputStream());

                document.open();
                Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
                fontTitle.setSize(18);

                Paragraph title = new Paragraph("System Health Report", fontTitle);
                title.setAlignment(Paragraph.ALIGN_CENTER);
                document.add(title);

                Paragraph timestampParagraph = new Paragraph("Generated on: "
                                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                timestampParagraph.setSpacingBefore(10);
                document.add(timestampParagraph);

                Paragraph stats = new Paragraph("\nCore Metrics:\n");
                stats.add("- Total Users: " + userRepository.count() + "\n");
                stats.add("- Total Courses: " + courseRepository.count() + "\n");
                stats.add("- Total Documents: " + documentRepository.count() + "\n");
                document.add(stats);

                document.close();
        }

        @DeleteMapping("/delete/{userId}")
        public ResponseEntity<?> deleteUser(@PathVariable("userId") Long userId) {
                userService.deleteUser(userId);
                Map<String, Object> response = new HashMap<>();
                response.put("message", "User deleted successfully");
                return ResponseEntity.ok(response);
        }

        @GetMapping("/chat/teachers")
        public ResponseEntity<List<ChatTeacherDTO>> getChatTeachers() {
                List<User.userRole> facultyRoles = Arrays.asList(
                                User.userRole.TEACHER,
                                User.userRole.HOD,
                                User.userRole.SUBJECTHEAD);

                List<User> faculty = userRepository.findByRoleIn(facultyRoles);

                List<ChatTeacherDTO> dtos = faculty.stream()
                                .map(u -> {
                                        String name = u.getTeacher() != null ? u.getTeacher().getName()
                                                        : u.getUsername();
                                        String dept = (u.getTeacher() != null && u.getTeacher().getDepartment() != null)
                                                        ? u.getTeacher().getDepartment().getName()
                                                        : "N/A";

                                        // Simple online logic: seen in the last 5 minutes
                                        boolean isOnline = u.getLastLogin() != null &&
                                                        u.getLastLogin().isAfter(LocalDateTime.now().minusMinutes(5));

                                        return new ChatTeacherDTO(
                                                        u.getId(),
                                                        name,
                                                        u.getEmail(),
                                                        dept,
                                                        isOnline);
                                })
                                .collect(Collectors.toList());

                return ResponseEntity.ok(dtos);
        }

        @Autowired
        private com.mitmeerut.CFM_Portal.Service.RolePermissionService rolePermissionService;

        @PostMapping("/roles/{role}/permissions")
        @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('MANAGE_ROLES')")
        public ResponseEntity<?> updateRolePermissions(
                        @PathVariable("role") String role,
                        @RequestBody Map<String, String> payload) {

                String permissionsJson = payload.get("permissions");
                // In a real app, we'd get the current user's ID
                Long adminId = 1L;

                com.mitmeerut.CFM_Portal.Model.RolePermission updated = rolePermissionService.updatePermissions(role,
                                permissionsJson, adminId);

                return ResponseEntity.ok(updated);
        }

        @PostMapping("/broadcast")
        public ResponseEntity<?> broadcastAnnouncement(@RequestBody Map<String, String> payload) {
                String message = payload.get("message");
                String title = payload.get("title");

                // Get all active users
                List<User> users = userRepository.findAll();

                for (User user : users) {
                        try {
                                com.mitmeerut.CFM_Portal.Model.Notification note = new com.mitmeerut.CFM_Portal.Model.Notification();
                                note.setUser(user);
                                note.setType("ANNOUNCEMENT");
                                note.setTitle(title != null ? title : "System Announcement");
                                note.setMessage(message);
                                note.setIsRead(false);
                                note.setCreatedAt(LocalDateTime.now());
                                // In a real app, set current admin as sender

                                notificationService.send(note);
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                }

                // Log this activity
                try {
                        com.mitmeerut.CFM_Portal.Model.Activity_Log log = new com.mitmeerut.CFM_Portal.Model.Activity_Log();
                        log.setAction("Global Announcement");
                        log.setTargetType("System");
                        Map<String, String> details = new HashMap<>();
                        details.put("message", message);
                        log.setDetails(objectMapper.writeValueAsString(details));
                        activityLogRepository.save(log);
                } catch (Exception e) {
                        e.printStackTrace();
                }

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Announcement broadcasted to " + users.size() + " users");
                return ResponseEntity.ok(response);
        }

        @GetMapping("/roles/permissions")
        @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('MANAGE_ROLES')")
        public ResponseEntity<?> getAllRolePermissions() {
                return ResponseEntity.ok(rolePermissionService.getAllRoles());
        }

        @GetMapping("/dashboard/compliance")
        public ResponseEntity<List<Map<String, Object>>> getDepartmentalCompliance() {
                // HIGH PERFORMANCE: Fetch aggregated compliance stats for all departments in
                // ONE query
                List<Object[]> stats = courseFileRepository.getCombinedComplianceStats();

                // Process results into the format expected by the frontend
                Map<String, Map<String, Object>> deptMap = new HashMap<>();

                for (Object[] row : stats) {
                        String deptName = (String) row[1];
                        ApprovalStatus status = (ApprovalStatus) row[2];
                        long count = (long) row[3];

                        Map<String, Object> deptData = deptMap.computeIfAbsent(deptName, k -> {
                                Map<String, Object> m = new HashMap<>();
                                m.put("department", deptName);
                                m.put("approved", 0L);
                                m.put("total", 0L);
                                return m;
                        });

                        long currentTotal = (long) deptData.get("total");
                        deptData.put("total", currentTotal + count);

                        if (status == ApprovalStatus.APPROVED || status == ApprovalStatus.FINAL_APPROVED) {
                                long currentApproved = (long) deptData.get("approved");
                                deptData.put("approved", currentApproved + count);
                        }
                }

                List<Map<String, Object>> complianceData = deptMap.values().stream()
                                .map(m -> {
                                        long approved = (long) m.get("approved");
                                        long total = (long) m.get("total");
                                        long pending = total - approved;
                                        double percentage = total > 0 ? (double) approved * 100 / total : 0;

                                        m.put("pending", pending);
                                        m.put("percentage", Math.round(percentage));
                                        return m;
                                })
                                .collect(Collectors.toList());

                return ResponseEntity.ok(complianceData);
        }
}
