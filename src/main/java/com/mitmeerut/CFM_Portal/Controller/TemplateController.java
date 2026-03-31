package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Model.Template;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;
import com.mitmeerut.CFM_Portal.Service.TemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/templates")

public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    private CustomUserDetails currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            throw new RuntimeException("Please login first");
        }

        return (CustomUserDetails) auth.getPrincipal();
    }

    @PostMapping
    public ResponseEntity<Template> create(@RequestBody Template template) {
        return ResponseEntity.ok(
                templateService.createTemplate(template, currentUser()));
    }

    // Get all templates (for admin)
    @GetMapping
    public List<Template> listAll() {
        return templateService.getByDepartment(null);
    }

    @GetMapping("/department")
    public List<Template> listByDepartment() {
        Long deptId = currentUser().getDepartmentId();
        if (deptId == null) {
            // Admin user - return all templates
            return templateService.getByDepartment(null);
        }
        return templateService.getByDepartment(deptId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Template> update(
            @PathVariable("id") Long id,
            @RequestBody Template template) {
        return ResponseEntity.ok(
                templateService.update(id, template, currentUser()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        templateService.delete(id, currentUser());
        return ResponseEntity.noContent().build();
    }
}
