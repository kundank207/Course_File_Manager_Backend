package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.dto.DepartmentDTO;
import com.mitmeerut.CFM_Portal.Model.Department;
import com.mitmeerut.CFM_Portal.Service.DepartmentService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/departments")
@CrossOrigin(origins = { "http://localhost:5000" }, allowCredentials = "true")
public class DepartmentController {

    @Autowired
    private DepartmentService service;

    @GetMapping
    public List<Department> getDepartments() {
        return service.getAllDepartments();
    }

    @PostMapping
    public Department create(@RequestBody DepartmentDTO dto) {
        return service.createDepartment(dto);
    }

    @PutMapping("/{id}")
    public Department update(@PathVariable("id") Long id, @RequestBody DepartmentDTO dto) {
        return service.updateDepartment(id, dto);
    }

    // @DeleteMapping("/{id}")
    // public void delete(@PathVariable("id") Long id) {
    // service.deleteDepartment(id);
    // }

    @DeleteMapping("/{id}/check")
    public ResponseEntity<String> checkDelete(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.checkBeforeDelete(id));
    }

    @DeleteMapping("/{id}/confirm")
    public ResponseEntity<String> confirmDelete(@PathVariable("id") Long id) {
        service.confirmDeleteDepartment(id);
        return ResponseEntity.ok("Department deleted successfully");
    }
}
