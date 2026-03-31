package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Model.CourseTeacher;
import com.mitmeerut.CFM_Portal.Model.Teacher;
import com.mitmeerut.CFM_Portal.Repository.DepartmentRepository;
import com.mitmeerut.CFM_Portal.Repository.TeacherRepository;
import com.mitmeerut.CFM_Portal.Service.CourseTeacherService;
import com.mitmeerut.CFM_Portal.dto.AssignCourseDTO;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hod")

public class HodCourseAssignController {

    private final CourseTeacherService courseTeacherService;
    private final DepartmentRepository departmentRepository;
    private final TeacherRepository teacherRepository;

    @Autowired
    public HodCourseAssignController(CourseTeacherService courseTeacherService,
            DepartmentRepository departmentRepository,
            TeacherRepository teacherRepository) {
        this.courseTeacherService = courseTeacherService;
        this.departmentRepository = departmentRepository;
        this.teacherRepository = teacherRepository;
    }

    @PostMapping("/assign-course")
    @PreAuthorize("hasRole('HOD') or hasAuthority('MANAGE_DEPT')")
    public void assignCourse(@RequestBody AssignCourseDTO dto, @AuthenticationPrincipal CustomUserDetails user) {
        System.out.println("DEBUG: Request received at /assign-course");
        System.out.println("DEBUG: DTO: " + dto);
        System.out.println("DEBUG: User: " + user.getUsername() + ", Role: " + user.getRole());
        try {
            courseTeacherService.assignCourse(dto, user);
            System.out.println("DEBUG: Service completed successfully");
        } catch (Exception e) {
            System.out.println("DEBUG: Service failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping("/course-assignments")
    @PreAuthorize("hasRole('HOD') or hasAuthority('MANAGE_DEPT')")
    public ResponseEntity<List<Map<String, Object>>> getCourseAssignments(
            @AuthenticationPrincipal CustomUserDetails user) {
        List<CourseTeacher> assignments = courseTeacherService.getAllAssignmentsForDepartment(user);

        // Return flat list of individual assignments for easier edit/delete
        List<Map<String, Object>> result = assignments.stream()
                .map(ct -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", ct.getId());
                    item.put("courseId", ct.getCourse().getId());
                    item.put("courseCode", ct.getCourse().getCode());
                    item.put("courseTitle", ct.getCourse().getTitle());
                    item.put("teacherId", ct.getTeacher().getId());
                    item.put("teacherName", ct.getTeacher().getName());
                    item.put("departmentId", ct.getDepartment() != null ? ct.getDepartment().getId() : null);
                    item.put("departmentName", ct.getDepartment() != null ? ct.getDepartment().getName() : "N/A");
                    item.put("departmentCode", ct.getDepartment() != null ? ct.getDepartment().getCode() : "");
                    item.put("section", ct.getSection());
                    item.put("academicYear", ct.getAcademicYear());
                    item.put("isSubjectHead", ct.getIsSubjectHead() != null ? ct.getIsSubjectHead() : false);
                    return item;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/departments")
    @PreAuthorize("hasRole('HOD') or hasAuthority('MANAGE_DEPT')")
    public ResponseEntity<?> getAllDepartments() {
        return ResponseEntity.ok(departmentRepository.findAll());
    }

    @GetMapping("/teachers-by-department")
    @PreAuthorize("hasRole('HOD') or hasAuthority('MANAGE_DEPT')")
    public ResponseEntity<?> getTeachersByDepartment(@RequestParam("department_id") Long departmentId) {
        List<Teacher> teachers = teacherRepository.findByDepartmentId(departmentId);
        List<Map<String, Object>> result = teachers.stream().map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId());
            map.put("name", t.getName());
            map.put("departmentName", t.getDepartment() != null ? t.getDepartment().getName() : "N/A");
            map.put("departmentCode", t.getDepartment() != null ? t.getDepartment().getCode() : "");
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/course-assignments/{id}")
    @PreAuthorize("hasRole('HOD') or hasAuthority('MANAGE_DEPT')")
    public ResponseEntity<?> updateAssignment(
            @PathVariable("id") Long id,
            @RequestBody AssignCourseDTO dto,
            @AuthenticationPrincipal CustomUserDetails user) {
        try {
            courseTeacherService.updateAssignment(id, dto, user);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Assignment updated successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/course-assignments/{id}")
    @PreAuthorize("hasRole('HOD') or hasAuthority('MANAGE_DEPT')")
    public ResponseEntity<?> deleteAssignment(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        try {
            courseTeacherService.deleteAssignment(id, user);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Assignment deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

}
