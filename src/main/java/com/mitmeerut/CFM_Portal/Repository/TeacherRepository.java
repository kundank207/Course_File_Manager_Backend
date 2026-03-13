package com.mitmeerut.CFM_Portal.Repository;

import com.mitmeerut.CFM_Portal.Model.Department;
import com.mitmeerut.CFM_Portal.Model.Teacher;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    java.util.Optional<Teacher> findByEmailOfficial(String emailOfficial);

    List<Teacher> findByDepartment(Department department);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Teacher t WHERE t.department.id = :departmentId")
    List<Teacher> findByDepartmentId(
            @org.springframework.data.repository.query.Param("departmentId") Long departmentId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) FROM Teacher t WHERE t.department.id = :departmentId")
    long countByDepartmentId(@org.springframework.data.repository.query.Param("departmentId") Long departmentId);

}
