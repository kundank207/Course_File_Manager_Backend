package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.dto.DepartmentDTO;
import com.mitmeerut.CFM_Portal.Model.Department;

import java.util.List;

public interface DepartmentService {

    List<Department> getAllDepartments();

    Department createDepartment(DepartmentDTO dto);

    Department updateDepartment(Long id, DepartmentDTO dto);

//    void deleteDepartment(Long id);
    String checkBeforeDelete(Long departmentId);
    void confirmDeleteDepartment(Long departmentId);

    Department getDepartmentById(Long id);
}
