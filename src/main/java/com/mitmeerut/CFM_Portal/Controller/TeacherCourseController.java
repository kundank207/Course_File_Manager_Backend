package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Model.Teacher;
import com.mitmeerut.CFM_Portal.Model.Template;
import com.mitmeerut.CFM_Portal.Repository.CourseTeacherRepository;
import com.mitmeerut.CFM_Portal.Repository.TemplateRepository;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;
import com.mitmeerut.CFM_Portal.Service.CourseFileService;
import com.mitmeerut.CFM_Portal.Model.Course;
import com.mitmeerut.CFM_Portal.Model.TemplateType;
import com.mitmeerut.CFM_Portal.Model.Program;
import com.mitmeerut.CFM_Portal.Repository.CourseRepository;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherCourseController {

    private final CourseTeacherRepository courseTeacherRepo;
    private final TemplateRepository templateRepo;
    private final CourseFileService courseFileService;
    private final CourseRepository courseRepo;

    @Autowired
    public TeacherCourseController(
            CourseTeacherRepository courseTeacherRepo,
            TemplateRepository templateRepo,
            CourseFileService courseFileService,
            CourseRepository courseRepo) {
        this.courseTeacherRepo = courseTeacherRepo;
        this.templateRepo = templateRepo;
        this.courseFileService = courseFileService;
        this.courseRepo = courseRepo;
    }

    @GetMapping("/my-courses")
    public List<Map<String, Object>> getMyCourses(
            @AuthenticationPrincipal CustomUserDetails user) {
        Teacher teacher = user.getTeacher();

        return courseTeacherRepo.findByTeacherId(teacher.getId())
                .stream()
                .map(ct -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("courseId", ct.getCourse().getId());
                    map.put("code", ct.getCourse().getCode());
                    map.put("title", ct.getCourse().getTitle());
                    map.put("academicYear", ct.getAcademicYear());
                    map.put("section", ct.getSection());
                    map.put("departmentName", ct.getDepartment() != null ? ct.getDepartment().getName() : "N/A");
                    return map;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/templates")
    public List<Template> getTemplates(
            @RequestParam(value = "course_id", required = false) Long courseId,
            @AuthenticationPrincipal CustomUserDetails user) {

        if (courseId != null) {
            Course course = courseRepo.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Course not found"));

            Program program = course.getProgram();
            if (program == null || program.getDepartment() == null) {
                // Fallback to teacher's department if course department mapping is broken
                Teacher teacher = user.getTeacher();
                if (teacher == null || teacher.getDepartment() == null) {
                    return java.util.Collections.emptyList();
                }
                return templateRepo.findByDepartmentId(teacher.getDepartment().getId());
            }

            Long deptId = program.getDepartment().getId();

            java.util.List<TemplateType> requiredTypes = new java.util.ArrayList<>();
            // Handle potentially null flags
            if (Boolean.TRUE.equals(course.getHasTheory()))
                requiredTypes.add(TemplateType.THEORY);
            if (Boolean.TRUE.equals(course.getHasLab()))
                requiredTypes.add(TemplateType.LAB);
            if (Boolean.TRUE.equals(course.getHasProject()))
                requiredTypes.add(TemplateType.PROJECT);

            if (requiredTypes.isEmpty()) {
                return templateRepo.findByDepartmentId(deptId);
            }

            return templateRepo.findByDepartmentIdAndTemplateTypeIn(deptId, requiredTypes);
        }

        // Generic load for department (optional)
        Long deptId = user.getTeacher().getDepartment().getId();
        return templateRepo.findByDepartmentId(deptId);
    }

    /**
     * Check if the current teacher is assigned as Subject Head for any course
     */
    @GetMapping("/is-subject-head")
    public Map<String, Object> isSubjectHead(
            @AuthenticationPrincipal CustomUserDetails user) {
        Teacher teacher = user.getTeacher();
        boolean isSubjectHead = courseTeacherRepo.existsByTeacherIdAndIsSubjectHeadTrue(teacher.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("isSubjectHead", isSubjectHead);

        if (isSubjectHead) {
            // Get the courses they are subject head for
            List<Map<String, Object>> courses = courseTeacherRepo.findSubjectHeadAssignmentsByTeacherId(teacher.getId())
                    .stream()
                    .map(ct -> {
                        Map<String, Object> courseInfo = new HashMap<>();
                        courseInfo.put("courseId", ct.getCourse().getId());
                        courseInfo.put("courseCode", ct.getCourse().getCode());
                        courseInfo.put("courseTitle", ct.getCourse().getTitle());
                        courseInfo.put("academicYear", ct.getAcademicYear());
                        courseInfo.put("section", ct.getSection());
                        return courseInfo;
                    })
                    .collect(Collectors.toList());
            result.put("subjectHeadCourses", courses);
        }

        return result;
    }

    @PostMapping("/course-file/{fileId}/revision")
    public Map<String, Object> createRevision(
            @PathVariable("fileId") Long fileId,
            @AuthenticationPrincipal CustomUserDetails user) {
        com.mitmeerut.CFM_Portal.Model.CourseFile newFile = courseFileService.createRevision(fileId,
                user.getTeacher().getId());

        Map<String, Object> response = new HashMap<>();
        response.put("id", newFile.getId());
        response.put("revisionNumber", newFile.getRevisionNumber());
        response.put("message", "New revision created successfully as DRAFT");
        return response;
    }

    @GetMapping("/course-files/previous")
    public List<Map<String, Object>> getPreviousFiles(
            @RequestParam("course_id") Long courseId,
            @RequestParam("academic_year") String academicYear) {
        return courseFileService.findPreviousCourseFiles(courseId, academicYear).stream()
                .map(cf -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", cf.getId());
                    map.put("courseCode", cf.getCourse().getCode());
                    map.put("courseTitle", cf.getCourse().getTitle());
                    map.put("academicYear", cf.getAcademicYear());
                    map.put("section", cf.getSection());
                    map.put("createdAt", cf.getCreatedAt());
                    map.put("status", cf.getStatus());
                    return map;
                }).collect(Collectors.toList());
    }

    @PostMapping("/course-files/reuse")
    public Map<String, Object> reuseFile(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal CustomUserDetails user) {
        Long previousFileId = Long.valueOf(body.get("previousFileId").toString());
        Long newCourseId = Long.valueOf(body.get("newCourseId").toString());
        String academicYear = body.get("academicYear").toString();
        String section = body.get("section").toString();

        com.mitmeerut.CFM_Portal.Model.CourseFile newFile = courseFileService.reuseCourseFile(
                previousFileId, newCourseId, academicYear, section, user.getTeacher().getId());

        Map<String, Object> response = new HashMap<>();
        response.put("id", newFile.getId());
        response.put("message", "Course file reused successfully");
        return response;
    }

    @PostMapping("/course-files/share")
    public ResponseEntity<?> shareFile(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal CustomUserDetails user) {
        Long fileId = Long.valueOf(body.get("fileId").toString());
        Long sharedWithId = Long.valueOf(body.get("sharedWithId").toString());
        String message = body.get("message") != null ? body.get("message").toString() : "";

        courseFileService.shareCourseFile(fileId, user.getTeacher().getId(), sharedWithId, message);
        return ResponseEntity.ok(Map.of("message", "Course file shared successfully"));
    }

    @GetMapping("/course-files/shared-with-me")
    public List<Map<String, Object>> getSharedWithMe(
            @AuthenticationPrincipal CustomUserDetails user) {
        return courseFileService.getSharedWithMe(user.getTeacher().getId()).stream()
                .map(s -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", s.getId());
                    map.put("fileId", s.getCourseFile().getId());
                    map.put("courseCode", s.getCourseFile().getCourse().getCode());
                    map.put("courseTitle", s.getCourseFile().getCourse().getTitle());
                    map.put("sharedBy", s.getSharedBy().getName());
                    map.put("sharedAt", s.getSharedAt());
                    map.put("message", s.getMessage());
                    return map;
                }).collect(Collectors.toList());
    }

    @PostMapping("/course-files/copy-shared")
    public Map<String, Object> copyShared(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal CustomUserDetails user) {
        Long shareId = Long.valueOf(body.get("shareId").toString());
        String academicYear = body.get("academicYear").toString();
        String section = body.get("section").toString();

        com.mitmeerut.CFM_Portal.Model.CourseFile newFile = courseFileService.copySharedFile(
                shareId, academicYear, section, user.getTeacher().getId());

        Map<String, Object> response = new HashMap<>();
        response.put("id", newFile.getId());
        response.put("message", "Shared file copied successfully");
        return response;
    }
}
