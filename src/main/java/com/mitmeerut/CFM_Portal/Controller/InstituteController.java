package com.mitmeerut.CFM_Portal.Controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mitmeerut.CFM_Portal.Model.Institute;
import com.mitmeerut.CFM_Portal.Service.InstituteService;

@RestController
@RequestMapping("/api/admin/institutes")
@CrossOrigin(origins = { "http://localhost:5000", "http://localhost:5173" }, allowCredentials = "true")
public class InstituteController {

    @Autowired
    private InstituteService service;

    @GetMapping
    public List<Institute> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Institute getById(@PathVariable("id") Long id) {
        return service.findById(id)
                .orElseThrow(() -> new RuntimeException("Institute not found"));
    }

    @PostMapping
    public Institute create(@RequestBody Institute inst) {
        return service.create(inst);
    }

    @PutMapping("/{id}")
    public Institute update(@PathVariable("id") Long id, @RequestBody Institute inst) {
        return service.update(id, inst);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        service.delete(id);
    }
}
