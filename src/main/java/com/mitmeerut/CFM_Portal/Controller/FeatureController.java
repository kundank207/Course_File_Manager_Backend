package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Model.ChangeLog;
import com.mitmeerut.CFM_Portal.Model.CourseFile;
import com.mitmeerut.CFM_Portal.Service.ChangeLogService;
import com.mitmeerut.CFM_Portal.Service.VersionComparisonService;
import com.mitmeerut.CFM_Portal.Repository.CourseFileRepository;
import com.mitmeerut.CFM_Portal.dto.DiffNode;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/features")
@CrossOrigin(origins = { "http://localhost:5000", "http://localhost:5173" }, allowCredentials = "true")
public class FeatureController {

    private final ChangeLogService changeLogService;
    private final VersionComparisonService comparisonService;
    private final CourseFileRepository courseFileRepo;
    private final com.mitmeerut.CFM_Portal.Repository.CourseTeacherRepository courseTeacherRepo;

    @Autowired
    public FeatureController(ChangeLogService changeLogService,
            VersionComparisonService comparisonService,
            CourseFileRepository courseFileRepo,
            com.mitmeerut.CFM_Portal.Repository.CourseTeacherRepository courseTeacherRepo) {
        this.changeLogService = changeLogService;
        this.comparisonService = comparisonService;
        this.courseFileRepo = courseFileRepo;
        this.courseTeacherRepo = courseTeacherRepo;
    }

    @GetMapping("/change-logs/{courseFileId}")
    public ResponseEntity<List<Map<String, Object>>> getChangeLogs(
            @PathVariable("courseFileId") Long courseFileId,
            @AuthenticationPrincipal CustomUserDetails user) {

        // Security check - Now allows SH and HOD
        validateAccess(courseFileId, user);

        List<ChangeLog> logs = changeLogService.getLogsForFile(courseFileId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChangeLog log : logs) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", log.getId());
            map.put("versionId", log.getVersionId());
            map.put("updatedByName", log.getUpdatedBy() != null ? log.getUpdatedBy().getName() : "System");
            map.put("changeType", log.getChangeType());
            map.put("sectionName", log.getSectionName());
            map.put("description", log.getDescription());
            map.put("createdAt", log.getCreatedAt());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/compare")
    public ResponseEntity<List<DiffNode>> compareVersions(
            @RequestParam("v1") Long v1Id,
            @RequestParam("v2") Long v2Id,
            @AuthenticationPrincipal CustomUserDetails user) {

        // Security checks
        validateAccess(v1Id, user);
        validateAccess(v2Id, user);

        return ResponseEntity.ok(comparisonService.compareVersions(v1Id, v2Id));
    }

    @GetMapping("/versions/{courseFileId}")
    public ResponseEntity<List<Map<String, Object>>> getRelatedVersions(
            @PathVariable("courseFileId") Long courseFileId,
            @AuthenticationPrincipal CustomUserDetails user) {

        CourseFile cf = courseFileRepo.findById(courseFileId)
                .orElseThrow(() -> new RuntimeException("Course file not found"));

        // Find all revisions for the same course, year, section
        List<CourseFile> versions = courseFileRepo.findByCourseIdAndAcademicYearAndSectionOrderByRevisionNumberDesc(
                cf.getCourse().getId(), cf.getAcademicYear(), cf.getSection());

        List<Map<String, Object>> result = new ArrayList<>();
        for (CourseFile v : versions) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", v.getId());
            map.put("revisionNumber", v.getRevisionNumber());
            map.put("status", v.getStatus());
            map.put("createdAt", v.getCreatedAt());
            map.put("createdByName", v.getCreatedBy() != null ? v.getCreatedBy().getName() : "Unknown");
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    private void validateAccess(Long courseFileId, CustomUserDetails user) {
        CourseFile cf = courseFileRepo.findById(courseFileId)
                .orElseThrow(() -> new RuntimeException("Course file not found"));

        Collection<? extends org.springframework.security.core.GrantedAuthority> authorities = user.getAuthorities();
        boolean isHod = authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_HOD"));
        boolean isAdmin = authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // Robust Subject Head check: Check authorities AND database directly
        boolean isSubjectHead = authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUBJECTHEAD"));
        if (!isSubjectHead && user.getTeacher() != null) {
            isSubjectHead = courseTeacherRepo.existsByTeacherIdAndIsSubjectHeadTrue(user.getTeacher().getId());
        }

        if (isHod || isSubjectHead || isAdmin) {
            return;
        }

        // Teachers can only access their own files
        if (cf.getCreatedBy() != null && user.getTeacher() != null) {
            if (cf.getCreatedBy().getId().equals(user.getTeacher().getId())) {
                return;
            }
        }

        throw new RuntimeException("Unauthorized access to this course file");
    }
}
