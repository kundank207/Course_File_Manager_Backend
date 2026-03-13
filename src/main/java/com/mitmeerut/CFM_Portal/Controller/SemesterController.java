package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Model.Semester;
import com.mitmeerut.CFM_Portal.Service.SemesterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/semester")
@CrossOrigin(origins = { "http://localhost:5000", "http://localhost:5173" }, allowCredentials = "true")
public class SemesterController {

    @Autowired
    private SemesterService semesterService;

    @GetMapping
    public List<Semester> getSemesters(
            @RequestParam("programId") Long programId,
            @RequestParam("branchId") Long branchId) {
        return semesterService.getSemesters(programId, branchId);
    }

    @GetMapping("/all")
    public List<Semester> getAllSemesters() {
        return semesterService.getAllSemesters();
    }

    @PostMapping
    public Semester createSemester(@RequestBody Map<String, Object> body) {
        return semesterService.createSemester(body);
    }

    @PutMapping("/{id}")
    public Semester updateSemester(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        return semesterService.updateSemester(id, body);
    }

    @DeleteMapping("/{id}")
    public String deleteSemester(@PathVariable("id") Long id) {
        semesterService.deleteSemester(id);
        return "Semester Deleted";
    }

    @PostMapping("/generate-bulk")
    public ResponseEntity<Map<String, String>> generateBulk(@RequestBody Map<String, Object> body) {
        Long programId = Long.valueOf(body.get("programId").toString());
        Long branchId = Long.valueOf(body.get("branchId").toString());
        int total = Integer.parseInt(body.get("total").toString());

        semesterService.generateSemesters(programId, branchId, total);

        return ResponseEntity.ok(Map.of("message", "Semesters generated successfully up to " + total));
    }
}
