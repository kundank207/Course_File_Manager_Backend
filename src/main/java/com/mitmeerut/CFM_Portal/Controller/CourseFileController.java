package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Model.CourseFile;
import com.mitmeerut.CFM_Portal.Service.CourseFileService;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/teacher/course-files")
@CrossOrigin(origins = { "http://localhost:5000", "http://localhost:5173" }, allowCredentials = "true")
public class CourseFileController {

    private final CourseFileService courseFileService;
    private final com.mitmeerut.CFM_Portal.Repository.CourseFileRepository courseFileRepo;

    @Autowired
    public CourseFileController(CourseFileService courseFileService,
            com.mitmeerut.CFM_Portal.Repository.CourseFileRepository courseFileRepo) {
        this.courseFileService = courseFileService;
        this.courseFileRepo = courseFileRepo;
    }

    @PostMapping
    public CourseFile createCourseFile(
            @RequestBody com.mitmeerut.CFM_Portal.dto.CreateCourseFileRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        return courseFileService.createCourseFile(request, user);
    }

    @GetMapping("/my")
    public ResponseEntity<List<Map<String, Object>>> getMyCourseFiles(
            @AuthenticationPrincipal CustomUserDetails user) {
        Long teacherId = user.getTeacher().getId();
        List<CourseFile> courseFiles = courseFileService.getCourseFilesByTeacher(teacherId);

        // HIGH PERFORMANCE: Batch fetch all version counts for this teacher in ONE
        // query
        List<Object[]> versionCounts = courseFileRepo.findVersionCountsByTeacher(teacherId);
        Map<String, Long> countMap = new HashMap<>();
        for (Object[] row : versionCounts) {
            String key = row[0] + "_" + row[1] + "_" + row[2];
            countMap.put(key, (Long) row[3]);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (CourseFile cf : courseFiles) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", cf.getId());
            item.put("academicYear", cf.getAcademicYear());
            item.put("section", cf.getSection());

            // Map status for frontend UI
            item.put("status", cf.getStatus().name());
            item.put("createdAt", cf.getCreatedAt());
            item.put("rejectedAt", cf.getRejectedAt());
            item.put("systemComment", cf.getSystemComment());
            item.put("revisionNumber", cf.getRevisionNumber());

            // Use the pre-fetched count map instead of calling getVersionCount in a loop
            String key = cf.getCourse().getId() + "_" + cf.getAcademicYear() + "_" + cf.getSection();
            item.put("versionCount", countMap.getOrDefault(key, 1L));

            // Include course info
            if (cf.getCourse() != null) {
                Map<String, Object> courseInfo = new HashMap<>();
                courseInfo.put("id", cf.getCourse().getId());
                courseInfo.put("code", cf.getCourse().getCode());
                courseInfo.put("title", cf.getCourse().getTitle());
                item.put("course", courseInfo);
            }

            result.add(item);
        }

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteCourseFile(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        try {
            courseFileService.deleteCourseFile(id, user.getTeacher().getId());
            Map<String, String> response = new HashMap<>();
            response.put("message", "Course file deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
