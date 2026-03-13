package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.Department;
import com.mitmeerut.CFM_Portal.Model.Program;
import com.mitmeerut.CFM_Portal.Repository.DepartmentRepository;
import com.mitmeerut.CFM_Portal.Repository.ProgramRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ProgramServiceImpl implements ProgramService {

    @Autowired
    private ProgramRepository programRepo;

    @Autowired
    private DepartmentRepository departmentRepo;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @jakarta.annotation.PostConstruct
    public void fixDatabaseIndexes() {
        try {
            // Hum us purane index ko delete karne ki koshish karenge jo error de raha hai
            jdbcTemplate.execute("ALTER TABLE Program DROP INDEX UKha1ojetw3fv9tfdrrvfy99yuf");
            System.out.println("SUCCESS: Purana unique index 'UKha1ojetw3fv9tfdrrvfy99yuf' delete ho gaya.");
        } catch (Exception e) {
            // Agar index pehle hi delete ho chuka hai ya nahi mila toh yahan error ignore
            // karenge
            System.out.println("INFO: Unique index pehle se hi deleted hai ya koi aur issue: " + e.getMessage());
        }
    }

    @Override
    @Cacheable(value = "programs")
    public List<Program> getAllPrograms() {
        return programRepo.findAll();
    }

    @Override
    @CacheEvict(value = "programs", allEntries = true)
    public Program createProgram(Map<String, Object> body) {
        String name = (String) body.get("name");
        Long deptId = Long.valueOf(body.get("department_id").toString());

        if (programRepo.existsByNameAndDepartmentId(name, deptId)) {
            throw new RuntimeException("Program with name '" + name + "' already exists in this department");
        }

        Program p = new Program();
        p.setName(name);
        p.setCode((String) body.get("code"));
        p.setDegree_type((String) body.get("degree_type"));
        p.setDuration_year(Integer.valueOf(body.get("duration_year").toString()));

        Department d = departmentRepo.findById(deptId)
                .orElseThrow(() -> new RuntimeException("Department Not Found"));

        p.setDepartment(d);

        return programRepo.save(p);
    }

    @Override
    @CacheEvict(value = "programs", allEntries = true)
    public Program updateProgram(Long id, Map<String, Object> body) {

        Program p = programRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Program Not Found"));

        if (body.containsKey("name") || body.containsKey("department_id")) {
            String newName = body.containsKey("name") ? (String) body.get("name") : p.getName();
            Long newDeptId = body.containsKey("department_id") ? Long.valueOf(body.get("department_id").toString())
                    : p.getDepartment().getId();

            // OPTIMIZED: Use repository instead of stream filtering ALL programs
            if (programRepo.existsByNameAndDepartmentId(newName, newDeptId) &&
                    (!newName.equals(p.getName()) || !newDeptId.equals(p.getDepartment().getId()))) {
                throw new RuntimeException("Program with name '" + newName + "' already exists in this department");
            }

            p.setName(newName);
            if (body.containsKey("department_id")) {
                Department d = departmentRepo.findById(newDeptId)
                        .orElseThrow(() -> new RuntimeException("Department Not Found"));
                p.setDepartment(d);
            }
        }
        // ... rest of method matches ...

        if (body.containsKey("code"))
            p.setCode((String) body.get("code"));

        if (body.containsKey("degree_type"))
            p.setDegree_type((String) body.get("degree_type"));

        if (body.containsKey("duration_year"))
            p.setDuration_year(Integer.valueOf(body.get("duration_year").toString()));

        return programRepo.save(p);
    }

    @Override
    @CacheEvict(value = "programs", allEntries = true)
    public void deleteProgram(Long id) {
        programRepo.deleteById(id);
    }
}
