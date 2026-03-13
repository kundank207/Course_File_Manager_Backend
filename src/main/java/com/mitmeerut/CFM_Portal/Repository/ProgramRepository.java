package com.mitmeerut.CFM_Portal.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import com.mitmeerut.CFM_Portal.Model.Program;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Long> {
    List<Program> findByDepartment_Id(Long departmentId);

    boolean existsByName(String name);

    java.util.Optional<Program> findByName(String name);

    boolean existsByNameAndDepartmentId(String name, Long departmentId);

    java.util.Optional<Program> findByNameAndDepartmentId(String name, Long departmentId);
}
