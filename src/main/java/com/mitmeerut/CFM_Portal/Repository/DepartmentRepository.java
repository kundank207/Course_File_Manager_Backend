package com.mitmeerut.CFM_Portal.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

import com.mitmeerut.CFM_Portal.Model.Department;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
	Optional<Department> findByHodId(Long hodId);

	Optional<Department> findByName(String name);

	boolean existsByName(String name);

	boolean existsByCode(String code);

}
