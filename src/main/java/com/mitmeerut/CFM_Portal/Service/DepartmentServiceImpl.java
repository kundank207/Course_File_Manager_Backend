package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.Department;
import com.mitmeerut.CFM_Portal.Model.Institute;
import com.mitmeerut.CFM_Portal.Model.Teacher;
import com.mitmeerut.CFM_Portal.Repository.DepartmentRepository;
import com.mitmeerut.CFM_Portal.Repository.InstituteRepository;
import com.mitmeerut.CFM_Portal.Repository.TeacherRepository;
import com.mitmeerut.CFM_Portal.dto.DepartmentDTO;
import com.mitmeerut.CFM_Portal.Repository.UserRepository;
import com.mitmeerut.CFM_Portal.Model.User;

import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepo;

    @Autowired
    private InstituteRepository instituteRepo;

    @Autowired
    private TeacherRepository teacherRepo;

    @Autowired
    private UserRepository userRepo;

    @Override
    @Cacheable(value = "departments")
    public List<Department> getAllDepartments() {
        return departmentRepo.findAll();
    }

    @Override
    @Cacheable(value = "departments", key = "#id")
    public Department getDepartmentById(Long id) {
        return departmentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));
    }

    @Override
    @CacheEvict(value = "departments", allEntries = true)
    public Department createDepartment(DepartmentDTO dto) {
        if (departmentRepo.existsByName(dto.getName())) {
            throw new RuntimeException("Department with name '" + dto.getName() + "' already exists");
        }
        if (departmentRepo.existsByCode(dto.getCode())) {
            throw new RuntimeException("Department with code '" + dto.getCode() + "' already exists");
        }
        Department dept = new Department();
        mapDtoToEntity(dto, dept);
        return departmentRepo.save(dept);
    }

    @Override
    @CacheEvict(value = "departments", allEntries = true)
    public Department updateDepartment(Long id, DepartmentDTO dto) {
        Department dept = getDepartmentById(id);
        mapDtoToEntity(dto, dept);
        return departmentRepo.save(dept);
    }

    @Override
    public String checkBeforeDelete(Long departmentId) {

        if (!departmentRepo.existsById(departmentId)) {
            throw new RuntimeException("Department not found");
        }

        return "This department and all related data will be deleted. Do you want to continue?";
    }

    @Override
    @Transactional
    @CacheEvict(value = "departments", allEntries = true)
    public void confirmDeleteDepartment(Long departmentId) {

        Department dept = departmentRepo.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        // Clear the HOD reference from department
        Teacher hod = dept.getHod();
        if (hod != null) {
            hod.setDepartment(null);
            teacherRepo.save(hod);
        }
        dept.setHod(null);

        // Clear department reference from all teachers in this department
        List<Teacher> teachers = teacherRepo.findByDepartmentId(departmentId);
        for (Teacher teacher : teachers) {
            teacher.setDepartment(null);
            teacherRepo.save(teacher);
        }

        departmentRepo.delete(dept);
    }

    private void mapDtoToEntity(DepartmentDTO dto, Department dept) {

        // Institute (REQUIRED)
        Institute institute = instituteRepo.findById(dto.getInstituteId())
                .orElseThrow(() -> new RuntimeException("Institute not found"));

        dept.setName(dto.getName());
        dept.setCode(dto.getCode());
        dept.setInstitute(institute);

        // HOD (OPTIONAL)
        if (dto.getHodId() != null) {
            Teacher newHodTeacher = teacherRepo.findById(dto.getHodId())
                    .orElseThrow(() -> new RuntimeException("HOD not found"));

            Teacher oldHodTeacher = dept.getHod();

            // Only act if the HOD is actually changing
            if (oldHodTeacher == null || !oldHodTeacher.getId().equals(newHodTeacher.getId())) {

                // 1. CLEAR HOD role for ANYONE in this department to ensure only one HOD
                List<User> currentDeptUsers = userRepo.findByTeacher_DepartmentId(dept.getId());
                for (User u : currentDeptUsers) {
                    if (u.getRole() == User.userRole.HOD) {
                        u.setRole(User.userRole.TEACHER);
                        userRepo.save(u);
                    }
                }

                // 2. Set role of new HOD to HOD
                userRepo.findByTeacherId(newHodTeacher.getId()).ifPresent(u -> {
                    u.setRole(User.userRole.HOD);
                    userRepo.save(u);
                });

                dept.setHod(newHodTeacher);
                newHodTeacher.setDepartment(dept);
                teacherRepo.save(newHodTeacher);
            }
        } else {
            // HOD is being removed
            Teacher oldHodTeacher = dept.getHod();
            if (oldHodTeacher != null) {
                userRepo.findByTeacherId(oldHodTeacher.getId()).ifPresent(u -> {
                    u.setRole(User.userRole.TEACHER);
                    userRepo.save(u);
                });
            }
            dept.setHod(null);
        }
    }
}
