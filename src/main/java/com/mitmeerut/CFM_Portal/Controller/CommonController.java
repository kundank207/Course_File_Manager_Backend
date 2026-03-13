package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Model.Teacher;
import com.mitmeerut.CFM_Portal.Repository.TeacherRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/common")
@PreAuthorize("isAuthenticated()")
public class CommonController {

    private final TeacherRepository teacherRepository;

    public CommonController(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    @GetMapping("/faculty")
    public ResponseEntity<List<Map<String, Object>>> getAllFaculty() {
        List<Teacher> teachers = teacherRepository.findAll();
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
}
