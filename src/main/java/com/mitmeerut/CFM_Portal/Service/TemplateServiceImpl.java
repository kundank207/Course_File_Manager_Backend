package com.mitmeerut.CFM_Portal.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mitmeerut.CFM_Portal.Model.Department;
import com.mitmeerut.CFM_Portal.Model.Teacher;
import com.mitmeerut.CFM_Portal.Model.Template;
import com.mitmeerut.CFM_Portal.Model.User;
import com.mitmeerut.CFM_Portal.Repository.TemplateRepository;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository repo;
    private final ObjectMapper mapper = new ObjectMapper();

    public TemplateServiceImpl(TemplateRepository repo) {
        this.repo = repo;
    }

    @Override
    public Template createTemplate(Template template, CustomUserDetails user) {

        // Allow both ADMIN and HOD to create templates
        if (user.getRole() != User.userRole.HOD && user.getRole() != User.userRole.ADMIN) {
            throw new RuntimeException("Only ADMIN or HOD can create templates");
        }

        Long departmentId;

        if (user.getRole() == User.userRole.ADMIN) {
            // Admin must provide departmentId in request, or use default (1)
            departmentId = template.getDepartmentId() != null ? template.getDepartmentId() : 1L;
        } else {
            // HOD gets department from their teacher profile
            Teacher hodTeacher = user.getTeacher();
            if (hodTeacher == null) {
                throw new RuntimeException("HOD teacher profile missing");
            }

            Department department = hodTeacher.getDepartment();
            if (department == null) {
                throw new RuntimeException("Department not linked to HOD");
            }
            departmentId = department.getId();
        }

        template.setId(null); // new insert
        template.setDepartmentId(departmentId);
        template.setCreatedBy(user.getUserId());
        template.setCreatedAt(LocalDateTime.now());

        return repo.save(template);
    }

    @Override
    public List<Template> getByDepartment(Long departmentId) {
        if (departmentId == null) {
            return repo.findAll();
        }
        return repo.findByDepartmentId(departmentId);
    }

    @Override
    public Template update(Long id, Template template, CustomUserDetails user) {

        Template existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        // Admin can update any template, HOD can only update their department's
        // templates
        if (user.getRole() != User.userRole.ADMIN && !existing.getDepartmentId().equals(user.getDepartmentId())) {
            throw new RuntimeException("Unauthorized");
        }

        existing.setName(template.getName());
        existing.setDescription(template.getDescription());
        existing.setStructure(template.getStructure());
        existing.setChecklist(template.getChecklist());
        existing.setTemplateType(template.getTemplateType());
        existing.setSectionsCount(template.getSectionsCount());

        return repo.save(existing);
    }

    @Override
    public void delete(Long id, CustomUserDetails user) {

        Template template = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        // Admin can delete any template, HOD can only delete their department's
        // templates
        if (user.getRole() != User.userRole.ADMIN && !template.getDepartmentId().equals(user.getDepartmentId())) {
            throw new RuntimeException("Unauthorized");
        }

        repo.delete(template);
    }

    @Override
    public Template getDepartmentTemplate(Long departmentId) {

        return repo.findFirstByDepartmentId(departmentId)
                .orElseThrow(() -> new RuntimeException("Template not found for department"));
    }

}
