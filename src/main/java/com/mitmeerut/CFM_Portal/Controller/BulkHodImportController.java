package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Service.BulkHodImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/hod/bulk-import")

@PreAuthorize("hasRole('HOD')")
public class BulkHodImportController {

    @Autowired
    private BulkHodImportService importService;

    // ================= COURSES =================

    @GetMapping("/course/template")
    public ResponseEntity<byte[]> downloadCourseTemplate() throws IOException {
        System.out.println("DEBUG: Downloading course template...");
        byte[] data = importService.generateCourseTemplate();
        System.out.println("DEBUG: Template generated, size: " + data.length);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=course_import_template.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @PostMapping("/course")
    public ResponseEntity<Map<String, Object>> importCourses(
            @RequestParam("file") MultipartFile file,
            @RequestParam("departmentId") Long departmentId) throws IOException {
        return ResponseEntity.ok(importService.importCourses(file, departmentId));
    }

    // ================= ASSIGNMENTS =================

    @GetMapping("/assignment/template")
    public ResponseEntity<byte[]> downloadAssignmentTemplate() throws IOException {
        System.out.println("DEBUG: Downloading assignment template...");
        byte[] data = importService.generateAssignmentTemplate();
        System.out.println("DEBUG: Template generated, size: " + data.length);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=assignment_import_template.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @PostMapping("/assignment")
    public ResponseEntity<Map<String, Object>> importAssignments(
            @RequestParam("file") MultipartFile file,
            @RequestParam("departmentId") Long departmentId) throws IOException {
        return ResponseEntity.ok(importService.importAssignments(file, departmentId));
    }
}
